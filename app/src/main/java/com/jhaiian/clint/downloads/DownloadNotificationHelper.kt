package com.jhaiian.clint.downloads

import com.jhaiian.clint.util.formatFileSize

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal object DownloadNotificationHelper {

    const val DOWNLOAD_GROUP_KEY = "com.jhaiian.clint.downloads.GROUP"
    private const val COMPLETE_TAG = "com.jhaiian.clint.downloads.COMPLETE"
    private const val PROGRESS_NOTIFY_INTERVAL_MS = 1000L
    private const val GLOBAL_WINDOW_MS = 1000L
    private const val GLOBAL_PROGRESS_POSTS_PER_WINDOW = 4
    private const val REPOST_DELAY_MS = 1500L

    private val lastProgressPostAt = ConcurrentHashMap<Int, Long>()
    private val recentPosts = ArrayDeque<Long>()

    private fun recordPost(enforceLimit: Boolean): Boolean = synchronized(recentPosts) {
        val now = SystemClock.elapsedRealtime()
        while (recentPosts.isNotEmpty() && now - recentPosts.first() >= GLOBAL_WINDOW_MS) recentPosts.removeFirst()
        val withinBudget = recentPosts.size < GLOBAL_PROGRESS_POSTS_PER_WINDOW
        if (withinBudget || !enforceLimit) recentPosts.addLast(now)
        withinBudget
    }

    private fun takeGlobalSlot(force: Boolean): Boolean = recordPost(enforceLimit = !force)

    private fun stateKey(item: DownloadItem): String =
        "${item.status}|${item.waitingForNetwork}|${item.waitingForUnmetered}|${item.waitingForSchedule}|${item.waitingForCustomSchedule}"

    private fun isStillWorking(id: Int): Boolean {
        val status = ClintDownloadManager.downloadsFlow.value.firstOrNull { it.id == id }?.status ?: return false
        return status in DownloadStatus.ACTIVELY_WORKING
    }

    private fun shouldPostProgress(id: Int): Boolean {
        val now = SystemClock.elapsedRealtime()
        val last = lastProgressPostAt[id]
        if (last != null && now - last < PROGRESS_NOTIFY_INTERVAL_MS) return false
        if (!isStillWorking(id)) return false
        if (!takeGlobalSlot(force = false)) return false
        lastProgressPostAt[id] = now
        return true
    }

    fun showProgressNotificationThrottled(context: Context, item: DownloadItem) {
        if (shouldPostProgress(item.id)) postProgressNotification(context, item)
    }

    fun showCopyingTempNotificationThrottled(context: Context, item: DownloadItem) {
        if (shouldPostProgress(item.id)) postCopyingTempNotification(context, item)
    }

    private fun notifyReplacing(nm: NotificationManager, item: DownloadItem, notification: Notification) {
        val calm = takeGlobalSlot(force = true)
        nm.cancel(item.id)
        nm.notify(item.id, notification)
        if (calm) return
        val expected = stateKey(item)
        ClintDownloadManager.applicationScope.launch {
            delay(REPOST_DELAY_MS)
            val latest = ClintDownloadManager.downloadsFlow.value.firstOrNull { it.id == item.id } ?: return@launch
            if (!ClintDownloadManager.isRemoved(item.id) && stateKey(latest) == expected) {
                takeGlobalSlot(force = true)
                nm.notify(item.id, notification)
            }
        }
    }

    fun cancelCompleteNotification(context: Context, id: Int) {
        context.getSystemService(NotificationManager::class.java).cancel(COMPLETE_TAG, id)
    }

    fun createNotificationChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val progressChannel = NotificationChannel(
            ClintDownloadManager.CHANNEL_ID,
            context.getString(R.string.download_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.download_notification_channel_desc)
            setSound(null, null)
        }
        nm.createNotificationChannel(progressChannel)
        val eventChannel = NotificationChannel(
            ClintDownloadManager.EVENT_CHANNEL_ID,
            context.getString(R.string.download_event_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.download_event_notification_channel_desc)
        }
        nm.createNotificationChannel(eventChannel)
    }

    fun buildSummaryNotification(context: Context, activeCount: Int): Notification {
        val downloadsIntent = Intent(context, DownloadsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val downloadsPi = PendingIntent.getActivity(
            context, 0, downloadsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val text = if (activeCount > 0)
            context.resources.getQuantityString(R.plurals.download_notification_group_summary, activeCount, activeCount)
        else
            context.getString(R.string.download_foreground_notification_text)
        return NotificationCompat.Builder(context, ClintDownloadManager.CHANNEL_ID)
            .setContentTitle(context.getString(R.string.download_foreground_notification_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification_24)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(downloadsPi)
            .setGroup(DOWNLOAD_GROUP_KEY)
            .setGroupSummary(true)
            .build()
    }

    fun showQueuedNotification(context: Context, item: DownloadItem) {
        if (ClintDownloadManager.isRemoved(item.id)) return
        takeGlobalSlot(force = true)
        val nm = context.getSystemService(NotificationManager::class.java)
        NotificationCompat.Builder(context, ClintDownloadManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_24)
            .setContentTitle(item.filename)
            .setContentText(context.getString(R.string.download_status_queued))
            .setGroup(DOWNLOAD_GROUP_KEY)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setProgress(0, 0, false)
            .addAction(0, context.getString(R.string.action_pause), pausePendingIntent(context, item.id))
            .build()
            .let { nm.notify(item.id, it) }
    }

    fun showProgressNotification(context: Context, item: DownloadItem) {
        if (ClintDownloadManager.isRemoved(item.id)) return
        takeGlobalSlot(force = true)
        postProgressNotification(context, item)
    }

    private fun postProgressNotification(context: Context, item: DownloadItem) {
        if (ClintDownloadManager.isRemoved(item.id)) return
        lastProgressPostAt[item.id] = SystemClock.elapsedRealtime()
        val nm = context.getSystemService(NotificationManager::class.java)

        val statusText: String
        val metaText: String?
        val indeterminate: Boolean
        val progress: Int
        val showPause: Boolean

        when (item.status) {
            DownloadStatus.CONNECTING -> {
                val sizeStr = if (item.totalBytes > 0) formatFileSize(item.totalBytes) else null
                statusText = sizeStr ?: context.getString(R.string.download_status_connecting)
                metaText = if (sizeStr != null) context.getString(R.string.download_status_connecting) else null
                indeterminate = true
                progress = 0
                showPause = true
            }
            DownloadStatus.RETRYING -> {
                val pct = item.progressPercent
                val sizeStr = when {
                    item.bytesDownloaded > 0 && pct >= 0 && item.totalBytes > 0 ->
                        context.getString(R.string.download_status_progress, pct, formatFileSize(item.bytesDownloaded), formatFileSize(item.totalBytes))
                    item.bytesDownloaded > 0 && pct >= 0 ->
                        context.getString(R.string.download_status_progress_unknown_total, pct, formatFileSize(item.bytesDownloaded))
                    item.bytesDownloaded > 0 ->
                        context.getString(R.string.download_status_progress_indeterminate, formatFileSize(item.bytesDownloaded))
                    item.totalBytes > 0 -> formatFileSize(item.totalBytes)
                    else -> null
                }
                val retryStr = if (item.retryDelaySec > 0)
                    context.getString(R.string.download_status_retrying_in, item.retryDelaySec)
                else
                    context.getString(R.string.download_status_retrying)
                statusText = sizeStr ?: retryStr
                metaText = if (sizeStr != null) retryStr else null
                indeterminate = true
                progress = 0
                showPause = true
            }
            DownloadStatus.DECRYPTING -> {
                val total = item.segmentsTotal.coerceAtLeast(1)
                statusText = context.getString(R.string.download_status_decrypting)
                metaText = context.getString(R.string.download_status_segment_progress, item.segmentsCompleted, item.segmentsTotal)
                indeterminate = false
                progress = item.segmentsCompleted * 100 / total
                showPause = false
            }
            DownloadStatus.MUXING -> {
                statusText = muxStatusLabel(context, item)
                metaText = null
                val muxFraction = muxProgressFraction(item)
                indeterminate = muxFraction == null
                progress = ((muxFraction ?: 0f) * 100).toInt()
                showPause = false
            }
            DownloadStatus.CONVERTING -> {
                val pct = item.muxProgress
                statusText = context.getString(R.string.download_status_converting, pct)
                metaText = null
                indeterminate = pct <= 0
                progress = pct
                showPause = false
            }
            else -> {
                val pct = item.progressPercent
                val downloaded = formatFileSize(item.bytesDownloaded)
                statusText = if (pct >= 0) {
                    if (item.totalBytes > 0)
                        context.getString(R.string.download_status_progress, pct, downloaded, formatFileSize(item.totalBytes))
                    else
                        context.getString(R.string.download_status_progress_unknown_total, pct, downloaded)
                } else {
                    context.getString(R.string.download_status_progress_indeterminate, downloaded)
                }
                val speedEtaText = buildSpeedEtaText(context, item)
                val segmentText = if (item.segmentsTotal > 0) context.getString(R.string.download_status_segment_progress, item.segmentsCompleted, item.segmentsTotal) else null
                val combinedMeta = listOfNotNull(streamStageLabel(context, item), segmentText, speedEtaText)
                metaText = if (combinedMeta.isEmpty()) null else combinedMeta.joinToString("  \u2022  ")
                indeterminate = pct < 0
                progress = pct.coerceAtLeast(0)
                showPause = item.resumable
            }
        }

        val downloadsIntent = Intent(context, DownloadsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val downloadsPi = PendingIntent.getActivity(
            context, item.id + 50000, downloadsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (metaText != null) "$statusText  \u2022  $metaText" else statusText
        val builder = NotificationCompat.Builder(context, ClintDownloadManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_24)
            .setContentTitle(item.filename)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setProgress(100, progress, indeterminate)
            .setGroup(DOWNLOAD_GROUP_KEY)
            .setContentIntent(downloadsPi)

        if (showPause) {
            builder.addAction(0, context.getString(R.string.action_pause), pausePendingIntent(context, item.id))
        }

        nm.notify(item.id, builder.build())
    }

    fun showAllocationNotification(context: Context, item: DownloadItem) {
        if (ClintDownloadManager.isRemoved(item.id)) return
        takeGlobalSlot(force = true)
        val nm = context.getSystemService(NotificationManager::class.java)
        val pct = item.allocationProgress
        val allocStr = context.getString(R.string.download_status_allocating, pct)
        val contentText = if (item.totalBytes > 0)
            "${formatFileSize(item.totalBytes)}  \u2022  $allocStr"
        else
            allocStr
        NotificationCompat.Builder(context, ClintDownloadManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_24)
            .setContentTitle(item.filename)
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setProgress(100, pct, pct == 0)
            .setGroup(DOWNLOAD_GROUP_KEY)
            .build()
            .let { nm.notify(item.id, it) }
    }

    fun showCopyingTempNotification(context: Context, item: DownloadItem) {
        if (ClintDownloadManager.isRemoved(item.id)) return
        takeGlobalSlot(force = true)
        postCopyingTempNotification(context, item)
    }

    private fun postCopyingTempNotification(context: Context, item: DownloadItem) {
        if (ClintDownloadManager.isRemoved(item.id)) return
        lastProgressPostAt[item.id] = SystemClock.elapsedRealtime()
        val nm = context.getSystemService(NotificationManager::class.java)
        val progress = item.copyProgress
        NotificationCompat.Builder(context, ClintDownloadManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_24)
            .setContentTitle(item.filename)
            .setContentText(context.getString(R.string.download_copying_temp_notification, progress))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setProgress(100, progress, progress == 0)
            .setGroup(DOWNLOAD_GROUP_KEY)
            .build()
            .let { nm.notify(item.id, it) }
    }

    fun showDeletingTempNotification(context: Context, item: DownloadItem) {
        if (ClintDownloadManager.isRemoved(item.id)) return
        takeGlobalSlot(force = true)
        val nm = context.getSystemService(NotificationManager::class.java)
        NotificationCompat.Builder(context, ClintDownloadManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_24)
            .setContentTitle(item.filename)
            .setContentText(context.getString(R.string.download_deleting_temp_notification))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setProgress(0, 0, true)
            .setGroup(DOWNLOAD_GROUP_KEY)
            .build()
            .let { nm.notify(item.id, it) }
    }

    fun showPausedNotification(context: Context, item: DownloadItem) {
        showWaitingStateNotification(
            context, item,
            label = context.getString(R.string.download_notification_paused),
            includeElapsed = true,
            actionLabel = context.getString(R.string.action_resume),
            actionPendingIntent = resumePendingIntent(context, item.id)
        )
    }

    fun showWaitingUnmeteredNotification(context: Context, item: DownloadItem) {
        showWaitingStateNotification(
            context, item,
            label = context.getString(R.string.download_notification_waiting_unmetered),
            includeElapsed = true,
            actionLabel = context.getString(R.string.action_pause),
            actionPendingIntent = pausePendingIntent(context, item.id)
        )
    }

    fun showWaitingNetworkNotification(context: Context, item: DownloadItem) {
        showWaitingStateNotification(
            context, item,
            label = context.getString(R.string.download_notification_waiting_network),
            includeElapsed = true,
            actionLabel = context.getString(R.string.action_pause),
            actionPendingIntent = pausePendingIntent(context, item.id)
        )
    }

    fun showWaitingScheduleNotification(context: Context, item: DownloadItem) {
        showWaitingStateNotification(
            context, item,
            label = context.getString(R.string.download_notification_waiting_schedule),
            includeElapsed = true,
            actionLabel = context.getString(R.string.action_pause),
            actionPendingIntent = pausePendingIntent(context, item.id)
        )
    }

    fun showWaitingCustomScheduleNotification(context: Context, item: DownloadItem) {
        showWaitingStateNotification(
            context, item,
            label = context.getString(
                R.string.download_notification_waiting_custom_schedule,
                formatScheduledDateTime(context, item.scheduledStartAtMillis)
            ),
            includeElapsed = false,
            actionLabel = context.getString(R.string.action_pause),
            actionPendingIntent = pausePendingIntent(context, item.id)
        )
    }

    private fun showWaitingStateNotification(
        context: Context,
        item: DownloadItem,
        label: String,
        includeElapsed: Boolean,
        actionLabel: String,
        actionPendingIntent: PendingIntent
    ) {
        if (ClintDownloadManager.isRemoved(item.id)) return
        val nm = context.getSystemService(NotificationManager::class.java)
        val pct = item.progressPercent
        val progressText = buildPausedProgressText(context, item)
        val metaLabel = if (includeElapsed) {
            val elapsedSec = item.activeElapsedMs / 1000L
            if (elapsedSec >= 1L) "$label  \u2022  ${formatElapsed(elapsedSec)}" else label
        } else label
        val contentText = if (progressText != null) "$progressText  \u2022  $metaLabel" else metaLabel

        NotificationCompat.Builder(context, ClintDownloadManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_24)
            .setGroup(DOWNLOAD_GROUP_KEY)
            .setContentTitle(item.filename)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setProgress(100, pct.coerceAtLeast(0), false)
            .addAction(0, actionLabel, actionPendingIntent)
            .build()
            .let { notifyReplacing(nm, item, it) }
    }

    fun showCompleteNotification(context: Context, item: DownloadItem) {
        lastProgressPostAt.remove(item.id)
        val nm = context.getSystemService(NotificationManager::class.java)
        if (item.file == null && item.contentUri == null) {
            nm.cancel(item.id)
            return
        }

        nm.cancel(item.id)

        if (!isPushEnabled(context)) return

        val openIntent = Intent(context, DownloadsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(DownloadsActivity.EXTRA_OPEN_ID, item.id)
        }
        val openPi = PendingIntent.getActivity(
            context, item.id + 10000, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val downloadsIntent = Intent(context, DownloadsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val downloadsPi = PendingIntent.getActivity(
            context, item.id + 20000, downloadsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        NotificationCompat.Builder(context, ClintDownloadManager.EVENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_24)
            .setContentTitle(item.filename)
            .setContentText(context.getString(R.string.download_notification_complete))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(downloadsPi)
            .addAction(0, context.getString(R.string.action_open), openPi)
            .build()
            .let {
                takeGlobalSlot(force = true)
                nm.notify(COMPLETE_TAG, item.id, it)
            }
    }

    fun showFailedNotification(context: Context, item: DownloadItem) {
        lastProgressPostAt.remove(item.id)
        val nm = context.getSystemService(NotificationManager::class.java)
        if (!isPushEnabled(context)) {
            nm.cancel(item.id)
            return
        }
        val downloadsIntent = Intent(context, DownloadsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val downloadsPi = PendingIntent.getActivity(
            context, item.id + 30000, downloadsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        NotificationCompat.Builder(context, ClintDownloadManager.EVENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_24)
            .setContentTitle(item.filename)
            .setContentText(context.getString(R.string.download_notification_failed))
            .setGroup(DOWNLOAD_GROUP_KEY)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(downloadsPi)
            .build()
            .let { notifyReplacing(nm, item, it) }
    }

    fun showRetryingNotification(context: Context, item: DownloadItem) {
        lastProgressPostAt.remove(item.id)
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.cancel(item.id)
        if (!isPushEnabled(context)) return
        val downloadsIntent = Intent(context, DownloadsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val downloadsPi = PendingIntent.getActivity(
            context, item.id + 60000, downloadsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val retryText = if (item.retryDelaySec > 0)
            context.getString(R.string.download_notification_retrying_in, item.retryDelaySec)
        else
            context.getString(R.string.download_notification_retrying)
        NotificationCompat.Builder(context, ClintDownloadManager.EVENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_24)
            .setContentTitle(item.filename)
            .setContentText(retryText)
            .setGroup(DOWNLOAD_GROUP_KEY)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(downloadsPi)
            .build()
            .let {
                takeGlobalSlot(force = true)
                nm.notify(item.id, it)
            }
    }

    private fun isPushEnabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(DownloadSettingsKeys.PREF_PUSH_NOTIFICATIONS, DownloadSettingsKeys.DEFAULT_PUSH_NOTIFICATIONS)

    private fun pausePendingIntent(context: Context, id: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, id + 40000,
            Intent(context, DownloadActionReceiver::class.java).apply {
                action = DownloadActionReceiver.ACTION_PAUSE
                putExtra(DownloadActionReceiver.EXTRA_ID, id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun resumePendingIntent(context: Context, id: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, id + 50000,
            Intent(context, DownloadActionReceiver::class.java).apply {
                action = DownloadActionReceiver.ACTION_RESUME
                putExtra(DownloadActionReceiver.EXTRA_ID, id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun buildPausedProgressText(context: Context, item: DownloadItem): String? {
        val pct = item.progressPercent
        return when {
            pct >= 0 && item.totalBytes > 0 ->
                context.getString(R.string.download_status_progress, pct, formatFileSize(item.bytesDownloaded), formatFileSize(item.totalBytes))
            pct >= 0 ->
                context.getString(R.string.download_status_progress_unknown_total, pct, formatFileSize(item.bytesDownloaded))
            item.bytesDownloaded > 0 ->
                context.getString(R.string.download_status_progress_indeterminate, formatFileSize(item.bytesDownloaded))
            else -> null
        }
    }

    private fun buildSpeedEtaText(context: Context, item: DownloadItem): String? {
        val speed = item.speedBytesPerSec
        val elapsedMs = item.activeElapsedMs +
                if (item.activeStartedAt > 0L) System.currentTimeMillis() - item.activeStartedAt else 0L
        val elapsedStr = if (elapsedMs >= 1000L) formatElapsed(elapsedMs / 1000L) else null
        if (speed <= 0L) return elapsedStr
        val remaining = item.totalBytes - item.bytesDownloaded
        val speedEta = if (item.totalBytes <= 0L || remaining <= 0L)
            context.getString(R.string.download_speed_only, formatFileSize(speed))
        else {

            val etaSpeed = item.averageSpeedBytesPerSec().takeIf { it > 0L } ?: speed
            context.getString(R.string.download_speed_eta, formatFileSize(speed), formatEta(context, remaining / etaSpeed))
        }
        return if (elapsedStr != null) "$speedEta  \u2022  $elapsedStr" else speedEta
    }

    private fun formatElapsed(seconds: Long): String = when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }

    private fun formatEta(context: Context, seconds: Long): String = when {
        seconds < 60 -> context.getString(R.string.download_eta_seconds, seconds)
        seconds < 3600 -> context.getString(R.string.download_eta_minutes, seconds / 60, seconds % 60)
        else -> context.getString(R.string.download_eta_hours, seconds / 3600, (seconds % 3600) / 60)
    }

}
