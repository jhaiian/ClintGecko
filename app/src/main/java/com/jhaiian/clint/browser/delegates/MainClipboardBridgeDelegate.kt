package com.jhaiian.clint.browser.delegates
import com.jhaiian.clint.browser.MainActivity

import com.jhaiian.clint.browser.webview.ClintWebView
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.sitepermissions.SitePermissionDatabase
import com.jhaiian.clint.settings.sitepermissions.SitePermissionManager
import com.jhaiian.clint.settings.sitepermissions.SitePermissionActivity

internal fun MainActivity.showWebClipboardPermissionFromBridge(
    webView: ClintWebView,
    callbackId: String,
    rawOrigin: String,
    isIncognito: Boolean = false
) {
    val safeId = callbackId.replace("'", "")

    if (!isIncognito) {
        val stored = SitePermissionManager.getState(this, rawOrigin, SitePermissionDatabase.TYPE_CLIPBOARD)
        if (stored == SitePermissionDatabase.STATE_ALLOW) {
            webView.evaluateJavascript("window._ClintResolvePermission('$safeId','granted')", null)
            return
        }
        if (stored == SitePermissionDatabase.STATE_DENY) {
            webView.evaluateJavascript("window._ClintResolvePermission('$safeId','denied')", null)
            return
        }

        val globalDefault = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("site_perm_default_${SitePermissionDatabase.TYPE_CLIPBOARD}", SitePermissionActivity.PREF_VALUE_ASK) ?: SitePermissionActivity.PREF_VALUE_ASK
        when (globalDefault) {
            SitePermissionActivity.PREF_VALUE_ALLOW -> {
                webView.evaluateJavascript("window._ClintResolvePermission('$safeId','granted')", null)
                return
            }
            SitePermissionActivity.PREF_VALUE_DENY -> {
                webView.evaluateJavascript("window._ClintResolvePermission('$safeId','denied')", null)
                return
            }
            else -> {}
        }
    }

    val displayOrigin = rawOrigin.ifEmpty { getString(R.string.clipboard_web_request_origin_unknown) }
    uiState.webPermissionDialogRequest = com.jhaiian.clint.ui.WebPermissionDialogRequest(
        title = getString(R.string.clipboard_web_request_title),
        message = getString(R.string.clipboard_web_request_message, displayOrigin),
        isIncognito = isIncognito,
        onAllow = { remember ->
            if (remember && !isIncognito) SitePermissionManager.setState(this, rawOrigin, SitePermissionDatabase.TYPE_CLIPBOARD, SitePermissionDatabase.STATE_ALLOW)
            webView.evaluateJavascript("window._ClintResolvePermission('$safeId','granted')", null)
        },
        onDeny = { remember ->
            if (remember && !isIncognito) SitePermissionManager.setState(this, rawOrigin, SitePermissionDatabase.TYPE_CLIPBOARD, SitePermissionDatabase.STATE_DENY)
            webView.evaluateJavascript("window._ClintResolvePermission('$safeId','denied')", null)
        }
    )
}
