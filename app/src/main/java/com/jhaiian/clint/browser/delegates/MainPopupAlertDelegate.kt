package com.jhaiian.clint.browser.delegates

import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.dialogs.PopupAlertRequest
import com.jhaiian.clint.ui.showClintDestinationSnackbar

internal fun MainActivity.showPopupAlertDialog(newUrl: String, isIncognito: Boolean, sourceTabId: String? = null) {
    if (uiState.isFullscreen) return
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
        if (scheme == "http" || scheme == "https") {
            if (client == null || !client.tryOpenInApp(activeWebView, uri)) {
                openNewTab(isIncognito = isIncognito, url = newUrl, openerTabId = sourceTabId)
            }
        } else {
            openNewTab(isIncognito = isIncognito, url = newUrl, openerTabId = sourceTabId)
        }
    }

    if (isSameOrigin(sourceUrl, newUrl)) {
        onAllow()
        return
    }

    if (prefs.getString("popup_alert_style", "dialog") == "snackbar") {
        uiState.popupAlertRequest = null
        showClintDestinationSnackbar(
            message = getString(R.string.popup_alert_snackbar_message, sourceHost),
            destination = newUrl,
            actionLabel = getString(R.string.popup_alert_snackbar_action),
            onAction = onAllow
        )
        return
    }

    uiState.popupAlertRequest = PopupAlertRequest(
        sourceHost = sourceHost,
        newUrl = newUrl,
        onAllow = onAllow
    )
}

private fun isSameOrigin(sourceUrl: String?, targetUrl: String): Boolean {
    val source = sourceUrl?.let { android.net.Uri.parse(it) } ?: return false
    val target = android.net.Uri.parse(targetUrl)
    val sourceScheme = source.scheme?.lowercase() ?: return false
    val targetScheme = target.scheme?.lowercase() ?: return false
    if (sourceScheme != targetScheme || (sourceScheme != "http" && sourceScheme != "https")) return false
    val sourceHost = source.host?.lowercase()?.takeIf { it.isNotEmpty() } ?: return false
    val targetHost = target.host?.lowercase()?.takeIf { it.isNotEmpty() } ?: return false
    return sourceHost == targetHost && effectivePort(source, sourceScheme) == effectivePort(target, targetScheme)
}

private fun effectivePort(uri: android.net.Uri, scheme: String): Int =
    uri.port.takeIf { it != -1 } ?: if (scheme == "https") 443 else 80
