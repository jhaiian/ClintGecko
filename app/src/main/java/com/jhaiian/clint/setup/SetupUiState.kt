package com.jhaiian.clint.setup

import android.os.Bundle
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig

enum class SetupStep { WELCOME, RESTORE, LOOK, LAYOUT, SEARCH, PRIVACY, SECURITY, ADDONS, NOTIFICATIONS, DEFAULT_BROWSER, DONE }

class SetupUiState(
    val steps: List<SetupStep>,
    initialScroll: Map<SetupStep, Int> = emptyMap()
) {
    var index by mutableIntStateOf(0)
    val step: SetupStep get() = steps[index.coerceIn(0, steps.lastIndex)]

    var animateEntrance by mutableStateOf(true)
    var consentChecked by mutableStateOf(false)
    var consentNudge by mutableIntStateOf(0)

    var theme by mutableStateOf("system")
    var accent by mutableStateOf("material_you")
    var intensity by mutableStateOf("soft_tint")
    var language by mutableStateOf("system")
    var showAllAccents by mutableStateOf(false)

    var addressBarPosition by mutableStateOf("top")
    var menuStyle by mutableStateOf("popup")
    var scrollHideMode by mutableStateOf("off")

    var engine by mutableStateOf("duckduckgo")
    var customEngineName by mutableStateOf("")
    var customEngineUrl by mutableStateOf("")

    var httpsOnly by mutableStateOf(true)
    var doNotTrack by mutableStateOf(true)
    var etpLevel by mutableStateOf("standard")
    var cookiePolicy by mutableStateOf("isolate")
    var fingerprintingProtection by mutableStateOf(true)
    var stripTrackingParams by mutableStateOf(true)
    var globalPrivacyControl by mutableStateOf(true)
    var webRtcProtection by mutableStateOf(true)
    var secureDnsMode by mutableStateOf("off")
    var clearOnExit by mutableStateOf(false)
    var screenshotBlocking by mutableStateOf(false)

    var addonRequests by mutableStateOf<Set<String>>(emptySet())

    var notificationsGranted by mutableStateOf(false)
    var notificationsAsked by mutableStateOf(false)
    var isDefaultBrowser by mutableStateOf(false)
    var defaultBrowserRequested by mutableStateOf(false)

    var hideStatusBar = false
    var hideSystemNavigation = false

    var confirmDialogConfig by mutableStateOf<ConfirmDialogConfig?>(null)

    val scrollStates: Map<SetupStep, ScrollState> = steps.associateWith { ScrollState(initialScroll[it] ?: 0) }

    fun scrollState(step: SetupStep): ScrollState = scrollStates.getValue(step)

    fun saveTo(out: Bundle) {
        out.putString(KEY_STEP, step.name)
        out.putBoolean(KEY_CONSENT, consentChecked)
        out.putBoolean(KEY_SHOW_ALL_ACCENTS, showAllAccents)
        out.putString(KEY_ADDRESS_BAR, addressBarPosition)
        out.putString(KEY_MENU_STYLE, menuStyle)
        out.putString(KEY_SCROLL_HIDE, scrollHideMode)
        out.putString(KEY_ENGINE, engine)
        out.putString(KEY_CUSTOM_NAME, customEngineName)
        out.putString(KEY_CUSTOM_URL, customEngineUrl)
        out.putBoolean(KEY_HTTPS_ONLY, httpsOnly)
        out.putBoolean(KEY_DNT, doNotTrack)
        out.putString(KEY_ETP, etpLevel)
        out.putString(KEY_COOKIES, cookiePolicy)
        out.putBoolean(KEY_FINGERPRINTING, fingerprintingProtection)
        out.putBoolean(KEY_STRIP_PARAMS, stripTrackingParams)
        out.putBoolean(KEY_GPC, globalPrivacyControl)
        out.putBoolean(KEY_WEBRTC, webRtcProtection)
        out.putString(KEY_SECURE_DNS, secureDnsMode)
        out.putBoolean(KEY_CLEAR_ON_EXIT, clearOnExit)
        out.putBoolean(KEY_SCREENSHOTS, screenshotBlocking)
        out.putStringArrayList(KEY_ADDON_REQUESTS, ArrayList(addonRequests))
        out.putBoolean(KEY_NOTIFICATIONS_ASKED, notificationsAsked)
        scrollStates.forEach { (key, state) -> out.putInt(SCROLL_PREFIX + key.name, state.value) }
    }

    fun restoreFrom(saved: Bundle) {
        val savedStep = saved.getString(KEY_STEP)?.let { name -> SetupStep.entries.firstOrNull { it.name == name } }
        index = savedStep?.let { steps.indexOf(it) }?.takeIf { it >= 0 } ?: 0
        consentChecked = saved.getBoolean(KEY_CONSENT, consentChecked)
        showAllAccents = saved.getBoolean(KEY_SHOW_ALL_ACCENTS, showAllAccents)
        addressBarPosition = saved.getString(KEY_ADDRESS_BAR) ?: addressBarPosition
        menuStyle = saved.getString(KEY_MENU_STYLE) ?: menuStyle
        scrollHideMode = saved.getString(KEY_SCROLL_HIDE) ?: scrollHideMode
        engine = saved.getString(KEY_ENGINE) ?: engine
        customEngineName = saved.getString(KEY_CUSTOM_NAME) ?: customEngineName
        customEngineUrl = saved.getString(KEY_CUSTOM_URL) ?: customEngineUrl
        httpsOnly = saved.getBoolean(KEY_HTTPS_ONLY, httpsOnly)
        doNotTrack = saved.getBoolean(KEY_DNT, doNotTrack)
        etpLevel = saved.getString(KEY_ETP) ?: etpLevel
        cookiePolicy = saved.getString(KEY_COOKIES) ?: cookiePolicy
        fingerprintingProtection = saved.getBoolean(KEY_FINGERPRINTING, fingerprintingProtection)
        stripTrackingParams = saved.getBoolean(KEY_STRIP_PARAMS, stripTrackingParams)
        globalPrivacyControl = saved.getBoolean(KEY_GPC, globalPrivacyControl)
        webRtcProtection = saved.getBoolean(KEY_WEBRTC, webRtcProtection)
        secureDnsMode = saved.getString(KEY_SECURE_DNS) ?: secureDnsMode
        clearOnExit = saved.getBoolean(KEY_CLEAR_ON_EXIT, clearOnExit)
        screenshotBlocking = saved.getBoolean(KEY_SCREENSHOTS, screenshotBlocking)
        addonRequests = saved.getStringArrayList(KEY_ADDON_REQUESTS)?.toSet() ?: addonRequests
        notificationsAsked = saved.getBoolean(KEY_NOTIFICATIONS_ASKED, notificationsAsked)
    }

    companion object {
        private const val KEY_STEP = "setup_step"
        private const val KEY_CONSENT = "setup_consent"
        private const val KEY_SHOW_ALL_ACCENTS = "setup_show_all_accents"
        private const val KEY_ADDRESS_BAR = "setup_address_bar"
        private const val KEY_MENU_STYLE = "setup_menu_style"
        private const val KEY_SCROLL_HIDE = "setup_scroll_hide"
        private const val KEY_ENGINE = "setup_engine"
        private const val KEY_CUSTOM_NAME = "setup_custom_name"
        private const val KEY_CUSTOM_URL = "setup_custom_url"
        private const val KEY_HTTPS_ONLY = "setup_https_only"
        private const val KEY_DNT = "setup_dnt"
        private const val KEY_ETP = "setup_etp"
        private const val KEY_COOKIES = "setup_cookies"
        private const val KEY_FINGERPRINTING = "setup_fingerprinting"
        private const val KEY_STRIP_PARAMS = "setup_strip_params"
        private const val KEY_GPC = "setup_gpc"
        private const val KEY_WEBRTC = "setup_webrtc"
        private const val KEY_SECURE_DNS = "setup_secure_dns"
        private const val KEY_CLEAR_ON_EXIT = "setup_clear_on_exit"
        private const val KEY_SCREENSHOTS = "setup_screenshots"
        private const val KEY_ADDON_REQUESTS = "setup_addon_requests"
        private const val KEY_NOTIFICATIONS_ASKED = "setup_notifications_asked"
        const val SCROLL_PREFIX = "setup_scroll_"

        fun initialScrollFrom(saved: Bundle?, steps: List<SetupStep>): Map<SetupStep, Int> =
            if (saved == null) emptyMap() else steps.associateWith { saved.getInt(SCROLL_PREFIX + it.name, 0) }
    }
}
