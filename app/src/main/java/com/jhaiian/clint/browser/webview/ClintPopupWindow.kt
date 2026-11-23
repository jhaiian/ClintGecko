package com.jhaiian.clint.browser.webview

import org.mozilla.geckoview.GeckoSession

class ClintPopupWindow internal constructor(
    val url: String,
    val session: GeckoSession?,
    val isLoaded: Boolean,
    val skipPrompt: Boolean,
    private val titleProvider: () -> String?,
    private val onAccept: () -> Unit,
    private val onDecline: () -> Unit
) {

    var isSettled = false
        private set

    val title: String? get() = titleProvider()

    fun accept() {
        if (isSettled) return
        isSettled = true
        onAccept()
    }

    fun decline() {
        if (isSettled) return
        isSettled = true
        onDecline()
    }
}
