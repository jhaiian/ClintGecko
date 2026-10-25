package com.jhaiian.clint.downloads

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.PowerManager
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

internal object DownloadAutoTuner {

    const val AUTO = 0
    const val MAX_AUTO_PARTS = 128
    const val MIN_AUTO_PART_BYTES = 1024L * 1024L
    private const val TARGET_AUTO_PART_BYTES = 16L * 1024L * 1024L

    private const val MAX_CONNECTIONS = 16
    private const val METERED_CONNECTIONS = 8
    private const val CONSTRAINED_CONNECTIONS = 4
    private const val MAX_SEGMENTS_MULTIPLEXED = 32
    private const val MAX_SEGMENTS_HTTP1 = 16
    private const val METERED_SEGMENTS = 12
    private const val CONSTRAINED_SEGMENTS = 6
    private const val START_CONNECTIONS = 3
    private const val START_SEGMENTS = 4

    private const val PREFS_NAME = "clint_download_auto"
    private const val KIND_CONNECTIONS = "c"
    private const val KIND_SEGMENTS = "s"
    private const val MAX_REMEMBERED = 100

    private enum class Condition { NORMAL, METERED, CONSTRAINED }

    private val remembered = ConcurrentHashMap<String, Int>()
    @Volatile private var loaded = false

    fun isAuto(value: Int): Boolean = value <= AUTO

    private fun condition(context: Context?): Condition {
        val ctx = context ?: return Condition.NORMAL
        val power = ctx.getSystemService(PowerManager::class.java)
        if (power != null) {
            if (power.isPowerSaveMode) return Condition.CONSTRAINED
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                power.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE
            ) return Condition.CONSTRAINED
        }
        val connectivity = ctx.getSystemService(ConnectivityManager::class.java)
        return if (connectivity?.isActiveNetworkMetered == true) Condition.METERED else Condition.NORMAL
    }

    fun connectionCeiling(context: Context?): Int = when (condition(context)) {
        Condition.NORMAL -> MAX_CONNECTIONS
        Condition.METERED -> METERED_CONNECTIONS
        Condition.CONSTRAINED -> CONSTRAINED_CONNECTIONS
    }

    fun segmentCeiling(context: Context?, multiplexed: Boolean): Int {
        val protocolCap = if (multiplexed) MAX_SEGMENTS_MULTIPLEXED else MAX_SEGMENTS_HTTP1
        return when (condition(context)) {
            Condition.NORMAL -> protocolCap
            Condition.METERED -> minOf(protocolCap, METERED_SEGMENTS)
            Condition.CONSTRAINED -> minOf(protocolCap, CONSTRAINED_SEGMENTS)
        }
    }

    fun startingConnections(context: Context?, host: String, ceiling: Int): Int =
        (learned(context, KIND_CONNECTIONS, host) ?: START_CONNECTIONS).coerceIn(1, ceiling.coerceAtLeast(1))

    fun startingSegments(context: Context?, host: String, ceiling: Int): Int =
        (learned(context, KIND_SEGMENTS, host) ?: START_SEGMENTS).coerceIn(1, ceiling.coerceAtLeast(1))

    fun rememberConnections(context: Context?, host: String, value: Int) = remember(context, KIND_CONNECTIONS, host, value)

    fun rememberSegments(context: Context?, host: String, value: Int) = remember(context, KIND_SEGMENTS, host, value)

    fun plannedParts(context: Context?, item: DownloadItem, host: String, manualMinPartBytes: Long): Int {
        if (!isAuto(item.splitParts)) {
            return maxOf(1, minOf(item.splitParts, (item.totalBytes / manualMinPartBytes).toInt()))
        }
        val connections = if (isAuto(item.multithreadingParts)) {
            startingConnections(context, host, connectionCeiling(context))
        } else item.multithreadingParts
        val bySize = (item.totalBytes / MIN_AUTO_PART_BYTES).coerceAtMost(MAX_AUTO_PARTS.toLong()).toInt()
        val bySpeed = (item.totalBytes / TARGET_AUTO_PART_BYTES).coerceAtMost(MAX_AUTO_PARTS.toLong()).toInt()
        return maxOf(1, minOf(maxOf(connections, 2, bySpeed), bySize))
    }

    private fun keyFor(kind: String, host: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(host.lowercase().toByteArray(Charsets.UTF_8))
        return kind + ":" + digest.take(8).joinToString("") { "%02x".format(it) }
    }

    private fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).all.forEach { (key, value) ->
                if (value is Int) remembered[key] = value
            }
            loaded = true
        }
    }

    private fun learned(context: Context?, kind: String, host: String): Int? {
        if (context == null || host.isBlank()) return null
        ensureLoaded(context)
        return remembered[keyFor(kind, host)]
    }

    private fun remember(context: Context?, kind: String, host: String, value: Int) {
        if (context == null || host.isBlank() || value <= 0) return
        ensureLoaded(context)
        val key = keyFor(kind, host)
        if (remembered[key] == value) return
        remembered[key] = value
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        val overflow = remembered.size - MAX_REMEMBERED
        if (overflow > 0) {
            remembered.keys.filter { it != key }.take(overflow).forEach {
                remembered.remove(it)
                editor.remove(it)
            }
        }
        editor.putInt(key, value).apply()
    }
}

internal class AutoConcurrencyController(
    private val current: AtomicInteger,
    private val maxAllowed: () -> Int,
    private val speedLimit: () -> Long
) {
    private var windowStartAt = 0L
    private var windowStartBytes = 0L
    private var lastRate = -1.0
    private var probing = false
    private var holdUntil = 0L

    fun onTick(now: Long, totalBytes: Long, canGrow: Boolean, blocked: Boolean) {
        if (windowStartAt == 0L) {
            windowStartAt = now
            windowStartBytes = totalBytes
            return
        }
        val elapsed = now - windowStartAt
        if (elapsed < WINDOW_MS) return
        val rate = (totalBytes - windowStartBytes) * 1000.0 / elapsed
        windowStartAt = now
        windowStartBytes = totalBytes
        val ceiling = maxAllowed().coerceAtLeast(1)
        if (current.get() > ceiling) current.set(ceiling)
        if (blocked) {
            probing = false
            lastRate = rate
            holdUntil = now + HOLD_MS
            return
        }
        if (probing) {
            probing = false
            if (lastRate > 0.0 && rate < lastRate * (1.0 + REQUIRED_GAIN)) {
                current.updateAndGet { (it - 1).coerceAtLeast(1) }
                lastRate = rate
                holdUntil = now + HOLD_MS
                return
            }
        }
        lastRate = rate
        val limit = speedLimit()
        val nearLimit = limit > 0L && rate >= limit * LIMIT_SATURATION
        if (now >= holdUntil && canGrow && !nearLimit && current.get() < ceiling) {
            current.incrementAndGet()
            probing = true
        }
    }

    private companion object {
        const val WINDOW_MS = 3000L
        const val HOLD_MS = 30_000L
        const val REQUIRED_GAIN = 0.07
        const val LIMIT_SATURATION = 0.9
    }
}
