package com.jhaiian.clint.downloads

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DownloadBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {

            val pendingResult = goAsync()
            val job = ClintDownloadManager.init(context)
            job.invokeOnCompletion { pendingResult.finish() }
        }
    }
}
