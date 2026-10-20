package com.jhaiian.clint.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import com.jhaiian.clint.browser.home.HomeTileIconFetcher
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object SharpFavicon {
    private const val TARGET_SIDE = 96
    private const val MAX_PARALLEL = 4

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Semaphore(MAX_PARALLEL)
    private val memory = LruCache<String, Bitmap>(160)
    private val inFlight = HashMap<String, Deferred<Bitmap?>>()

    private fun originOf(pageUrl: String): String? = runCatching {
        val url = URL(pageUrl)
        if (url.protocol != "http" && url.protocol != "https") return@runCatching null
        if (url.port == -1) "${url.protocol}://${url.host}" else "${url.protocol}://${url.host}:${url.port}"
    }.getOrNull()

    fun cached(pageUrl: String): Bitmap? = originOf(pageUrl)?.let { memory.get(it) }

    suspend fun load(context: Context, pageUrl: String): Bitmap? {
        val origin = originOf(pageUrl) ?: return null
        memory.get(origin)?.let { return it }
        val appContext = context.applicationContext
        val job = synchronized(inFlight) {
            inFlight.getOrPut(origin) {
                scope.async {
                    try {
                        val full = gate.withPermit { HomeTileIconFetcher.load(appContext, origin) }
                        full?.let { shrink(it) }?.also { memory.put(origin, it) }
                    } finally {
                        synchronized(inFlight) { inFlight.remove(origin) }
                    }
                }
            }
        }
        return job.await()
    }

    fun pick(regular: Bitmap?, sharp: Bitmap?): Bitmap? {
        if (sharp == null) return regular
        if (regular == null) return sharp
        return if (minOf(sharp.width, sharp.height) > minOf(regular.width, regular.height)) sharp else regular
    }

    private fun shrink(source: Bitmap): Bitmap {
        var current = source
        while (minOf(current.width, current.height) >= TARGET_SIDE * 2) {
            current = Bitmap.createScaledBitmap(current, current.width / 2, current.height / 2, true)
        }
        val side = minOf(current.width, current.height)
        if (side > TARGET_SIDE) {
            val scale = TARGET_SIDE.toFloat() / side
            current = Bitmap.createScaledBitmap(
                current,
                (current.width * scale).toInt().coerceAtLeast(1),
                (current.height * scale).toInt().coerceAtLeast(1),
                true
            )
        }
        return current
    }
}
