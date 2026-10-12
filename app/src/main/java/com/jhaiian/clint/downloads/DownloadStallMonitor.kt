package com.jhaiian.clint.downloads

import android.content.Context
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal object DownloadStallMonitor {

    private const val CHECK_INTERVAL_MS = 10_000L
    private const val STALL_THRESHOLD_MS = 30_000L
    private const val RESUME_GRACE_MS = 1_500L
    private const val PAUSE_CONFIRM_TIMEOUT_MS = 20_000L

    private data class Progress(val bytes: Long, val sinceMs: Long)

    private val lastProgress: MutableMap<Int, Progress> = ConcurrentHashMap()
    private val recovering: MutableSet<Int> = ConcurrentHashMap.newKeySet()
    private var started = false

    fun register(context: Context) {
        if (started) return
        started = true
        val appContext = context.applicationContext
        ClintDownloadManager.applicationScope.launch {
            while (true) {
                delay(CHECK_INTERVAL_MS)
                checkForStalls(appContext)
            }
        }
    }

    private fun checkForStalls(context: Context) {
        val now = System.currentTimeMillis()
        val activeIds = mutableSetOf<Int>()

        for (item in ClintDownloadManager.downloadsFlow.value) {
            if (item.status != DownloadStatus.DOWNLOADING || !item.resumable || item.streamIsLive) continue
            activeIds += item.id
            if (item.id in recovering) continue

            val previous = lastProgress[item.id]
            if (previous == null || previous.bytes != item.bytesDownloaded) {
                lastProgress[item.id] = Progress(item.bytesDownloaded, now)
                continue
            }

            if (now - previous.sinceMs >= STALL_THRESHOLD_MS) {
                lastProgress[item.id] = Progress(item.bytesDownloaded, now)
                recoverStalledDownload(context, item.id)
            }
        }

        lastProgress.keys.retainAll(activeIds)
    }

    private fun recoverStalledDownload(context: Context, id: Int) {
        if (!recovering.add(id)) return
        ClintDownloadManager.applicationScope.launch {
            try {
                ClintDownloadManager.pause(context, id)
                val becamePaused = withTimeoutOrNull(PAUSE_CONFIRM_TIMEOUT_MS) {
                    ClintDownloadManager.downloadsFlow.first { list ->
                        list.find { it.id == id }?.status == DownloadStatus.PAUSED
                    }
                }
                if (becamePaused != null) {
                    delay(RESUME_GRACE_MS)
                    ClintDownloadManager.resume(context, id)
                }
            } finally {
                recovering.remove(id)
            }
        }
    }
}
