package com.jhaiian.clint.browser.delegates
import com.jhaiian.clint.browser.webview.*
import com.jhaiian.clint.browser.MainActivity

import android.os.SystemClock
import com.jhaiian.clint.browser.webview.ClintWebView
import android.animation.ValueAnimator
import android.view.MotionEvent
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
    if (swipeRefreshView.isInLayout) {
        if (!contentInsetsUpdatePosted) {
            contentInsetsUpdatePosted = true
            swipeRefreshView.post {
                contentInsetsUpdatePosted = false
                updateMainContentInsets()
            }
        }
        return
    }
    if (uiState.isFullscreen) {
        uiState.contentPaddingTopPx = 0
        uiState.contentPaddingBottomPx = 0
        applyWebContentLayout()
        swipeRefreshView.isEnabled = false
        return
    }
    val contentBarHeight = (uiState.topBarFullHeightPx - uiState.statusBarInsetPx).coerceAtLeast(0)
    val topCollapsed = uiState.topBarFraction > 0f || uiState.topBarAnimating
    val bottomCollapsed = uiState.bottomBarFraction > 0f || uiState.bottomBarAnimating
    uiState.contentPaddingTopPx = uiState.statusBarInsetPx + if (topCollapsed) 0 else contentBarHeight
    uiState.contentPaddingBottomPx = maxOf(if (bottomCollapsed) 0 else uiState.bottomBarFullHeightPx, uiState.systemNavInsetPx)
    applyWebContentLayout()
    val mode = prefs.getString("scroll_hide_mode", "off") ?: "off"
    val position = prefs.getString("address_bar_position", "top") ?: "top"
    val barsHidden = mode != "off" && when (mode) {
        "search_bar" -> if (position == "bottom") uiState.bottomBarFraction >= 1f else uiState.topBarFraction >= 1f
        "navigation_bar" -> uiState.bottomBarFraction >= 1f
        else -> uiState.topBarFraction >= 1f
    }
    swipeRefreshView.isEnabled = !barsHidden
}

private fun scrollHidesTop(mode: String, position: String): Boolean = when (mode) {
    "search_bar" -> position != "bottom"
    "both" -> true
    else -> false
}

private fun scrollHidesBottom(mode: String, position: String): Boolean = when (mode) {
    "search_bar" -> position == "bottom"
    "navigation_bar", "both" -> true
    else -> false
}

internal fun MainActivity.applyWebContentLayout() {
    val webView = tabManager.activeTab?.webView
    if (uiState.isFullscreen) {
        swipeRefreshView.setPadding(0, 0, 0, 0)
        webContainer.translationY = 0f
        webView?.setDynamicToolbar(0, 0)
        return
    }
    val mode = prefs.getString("scroll_hide_mode", "off") ?: "off"
    val position = prefs.getString("address_bar_position", "top") ?: "top"
    val contentBarHeight = (uiState.topBarFullHeightPx - uiState.statusBarInsetPx).coerceAtLeast(0)
    val topFraction = uiState.topBarFraction.coerceIn(0f, 1f)
    val bottomFraction = uiState.bottomBarFraction.coerceIn(0f, 1f)
    val bottomBase = maxOf(uiState.systemNavInsetPx, uiState.imeInsetPx)
    val dynamicTop = scrollHidesTop(mode, position) && contentBarHeight > 0
    val dynamicBottom = scrollHidesBottom(mode, position) && uiState.bottomBarFullHeightPx > bottomBase
    val topFlexible = dynamicTop || topFraction > 0f || uiState.topBarAnimating
    val bottomFlexible = dynamicBottom || bottomFraction > 0f || uiState.bottomBarAnimating
    val find = uiState.find
    val findTop = if (find.open && find.barAtTop) find.barHeightPx else 0
    val findBottom = if (find.open && !find.barAtTop) find.barHeightPx else 0
    val paddingTop = uiState.statusBarInsetPx + (if (topFlexible) 0 else contentBarHeight) + findTop
    val paddingBottom = maxOf(if (bottomFlexible) uiState.systemNavInsetPx else maxOf(uiState.bottomBarFullHeightPx, uiState.systemNavInsetPx), uiState.imeInsetPx) + findBottom
    swipeRefreshView.setPadding(0, paddingTop, 0, paddingBottom)
    webContainer.translationY = if (topFlexible) (1f - topFraction) * contentBarHeight else 0f
    val topRange = if (dynamicTop) contentBarHeight else 0
    val bottomRange = if (dynamicBottom) uiState.bottomBarFullHeightPx - bottomBase else 0
    val clipping = -(Math.round(topFraction * topRange) + Math.round(bottomFraction * bottomRange))
    webView?.setDynamicToolbar(topRange + bottomRange, clipping)
}

private const val HIDE_THRESHOLD_DP = 24
private const val SHOW_THRESHOLD_DP = 12
private const val BAR_ANIMATION_MS = 200L
private const val BAR_SETTLE_MS = 160L
private const val BAR_MIN_ANIMATION_MS = 90L
private const val SNAP_SLOP_DP = 8

private fun MainActivity.scrollHideFraction(mode: String, position: String): Float = when (mode) {
    "search_bar" -> if (position == "bottom") uiState.bottomBarFraction else uiState.topBarFraction
    else -> uiState.bottomBarFraction
}

private fun MainActivity.scrollHideRangePx(mode: String, position: String): Int {
    val top = if (scrollHidesTop(mode, position)) (uiState.topBarFullHeightPx - uiState.statusBarInsetPx).coerceAtLeast(0) else 0
    val bottom = if (scrollHidesBottom(mode, position)) (uiState.bottomBarFullHeightPx - maxOf(uiState.systemNavInsetPx, uiState.imeInsetPx)).coerceAtLeast(0) else 0
    return maxOf(top, bottom)
}

private fun MainActivity.setScrollHideFraction(mode: String, position: String, fraction: Float) {
    if (bottomBarAnimator2?.isRunning == true) bottomBarAnimator2?.cancel()
    when (mode) {
        "search_bar" -> if (position == "bottom") uiState.bottomBarFraction = fraction else uiState.topBarFraction = fraction
        "navigation_bar" -> uiState.bottomBarFraction = fraction
        "both" -> {
            uiState.topBarFraction = fraction
            uiState.bottomBarFraction = fraction
        }
    }
    uiState.barsTargetHidden = fraction >= 1f
    updateMainContentInsets()
}

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
        duration = (BAR_ANIMATION_MS * kotlin.math.abs(target - start)).toLong().coerceAtLeast(BAR_MIN_ANIMATION_MS)
        interpolator = DecelerateInterpolator(1.5f)
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
    val snapSlopPx = SNAP_SLOP_DP * density
    var accumulated = 0
    var settleUntil = 0L
    var touching = false
    var gestureTravel = 0f
    var lastDirection = 0

    fun activeMode(): String? {
        val mode = prefs.getString("scroll_hide_mode", "off") ?: "off"
        if (mode == "off" || uiState.isFullscreen || webView !== tabManager.activeTab?.webView) return null
        return mode
    }

    fun handleScroll(atTop: Boolean, delta: Int) {
        val mode = activeMode() ?: return
        val position = prefs.getString("address_bar_position", "top") ?: "top"
        val hidden = scrollHideSettledHidden(mode, position)
        val now = SystemClock.uptimeMillis()
        if (atTop) {
            accumulated = 0
            if (hidden || scrollHideFraction(mode, position) > 0f) {
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

    fun handleDrag(delta: Int) {
        if (delta == 0 || canvasTouchActive) return
        val mode = activeMode() ?: return
        val position = prefs.getString("address_bar_position", "top") ?: "top"
        val range = scrollHideRangePx(mode, position)
        if (range <= 0) return
        gestureTravel += kotlin.math.abs(delta)
        lastDirection = if (delta > 0) 1 else -1
        val current = scrollHideFraction(mode, position)
        val next = (current + delta.toFloat() / range).coerceIn(0f, 1f)
        if (next != current) setScrollHideFraction(mode, position, next)
    }

    fun settle() {
        val mode = activeMode() ?: return
        val position = prefs.getString("address_bar_position", "top") ?: "top"
        val current = scrollHideFraction(mode, position)
        val target = when {
            !webView.canScrollVertically(-1) -> 0f
            current <= 0f || current >= 1f -> return
            gestureTravel < snapSlopPx -> if (current >= 0.5f) 1f else 0f
            lastDirection > 0 -> 1f
            else -> 0f
        }
        if (target == current) return
        accumulated = 0
        settleUntil = SystemClock.uptimeMillis() + BAR_ANIMATION_MS + BAR_SETTLE_MS
        animateScrollHideBars(mode, position, target)
    }

    webView.touchObserver = { ev ->
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touching = true
                gestureTravel = 0f
                lastDirection = 0
                accumulated = 0
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touching = false
                settle()
            }
        }
    }
    webView.scrollListener = { scrollY, oldScrollY ->
        if (!touching && (webView.progress >= 100 || scrollY <= 0)) {
            handleScroll(scrollY <= 0, ((scrollY - oldScrollY) * density).toInt())
        }
    }
    webView.dragScrollListener = { delta -> handleDrag(delta) }
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
