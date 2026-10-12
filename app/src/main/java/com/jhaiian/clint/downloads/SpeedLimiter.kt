package com.jhaiian.clint.downloads

class SpeedLimiter(initialLimitBytesPerSec: Long) {

    @Volatile
    private var limitBytesPerSec: Long = initialLimitBytesPerSec

    private var availableTokens = 0.0
    private var lastRefillNanos = System.nanoTime()
    private val lock = Any()

    fun updateLimit(newLimitBytesPerSec: Long) {
        synchronized(lock) {
            limitBytesPerSec = newLimitBytesPerSec
            if (newLimitBytesPerSec > 0L) {
                availableTokens = minOf(availableTokens, newLimitBytesPerSec.toDouble())
            }
        }
    }

    fun acquire(bytes: Int) {
        if (bytes <= 0) return
        var remaining = bytes.toDouble()
        while (remaining > 0.0) {
            val waitMs: Long
            synchronized(lock) {
                val limit = limitBytesPerSec
                if (limit <= 0L) return
                val now = System.nanoTime()
                val elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0
                lastRefillNanos = now
                availableTokens = minOf(limit.toDouble(), availableTokens + elapsedSeconds * limit)
                if (availableTokens >= remaining) {
                    availableTokens -= remaining
                    remaining = 0.0
                    waitMs = 0L
                } else {
                    remaining -= availableTokens
                    availableTokens = 0.0
                    waitMs = ((remaining / limit) * 1000.0).toLong().coerceAtLeast(1L)
                }
            }
            if (waitMs > 0L) Thread.sleep(waitMs)
        }
    }
}
