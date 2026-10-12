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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DownloadForegroundService : LifecycleService() {

    companion object {
        internal const val FOREGROUND_ID = 9001

        fun start(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    private var wakeLock: PowerManager.WakeLock? = null

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
        val notification = DownloadNotificationHelper.buildSummaryNotification(this, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                FOREGROUND_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(FOREGROUND_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(FOREGROUND_ID, notification)
        }

        val nm = getSystemService(NotificationManager::class.java)
        wakeLock?.let { if (!it.isHeld) it.acquire(6 * 60 * 60 * 1000L) }

        lifecycleScope.launch {
            ClintDownloadManager.downloadsFlow
                .map { list -> list.count { it.status in DownloadStatus.ACTIVELY_WORKING } }
                .distinctUntilChanged()
                .collectLatest { activeCount ->
                    if (activeCount > 0) {
                        nm.notify(FOREGROUND_ID, DownloadNotificationHelper.buildSummaryNotification(this@DownloadForegroundService, activeCount))
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
