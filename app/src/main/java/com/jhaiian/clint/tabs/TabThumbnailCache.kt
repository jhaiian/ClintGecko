package com.jhaiian.clint.tabs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.os.Build
import android.util.LruCache
import com.jhaiian.clint.browser.webview.ClintWebView
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object TabThumbnailCache {
    private const val MAX_DIMENSION_PX = 1024
    const val DIR_NAME = "tab_thumbnails"
    private const val WEBP_QUALITY = 85

    private val ioExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "tab-thumbnail-io").apply { priority = Thread.MIN_PRIORITY }
    }

    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 10L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    private val incognitoCache = ConcurrentHashMap<String, Bitmap>()

    fun capture(tabId: String, webView: ClintWebView, isIncognito: Boolean = false) {
        if (webView.width <= 0 || webView.height <= 0) return
        val context = webView.context.applicationContext
        webView.captureBitmap { bitmap ->
            if (bitmap != null) ioExecutor.execute { put(context, tabId, bitmap, isIncognito) }
        }
    }

    fun put(context: Context, tabId: String, source: Bitmap, isIncognito: Boolean = false) {
        val width = source.width
        val height = source.height
        if (width <= 0 || height <= 0) return
        val scale = (MAX_DIMENSION_PX.toFloat() / maxOf(width, height)).coerceAtMost(1f)
        runCatching {
            val bitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    source,
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                source.copy(Bitmap.Config.ARGB_8888, false)
            }
            store(context.applicationContext, tabId, bitmap, isIncognito)
        }
    }

    fun peek(tabId: String): Bitmap? = incognitoCache[tabId] ?: cache.get(tabId)

    fun get(context: Context, tabId: String): Bitmap? {
        peek(tabId)?.let { return it }
        val fromDisk = readFromDisk(context.applicationContext, tabId) ?: return null
        cache.put(tabId, fromDisk)
        return fromDisk
    }

    fun evict(context: Context, tabId: String) {
        cache.remove(tabId)
        incognitoCache.remove(tabId)
        val appContext = context.applicationContext
        ioExecutor.execute {
            cache.remove(tabId)
            incognitoCache.remove(tabId)
            diskFiles(appContext, tabId).forEach { it.delete() }
        }
    }

    fun clear() {
        cache.evictAll()
        incognitoCache.clear()
    }

    fun pruneDisk(context: Context, keepTabIds: Set<String>) {
        val appContext = context.applicationContext
        ioExecutor.execute {
            runCatching {
                diskDir(appContext).listFiles()?.forEach { file ->
                    if (file.nameWithoutExtension !in keepTabIds) file.delete()
                }
            }
        }
    }

    private fun store(context: Context, tabId: String, bitmap: Bitmap, isIncognito: Boolean) {
        if (isIncognito) {
            incognitoCache[tabId] = bitmap
            return
        }
        cache.put(tabId, bitmap)
        ioExecutor.execute { writeToDisk(context, tabId, bitmap) }
    }

    private fun diskDir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { mkdirs() }

    private fun webpFile(context: Context, tabId: String): File =
        File(diskDir(context), "$tabId.webp")

    private fun legacyFile(context: Context, tabId: String): File =
        File(diskDir(context), "$tabId.png")

    private fun diskFiles(context: Context, tabId: String): List<File> =
        listOf(webpFile(context, tabId), legacyFile(context, tabId))

    @Suppress("DEPRECATION")
    private fun writeToDisk(context: Context, tabId: String, bitmap: Bitmap) {
        runCatching {
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.WEBP
            }
            FileOutputStream(webpFile(context, tabId)).use { out ->
                bitmap.compress(format, WEBP_QUALITY, out)
            }
            legacyFile(context, tabId).delete()
        }
    }

    private fun readFromDisk(context: Context, tabId: String): Bitmap? {
        val file = diskFiles(context, tabId).firstOrNull { it.exists() } ?: return null
        return runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
    }
}
