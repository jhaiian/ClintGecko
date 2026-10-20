package com.jhaiian.clint.browser.delegates
import com.jhaiian.clint.browser.MainActivity

import android.annotation.SuppressLint
import com.jhaiian.clint.tabs.BrowserTab
import com.jhaiian.clint.profiles.WebProfiles
import com.jhaiian.clint.tabs.InactiveTabsPolicy
import com.jhaiian.clint.tabs.SavedTab
import com.jhaiian.clint.tabs.TabSessionManager
import com.jhaiian.clint.tabs.TabThumbnailCache
import com.jhaiian.clint.browser.webview.ClintAutofill
import com.jhaiian.clint.browser.webview.ClintWebChromeClient
import com.jhaiian.clint.browser.webview.ClintWebViewClient

internal fun MainActivity.saveTabs() {
    tabManager.touchActive()
    val activeTab = tabManager.activeTab
    val restoreActiveId = if (activeTab != null && tabManager.isGhostTab(activeTab)) {
        activeTab.previousTabId
    } else {
        null
    }
    val savedTabs = tabManager.tabs
        .filter { !it.isIncognito && !it.isRefreshLinkTab && !tabManager.isGhostTab(it) }
        .mapIndexedNotNull { index, tab ->
            val url = tab.pendingUrl?.takeIf { it.isNotEmpty() && it != "about:blank" }
                ?: tab.webView.url?.takeIf { it.isNotEmpty() && it != "about:blank" }
                ?: tab.url.takeIf { it.isNotEmpty() && it != "about:blank" }
                ?: return@mapIndexedNotNull null
            SavedTab(
                position = index,
                url = url,
                title = tab.title,
                isActive = if (restoreActiveId != null) tab.id == restoreActiveId else tab == tabManager.activeTab,
                tabId = tab.id,
                shortcutId = tab.shortcutId,
                lastActiveAt = tab.lastActiveAt,
                profileId = tab.profileId,
                groupId = tab.groupId
            )
        }
    val savedGroupIds = savedTabs.mapNotNull { it.groupId }.toSet()
    val savedGroups = tabManager.groups.filter { !it.isIncognito && it.id in savedGroupIds }
    Thread { TabSessionManager.save(this, savedTabs, savedGroups) }.start()
    persistAllShortcutTabGroups()
}

internal fun MainActivity.restoreTabs(): Boolean {
    migrateTabsFromPrefsIfNeeded()
    val savedTabs = TabSessionManager.load(this).filter { it.shortcutId == null }
    if (savedTabs.isEmpty()) return false
    val activeIndex = savedTabs.indexOfFirst { it.isActive }.coerceAtLeast(0)
    val referencedGroupIds = savedTabs.mapNotNull { it.groupId }.toSet()
    TabSessionManager.loadGroups(this)
        .filter { it.id in referencedGroupIds }
        .forEach { tabManager.restoreGroup(it) }
    savedTabs.forEachIndexed { index, saved ->
        openNewTabSilent(saved.url, saved.tabId, saved.shortcutId, deferLoad = index != activeIndex, title = saved.title, profileId = saved.profileId, groupId = saved.groupId)
    }
    tabManager.pruneEmptyGroups()
    val activeTabId = savedTabs[activeIndex].tabId
    tabManager.switchTo(tabManager.tabs.indexOfFirst { it.id == activeTabId }.coerceAtLeast(0))
    savedTabs.forEach { saved ->
        tabManager.tabs.firstOrNull { it.id == saved.tabId }?.lastActiveAt = saved.lastActiveAt
    }
    attachActiveWebView()
    TabThumbnailCache.pruneDisk(this, savedTabs.map { it.tabId }.toSet())
    return true
}

internal fun MainActivity.closeInactiveTabs() {
    val setting = InactiveTabsPolicy.normalize(prefs.getString(InactiveTabsPolicy.PREF_DELETE_INACTIVE_TABS, null))
    if (setting == InactiveTabsPolicy.NEVER) return
    tabManager.touchActive()
    val activeId = tabManager.activeTab?.id ?: return
    val threshold = InactiveTabsPolicy.thresholdMillis(setting)
    val now = System.currentTimeMillis()
    val expired = tabManager.tabs.filter { tab ->
        tab.id != activeId &&
            !tab.isRefreshLinkTab &&
            !tabManager.isGhostTab(tab) &&
            !tab.webView.isMediaPlaying &&
            now - tab.lastActiveAt >= threshold
    }
    if (expired.isEmpty()) return
    expired.forEach { tab ->
        removeDesktopScript(tab)
        com.jhaiian.clint.mediacapture.MediaCaptureStore.removeTab(tab.id)
        if (!tab.isIncognito) com.jhaiian.clint.ui.FaviconCache.evict(this, tab.url)
        TabThumbnailCache.evict(this, tab.id)
        val index = tabManager.tabs.indexOfFirst { it.id == tab.id }
        if (index != -1) tabManager.closeTab(index)
    }
    val activeIndex = tabManager.tabs.indexOfFirst { it.id == activeId }
    if (activeIndex != -1) tabManager.activeIndex = activeIndex
    updateTabCount()
    saveTabs()
}

internal fun MainActivity.captureActiveTabThumbnail() {
    if (uiState.isHomePage) return
    val tab = tabManager.activeTab ?: return
    TabThumbnailCache.capture(tab.id, tab.webView, tab.isIncognito)
}

private fun MainActivity.migrateTabsFromPrefsIfNeeded() {
    if (!TabSessionManager.isEmpty(this)) return
    val savedUrls = prefs.getString("saved_tab_urls", null)
        ?.split("\n")
        ?.filter { it.isNotEmpty() }
        ?: return
    if (savedUrls.isEmpty()) return
    val activeIdx = prefs.getInt("saved_tab_active", 0).coerceIn(0, savedUrls.lastIndex)
    val migratedTabs = savedUrls.mapIndexed { index, url ->
        SavedTab(
            position = index,
            url = url,
            title = "",
            isActive = index == activeIdx,
            tabId = java.util.UUID.randomUUID().toString()
        )
    }
    TabSessionManager.save(this, migratedTabs)
    prefs.edit()
        .remove("saved_tab_urls")
        .remove("saved_tab_active")
        .apply()
}

@SuppressLint("SetJavaScriptEnabled")
internal fun MainActivity.openNewTabSilent(
    url: String,
    id: String = java.util.UUID.randomUUID().toString(),
    shortcutId: String? = null,
    deferLoad: Boolean = false,
    title: String? = null,
    profileId: String = WebProfiles.DEFAULT_ID,
    groupId: String? = null
) {
    val webView = createClintWebView(false, WebProfiles.resolve(this, profileId), deferLoad)
    val tab = BrowserTab(id = id, url = url, shortcutId = shortcutId, profileId = WebProfiles.profileOf(webView), groupId = groupId, webView = webView)
    if (!title.isNullOrBlank()) tab.title = title
    tabManager.add(tab)
    if (isDesktopMode) addDesktopScript(tab)
    addAutoplayScript(tab)
    addDoNotTrackScript(tab)
    addDataSaverMediaScript(tab)
    webView.webViewClient = ClintWebViewClient(
        prefs = prefs,
        isActive = { tabManager.activeTab?.id == tab.id },
        onPageStartedCallback = { url -> if (tabManager.activeTab?.id == tab.id) onPageStarted(url) },
        onPageFinishedCallback = { url -> if (tabManager.activeTab?.id == tab.id) onPageFinished(url) },
        onTabUrlUpdatedCallback = { wv, url -> onTabUrlUpdated(wv, url) },
        onWebsiteBlockedCallback = { blockedUrl -> onWebsiteBlocked(blockedUrl, tab.url, tab.id) },
        getDesktopHeaders = { buildRequestHeaders() },
        getTabId = { tab.id },
        isIncognito = { tab.isIncognito },
        onHttpAuthRequest = { request -> onHttpAuthRequest(request) },
        onSslWarning = { request -> onSslWarningRequest(request) },
        onHttpsOnlyBlocked = { request -> onHttpsOnlyBlockedRequest(request) }
    )
    webView.webChromeClient = ClintWebChromeClient(
        isActive = { tabManager.activeTab?.id == tab.id },
        onTitleChanged = { title ->
            tab.title = title
            if (tabManager.activeTab?.id == tab.id) updateTabCount() else if (uiState.tabStrip != null) refreshTabStrip()
        },
        onProgressChanged = { progress -> if (tabManager.activeTab?.id == tab.id) onProgressChanged(progress) },
        onUrlChanged = { url -> if (tabManager.activeTab?.id == tab.id) updateAddressBar(url) },
        onFullscreenShow = { view, cb -> onShowCustomView(view, cb) },
        onFullscreenHide = { exitFullscreen() },
        onFileChooser = { callback, params -> onShowFileChooser(callback, params) },
        onWebPermissionRequest = { request -> onWebPermissionRequest(request) },
        onGeolocationRequest = { origin, callback -> onWebGeolocationRequest(origin, callback) },
        onJsDialog = { request -> onJsDialogRequest(request) },
        isFullscreenActive = { uiState.isFullscreen },
        onNewWindowRequest = { newUrl ->
            showPopupAlertDialog(newUrl, tab.isIncognito, tab.id)
        }
    )
    if (deferLoad) tab.pendingUrl = url else {
        webView.loadUrl(url)
    }
}

internal fun MainActivity.openNewTabInBackground(url: String, openerTabId: String? = null) {
    val sourceTab = openerTabId?.let { id -> tabManager.tabs.firstOrNull { it.id == id } } ?: tabManager.activeTab
    val sourceProfile = sourceTab?.takeIf { !it.isIncognito }?.profileId ?: WebProfiles.DEFAULT_ID
    val webView = createClintWebView(false, WebProfiles.resolve(this, sourceProfile))
    val inheritedGroupId = sourceTab?.takeIf { !it.isIncognito }?.groupId
    val tab = BrowserTab(url = url, openerTabId = openerTabId, profileId = WebProfiles.profileOf(webView), groupId = inheritedGroupId, webView = webView)
    tabManager.addInBackground(tab)
    if (isDesktopMode) addDesktopScript(tab)
    addAutoplayScript(tab)
    addDoNotTrackScript(tab)
    addDataSaverMediaScript(tab)
    webView.webViewClient = ClintWebViewClient(
        prefs = prefs,
        isActive = { tabManager.activeTab?.id == tab.id },
        onPageStartedCallback = { url -> if (tabManager.activeTab?.id == tab.id) onPageStarted(url) },
        onPageFinishedCallback = { url -> if (tabManager.activeTab?.id == tab.id) onPageFinished(url) },
        onTabUrlUpdatedCallback = { wv, url -> onTabUrlUpdated(wv, url) },
        onWebsiteBlockedCallback = { blockedUrl -> onWebsiteBlocked(blockedUrl, tab.url, tab.id) },
        getDesktopHeaders = { buildRequestHeaders() },
        getTabId = { tab.id },
        isIncognito = { tab.isIncognito },
        onHttpAuthRequest = { request -> onHttpAuthRequest(request) },
        onSslWarning = { request -> onSslWarningRequest(request) },
        onHttpsOnlyBlocked = { request -> onHttpsOnlyBlockedRequest(request) }
    )
    webView.webChromeClient = ClintWebChromeClient(
        isActive = { tabManager.activeTab?.id == tab.id },
        onTitleChanged = { title ->
            tab.title = title
            if (tabManager.activeTab?.id == tab.id) updateTabCount() else if (uiState.tabStrip != null) refreshTabStrip()
        },
        onProgressChanged = { progress -> if (tabManager.activeTab?.id == tab.id) onProgressChanged(progress) },
        onUrlChanged = { url -> if (tabManager.activeTab?.id == tab.id) updateAddressBar(url) },
        onFullscreenShow = { view, cb -> onShowCustomView(view, cb) },
        onFullscreenHide = { exitFullscreen() },
        onFileChooser = { callback, params -> onShowFileChooser(callback, params) },
        onWebPermissionRequest = { request -> onWebPermissionRequest(request) },
        onGeolocationRequest = { origin, callback -> onWebGeolocationRequest(origin, callback) },
        onJsDialog = { request -> onJsDialogRequest(request) },
        isFullscreenActive = { uiState.isFullscreen },
        onNewWindowRequest = { newUrl ->
            showPopupAlertDialog(newUrl, false, tab.id)
        }
    )
    webView.loadUrl(url)
    updateTabCount()
}

internal fun MainActivity.activeProfileId(): String =
    tabManager.activeTab?.takeIf { !it.isIncognito }?.profileId ?: WebProfiles.DEFAULT_ID

internal fun MainActivity.openNewTab(isIncognito: Boolean, url: String = getHomepageUrl(), openerTabId: String? = null, shortcutId: String? = null, previousTabId: String? = null, profileId: String? = null, groupId: String? = null) {
    captureActiveTabThumbnail()
    val resolvedGroupId = groupId ?: openerTabId
        ?.let { id -> tabManager.tabs.firstOrNull { it.id == id } }
        ?.takeIf { it.isIncognito == isIncognito && shortcutId == null }
        ?.groupId
    val requestedProfile = when {
        isIncognito -> WebProfiles.DEFAULT_ID
        profileId != null -> profileId
        else -> openerTabId
            ?.let { id -> tabManager.tabs.firstOrNull { it.id == id } }
            ?.takeIf { !it.isIncognito }
            ?.profileId
            ?: WebProfiles.DEFAULT_ID
    }
    val webView = createClintWebView(isIncognito, WebProfiles.resolve(this, requestedProfile))
    val tab = BrowserTab(isIncognito = isIncognito, openerTabId = openerTabId, shortcutId = shortcutId, previousTabId = previousTabId, profileId = WebProfiles.profileOf(webView), groupId = resolvedGroupId, webView = webView)
    val index = tabManager.add(tab)
    if (isDesktopMode) addDesktopScript(tab)
    addAutoplayScript(tab)
    addDoNotTrackScript(tab)
    addDataSaverMediaScript(tab)
    webView.webViewClient = ClintWebViewClient(
        prefs = prefs,
        isActive = { tabManager.activeTab?.id == tab.id },
        onPageStartedCallback = { url -> if (tabManager.activeTab?.id == tab.id) onPageStarted(url) },
        onPageFinishedCallback = { url -> if (tabManager.activeTab?.id == tab.id) onPageFinished(url) },
        onTabUrlUpdatedCallback = { wv, url -> onTabUrlUpdated(wv, url) },
        onWebsiteBlockedCallback = { blockedUrl -> onWebsiteBlocked(blockedUrl, tab.url, tab.id) },
        getDesktopHeaders = { buildRequestHeaders() },
        getTabId = { tab.id },
        isIncognito = { tab.isIncognito },
        onHttpAuthRequest = { request -> onHttpAuthRequest(request) },
        onSslWarning = { request -> onSslWarningRequest(request) },
        onHttpsOnlyBlocked = { request -> onHttpsOnlyBlockedRequest(request) }
    )
    webView.webChromeClient = ClintWebChromeClient(
        isActive = { tabManager.activeTab?.id == tab.id },
        onTitleChanged = { title ->
            tab.title = title
            if (tabManager.activeTab?.id == tab.id) updateTabCount() else if (uiState.tabStrip != null) refreshTabStrip()
        },
        onProgressChanged = { progress -> if (tabManager.activeTab?.id == tab.id) onProgressChanged(progress) },
        onUrlChanged = { url -> if (tabManager.activeTab?.id == tab.id) updateAddressBar(url) },
        onFullscreenShow = { view, cb -> onShowCustomView(view, cb) },
        onFullscreenHide = { exitFullscreen() },
        onFileChooser = { callback, params -> onShowFileChooser(callback, params) },
        onWebPermissionRequest = { request -> onWebPermissionRequest(request) },
        onGeolocationRequest = { origin, callback -> onWebGeolocationRequest(origin, callback) },
        onJsDialog = { request -> onJsDialogRequest(request) },
        isFullscreenActive = { uiState.isFullscreen },
        onNewWindowRequest = { newUrl ->
            showPopupAlertDialog(newUrl, isIncognito, tab.id)
        }
    )
    tabManager.switchTo(index)
    attachActiveWebView()
    loadUrl(url)
}

internal fun MainActivity.switchToTabMode(incognito: Boolean) {
    val index = tabManager.tabs.indexOfLast {
        it.isIncognito == incognito && !it.isRefreshLinkTab && !tabManager.isGhostTab(it)
    }
    if (index == -1) {
        openNewTab(isIncognito = incognito)
        return
    }
    captureActiveTabThumbnail()
    tabManager.switchTo(index)
    attachActiveWebView()
}

internal fun MainActivity.attachActiveWebView() {
    val tab = tabManager.activeTab ?: return
    tab.lastActiveAt = System.currentTimeMillis()
    ClintAutofill.cancel(this)
    webContainer.removeAllViews()
    (tab.webView.parent as? android.view.ViewGroup)?.removeView(tab.webView)
    webContainer.addView(tab.webView, android.view.ViewGroup.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT
    ))
    autoDesktopPendingReload = null
    syncDesktopModeForUrl(tab.pendingUrl ?: tab.webView.url ?: tab.url)
    tab.pendingUrl?.let { pending ->
        tab.pendingUrl = null
        tab.webView.loadUrl(pending)
    }
    updateIncognitoState(tab.isIncognito)
    uiState.activeTabId = tab.id
    com.jhaiian.clint.extensions.ClintExtensions.setActiveSession(tab.webView.session)
    updateSwipeRefreshColors(tab.isIncognito)
    updateTabCount()
    updateAddressBar(tab.webView.url ?: tab.url)
    uiState.pageLoadProgress = tab.webView.progress
    uiState.isPageLoading = tab.webView.progress < 100
    updateNavigationState()
    updateBookmarkIcon()
    nestedScrollActive = false
    canvasTouchActive = false
    hasWebBottomNav = false
    animateBottomBarTo(0f, animated = false)
    attachScrollListener(tab.webView)
    injectScrollTracker(tab.webView)
    syncTabPlayback()
}

internal fun MainActivity.syncTabPlayback() {
    val activeId = tabManager.activeTab?.id
    tabManager.tabs.forEach { tab ->
        if (tab.id == activeId) {
            tab.webView.onResume()
        } else if (tab.pendingUrl == null) {
            tab.webView.onPause()
        }
    }
    trimBackgroundTabs()
}

internal fun MainActivity.focusTabById(tabId: String) {
    val index = tabManager.tabs.indexOfFirst { it.id == tabId }
    if (index < 0 || index == tabManager.activeIndex) return
    onTabSelected(index)
}

internal fun MainActivity.toggleTabMute(tabId: String) {
    val tab = tabManager.tabs.firstOrNull { it.id == tabId } ?: return
    tab.webView.setAudioMuted(!tab.webView.audioState.muted)
}

@SuppressLint("SetJavaScriptEnabled")
internal fun MainActivity.openRefreshLinkTab(url: String) {
    captureActiveTabThumbnail()
    val webView = createClintWebView(false)
    val tab = BrowserTab(url = url, isRefreshLinkTab = true, webView = webView)
    val index = tabManager.add(tab)
    if (isDesktopMode) addDesktopScript(tab)
    addAutoplayScript(tab)
    addDoNotTrackScript(tab)
    addDataSaverMediaScript(tab)
    webView.webViewClient = ClintWebViewClient(
        prefs = prefs,
        isActive = { tabManager.activeTab?.id == tab.id },
        onPageStartedCallback = { u -> if (tabManager.activeTab?.id == tab.id) onPageStarted(u) },
        onPageFinishedCallback = { u -> if (tabManager.activeTab?.id == tab.id) onPageFinished(u) },
        onTabUrlUpdatedCallback = { wv, u -> onTabUrlUpdated(wv, u) },
        onWebsiteBlockedCallback = { blockedUrl -> onWebsiteBlocked(blockedUrl, tab.url, tab.id) },
        getDesktopHeaders = { buildRequestHeaders() },
        getTabId = { tab.id },
        isIncognito = { tab.isIncognito },
        onHttpAuthRequest = { request -> onHttpAuthRequest(request) },
        onSslWarning = { request -> onSslWarningRequest(request) },
        onHttpsOnlyBlocked = { request -> onHttpsOnlyBlockedRequest(request) }
    )
    webView.webChromeClient = ClintWebChromeClient(
        isActive = { tabManager.activeTab?.id == tab.id },
        onTitleChanged = { title ->
            tab.title = title
            if (tabManager.activeTab?.id == tab.id) updateTabCount() else if (uiState.tabStrip != null) refreshTabStrip()
        },
        onProgressChanged = { progress -> if (tabManager.activeTab?.id == tab.id) onProgressChanged(progress) },
        onUrlChanged = { u -> if (tabManager.activeTab?.id == tab.id) updateAddressBar(u) },
        onFullscreenShow = { view, cb -> onShowCustomView(view, cb) },
        onFullscreenHide = { exitFullscreen() },
        onFileChooser = { callback, params -> onShowFileChooser(callback, params) },
        onWebPermissionRequest = { request -> onWebPermissionRequest(request) },
        onGeolocationRequest = { origin, callback -> onWebGeolocationRequest(origin, callback) },
        onJsDialog = { request -> onJsDialogRequest(request) },
        isFullscreenActive = { uiState.isFullscreen },
        onNewWindowRequest = { newUrl ->
            showPopupAlertDialog(newUrl, false, tab.id)
        }
    )
    tabManager.switchTo(index)
    attachActiveWebView()
    loadUrl(url)
}

internal fun MainActivity.cleanupRefreshLinkTabs() {
    val previousIndex = refreshLinkSession?.previousTabIndex ?: -1
    val indices = tabManager.tabs.indices.filter { tabManager.tabs[it].isRefreshLinkTab }.reversed()
    for (i in indices) {
        tabManager.closeTab(i)
    }
    resetProgressBar()
    if (tabManager.tabs.isEmpty()) {
        openNewTab(isIncognito = false, url = getHomepageUrl())
        return
    }
    val targetIndex = when {
        previousIndex in tabManager.tabs.indices -> previousIndex
        else -> (tabManager.tabs.size - 1).coerceAtLeast(0)
    }
    tabManager.switchTo(targetIndex)
    attachActiveWebView()
}

internal fun MainActivity.closePopupTabToOpener(tab: BrowserTab): Boolean {
    val openerId = tab.openerTabId ?: return false
    val openerIndex = tabManager.tabs.indexOfFirst { it.id == openerId }
    if (openerIndex !in tabManager.tabs.indices) return false
    val closingIndex = tabManager.tabs.indexOfFirst { it.id == tab.id }
    if (closingIndex !in tabManager.tabs.indices) return false

    removeDesktopScript(tab)
    com.jhaiian.clint.mediacapture.MediaCaptureStore.removeTab(tab.id)
    if (!tab.isIncognito) com.jhaiian.clint.ui.FaviconCache.evict(this, tab.url)
    TabThumbnailCache.evict(this, tab.id)
    tabManager.closeTab(closingIndex)
    resetProgressBar()

    val restoredIndex = tabManager.tabs.indexOfFirst { it.id == openerId }
    if (restoredIndex !in tabManager.tabs.indices) return false
    tabManager.switchTo(restoredIndex)
    attachActiveWebView()
    return true
}
