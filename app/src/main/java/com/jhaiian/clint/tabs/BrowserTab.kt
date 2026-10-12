package com.jhaiian.clint.tabs

import com.jhaiian.clint.browser.webview.ClintWebView
import com.jhaiian.clint.profiles.WebProfiles
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "New Tab",
    var url: String = "",
    val isIncognito: Boolean = false,
    val isRefreshLinkTab: Boolean = false,
    val openerTabId: String? = null,
    val shortcutId: String? = null,
    val previousTabId: String? = null,
    var pendingUrl: String? = null,
    val profileId: String = WebProfiles.DEFAULT_ID,
    val webView: ClintWebView
) {
    var lastActiveAt: Long = System.currentTimeMillis()

    init {
        WebProfiles.registerTab(id, profileId)
    }
}
