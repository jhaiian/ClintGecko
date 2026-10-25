package com.jhaiian.clint.downloads

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal object DownloadDispatchers {

    const val MAX_CONNECTIONS = 128
    private const val EXTRA_TRANSFER_THREADS = 32

    val network: CoroutineDispatcher =
        Dispatchers.IO.limitedParallelism(MAX_CONNECTIONS + EXTRA_TRANSFER_THREADS, "clint-downloads")

    val database: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1, "clint-download-db")
}
