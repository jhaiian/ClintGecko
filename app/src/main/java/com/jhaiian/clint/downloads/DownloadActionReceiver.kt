package com.jhaiian.clint.downloads

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DownloadActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PAUSE  = "com.jhaiian.clint.DOWNLOAD_PAUSE"
        const val ACTION_RESUME = "com.jhaiian.clint.DOWNLOAD_RESUME"
        const val ACTION_CANCEL = "com.jhaiian.clint.DOWNLOAD_CANCEL"
        const val EXTRA_ID = "download_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, -1)
        if (id == -1) return
        val action = intent.action
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        ClintDownloadManager.init(appContext).invokeOnCompletion {
            try {
                when (action) {
                    ACTION_PAUSE  -> ClintDownloadManager.pauseByUser(appContext, id)
                    ACTION_RESUME -> ClintDownloadManager.resume(appContext, id)
                    ACTION_CANCEL -> ClintDownloadManager.cancel(appContext, id)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
