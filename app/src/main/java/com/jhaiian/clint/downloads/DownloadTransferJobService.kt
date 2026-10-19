package com.jhaiian.clint.downloads

import android.app.ActivityManager
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DownloadTransferJobService : JobService() {

    companion object {
        private const val JOB_ID = 74_210

        @Volatile
        private var running = false

        fun ensureRunning(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
            if (running) return true
            val scheduler = context.getSystemService(JobScheduler::class.java) ?: return false
            if (scheduler.getPendingJob(JOB_ID) != null) return true
            if (!isAppVisible()) return false
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, DownloadTransferJobService::class.java))
                .setUserInitiated(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build()
            val scheduled = runCatching { scheduler.schedule(job) == JobScheduler.RESULT_SUCCESS }.getOrDefault(false)
            DownloadLog.i(DownloadLog.GLOBAL_ID, "SYSTEM") {
                if (scheduled) "Running downloads as a user-initiated data transfer job" else "Could not schedule a data transfer job; using the foreground service"
            }
            return scheduled
        }

        private fun isAppVisible(): Boolean = runCatching {
            val info = ActivityManager.RunningAppProcessInfo()
            ActivityManager.getMyMemoryState(info)
            info.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
        }.getOrDefault(false)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartJob(params: JobParameters): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        running = true
        ClintDownloadManager.createNotificationChannel(this)
        val initialCount = ClintDownloadManager.downloadsFlow.value.count { it.status in DownloadStatus.ACTIVELY_WORKING }
        setNotification(
            params,
            DownloadForegroundService.FOREGROUND_ID,
            DownloadNotificationHelper.buildSummaryNotification(this, initialCount),
            JOB_END_NOTIFICATION_POLICY_REMOVE
        )
        val nm = getSystemService(NotificationManager::class.java)
        scope.launch {
            ClintDownloadManager.downloadsFlow
                .map { list -> list.count { it.status in DownloadStatus.ACTIVELY_WORKING } }
                .distinctUntilChanged()
                .collectLatest { count ->
                    if (count > 0) {
                        nm?.notify(DownloadForegroundService.FOREGROUND_ID, DownloadNotificationHelper.buildSummaryNotification(this@DownloadTransferJobService, count))
                    } else {
                        delay(1500L)
                        running = false
                        jobFinished(params, false)
                    }
                }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        running = false
        scope.coroutineContext.cancelChildren()
        val reason = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) params.stopReason else -1
        DownloadLog.w(DownloadLog.GLOBAL_ID, "SYSTEM") { "Data transfer job stopped by the system (reason $reason); pausing resumable downloads" }
        ClintDownloadManager.downloadsFlow.value
            .filter { it.status in DownloadStatus.ACTIVELY_WORKING && it.resumable }
            .forEach {
                ClintDownloadManager.timeoutPausedIds.add(it.id)
                ClintDownloadManager.pause(applicationContext, it.id)
            }
        return false
    }

    override fun onDestroy() {
        running = false
        scope.coroutineContext.cancelChildren()
        super.onDestroy()
    }
}
