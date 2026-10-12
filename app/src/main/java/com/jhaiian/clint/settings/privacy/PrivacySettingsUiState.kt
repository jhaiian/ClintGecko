package com.jhaiian.clint.settings.privacy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PrivacySettingsUiState(
    initialBlockThirdPartyCookies: Boolean,
    initialCustomUserAgent: Boolean,
    initialHttpsOnly: Boolean,
    initialDoNotTrack: Boolean,
    initialAutofillEnabled: Boolean,
    initialAutofillServiceActive: Boolean,
    val autofillSupported: Boolean
) {
    var blockThirdPartyCookies by mutableStateOf(initialBlockThirdPartyCookies)
    var customUserAgent by mutableStateOf(initialCustomUserAgent)
    var httpsOnly by mutableStateOf(initialHttpsOnly)
    var doNotTrack by mutableStateOf(initialDoNotTrack)
    var autofillEnabled by mutableStateOf(initialAutofillEnabled)
    var autofillServiceActive by mutableStateOf(initialAutofillServiceActive)
}
