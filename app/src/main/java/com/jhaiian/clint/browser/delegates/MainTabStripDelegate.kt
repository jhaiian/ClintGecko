package com.jhaiian.clint.browser.delegates

import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.profiles.ProfileRepository
import com.jhaiian.clint.profiles.WebProfiles
import com.jhaiian.clint.tabs.TabBarPlacement
import com.jhaiian.clint.tabs.TabGroupPrefs
import com.jhaiian.clint.tabs.TabStripPrefs
import com.jhaiian.clint.tabs.TabStripState

internal fun MainActivity.refreshTabStrip() {
    if (!TabStripPrefs.isEnabled(this, prefs)) {
        uiState.tabStrip = null
        return
    }
    val active = tabManager.activeTab
    val incognito = active?.isIncognito == true
    val activeId = active?.takeIf { !tabManager.isGhostTab(it) }?.id
    val previews = tabManager.previews().filter { it.isIncognito == incognito }
    val profileIds = previews.map { it.profileId }.filter { it != WebProfiles.DEFAULT_ID }.toSet()
    val profileColors = if (profileIds.isEmpty()) emptyMap() else {
        ProfileRepository.all(this).filter { it.id in profileIds }.associate { it.id to it.color }
    }
    uiState.tabStrip = TabStripState(
        tabs = previews,
        groups = tabManager.groups.filter { it.isIncognito == incognito },
        activeTabId = activeId,
        isIncognito = incognito,
        profileColors = profileColors,
        groupsEnabled = TabGroupPrefs.isEnabled(prefs)
    )
}

internal fun MainActivity.updateTabBarPositions() {
    uiState.tabStripPosition = TabBarPlacement.normalize(prefs.getString(TabBarPlacement.PREF_TAB_STRIP_POSITION, null))
    uiState.tabGroupBarPosition = TabBarPlacement.normalize(prefs.getString(TabBarPlacement.PREF_TAB_GROUP_BAR_POSITION, null))
}

internal fun MainActivity.openNewTabFromStrip() {
    openNewTab(isIncognito = tabManager.activeTab?.isIncognito == true)
}

internal fun MainActivity.closeTabFromStrip(tabId: String) {
    closeTabsWithUndo(listOf(tabId), resources.getQuantityString(com.jhaiian.clint.R.plurals.tab_menu_closed_tabs, 1, 1))
}

internal fun MainActivity.closeOtherTabsFromStrip(tabId: String) {
    val keep = tabManager.tabs.firstOrNull { it.id == tabId } ?: return
    val others = tabManager.previews().filter { it.isIncognito == keep.isIncognito && it.id != tabId }.map { it.id }
    if (others.isEmpty()) return
    if (tabManager.activeTab?.id != tabId) focusTabById(tabId)
    closeTabsWithUndo(others, resources.getQuantityString(com.jhaiian.clint.R.plurals.tab_menu_closed_tabs, others.size, others.size))
}

internal fun MainActivity.addTabToNewGroup(tabId: String, name: String, color: Int) {
    tabManager.createGroup(name, color, listOf(tabId)) ?: return
    onTabGroupsChanged()
}

internal fun MainActivity.removeTabFromGroup(tabId: String) {
    tabManager.removeFromGroup(listOf(tabId))
    onTabGroupsChanged()
}

internal fun MainActivity.toggleTabGroupCollapsed(groupId: String) {
    tabManager.updateGroup(groupId) { it.copy(collapsed = !it.collapsed) }
    onTabGroupsChanged()
}
