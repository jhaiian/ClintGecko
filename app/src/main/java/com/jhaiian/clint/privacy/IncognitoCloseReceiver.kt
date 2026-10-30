package com.jhaiian.clint.privacy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class IncognitoCloseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != IncognitoNotification.ACTION_CLOSE_INCOGNITO) return
        IncognitoNotification.closeAll(context.applicationContext)
    }
}
