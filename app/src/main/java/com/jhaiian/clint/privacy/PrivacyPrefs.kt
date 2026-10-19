package com.jhaiian.clint.privacy

import android.content.Context
import androidx.preference.PreferenceManager
import org.mozilla.geckoview.GeckoPreferenceController

object PrivacyPrefs {

    const val STRIP_TRACKING_PARAMS = "strip_tracking_params"
    const val CLEAR_ON_EXIT = "clear_on_exit"
    const val CLEAR_ON_EXIT_HISTORY = "clear_on_exit_history"
    const val CLEAR_ON_EXIT_COOKIES = "clear_on_exit_cookies"
    const val CLEAR_ON_EXIT_CACHE = "clear_on_exit_cache"
    const val APP_LOCK = "app_lock"
    const val APP_LOCK_TIMEOUT = "app_lock_timeout_seconds"
    const val SCREENSHOT_BLOCKING = "screenshot_blocking"
    const val INCOGNITO_KEYBOARD = "incognito_keyboard"
    const val CLIPBOARD_CLEAR_SECONDS = "clipboard_auto_clear_seconds"
    const val WEBRTC_PROTECTION = "webrtc_protection"
    const val GLOBAL_PRIVACY_CONTROL = "global_privacy_control"
    const val FINGERPRINTING_PROTECTION = "fingerprinting_protection"
    const val STRICT_CERTIFICATES = "strict_certificates"
    const val EXIT_CLEAR_PENDING = "exit_clear_pending"

    const val DEFAULT_STRIP_TRACKING_PARAMS = true
    const val DEFAULT_CLEAR_ON_EXIT = false
    const val DEFAULT_CLEAR_ON_EXIT_HISTORY = true
    const val DEFAULT_CLEAR_ON_EXIT_COOKIES = true
    const val DEFAULT_CLEAR_ON_EXIT_CACHE = true
    const val DEFAULT_APP_LOCK = false
    const val DEFAULT_APP_LOCK_TIMEOUT = 60
    const val DEFAULT_SCREENSHOT_BLOCKING = false
    const val DEFAULT_INCOGNITO_KEYBOARD = true
    const val DEFAULT_CLIPBOARD_CLEAR_SECONDS = 0
    const val DEFAULT_WEBRTC_PROTECTION = true
    const val DEFAULT_GLOBAL_PRIVACY_CONTROL = true
    const val DEFAULT_FINGERPRINTING_PROTECTION = true
    const val DEFAULT_STRICT_CERTIFICATES = false

    val appLockTimeouts = listOf(0, 60, 300, 900)
    val clipboardDelays = listOf(0, 30, 60, 300)

    fun applyGeckoPrefs(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val fingerprinting = prefs.getBoolean(FINGERPRINTING_PROTECTION, DEFAULT_FINGERPRINTING_PROTECTION)
        val webrtc = prefs.getBoolean(WEBRTC_PROTECTION, DEFAULT_WEBRTC_PROTECTION)
        val gpc = prefs.getBoolean(GLOBAL_PRIVACY_CONTROL, DEFAULT_GLOBAL_PRIVACY_CONTROL)
        setBoolean("privacy.fingerprintingProtection", fingerprinting)
        setBoolean("privacy.trackingprotection.fingerprinting.enabled", fingerprinting)
        setBoolean("media.peerconnection.ice.default_address_only", webrtc)
        setBoolean("media.peerconnection.ice.no_host", webrtc)
        setBoolean("privacy.globalprivacycontrol.enabled", gpc)
        setBoolean("privacy.globalprivacycontrol.functionality.enabled", gpc)
    }

    private fun setBoolean(pref: String, value: Boolean) {
        runCatching { GeckoPreferenceController.setGeckoPref(pref, value, GeckoPreferenceController.PREF_BRANCH_USER) }
    }
}
