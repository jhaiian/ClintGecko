package com.jhaiian.clint.browser.delegates
import com.jhaiian.clint.settings.datasaver.DataSaverBridge
import com.jhaiian.clint.settings.datasaver.DataSaverMode
import androidx.compose.ui.graphics.toArgb
import com.jhaiian.clint.ui.theme.ThemeMode
import com.jhaiian.clint.browser.webview.*
import com.jhaiian.clint.browser.MainActivity

import android.annotation.SuppressLint
import android.content.SharedPreferences
import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.downloads.DownloadFilenameResolver
import com.jhaiian.clint.mediacapture.MEDIA_CAPTURE_ENABLED_PREF
import com.jhaiian.clint.mediacapture.MediaCaptureDetector
import com.jhaiian.clint.mediacapture.MediaCaptureStore
import com.jhaiian.clint.tabs.BrowserTab
import com.jhaiian.clint.profiles.WebProfiles
import com.jhaiian.clint.util.registeredDomain
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@SuppressLint("SetJavaScriptEnabled")
internal fun MainActivity.createClintWebView(isIncognito: Boolean, profileId: String = WebProfiles.DEFAULT_ID, deferOpen: Boolean = false): ClintWebView {
    val webView = ClintWebView(this, isIncognito, if (isIncognito || profileId == WebProfiles.DEFAULT_ID) null else profileId, deferOpen && !isIncognito)
    if (!isIncognito) WebProfiles.bind(webView, profileId)
    val settings = webView.settings
    settings.javaScriptEnabled = prefs.getBoolean("javascript_enabled", true)
    settings.domStorageEnabled = !isIncognito
    settings.userAgentString = buildUserAgent()
    ClintAutofill.apply(webView, isIncognito, prefs.getBoolean(ClintAutofill.PREF_AUTOFILL_ENABLED, ClintAutofill.DEFAULT_AUTOFILL_ENABLED))
    webView.addJavascriptInterface(NestedScrollBridge(), "NestedScrollBridge")
    webView.addJavascriptInterface(CanvasTouchBridge(), "CanvasTouchBridge")
    webView.addJavascriptInterface(BottomNavBridge(), "BottomNavBridge")
    webView.addJavascriptInterface(NotificationBridge(webView), "ClintNotificationBridge")
    webView.clipboardPermissionHandler = { uri, onResult -> showWebClipboardPermission(webView, uri, onResult) }
    webView.clipboardDismissHandler = { dismissWebClipboardPermission() }
    webView.addJavascriptInterface(BlobDownloadBridge(), "BlobDownloadBridge")
    webView.addJavascriptInterface(DateTimePickerBridge(this, webView), "DateTimePickerBridge")
    webView.addJavascriptInterface(ColorPickerBridge(this, webView), "ColorPickerBridge")
    webView.addJavascriptInterface(DataSaverBridge(applicationContext), "ClintDataSaverBridge")
    webView.selectPromptHandler = { title, options, multiple, onConfirm, onCancel ->
        if (tabManager.activeTab?.webView !== webView) {
            onCancel()
        } else {
            uiState.selectPickerRequest?.onCancel?.invoke()
            uiState.selectPickerRequest = com.jhaiian.clint.browser.dialogs.SelectPickerRequest(
                System.nanoTime().toString(), title, options, multiple, java.lang.ref.WeakReference(webView), onConfirm, onCancel
            )
        }
    }
    webView.downloadListener = ClintDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
        if (tabManager.activeTab?.webView !== webView) return@ClintDownloadListener
        val mimetype = mimeType ?: ""
        if (url.startsWith("blob:")) {
            val filename = DownloadFilenameResolver.resolve(url, contentDisposition, mimetype)
            val safeUrl = url.replace("\\", "\\\\").replace("'", "\\'")
            val safeFilename = filename.replace("\\", "\\\\").replace("'", "\\'")
            val safeMime = mimetype.replace("\\", "\\\\").replace("'", "\\'")
            webView.evaluateJavascript("""
                (function(){
                    fetch('$safeUrl')
                        .then(function(r){return r.blob();})
                        .then(function(blob){
                            var reader=new FileReader();
                            reader.onloadend=function(){
                                var b64=reader.result.split(',')[1];
                                BlobDownloadBridge.receiveBlob(b64,'$safeFilename',blob.type||'$safeMime');
                            };
                            reader.readAsDataURL(blob);
                        })
                        .catch(function(e){BlobDownloadBridge.onError(e.toString());});
                })();
            """.trimIndent(), null)
            return@ClintDownloadListener
        }
        val finalName = DownloadFilenameResolver.resolve(url, contentDisposition, mimetype)
        val referer = webView.url ?: ""
        WebProfiles.cookieForAsync(webView, url) { cookies ->
            val session = refreshLinkSession
            if (session != null && tabManager.activeTab?.isRefreshLinkTab == true && isRelatedToSession(url, referer, session)) {
                showRefreshLinkDownloadDialog(url, finalName, userAgent, referer, cookies, session)
            } else {
                showDownloadDialog(url, finalName, userAgent, referer, cookies)
            }
        }
    }
    applyWebDarkMode(webView)
    setupImageLongPress(webView)
    return webView
}

internal fun MainActivity.syncBaseScripts() {
    val names = listOf(
        "link_touch_tracker" to "link_touch_tracker.js",
        "notification_bridge" to "web_notification_bridge.js",
        "date_time_picker" to "date_time_picker.js",
        "color_picker" to "color_picker.js",
        "fullscreen_popup_guard" to "fullscreen_popup_guard.js"
    )
    names.forEach { (id, file) -> ClintGecko.setScript(id, loadJsAsset(file)) }
}

internal fun MainActivity.buildRequestConfig(): org.json.JSONObject {
    val active = DataSaverMode.isActive(prefs)
    val skip = org.json.JSONArray()
    if (active) {
        runCatching {
            com.jhaiian.clint.settings.sitepermissions.SitePermissionManager.getAllByType(
                applicationContext, com.jhaiian.clint.settings.sitepermissions.SitePermissionDatabase.TYPE_DATA_SAVER_EXCEPTION
            ).forEach { skip.put(it.first) }
        }
    }
    val dataSaver = org.json.JSONObject()
        .put("on", active)
        .put("images", prefs.getBoolean("data_saver_disable_images", false))
        .put("fonts", prefs.getBoolean("data_saver_block_fonts", true))
        .put("frames", prefs.getBoolean("data_saver_block_frames", true))
        .put("video", prefs.getBoolean("data_saver_block_video", false))
        .put("scripts", prefs.getBoolean("data_saver_block_scripts", false))
        .put("css", prefs.getBoolean("data_saver_block_css", false))
        .put("header", prefs.getBoolean("data_saver_send_header", true))
        .put("cache", prefs.getBoolean("data_saver_cache_first", false))
        .put("skip", skip)
    return org.json.JSONObject()
        .put("media", prefs.getBoolean(MEDIA_CAPTURE_ENABLED_PREF, true))
        .put("dnt", isDoNotTrackActive())
        .put("ds", dataSaver)
}

internal fun MainActivity.syncRequestConfig() {
    ClintGecko.pushConfig()
}

internal fun MainActivity.installServiceWorkerMediaCapture() {}

internal fun MainActivity.buildDesktopHeaders(): Map<String, String>? = null

internal fun MainActivity.loadDataSaverJs(filename: String): String =
    "(function(){try{var hs=[location.hostname];if(window!==window.top&&document.referrer){hs.push(new URL(document.referrer).hostname);}var b=window.ClintDataSaverBridge;if(b){for(var i=0;i<hs.length;i++){if(b.isExcepted(hs[i])){return;}}}}catch(e){}\n" +
        loadJsAsset(filename) + "\n})();"

internal fun MainActivity.isDataSaverBlockActive(): Boolean =
    DataSaverMode.isActive(prefs) && prefs.getBoolean("data_saver_block_preload", true)

internal fun MainActivity.addDataSaverMediaScript(tab: BrowserTab? = null) {
    ClintGecko.setScript("ds_media", if (isDataSaverBlockActive()) loadDataSaverJs("data_saver_media.js") else null)
}

internal fun MainActivity.isDoNotTrackActive(): Boolean =
    prefs.getBoolean("do_not_track", true)

internal fun MainActivity.buildRequestHeaders(url: String? = null): Map<String, String>? = null

internal fun MainActivity.addDoNotTrackScript(tab: BrowserTab? = null) {
    ClintGecko.setScript("dnt", if (isDoNotTrackActive()) loadJsAsset("do_not_track.js") else null)
}

internal fun MainActivity.removeDoNotTrackScript(tab: BrowserTab? = null) {}

internal fun MainActivity.reloadActiveTabWithHeaders() {
    ClintGecko.pushConfig()
    val webView = tabManager.activeTab?.webView ?: return
    ClintGecko.syncThen { webView.reload() }
}

internal fun MainActivity.applyDoNotTrackSettings() {
    tabManager.tabs.forEach { addDoNotTrackScript(it) }
    reloadActiveTabWithHeaders()
}

internal fun MainActivity.buildUserAgent(): String {
    if (!isDesktopMode) return ClintGecko.defaultUserAgent()
    val major = ClintGecko.geckoVersion.substringBefore('.')
    return "Mozilla/5.0 (X11; Linux x86_64; rv:$major.0) Gecko/20100101 Firefox/$major.0"
}

internal fun MainActivity.applyWebDarkMode(webView: ClintWebView) {
    val theme = prefs.getString("app_theme", "system") ?: "system"
    val chrome = com.jhaiian.clint.customtheme.resolveThemeChrome(
        this,
        if (ThemeMode.isLight(theme)) "light" else "dark",
        prefs.getString("accent_color", "material_you") ?: "material_you",
        prefs.getString("surface_intensity", "soft_tint") ?: "soft_tint"
    )
    webView.setLoadingBackground(chrome.background, true)
    ClintGecko.setDarkMode(
        when {
            ThemeMode.isLight(theme) -> 1
            theme == "dark" -> 2
            else -> 0
        }
    )
}

internal fun MainActivity.applyWebDarkModeToAllTabs() {
    tabManager.tabs.forEach { applyWebDarkMode(it.webView) }
}

internal fun MainActivity.addAutoplayScript(tab: BrowserTab? = null) {
    val active = DataSaverMode.isActive(prefs) && prefs.getBoolean("data_saver_disable_autoplay", true)
    ClintGecko.setScript("autoplay", if (active) loadDataSaverJs("disable_autoplay.js") else null)
}

internal fun MainActivity.applyDataSaverSettings() {
    syncGlobalScripts()
    reloadActiveTabWithHeaders()
}

internal fun MainActivity.applyJavaScript() {
    val enabled = prefs.getBoolean("javascript_enabled", true)
    tabManager.tabs.forEach { it.webView.settings.javaScriptEnabled = enabled }
    tabManager.activeTab?.webView?.reload()
}

internal fun MainActivity.applyCookiePolicy() {
    ClintGecko.setBlockThirdPartyCookies(prefs.getBoolean("block_third_party_cookies", true))
    tabManager.activeTab?.webView?.reload()
}

internal fun MainActivity.applyAutofillSettings() {
    val enabled = prefs.getBoolean(ClintAutofill.PREF_AUTOFILL_ENABLED, ClintAutofill.DEFAULT_AUTOFILL_ENABLED)
    tabManager.tabs.forEach { ClintAutofill.apply(it.webView, it.isIncognito, enabled) }
    ClintAutofill.cancel(this)
}

internal fun MainActivity.applyUserAgentMetadata(webView: ClintWebView) {}

internal fun MainActivity.applyUserAgent() {
    val ua = buildUserAgent()
    tabManager.tabs.forEach {
        it.webView.settings.userAgentString = ua
        applyUserAgentMetadata(it.webView)
    }
    tabManager.activeTab?.webView?.reload()
}

internal fun MainActivity.addDesktopScript(tab: BrowserTab? = null) {
    ClintGecko.setScript("desktop", if (isDesktopMode) loadJsAsset("desktop_mode.js") else null)
}

internal fun MainActivity.removeDesktopScript(tab: BrowserTab? = null) {}

internal fun MainActivity.isDesktopModeSaveStateEnabled(): Boolean =
    prefs.getString(
        com.jhaiian.clint.settings.desktopmode.DesktopModeActivity.PREF_DESKTOP_MODE_SAVE_STATE,
        com.jhaiian.clint.settings.desktopmode.DesktopModeActivity.VALUE_SAVE_STATE
    ) == com.jhaiian.clint.settings.desktopmode.DesktopModeActivity.VALUE_SAVE_STATE

internal fun MainActivity.applyDesktopModeToTabs() {
    val ua = buildUserAgent()
    tabManager.tabs.forEach { tab ->
        tab.webView.settings.userAgentString = ua
        applyUserAgentMetadata(tab.webView)
    }
    addDesktopScript()
}

internal fun MainActivity.syncDesktopModeForUrl(url: String): Boolean {
    if (!url.startsWith("http") || com.jhaiian.clint.browser.home.isClintHomeUrl(url)) return false
    val host = runCatching { android.net.Uri.parse(url).host }.getOrNull()?.takeIf { it.isNotEmpty() } ?: return false
    val shouldSaveState = isDesktopModeSaveStateEnabled()
    val domain = registeredDomain(host)
    val optedOut = tabManager.activeTab?.isIncognito == true && domain in desktopModeOptOutDomains
    val isSaved = shouldSaveState && !optedOut &&
        com.jhaiian.clint.settings.sitepermissions.SitePermissionManager.getState(
            this, host, com.jhaiian.clint.settings.sitepermissions.SitePermissionDatabase.TYPE_DESKTOP_MODE
        ) != null
    val lockedHost = desktopModeHost
    val enable = when {
        isSaved -> true
        !isDesktopMode -> false
        !shouldSaveState || lockedHost == null || lockedHost == com.jhaiian.clint.browser.home.CLINT_HOME_HOST -> true
        else -> registeredDomain(lockedHost) == domain
    }
    desktopModeHost = if (enable) host else null
    if (enable == isDesktopMode) return false
    isDesktopMode = enable
    applyDesktopModeToTabs()
    return true
}

internal fun MainActivity.getHomepageUrl(): String {
    val homepage = prefs.getString(com.jhaiian.clint.browser.home.PREF_HOMEPAGE, com.jhaiian.clint.browser.home.HOMEPAGE_CLINT)
    return when (homepage) {
        com.jhaiian.clint.browser.home.HOMEPAGE_SEARCH_ENGINE -> getSearchEngineHomeUrl()
        com.jhaiian.clint.browser.home.HOMEPAGE_CUSTOM -> com.jhaiian.clint.browser.home.normalizeCustomHomepageUrl(
            prefs.getString(com.jhaiian.clint.browser.home.PREF_HOMEPAGE_CUSTOM_URL, "") ?: ""
        ) ?: com.jhaiian.clint.browser.home.CLINT_HOME_URL
        else -> com.jhaiian.clint.browser.home.CLINT_HOME_URL
    }
}

internal fun MainActivity.getSearchEngineHomeUrl(): String {
    return when (prefs.getString("search_engine", "duckduckgo")) {
        "brave" -> "https://search.brave.com"
        "ecosia" -> "https://www.ecosia.org"
        "google" -> "https://www.google.com"
        "custom" -> com.jhaiian.clint.browser.customSearchEngineHomeUrl(prefs)
        else -> "https://duckduckgo.com"
    }
}

internal fun MainActivity.getSearchQueryUrl(query: String): String {
    val encoded = android.net.Uri.encode(query)
    return when (prefs.getString("search_engine", "duckduckgo")) {
        "brave" -> "https://search.brave.com/search?q=$encoded"
        "ecosia" -> "https://www.ecosia.org/search?q=$encoded"
        "google" -> "https://www.google.com/search?q=$encoded"
        "custom" -> com.jhaiian.clint.browser.customSearchEngineQueryUrl(prefs, encoded)
        else -> "https://duckduckgo.com/?q=$encoded"
    }
}

private fun isRelatedToSession(
    downloadUrl: String,
    pageUrl: String,
    session: MainActivity.RefreshLinkSession
): Boolean {
    val originalDownloadDomain = session.originalUrl.toHttpUrlOrNull()?.host
        ?.let { registeredDomain(it) }
    val originalRefererDomain = session.originalReferer.toHttpUrlOrNull()?.host
        ?.let { registeredDomain(it) }

    if (originalDownloadDomain == null && originalRefererDomain == null) return true

    val newDownloadDomain = downloadUrl.toHttpUrlOrNull()?.host
        ?.let { registeredDomain(it) }
    val newPageDomain = pageUrl.toHttpUrlOrNull()?.host
        ?.let { registeredDomain(it) }

    val originalDomains = setOfNotNull(originalDownloadDomain, originalRefererDomain)
    return newDownloadDomain in originalDomains || newPageDomain in originalDomains
}

internal fun MainActivity.addBackgroundPlaybackScript() {
    val enabled = com.jhaiian.clint.media.MediaPlaybackSettings.isBackgroundPlaybackEnabled(this)
    ClintGecko.setScript("backgroundPlayback", if (enabled) loadJsAsset("background_playback.js") else null)
}

internal fun MainActivity.syncGlobalScripts() {
    addBackgroundPlaybackScript()
    addDesktopScript()
    addAutoplayScript()
    addDoNotTrackScript()
    addDataSaverMediaScript()
}
