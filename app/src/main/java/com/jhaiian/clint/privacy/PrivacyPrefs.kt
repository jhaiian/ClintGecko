package com.jhaiian.clint.privacy

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.mozilla.geckoview.GeckoPreferenceController

object PrivacyPrefs {

    const val STRIP_TRACKING_PARAMS = "strip_tracking_params"
    const val CLEAR_ON_EXIT = "clear_on_exit"
    const val CLEAR_ON_EXIT_HISTORY = "clear_on_exit_history"
    const val CLEAR_ON_EXIT_COOKIES = "clear_on_exit_cookies"
    const val CLEAR_ON_EXIT_CACHE = "clear_on_exit_cache"
    const val CLEAR_ON_EXIT_TABS = "clear_on_exit_tabs"
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
    const val COOKIE_POLICY = "cookie_policy"
    const val LEGACY_BLOCK_THIRD_PARTY_COOKIES = "block_third_party_cookies"

    const val COOKIE_POLICY_ALLOW_ALL = "allow_all"
    const val COOKIE_POLICY_ISOLATE = "isolate"
    const val COOKIE_POLICY_BLOCK_THIRD_PARTY = "block_third_party"
    const val ETP_LEVEL = "etp_level"

    const val ETP_LEVEL_OFF = "off"
    const val ETP_LEVEL_STANDARD = "standard"
    const val ETP_LEVEL_STRICT = "strict"

    const val DEFAULT_STRIP_TRACKING_PARAMS = true
    const val DEFAULT_CLEAR_ON_EXIT = false
    const val DEFAULT_CLEAR_ON_EXIT_HISTORY = true
    const val DEFAULT_CLEAR_ON_EXIT_COOKIES = true
    const val DEFAULT_CLEAR_ON_EXIT_CACHE = true
    const val DEFAULT_CLEAR_ON_EXIT_TABS = false
    const val DEFAULT_APP_LOCK = false
    const val DEFAULT_APP_LOCK_TIMEOUT = 60
    const val DEFAULT_SCREENSHOT_BLOCKING = false
    const val DEFAULT_INCOGNITO_KEYBOARD = true
    const val DEFAULT_CLIPBOARD_CLEAR_SECONDS = 0
    const val DEFAULT_WEBRTC_PROTECTION = true
    const val DEFAULT_GLOBAL_PRIVACY_CONTROL = true
    const val DEFAULT_FINGERPRINTING_PROTECTION = true
    const val DEFAULT_STRICT_CERTIFICATES = false
    const val DEFAULT_COOKIE_POLICY = COOKIE_POLICY_ISOLATE
    const val DEFAULT_ETP_LEVEL = ETP_LEVEL_STANDARD

    val appLockTimeouts = listOf(0, 60, 300, 900)
    val clipboardDelays = listOf(0, 30, 60, 300)
    val cookiePolicies = listOf(COOKIE_POLICY_ALLOW_ALL, COOKIE_POLICY_ISOLATE, COOKIE_POLICY_BLOCK_THIRD_PARTY)

    fun cookiePolicy(prefs: SharedPreferences): String {
        prefs.getString(COOKIE_POLICY, null)?.takeIf { it in cookiePolicies }?.let { return it }
        if (!prefs.contains(LEGACY_BLOCK_THIRD_PARTY_COOKIES)) return DEFAULT_COOKIE_POLICY
        return if (prefs.getBoolean(LEGACY_BLOCK_THIRD_PARTY_COOKIES, true)) COOKIE_POLICY_BLOCK_THIRD_PARTY else COOKIE_POLICY_ALLOW_ALL
    }

    fun closesTabsOnExit(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(CLEAR_ON_EXIT, DEFAULT_CLEAR_ON_EXIT) &&
            prefs.getBoolean(CLEAR_ON_EXIT_TABS, DEFAULT_CLEAR_ON_EXIT_TABS)

    val etpLevels = listOf(ETP_LEVEL_OFF, ETP_LEVEL_STANDARD, ETP_LEVEL_STRICT)

    fun etpLevel(prefs: SharedPreferences): String =
        prefs.getString(ETP_LEVEL, null)?.takeIf { it in etpLevels } ?: DEFAULT_ETP_LEVEL

    fun setCookiePolicy(prefs: SharedPreferences, policy: String) {
        prefs.edit().putString(COOKIE_POLICY, policy).remove(LEGACY_BLOCK_THIRD_PARTY_COOKIES).apply()
    }

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
