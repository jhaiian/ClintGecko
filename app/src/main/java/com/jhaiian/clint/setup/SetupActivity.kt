package com.jhaiian.clint.setup

import android.app.role.RoleManager
import androidx.activity.addCallback
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.crash.CrashHandler
import com.jhaiian.clint.ui.DocumentViewer
import com.jhaiian.clint.ui.OverlayHostActivity
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
        private const val KEY_PENDING_PAGE = "setup_pending_page"
        private const val KEY_PENDING_SCROLL = "setup_pending_scroll"
        private const val KEY_PENDING_CONSENT = "setup_pending_consent"
        private const val KEY_PENDING_HIDE_STATUS_BAR = "setup_pending_hide_status_bar"
        private const val KEY_PENDING_HIDE_SYSTEM_NAVIGATION = "setup_pending_hide_system_navigation"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) {
            if (uiState.currentPage > 0) {
                uiState.currentPage -= 1
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }

        CrashHandler.install(this)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getBoolean("setup_complete", false)) { startMainActivity(); return }

        var hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        var hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)
        if (prefs.contains(KEY_PENDING_HIDE_STATUS_BAR)) {
            hideStatusBar = prefs.getBoolean(KEY_PENDING_HIDE_STATUS_BAR, false)
            prefs.edit().remove(KEY_PENDING_HIDE_STATUS_BAR).apply()
        }
        if (prefs.contains(KEY_PENDING_HIDE_SYSTEM_NAVIGATION)) {
            hideSystemNavigation = prefs.getBoolean(KEY_PENDING_HIDE_SYSTEM_NAVIGATION, false)
            prefs.edit().remove(KEY_PENDING_HIDE_SYSTEM_NAVIGATION).apply()
        }
        var consentChecked = false
        if (prefs.contains(KEY_PENDING_CONSENT)) {
            consentChecked = prefs.getBoolean(KEY_PENDING_CONSENT, false)
            prefs.edit().remove(KEY_PENDING_CONSENT).apply()
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val pendingPage = prefs.getInt(KEY_PENDING_PAGE, -1)
        val initialPage: Int
        val initialScroll: Int
        if (pendingPage >= 0) {
            prefs.edit().remove(KEY_PENDING_PAGE).apply()
            initialPage = pendingPage
            initialScroll = prefs.getInt(KEY_PENDING_SCROLL, 0)
            if (initialScroll > 0) prefs.edit().remove(KEY_PENDING_SCROLL).apply()
        } else {
            initialPage = 0
            initialScroll = 0
        }

        uiState = SetupUiState(
            initialPage = initialPage,
            initialScrollY = initialScroll,
            initialTheme = prefs.getString("app_theme", "system") ?: "system",
            initialAccent = prefs.getString("accent_color", "material_you") ?: "material_you",
            initialIntensity = prefs.getString("surface_intensity", "soft_tint") ?: "soft_tint",
            initialLanguage = prefs.getString(LocaleHelper.PREF_APP_LANGUAGE, LocaleHelper.LANGUAGE_SYSTEM) ?: LocaleHelper.LANGUAGE_SYSTEM,
            initialAddressBarPosition = prefs.getString("address_bar_position", "top") ?: "top",
            initialMenuStyle = prefs.getString("menu_style", "popup") ?: "popup",
            initialScrollHideMode = prefs.getString("scroll_hide_mode", "off") ?: "off",
            initialHideStatusBar = hideStatusBar, initialHideSystemNavigation = hideSystemNavigation,
            initialEngine = "duckduckgo",
            initialCustomEngineName = "", initialCustomEngineUrl = ""
        )
        uiState.consentChecked = consentChecked
        if (uiState.currentPage == 5) refreshDefaultBrowserState()

        setContent {
          com.jhaiian.clint.ui.theme.ClintComposeTheme(theme = uiState.theme) {
            SetupScreen(
                activity = this,
                state = uiState,
                onPrivacyClick = {
                    DocumentViewer.show(this, getString(R.string.document_viewer_privacy_policy_title), DocumentViewer.PRIVACY_POLICY_URL)
                },
                onTermsClick = {
                    DocumentViewer.show(this, getString(R.string.document_viewer_terms_title), DocumentViewer.TERMS_URL)
                },
                onHideStatusBarToggled = { checked ->
                    uiState.hideStatusBar = checked
                    Toast.makeText(this, getString(R.string.setup_status_bar_applied_later), Toast.LENGTH_SHORT).show()
                },
                onHideSystemNavigationToggled = { checked ->
                    uiState.hideSystemNavigation = checked
                    Toast.makeText(this, getString(R.string.setup_status_bar_applied_later), Toast.LENGTH_SHORT).show()
                },
                onThemeSelected = { theme -> onSetupThemeSelected(theme) },
                onAccentSelected = { accent -> onSetupAccentSelected(accent) },
                onIntensitySelected = { intensity -> onSetupIntensitySelected(intensity) },
                onLanguageSelected = { language -> onSetupLanguageSelected(language) },
                onAddressBarPositionSelected = { position ->
                    uiState.addressBarPosition = position
                    uiState.scrollHideMode = sanitizeScrollHideMode(uiState.scrollHideMode, position)
                },
                onMenuStyleSelected = { style -> uiState.menuStyle = style },
                onScrollHideModeSelected = { mode -> uiState.scrollHideMode = mode },
                onEngineSelected = { engine -> uiState.engine = engine },
                onCustomEngineSaved = { name, url ->
                    uiState.customEngineName = name
                    uiState.customEngineUrl = url
                    uiState.engine = "custom"
                },
                onContinueFromWelcome = { uiState.currentPage = 1 },
                onSkipRestore = { uiState.currentPage = 2 },
                onRestoreComplete = { restartAppAfterRestore() },
                onNextFromLayoutPage = { onNextFromLayoutPage() },
                onNextFromEnginePage = { onNextFromEnginePage() },
                onSetDefaultBrowser = {
                    if (uiState.isDefaultBrowser) saveAndProceed() else openDefaultBrowserPicker()
                },
                onSkipDefaultBrowser = { saveAndProceed() }
            )
            com.jhaiian.clint.ui.listscreen.ConfirmDialogHost(uiState.confirmDialogConfig, uiState.hideStatusBar, uiState.hideSystemNavigation) { uiState.confirmDialogConfig = null }
            overlayContent?.invoke()
          }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::uiState.isInitialized && uiState.currentPage == 5) refreshDefaultBrowserState()
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

    private fun openDefaultBrowserPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val roleManager = getSystemService(RoleManager::class.java)
                if (!roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
                    browserRoleLauncher.launch(intent)
                    return
                }
            } catch (_: Exception) {}
        }
        startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
    }

    private fun onNextFromLayoutPage() {
        uiState.currentPage = 4
    }

    private fun onNextFromEnginePage() {
        if (uiState.engine == "google") {
            uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
                title = getString(R.string.google_warning_title),
                message = getString(R.string.google_warning_message),
                positiveLabel = getString(R.string.use_google_anyway),
                onPositive = { goToDefaultBrowserPage() },
                negativeLabel = getString(R.string.choose_another)
            )
        } else {
            goToDefaultBrowserPage()
        }
    }

    private fun goToDefaultBrowserPage() {
        uiState.currentPage = 5
        refreshDefaultBrowserState()
    }

    private fun onSetupThemeSelected(theme: String) {
        if (theme == uiState.theme) { uiState.currentPage = 3; return }
        savePendingNavigationState()
        captureAndRecreate(theme)
    }

    private fun onSetupAccentSelected(accent: String) {
        if (accent == uiState.accent) return
        savePendingNavigationState()
        captureAndApplyAccentColor(accent)
    }

    private fun onSetupIntensitySelected(intensity: String) {
        if (intensity == uiState.intensity) return
        savePendingNavigationState()
        captureAndApplySurfaceIntensity(intensity)
    }

    private fun onSetupLanguageSelected(language: String) {
        if (language == uiState.language) return
        savePendingNavigationState(0)
        captureAndApplyLanguage(language)
    }

    private fun savePendingNavigationState(page: Int = 2) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit()
            .putInt(KEY_PENDING_PAGE, page)
            .putInt(KEY_PENDING_SCROLL, if (page == 2) uiState.themePageScrollState.value else 0)
            .putBoolean(KEY_PENDING_CONSENT, uiState.consentChecked)
            .putBoolean(KEY_PENDING_HIDE_STATUS_BAR, uiState.hideStatusBar)
            .putBoolean(KEY_PENDING_HIDE_SYSTEM_NAVIGATION, uiState.hideSystemNavigation)
            .apply()
    }

    private fun saveAndProceed() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit()
            .putString("search_engine", uiState.engine)
            .putString(com.jhaiian.clint.browser.CustomSearchEngineNameKey, uiState.customEngineName)
            .putString(com.jhaiian.clint.browser.CustomSearchEngineUrlKey, uiState.customEngineUrl)
            .putString("address_bar_position", uiState.addressBarPosition)
            .putString("menu_style", uiState.menuStyle)
            .putString("scroll_hide_mode", uiState.scrollHideMode)
            .putBoolean("hide_status_bar", uiState.hideStatusBar)
            .putBoolean("hide_system_navigation", uiState.hideSystemNavigation)
            .putBoolean("setup_complete", true)
            .apply()
        startMainActivity()
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
