package com.jhaiian.clint.shortcuts

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

object ShortcutIconStore {

    fun save(context: Context, shortcutId: String, bitmap: Bitmap): String {
        val file = fileFor(context, shortcutId)
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file.absolutePath
    }

    fun load(path: String?): Bitmap? {
        if (path.isNullOrEmpty()) return null
        return runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
    }

    fun delete(context: Context, shortcutId: String) {
        runCatching { fileFor(context, shortcutId).delete() }
    }

    fun directory(context: Context): File =
        File(context.applicationContext.filesDir, "shortcut_icons")

    fun fileFor(context: Context, shortcutId: String): File =
        File(directory(context), "$shortcutId.png")
}
