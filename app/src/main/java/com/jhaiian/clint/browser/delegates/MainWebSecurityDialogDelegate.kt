package com.jhaiian.clint.browser.delegates

import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.dialogs.HttpAuthRequest
import com.jhaiian.clint.browser.dialogs.HttpsOnlyRequest
import com.jhaiian.clint.browser.dialogs.SslWarningRequest

internal fun MainActivity.onHttpAuthRequest(request: HttpAuthRequest) {
    uiState.httpAuthRequest?.cancel()
    uiState.httpAuthRequest = request
}

internal fun MainActivity.onSslWarningRequest(request: SslWarningRequest) {
    uiState.sslWarningRequest?.cancel()
    uiState.sslWarningRequest = request
}

internal fun MainActivity.onHttpsOnlyBlockedRequest(request: HttpsOnlyRequest) {
    uiState.httpsOnlyRequest?.cancel()
    uiState.httpsOnlyRequest = request
}
