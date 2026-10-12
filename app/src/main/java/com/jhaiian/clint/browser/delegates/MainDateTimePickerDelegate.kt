package com.jhaiian.clint.browser.delegates

import android.webkit.JavascriptInterface
import com.jhaiian.clint.browser.webview.ClintWebView
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.dialogs.DateTimePickerRequest
import com.jhaiian.clint.browser.dialogs.DateTimePickerType
import java.lang.ref.WeakReference

class DateTimePickerBridge(private val activity: MainActivity, private val webView: ClintWebView) {

    @JavascriptInterface
    fun onDateTimeOpen(id: String, type: String, value: String) {
        activity.runOnUiThread {
            if (activity.tabManager.activeTab?.webView !== webView) return@runOnUiThread
            val pickerType = DateTimePickerType.fromInputType(type) ?: return@runOnUiThread
            activity.uiState.dateTimePickerRequest = DateTimePickerRequest(id, pickerType, value, WeakReference(webView))
        }
    }
}
