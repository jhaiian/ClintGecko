package com.jhaiian.clint.downloads

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

internal object HostConnectionLimiter {

    private const val MAX_CONNECTIONS_PER_HOST = 32

    private val semaphores = ConcurrentHashMap<String, Semaphore>()

    suspend fun <T> withPermit(host: String, block: suspend () -> T): T {
        if (host.isBlank()) return block()
        return semaphores.getOrPut(host.lowercase()) { Semaphore(MAX_CONNECTIONS_PER_HOST) }.withPermit { block() }
    }
}
