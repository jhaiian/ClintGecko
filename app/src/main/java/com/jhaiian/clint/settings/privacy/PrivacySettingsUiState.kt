package com.jhaiian.clint.settings.privacy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PrivacySettingsUiState(
    initialEtpLevel: String,
    initialCookiePolicy: String,
    initialCustomUserAgent: Boolean,
    initialHttpsOnly: Boolean,
    initialDoNotTrack: Boolean,
    initialStrictCertificates: Boolean,
    initialWebsiteIsolation: Boolean,
    initialStripTrackingParams: Boolean,
    initialGlobalPrivacyControl: Boolean,
    initialFingerprintingProtection: Boolean,
    initialWebRtcProtection: Boolean,
    initialClearOnExit: Boolean,
    initialClearHistory: Boolean,
    initialClearCookies: Boolean,
    initialClearCache: Boolean,
    initialClearTabs: Boolean,
    initialAppLock: Boolean,
    initialAppLockTimeout: Int,
    initialScreenshotBlocking: Boolean,
    initialIncognitoKeyboard: Boolean,
    initialClipboardClearSeconds: Int,
    initialAutofillEnabled: Boolean,
    initialAutofillServiceActive: Boolean,
    val autofillSupported: Boolean,
    initialSecureDnsMode: String,
    initialSecureDnsProvider: String,
    initialSecureDnsCustomUrl: String,
    val hideStatusBar: Boolean,
    val hideSystemNavigation: Boolean
) {
    var etpLevel by mutableStateOf(initialEtpLevel)
    var etpDialogOpen by mutableStateOf(false)
    var cookiePolicy by mutableStateOf(initialCookiePolicy)
    var cookiePolicyDialogOpen by mutableStateOf(false)
    var customUserAgent by mutableStateOf(initialCustomUserAgent)
    var httpsOnly by mutableStateOf(initialHttpsOnly)
    var doNotTrack by mutableStateOf(initialDoNotTrack)
    var strictCertificates by mutableStateOf(initialStrictCertificates)
    var websiteIsolation by mutableStateOf(initialWebsiteIsolation)
    var stripTrackingParams by mutableStateOf(initialStripTrackingParams)
    var globalPrivacyControl by mutableStateOf(initialGlobalPrivacyControl)
    var fingerprintingProtection by mutableStateOf(initialFingerprintingProtection)
    var webRtcProtection by mutableStateOf(initialWebRtcProtection)
    var clearOnExit by mutableStateOf(initialClearOnExit)
    var clearHistory by mutableStateOf(initialClearHistory)
    var clearCookies by mutableStateOf(initialClearCookies)
    var clearCache by mutableStateOf(initialClearCache)
    var clearTabs by mutableStateOf(initialClearTabs)
    var appLock by mutableStateOf(initialAppLock)
    var appLockTimeout by mutableStateOf(initialAppLockTimeout)
    var screenshotBlocking by mutableStateOf(initialScreenshotBlocking)
    var incognitoKeyboard by mutableStateOf(initialIncognitoKeyboard)
    var clipboardClearSeconds by mutableStateOf(initialClipboardClearSeconds)
    var clearOnExitDialogOpen by mutableStateOf(false)
    var appLockTimeoutDialogOpen by mutableStateOf(false)
    var clipboardDialogOpen by mutableStateOf(false)
    var autofillEnabled by mutableStateOf(initialAutofillEnabled)
    var autofillServiceActive by mutableStateOf(initialAutofillServiceActive)
    var secureDnsMode by mutableStateOf(initialSecureDnsMode)
    var secureDnsProvider by mutableStateOf(initialSecureDnsProvider)
    var secureDnsCustomUrl by mutableStateOf(initialSecureDnsCustomUrl)
    var secureDnsModeDialogOpen by mutableStateOf(false)
    var secureDnsProviderDialogOpen by mutableStateOf(false)
}
