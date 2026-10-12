package com.jhaiian.clint.bookmarks

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object BookmarksHtmlFormat {

    fun export(folders: List<BookmarkFolder>, bookmarks: List<Bookmark>): String {
        val childFolders = folders.groupBy { it.parentId }
        val childBookmarks = bookmarks.groupBy { it.folderId }
        val sb = StringBuilder()
        sb.append("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n")
        sb.append("<!-- This is an automatically generated file.\n     It will be read and overwritten.\n     DO NOT EDIT! -->\n")
        sb.append("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n")
        sb.append("<TITLE>Bookmarks</TITLE>\n")
        sb.append("<H1>Bookmarks</H1>\n")
        sb.append("<DL><p>\n")
        appendEntries(sb, null, childFolders, childBookmarks, 1)
        sb.append("</DL><p>\n")
        return sb.toString()
    }

    private fun appendEntries(
        sb: StringBuilder,
        parentId: Long?,
        childFolders: Map<Long?, List<BookmarkFolder>>,
        childBookmarks: Map<Long?, List<Bookmark>>,
        depth: Int
    ) {
        val indent = "    ".repeat(depth)
        childFolders[parentId].orEmpty().forEach { folder ->
            sb.append(indent).append("<DT><H3 ADD_DATE=\"").append(folder.createdAt / 1000).append("\">")
                .append(escapeHtml(folder.name)).append("</H3>\n")
            sb.append(indent).append("<DL><p>\n")
            appendEntries(sb, folder.id, childFolders, childBookmarks, depth + 1)
            sb.append(indent).append("</DL><p>\n")
        }
        childBookmarks[parentId].orEmpty().forEach { bookmark ->
            sb.append(indent).append("<DT><A HREF=\"").append(escapeHtml(bookmark.url))
                .append("\" ADD_DATE=\"").append(bookmark.addedAt / 1000).append("\">")
                .append(escapeHtml(bookmark.title)).append("</A>\n")
        }
    }

    private fun escapeHtml(s: String) = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun unescapeHtml(s: String) = s
        .replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'")
        .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")

    private val folderRegex = Regex("<DT><H3[^>]*>(.*?)</H3>", RegexOption.IGNORE_CASE)
    private val bookmarkRegex = Regex("<DT><A\\s+HREF=\"([^\"]*)\"[^>]*>(.*?)</A>", RegexOption.IGNORE_CASE)
    private val dlOpenRegex = Regex("<DL>", RegexOption.IGNORE_CASE)
    private val dlCloseRegex = Regex("</DL>", RegexOption.IGNORE_CASE)

    fun import(context: Context, html: String, rootFolderId: Long?): Int {
        var count = 0
        val stack = ArrayDeque<Long?>()
        stack.addLast(rootFolderId)
        var pendingFolderId: Long? = null
        var pendingIsFolder = false

        html.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            val folderMatch = folderRegex.find(line)
            val bookmarkMatch = bookmarkRegex.find(line)
            when {
                folderMatch != null -> {
                    val name = unescapeHtml(folderMatch.groupValues[1]).trim().ifBlank { "Untitled" }
                    pendingFolderId = BookmarkManager.createFolder(context, name, stack.last())
                    pendingIsFolder = true
                }
                bookmarkMatch != null -> {
                    val url = bookmarkMatch.groupValues[1].trim()
                    val title = unescapeHtml(bookmarkMatch.groupValues[2]).trim()
                    if (url.isNotBlank()) {
                        BookmarkManager.add(
                            context,
                            Bookmark(url = url, title = title.ifBlank { url }, addedAt = System.currentTimeMillis(), folderId = stack.last())
                        )
                        count++
                    }
                }
                dlOpenRegex.containsMatchIn(line) -> {
                    if (pendingIsFolder) {
                        stack.addLast(pendingFolderId)
                        pendingIsFolder = false
                    }
                }
                dlCloseRegex.containsMatchIn(line) -> {
                    if (stack.size > 1) stack.removeLast()
                }
            }
        }
        return count
    }
}

object BookmarksSqliteFormat {

    fun export(context: Context, out: OutputStream) {
        val dbFile = BookmarkManager.checkpointAndGetDatabaseFile(context)
        dbFile.inputStream().use { input -> input.copyTo(out) }
    }

    fun import(context: Context, input: InputStream): Int {
        val tempFile = File.createTempFile("bookmarks_import", ".db", context.cacheDir)
        try {
            tempFile.outputStream().use { out -> input.copyTo(out) }
            var count = 0
            SQLiteDatabase.openDatabase(tempFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                val folderIdMap = mutableMapOf<Long, Long>()
                val remaining = mutableListOf<Triple<Long, String, Long?>>()
                db.query("bookmark_folders", arrayOf("id", "name", "parent_id"), null, null, null, null, null).use { cursor ->
                    while (cursor.moveToNext()) {
                        remaining.add(Triple(cursor.getLong(0), cursor.getString(1), if (cursor.isNull(2)) null else cursor.getLong(2)))
                    }
                }
                while (remaining.isNotEmpty()) {
                    val iterator = remaining.iterator()
                    var progressed = false
                    while (iterator.hasNext()) {
                        val (oldId, name, oldParentId) = iterator.next()
                        val newParentId = if (oldParentId == null) null else folderIdMap[oldParentId]
                        if (oldParentId == null || newParentId != null) {
                            folderIdMap[oldId] = BookmarkManager.createFolder(context, name, newParentId)
                            iterator.remove()
                            progressed = true
                        }
                    }
                    if (!progressed) break
                }
                db.query(
                    "bookmarks", arrayOf("url", "title", "favicon_url", "added_at", "last_visit", "folder_id"),
                    null, null, null, null, null
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val oldFolderId = if (cursor.isNull(5)) null else cursor.getLong(5)
                        BookmarkManager.restoreBookmark(
                            context,
                            Bookmark(
                                url = cursor.getString(0), title = cursor.getString(1), faviconUrl = cursor.getString(2),
                                addedAt = cursor.getLong(3), lastVisit = cursor.getLong(4),
                                folderId = oldFolderId?.let { folderIdMap[it] }
                            )
                        )
                        count++
                    }
                }
            }
            return count
        } finally {
            tempFile.delete()
        }
    }
}
