package com.jhaiian.clint.crash

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jhaiian.clint.R
import com.jhaiian.clint.util.LocaleHelper
import org.mozilla.geckoview.GeckoRuntime

class GeckoCrashService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        runCatching { promoteToForeground() }
        if (intent?.action == GeckoRuntime.ACTION_CRASHED) {
            runCatching {
                GeckoCrashReports.handleNativeCrash(
                    applicationContext,
                    intent.getStringExtra(GeckoRuntime.EXTRA_MINIDUMP_PATH),
                    intent.getStringExtra(GeckoRuntime.EXTRA_EXTRAS_PATH),
                    intent.getStringExtra(GeckoRuntime.EXTRA_CRASH_PROCESS_VISIBILITY),
                    intent.getStringExtra(GeckoRuntime.EXTRA_CRASH_PROCESS_TYPE),
                    intent.getStringExtra(GeckoRuntime.EXTRA_CRASH_REMOTE_TYPE)
                )
            }.onFailure { android.util.Log.e(GeckoCrashReports.TAG, "Failed to save engine crash report", it) }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun promoteToForeground() {
        val localized = runCatching { LocaleHelper.wrapContext(this) }.getOrDefault(this)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                localized.getString(R.string.crash_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_24)
            .setContentTitle(localized.getString(R.string.crash_service_notification_title))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private companion object {
        const val CHANNEL_ID = "gecko_crash_reports"
        const val NOTIFICATION_ID = 7421
    }
}
