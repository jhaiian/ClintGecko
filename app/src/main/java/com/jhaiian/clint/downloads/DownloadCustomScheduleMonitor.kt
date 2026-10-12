package com.jhaiian.clint.downloads

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

internal object DownloadCustomScheduleMonitor {

    private const val REQUEST_CODE_BASE = 80_000

    private fun pendingIntent(context: Context, id: Int): PendingIntent {
        val intent = Intent(context, DownloadCustomScheduleReceiver::class.java)
            .putExtra(DownloadCustomScheduleReceiver.EXTRA_ID, id)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun schedule(context: Context, id: Int, atMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = pendingIntent(context, id)
        if (canScheduleExact(context)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        } else {

            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        }
    }

    fun cancel(context: Context, id: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(pendingIntent(context, id))
    }

    fun rearmAll(context: Context) {
        ClintDownloadManager.downloadsFlow.value
            .filter { it.status == DownloadStatus.PAUSED && it.waitingForCustomSchedule && it.scheduledStartAtMillis > 0L }
            .forEach { item ->
                if (item.scheduledStartAtMillis <= System.currentTimeMillis()) {
                    ClintDownloadManager.resume(context, item.id)
                } else {
                    schedule(context, item.id, item.scheduledStartAtMillis)
                }
            }
    }
}

class DownloadCustomScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, -1)
        if (id == -1) return
        val pendingResult = goAsync()
        val job = ClintDownloadManager.init(context)
        job.invokeOnCompletion {
            ClintDownloadManager.resume(context, id)
            pendingResult.finish()
        }
    }

    companion object {
        const val EXTRA_ID = "id"
    }
}
