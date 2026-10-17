package com.jhaiian.clint.browser.delegates

import android.net.Uri
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.webview.ClintWebView
import com.jhaiian.clint.settings.sitepermissions.SitePermissionActivity
import com.jhaiian.clint.settings.sitepermissions.SitePermissionDatabase
import com.jhaiian.clint.settings.sitepermissions.SitePermissionManager
import com.jhaiian.clint.ui.WebPermissionDialogRequest

internal fun MainActivity.showWebClipboardPermission(
    webView: ClintWebView,
    uri: String,
    onResult: (Boolean) -> Unit
) {
    if (webView.isPrivate || tabManager.activeTab?.webView !== webView) {
        onResult(false)
        return
    }
    val origin = Uri.parse(uri).host ?: uri
    when (SitePermissionManager.getState(this, origin, SitePermissionDatabase.TYPE_CLIPBOARD)) {
        SitePermissionDatabase.STATE_ALLOW -> {
            onResult(true)
            return
        }
        SitePermissionDatabase.STATE_DENY -> {
            onResult(false)
            return
        }
    }
    val globalDefault = PreferenceManager.getDefaultSharedPreferences(this)
        .getString("site_perm_default_${SitePermissionDatabase.TYPE_CLIPBOARD}", SitePermissionActivity.PREF_VALUE_ASK)
        ?: SitePermissionActivity.PREF_VALUE_ASK
    when (globalDefault) {
        SitePermissionActivity.PREF_VALUE_ALLOW -> {
            onResult(true)
            return
        }
        SitePermissionActivity.PREF_VALUE_DENY -> {
            onResult(false)
            return
        }
    }
    val displayOrigin = origin.ifEmpty { getString(R.string.clipboard_web_request_origin_unknown) }
    uiState.webPermissionDialogRequest = WebPermissionDialogRequest(
        title = getString(R.string.clipboard_web_request_title),
        message = getString(R.string.clipboard_web_request_message, displayOrigin),
        isIncognito = false,
        onAllow = { remember ->
            if (remember) SitePermissionManager.setState(this, origin, SitePermissionDatabase.TYPE_CLIPBOARD, SitePermissionDatabase.STATE_ALLOW)
            onResult(true)
        },
        onDeny = { remember ->
            if (remember) SitePermissionManager.setState(this, origin, SitePermissionDatabase.TYPE_CLIPBOARD, SitePermissionDatabase.STATE_DENY)
            onResult(false)
        }
    )
}

internal fun MainActivity.dismissWebClipboardPermission() {
    if (uiState.webPermissionDialogRequest?.title == getString(R.string.clipboard_web_request_title)) {
        uiState.webPermissionDialogRequest = null
    }
}
