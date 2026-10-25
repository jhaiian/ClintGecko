package com.jhaiian.clint.downloads

import java.io.IOException
import java.io.InputStream

internal class BatchedReader(private val input: InputStream, val buffer: ByteArray) {

    private var deferred: IOException? = null

    fun fill(limit: Int): Int {
        deferred?.let {
            deferred = null
            throw it
        }
        val max = limit.coerceIn(1, buffer.size)
        val deadline = System.nanoTime() + FLUSH_INTERVAL_NANOS
        var filled = 0
        while (filled < max) {
            val read = try {
                input.read(buffer, filled, max - filled)
            } catch (e: IOException) {
                if (filled == 0) throw e
                deferred = e
                return filled
            }
            if (read < 0) return if (filled == 0) -1 else filled
            filled += read
            if (System.nanoTime() - deadline >= 0L) break
        }
        return filled
    }

    private companion object {
        const val FLUSH_INTERVAL_NANOS = 50_000_000L
    }
}
