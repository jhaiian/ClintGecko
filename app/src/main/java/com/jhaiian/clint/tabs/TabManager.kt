package com.jhaiian.clint.tabs

class TabManager {

    val tabs = mutableListOf<BrowserTab>()
    val groups = mutableListOf<TabGroup>()
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
        normalizeGroups(tab.id)
        return activeIndex
    }

    fun addInBackground(tab: BrowserTab): Int {
        tabs.add(tab)
        normalizeGroups()
        return tabs.indexOf(tab)
    }

    fun closeTab(index: Int) {
        if (index !in tabs.indices) return
        val tab = tabs[index]
        val activeTabId = activeTab?.id
        val wasActive = tab.id == activeTabId
        if (tab.isIncognito) {
            tab.webView.clearCache(true)
            tab.webView.clearHistory()
        }
        releaseWebView(tab)
        tabs.removeAt(index)
        if (!wasActive) {
            activeIndex = tabs.indexOfFirst { it.id == activeTabId }.coerceAtLeast(0)
        } else {
            val groupId = tab.groupId
            val sibling = if (groupId == null) null else {
                tabs.getOrNull(index)?.takeIf { it.groupId == groupId && !isGhostTab(it) }
                    ?: tabs.getOrNull(index - 1)?.takeIf { it.groupId == groupId && !isGhostTab(it) }
            }
            activeIndex = when {
                sibling != null -> tabs.indexOf(sibling)
                activeIndex >= tabs.size -> (tabs.size - 1).coerceAtLeast(0)
                else -> activeIndex
            }
        }
        pruneEmptyGroups()
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
        TabPreview(it.id, it.title.ifBlank { "New Tab" }, if (com.jhaiian.clint.browser.home.isClintHomeUrl(it.url)) "" else it.url, it.isIncognito, it.profileId, it.webView.audioState, it.groupId)
    }

    fun moveTab(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in tabs.indices || toIndex !in tabs.indices || fromIndex == toIndex) return
        val activeTabId = activeTab?.id
        val tab = tabs.removeAt(fromIndex)
        tabs.add(toIndex, tab)
        if (activeTabId != null) activeIndex = tabs.indexOfFirst { it.id == activeTabId }
        normalizeGroups()
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

    fun group(groupId: String?): TabGroup? = groupId?.let { id -> groups.firstOrNull { it.id == id } }

    fun groupOf(tab: BrowserTab?): TabGroup? = group(tab?.groupId)

    fun tabsInGroup(groupId: String): List<BrowserTab> = tabs.filter { it.groupId == groupId && !isGhostTab(it) }

    fun createGroup(name: String, color: Int, tabIds: List<String>): TabGroup? {
        val members = groupableTabs(tabIds)
        val first = members.firstOrNull() ?: return null
        val group = TabGroup(name = name, color = color, isIncognito = first.isIncognito)
        groups.add(group)
        moveIntoGroup(members.filter { it.isIncognito == group.isIncognito }, group.id)
        return group(group.id)
    }

    fun addToGroup(tabIds: List<String>, groupId: String) {
        val group = group(groupId) ?: return
        val members = groupableTabs(tabIds).filter { it.isIncognito == group.isIncognito && it.groupId != groupId }
        moveIntoGroup(members, groupId)
    }

    fun removeFromGroup(tabIds: List<String>) {
        val activeTabId = activeTab?.id
        val ids = tabIds.toSet()
        val members = tabs.filter { it.id in ids && it.groupId != null }
        if (members.isEmpty()) return
        members.groupBy { it.groupId }.forEach { (groupId, leaving) ->
            val groupTabs = tabs.filter { it.groupId == groupId }
            val start = tabs.indexOf(groupTabs.first())
            val remaining = groupTabs - leaving.toSet()
            tabs.removeAll(leaving.toSet())
            leaving.forEach { it.groupId = null }
            val insertAt = remaining.lastOrNull()?.let { tabs.indexOf(it) + 1 } ?: start.coerceAtMost(tabs.size)
            tabs.addAll(insertAt, leaving)
        }
        normalizeGroups(activeTabId)
        pruneEmptyGroups()
    }

    fun ungroup(groupId: String) {
        removeFromGroup(tabs.filter { it.groupId == groupId }.map { it.id })
        groups.removeAll { it.id == groupId }
    }

    fun updateGroup(groupId: String, transform: (TabGroup) -> TabGroup) {
        val index = groups.indexOfFirst { it.id == groupId }
        if (index >= 0) groups[index] = transform(groups[index]).copy(id = groupId, isIncognito = groups[index].isIncognito)
    }

    fun restoreGroup(group: TabGroup) {
        if (groups.none { it.id == group.id }) groups.add(group)
    }

    fun applyArrangement(orderedIds: List<String>, assignments: Map<String, String?>) {
        val activeTabId = activeTab?.id
        assignments.forEach { (tabId, groupId) ->
            val tab = tabs.firstOrNull { it.id == tabId } ?: return@forEach
            val target = group(groupId)
            tab.groupId = if (target != null && target.isIncognito == tab.isIncognito && !isGhostTab(tab)) target.id else null
        }
        reorderTo(orderedIds)
        normalizeGroups(activeTabId)
        pruneEmptyGroups()
    }

    fun pruneEmptyGroups() {
        groups.retainAll { group -> tabs.any { it.groupId == group.id } }
    }

    fun normalizeGroups(activeTabId: String? = activeTab?.id) {
        tabs.forEach { tab ->
            val group = group(tab.groupId)
            if (tab.groupId != null && (group == null || group.isIncognito != tab.isIncognito || tab.isRefreshLinkTab)) tab.groupId = null
        }
        val emitted = mutableSetOf<String>()
        val ordered = ArrayList<BrowserTab>(tabs.size)
        tabs.forEach { tab ->
            val groupId = tab.groupId
            if (groupId == null) ordered.add(tab)
            else if (emitted.add(groupId)) ordered.addAll(tabs.filter { it.groupId == groupId })
        }
        tabs.clear()
        tabs.addAll(ordered)
        val groupOrder = tabs.mapNotNull { it.groupId }.distinct()
        groups.sortBy { group -> groupOrder.indexOf(group.id).let { if (it < 0) Int.MAX_VALUE else it } }
        if (activeTabId != null) {
            val index = tabs.indexOfFirst { it.id == activeTabId }
            if (index >= 0) activeIndex = index
        }
        if (activeIndex !in tabs.indices) activeIndex = (tabs.size - 1).coerceAtLeast(0)
    }

    private fun groupableTabs(tabIds: List<String>): List<BrowserTab> {
        val byId = tabs.associateBy { it.id }
        return tabIds.distinct().mapNotNull { byId[it] }.filter { !isGhostTab(it) && !it.isRefreshLinkTab }
    }

    private fun moveIntoGroup(members: List<BrowserTab>, groupId: String) {
        if (members.isEmpty()) {
            pruneEmptyGroups()
            return
        }
        val activeTabId = activeTab?.id
        val existing = tabs.filter { it.groupId == groupId }
        val anchor = members.minOf { tabs.indexOf(it) }
        tabs.removeAll(members.toSet())
        members.forEach { it.groupId = groupId }
        val insertAt = existing.lastOrNull()?.let { tabs.indexOf(it) + 1 } ?: anchor.coerceAtMost(tabs.size)
        tabs.addAll(insertAt, members)
        normalizeGroups(activeTabId)
        pruneEmptyGroups()
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
        groups.clear()
        activeIndex = 0
    }
}
