package com.jhaiian.clint.browser.delegates
import androidx.compose.ui.graphics.toArgb
import com.jhaiian.clint.browser.home.CLINT_HOME_HOST
import com.jhaiian.clint.browser.webview.ClintWebView
import com.jhaiian.clint.browser.home.isClintHomeUrl
import com.jhaiian.clint.ui.theme.ThemeMode
import com.jhaiian.clint.browser.*
import com.jhaiian.clint.browser.suggestions.SuggestionFetcher
import com.jhaiian.clint.settings.datasaver.DataSaverMode
import com.jhaiian.clint.browser.webview.loadJsAsset
import android.content.Context
import android.Manifest
import android.util.TypedValue
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.jhaiian.clint.R
import com.jhaiian.clint.bookmarks.Bookmark
import com.jhaiian.clint.bookmarks.BookmarkManager
import com.jhaiian.clint.history.HistoryItem
import com.jhaiian.clint.history.SearchHistoryManager

private const val SUGGESTION_HISTORY_LIMIT = 20
private const val SUGGESTION_BOOKMARK_LIMIT = 10

internal fun MainActivity.applyAddressBarPosition() {
    val position = prefs.getString("address_bar_position", "top") ?: "top"
    uiState.addressBarPosition = when (position) {
        "top" -> AddressBarPosition.TOP
        "bottom" -> AddressBarPosition.BOTTOM
        else -> AddressBarPosition.SPLIT
    }
    uiState.topBarFraction = 0f
    uiState.bottomBarFraction = 0f
    uiState.topBarFullHeightPx = 0
    uiState.bottomBarFullHeightPx = 0
    updateMainContentInsets()
}

internal fun MainActivity.setupAddressBar() {
    suggestionFetcher = SuggestionFetcher()
    val bgThread = android.os.HandlerThread("ClintSuggestions").also { it.start() }
    suggestionsBgThread = bgThread
    suggestionsBgHandler = android.os.Handler(bgThread.looper)
}

private fun combineSuggestions(
    bookmarks: List<Bookmark>,
    history: List<HistoryItem>,
    suggestions: List<String>
): List<SuggestionItem> {
    val seenUrls = mutableSetOf<String>()
    val items = mutableListOf<SuggestionItem>()
    bookmarks.forEach {
        seenUrls.add(it.url)
        items.add(SuggestionItem(it.url, it.title.ifBlank { it.url }, SuggestionType.BOOKMARK))
    }
    history.forEach {
        if (seenUrls.add(it.query)) {
            items.add(SuggestionItem(it.query, it.title.ifBlank { it.query }, SuggestionType.HISTORY))
        }
    }
    suggestions.forEach {
        if (seenUrls.add(it)) {
            items.add(SuggestionItem(it, it, SuggestionType.SUGGESTION))
        }
    }
    return items
}

internal fun MainActivity.openSearchOverlay(isBottom: Boolean) {
    uiState.searchOverlayIsBottom = isBottom
    val current = tabManager.activeTab?.webView?.url?.takeUnless { isClintHomeUrl(it) } ?: ""
    uiState.searchOverlayOpen = true
    onSearchQueryChanged(current)
}

internal fun MainActivity.closeSearchOverlay() {
    uiState.searchOverlayOpen = false
    suggestionFetcher?.cancel()
    lastOnlineSuggestions = emptyList()
    uiState.suggestions = emptyList()
    updateAddressBar(tabManager.activeTab?.url ?: "")
}

internal fun MainActivity.onSearchQueryChanged(query: String) {
    uiState.searchQuery = query
    val bgHandler = suggestionsBgHandler ?: return
    bgHandler.removeCallbacksAndMessages(null)
    bgHandler.post {
        val isIncognito = tabManager.activeTab?.isIncognito == true
        val incognitoHistoryAllowed = prefs.getBoolean("incognito_search_history_enabled", false)
        val historyAllowed = !isIncognito || incognitoHistoryAllowed
        if (query.isBlank()) {
            suggestionFetcher?.cancel()
            lastOnlineSuggestions = emptyList()
            val history = if (historyAllowed) SearchHistoryManager.getAll(this).take(SUGGESTION_HISTORY_LIMIT) else emptyList()
            val bookmarks = BookmarkManager.getAll(this).take(SUGGESTION_BOOKMARK_LIMIT)
            runOnUiThread { uiState.suggestions = combineSuggestions(bookmarks, history, emptyList()) }
            return@post
        }
        val suggestionsApi = prefs.getString("search_suggestions_api", "duckduckgo") ?: "duckduckgo"
        val onlineBlocked = suggestionsApi == "off" || (DataSaverMode.isActive(prefs) && prefs.getBoolean("data_saver_disable_suggestions", true))
        if (onlineBlocked) {
            suggestionFetcher?.cancel()
            lastOnlineSuggestions = emptyList()
        }
        val history = if (historyAllowed) SearchHistoryManager.search(this, query).take(SUGGESTION_HISTORY_LIMIT) else emptyList()
        val bookmarks = BookmarkManager.search(this, query).take(SUGGESTION_BOOKMARK_LIMIT)
        runOnUiThread { uiState.suggestions = combineSuggestions(bookmarks, history, lastOnlineSuggestions) }
        if (onlineBlocked) return@post
        val customSuggestionsUrl = if (suggestionsApi == "custom") {
            customSearchSuggestionsApiQueryUrl(prefs, android.net.Uri.encode(query))
        } else null
        suggestionFetcher?.fetch(query, suggestionsApi, customSuggestionsUrl) { suggestions ->
            lastOnlineSuggestions = suggestions
            runOnUiThread { uiState.suggestions = combineSuggestions(bookmarks, history, suggestions) }
        }
    }
}

internal fun MainActivity.onSuggestionHistoryDelete(query: String) {
    val bgHandler = suggestionsBgHandler ?: return
    bgHandler.post {
        SearchHistoryManager.delete(this, query)
        val currentQuery = uiState.searchQuery
        val history = SearchHistoryManager.search(this, currentQuery).take(SUGGESTION_HISTORY_LIMIT)
        val bookmarks = BookmarkManager.search(this, currentQuery).take(SUGGESTION_BOOKMARK_LIMIT)
        runOnUiThread { uiState.suggestions = combineSuggestions(bookmarks, history, emptyList()) }
    }
}

internal fun MainActivity.onSearchSubmitted(input: String) {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return
    val formatted = formatUrl(trimmed)
    setAddressBarText(formatted, isBottom = uiState.searchOverlayIsBottom)
    uiState.searchOverlayOpen = false
    suggestionFetcher?.cancel()
    lastOnlineSuggestions = emptyList()
    uiState.suggestions = emptyList()
    if (tabManager.activeTab?.isIncognito != true) {
        Thread { SearchHistoryManager.add(this, trimmed) }.start()
    }
    loadUrl(trimmed)
}

internal fun MainActivity.onSuggestionChosen(query: String) {
    val formatted = formatUrl(query)
    setAddressBarText(formatted, isBottom = uiState.searchOverlayIsBottom)
    uiState.searchOverlayOpen = false
    suggestionFetcher?.cancel()
    lastOnlineSuggestions = emptyList()
    uiState.suggestions = emptyList()
    if (tabManager.activeTab?.isIncognito != true) {
        SearchHistoryManager.add(this, query)
    }
    loadUrl(query)
}

private fun MainActivity.setAddressBarText(formatted: String, isBottom: Boolean) {
    val secure = formatted.startsWith("https://")
    if (isBottom) {
        uiState.addressBarTextBottom = formatted
        uiState.addressBarSecureBottom = secure
    } else {
        uiState.addressBarTextTop = formatted
        uiState.addressBarSecureTop = secure
    }
}

internal fun MainActivity.navGoBack() { tabManager.activeTab?.webView?.let { if (it.canGoBack()) it.goBack() } }
internal fun MainActivity.navGoForward() { tabManager.activeTab?.webView?.let { if (it.canGoForward()) it.goForward() } }
internal fun MainActivity.navGoHome() { loadUrl(getHomepageUrl()) }
internal fun MainActivity.navRefreshOrStop() {
    tabManager.activeTab?.webView?.let { wv ->
        if (uiState.isPageLoading) { wv.stopLoading(); onPageFinished(wv.url ?: "") } else { wv.reload(true) }
    }
}
internal fun MainActivity.navToggleBookmark() {
    val url = tabManager.activeTab?.webView?.url?.takeUnless { isClintHomeUrl(it) } ?: return
    val title = tabManager.activeTab?.title ?: url
    if (BookmarkManager.isBookmarked(this, url)) {
        BookmarkManager.remove(this, url)
        updateBookmarkIcon()
    } else {
        uiState.pendingBookmarkUrl = url
        uiState.pendingBookmarkTitle = title
        Thread {
            val tree = com.jhaiian.clint.bookmarks.buildFolderTree(BookmarkManager.getAllFoldersFlat(this))
            runOnUiThread {
                uiState.bookmarkFolderTree = tree
                uiState.bookmarkFolderDialogOpen = true
            }
        }.start()
    }
}

internal fun MainActivity.confirmPendingBookmark(folderId: Long?) {
    val url = uiState.pendingBookmarkUrl
    val title = uiState.pendingBookmarkTitle
    uiState.bookmarkFolderDialogOpen = false
    if (url.isEmpty()) return
    Thread {
        BookmarkManager.add(this, Bookmark(url = url, title = title, folderId = folderId))
        runOnUiThread { updateBookmarkIcon() }
    }.start()
}

internal fun MainActivity.dismissBookmarkFolderDialog() {
    uiState.bookmarkFolderDialogOpen = false
}

internal fun MainActivity.loadUrl(input: String) {
    val url = formatUrl(input)
    val wv = tabManager.activeTab?.webView ?: return
    tabManager.activeTab?.url = url
    uiState.isHomePage = isClintHomeUrl(url)
    val headers = buildRequestHeaders(url)
    if (headers != null) wv.loadUrl(url, headers) else wv.loadUrl(url)
    hideKeyboardOnly()
}

internal fun MainActivity.formatUrl(input: String): String {
    val t = input.trim()
    return when {
        t.equals("about:config", ignoreCase = true) && prefs.getBoolean("about_config_enabled", false) -> "about:config"
        t.startsWith("http://") || t.startsWith("https://") || t.startsWith("moz-extension://") -> t
        t.contains(".") && !t.contains(" ") -> {
            val host = t.substringBefore("/").substringBefore(":")
            val isIpAddress = host.matches(Regex("""^(\d{1,3}\.){3}\d{1,3}$"""))
            if (isIpAddress) "http://$t" else "https://$t"
        }
        else -> getSearchQueryUrl(t)
    }
}

internal fun MainActivity.updateAddressBar(url: String) {
    val isHome = isClintHomeUrl(url)
    uiState.isHomePage = isHome
    if (uiState.searchOverlayOpen) return
    val shown = if (isHome) "" else url
    val secure = shown.startsWith("https://")
    uiState.addressBarTextTop = shown
    uiState.addressBarSecureTop = secure
    uiState.addressBarTextBottom = shown
    uiState.addressBarSecureBottom = secure
}

internal fun MainActivity.onTabUrlUpdated(webView: ClintWebView, url: String) {
    tabManager.tabs.find { it.webView === webView }?.url = url
    if (tabManager.activeTab?.webView === webView && !uiState.searchOverlayOpen) {
        updateAddressBar(url)
    }
}

internal fun MainActivity.onPageStarted(url: String) {
    swipeRefreshView.isRefreshing = false
    updateAddressBar(url)
    uiState.isPageLoading = true
    uiState.pageLoadProgress = 0
    updateNavigationState()
    if (hasWebBottomNav) {
        hasWebBottomNav = false
        animateBottomBarTo(0f, animated = true)
    }


    if (url.startsWith("http") && !isClintHomeUrl(url)) {
        if (url == autoDesktopPendingReload) {
            autoDesktopPendingReload = null
        } else {
            val host = runCatching { android.net.Uri.parse(url).host }.getOrNull()
            if (host != null) {
                val shouldSaveState = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
                    .getString(
                        com.jhaiian.clint.settings.desktopmode.DesktopModeActivity.PREF_DESKTOP_MODE_SAVE_STATE,
                        com.jhaiian.clint.settings.desktopmode.DesktopModeActivity.VALUE_SAVE_STATE
                    ) == com.jhaiian.clint.settings.desktopmode.DesktopModeActivity.VALUE_SAVE_STATE

                val isSaved = shouldSaveState && com.jhaiian.clint.settings.sitepermissions.SitePermissionManager
                    .getState(this, host, com.jhaiian.clint.settings.sitepermissions.SitePermissionDatabase.TYPE_DESKTOP_MODE) != null

                val hostDomain = registeredDomain(host)
                val lockedDomain = desktopModeHost?.let { registeredDomain(it) }

                when {
                    isSaved && !isDesktopMode -> {
                        isDesktopMode = true
                        desktopModeHost = host
                        tabManager.tabs.forEach { tab ->
                            tab.webView.settings.userAgentString = buildUserAgent()
                            applyUserAgentMetadata(tab.webView)
                            addDesktopScript(tab)
                        }
                        tabManager.activeTab?.webView?.let { wv ->
                            autoDesktopPendingReload = url
                            wv.reload()
                        }
                    }
                    isSaved && isDesktopMode && host != desktopModeHost -> {
                        desktopModeHost = host
                    }
                    !isSaved && isDesktopMode && host != desktopModeHost -> {
                        if (hostDomain == lockedDomain || !shouldSaveState || desktopModeHost == CLINT_HOME_HOST) {
                            desktopModeHost = host
                        } else {
                            isDesktopMode = false
                            desktopModeHost = null
                            tabManager.tabs.forEach { tab ->
                                tab.webView.settings.userAgentString = buildUserAgent()
                                applyUserAgentMetadata(tab.webView)
                                addDesktopScript(tab)
                            }
                            tabManager.activeTab?.webView?.reload()
                        }
                    }
                }
            }
        }
    }
}

internal fun MainActivity.onPageFinished(url: String) {
    swipeRefreshView.isRefreshing = false
    updateAddressBar(url)
    uiState.isPageLoading = false
    updateNavigationState()
    tabManager.activeTab?.webView?.let { wv ->
        injectScrollTracker(wv)
        injectBottomNavDetector(wv)
        injectCanvasTouchDetector(wv)
        wv.evaluateJavascript(loadJsAsset("link_touch_tracker.js"), null)
    }
    nestedScrollActive = false
    canvasTouchActive = false
    updateBookmarkIcon()

    tabManager.activeTab?.let { tab ->
        com.jhaiian.clint.tabs.TabThumbnailCache.capture(tab.id, tab.webView, tab.isIncognito)
    }

    val activeTab = tabManager.activeTab
    if (activeTab?.isIncognito != true && url.startsWith("http") && !isClintHomeUrl(url) && !SearchHistoryManager.isSearchEngineUrl(applicationContext, url)) {
        val title = activeTab?.webView?.title ?: ""
        Thread {
            SearchHistoryManager.add(applicationContext, url, title)
            if (com.jhaiian.clint.bookmarks.BookmarkManager.isBookmarked(applicationContext, url)) {
                com.jhaiian.clint.bookmarks.BookmarkManager.updateLastVisit(applicationContext, url)
            }
        }.start()
    }
}

internal fun MainActivity.onProgressChanged(progress: Int) {
    uiState.pageLoadProgress = progress
    uiState.isPageLoading = progress < 100
}

internal fun MainActivity.resetProgressBar() {
    uiState.pageLoadProgress = 0
    uiState.isPageLoading = false
}

internal fun MainActivity.updateBookmarkIcon() {
    val url = tabManager.activeTab?.webView?.url?.takeUnless { isClintHomeUrl(it) } ?: ""
    uiState.hasActiveUrl = url.isNotEmpty()
    uiState.isBookmarked = url.isNotEmpty() && BookmarkManager.isBookmarked(this, url)
}

internal fun MainActivity.updateNavigationState() {
    val wv = tabManager.activeTab?.webView
    uiState.canGoBack = wv?.canGoBack() == true
    uiState.canGoForward = wv?.canGoForward() == true
}

internal fun MainActivity.updateTabCount() {
    val count = tabManager.previews().size
    uiState.tabCountText = if (count > 99) ":D" else count.toString()
}

internal fun MainActivity.updateIncognitoState(isIncognito: Boolean) {

    uiState.isIncognito = isIncognito
}

internal fun MainActivity.updateMediaCaptureEnabledState() {
    uiState.isMediaCaptureEnabled = prefs.getBoolean(
        com.jhaiian.clint.mediacapture.MEDIA_CAPTURE_ENABLED_PREF, true
    )
}

internal fun MainActivity.updateHomeButtonState() {
    uiState.showHomeButton = prefs.getBoolean("show_home_button", true)
}

internal fun MainActivity.updateSwipeRefreshColors(isIncognito: Boolean) {
    val chrome = com.jhaiian.clint.customtheme.resolveThemeChrome(
        this,
        if (com.jhaiian.clint.ui.theme.ThemeMode.isLight(prefs.getString("app_theme", "system") ?: "system")) "light" else "dark",
        prefs.getString("accent_color", "material_you") ?: "material_you",
        prefs.getString("surface_intensity", "soft_tint") ?: "soft_tint"
    )
    swipeRefreshView.setProgressBackgroundColorSchemeColor(chrome.cardBackground)
    swipeRefreshView.setColorSchemeColors(chrome.primary)
}

internal fun MainActivity.hideKeyboard() {
    hideKeyboardOnly()
}

private fun MainActivity.hideKeyboardOnly() {
    if (uiState.searchOverlayOpen) closeSearchOverlay()
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
    imm.hideSoftInputFromWindow(window.decorView.windowToken, 0)
}

internal fun MainActivity.getThemeColor(attrId: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attrId, typedValue, true)
    return typedValue.data
}

internal fun MainActivity.handleVoiceSearchTap() {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        == android.content.pm.PackageManager.PERMISSION_GRANTED) {
        launchVoiceSearch()
    } else {
        uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
            title = getString(R.string.voice_search_permission_title),
            message = getString(R.string.voice_search_permission_message),
            positiveLabel = getString(android.R.string.ok),
            onPositive = { microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
            negativeLabel = getString(android.R.string.cancel)
        )
    }
}

private fun registeredDomain(host: String): String =
    com.jhaiian.clint.util.registeredDomain(host)

internal fun MainActivity.launchVoiceSearch() {
    val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            android.speech.RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
    }
    runCatching { voiceSearchLauncher.launch(intent) }.onFailure {
        Toast.makeText(this, getString(R.string.voice_search_not_available), Toast.LENGTH_SHORT).show()
    }
}
