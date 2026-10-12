package com.jhaiian.clint.downloads

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import java.util.Date

internal fun formatScheduledDateTime(context: Context, millis: Long): String {
    val date = Date(millis)
    val datePart = android.text.format.DateFormat.getMediumDateFormat(context).format(date)
    val timePart = android.text.format.DateFormat.getTimeFormat(context).format(date)
    return "$datePart, $timePart"
}

internal fun needsExactAlarmPermissionRationale(context: Context): Boolean =
    !DownloadCustomScheduleMonitor.canScheduleExact(context)

internal fun exactAlarmPermissionDialogConfig(context: Context): ConfirmDialogConfig = ConfirmDialogConfig(
    title = context.getString(R.string.download_schedule_exact_alarm_title),
    message = context.getString(R.string.download_schedule_exact_alarm_message, context.getString(R.string.app_name)),
    positiveLabel = context.getString(R.string.action_allow),
    onPositive = {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    },
    negativeLabel = context.getString(R.string.action_not_now)
)
