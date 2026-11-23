package com.jhaiian.clint.browser.webview

import android.net.Uri
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebRequestError
import org.mozilla.geckoview.WebResponse

internal class ClintPopupProbe(
    isPrivate: Boolean,
    profileId: String?,
    private val requestedUri: String,
    private val onFinished: (ClintPopupProbe, String?, Boolean) -> Unit
) {

    val session: GeckoSession = GeckoSession(ClintWebView.sessionSettings(isPrivate, profileId))
    var title: String? = null
        private set
    private var finished = false
    private var released = false
    private var committedUrl: String? = null
    private var loadStarted = false
    private val graceRunnable = Runnable { settle() }
    private val timeoutRunnable = Runnable { settle() }

    fun cancel() {
        finished = true
        release(close = true)
    }

    fun detach() {
        finished = true
        release(close = false)
    }

    private fun settle() {
        val url = committedUrl
        if (url != null) finish(url, true) else finish(requestedUri, false)
    }

    private fun block() {
        if (finished) return
        finished = true
        release(close = true)
        onFinished(this, null, false)
    }

    private fun finish(url: String, adoptable: Boolean) {
        if (finished) return
        finished = true
        ClintGecko.main.removeCallbacks(graceRunnable)
        ClintGecko.main.removeCallbacks(timeoutRunnable)
        if (!adoptable) release(close = true)
        onFinished(this, url, adoptable)
    }

    private fun release(close: Boolean) {
        ClintGecko.main.removeCallbacks(graceRunnable)
        ClintGecko.main.removeCallbacks(timeoutRunnable)
        if (released) return
        released = true
        if (!close) return
        ClintGecko.main.post {
            runCatching { session.stop() }
            runCatching { session.close() }
            runCatching {
                session.navigationDelegate = null
                session.progressDelegate = null
                session.contentDelegate = null
            }
        }
    }

    private fun isExtensionPage(url: String?): Boolean =
        url?.startsWith("moz-extension:", ignoreCase = true) == true

    private fun isBlank(url: String?): Boolean =
        url.isNullOrBlank() || url.startsWith("about:", ignoreCase = true)

    private fun isWebUrl(url: String?): Boolean {
        val scheme = url?.let { Uri.parse(it).scheme?.lowercase() } ?: return false
        return scheme == "http" || scheme == "https"
    }

    private fun isInternalScheme(url: String): Boolean {
        val scheme = Uri.parse(url).scheme?.lowercase() ?: return true
        return scheme in INTERNAL_SCHEMES
    }

    private val navigationDelegate = object : GeckoSession.NavigationDelegate {
        override fun onLoadRequest(
            session: GeckoSession,
            request: GeckoSession.NavigationDelegate.LoadRequest
        ): GeckoResult<AllowOrDeny>? {
            if (finished) return GeckoResult.fromValue(if (released) AllowOrDeny.DENY else AllowOrDeny.ALLOW)
            val target = request.uri
            if (isExtensionPage(target)) {
                block()
                return GeckoResult.fromValue(AllowOrDeny.DENY)
            }
            if (!isInternalScheme(target)) {
                val known = committedUrl
                if (known != null) finish(known, true) else finish(if (isWebUrl(requestedUri)) requestedUri else target, false)
                return GeckoResult.fromValue(AllowOrDeny.DENY)
            }
            return GeckoResult.fromValue(AllowOrDeny.ALLOW)
        }

        override fun onLocationChange(
            session: GeckoSession,
            url: String?,
            perms: List<GeckoSession.PermissionDelegate.ContentPermission>,
            hasUserGesture: Boolean
        ) {
            if (finished) {
                if (!released && isWebUrl(url)) committedUrl = url
                return
            }
            if (isExtensionPage(url)) {
                block()
                return
            }
            if (!isWebUrl(url)) return
            val first = committedUrl == null
            committedUrl = url
            if (first) ClintGecko.main.postDelayed(graceRunnable, GRACE_MS)
        }

        override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? = null

        override fun onLoadError(session: GeckoSession, uri: String?, error: WebRequestError): GeckoResult<String>? {
            if (!finished && committedUrl == null) finish(requestedUri, false)
            return null
        }
    }

    private val progressDelegate = object : GeckoSession.ProgressDelegate {
        override fun onPageStart(session: GeckoSession, url: String) {
            if (finished) return
            if (isExtensionPage(url)) {
                block()
                return
            }
            if (!isBlank(url)) loadStarted = true
        }

        override fun onPageStop(session: GeckoSession, success: Boolean) {
            if (finished || !loadStarted || committedUrl != null) return
            block()
        }
    }

    private val contentDelegate = object : GeckoSession.ContentDelegate {
        override fun onTitleChange(session: GeckoSession, title: String?) {
            this@ClintPopupProbe.title = title
        }

        override fun onCloseRequest(session: GeckoSession) {
            block()
        }

        override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
            runCatching { response.body?.close() }
            finish(committedUrl ?: requestedUri, false)
        }

        override fun onCrash(session: GeckoSession) {
            finish(committedUrl ?: requestedUri, false)
        }

        override fun onKill(session: GeckoSession) {
            finish(committedUrl ?: requestedUri, false)
        }
    }

    init {
        session.navigationDelegate = navigationDelegate
        session.progressDelegate = progressDelegate
        session.contentDelegate = contentDelegate
        com.jhaiian.clint.extensions.ClintExtensions.watchTabClose(session) { block() }
        ClintGecko.main.post {
            if (!finished && session.isOpen) {
                com.jhaiian.clint.extensions.ClintExtensions.watchTabClose(session) { block() }
            }
        }
        ClintGecko.main.postDelayed(timeoutRunnable, TIMEOUT_MS)
    }

    companion object {
        private const val GRACE_MS = 700L
        private const val TIMEOUT_MS = 8000L
        private val INTERNAL_SCHEMES = setOf("http", "https", "about", "data", "blob", "javascript")
    }
}
