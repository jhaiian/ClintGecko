package com.jhaiian.clint.browser.delegates
import com.jhaiian.clint.browser.webview.*
import com.jhaiian.clint.browser.MainActivity

import android.view.View
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

internal fun MainActivity.onShowCustomView(view: View, callback: CustomViewCallback) {
    if (fullscreenView != null) { callback.onCustomViewHidden(); return }
    fullscreenCallback = callback
    fullscreenView = view
    (view.parent as? android.view.ViewGroup)?.removeView(view)
    fullscreenContainerView.addView(view, android.view.ViewGroup.LayoutParams(
        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
        android.view.ViewGroup.LayoutParams.MATCH_PARENT
    ))
    uiState.isFullscreen = true
    val ctrl = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
    ctrl.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
    ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    tabManager.activeTab?.webView?.evaluateJavascript(loadJsAsset("video_dimensions.js")) { result ->
        val parts = result?.trim('"')?.split(",")
        val vw = parts?.getOrNull(0)?.toIntOrNull() ?: 0
        val vh = parts?.getOrNull(1)?.toIntOrNull() ?: 0

        requestedOrientation = when {
            vw > 0 && vh > 0 && vw >= vh -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            vw > 0 && vh > 0 && vh > vw  -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            else                          -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }
}

internal fun MainActivity.exitFullscreen() {
    fullscreenCallback?.onCustomViewHidden()
    fullscreenCallback = null
    fullscreenView?.let { fullscreenContainerView.removeView(it) }
    fullscreenView = null
    attachActiveWebView()
    uiState.isFullscreen = false
    bottomBarAnimator2?.cancel()
    uiState.topBarFraction = 0f
    uiState.bottomBarFraction = 0f
    nestedScrollActive = false
    canvasTouchActive = false
    hasWebBottomNav = false
    updateMainContentInsets()
    swipeRefreshView.isEnabled = true
    val ctrl = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
    ctrl.show(WindowInsetsCompat.Type.navigationBars())
    applySystemUiVisibility()
    window.decorView.post {
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        uiState.topBarFullHeightPx = 0
        uiState.bottomBarFullHeightPx = 0
    }
}

internal fun MainActivity.applySystemUiVisibility() {
    val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
    val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)
    uiState.hideStatusBar = hideStatusBar
    uiState.hideSystemNavigation = hideSystemNavigation
    val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
    if (hideStatusBar) {
        controller.hide(WindowInsetsCompat.Type.statusBars())
    } else {
        controller.show(WindowInsetsCompat.Type.statusBars())
    }
    if (hideSystemNavigation) {
        controller.hide(WindowInsetsCompat.Type.navigationBars())
    } else {
        controller.show(WindowInsetsCompat.Type.navigationBars())
    }
}
