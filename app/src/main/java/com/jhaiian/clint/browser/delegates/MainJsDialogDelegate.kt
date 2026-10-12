package com.jhaiian.clint.browser.delegates

import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.dialogs.JsDialogRequest

internal fun MainActivity.onJsDialogRequest(request: JsDialogRequest) {
    uiState.jsDialogRequest?.cancel()
    uiState.jsDialogRequest = request
}
