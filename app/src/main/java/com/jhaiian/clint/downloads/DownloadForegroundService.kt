package com.jhaiian.clint.downloads

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.jhaiian.clint.R
import com.jhaiian.clint.util.LocaleHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DownloadForegroundService : LifecycleService() {

    companion object {
        internal const val FOREGROUND_ID = 9001
        private const val WAKE_LOCK_TIMEOUT_MS = 6 * 60 * 60 * 1000L
        private const val LONG_RETRY_WAIT_SEC = 60

        private fun needsWakeLock(item: DownloadItem): Boolean =
            item.status in DownloadStatus.ACTIVELY_WORKING &&
                !(item.status == DownloadStatus.RETRYING && item.retryDelaySec >= LONG_RETRY_WAIT_SEC)

        private val DOWNLOAD_PHASE = setOf(
            DownloadStatus.CONNECTING, DownloadStatus.ALLOCATING, DownloadStatus.DOWNLOADING, DownloadStatus.RETRYING
        )

        private val PROCESS_PHASE = setOf(
            DownloadStatus.DECRYPTING, DownloadStatus.MUXING, DownloadStatus.CONVERTING,
            DownloadStatus.COPYING_TEMP, DownloadStatus.DELETING_TEMP
        )

        fun start(context: Context) {
            if (DownloadTransferJobService.ensureRunning(context)) return
            val started = runCatching {
                val intent = Intent(context, DownloadForegroundService::class.java)
                androidx.core.content.ContextCompat.startForegroundService(context, intent)
            }.isSuccess
            if (!started) DownloadBackgroundJobService.schedule(context)
        }

        private fun typeFor(items: List<DownloadItem>): Int {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return 0
            val dataSync = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return dataSync
            val downloading = items.any { it.status in DOWNLOAD_PHASE }
            val processing = items.any { it.status in PROCESS_PHASE }
            return when {
                downloading && processing -> dataSync or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
                processing -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
                else -> dataSync
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var currentType = -1
    private var stateJob: Job? = null

    private fun promote(notification: Notification, type: Int): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(FOREGROUND_ID, notification, type)
        } else {
            startForeground(FOREGROUND_ID, notification)
        }
        currentType = type
    }.isSuccess

    override fun onTimeout(startId: Int, fgsType: Int) {
        val items = ClintDownloadManager.downloadsFlow.value
        items.filter { it.status in DOWNLOAD_PHASE && it.resumable }.forEach {
            ClintDownloadManager.timeoutPausedIds.add(it.id)
            ClintDownloadManager.pause(applicationContext, it.id)
        }
        val keepProcessing = items.any { it.status in PROCESS_PHASE } &&
            fgsType and ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC != 0
        stopSelf()
        if (keepProcessing) start(applicationContext)
        if (ClintDownloadManager.timeoutPausedIds.isNotEmpty()) DownloadBackgroundJobService.schedule(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        ClintDownloadManager.createNotificationChannel(this)
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Clint:downloadConvert").apply {
            setReferenceCounted(false)
        }
    }

    override fun onDestroy() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val notification = DownloadNotificationHelper.buildSummaryNotification(this, ClintDownloadManager.activeCount())
        val initialType = typeFor(ClintDownloadManager.downloadsFlow.value)
        if (!promote(notification, initialType) && !promote(notification, typeFor(emptyList()))) {
            DownloadBackgroundJobService.schedule(applicationContext)
            stopSelf()
            return START_NOT_STICKY
        }

        val nm = getSystemService(NotificationManager::class.java)
        wakeLock?.let { if (!it.isHeld) it.acquire(WAKE_LOCK_TIMEOUT_MS) }

        if (stateJob?.isActive == true) return START_STICKY
        stateJob = lifecycleScope.launch {
            ClintDownloadManager.init(applicationContext).join()
            ClintDownloadManager.downloadsFlow
                .map { list ->
                    Triple(list.count { it.status in DownloadStatus.ACTIVELY_WORKING }, typeFor(list), list.any { needsWakeLock(it) })
                }
                .distinctUntilChanged()
                .collectLatest { (activeCount, type, holdWakeLock) ->
                    if (activeCount > 0) {
                        val lock = wakeLock
                        if (lock != null) {
                            if (holdWakeLock && !lock.isHeld) lock.acquire(WAKE_LOCK_TIMEOUT_MS)
                            if (!holdWakeLock && lock.isHeld) lock.release()
                        }
                        val summary = DownloadNotificationHelper.buildSummaryNotification(this@DownloadForegroundService, activeCount)
                        if (type != currentType) promote(summary, type)
                        nm.notify(FOREGROUND_ID, summary)
                    } else {
                        wakeLock?.let { if (it.isHeld) it.release() }
                        delay(1000)
                        stopSelf()
                    }
                }
        }

        return START_STICKY
    }
}
