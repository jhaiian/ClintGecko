package com.jhaiian.clint.settings.datasaver

import android.content.Context
import android.net.Uri
import android.webkit.JavascriptInterface
import com.jhaiian.clint.settings.sitepermissions.SitePermissionDatabase
import com.jhaiian.clint.settings.sitepermissions.SitePermissionManager
import com.jhaiian.clint.util.registeredDomain
import java.util.concurrent.ConcurrentHashMap

object DataSaverSiteException {

    private val cache = ConcurrentHashMap<String, Boolean>()

    fun isExcepted(context: Context, host: String?): Boolean {
        if (host.isNullOrEmpty()) return false
        val key = registeredDomain(host)
        return cache.getOrPut(key) {
            SitePermissionManager.getState(
                context.applicationContext, key, SitePermissionDatabase.TYPE_DATA_SAVER_EXCEPTION
            ) != null
        }
    }

    fun isExceptedUrl(context: Context, url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        val host = runCatching { Uri.parse(url).host }.getOrNull()
        return isExcepted(context, host)
    }

    fun add(context: Context, host: String) {
        SitePermissionManager.setState(
            context.applicationContext, host,
            SitePermissionDatabase.TYPE_DATA_SAVER_EXCEPTION, SitePermissionDatabase.STATE_ALLOW
        )
        cache.clear()
        com.jhaiian.clint.browser.webview.ClintGecko.scheduleBundle()
        com.jhaiian.clint.browser.webview.ClintGecko.pushConfig()
    }

    fun invalidate() {
        cache.clear()
        com.jhaiian.clint.browser.webview.ClintGecko.scheduleBundle()
        com.jhaiian.clint.browser.webview.ClintGecko.pushConfig()
    }

    fun remove(context: Context, host: String) {
        SitePermissionManager.deleteEntry(
            context.applicationContext, host, SitePermissionDatabase.TYPE_DATA_SAVER_EXCEPTION
        )
        cache.clear()
        com.jhaiian.clint.browser.webview.ClintGecko.scheduleBundle()
        com.jhaiian.clint.browser.webview.ClintGecko.pushConfig()
    }
}

class DataSaverBridge(private val context: Context) {
    @JavascriptInterface
    fun isExcepted(host: String?): Boolean = DataSaverSiteException.isExcepted(context, host)
}
