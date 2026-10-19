package com.jhaiian.clint.downloads

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object DownloadSpeedHistory {

    const val CAPACITY = 60
    private const val SAMPLE_INTERVAL_MS = 1000L

    private val _perItem = MutableStateFlow<Map<Int, List<Long>>>(emptyMap())
    val perItem: StateFlow<Map<Int, List<Long>>> = _perItem.asStateFlow()

    private val _total = MutableStateFlow<List<Long>>(emptyList())
    val total: StateFlow<List<Long>> = _total.asStateFlow()

    private var job: Job? = null

    private val TRANSFERRING = setOf(DownloadStatus.DOWNLOADING, DownloadStatus.CONNECTING, DownloadStatus.RETRYING)

    internal fun start(scope: CoroutineScope, source: StateFlow<List<DownloadItem>>) {
        if (job != null) return
        job = scope.launch {
            source
                .map { list -> list.any { it.status in TRANSFERRING } }
                .distinctUntilChanged()
                .collectLatest { active ->
                    if (!active) {
                        val alive = source.value.filter { it.status in DownloadStatus.NOT_FINISHED }.map { it.id }.toSet()
                        _perItem.update { previous -> previous.filterKeys { it in alive } }
                        return@collectLatest
                    }
                    while (true) {
                        record(source.value)
                        delay(SAMPLE_INTERVAL_MS)
                    }
                }
        }
    }

    private fun record(items: List<DownloadItem>) {
        val working = items.filter { it.status in TRANSFERRING }
        val alive = items.filter { it.status in DownloadStatus.NOT_FINISHED }.map { it.id }.toSet()
        _perItem.update { previous ->
            val next = previous.filterKeys { it in alive }.toMutableMap()
            working.forEach { item ->
                val speed = if (item.status == DownloadStatus.DOWNLOADING) item.speedBytesPerSec else 0L
                next[item.id] = ((previous[item.id] ?: emptyList()) + speed).takeLast(CAPACITY)
            }
            next
        }
        val totalSpeed = working.sumOf { if (it.status == DownloadStatus.DOWNLOADING) it.speedBytesPerSec else 0L }
        _total.update { (it + totalSpeed).takeLast(CAPACITY) }
    }
}
