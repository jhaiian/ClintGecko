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
    if (tab.webView.canGoBack()) {
        tab.webView.goBack()
    } else {
        tab.webView.loadUrl(getHomepageUrl())
    }
}
