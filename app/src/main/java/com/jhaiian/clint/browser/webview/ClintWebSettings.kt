package com.jhaiian.clint.browser.webview

class ClintWebSettings(private val view: ClintWebView) {

    var javaScriptEnabled: Boolean = true
        set(value) {
            field = value
            view.session.settings.allowJavascript = value
        }

    var mediaPlaybackRequiresUserGesture: Boolean = false

    var domStorageEnabled: Boolean = true
    var cacheMode: Int = 0
    var loadsImagesAutomatically: Boolean = true
    var allowFileAccess: Boolean = false
    var allowContentAccess: Boolean = false

    var userAgentString: String = ClintGecko.defaultUserAgent()
        set(value) {
            field = value
            view.applyDesktopMode(value.isNotEmpty() && !value.contains("Mobile", ignoreCase = true))
            view.applyUserAgentOverride(if (value.isEmpty() || value == ClintGecko.nativeUserAgent()) null else value)
        }

    fun setSupportMultipleWindows(enabled: Boolean) {}
    fun setSupportZoom(enabled: Boolean) {}
    fun setBuiltInZoomControls(enabled: Boolean) {}
    fun setDisplayZoomControls(enabled: Boolean) {}
}
