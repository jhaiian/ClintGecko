package com.jhaiian.clint.privacy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R

object IncognitoNotification {

    const val ACTION_CLOSE_INCOGNITO = "com.jhaiian.clint.action.CLOSE_INCOGNITO_TABS"
    private const val CHANNEL_ID = "clint_incognito_tabs"
    private const val NOTIFICATION_ID = 0x1C06
    private const val REQUEST_CODE = 0x1C06

    private var shown: Boolean? = null
    var closeTabsHandler: (() -> Unit)? = null

    fun closeAll(context: Context) {
        val handler = closeTabsHandler
        if (handler != null) {
            handler()
        } else {
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(com.jhaiian.clint.browser.delegates.PREF_LAST_BROWSING_MODE_INCOGNITO, false)
                .apply()
            sync(context, false)
        }
    }

    fun sync(context: Context, hasIncognitoTabs: Boolean) {
        if (shown == hasIncognitoTabs) return
        if (hasIncognitoTabs) {
            if (show(context)) shown = true
        } else {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            shown = false
        }
    }

    private fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.incognito_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.incognito_notification_channel_desc)
            setSound(null, null)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun show(context: Context): Boolean {
        if (!canPost(context)) return false
        createChannel(context)
        val intent = Intent(context, IncognitoCloseReceiver::class.java).setAction(ACTION_CLOSE_INCOGNITO)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_24)
            .setContentTitle(context.getString(R.string.incognito_notification_title))
            .setContentText(context.getString(R.string.incognito_notification_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        return true
    }
}
