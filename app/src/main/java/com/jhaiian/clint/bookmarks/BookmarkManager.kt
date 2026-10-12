package com.jhaiian.clint.bookmarks

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import java.io.File

object BookmarkManager {

    private const val LEGACY_PREFS_NAME = "clint_bookmarks"
    private const val LEGACY_KEY = "bookmarks"

    private val BOOKMARK_COLUMNS = arrayOf(
        BookmarkDatabase.COL_ID, BookmarkDatabase.COL_URL, BookmarkDatabase.COL_TITLE,
        BookmarkDatabase.COL_FAVICON_URL, BookmarkDatabase.COL_LAST_VISIT,
        BookmarkDatabase.COL_ADDED_AT, BookmarkDatabase.COL_BOOKMARK_FOLDER_ID
    )

    private val FOLDER_COLUMNS = arrayOf(
        BookmarkDatabase.COL_FOLDER_ID, BookmarkDatabase.COL_FOLDER_NAME,
        BookmarkDatabase.COL_FOLDER_PARENT_ID, BookmarkDatabase.COL_FOLDER_CREATED_AT
    )

    @Volatile private var db: BookmarkDatabase? = null

    private fun db(context: Context): BookmarkDatabase {
        return db ?: synchronized(this) {
            db ?: BookmarkDatabase(context.applicationContext).also {
                db = it
                migrateLegacyPrefs(context, it.writableDatabase)
            }
        }
    }

    private fun migrateLegacyPrefs(context: Context, writable: SQLiteDatabase) {
        val prefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(LEGACY_KEY, null) ?: return
        runCatching {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val values = ContentValues().apply {
                    put(BookmarkDatabase.COL_URL, obj.getString("url"))
                    put(BookmarkDatabase.COL_TITLE, obj.optString("title", ""))
                    put(BookmarkDatabase.COL_FAVICON_URL, obj.optString("faviconUrl", ""))
                    put(BookmarkDatabase.COL_ADDED_AT, obj.optLong("addedAt", System.currentTimeMillis()))
                    put(BookmarkDatabase.COL_LAST_VISIT, obj.optLong("lastVisit", 0L))
                }
                writable.insertWithOnConflict(BookmarkDatabase.TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE)
            }
        }
        prefs.edit().remove(LEGACY_KEY).apply()
    }

    private fun readBookmark(cursor: Cursor): Bookmark {
        return Bookmark(
            id = cursor.getLong(0),
            url = cursor.getString(1),
            title = cursor.getString(2),
            faviconUrl = cursor.getString(3),
            lastVisit = cursor.getLong(4),
            addedAt = cursor.getLong(5),
            folderId = if (cursor.isNull(6)) null else cursor.getLong(6)
        )
    }

    private fun readFolder(cursor: Cursor): BookmarkFolder {
        return BookmarkFolder(
            id = cursor.getLong(0),
            name = cursor.getString(1),
            parentId = if (cursor.isNull(2)) null else cursor.getLong(2),
            createdAt = cursor.getLong(3)
        )
    }

    fun getAll(context: Context): MutableList<Bookmark> {
        val cursor = db(context).readableDatabase.query(
            BookmarkDatabase.TABLE, BOOKMARK_COLUMNS,
            null, null, null, null,
            "${BookmarkDatabase.COL_ADDED_AT} DESC"
        )
        val list = mutableListOf<Bookmark>()
        cursor.use { while (it.moveToNext()) list.add(readBookmark(it)) }
        return list
    }

    fun getAllFoldersFlat(context: Context): List<BookmarkFolder> {
        val cursor = db(context).readableDatabase.query(
            BookmarkDatabase.FOLDER_TABLE, FOLDER_COLUMNS,
            null, null, null, null,
            "${BookmarkDatabase.COL_FOLDER_NAME} COLLATE NOCASE ASC"
        )
        val list = mutableListOf<BookmarkFolder>()
        cursor.use { while (it.moveToNext()) list.add(readFolder(it)) }
        return list
    }

    fun createFolder(context: Context, name: String, parentId: Long?): Long {
        val values = ContentValues().apply {
            put(BookmarkDatabase.COL_FOLDER_NAME, name)
            if (parentId == null) putNull(BookmarkDatabase.COL_FOLDER_PARENT_ID) else put(BookmarkDatabase.COL_FOLDER_PARENT_ID, parentId)
            put(BookmarkDatabase.COL_FOLDER_CREATED_AT, System.currentTimeMillis())
        }
        return db(context).writableDatabase.insert(BookmarkDatabase.FOLDER_TABLE, null, values)
    }

    fun renameFolder(context: Context, folderId: Long, name: String) {
        val values = ContentValues().apply { put(BookmarkDatabase.COL_FOLDER_NAME, name) }
        db(context).writableDatabase.update(
            BookmarkDatabase.FOLDER_TABLE, values,
            "${BookmarkDatabase.COL_FOLDER_ID} = ?", arrayOf(folderId.toString())
        )
    }

    fun moveFolder(context: Context, folderId: Long, targetParentId: Long?) {
        if (targetParentId == folderId) return
        val values = ContentValues().apply {
            if (targetParentId == null) putNull(BookmarkDatabase.COL_FOLDER_PARENT_ID) else put(BookmarkDatabase.COL_FOLDER_PARENT_ID, targetParentId)
        }
        db(context).writableDatabase.update(
            BookmarkDatabase.FOLDER_TABLE, values,
            "${BookmarkDatabase.COL_FOLDER_ID} = ?", arrayOf(folderId.toString())
        )
    }

    fun deleteFolderRecursive(context: Context, folderId: Long) {
        val writable = db(context).writableDatabase
        writable.beginTransaction()
        try {
            deleteFolderInternal(writable, folderId)
            writable.setTransactionSuccessful()
        } finally {
            writable.endTransaction()
        }
    }

    private fun deleteFolderInternal(writable: SQLiteDatabase, folderId: Long) {
        val childCursor = writable.query(
            BookmarkDatabase.FOLDER_TABLE, arrayOf(BookmarkDatabase.COL_FOLDER_ID),
            "${BookmarkDatabase.COL_FOLDER_PARENT_ID} = ?", arrayOf(folderId.toString()),
            null, null, null
        )
        val childIds = mutableListOf<Long>()
        childCursor.use { while (it.moveToNext()) childIds.add(it.getLong(0)) }
        childIds.forEach { deleteFolderInternal(writable, it) }
        writable.delete(BookmarkDatabase.TABLE, "${BookmarkDatabase.COL_BOOKMARK_FOLDER_ID} = ?", arrayOf(folderId.toString()))
        writable.delete(BookmarkDatabase.FOLDER_TABLE, "${BookmarkDatabase.COL_FOLDER_ID} = ?", arrayOf(folderId.toString()))
    }

    fun moveBookmark(context: Context, url: String, targetFolderId: Long?) {
        val values = ContentValues().apply {
            if (targetFolderId == null) putNull(BookmarkDatabase.COL_BOOKMARK_FOLDER_ID) else put(BookmarkDatabase.COL_BOOKMARK_FOLDER_ID, targetFolderId)
        }
        db(context).writableDatabase.update(
            BookmarkDatabase.TABLE, values,
            "${BookmarkDatabase.COL_URL} = ?", arrayOf(url)
        )
    }

    fun add(context: Context, bookmark: Bookmark) {
        val values = ContentValues().apply {
            put(BookmarkDatabase.COL_URL, bookmark.url)
            put(BookmarkDatabase.COL_TITLE, bookmark.title)
            put(BookmarkDatabase.COL_FAVICON_URL, bookmark.faviconUrl)
            put(BookmarkDatabase.COL_ADDED_AT, System.currentTimeMillis())
            put(BookmarkDatabase.COL_LAST_VISIT, bookmark.lastVisit)
            if (bookmark.folderId == null) putNull(BookmarkDatabase.COL_BOOKMARK_FOLDER_ID) else put(BookmarkDatabase.COL_BOOKMARK_FOLDER_ID, bookmark.folderId)
        }
        db(context).writableDatabase.insertWithOnConflict(
            BookmarkDatabase.TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    fun remove(context: Context, url: String) {
        db(context).writableDatabase.delete(
            BookmarkDatabase.TABLE,
            "${BookmarkDatabase.COL_URL} = ?",
            arrayOf(url)
        )
    }

    fun updateLastVisit(context: Context, url: String) {
        val values = ContentValues().apply {
            put(BookmarkDatabase.COL_LAST_VISIT, System.currentTimeMillis())
        }
        db(context).writableDatabase.update(
            BookmarkDatabase.TABLE,
            values,
            "${BookmarkDatabase.COL_URL} = ?",
            arrayOf(url)
        )
    }

    fun search(context: Context, query: String): List<Bookmark> {
        val q = query.trim()
        if (q.isBlank()) return getAll(context)
        val cursor = db(context).readableDatabase.query(
            BookmarkDatabase.TABLE, BOOKMARK_COLUMNS,
            "${BookmarkDatabase.COL_URL} LIKE ? OR ${BookmarkDatabase.COL_TITLE} LIKE ?",
            arrayOf("%$q%", "%$q%"),
            null, null,
            "${BookmarkDatabase.COL_ADDED_AT} DESC"
        )
        val list = mutableListOf<Bookmark>()
        cursor.use { while (it.moveToNext()) list.add(readBookmark(it)) }
        return list
    }

    fun isBookmarked(context: Context, url: String): Boolean {
        val cursor = db(context).readableDatabase.query(
            BookmarkDatabase.TABLE,
            arrayOf(BookmarkDatabase.COL_ID),
            "${BookmarkDatabase.COL_URL} = ?",
            arrayOf(url),
            null, null, null, "1"
        )
        return cursor.use { it.moveToFirst() }
    }

    fun restoreBookmark(context: Context, bookmark: Bookmark) {
        val values = ContentValues().apply {
            put(BookmarkDatabase.COL_URL, bookmark.url)
            put(BookmarkDatabase.COL_TITLE, bookmark.title)
            put(BookmarkDatabase.COL_FAVICON_URL, bookmark.faviconUrl)
            put(BookmarkDatabase.COL_ADDED_AT, if (bookmark.addedAt > 0) bookmark.addedAt else System.currentTimeMillis())
            put(BookmarkDatabase.COL_LAST_VISIT, bookmark.lastVisit)
            if (bookmark.folderId == null) putNull(BookmarkDatabase.COL_BOOKMARK_FOLDER_ID) else put(BookmarkDatabase.COL_BOOKMARK_FOLDER_ID, bookmark.folderId)
        }
        db(context).writableDatabase.insertWithOnConflict(
            BookmarkDatabase.TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE
        )
    }

    fun checkpointAndGetDatabaseFile(context: Context): File {
        db(context).writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
        return context.getDatabasePath(BookmarkDatabase.DB_NAME)
    }
}
