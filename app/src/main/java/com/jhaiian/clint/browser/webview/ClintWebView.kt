package com.jhaiian.clint.browser.webview

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.dialogs.JsDialogType
import com.jhaiian.clint.browser.home.CLINT_HOME_URL
import com.jhaiian.clint.browser.home.isClintHomeUrl
import com.jhaiian.clint.media.MediaPlaybackController
import com.jhaiian.clint.media.TabAudioState
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.BasicSelectionActionDelegate
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.MediaSession
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebRequestError
import org.mozilla.geckoview.WebResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

typealias PromptResponse = GeckoSession.PromptDelegate.PromptResponse

fun interface ClintDownloadListener {
    fun onDownloadStart(url: String, userAgent: String, contentDisposition: String?, mimeType: String?, contentLength: Long)
}

class ClintWebView(
    context: Context,
    val isPrivate: Boolean = false,
    val profileId: String? = null,
    deferOpen: Boolean = false
) : FrameLayout(context) {

    val geckoView = object : GeckoView(context) {
        override fun onCreateInputConnection(outAttrs: android.view.inputmethod.EditorInfo): android.view.inputmethod.InputConnection? {
            val connection = super.onCreateInputConnection(outAttrs)
            if (isPrivate && com.jhaiian.clint.privacy.IncognitoKeyboard.isEnabled(context)) {
                com.jhaiian.clint.privacy.IncognitoKeyboard.apply(outAttrs)
            }
            return connection
        }
    }
    val session: GeckoSession
    private val cover = android.view.View(context).apply {
        visibility = android.view.View.GONE
        importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private val hideCoverRunnable = Runnable { hideCover() }
    private var loadingBackground = 0
    private var coverEnabled = false
    private var initialCoverPending = true
    private var pagePainted = false
    val settings = ClintWebSettings(this)

    var webViewClient: ClintWebViewClient? = null
    var webChromeClient: ClintWebChromeClient? = null
    var downloadListener: ClintDownloadListener? = null
    var contextMenuListener: ((ContextMenuInfo) -> Unit)? = null
    var scrollListener: ((Int, Int) -> Unit)? = null
    var touchObserver: ((MotionEvent) -> Unit)? = null
    var cookieStoreId: String? = null
    var selectPromptHandler: ((String, List<com.jhaiian.clint.browser.dialogs.SelectPickerOption>, Boolean, (List<String>) -> Unit, () -> Unit) -> Unit)? = null
    var autofillAllowed: Boolean = true
        set(value) {
            field = value
            geckoView.setAutofillEnabled(value)
            geckoView.importantForAutofill = if (value) {
                android.view.View.IMPORTANT_FOR_AUTOFILL_YES
            } else {
                android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            }
        }
    var clipboardPermissionHandler: ((String, (Boolean) -> Unit) -> Unit)? = null
    var clipboardDismissHandler: (() -> Unit)? = null

    @Volatile var rawUrl: String? = null
        private set
    private var currentUrl: String? = null
    private var currentTitle: String? = null
    private var lastFailedUrl: String? = null
    private var awaitingRealLoad = false
    private var homeShown = false
    var progress: Int = 0
        private set
    var isSecure: Boolean = false
        private set
    private var canGoBackState = false
    private var canGoForwardState = false
    private var scrollYValue = 0
    private var destroyed = false
    private var extensionAttached = false
    private var lastSessionState: GeckoSession.SessionState? = null
    private var recoveryPending = false
    private var neverOpened = deferOpen
    private var paused = false

    val url: String? get() = currentUrl
    val originalUrl: String? get() = currentUrl
    val title: String? get() = currentTitle
    val bridges = ConcurrentHashMap<String, Any>()
    val audioState = TabAudioState()
    val isMediaPlaying: Boolean get() = MediaPlaybackController.isPlaying(this)

    private val evalSeq = AtomicInteger()
    private val pendingEval = ConcurrentHashMap<String, (String?) -> Unit>()
    private val queuedEvals = ArrayList<JSONObject>()
    private var topPort: WebExtension.Port? = null
    private val ports = ArrayList<WebExtension.Port>()

    fun attachExtension(ext: WebExtension) {
        if (extensionAttached || destroyed) return
        extensionAttached = true
        session.webExtensionController.setMessageDelegate(ext, contentMessageDelegate, ClintGecko.CONTENT_APP)
    }

    private val contentMessageDelegate = object : WebExtension.MessageDelegate {
        override fun onConnect(port: WebExtension.Port) {
            val top = runCatching { port.sender.isTopLevel }.getOrDefault(false)
            ports.add(port)
            if (audioState.muted) port.postMessage(muteMessage(true))
            port.setDelegate(object : WebExtension.PortDelegate {
                override fun onPortMessage(message: Any, port: WebExtension.Port) {}
                override fun onDisconnect(port: WebExtension.Port) {
                    ports.remove(port)
                    if (topPort === port) topPort = null
                }
            })
            if (top) {
                topPort = port
                val queued = ArrayList(queuedEvals)
                queuedEvals.clear()
                queued.forEach { port.postMessage(it) }
            }
        }

        override fun onMessage(nativeApp: String, message: Any, sender: WebExtension.MessageSender): GeckoResult<Any>? {
            val msg = message as? JSONObject ?: return null
            return when (msg.optString("k")) {
                "call" -> handleCall(msg)
                "evalRes" -> {
                    pendingEval.remove(msg.optString("id"))?.invoke(msg.optString("v"))
                    null
                }
                "nonce" -> {
                    ClintGecko.noteNonce(msg.optString("n"), this@ClintWebView)
                    null
                }
                else -> null
            }
        }
    }

    private fun handleCall(msg: JSONObject): GeckoResult<Any> {
        val result = GeckoResult<Any>()
        val iface = msg.optString("i")
        val method = msg.optString("m")
        val args = msg.optJSONArray("a") ?: JSONArray()
        ClintGecko.bridgeExecutor.execute {
            val out = JSONObject()
            runCatching {
                val value = invokeBridge(iface, method, args)
                if (value != null) out.put("r", value)
            }
            result.complete(out)
        }
        return result
    }

    private fun invokeBridge(iface: String, method: String, args: JSONArray): Any? {
        val target = bridges[iface] ?: return null
        val m = target.javaClass.methods.firstOrNull {
            it.name == method && it.parameterTypes.size == args.length() && it.isAnnotationPresent(JavascriptInterface::class.java)
        } ?: return null
        m.isAccessible = true
        val params = Array<Any?>(args.length()) { i ->
            val raw = args.opt(i)
            val isNull = raw == null || raw == JSONObject.NULL
            when (m.parameterTypes[i]) {
                String::class.java -> if (isNull) null else raw.toString()
                Boolean::class.javaPrimitiveType, Boolean::class.javaObjectType -> if (isNull) false else (raw as? Boolean ?: raw.toString().toBoolean())
                Int::class.javaPrimitiveType, Int::class.javaObjectType -> args.optInt(i)
                Long::class.javaPrimitiveType, Long::class.javaObjectType -> args.optLong(i)
                Double::class.javaPrimitiveType, Double::class.javaObjectType -> args.optDouble(i)
                Float::class.javaPrimitiveType, Float::class.javaObjectType -> args.optDouble(i).toFloat()
                else -> raw
            }
        }
        return m.invoke(target, *params)
    }

    fun addJavascriptInterface(obj: Any, name: String) {
        bridges[name] = obj
        ClintGecko.registerInterface(name, obj)
    }

    private fun muteMessage(muted: Boolean): JSONObject =
        JSONObject().put("k", "eval").put("code", TabMediaControl.muteScript(muted)).put("id", "")

    fun setAudioMuted(muted: Boolean) {
        audioState.muted = muted
        MediaPlaybackController.applyNativeMute(this, muted)
        val message = muteMessage(muted)
        ArrayList(ports).forEach { runCatching { it.postMessage(message) } }
    }

    fun reapplyAudioMute() {
        if (audioState.muted) MediaPlaybackController.applyNativeMute(this, true)
    }

    fun onMediaIdle() {
        if (!destroyed && session.isOpen && (paused || !isAttachedToWindow)) session.setActive(false)
    }

    fun evaluateJavascript(script: String, callback: ((String?) -> Unit)?) {
        val id = if (callback != null) evalSeq.incrementAndGet().toString() else ""
        if (callback != null) {
            pendingEval[id] = callback
            ClintGecko.main.postDelayed({ pendingEval.remove(id)?.invoke(null) }, 5000)
        }
        dispatchToPage(JSONObject().put("k", "eval").put("code", script).put("id", id))
    }

    fun extractReaderContent(callback: (String?) -> Unit) {
        val id = evalSeq.incrementAndGet().toString()
        pendingEval[id] = callback
        ClintGecko.main.postDelayed({ pendingEval.remove(id)?.invoke(null) }, 8000)
        dispatchToPage(JSONObject().put("k", "reader").put("id", id))
    }

    private fun dispatchToPage(msg: JSONObject) {
        val port = topPort
        if (port != null) {
            port.postMessage(msg)
        } else {
            if (queuedEvals.size >= 20) queuedEvals.removeAt(0)
            queuedEvals.add(msg)
        }
    }

    fun loadUrl(url: String) {
        ClintGecko.whenReady { if (!destroyed) doLoad(url) }
    }

    fun loadUrl(url: String, headers: Map<String, String>) = loadUrl(url)

    fun loadUrlDeferred(url: String) {
        ClintGecko.main.post { loadUrl(url) }
    }

    fun loadHtml(html: String) {
        ClintGecko.whenReady {
            if (destroyed) return@whenReady
            ensureOpen()
            val encoded = Base64.encodeToString(html.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            session.load(GeckoSession.Loader().uri(ERROR_PAGE_PREFIX + encoded))
        }
    }

    private fun doLoad(url: String) {
        if (isClintHomeUrl(url)) {
            showHome()
            return
        }
        lastFailedUrl = null
        awaitingRealLoad = true
        if (!pagePainted) showCover()
        ensureOpen()
        session.loadUri(url)
    }

    private fun showHome() {
        homeShown = true
        pagePainted = false
        awaitingRealLoad = false
        currentUrl = CLINT_HOME_URL
        rawUrl = "about:blank"
        currentTitle = context.getString(R.string.new_tab)
        lastFailedUrl = null
        ensureOpen()
        session.loadUri("about:blank")
        ClintGecko.main.post {
            if (destroyed) return@post
            webViewClient?.onPageStarted(this, CLINT_HOME_URL)
            webChromeClient?.onReceivedTitle(this, currentTitle.orEmpty())
            webChromeClient?.onProgressChanged(this, 100)
            webViewClient?.onPageFinished(this, CLINT_HOME_URL)
        }
    }

    fun reload(bypassCache: Boolean = false) {
        if (isClintHomeUrl(currentUrl)) return
        if (recoverIfNeeded()) return
        if (destroyed || !session.isOpen) return
        if (bypassCache) session.reload(org.mozilla.geckoview.GeckoSession.LOAD_FLAGS_BYPASS_CACHE) else session.reload()
    }

    fun stopLoading() {
        if (!destroyed && session.isOpen) session.stop()
    }

    fun onBlockedLoadDismissed() {
        awaitingRealLoad = false
    }

    fun canGoBack(): Boolean = canGoBackState
    fun canGoForward(): Boolean = canGoForwardState
    fun goBack() {
        if (!destroyed && session.isOpen) session.goBack()
    }

    fun goForward() {
        if (!destroyed && session.isOpen) session.goForward()
    }

    fun pauseTimers() {}
    fun resumeTimers() {}

    fun onResume() {
        if (destroyed) return
        paused = false
        ClintGecko.setForeground(this)
        if (recoverIfNeeded()) return
        if (!session.isOpen) return
        session.setActive(true)
        session.setFocused(true)
    }

    fun onPause() {
        paused = true
        ClintGecko.clearForeground(this)
        if (destroyed || !session.isOpen) return
        session.setFocused(false)
        if (!isMediaPlaying) session.setActive(false)
    }

    fun setLoadingBackground(color: Int, enabled: Boolean) {
        loadingBackground = color
        coverEnabled = enabled
        if (!enabled) hideCover()
        cover.setBackgroundColor(color)
        setBackgroundColor(color)
        geckoView.setBackgroundColor(color)
        coverSurface()
        if (enabled && color != 0 && initialCoverPending) {
            initialCoverPending = false
            showCover()
            removeCallbacks(hideCoverRunnable)
            postDelayed(hideCoverRunnable, 10000)
        }
    }

    private fun coverSurface() {
        if (destroyed || loadingBackground == 0) return
        runCatching { geckoView.coverUntilFirstPaint(loadingBackground) }
    }

    private fun showCover() {
        if (destroyed || !coverEnabled || loadingBackground == 0) return
        removeCallbacks(hideCoverRunnable)
        cover.animate().cancel()
        cover.alpha = 1f
        cover.visibility = android.view.View.VISIBLE
        postDelayed(hideCoverRunnable, 4000)
    }

    private fun hideCover() {
        removeCallbacks(hideCoverRunnable)
        if (cover.visibility != android.view.View.VISIBLE) return
        cover.animate().cancel()
        cover.animate().alpha(0f).setDuration(120).withEndAction {
            cover.visibility = android.view.View.GONE
            cover.alpha = 1f
        }.start()
    }

    fun canHibernate(force: Boolean = false): Boolean =
        !destroyed && !isPrivate && session.isOpen && !recoveryPending &&
            lastSessionState != null && progress >= 100 && (force || !isAttachedToWindow) && !isMediaPlaying

    fun hibernate(force: Boolean = false): Boolean {
        if (!canHibernate(force)) return false
        recoveryPending = true
        pagePainted = false
        topPort = null
        ports.clear()
        queuedEvals.clear()
        ClintGecko.clearForeground(this)
        MediaPlaybackController.onViewReleased(this)
        runCatching { geckoView.releaseSession() }
        runCatching { session.close() }
        return true
    }

    fun clearHistory() {
        if (destroyed || !session.isOpen) return
        session.purgeHistory()
    }

    fun clearCache(includeDiskFiles: Boolean = true) {
        if (!isPrivate) ClintGecko.clearCache()
    }

    fun applyUserAgentOverride(userAgent: String?) {
        if (destroyed) return
        session.settings.userAgentOverride = userAgent
    }

    fun applyDesktopMode(enabled: Boolean) {
        session.settings.userAgentMode =
            if (enabled) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP else GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        session.settings.viewportMode =
            if (enabled) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP else GeckoSessionSettings.VIEWPORT_MODE_MOBILE
    }

    fun captureBitmap(callback: (Bitmap?) -> Unit) {
        val result = runCatching { geckoView.capturePixels() }.getOrNull()
        if (result == null) {
            callback(null)
            return
        }
        result.accept({ bmp -> callback(bmp) }, { callback(null) })
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        removeCallbacks(hideCoverRunnable)
        pendingEval.clear()
        queuedEvals.clear()
        ports.clear()
        topPort = null
        bridges.clear()
        lastSessionState = null
        MediaPlaybackController.onViewReleased(this)
        ClintGecko.clearForeground(this)
        ClintGecko.unregisterView(this)
        runCatching { geckoView.releaseSession() }
        runCatching { session.close() }
        runCatching {
            session.contentDelegate = null
            session.navigationDelegate = null
            session.progressDelegate = null
            session.permissionDelegate = null
            session.promptDelegate = null
            session.scrollDelegate = null
            session.mediaSessionDelegate = null
            session.selectionActionDelegate = null
        }
        webViewClient = null
        webChromeClient = null
        downloadListener = null
        contextMenuListener = null
        scrollListener = null
        touchObserver = null
        selectPromptHandler = null
        clipboardPermissionHandler = null
        clipboardDismissHandler = null
        removeAllViews()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        coverSurface()
        paused = false
        if (!destroyed && !(isWindowActive() && recoverIfNeeded()) && session.isOpen) {
            session.setActive(true)
            session.setFocused(true)
        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == android.view.View.VISIBLE && isAttachedToWindow) recoverIfNeeded()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (!destroyed && session.isOpen && !isMediaPlaying) session.setActive(false)
    }

    override fun canScrollVertically(direction: Int): Boolean =
        if (direction < 0) scrollYValue > 0 else true

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        touchObserver?.invoke(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun mapUrl(raw: String?): String? {
        if (raw == null) return null
        rawUrl = raw
        if (raw == "about:blank") return if (homeShown) CLINT_HOME_URL else null
        if (raw.startsWith(ERROR_PAGE_PREFIX) && lastFailedUrl != null) return lastFailedUrl
        return raw
    }

    private fun buildErrorPage(uri: String, error: WebRequestError): String {
        val host = runCatching { Uri.parse(uri).host }.getOrNull().orEmpty().ifEmpty { uri }
        val isCert = error.category == WebRequestError.ERROR_CATEGORY_SECURITY
        val strictCert = isCert && androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(com.jhaiian.clint.privacy.PrivacyPrefs.STRICT_CERTIFICATES, com.jhaiian.clint.privacy.PrivacyPrefs.DEFAULT_STRICT_CERTIFICATES)
        val message = context.getString(
            when {
                strictCert -> R.string.error_page_cert_strict_message
                isCert -> R.string.error_page_cert_message
                else -> R.string.error_page_message
            },
            host
        )
        val js = JSONObject.quote(uri)
        val proceed = if (isCert && !strictCert) {
            "<button class=\"warn\" onclick=\"document.addCertException(false).then(function(){location.href=$js})\">" +
                "${android.text.TextUtils.htmlEncode(context.getString(R.string.error_page_proceed))}</button>"
        } else ""
        val html = "<!doctype html><html><head><meta charset=\"utf-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
            "<meta name=\"color-scheme\" content=\"light dark\">" +
            "<title>${android.text.TextUtils.htmlEncode(context.getString(R.string.error_page_title))}</title>" +
            "<style>body{font-family:sans-serif;margin:0;padding:32px 24px;max-width:520px;margin:auto;line-height:1.5}" +
            "h1{font-size:1.4em}p{opacity:.8}button{font-size:1em;padding:12px 20px;margin:8px 8px 0 0;border-radius:20px;border:1px solid #888;background:transparent;color:inherit}" +
            ".warn{border-color:#c33;color:#c33}</style></head><body>" +
            "<h1>${android.text.TextUtils.htmlEncode(context.getString(R.string.error_page_title))}</h1>" +
            "<p>${android.text.TextUtils.htmlEncode(message)}</p>" +
            "<button onclick=\"location.href=$js\">${android.text.TextUtils.htmlEncode(context.getString(R.string.error_page_retry))}</button>" +
            "<button onclick=\"history.back()\">${android.text.TextUtils.htmlEncode(context.getString(R.string.error_page_back))}</button>" +
            "$proceed</body></html>"
        return ERROR_PAGE_PREFIX + Base64.encodeToString(html.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private val navigationDelegate = object : GeckoSession.NavigationDelegate {
        override fun onLocationChange(
            session: GeckoSession,
            url: String?,
            perms: List<GeckoSession.PermissionDelegate.ContentPermission>,
            hasUserGesture: Boolean
        ) {
            if (url == "about:blank" && (awaitingRealLoad || recoveryPending || !session.isOpen)) return
            if (url != null && url != "about:blank") awaitingRealLoad = false
            val mapped = mapUrl(url) ?: return
            currentUrl = mapped
            webViewClient?.doUpdateVisitedHistory(this@ClintWebView, mapped)
        }

        override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
            canGoBackState = canGoBack
        }

        override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
            canGoForwardState = canGoForward
        }

        override fun onLoadRequest(
            session: GeckoSession,
            request: GeckoSession.NavigationDelegate.LoadRequest
        ): GeckoResult<AllowOrDeny>? {
            if (com.jhaiian.clint.extensions.ClintExtensions.isXpiUrl(request.uri) &&
                com.jhaiian.clint.extensions.ClintExtensions.installFromWeb(request.uri)
            ) {
                return GeckoResult.fromValue(AllowOrDeny.DENY)
            }
            if (request.target == GeckoSession.NavigationDelegate.TARGET_WINDOW_NEW) {
                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }
            val client = webViewClient ?: return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            val deny = runCatching {
                client.shouldOverrideUrlLoading(this@ClintWebView, Uri.parse(request.uri), request.isRedirect)
            }.getOrDefault(false)
            return GeckoResult.fromValue(if (deny) AllowOrDeny.DENY else AllowOrDeny.ALLOW)
        }

        override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
            webChromeClient?.onNewWindow(uri)
            return GeckoResult.fromValue(null)
        }

        override fun onLoadError(session: GeckoSession, uri: String?, error: WebRequestError): GeckoResult<String>? {
            val target = uri ?: return null
            val parsed = Uri.parse(target)
            val lookup = error.code == WebRequestError.ERROR_UNKNOWN_HOST
            if (webViewClient?.onReceivedError(this@ClintWebView, parsed, lookup) == true) return null
            lastFailedUrl = target
            return GeckoResult.fromValue(buildErrorPage(target, error))
        }
    }

    private val progressDelegate = object : GeckoSession.ProgressDelegate {
        override fun onSessionStateChange(session: GeckoSession, sessionState: GeckoSession.SessionState) {
            lastSessionState = sessionState
        }

        override fun onPageStart(session: GeckoSession, url: String) {
            if (url == "about:blank" && (awaitingRealLoad || recoveryPending || !session.isOpen)) return
            progress = 0
            if (url.startsWith(ERROR_PAGE_PREFIX)) hideCover() else if (url != "about:blank" && !isClintHomeUrl(url) && !pagePainted) showCover()
            if (!url.startsWith(ERROR_PAGE_PREFIX)) lastFailedUrl = null
            val mapped = mapUrl(url) ?: return
            webViewClient?.onPageStarted(this@ClintWebView, mapped)
        }

        override fun onPageStop(session: GeckoSession, success: Boolean) {
            if ((awaitingRealLoad && (currentUrl.isNullOrEmpty() || isClintHomeUrl(currentUrl))) || recoveryPending || !session.isOpen) return
            progress = 100
            if (!isClintHomeUrl(currentUrl)) hideCover()
            webChromeClient?.onProgressChanged(this@ClintWebView, 100)
            webViewClient?.onPageFinished(this@ClintWebView, currentUrl.orEmpty())
        }

        override fun onProgressChange(session: GeckoSession, progress: Int) {
            this@ClintWebView.progress = progress
            webChromeClient?.onProgressChanged(this@ClintWebView, progress)
        }

        override fun onSecurityChange(session: GeckoSession, securityInfo: GeckoSession.ProgressDelegate.SecurityInformation) {
            isSecure = securityInfo.isSecure
        }
    }

    private val mediaDelegate = object : MediaSession.Delegate {
        override fun onActivated(session: GeckoSession, mediaSession: MediaSession) {
            MediaPlaybackController.onActivated(this@ClintWebView, mediaSession)
        }

        override fun onDeactivated(session: GeckoSession, mediaSession: MediaSession) {
            MediaPlaybackController.onDeactivated(this@ClintWebView)
        }

        override fun onMetadata(session: GeckoSession, mediaSession: MediaSession, meta: MediaSession.Metadata) {
            MediaPlaybackController.onMetadata(this@ClintWebView, mediaSession, meta.title, meta.artist, meta.album, meta.artwork)
        }

        override fun onFeatures(session: GeckoSession, mediaSession: MediaSession, features: Long) {
            MediaPlaybackController.onFeatures(this@ClintWebView, mediaSession, features)
        }

        override fun onPlay(session: GeckoSession, mediaSession: MediaSession) {
            MediaPlaybackController.onPlay(this@ClintWebView, mediaSession)
        }

        override fun onPause(session: GeckoSession, mediaSession: MediaSession) {
            MediaPlaybackController.onPause(this@ClintWebView, mediaSession)
        }

        override fun onStop(session: GeckoSession, mediaSession: MediaSession) {
            MediaPlaybackController.onPause(this@ClintWebView, mediaSession)
        }

        override fun onPositionState(session: GeckoSession, mediaSession: MediaSession, state: MediaSession.PositionState) {
            MediaPlaybackController.onPositionState(this@ClintWebView, mediaSession, state.duration, state.position, state.playbackRate)
        }
    }

    private val scrollDelegate = object : GeckoSession.ScrollDelegate {
        override fun onScrollChanged(session: GeckoSession, scrollX: Int, scrollY: Int) {
            val old = scrollYValue
            scrollYValue = scrollY
            scrollListener?.invoke(scrollY, old)
        }
    }

    private val contentDelegate = object : GeckoSession.ContentDelegate {
        override fun onFirstContentfulPaint(session: GeckoSession) {
            if (!isClintHomeUrl(currentUrl)) {
                pagePainted = true
                hideCover()
            }
        }

        override fun onTitleChange(session: GeckoSession, title: String?) {
            if (isClintHomeUrl(currentUrl)) return
            currentTitle = title.orEmpty()
            webChromeClient?.onReceivedTitle(this@ClintWebView, currentTitle.orEmpty())
        }

        override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
            if (fullScreen) {
                webChromeClient?.onShowCustomView(this@ClintWebView, CustomViewCallback { session.exitFullScreen() })
            } else {
                webChromeClient?.onHideCustomView()
            }
        }

        override fun onContextMenu(
            session: GeckoSession,
            screenX: Int,
            screenY: Int,
            element: GeckoSession.ContentDelegate.ContextElement
        ) {
            contextMenuListener?.invoke(
                ContextMenuInfo(element.type, element.linkUri, element.srcUri, element.title, element.altText)
            )
        }

        override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
            val headers = response.headers
            fun header(name: String): String? = headers.entries.firstOrNull { it.key.equals(name, true) }?.value
            runCatching { response.body?.close() }
            if (com.jhaiian.clint.extensions.ClintExtensions.isXpiMime(
                    header("Content-Type")?.substringBefore(';')?.trim(),
                    header("Content-Disposition"),
                    response.uri
                ) && com.jhaiian.clint.extensions.ClintExtensions.installFromWeb(response.uri)
            ) return
            downloadListener?.onDownloadStart(
                response.uri,
                settings.userAgentString,
                header("Content-Disposition"),
                header("Content-Type")?.substringBefore(';')?.trim(),
                header("Content-Length")?.toLongOrNull() ?: -1L
            )
        }

        override fun onCloseRequest(session: GeckoSession) {
            webChromeClient?.onNewWindow("about:blank")
        }

        override fun onCrash(session: GeckoSession) {
            com.jhaiian.clint.crash.GeckoCrashReports.onContentProcessGone(context, killed = false)
            markForRecovery()
        }

        override fun onKill(session: GeckoSession) {
            com.jhaiian.clint.crash.GeckoCrashReports.onContentProcessGone(context, killed = true)
            markForRecovery()
        }
    }

    private fun isWindowActive(): Boolean =
        isAttachedToWindow && windowVisibility == android.view.View.VISIBLE

    private fun markForRecovery() {
        if (destroyed) return
        recoveryPending = true
        ClintGecko.main.post {
            if (!destroyed && recoveryPending && isWindowActive()) recoverNow()
        }
    }

    private fun reopenSession() {
        neverOpened = false
        pagePainted = false
        runCatching { geckoView.releaseSession() }
        if (!session.isOpen) session.open(ClintGecko.runtime)
        geckoView.setSession(session)
        coverSurface()
        extensionAttached = false
        ClintGecko.registerView(this)
        com.jhaiian.clint.extensions.ClintExtensions.onSessionReopened(session)
    }

    private fun recoverNow() {
        if (destroyed) return
        recoveryPending = false
        reopenSession()
        val target = currentUrl
        if (!target.isNullOrEmpty() && !isClintHomeUrl(target)) showCover()
        val state = lastSessionState
        awaitingRealLoad = !(target.isNullOrEmpty() || isClintHomeUrl(target))
        when {
            target.isNullOrEmpty() || isClintHomeUrl(target) -> session.loadUri("about:blank")
            state != null -> session.restoreState(state)
            else -> session.loadUri(target)
        }
        session.setActive(true)
        session.setFocused(true)
    }

    private fun ensureOpen() {
        if (destroyed || session.isOpen) return
        recoveryPending = false
        reopenSession()
    }

    private fun recoverIfNeeded(): Boolean {
        if (destroyed || neverOpened) return false
        if (recoveryPending || !session.isOpen) {
            recoverNow()
            return true
        }
        return false
    }

    private val permissionDelegate = object : GeckoSession.PermissionDelegate {
        override fun onContentPermissionRequest(
            session: GeckoSession,
            perm: GeckoSession.PermissionDelegate.ContentPermission
        ): GeckoResult<Int>? {
            val allow = GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
            val deny = GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
            return when (perm.permission) {
                GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION -> {
                    val result = GeckoResult<Int>()
                    val client = webChromeClient
                    if (client == null) {
                        result.complete(deny)
                    } else {
                        client.onGeolocationPermissionsShowPrompt(perm.uri, GeolocationCallback { _, ok, _ ->
                            result.complete(if (ok) allow else deny)
                        })
                    }
                    result
                }
                GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE ->
                    GeckoResult.fromValue(allow)
                GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE ->
                    GeckoResult.fromValue(if (settings.mediaPlaybackRequiresUserGesture) deny else allow)
                GeckoSession.PermissionDelegate.PERMISSION_PERSISTENT_STORAGE,
                GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS ->
                    GeckoResult.fromValue(allow)
                else -> GeckoResult.fromValue(deny)
            }
        }

        override fun onAndroidPermissionsRequest(
            session: GeckoSession,
            permissions: Array<out String>?,
            callback: GeckoSession.PermissionDelegate.Callback
        ) {
            val granted = permissions.orEmpty().all {
                ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            if (granted) callback.grant() else callback.reject()
        }

        override fun onMediaPermissionRequest(
            session: GeckoSession,
            uri: String,
            video: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
            audio: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
            callback: GeckoSession.PermissionDelegate.MediaCallback
        ) {
            val client = webChromeClient
            val resources = ArrayList<String>()
            if (!video.isNullOrEmpty()) resources.add(ClintPermissionRequest.RESOURCE_VIDEO_CAPTURE)
            if (!audio.isNullOrEmpty()) resources.add(ClintPermissionRequest.RESOURCE_AUDIO_CAPTURE)
            if (client == null || resources.isEmpty()) {
                callback.reject()
                return
            }
            client.onPermissionRequest(
                ClintPermissionRequest(
                    origin = Uri.parse(uri),
                    resources = resources.toTypedArray(),
                    onGrant = { granted ->
                        val v = if (ClintPermissionRequest.RESOURCE_VIDEO_CAPTURE in granted) video?.firstOrNull() else null
                        val a = if (ClintPermissionRequest.RESOURCE_AUDIO_CAPTURE in granted) audio?.firstOrNull() else null
                        if (v == null && a == null) callback.reject() else callback.grant(v, a)
                    },
                    onDeny = { callback.reject() }
                )
            )
        }
    }

    private val promptDelegate = object : GeckoSession.PromptDelegate {
        private fun dialog(
            type: JsDialogType,
            message: String?,
            default: String,
            onConfirm: (String) -> Unit,
            onCancel: () -> Unit
        ) {
            val client = webChromeClient
            if (client == null) {
                onCancel()
                return
            }
            client.dispatchJsDialog(type, currentUrl.orEmpty(), message.orEmpty(), default, onConfirm, onCancel)
        }

        override fun onAlertPrompt(session: GeckoSession, prompt: GeckoSession.PromptDelegate.AlertPrompt): GeckoResult<PromptResponse>? {
            val result = GeckoResult<PromptResponse>()
            dialog(JsDialogType.Alert, prompt.message, "", { result.complete(prompt.dismiss()) }, { result.complete(prompt.dismiss()) })
            return result
        }

        override fun onButtonPrompt(session: GeckoSession, prompt: GeckoSession.PromptDelegate.ButtonPrompt): GeckoResult<PromptResponse>? {
            val result = GeckoResult<PromptResponse>()
            dialog(
                JsDialogType.Confirm, prompt.message, "",
                { result.complete(prompt.confirm(GeckoSession.PromptDelegate.ButtonPrompt.Type.POSITIVE)) },
                { result.complete(prompt.confirm(GeckoSession.PromptDelegate.ButtonPrompt.Type.NEGATIVE)) }
            )
            return result
        }

        override fun onTextPrompt(session: GeckoSession, prompt: GeckoSession.PromptDelegate.TextPrompt): GeckoResult<PromptResponse>? {
            val result = GeckoResult<PromptResponse>()
            dialog(
                JsDialogType.Prompt, prompt.message, prompt.defaultValue.orEmpty(),
                { value -> result.complete(prompt.confirm(value)) },
                { result.complete(prompt.dismiss()) }
            )
            return result
        }

        override fun onBeforeUnloadPrompt(session: GeckoSession, prompt: GeckoSession.PromptDelegate.BeforeUnloadPrompt): GeckoResult<PromptResponse>? {
            val result = GeckoResult<PromptResponse>()
            dialog(
                JsDialogType.BeforeUnload, prompt.title, "",
                { result.complete(prompt.confirm(AllowOrDeny.ALLOW)) },
                { result.complete(prompt.confirm(AllowOrDeny.DENY)) }
            )
            return result
        }

        override fun onPopupPrompt(session: GeckoSession, prompt: GeckoSession.PromptDelegate.PopupPrompt): GeckoResult<PromptResponse>? =
            GeckoResult.fromValue(prompt.confirm(AllowOrDeny.ALLOW))

        override fun onAuthPrompt(session: GeckoSession, prompt: GeckoSession.PromptDelegate.AuthPrompt): GeckoResult<PromptResponse>? {
            val result = GeckoResult<PromptResponse>()
            val client = webClientOrNull()
            if (client == null) {
                result.complete(prompt.dismiss())
                return result
            }
            val host = runCatching { Uri.parse(prompt.authOptions.uri).host }.getOrNull() ?: currentUrl.orEmpty()
            client.onAuthRequest(
                this@ClintWebView, host, prompt.message,
                { user, pass -> result.complete(prompt.confirm(user, pass)) },
                { result.complete(prompt.dismiss()) }
            )
            return result
        }

        override fun onChoicePrompt(session: GeckoSession, prompt: GeckoSession.PromptDelegate.ChoicePrompt): GeckoResult<PromptResponse>? {
            val handler = selectPromptHandler ?: return null
            if (prompt.type == GeckoSession.PromptDelegate.ChoicePrompt.Type.MENU) return null
            val multiple = prompt.type == GeckoSession.PromptDelegate.ChoicePrompt.Type.MULTIPLE
            val options = ArrayList<com.jhaiian.clint.browser.dialogs.SelectPickerOption>()
            for (choice in prompt.choices) {
                val children = choice.items
                if (children != null && children.isNotEmpty()) {
                    for (child in children) {
                        if (child.separator) continue
                        options.add(com.jhaiian.clint.browser.dialogs.SelectPickerOption(child.id, child.label.orEmpty(), child.selected, child.disabled || choice.disabled, choice.label.orEmpty()))
                    }
                } else if (!choice.separator) {
                    options.add(com.jhaiian.clint.browser.dialogs.SelectPickerOption(choice.id, choice.label.orEmpty(), choice.selected, choice.disabled, null))
                }
            }
            if (options.isEmpty()) return null
            val result = GeckoResult<PromptResponse>()
            val finished = java.util.concurrent.atomic.AtomicBoolean(false)
            handler(
                prompt.title.orEmpty(),
                options,
                multiple,
                { values ->
                    if (finished.compareAndSet(false, true)) {
                        result.complete(
                            when {
                                multiple -> prompt.confirm(values.toTypedArray())
                                values.isNotEmpty() -> prompt.confirm(values[0])
                                else -> prompt.dismiss()
                            }
                        )
                    }
                },
                {
                    if (finished.compareAndSet(false, true)) result.complete(prompt.dismiss())
                }
            )
            return result
        }

        override fun onFilePrompt(session: GeckoSession, prompt: GeckoSession.PromptDelegate.FilePrompt): GeckoResult<PromptResponse>? {
            val result = GeckoResult<PromptResponse>()
            val client = webChromeClient
            val multiple = prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE
            val callback = ValueCallback<Array<Uri>> { uris ->
                when {
                    uris == null || uris.isEmpty() -> result.complete(prompt.dismiss())
                    multiple -> result.complete(prompt.confirm(context, uris))
                    else -> result.complete(prompt.confirm(context, uris[0]))
                }
            }
            val params = FileChooserParams(
                prompt.mimeTypes?.let { arrayOf(*it) },
                if (multiple) FileChooserParams.MODE_OPEN_MULTIPLE else FileChooserParams.MODE_OPEN,
                prompt.capture != GeckoSession.PromptDelegate.FilePrompt.Capture.NONE
            )
            if (client == null || !client.onShowFileChooser(callback, params)) callback.onReceiveValue(null)
            return result
        }

        private fun webClientOrNull(): ClintWebViewClient? = webViewClient
    }

    private inner class ClipboardSelectionDelegate(activity: android.app.Activity) : BasicSelectionActionDelegate(activity) {
        private var pending: GeckoResult<AllowOrDeny>? = null

        private fun finish(result: GeckoResult<AllowOrDeny>, allow: Boolean) {
            if (pending !== result) return
            pending = null
            result.complete(if (allow) AllowOrDeny.ALLOW else AllowOrDeny.DENY)
        }

        override fun onShowClipboardPermissionRequest(
            session: GeckoSession,
            permission: GeckoSession.SelectionActionDelegate.ClipboardPermission
        ): GeckoResult<AllowOrDeny>? {
            val handler = clipboardPermissionHandler ?: return super.onShowClipboardPermissionRequest(session, permission)
            pending?.let { finish(it, false) }
            val result = GeckoResult<AllowOrDeny>()
            pending = result
            handler(this@ClintWebView.url.orEmpty()) { allow -> finish(result, allow) }
            return result
        }

        override fun onDismissClipboardPermissionRequest(session: GeckoSession) {
            val result = pending
            if (result == null) {
                super.onDismissClipboardPermissionRequest(session)
                return
            }
            finish(result, false)
            clipboardDismissHandler?.invoke()
        }
    }

    private fun findActivity(): android.app.Activity? {
        var ctx: Context? = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    init {
        ClintGecko.init(context)
        val builder = GeckoSessionSettings.Builder()
            .usePrivateMode(isPrivate)
            .allowJavascript(true)
        if (!profileId.isNullOrEmpty()) builder.contextId(profileId)
        session = GeckoSession(builder.build())
        session.contentDelegate = contentDelegate
        session.navigationDelegate = navigationDelegate
        session.progressDelegate = progressDelegate
        session.permissionDelegate = permissionDelegate
        session.promptDelegate = promptDelegate
        session.scrollDelegate = scrollDelegate
        session.mediaSessionDelegate = mediaDelegate
        findActivity()?.let { session.selectionActionDelegate = ClipboardSelectionDelegate(it) }
        autofillAllowed = !isPrivate
        if (!deferOpen) {
            session.open(ClintGecko.runtime)
            geckoView.setSession(session)
        }
        addView(geckoView, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        addView(cover, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        if (!deferOpen) ClintGecko.registerView(this)
    }

    companion object {
        const val ERROR_PAGE_PREFIX = "data:text/html;charset=utf-8;base64,"
    }
}
