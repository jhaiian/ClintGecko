package com.jhaiian.clint.browser.delegates

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.webview.ClintWebView
import com.jhaiian.clint.settings.SettingsActivity
import com.jhaiian.clint.translation.ClintTranslations
import com.jhaiian.clint.translation.PREF_TRANSLATION_OFFER
import com.jhaiian.clint.translation.PREF_TRANSLATION_SHOW_BUTTON
import com.jhaiian.clint.translation.TranslateBarRequest
import com.jhaiian.clint.ui.showClintSnackbar
import kotlinx.coroutines.launch

private const val SETTINGS_DESTINATION_TRANSLATIONS = "translations"

internal fun MainActivity.setupTranslations() {
    uiState.showTranslateButton = prefs.getBoolean(PREF_TRANSLATION_SHOW_BUTTON, true)
    ClintTranslations.offerHandler = { view -> runOnUiThread { onTranslationOffered(view) } }
}

internal fun MainActivity.teardownTranslations() {
    ClintTranslations.offerHandler = null
}

internal fun MainActivity.onTranslationPrefChanged(key: String?) {
    when (key) {
        PREF_TRANSLATION_SHOW_BUTTON -> uiState.showTranslateButton = prefs.getBoolean(PREF_TRANSLATION_SHOW_BUTTON, true)
        PREF_TRANSLATION_OFFER -> ClintTranslations.applyPrefs(this)
    }
}

private fun MainActivity.onTranslationOffered(view: ClintWebView) {
    if (isFinishing || isDestroyed) return
    val tab = tabManager.activeTab ?: return
    if (tab.webView !== view) return
    if (!prefs.getBoolean(PREF_TRANSLATION_OFFER, true)) return
    if (uiState.translateBarRequest != null) return
    val state = view.translator.state.value
    if (!state.canTranslate || state.isRequested) return
    uiState.translateBarRequest = TranslateBarRequest(view, tab.id, allowSiteSetting = !tab.isIncognito, offered = true)
}

internal fun MainActivity.openTranslatePage() {
    val tab = tabManager.activeTab ?: return
    val url = tab.webView.url ?: return
    if (!url.startsWith("http://") && !url.startsWith("https://")) return
    if (uiState.translateBarRequest?.webView === tab.webView) {
        uiState.translateBarRequest = null
        return
    }
    uiState.translateBarRequest = TranslateBarRequest(tab.webView, tab.id, allowSiteSetting = !tab.isIncognito, offered = false)
}

internal fun MainActivity.translatePage(view: ClintWebView, from: String, to: String) {
    ClintTranslations.saveTarget(this, to)
    lifecycleScope.launch {
        val translated = view.translator.translate(from, to)
        val barShowsError = uiState.translateBarRequest?.webView === view && view.translator.state.value.error != null
        if (!translated && !isFinishing && !barShowsError) {
            showClintSnackbar(getString(R.string.translation_failed))
        }
    }
}

internal fun MainActivity.restoreTranslatedPage(view: ClintWebView) {
    lifecycleScope.launch {
        if (!view.translator.restoreOriginal() && !isFinishing) {
            showClintSnackbar(getString(R.string.translation_restore_failed))
        }
    }
}

internal fun MainActivity.openTranslationSettings() {
    startActivity(
        Intent(this, SettingsActivity::class.java)
            .putExtra(SettingsActivity.EXTRA_OPEN_FRAGMENT, SETTINGS_DESTINATION_TRANSLATIONS)
    )
}
