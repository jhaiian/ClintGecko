package com.jhaiian.clint.blocker.engine

import android.content.Context

object WebsiteBlockerWebIntegration {
    fun initialize(context: Context) {
        WebsiteBlockerEngine.activate(context)
    }

    fun onCompileComplete(context: Context) {
        WebsiteBlockerEngine.activate(context)
    }

    fun isHostBlocked(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        return WebsiteBlockerEngine.isBlocked(host)
    }
}
