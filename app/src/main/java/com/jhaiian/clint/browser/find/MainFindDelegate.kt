package com.jhaiian.clint.browser.find

import android.view.KeyEvent
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.SuggestionItem
import com.jhaiian.clint.browser.SuggestionType
import com.jhaiian.clint.browser.delegates.clipboardText
import com.jhaiian.clint.browser.delegates.closeSearchOverlay
import com.jhaiian.clint.browser.delegates.handleVoiceSearchTap
import com.jhaiian.clint.browser.delegates.updateMainContentInsets
import com.jhaiian.clint.browser.home.isClintHomeUrl
import com.jhaiian.clint.browser.webview.ClintWebView
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.GeckoSession

private const val SEARCH_DEBOUNCE_MS = 180L
private const val SCAN_DEBOUNCE_MS = 350L
private const val RECENT_LIMIT = 8
private const val MIN_RECENT_LENGTH = 2

private class FindScan(val count: Int, val hits: List<FindHit>, val ticks: List<Float>, val invalid: Boolean)

internal fun MainActivity.canFindInPage(): Boolean {
    val url = tabManager.activeTab?.webView?.url
    return !url.isNullOrEmpty() && url != "about:blank" && !isClintHomeUrl(url)
}

internal fun MainActivity.openFind(initialQuery: String? = null) {
    val wv = tabManager.activeTab?.webView ?: return
    if (!canFindInPage()) return
    val find = uiState.find
    if (uiState.searchOverlayOpen) closeSearchOverlay()
    if (find.target !== wv) {
        releaseFindTarget()
        find.target = wv
    }
    if (!find.open) loadFindPrefs()
    find.open = true
    wv.setFindDisplay(prefs.getBoolean(PREF_FIND_DIM_PAGE, false))
    find.recent = readFindRecent()
    if (initialQuery != null) find.query = initialQuery
    if (find.query.isNotEmpty()) startFind()
}

internal fun MainActivity.closeFind() {
    val find = uiState.find
    if (!find.open && find.target == null) return
    if (find.total > 0) rememberFindQuery()
    cancelFindWork()
    releaseFindTarget()
    find.open = false
    find.query = ""
    find.current = 0
    find.total = 0
    find.hits = emptyList()
    find.hitTotal = 0
    find.ticks = emptyList()
    find.listOpen = false
    find.optionsOpen = false
    find.barHeightPx = 0
    find.searching = false
    find.invalidPattern = false
    updateMainContentInsets()
}

internal fun MainActivity.closeFindForTab(webView: ClintWebView) {
    val find = uiState.find
    if (find.open && find.target !== webView) closeFind()
}

private fun MainActivity.releaseFindTarget() {
    val find = uiState.find
    find.target?.let {
        it.clearFind()
        if (find.regex) it.clearScanned()
    }
    find.target = null
}

private fun MainActivity.cancelFindWork() {
    val find = uiState.find
    find.searchRunnable?.let { find.handler.removeCallbacks(it) }
    find.scanRunnable?.let { find.handler.removeCallbacks(it) }
    find.searchRunnable = null
    find.scanRunnable = null
    find.scanToken++
    find.jumpToken++
}

private fun MainActivity.loadFindPrefs() {
    val find = uiState.find
    val remember = prefs.getBoolean(PREF_FIND_REMEMBER_OPTIONS, true)
    find.matchCase = remember && prefs.getBoolean(PREF_FIND_MATCH_CASE, false)
    find.wholeWord = remember && prefs.getBoolean(PREF_FIND_WHOLE_WORD, false)
    find.linksOnly = remember && prefs.getBoolean(PREF_FIND_LINKS_ONLY, false)
    find.regex = remember && prefs.getBoolean(PREF_FIND_REGEX, false)
    if (find.regex) {
        find.wholeWord = false
        find.linksOnly = false
    }
    find.barAtTop = prefs.getBoolean(PREF_FIND_BAR_TOP, false)
    find.showScrollMarkers = prefs.getBoolean(PREF_FIND_SCROLL_MARKERS, true)
}

private fun MainActivity.persistFindOptions() {
    if (!prefs.getBoolean(PREF_FIND_REMEMBER_OPTIONS, true)) return
    val find = uiState.find
    prefs.edit()
        .putBoolean(PREF_FIND_MATCH_CASE, find.matchCase)
        .putBoolean(PREF_FIND_WHOLE_WORD, find.wholeWord)
        .putBoolean(PREF_FIND_LINKS_ONLY, find.linksOnly)
        .putBoolean(PREF_FIND_REGEX, find.regex)
        .apply()
}

private fun nativeFlags(find: FindUiState, backwards: Boolean): Int {
    var flags = if (backwards) GeckoSession.FINDER_FIND_BACKWARDS else GeckoSession.FINDER_FIND_FORWARD
    if (find.matchCase) flags = flags or GeckoSession.FINDER_FIND_MATCH_CASE
    if (find.wholeWord) flags = flags or GeckoSession.FINDER_FIND_WHOLE_WORD
    if (find.linksOnly) flags = flags or GeckoSession.FINDER_FIND_LINKS_ONLY
    return flags
}

private fun applyNativeResult(find: FindUiState, result: GeckoSession.FinderResult?) {
    if (result == null || !result.found) {
        find.current = 0
        find.total = 0
        return
    }
    find.current = result.current
    find.total = maxOf(result.total, result.current)
}

private fun MainActivity.resetFindResults() {
    val find = uiState.find
    find.scanToken++
    find.current = 0
    find.total = 0
    find.hits = emptyList()
    find.hitTotal = 0
    find.ticks = emptyList()
    find.invalidPattern = false
    find.searching = false
}

internal fun MainActivity.startFind() {
    val find = uiState.find
    cancelFindWork()
    if (find.query.isEmpty()) {
        resetFindResults()
        find.target?.let {
            it.clearFind()
            if (find.regex) it.clearScanned()
        }
        return
    }
    if (!find.regex) {
        val search = Runnable { performFind(forward = true) }
        find.searchRunnable = search
        find.handler.postDelayed(search, SEARCH_DEBOUNCE_MS)
    }
    val scan = Runnable { performScan() }
    find.scanRunnable = scan
    find.handler.postDelayed(scan, if (find.regex) SEARCH_DEBOUNCE_MS else SCAN_DEBOUNCE_MS)
}

private fun MainActivity.performFind(forward: Boolean) {
    val find = uiState.find
    val wv = find.target ?: return
    val query = find.query
    if (query.isEmpty() || find.regex) return
    wv.findInPage(query, nativeFlags(find, !forward)) { result ->
        if (find.target !== wv || find.query != query || find.regex) return@findInPage
        applyNativeResult(find, result)
    }
}

private fun MainActivity.performScan() {
    val find = uiState.find
    val wv = find.target ?: return
    val query = find.query
    if (query.isEmpty()) return
    val token = ++find.scanToken
    val regex = find.regex
    find.searching = true
    wv.scanPage(query, regex, find.matchCase, find.wholeWord) { raw ->
        runOnUiThread {
            if (token != find.scanToken || find.target !== wv) return@runOnUiThread
            find.searching = false
            val scan = parseFindScan(raw)
            if (scan == null) {
                find.hits = emptyList()
                find.hitTotal = 0
                find.ticks = emptyList()
                if (regex) {
                    find.current = 0
                    find.total = 0
                }
                return@runOnUiThread
            }
            find.invalidPattern = regex && scan.invalid
            find.hits = scan.hits
            find.hitTotal = scan.count
            find.ticks = scan.ticks
            if (regex) {
                find.total = scan.count
                find.current = if (scan.count > 0) 1 else 0
                if (scan.count > 0) wv.selectScanned(0) {}
            }
        }
    }
}

private fun parseFindScan(raw: String?): FindScan? {
    if (raw.isNullOrEmpty()) return null
    val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null
    val items = json.optJSONArray("items") ?: JSONArray()
    val y = json.optJSONArray("y") ?: JSONArray()
    val hits = (0 until items.length()).mapNotNull { i ->
        items.optJSONObject(i)?.let { FindHit(it.optString("b"), it.optString("m"), it.optString("a")) }
    }
    val ticks = (0 until y.length()).map { y.optDouble(it, 0.0).toFloat() }
    return FindScan(json.optInt("n", 0), hits, ticks, json.optBoolean("err", false))
}

internal fun MainActivity.setFindQuery(query: String) {
    val find = uiState.find
    if (find.query == query && find.open) return
    find.query = query
    startFind()
}

internal fun MainActivity.setFindMatchCase(enabled: Boolean) {
    uiState.find.matchCase = enabled
    persistFindOptions()
    startFind()
}

internal fun MainActivity.setFindWholeWord(enabled: Boolean) {
    uiState.find.wholeWord = enabled
    persistFindOptions()
    startFind()
}

internal fun MainActivity.setFindLinksOnly(enabled: Boolean) {
    uiState.find.linksOnly = enabled
    persistFindOptions()
    startFind()
}

internal fun MainActivity.setFindRegex(enabled: Boolean) {
    val find = uiState.find
    if (find.regex == enabled) return
    find.target?.let {
        if (enabled) it.clearFind() else it.clearScanned()
    }
    find.regex = enabled
    if (enabled) {
        find.wholeWord = false
        find.linksOnly = false
    }
    persistFindOptions()
    resetFindResults()
    startFind()
}

internal fun MainActivity.findStep(forward: Boolean) {
    val find = uiState.find
    if (find.query.isEmpty() || find.target == null) return
    find.searchRunnable?.let { find.handler.removeCallbacks(it) }
    find.searchRunnable = null
    find.jumpToken++
    rememberFindQuery()
    if (find.regex) {
        val total = find.total
        if (total <= 0) return
        selectRegexHit(((find.current - 1) + (if (forward) 1 else -1) + total) % total)
    } else {
        performFind(forward)
    }
}

private fun MainActivity.selectRegexHit(index: Int) {
    val find = uiState.find
    val wv = find.target ?: return
    find.current = index + 1
    wv.selectScanned(index) {}
}

internal fun MainActivity.findJumpTo(index: Int) {
    val find = uiState.find
    val wv = find.target ?: return
    find.listOpen = false
    rememberFindQuery()
    if (find.regex) selectRegexHit(index) else jumpNative(wv, index + 1)
}

private fun MainActivity.jumpNative(wv: ClintWebView, target: Int) {
    val find = uiState.find
    val total = find.total
    if (total <= 0) return
    val current = find.current
    if (current == target) return
    val forwardSteps = ((target - current) % total + total) % total
    val backwardSteps = total - forwardSteps
    val backwards = backwardSteps < forwardSteps
    val token = ++find.jumpToken
    val query = find.query
    fun step(left: Int) {
        if (left <= 0 || token != find.jumpToken || find.target !== wv || find.query != query) return
        wv.findInPage(query, nativeFlags(find, backwards)) { result ->
            if (token != find.jumpToken) return@findInPage
            applyNativeResult(find, result)
            step(left - 1)
        }
    }
    step(minOf(forwardSteps, backwardSteps))
}

internal fun MainActivity.onFindPageLoading(loading: Boolean) {
    val find = uiState.find
    if (!find.open) return
    if (loading) {
        cancelFindWork()
        resetFindResults()
    } else if (find.query.isNotEmpty()) {
        find.target?.setFindDisplay(prefs.getBoolean(PREF_FIND_DIM_PAGE, false))
        startFind()
    }
}

internal fun MainActivity.findPaste() {
    clipboardText()?.let { setFindQuery(it.take(200)) }
}

internal fun MainActivity.findVoiceInput() {
    uiState.find.voicePending = true
    handleVoiceSearchTap()
}

internal fun MainActivity.findUseRecent(query: String) {
    setFindQuery(query)
}

private fun MainActivity.findHistoryAllowed(): Boolean =
    tabManager.activeTab?.isIncognito != true && prefs.getBoolean(PREF_FIND_SAVE_RECENT, true)

internal fun MainActivity.readFindRecent(): List<String> {
    if (!findHistoryAllowed()) return emptyList()
    val array = runCatching { JSONArray(prefs.getString(PREF_FIND_RECENT, "[]")) }.getOrNull() ?: return emptyList()
    return (0 until array.length()).mapNotNull { array.optString(it).takeIf { text -> text.isNotEmpty() } }
}

private fun MainActivity.rememberFindQuery() {
    val query = uiState.find.query.trim()
    if (query.length < MIN_RECENT_LENGTH || !findHistoryAllowed()) return
    val list = (listOf(query) + readFindRecent().filterNot { it.equals(query, ignoreCase = true) }).take(RECENT_LIMIT)
    prefs.edit().putString(PREF_FIND_RECENT, JSONArray(list).toString()).apply()
    uiState.find.recent = list
}

internal fun MainActivity.clearFindRecent() {
    prefs.edit().remove(PREF_FIND_RECENT).apply()
    uiState.find.recent = emptyList()
}

internal fun MainActivity.findSuggestionFor(query: String): List<SuggestionItem> {
    val text = query.trim()
    if (text.isEmpty() || text.startsWith("http://") || text.startsWith("https://")) return emptyList()
    if (!prefs.getBoolean(PREF_FIND_ADDRESS_BAR_SUGGESTION, true) || !canFindInPage()) return emptyList()
    return listOf(SuggestionItem(text, getString(R.string.find_suggestion, text), SuggestionType.FIND_IN_PAGE))
}

internal fun MainActivity.onSuggestionFindChosen(query: String) {
    closeSearchOverlay()
    openFind(query)
}

internal fun MainActivity.handleFindShortcut(event: KeyEvent): Boolean {
    val find = uiState.find
    if (event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_F) {
        openFind()
        return find.open
    }
    if (!find.open) return false
    return when {
        event.keyCode == KeyEvent.KEYCODE_ESCAPE -> {
            closeFind()
            true
        }
        event.keyCode == KeyEvent.KEYCODE_F3 || (event.isCtrlPressed && event.keyCode == KeyEvent.KEYCODE_G) -> {
            findStep(forward = !event.isShiftPressed)
            true
        }
        else -> false
    }
}
