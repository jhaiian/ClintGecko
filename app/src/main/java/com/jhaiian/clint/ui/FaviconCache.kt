package com.jhaiian.clint.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager
import com.jhaiian.clint.settings.datasaver.DataSaverMode
import java.io.File
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

object FaviconCache {

    private val memoryCache = HashMap<String, Bitmap>()
    private val executor = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun faviconUrlFor(pageUrl: String): String {
        return runCatching {
            val host = URL(pageUrl).host
            "https://$host/favicon.ico"
        }.getOrDefault("")
    }

    private fun isNetworkBlocked(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return DataSaverMode.isActive(prefs) && prefs.getBoolean("data_saver_disable_favicons", true)
    }

    fun load(context: Context, faviconUrl: String, cacheOnly: Boolean = false, onResult: (Bitmap?) -> Unit) {
        if (faviconUrl.isEmpty()) {
            onResult(null)
            return
        }
        val key = keyFor(faviconUrl)
        memoryCache[key]?.let { onResult(it); return }
        val appContext = context.applicationContext
        val cacheOnly = cacheOnly || isNetworkBlocked(appContext)
        executor.execute {
            val file = diskFile(appContext, key)
            val cached = if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            if (cached != null) {
                memoryCache[key] = cached
                mainHandler.post { onResult(cached) }
                return@execute
            }
            if (cacheOnly) {
                mainHandler.post { onResult(null) }
                return@execute
            }
            val bmp = tryFetch(faviconUrl) ?: tryFetch(fallbackUrlFor(faviconUrl))
            if (bmp != null) {
                memoryCache[key] = bmp
                runCatching {
                    file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
            }
            mainHandler.post { onResult(bmp) }
        }
    }

    fun loadMemoryOnly(context: Context, faviconUrl: String, onResult: (Bitmap?) -> Unit) {
        if (faviconUrl.isEmpty()) {
            onResult(null)
            return
        }
        val key = keyFor(faviconUrl)
        memoryCache[key]?.let { onResult(it); return }
        if (isNetworkBlocked(context.applicationContext)) {
            onResult(null)
            return
        }
        executor.execute {
            val bmp = tryFetch(faviconUrl) ?: tryFetch(fallbackUrlFor(faviconUrl))
            if (bmp != null) memoryCache[key] = bmp
            mainHandler.post { onResult(bmp) }
        }
    }

    fun evict(context: Context, pageUrl: String) {
        val faviconUrl = faviconUrlFor(pageUrl)
        if (faviconUrl.isEmpty()) return
        val key = keyFor(faviconUrl)
        memoryCache.remove(key)
        diskFile(context.applicationContext, key).delete()
    }

    private fun tryFetch(url: String): Bitmap? {
        if (url.isEmpty()) return null
        if (url.startsWith("data:", ignoreCase = true)) return decodeDataUri(url)
        return runCatching {
            val conn = URL(url).openConnection()
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            BitmapFactory.decodeStream(conn.getInputStream())
        }.getOrNull()
    }

    private fun decodeDataUri(uri: String): Bitmap? {
        return runCatching {
            val comma = uri.indexOf(',')
            if (comma < 0) return@runCatching null
            val header = uri.substring(0, comma)
            val payload = uri.substring(comma + 1)
            val bytes = if (header.contains(";base64", ignoreCase = true)) {
                Base64.decode(payload, Base64.DEFAULT)
            } else {
                java.net.URLDecoder.decode(payload, "UTF-8").toByteArray()
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    private fun fallbackUrlFor(faviconUrl: String): String {
        return runCatching {
            val host = URL(faviconUrl).host
            "https://icons.duckduckgo.com/ip3/$host.ico"
        }.getOrDefault("")
    }

    private fun keyFor(url: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun diskFile(context: Context, key: String): File {
        val dir = File(context.cacheDir, "favicons")
        dir.mkdirs()
        return File(dir, "$key.png")
    }
}
