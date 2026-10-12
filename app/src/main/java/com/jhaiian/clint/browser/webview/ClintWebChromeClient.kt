package com.jhaiian.clint.browser.webview

import android.net.Uri
import android.os.SystemClock
import android.view.View
import android.webkit.ValueCallback
import com.jhaiian.clint.browser.dialogs.JsDialogRequest
import com.jhaiian.clint.browser.dialogs.JsDialogType

class ClintWebChromeClient(
    private val isActive: () -> Boolean = { true },
    private val onTitleChanged: (String) -> Unit = {},
    private val onProgressChanged: (Int) -> Unit = {},
    private val onUrlChanged: (String) -> Unit = {},
    private val onFullscreenShow: (View, CustomViewCallback) -> Unit = { _, _ -> },
    private val onFullscreenHide: () -> Unit = {},
    private val onFileChooser: (ValueCallback<Array<Uri>>, FileChooserParams) -> Boolean = { _, _ -> false },
    private val onNewWindowRequest: (String) -> Unit = {},
    private val isFullscreenActive: () -> Boolean = { false },
    private val onWebPermissionRequest: (ClintPermissionRequest) -> Unit = { it.deny() },
    private val onGeolocationRequest: (String, GeolocationCallback) -> Unit = { _, cb -> cb.invoke("", false, false) },
    private val onJsDialog: (JsDialogRequest) -> Unit = {}
) {

    private var fullscreenExitedAtMs = 0L

    fun onProgressChanged(view: ClintWebView, newProgress: Int) {
        if (isActive()) onProgressChanged(newProgress)
    }

    fun onReceivedTitle(view: ClintWebView, title: String) {
        onTitleChanged(title)
        if (isActive()) onUrlChanged(view.url ?: "")
    }

    fun onShowCustomView(view: View, callback: CustomViewCallback) {
        onFullscreenShow(view, callback)
    }

    fun onHideCustomView() {
        fullscreenExitedAtMs = SystemClock.elapsedRealtime()
        onFullscreenHide()
    }

    fun onShowFileChooser(filePathCallback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean =
        onFileChooser(filePathCallback, params)

    fun onPermissionRequest(request: ClintPermissionRequest) {
        onWebPermissionRequest(request)
    }

    fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationCallback) {
        onGeolocationRequest(origin, callback)
    }

    fun dispatchJsDialog(
        type: JsDialogType,
        url: String,
        message: String,
        defaultValue: String,
        onConfirm: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        if (!isActive()) {
            onCancel()
            return
        }
        onJsDialog(
            JsDialogRequest(
                type = type,
                url = url,
                message = message,
                defaultValue = defaultValue,
                onConfirm = onConfirm,
                onCancel = onCancel
            )
        )
    }

    fun onNewWindow(uri: String): Boolean {
        val hijackedFullscreenExit = SystemClock.elapsedRealtime() - fullscreenExitedAtMs < FULLSCREEN_EXIT_POPUP_GUARD_MS
        if (isFullscreenActive() || hijackedFullscreenExit) return false
        if (uri.isBlank() || uri == "about:blank") return false
        onNewWindowRequest(uri)
        return true
    }

    companion object {
        private const val FULLSCREEN_EXIT_POPUP_GUARD_MS = 700L
    }
}
