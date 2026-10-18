package com.jhaiian.clint.browser.webview

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.util.UUID

internal object UploadCache {

    private const val DIR = "uploads"
    private const val MAX_AGE_MS = 24L * 60 * 60 * 1000

    fun copyAsync(context: Context, uris: Array<Uri>, onDone: (Array<Uri>) -> Unit) {
        val appContext = context.applicationContext
        ClintGecko.io.execute {
            prune(appContext)
            val copied = uris.mapNotNull { toFileUri(appContext, it) }.toTypedArray()
            ClintGecko.main.post { onDone(copied) }
        }
    }

    private fun toFileUri(context: Context, uri: Uri): Uri? {
        if (uri.scheme == "file") return uri
        return runCatching {
            val dir = File(File(context.cacheDir, DIR), UUID.randomUUID().toString()).apply { mkdirs() }
            val target = File(dir, displayName(context, uri))
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use { src -> target.outputStream().use { src.copyTo(it) } }
            Uri.fromFile(target)
        }.getOrNull()
    }

    private fun displayName(context: Context, uri: Uri): String {
        val queried = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
        var name = (queried ?: uri.lastPathSegment ?: "")
            .substringAfterLast('/')
            .replace(Regex("[\\\\:*?\"<>|\\u0000-\\u001f]"), "_")
            .trim()
            .trimStart('.')
        if (name.isEmpty()) name = "upload"
        if (!name.contains('.')) {
            val ext = context.contentResolver.getType(uri)?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            if (!ext.isNullOrEmpty()) name = "$name.$ext"
        }
        return if (name.length > 200) name.takeLast(200) else name
    }

    private fun prune(context: Context) {
        val root = File(context.cacheDir, DIR)
        val cutoff = System.currentTimeMillis() - MAX_AGE_MS
        root.listFiles()?.forEach { if (it.lastModified() < cutoff) it.deleteRecursively() }
    }
}
