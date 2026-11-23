package com.jhaiian.clint.browser.delegates

import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.home.isClintHomeUrl
import com.jhaiian.clint.profiles.ProfileRepository
import com.jhaiian.clint.profiles.WebProfiles
import com.jhaiian.clint.tabs.BrowserTab
import com.jhaiian.clint.tabs.ClosedTab
import com.jhaiian.clint.tabs.TabGroup
import com.jhaiian.clint.tabs.TabGroupBarState
import com.jhaiian.clint.tabs.TabGroupPrefs
import com.jhaiian.clint.tabs.TabSessionManager
import com.jhaiian.clint.ui.UNDO_SNACKBAR_TIMEOUT_MS
import com.jhaiian.clint.ui.showClintSnackbar

private data class ClosedTabSnapshot(
    val url: String,
    val title: String,
    val profileId: String,
    val groupId: String?,
    val index: Int,
    val isPinned: Boolean = false
)

internal fun MainActivity.refreshTabGroupBar() {
    val active = tabManager.activeTab
    val group = tabManager.groupOf(active)
    if (active == null || group == null || tabManager.isGhostTab(active) || !prefs.getBoolean(TabGroupPrefs.PREF_SHOW_GROUP_BAR, true)) {
        uiState.tabGroupBar = null
        return
    }
    val members = tabManager.previews().filter { it.groupId == group.id }
    uiState.tabGroupBar = TabGroupBarState(group, members, active.id)
}

internal fun MainActivity.applyTabGroupsSetting() {
    if (TabGroupPrefs.isEnabled(prefs) || tabManager.groups.isEmpty()) {
        updateTabCount()
        return
    }
    tabManager.removeFromGroup(tabManager.tabs.filter { it.groupId != null }.map { it.id })
    tabManager.groups.clear()
    uiState.tabMenuFocusGroupId = null
    onTabGroupsChanged()
}

internal fun MainActivity.onTabGroupsChanged() {
    updateTabCount()
    saveTabs()
    uiState.tabsRevision++
}

internal fun MainActivity.openNewTabInGroup(groupId: String) {
    val group = tabManager.group(groupId) ?: return
    val profileId = tabManager.tabsInGroup(groupId).lastOrNull()?.takeIf { !it.isIncognito }?.profileId
    openNewTab(isIncognito = group.isIncognito, profileId = profileId, groupId = groupId)
    onTabGroupsChanged()
}

internal fun MainActivity.createTabGroupWithNewTab(name: String, color: Int, isIncognito: Boolean = false) {
    openNewTab(isIncognito = isIncognito)
    val tabId = tabManager.activeTab?.id ?: return
    tabManager.createGroup(name, color, listOf(tabId))
    onTabGroupsChanged()
}

internal fun MainActivity.closeTabsWithUndo(ids: List<String>, message: String) {
    val targets = ids.distinct().mapNotNull { id -> tabManager.tabs.firstOrNull { it.id == id } }
    if (targets.isEmpty()) return
    val previousActiveId = tabManager.activeTab?.id
    val closingAll = targets.map { it.id }.toSet().containsAll(tabManager.previews().map { it.id })
    val snapshots = targets
        .filter { !it.isIncognito && !it.isRefreshLinkTab && !tabManager.isGhostTab(it) }
        .mapNotNull { tab ->
            val url = tab.pendingUrl?.takeIf { it.isNotEmpty() && it != "about:blank" }
                ?: tab.webView.url?.takeIf { it.isNotEmpty() && it != "about:blank" }
                ?: tab.url.takeIf { it.isNotEmpty() && it != "about:blank" }
                ?: return@mapNotNull null
            ClosedTabSnapshot(url, tab.title, tab.profileId, tab.groupId, tabManager.tabs.indexOf(tab), tab.isPinned)
        }
        .sortedBy { it.index }
    val closedGroups = snapshots.mapNotNull { it.groupId }.distinct().mapNotNull { tabManager.group(it) }
    val activeWasClosed = targets.any { it.id == previousActiveId }
    val closedAt = System.currentTimeMillis()
    recordClosedTabs(targets, closedAt)
    targets.forEach { tab ->
        val index = tabManager.tabs.indexOfFirst { it.id == tab.id }
        if (index >= 0) onTabClosed(index)
    }
    val placeholderTabId = if (closingAll) tabManager.activeTab?.id else null
    onTabGroupsChanged()
    if (snapshots.isEmpty()) return
    showClintSnackbar(message, getString(R.string.action_undo), timeoutMs = UNDO_SNACKBAR_TIMEOUT_MS) {
        TabSessionManager.removeClosedBatchAsync(this, closedAt)
        reopenClosedTabs(snapshots, closedGroups, placeholderTabId, if (activeWasClosed) null else previousActiveId)
    }
}

private fun MainActivity.closedTabEntries(tabs: List<BrowserTab>, closedAt: Long): List<ClosedTab> =
    tabs
        .filter { !it.isIncognito && !it.isRefreshLinkTab && !tabManager.isGhostTab(it) }
        .mapNotNull { tab ->
            val url = tab.pendingUrl?.takeIf { it.isNotEmpty() && it != "about:blank" }
                ?: tab.webView.url?.takeIf { it.isNotEmpty() && it != "about:blank" }
                ?: tab.url.takeIf { it.isNotEmpty() && it != "about:blank" }
                ?: return@mapNotNull null
            if (isClintHomeUrl(url)) return@mapNotNull null
            val group = tabManager.group(tab.groupId)
            tabManager.tabs.indexOf(tab) to ClosedTab(
                url = url,
                title = tab.title,
                profileId = tab.profileId,
                groupName = group?.name,
                groupColor = group?.color,
                closedAt = closedAt
            )
        }
        .sortedBy { it.first }
        .map { it.second }

internal fun MainActivity.recordClosedTabs(tabs: List<BrowserTab>, closedAt: Long) {
    TabSessionManager.addClosedAsync(this, closedTabEntries(tabs, closedAt))
}

internal fun MainActivity.reopenClosedTab(entry: ClosedTab) {
    val profileId = if (entry.profileId == WebProfiles.DEFAULT_ID || ProfileRepository.get(this, entry.profileId) != null) {
        entry.profileId
    } else {
        WebProfiles.DEFAULT_ID
    }
    val groupName = entry.groupName
    val groupColor = entry.groupColor
    val grouped = TabGroupPrefs.isEnabled(prefs) && groupName != null && groupColor != null
    val match = if (grouped) {
        tabManager.groups.firstOrNull { !it.isIncognito && it.name == groupName && it.color == groupColor }
    } else {
        null
    }
    openNewTab(isIncognito = false, url = entry.url, profileId = profileId, groupId = match?.id)
    if (grouped && match == null) {
        tabManager.activeTab?.id?.let { tabManager.createGroup(groupName, groupColor, listOf(it)) }
    }
    TabSessionManager.removeClosedAsync(this, entry.id)
    onTabGroupsChanged()
}

private fun MainActivity.reopenClosedTabs(
    snapshots: List<ClosedTabSnapshot>,
    groups: List<TabGroup>,
    placeholderTabId: String?,
    keepActiveId: String?
) {
    val groupsEnabled = TabGroupPrefs.isEnabled(prefs)
    if (groupsEnabled) groups.forEach { tabManager.restoreGroup(it) }
    val currentActiveId = tabManager.activeTab?.id
    val reopenedIds = snapshots.map { snapshot ->
        val id = java.util.UUID.randomUUID().toString()
        openNewTabSilent(
            snapshot.url,
            id = id,
            deferLoad = true,
            title = snapshot.title,
            profileId = snapshot.profileId,
            groupId = snapshot.groupId.takeIf { groupsEnabled },
            isPinned = snapshot.isPinned
        )
        id
    }
    val order = tabManager.tabs.map { it.id }.filterNot { it in reopenedIds }.toMutableList()
    snapshots.zip(reopenedIds).forEach { (snapshot, id) -> order.add(snapshot.index.coerceIn(0, order.size), id) }
    tabManager.reorderTo(order)
    tabManager.normalizeGroups()
    val placeholder = placeholderTabId?.let { id -> tabManager.tabs.firstOrNull { it.id == id } }
    val placeholderUntouched = placeholder != null && isClintHomeUrl(placeholder.webView.url ?: placeholder.url) && !placeholder.webView.canGoBack()
    if (placeholderUntouched) {
        tabManager.closeTab(tabManager.tabs.indexOf(placeholder))
    }
    val targetId = when {
        keepActiveId != null && tabManager.tabs.any { it.id == keepActiveId } -> keepActiveId
        placeholderUntouched || keepActiveId == null -> reopenedIds.first()
        else -> currentActiveId
    }
    val targetIndex = tabManager.tabs.indexOfFirst { it.id == targetId }
    if (targetIndex >= 0) tabManager.switchTo(targetIndex)
    attachActiveWebView()
    onTabGroupsChanged()
}
