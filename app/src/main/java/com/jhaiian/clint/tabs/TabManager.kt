package com.jhaiian.clint.tabs

class TabManager {

    val tabs = mutableListOf<BrowserTab>()
    var activeIndex = 0
    var framelessShortcutIds: Set<String> = emptySet()

    val activeTab: BrowserTab? get() = tabs.getOrNull(activeIndex)
    val count: Int get() = tabs.size

    fun touchActive() {
        activeTab?.lastActiveAt = System.currentTimeMillis()
    }

    fun add(tab: BrowserTab): Int {
        touchActive()
        tabs.add(tab)
        activeIndex = tabs.lastIndex
        return activeIndex
    }

    fun addInBackground(tab: BrowserTab): Int {
        tabs.add(tab)
        return tabs.lastIndex
    }

    fun closeTab(index: Int) {
        if (index !in tabs.indices) return
        val tab = tabs[index]
        if (tab.isIncognito) {
            tab.webView.clearCache(true)
            tab.webView.clearHistory()
        }
        releaseWebView(tab)
        tabs.removeAt(index)
        if (activeIndex >= tabs.size) {
            activeIndex = (tabs.size - 1).coerceAtLeast(0)
        }
    }

    private fun releaseWebView(tab: BrowserTab) {
        val webView = tab.webView
        (webView.parent as? android.view.ViewGroup)?.removeView(webView)
        webView.stopLoading()
        webView.webViewClient = null
        webView.webChromeClient = null
        webView.destroy()
        com.jhaiian.clint.profiles.WebProfiles.unregisterTab(tab.id)
    }

    fun switchTo(index: Int) {
        if (index !in tabs.indices) return
        touchActive()
        activeIndex = index
        touchActive()
    }

    fun effectiveShortcutId(tab: BrowserTab): String? {
        if (tab.shortcutId != null) return tab.shortcutId
        var openerId = tab.openerTabId
        val visited = mutableSetOf<String>()
        while (openerId != null && visited.add(openerId)) {
            val opener = tabs.firstOrNull { it.id == openerId } ?: return null
            if (opener.shortcutId != null) return opener.shortcutId
            openerId = opener.openerTabId
        }
        return null
    }

    fun isGhostTab(tab: BrowserTab): Boolean {
        val shortcutId = effectiveShortcutId(tab) ?: return false
        return shortcutId in framelessShortcutIds
    }

    fun previews(): List<TabPreview> = tabs.filter { !isGhostTab(it) }.map {
        TabPreview(it.id, it.title.ifBlank { "New Tab" }, if (com.jhaiian.clint.browser.home.isClintHomeUrl(it.url)) "" else it.url, it.isIncognito, it.profileId, it.webView.audioState)
    }

    fun moveTab(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in tabs.indices || toIndex !in tabs.indices || fromIndex == toIndex) return
        val activeTabId = activeTab?.id
        val tab = tabs.removeAt(fromIndex)
        tabs.add(toIndex, tab)
        if (activeTabId != null) activeIndex = tabs.indexOfFirst { it.id == activeTabId }
    }

    fun reorderTo(orderedIds: List<String>) {
        val activeTabId = activeTab?.id
        val byId = tabs.associateBy { it.id }
        val reorderedVisible = orderedIds.mapNotNull { byId[it] }
        if (reorderedVisible.size != orderedIds.size) return
        val includedIds = orderedIds.toSet()
        var cursor = 0
        val result = tabs.map { tab -> if (tab.id in includedIds) reorderedVisible[cursor++] else tab }
        tabs.clear()
        tabs.addAll(result)
        if (activeTabId != null) activeIndex = tabs.indexOfFirst { it.id == activeTabId }
    }

    fun destroyAll() {
        tabs.forEach {
            if (it.isIncognito) {
                it.webView.clearCache(true)
                it.webView.clearHistory()
            }
            releaseWebView(it)
        }
        tabs.clear()
        activeIndex = 0
    }
}
