package com.jhaiian.clint.setup

import android.Manifest
import android.app.ActivityOptions
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.webview.ClintGecko
import com.jhaiian.clint.browser.webview.SecureDns
import com.jhaiian.clint.crash.CrashHandler
import com.jhaiian.clint.customtheme.isCustomAccent
import com.jhaiian.clint.extensions.ClintExtensions
import com.jhaiian.clint.extensions.ExtensionHost
import com.jhaiian.clint.privacy.PrivacyPrefs
import com.jhaiian.clint.ui.DocumentViewer
import com.jhaiian.clint.ui.OverlayHostActivity
import com.jhaiian.clint.ui.listscreen.ConfirmDialogHost
import com.jhaiian.clint.ui.theme.ClintComposeTheme
import com.jhaiian.clint.util.LocaleHelper

class SetupActivity : ClintActivity(), OverlayHostActivity {

    private lateinit var uiState: SetupUiState

    override var overlayContent by mutableStateOf<(@Composable () -> Unit)?>(null)

    private val browserRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshDefaultBrowserState()
    }

    companion object {
        const val PRIVACY_POLICY_URL = "https://github.com/jhaiian/ClintGecko/blob/main/PRIVACY_POLICY.md"
        const val TERMS_URL = "https://github.com/jhaiian/ClintGecko/blob/main/TERMS_OF_SERVICE.md"
        const val PREF_NOTIFICATIONS_ASKED = "setup_notifications_asked"

        fun notificationsAllowed(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashHandler.install(this)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean("setup_complete", false)) { startMainActivity(animated = false); return }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val steps = SetupStep.entries.filter {
            it != SetupStep.NOTIFICATIONS || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        }
        uiState = SetupUiState(steps, SetupUiState.initialScrollFrom(savedInstanceState, steps)).apply {
            theme = prefs.getString("app_theme", "system") ?: "system"
            accent = prefs.getString("accent_color", "material_you") ?: "material_you"
            intensity = prefs.getString("surface_intensity", "soft_tint") ?: "soft_tint"
            language = prefs.getString(LocaleHelper.PREF_APP_LANGUAGE, LocaleHelper.LANGUAGE_SYSTEM) ?: LocaleHelper.LANGUAGE_SYSTEM
            addressBarPosition = prefs.getString("address_bar_position", "top") ?: "top"
            menuStyle = prefs.getString("menu_style", "popup") ?: "popup"
            scrollHideMode = sanitizeScrollHideMode(prefs.getString("scroll_hide_mode", "off") ?: "off", addressBarPosition)
            httpsOnly = prefs.getBoolean("https_only", true)
            doNotTrack = prefs.getBoolean("do_not_track", true)
            etpLevel = PrivacyPrefs.etpLevel(prefs)
            cookiePolicy = PrivacyPrefs.cookiePolicy(prefs)
            fingerprintingProtection = prefs.getBoolean(PrivacyPrefs.FINGERPRINTING_PROTECTION, PrivacyPrefs.DEFAULT_FINGERPRINTING_PROTECTION)
            stripTrackingParams = prefs.getBoolean(PrivacyPrefs.STRIP_TRACKING_PARAMS, PrivacyPrefs.DEFAULT_STRIP_TRACKING_PARAMS)
            globalPrivacyControl = prefs.getBoolean(PrivacyPrefs.GLOBAL_PRIVACY_CONTROL, PrivacyPrefs.DEFAULT_GLOBAL_PRIVACY_CONTROL)
            webRtcProtection = prefs.getBoolean(PrivacyPrefs.WEBRTC_PROTECTION, PrivacyPrefs.DEFAULT_WEBRTC_PROTECTION)
            secureDnsMode = prefs.getString(SecureDns.PREF_MODE, SecureDns.MODE_OFF) ?: SecureDns.MODE_OFF
            clearOnExit = prefs.getBoolean(PrivacyPrefs.CLEAR_ON_EXIT, PrivacyPrefs.DEFAULT_CLEAR_ON_EXIT)
            screenshotBlocking = prefs.getBoolean(PrivacyPrefs.SCREENSHOT_BLOCKING, PrivacyPrefs.DEFAULT_SCREENSHOT_BLOCKING)
            hideStatusBar = prefs.getBoolean("hide_status_bar", false)
            hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)
            showAllAccents = accent !in SetupCuratedAccents && !isCustomAccent(accent)
            notificationsGranted = notificationsAllowed(this@SetupActivity)
            notificationsAsked = prefs.getBoolean(PREF_NOTIFICATIONS_ASKED, false)
            isDefaultBrowser = isClintDefaultBrowser()
            animateEntrance = savedInstanceState == null
            if (savedInstanceState != null) restoreFrom(savedInstanceState)
        }

        setContent {
            ClintComposeTheme(theme = uiState.theme) {
                SetupScreen(activity = this, state = uiState)
                ExtensionHost()
                ConfirmDialogHost(uiState.confirmDialogConfig, uiState.hideStatusBar, uiState.hideSystemNavigation) { uiState.confirmDialogConfig = null }
                overlayContent?.invoke()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::uiState.isInitialized) uiState.saveTo(outState)
    }

    override fun onResume() {
        super.onResume()
        if (!::uiState.isInitialized) return
        refreshDefaultBrowserState()
        uiState.notificationsGranted = notificationsAllowed(this)
    }

    fun openPrivacyPolicy() {
        DocumentViewer.show(this, getString(R.string.document_viewer_privacy_policy_title), DocumentViewer.PRIVACY_POLICY_URL)
    }

    fun openTerms() {
        DocumentViewer.show(this, getString(R.string.document_viewer_terms_title), DocumentViewer.TERMS_URL)
    }

    fun selectTheme(theme: String) {
        if (theme == uiState.theme) return
        captureAndRecreate(theme)
    }

    fun selectAccent(accent: String) {
        if (accent == uiState.accent) return
        captureAndApplyAccentColor(accent)
    }

    fun selectIntensity(intensity: String) {
        if (intensity == uiState.intensity) return
        captureAndApplySurfaceIntensity(intensity)
    }

    fun selectLanguage(language: String) {
        if (language == uiState.language) return
        captureAndApplyLanguage(language)
    }

    fun markNotificationsAsked() {
        uiState.notificationsAsked = true
        PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(PREF_NOTIFICATIONS_ASKED, true).apply()
    }

    fun restartAppAfterRestore() {
        val restartIntent = packageManager.getLaunchIntentForPackage(packageName)
        restartIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        if (restartIntent != null) startActivity(restartIntent)
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun refreshDefaultBrowserState() {
        uiState.isDefaultBrowser = isClintDefaultBrowser()
    }

    private fun isClintDefaultBrowser(): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://"))
        val info = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return info?.activityInfo?.packageName == packageName
    }

    fun requestDefaultBrowser() {
        uiState.defaultBrowserRequested = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val roleManager = getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER) && !roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)) {
                    browserRoleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER))
                    return
                }
            } catch (_: Exception) {
            }
        }
        runCatching { startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) }
    }

    fun installAddon(addon: SetupAddon) {
        uiState.addonRequests = uiState.addonRequests + addon.id
        writeChoices(complete = false)
        runCatching {
            ClintGecko.init(this)
            ClintExtensions.install(addon.downloadUrl)
        }
    }

    fun finishSetup() {
        if (isFinishing) return
        writeChoices(complete = true)
        if (ClintGecko.isRuntimeReady()) {
            SecureDns.apply(this)
            PrivacyPrefs.applyGeckoPrefs(this)
            ClintGecko.applyTrackingProtection(this)
            ClintGecko.setCookiePolicy(uiState.cookiePolicy)
        }
        startMainActivity(animated = true)
    }

    private fun writeChoices(complete: Boolean) {
        val state = uiState
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        PrivacyPrefs.setCookiePolicy(prefs, state.cookiePolicy)
        prefs.edit()
            .putString("search_engine", state.engine)
            .putString(com.jhaiian.clint.browser.CustomSearchEngineNameKey, state.customEngineName)
            .putString(com.jhaiian.clint.browser.CustomSearchEngineUrlKey, state.customEngineUrl)
            .putString("address_bar_position", state.addressBarPosition)
            .putString("menu_style", state.menuStyle)
            .putString("scroll_hide_mode", sanitizeScrollHideMode(state.scrollHideMode, state.addressBarPosition))
            .putBoolean("https_only", state.httpsOnly)
            .putBoolean("do_not_track", state.doNotTrack)
            .putString(PrivacyPrefs.ETP_LEVEL, state.etpLevel)
            .putBoolean(PrivacyPrefs.FINGERPRINTING_PROTECTION, state.fingerprintingProtection)
            .putBoolean(PrivacyPrefs.STRIP_TRACKING_PARAMS, state.stripTrackingParams)
            .putBoolean(PrivacyPrefs.GLOBAL_PRIVACY_CONTROL, state.globalPrivacyControl)
            .putBoolean(PrivacyPrefs.WEBRTC_PROTECTION, state.webRtcProtection)
            .putString(SecureDns.PREF_MODE, state.secureDnsMode)
            .putBoolean(PrivacyPrefs.CLEAR_ON_EXIT, state.clearOnExit)
            .putBoolean(PrivacyPrefs.SCREENSHOT_BLOCKING, state.screenshotBlocking)
            .putBoolean(PREF_NOTIFICATIONS_ASKED, state.notificationsAsked || !state.steps.contains(SetupStep.NOTIFICATIONS))
            .putBoolean("setup_complete", complete)
            .commit()
    }

    private fun startMainActivity(animated: Boolean) {
        val intent = Intent(this, MainActivity::class.java)
        if (animated) {
            val options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
            startActivity(intent, options.toBundle())
        } else {
            startActivity(intent)
        }
        finish()
    }
}
