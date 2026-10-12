package com.jhaiian.clint.browser.delegates

import android.webkit.JavascriptInterface
import com.jhaiian.clint.browser.webview.ClintWebView
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.dialogs.ColorPickerRequest
import java.lang.ref.WeakReference

class ColorPickerBridge(private val activity: MainActivity, private val webView: ClintWebView) {

    @JavascriptInterface
    fun onColorOpen(id: String, value: String) {
        activity.runOnUiThread {
            if (activity.tabManager.activeTab?.webView !== webView) return@runOnUiThread
            activity.uiState.colorPickerRequest = ColorPickerRequest(id, value, WeakReference(webView))
        }
    }
}
