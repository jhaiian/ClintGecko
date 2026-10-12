package com.jhaiian.clint.browser.delegates
import com.jhaiian.clint.browser.webview.*
import com.jhaiian.clint.browser.MainActivity

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.VelocityTracker
import com.jhaiian.clint.browser.webview.ClintWebView
import android.animation.ValueAnimator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

internal fun MainActivity.setupSwipeRefresh() {
    swipeRefreshView.canChildScrollUpCallback = {
        swipeGuardBlocked || isYouTubeShorts() || canvasTouchActive || run {
            val wv = tabManager.activeTab?.webView
            val mode = prefs.getString("scroll_hide_mode", "off") ?: "off"
            val barsHiddenByScrolling = !hasWebBottomNav && mode != "off" && uiState.topBarFraction >= 1f
            wv != null && (barsHiddenByScrolling || wv.canScrollVertically(-1) || nestedScrollActive)
        }
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
    val contentBarHeight = uiState.topBarFullHeightPx - uiState.statusBarInsetPx
    val visibleTop = uiState.statusBarInsetPx + ((1f - uiState.topBarFraction) * contentBarHeight).toInt().coerceAtLeast(0)
    val visibleBottom = ((1f - uiState.bottomBarFraction) * uiState.bottomBarFullHeightPx).toInt().coerceAtLeast(0)
    uiState.contentPaddingTopPx = visibleTop
    uiState.contentPaddingBottomPx = visibleBottom
    val mode = prefs.getString("scroll_hide_mode", "off") ?: "off"
    val position = prefs.getString("address_bar_position", "top") ?: "top"
    val barsHidden = mode != "off" && when (mode) {
        "search_bar" -> if (position == "bottom") uiState.bottomBarFraction >= 1f else uiState.topBarFraction >= 1f
        "navigation_bar" -> uiState.bottomBarFraction >= 1f
        else -> uiState.topBarFraction >= 1f
    }
    swipeRefreshView.isEnabled = !barsHidden
}

internal fun MainActivity.setTopBarFraction(fraction: Float) {
    uiState.topBarFraction = fraction
    updateMainContentInsets()
}

internal fun MainActivity.setBottomBarFraction(fraction: Float) {
    uiState.bottomBarFraction = fraction
    updateMainContentInsets()
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
    bottomBarAnimator2 = ValueAnimator.ofFloat(startFraction, targetFraction).apply {
        duration = 200L
        interpolator = if (targetFraction > startFraction) AccelerateInterpolator() else DecelerateInterpolator()
        addUpdateListener { anim ->
            val f = anim.animatedValue as Float
            setTopBarFraction(f)
            setBottomBarFraction(f)
        }
        start()
    }
}

internal fun MainActivity.animateTopBarOnlyTo(targetFraction: Float, animated: Boolean = true) {
    bottomBarAnimator2?.cancel()
    if (!animated || uiState.topBarFullHeightPx == 0) {
        setTopBarFraction(targetFraction)
        return
    }
    val startFraction = uiState.topBarFraction
    if (startFraction == targetFraction) return
    bottomBarAnimator2 = ValueAnimator.ofFloat(startFraction, targetFraction).apply {
        duration = 200L
        interpolator = if (targetFraction > startFraction) AccelerateInterpolator() else DecelerateInterpolator()
        addUpdateListener { anim ->
            val f = anim.animatedValue as Float
            setTopBarFraction(f)
        }
        start()
    }
}

internal fun MainActivity.animateBottomBarOnlyTo(targetFraction: Float, animated: Boolean = true) {
    bottomBarAnimator2?.cancel()
    if (!animated || uiState.bottomBarFullHeightPx == 0) {
        setBottomBarFraction(targetFraction)
        return
    }
    val startFraction = uiState.bottomBarFraction
    if (startFraction == targetFraction) return
    bottomBarAnimator2 = ValueAnimator.ofFloat(startFraction, targetFraction).apply {
        duration = 200L
        interpolator = if (targetFraction > startFraction) AccelerateInterpolator() else DecelerateInterpolator()
        addUpdateListener { anim ->
            val f = anim.animatedValue as Float
            setBottomBarFraction(f)
        }
        start()
    }
}

private fun ValueAnimator.doOnEnd(action: () -> Unit) {
    addListener(object : android.animation.AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: android.animation.Animator) { action() }
    })
}

internal fun MainActivity.attachScrollListener(webView: ClintWebView) {
    var localVelocityTracker: VelocityTracker? = null

    val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            val mode = prefs.getString("scroll_hide_mode", "off") ?: "off"
            if (mode != "off") {
                val position = prefs.getString("address_bar_position", "top") ?: "top"
                val refHeight = when (mode) {
                    "search_bar" -> if (position == "bottom") uiState.bottomBarFullHeightPx.takeIf { it > 0 } ?: uiState.topBarFullHeightPx else uiState.topBarFullHeightPx
                    else -> uiState.bottomBarFullHeightPx.takeIf { it > 0 } ?: uiState.topBarFullHeightPx
                }
                if (refHeight > 0) {
                    val delta = distanceY / (refHeight * 1.5f)
                    when (mode) {
                        "search_bar" -> {
                            if (position == "bottom") {
                                val newFrac = (uiState.bottomBarFraction + delta).coerceIn(0f, 1f)
                                setBottomBarFraction(newFrac)
                            } else {
                                val newFrac = (uiState.topBarFraction + delta).coerceIn(0f, 1f)
                                setTopBarFraction(newFrac)
                            }
                        }
                        "navigation_bar" -> {
                            val newFrac = (uiState.bottomBarFraction + delta).coerceIn(0f, 1f)
                            setBottomBarFraction(newFrac)
                        }
                        "both" -> {
                            val newFrac = (uiState.bottomBarFraction + delta).coerceIn(0f, 1f)
                            setTopBarFraction(newFrac)
                            setBottomBarFraction(newFrac)
                        }
                    }
                }
            }
            return false
        }
    })

    webView.touchObserver = { event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                localVelocityTracker?.recycle()
                localVelocityTracker = VelocityTracker.obtain()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val mode = prefs.getString("scroll_hide_mode", "off") ?: "off"
                if (mode != "off") {
                    localVelocityTracker?.computeCurrentVelocity(1000)
                    val vy = localVelocityTracker?.yVelocity ?: 0f
                    val position = prefs.getString("address_bar_position", "top") ?: "top"
                    val currentFrac = when (mode) {
                        "search_bar" -> if (position == "bottom") uiState.bottomBarFraction else uiState.topBarFraction
                        else -> uiState.bottomBarFraction
                    }
                    val snapToHidden = when {
                        vy < -900f -> true
                        vy > 900f -> false
                        else -> currentFrac >= 0.5f
                    }
                    val target = if (snapToHidden) 1f else 0f
                    when (mode) {
                        "search_bar" -> {
                            if (position == "bottom") animateBottomBarOnlyTo(target)
                            else animateTopBarOnlyTo(target)
                        }
                        "navigation_bar" -> animateBottomBarOnlyTo(target)
                        "both" -> animateBottomBarTo(target)
                    }
                }
                localVelocityTracker?.recycle()
                localVelocityTracker = null
            }
        }
        localVelocityTracker?.addMovement(event)
        detector.onTouchEvent(event)
    }
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

internal fun MainActivity.isYouTubeShorts(): Boolean {
    val url = tabManager.activeTab?.webView?.url ?: return false
    return url.contains("youtube.com/shorts", ignoreCase = true)
}
