package com.jhaiian.clint.browser.delegates

import com.jhaiian.clint.blocker.blockedpage.WebsiteBlockedRequest
import com.jhaiian.clint.blocker.engine.WebsiteBlockerWebIntegration
import com.jhaiian.clint.browser.MainActivity

internal fun MainActivity.initializeWebsiteBlockerEngine() {
    WebsiteBlockerWebIntegration.initialize(this)
}

internal fun MainActivity.onWebsiteBlocked(blockedUrl: String, previousUrl: String, tabId: String) {
    if (tabManager.activeTab?.id != tabId) return
    uiState.websiteBlockedRequest = WebsiteBlockedRequest(
        blockedUrl = blockedUrl,
        previousUrl = previousUrl,
        tabId = tabId
    )
}

internal fun MainActivity.dismissWebsiteBlockedOverlay() {
    val request = uiState.websiteBlockedRequest ?: return
    uiState.websiteBlockedRequest = null
    val tab = tabManager.tabs.find { it.id == request.tabId } ?: return
    val currentUrl = tab.webView.url
    val blockedHost = runCatching { android.net.Uri.parse(request.blockedUrl).host }.getOrNull()
    val currentHost = runCatching { android.net.Uri.parse(currentUrl).host }.getOrNull()
    val blockedPageCommitted = !blockedHost.isNullOrEmpty() && blockedHost.equals(currentHost, ignoreCase = true)
    when {
        blockedPageCommitted && tab.webView.canGoBack() -> tab.webView.goBack()
        blockedPageCommitted || currentUrl.isNullOrEmpty() -> tab.webView.loadUrl(getHomepageUrl(tab.isIncognito))
        else -> tab.webView.onBlockedLoadDismissed()
    }
}
