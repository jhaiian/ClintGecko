package com.jhaiian.clint.browser.delegates

import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.dialogs.PopupAlertRequest
import com.jhaiian.clint.browser.webview.ClintPopupWindow
import com.jhaiian.clint.browser.webview.isSameOriginUrl
import com.jhaiian.clint.ui.showClintDestinationSnackbar

internal fun MainActivity.showPopupAlertDialog(popup: ClintPopupWindow, isIncognito: Boolean, sourceTabId: String? = null) {
    if (uiState.isFullscreen) {
        popup.decline()
        return
    }
    val newUrl = popup.url
    val sourceTab = sourceTabId?.let { id -> tabManager.tabs.firstOrNull { it.id == id } } ?: tabManager.activeTab
    val sourceUrl = sourceTab?.webView?.url
    val sourceHost = sourceUrl
        ?.let { android.net.Uri.parse(it).host?.takeIf { h -> h.isNotEmpty() } ?: it }
        ?: getString(R.string.popup_alert_source_unknown)

    val onAllow: () -> Unit = {
        val uri = android.net.Uri.parse(newUrl)
        val scheme = uri.scheme?.lowercase()
        val activeWebView = tabManager.activeTab?.webView
        val client = activeWebView?.webViewClient
        if ((scheme == "http" || scheme == "https") && client != null && client.tryOpenInApp(activeWebView, uri)) {
            popup.decline()
        } else {
            openNewTab(isIncognito = isIncognito, url = newUrl, openerTabId = sourceTabId, popupWindow = popup)
        }
    }

    if (popup.skipPrompt || isSameOriginUrl(sourceUrl, newUrl)) {
        onAllow()
        return
    }

    uiState.popupAlertRequest?.let { previous -> previous.onDeny() }
    if (prefs.getString("popup_alert_style", "dialog") == "snackbar") {
        uiState.popupAlertRequest = null
        showClintDestinationSnackbar(
            message = getString(R.string.popup_alert_snackbar_message, sourceHost),
            destination = newUrl,
            actionLabel = getString(R.string.popup_alert_snackbar_action),
            onAction = onAllow,
            onDismissed = { popup.decline() }
        )
        return
    }

    uiState.popupAlertRequest = PopupAlertRequest(
        sourceHost = sourceHost,
        newUrl = newUrl,
        onAllow = onAllow,
        onDeny = { popup.decline() }
    )
}
