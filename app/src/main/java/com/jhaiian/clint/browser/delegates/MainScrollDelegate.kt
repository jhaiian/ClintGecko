package com.jhaiian.clint.browser.delegates
import com.jhaiian.clint.browser.webview.*
import com.jhaiian.clint.browser.MainActivity

import android.os.SystemClock
import com.jhaiian.clint.browser.webview.ClintWebView
import android.animation.ValueAnimator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

internal fun MainActivity.setupSwipeRefresh() {
    swipeRefreshView.canChildScrollUpCallback = {
        swipeGuardBlocked || isShortVideoFeed() || canvasTouchActive || run {
            val wv = tabManager.activeTab?.webView
            val mode = prefs.getString("scroll_hide_mode", "off") ?: "off"
            val barsHiddenByScrolling = !hasWebBottomNav && mode != "off" && uiState.topBarFraction >= 1f
            wv != null && (wv.url?.startsWith("moz-extension://") == true || barsHiddenByScrolling || wv.canScrollVertically(-1) || nestedScrollActive)
        }
    }
    swipeRefreshView.touchVerdictCallback = { downTime ->
        tabManager.activeTab?.webView?.pullToRefreshVerdict(downTime)
    }
    updateSwipeRefreshColors(uiState.isIncognito)
    swipeRefreshView.apply {
        setOnRefreshListener {
            nestedScrollActive = false
            canvasTouchActive = false
            tabManager.activeTab?.webView?.reload(true) ?: run { isRefreshing = false }
        }
    }
}

internal fun MainActivity.updateMainContentInsets() {
    if (uiState.isFullscreen) {
        uiState.contentPaddingTopPx = 0
        uiState.contentPaddingBottomPx = 0
        swipeRefreshView.setPadding(0, 0, 0, 0)
        webContainer.translationY = 0f
        swipeRefreshView.isEnabled = false
        return
    }
    val contentBarHeight = (uiState.topBarFullHeightPx - uiState.statusBarInsetPx).coerceAtLeast(0)
    val topCollapsed = uiState.topBarFraction > 0f || uiState.topBarAnimating
    val bottomCollapsed = uiState.bottomBarFraction > 0f || uiState.bottomBarAnimating
    val visibleTop = uiState.statusBarInsetPx + if (topCollapsed) 0 else contentBarHeight
    val visibleBottom = maxOf(if (bottomCollapsed) 0 else uiState.bottomBarFullHeightPx, uiState.systemNavInsetPx)
    uiState.contentPaddingTopPx = visibleTop
    uiState.contentPaddingBottomPx = visibleBottom
    swipeRefreshView.setPadding(0, visibleTop, 0, maxOf(visibleBottom, uiState.imeInsetPx))
    webContainer.translationY = if (topCollapsed) (1f - uiState.topBarFraction) * contentBarHeight else 0f
    val mode = prefs.getString("scroll_hide_mode", "off") ?: "off"
    val position = prefs.getString("address_bar_position", "top") ?: "top"
    val barsHidden = mode != "off" && when (mode) {
        "search_bar" -> if (position == "bottom") uiState.bottomBarFraction >= 1f else uiState.topBarFraction >= 1f
        "navigation_bar" -> uiState.bottomBarFraction >= 1f
        else -> uiState.topBarFraction >= 1f
    }
    swipeRefreshView.isEnabled = !barsHidden
}

private const val HIDE_THRESHOLD_DP = 24
private const val SHOW_THRESHOLD_DP = 12
private const val BAR_ANIMATION_MS = 200L
private const val BAR_SETTLE_MS = 160L

private fun MainActivity.scrollHideSettledHidden(mode: String, position: String): Boolean {
    val topAnimating = uiState.topBarAnimating
    val bottomAnimating = uiState.bottomBarAnimating
    if (topAnimating || bottomAnimating) return uiState.barsTargetHidden
    return when (mode) {
        "search_bar" -> if (position == "bottom") uiState.bottomBarFraction >= 1f else uiState.topBarFraction >= 1f
        else -> uiState.bottomBarFraction >= 1f
    }
}

private fun MainActivity.animateScrollHideBars(mode: String, position: String, target: Float) {
    when (mode) {
        "search_bar" -> if (position == "bottom") animateBottomBarOnlyTo(target) else animateTopBarOnlyTo(target)
        "navigation_bar" -> animateBottomBarOnlyTo(target)
        "both" -> animateBottomBarTo(target)
    }
}

internal fun MainActivity.setTopBarFraction(fraction: Float) {
    uiState.topBarFraction = fraction
    updateMainContentInsets()
}

internal fun MainActivity.setBottomBarFraction(fraction: Float) {
    uiState.bottomBarFraction = fraction
    updateMainContentInsets()
}

private fun MainActivity.runBarAnimation(
    target: Float,
    start: Float,
    top: Boolean,
    bottom: Boolean
) {
    uiState.barsTargetHidden = target >= 1f
    uiState.topBarAnimating = top
    uiState.bottomBarAnimating = bottom
    val animator = ValueAnimator.ofFloat(start, target).apply {
        duration = BAR_ANIMATION_MS
        interpolator = if (target > start) AccelerateInterpolator() else DecelerateInterpolator()
        addUpdateListener { anim ->
            val f = anim.animatedValue as Float
            if (top) uiState.topBarFraction = f
            if (bottom) uiState.bottomBarFraction = f
            updateMainContentInsets()
        }
        doOnEnd {
            uiState.topBarAnimating = false
            uiState.bottomBarAnimating = false
            updateMainContentInsets()
        }
    }
    bottomBarAnimator2 = animator
    updateMainContentInsets()
    animator.start()
}

internal fun MainActivity.animateBottomBarTo(targetFraction: Float, animated: Boolean = true) {
    bottomBarAnimator2?.cancel()
    if (!animated || (uiState.topBarFullHeightPx == 0 && uiState.bottomBarFullHeightPx == 0)) {
        setTopBarFraction(targetFraction)
        setBottomBarFraction(targetFraction)
        return
    }
    val startFraction = uiState.bottomBarFraction
    if (startFraction == targetFraction) return
    runBarAnimation(targetFraction, startFraction, top = true, bottom = true)
}

internal fun MainActivity.animateTopBarOnlyTo(targetFraction: Float, animated: Boolean = true) {
    bottomBarAnimator2?.cancel()
    if (!animated || uiState.topBarFullHeightPx == 0) {
        setTopBarFraction(targetFraction)
        return
    }
    val startFraction = uiState.topBarFraction
    if (startFraction == targetFraction) return
    runBarAnimation(targetFraction, startFraction, top = true, bottom = false)
}

internal fun MainActivity.animateBottomBarOnlyTo(targetFraction: Float, animated: Boolean = true) {
    bottomBarAnimator2?.cancel()
    if (!animated || uiState.bottomBarFullHeightPx == 0) {
        setBottomBarFraction(targetFraction)
        return
    }
    val startFraction = uiState.bottomBarFraction
    if (startFraction == targetFraction) return
    runBarAnimation(targetFraction, startFraction, top = false, bottom = true)
}

private fun ValueAnimator.doOnEnd(action: () -> Unit) {
    addListener(object : android.animation.AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: android.animation.Animator) { action() }
    })
}

internal fun MainActivity.attachScrollListener(webView: ClintWebView) {
    val density = resources.displayMetrics.density
    val hideThresholdPx = (HIDE_THRESHOLD_DP * density).toInt()
    val showThresholdPx = (SHOW_THRESHOLD_DP * density).toInt()
    var accumulated = 0
    var settleUntil = 0L

    fun handleScroll(scrollY: Int, oldScrollY: Int) {
        val mode = prefs.getString("scroll_hide_mode", "off") ?: "off"
        if (mode == "off" || webView !== tabManager.activeTab?.webView) return
        val position = prefs.getString("address_bar_position", "top") ?: "top"
        val hidden = scrollHideSettledHidden(mode, position)
        val now = SystemClock.uptimeMillis()
        val delta = scrollY - oldScrollY
        if (scrollY <= 0) {
            accumulated = 0
            if (hidden) {
                animateScrollHideBars(mode, position, 0f)
                settleUntil = now + BAR_ANIMATION_MS + BAR_SETTLE_MS
            }
            return
        }
        if (now < settleUntil || delta == 0) {
            accumulated = 0
            return
        }
        if (accumulated != 0 && (delta > 0) != (accumulated > 0)) accumulated = 0
        accumulated = (accumulated + delta).coerceIn(-hideThresholdPx, hideThresholdPx)
        val shouldHide = accumulated >= hideThresholdPx && !hidden
        val shouldShow = accumulated <= -showThresholdPx && hidden
        if (shouldHide || shouldShow) {
            accumulated = 0
            settleUntil = now + BAR_ANIMATION_MS + BAR_SETTLE_MS
            animateScrollHideBars(mode, position, if (shouldHide) 1f else 0f)
        }
    }

    webView.touchObserver = null
    webView.scrollListener = { scrollY, oldScrollY -> handleScroll(scrollY, oldScrollY) }
}

internal fun MainActivity.injectScrollTracker(webView: ClintWebView) {
    webView.evaluateJavascript(loadJsAsset("scroll_tracker.js"), null)
}

internal fun MainActivity.injectBottomNavDetector(webView: ClintWebView) {
    webView.evaluateJavascript(loadJsAsset("bottom_nav_detector.js"), null)
}

internal fun MainActivity.injectCanvasTouchDetector(webView: ClintWebView) {
    webView.evaluateJavascript(loadJsAsset("canvas_touch_detector.js"), null)
}

private val TIKTOK_FEED_PATH = Regex("^/?$|^/(foryou|following|friends|explore|live)(/|$)|/(video|photo)/", RegexOption.IGNORE_CASE)

internal fun MainActivity.isShortVideoFeed(): Boolean {
    val url = tabManager.activeTab?.webView?.url ?: return false
    val uri = runCatching { android.net.Uri.parse(url) }.getOrNull() ?: return false
    val host = uri.host?.lowercase() ?: return false
    val path = uri.path.orEmpty()
    fun onHost(domain: String) = host == domain || host.endsWith(".$domain")
    return when {
        onHost("youtube.com") -> path.startsWith("/shorts", ignoreCase = true)
        onHost("instagram.com") || onHost("facebook.com") -> path.startsWith("/reel", ignoreCase = true)
        onHost("tiktok.com") -> TIKTOK_FEED_PATH.containsMatchIn(path)
        else -> false
    }
}
