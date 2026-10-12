package com.jhaiian.clint.setup

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SetupUiState(
    initialPage: Int,
    initialScrollY: Int,
    initialTheme: String,
    initialAccent: String,
    initialIntensity: String,
    initialLanguage: String,
    initialAddressBarPosition: String,
    initialMenuStyle: String,
    initialScrollHideMode: String,
    initialHideStatusBar: Boolean, initialHideSystemNavigation: Boolean,
    initialEngine: String,
    initialCustomEngineName: String, initialCustomEngineUrl: String
) {
    var currentPage by mutableStateOf(initialPage)
    var consentChecked by mutableStateOf(false)

    var theme by mutableStateOf(initialTheme)
    var accent by mutableStateOf(initialAccent)
    var intensity by mutableStateOf(initialIntensity)
    var language by mutableStateOf(initialLanguage)

    var addressBarPosition by mutableStateOf(initialAddressBarPosition)
    var menuStyle by mutableStateOf(initialMenuStyle)
    var scrollHideMode by mutableStateOf(initialScrollHideMode)
    var hideStatusBar by mutableStateOf(initialHideStatusBar)
    var hideSystemNavigation by mutableStateOf(initialHideSystemNavigation)

    var engine by mutableStateOf(initialEngine)
    var customEngineName by mutableStateOf(initialCustomEngineName)
    var customEngineUrl by mutableStateOf(initialCustomEngineUrl)

    var isDefaultBrowser by mutableStateOf(false)

    var confirmDialogConfig by mutableStateOf<com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig?>(null)

    val themePageScrollState = ScrollState(initialScrollY)
}
