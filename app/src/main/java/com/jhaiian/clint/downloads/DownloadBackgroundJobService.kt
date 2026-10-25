package com.jhaiian.clint.downloads

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DownloadBackgroundJobService : JobService() {

    companion object {
        private const val JOB_ID = 74_211
        private const val IDLE_GRACE_MS = 1500L

        fun schedule(context: Context) {
            val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
            if (scheduler.getPendingJob(JOB_ID) != null) return
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, DownloadBackgroundJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .build()
            val scheduled = runCatching { scheduler.schedule(job) == JobScheduler.RESULT_SUCCESS }.getOrDefault(false)
            DownloadLog.i(DownloadLog.GLOBAL_ID, "SYSTEM") {
                if (scheduled) "Downloads will continue in a background job" else "Could not schedule a background download job"
            }
        }

        private fun hasActiveDownloads(): Boolean =
            ClintDownloadManager.downloadsFlow.value.any { it.status in DownloadStatus.ACTIVELY_WORKING }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartJob(params: JobParameters): Boolean {
        scope.launch {
            ClintDownloadManager.resumeSystemPaused(applicationContext)
            ClintDownloadManager.tryDequeueNext(applicationContext)
            while (true) {
                ClintDownloadManager.downloadsFlow.first { list -> list.none { it.status in DownloadStatus.ACTIVELY_WORKING } }
                delay(IDLE_GRACE_MS)
                if (!hasActiveDownloads()) break
            }
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        scope.coroutineContext.cancelChildren()
        val paused = ClintDownloadManager.downloadsFlow.value
            .filter { it.status in DownloadStatus.ACTIVELY_WORKING && it.resumable }
        DownloadLog.w(DownloadLog.GLOBAL_ID, "SYSTEM") { "Background download job stopped by the system; pausing ${paused.size} download(s) until it runs again" }
        paused.forEach {
            ClintDownloadManager.timeoutPausedIds.add(it.id)
            ClintDownloadManager.pause(applicationContext, it.id)
        }
        return paused.isNotEmpty()
    }

    override fun onDestroy() {
        scope.coroutineContext.cancelChildren()
        super.onDestroy()
    }
}
