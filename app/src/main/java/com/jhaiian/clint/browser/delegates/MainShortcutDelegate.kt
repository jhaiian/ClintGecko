package com.jhaiian.clint.browser.delegates
import com.jhaiian.clint.browser.MainActivity

import android.graphics.Bitmap
import android.widget.Toast
import com.jhaiian.clint.R
import com.jhaiian.clint.profiles.WebProfiles
import com.jhaiian.clint.shortcuts.ShortcutIconStore
import com.jhaiian.clint.shortcuts.ShortcutPinner
import com.jhaiian.clint.shortcuts.ShortcutSavedTab
import com.jhaiian.clint.shortcuts.ShortcutStore
import com.jhaiian.clint.shortcuts.ShortcutTabSessionManager
import com.jhaiian.clint.shortcuts.WebAppShortcut
import java.util.UUID

internal fun MainActivity.syncFramelessShortcuts() {
    val ids = ShortcutStore.framelessIds(this)
    if (ids == tabManager.framelessShortcutIds) return
    tabManager.framelessShortcutIds = ids
    val active = tabManager.activeTab
    if (active != null && tabManager.isGhostTab(active) && !uiState.isShortcutFrameless) {
        val previousIndex = active.previousTabId
            ?.let { id -> tabManager.tabs.indexOfFirst { it.id == id && !tabManager.isGhostTab(it) } }
            ?.takeIf { it != -1 }
        val target = previousIndex ?: tabManager.tabs.indexOfFirst { !tabManager.isGhostTab(it) }
        if (target != -1) {
            tabManager.switchTo(target)
            attachActiveWebView()
        } else {
            openNewTab(isIncognito = false, url = getHomepageUrl())
        }
    }
    updateTabCount()
}

internal fun MainActivity.applyShortcutFrameless(shortcutId: String?) {
    val enabled = shortcutId != null && (ShortcutStore.get(this, shortcutId)?.frameless ?: false)
    if (uiState.isShortcutFrameless == enabled) return
    uiState.isShortcutFrameless = enabled
    uiState.topBarFraction = 0f
    uiState.bottomBarFraction = 0f
    uiState.topBarFullHeightPx = 0
    uiState.bottomBarFullHeightPx = 0
    updateMainContentInsets()
}

internal fun MainActivity.createHomeScreenShortcut(url: String, name: String, icon: Bitmap?, frameless: Boolean, profileId: String = WebProfiles.DEFAULT_ID) {
    if (!ShortcutPinner.isSupported(this)) {
        Toast.makeText(this, getString(R.string.create_shortcut_unsupported), Toast.LENGTH_SHORT).show()
        return
    }
    val shortcutId = "webapp_${UUID.randomUUID()}"
    val iconPath = icon?.let { ShortcutIconStore.save(this, shortcutId, it) }
    val shortcut = WebAppShortcut(id = shortcutId, url = url, name = name, iconPath = iconPath, tabId = null, frameless = frameless, profileId = profileId)
    ShortcutStore.save(this, shortcut)
    syncFramelessShortcuts()
    ShortcutPinner.requestPin(this, shortcut)
}

internal fun MainActivity.openOrResumeShortcutTab(shortcutId: String, fallbackUrl: String?) {
    val record = ShortcutStore.get(this, shortcutId)
    val shortcutProfile = WebProfiles.resolve(this, record?.profileId)
    val staleTabIds = tabManager.tabs
        .filter { tabManager.effectiveShortcutId(it) == shortcutId && !it.isIncognito && it.profileId != shortcutProfile }
        .map { it.id }
    if (staleTabIds.isNotEmpty()) {
        persistShortcutTabsSession(shortcutId, async = false)
        releaseShortcutGroupTabs(staleTabIds)
    }
    val urlChanged = ShortcutStore.consumeUrlChanged(shortcutId)
    val groupInMemory = tabManager.tabs.any { tabManager.effectiveShortcutId(it) == shortcutId }
    if (groupInMemory) {
        val persistedActiveId = ShortcutTabSessionManager.load(this, shortcutId).firstOrNull { it.isActive }?.tabId
        val targetIndex = persistedActiveId
            ?.let { id -> tabManager.tabs.indexOfFirst { it.id == id && tabManager.effectiveShortcutId(it) == shortcutId } }
            ?.takeIf { it != -1 }
            ?: tabManager.tabs.indexOfFirst { tabManager.effectiveShortcutId(it) == shortcutId }
        tabManager.switchTo(targetIndex)
        attachActiveWebView()
        if (urlChanged) {
            ShortcutStore.get(this, shortcutId)?.url?.let { newUrl -> tabManager.activeTab?.webView?.loadUrl(newUrl) }
        }
        return
    }
    val persistedTabs = ShortcutTabSessionManager.load(this, shortcutId)
    if (persistedTabs.isNotEmpty()) {
        val activeId = persistedTabs.firstOrNull { it.isActive }?.tabId ?: persistedTabs.first().tabId
        persistedTabs.forEach { saved ->
            openNewTabSilent(saved.url, saved.tabId, shortcutId, deferLoad = saved.tabId != activeId, title = saved.title, profileId = shortcutProfile)
        }
        val index = tabManager.tabs.indexOfFirst { it.id == activeId }
        tabManager.switchTo(if (index != -1) index else tabManager.tabs.lastIndex)
        attachActiveWebView()
        return
    }
    val url = record?.url ?: fallbackUrl ?: getHomepageUrl()
    val previousTabId = tabManager.activeTab?.id
    openNewTab(isIncognito = false, url = url, shortcutId = shortcutId, previousTabId = previousTabId, profileId = shortcutProfile)
    val name = record?.name
    if (!name.isNullOrEmpty()) tabManager.activeTab?.title = name
    tabManager.activeTab?.let { ShortcutStore.updateTabId(this, shortcutId, it.id) }
}

internal fun MainActivity.exitShortcutFramelessToNormal() {
    val activeTab = tabManager.activeTab ?: return
    val activeShortcutId = tabManager.effectiveShortcutId(activeTab) ?: return
    persistShortcutTabsSession(activeShortcutId)
    captureActiveTabThumbnail()
    val previousId = activeTab.previousTabId
    val previousIndex = previousId
        ?.let { id -> tabManager.tabs.indexOfFirst { it.id == id && !tabManager.isGhostTab(it) } }
        ?.takeIf { it != -1 }
    val targetIndex = previousIndex ?: tabManager.tabs.indexOfLast { !tabManager.isGhostTab(it) }
    if (targetIndex != -1) {
        tabManager.switchTo(targetIndex)
        attachActiveWebView()
    } else {
        openNewTab(isIncognito = false, url = getHomepageUrl())
    }
}

internal fun MainActivity.persistShortcutTabsSession(shortcutId: String, async: Boolean = true) {
    if (ShortcutStore.get(this, shortcutId) == null) return
    val groupTabs = tabManager.tabs.filter { tabManager.effectiveShortcutId(it) == shortcutId }
    if (groupTabs.isEmpty()) return
    val previousActiveId = ShortcutTabSessionManager.load(this, shortcutId).firstOrNull { it.isActive }?.tabId
    val currentActiveId = tabManager.activeTab?.id
    val activeId = when {
        groupTabs.any { it.id == currentActiveId } -> currentActiveId
        groupTabs.any { it.id == previousActiveId } -> previousActiveId
        else -> groupTabs.first().id
    }
    val saved = groupTabs.mapIndexedNotNull { index, tab ->
        val url = tab.webView.url?.takeIf { it.isNotEmpty() && it != "about:blank" }
            ?: tab.url.takeIf { it.isNotEmpty() && it != "about:blank" }
            ?: return@mapIndexedNotNull null
        ShortcutSavedTab(position = index, url = url, title = tab.title, isActive = tab.id == activeId, tabId = tab.id)
    }
    if (saved.isEmpty()) return
    if (async) Thread { ShortcutTabSessionManager.save(this, shortcutId, saved) }.start()
    else ShortcutTabSessionManager.save(this, shortcutId, saved)
}

internal fun MainActivity.persistAllShortcutTabGroups() {
    val shortcutIds = tabManager.tabs.mapNotNull { tabManager.effectiveShortcutId(it) }.distinct()
    shortcutIds.forEach { persistShortcutTabsSession(it) }
}
