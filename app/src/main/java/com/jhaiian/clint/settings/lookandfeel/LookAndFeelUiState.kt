package com.jhaiian.clint.settings.lookandfeel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class LookAndFeelDialog {
    THEME, ACCENT, SURFACE_INTENSITY, ADDRESS_BAR_POSITION, MENU_STYLE, SCROLL_HIDE_MODE, EXIT_CONFIRMATION, POPUP_ALERT_STYLE, LANGUAGE
}

class LookAndFeelUiState(
    initialTheme: String,
    initialAccent: String,
    initialIntensity: String,
    initialLanguage: String,
    initialScrollHideMode: String,
    initialAddressBarPosition: String,
    initialMenuStyle: String,
    initialHideStatusBar: Boolean, initialHideSystemNavigation: Boolean,
    initialShowHomeButton: Boolean,
    initialExitConfirmation: String,
    initialPopupAlertStyle: String
) {
    var theme by mutableStateOf(initialTheme)
    var accent by mutableStateOf(initialAccent)
    var intensity by mutableStateOf(initialIntensity)
    var language by mutableStateOf(initialLanguage)

    var scrollHideMode by mutableStateOf(initialScrollHideMode)
    var addressBarPosition by mutableStateOf(initialAddressBarPosition)
    var menuStyle by mutableStateOf(initialMenuStyle)
    var hideStatusBar by mutableStateOf(initialHideStatusBar)
    var hideSystemNavigation by mutableStateOf(initialHideSystemNavigation)
    var showHomeButton by mutableStateOf(initialShowHomeButton)

    var exitConfirmation by mutableStateOf(initialExitConfirmation)
    var popupAlertStyle by mutableStateOf(initialPopupAlertStyle)

    var openDialog by mutableStateOf<LookAndFeelDialog?>(null)
}
