package com.jhaiian.clint.browser.delegates

import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.home.isClintHomeUrl
import com.jhaiian.clint.tabs.TabGroup
import com.jhaiian.clint.tabs.TabGroupBarState
import com.jhaiian.clint.tabs.TabGroupPrefs
import com.jhaiian.clint.ui.UNDO_SNACKBAR_TIMEOUT_MS
import com.jhaiian.clint.ui.showClintSnackbar

private data class ClosedTabSnapshot(
    val url: String,
    val title: String,
    val profileId: String,
    val groupId: String?,
    val index: Int
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
            ClosedTabSnapshot(url, tab.title, tab.profileId, tab.groupId, tabManager.tabs.indexOf(tab))
        }
        .sortedBy { it.index }
    val closedGroups = snapshots.mapNotNull { it.groupId }.distinct().mapNotNull { tabManager.group(it) }
    val activeWasClosed = targets.any { it.id == previousActiveId }
    targets.forEach { tab ->
        val index = tabManager.tabs.indexOfFirst { it.id == tab.id }
        if (index >= 0) onTabClosed(index)
    }
    val placeholderTabId = if (closingAll) tabManager.activeTab?.id else null
    onTabGroupsChanged()
    if (snapshots.isEmpty()) return
    showClintSnackbar(message, getString(R.string.action_undo), timeoutMs = UNDO_SNACKBAR_TIMEOUT_MS) {
        reopenClosedTabs(snapshots, closedGroups, placeholderTabId, if (activeWasClosed) null else previousActiveId)
    }
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
            groupId = snapshot.groupId.takeIf { groupsEnabled }
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
