package com.jhaiian.clint.browser.delegates

import com.jhaiian.clint.browser.MainActivity

internal const val MAX_LIVE_BACKGROUND_TABS = 6

internal fun MainActivity.trimBackgroundTabs(keepLive: Int = MAX_LIVE_BACKGROUND_TABS) {
    val activeId = tabManager.activeTab?.id
    tabManager.tabs
        .filter { it.id != activeId && it.webView.canHibernate() }
        .sortedByDescending { it.lastActiveAt }
        .drop(keepLive)
        .forEach { it.webView.hibernate() }
}
