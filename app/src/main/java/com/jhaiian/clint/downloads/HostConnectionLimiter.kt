package com.jhaiian.clint.downloads

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

internal object HostConnectionLimiter {

    private const val MAX_CONNECTIONS_PER_HOST = 32

    private val semaphores = ConcurrentHashMap<String, Semaphore>()
    private val global = Semaphore(DownloadDispatchers.MAX_CONNECTIONS)

    suspend fun <T> withPermit(host: String, block: suspend () -> T): T {
        if (host.isBlank()) return global.withPermit { block() }
        val perHost = semaphores.getOrPut(host.lowercase()) { Semaphore(MAX_CONNECTIONS_PER_HOST) }
        return perHost.withPermit { global.withPermit { block() } }
    }
}
