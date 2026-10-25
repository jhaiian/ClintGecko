package com.jhaiian.clint.settings.translations

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.webview.ClintGecko
import com.jhaiian.clint.settings.SettingsActivity
import com.jhaiian.clint.settings.common.RowDivider
import com.jhaiian.clint.settings.common.SettingsIconTile
import com.jhaiian.clint.settings.common.SettingsPickerOptionBottomSpacing
import com.jhaiian.clint.settings.common.SettingsPickerOptionContentPadding
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.settings.common.SettingsSectionLabel
import com.jhaiian.clint.setup.SelectableCard
import com.jhaiian.clint.translation.ClintTranslations
import com.jhaiian.clint.translation.PREF_TRANSLATION_OFFER
import com.jhaiian.clint.translation.PREF_TRANSLATION_SHOW_BUTTON
import com.jhaiian.clint.translation.TranslationLanguage
import com.jhaiian.clint.translation.TranslationLanguageSetting
import com.jhaiian.clint.translation.TranslationModel
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintDialogActionFooter
import com.jhaiian.clint.ui.ClintRadioButton
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.scrollToSelection
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatFileSize
import kotlinx.coroutines.launch

private sealed interface TranslationsSettingsDialog {
    data object LanguagePreferences : TranslationsSettingsDialog
    data class LanguageSetting(val language: TranslationLanguage) : TranslationsSettingsDialog
    data object NeverSites : TranslationsSettingsDialog
    data object DeleteAll : TranslationsSettingsDialog
}

@Composable
fun TranslationsSettingsPane(activity: SettingsActivity) {
    val colors = LocalClintColors.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(activity) }
    val hideStatusBar = remember { prefs.getBoolean("hide_status_bar", false) }
    val hideSystemNavigation = remember { prefs.getBoolean("hide_system_navigation", false) }
    val configuration = LocalConfiguration.current
    val locale = remember(configuration) { ClintTranslations.appLocale(activity) }
    val scope = rememberCoroutineScope()

    var offerEnabled by remember { mutableStateOf(prefs.getBoolean(PREF_TRANSLATION_OFFER, true)) }
    var showButton by remember { mutableStateOf(prefs.getBoolean(PREF_TRANSLATION_SHOW_BUTTON, true)) }
    var loading by remember { mutableStateOf(true) }
    var engineSupported by remember { mutableStateOf(true) }
    var models by remember { mutableStateOf<List<TranslationModel>?>(null) }
    var busyLanguages by remember { mutableStateOf(emptySet<String>()) }
    var deletingAll by remember { mutableStateOf(false) }
    var supportedLanguages by remember { mutableStateOf<List<TranslationLanguage>>(emptyList()) }
    var languageSettings by remember { mutableStateOf<Map<String, TranslationLanguageSetting>>(emptyMap()) }
    var neverSites by remember { mutableStateOf<List<String>>(emptyList()) }
    var dialog by remember { mutableStateOf<TranslationsSettingsDialog?>(null) }

    LaunchedEffect(Unit) {
        ClintGecko.init(activity)
        engineSupported = ClintTranslations.isEngineSupported()
        if (engineSupported) {
            models = ClintTranslations.models(locale)
            supportedLanguages = ClintTranslations.supportedLanguages(locale)?.fromLanguages.orEmpty()
            languageSettings = ClintTranslations.languageSettings().orEmpty()
            neverSites = ClintTranslations.neverTranslateSites().orEmpty()
        }
        loading = false
    }

    val preferenceLanguages = remember(supportedLanguages, languageSettings, locale) {
        val codes = (supportedLanguages.map { it.code } + languageSettings.keys).distinct()
        codes.map { code ->
            supportedLanguages.firstOrNull { it.code == code } ?: TranslationLanguage(code, ClintTranslations.displayName(code, locale))
        }.sortedBy { it.name.lowercase(locale) }
    }

    fun showFailure() {
        Toast.makeText(activity, R.string.translation_operation_failed, Toast.LENGTH_SHORT).show()
    }

    fun toggleModel(model: TranslationModel) {
        val code = model.language.code
        if (code in busyLanguages || deletingAll) return
        busyLanguages = busyLanguages + code
        scope.launch {
            val success = if (model.isDownloaded) ClintTranslations.deleteLanguage(code) else ClintTranslations.downloadLanguage(code)
            if (!success) showFailure()
            ClintTranslations.models(locale)?.let { models = it }
            busyLanguages = busyLanguages - code
        }
    }

    fun deleteAll() {
        if (deletingAll) return
        deletingAll = true
        scope.launch {
            if (!ClintTranslations.deleteAllLanguages()) showFailure()
            ClintTranslations.models(locale)?.let { models = it }
            deletingAll = false
        }
    }

    val controlsEnabled = !loading && engineSupported

    SettingsScreenScaffold(
        overlay = {
            when (val current = dialog) {
                TranslationsSettingsDialog.LanguagePreferences -> LanguagePreferencesDialog(
                    languages = preferenceLanguages,
                    settings = languageSettings,
                    hideStatusBar = hideStatusBar,
                    hideSystemNavigation = hideSystemNavigation,
                    onLanguageClick = { dialog = TranslationsSettingsDialog.LanguageSetting(it) },
                    onDismiss = { dialog = null }
                )
                is TranslationsSettingsDialog.LanguageSetting -> LanguageSettingDialog(
                    language = current.language,
                    current = languageSettings[current.language.code] ?: TranslationLanguageSetting.OFFER,
                    hideStatusBar = hideStatusBar,
                    hideSystemNavigation = hideSystemNavigation,
                    onSelect = { setting ->
                        val code = current.language.code
                        dialog = TranslationsSettingsDialog.LanguagePreferences
                        scope.launch {
                            if (ClintTranslations.setLanguageSetting(code, setting)) {
                                languageSettings = languageSettings + (code to setting)
                            } else {
                                showFailure()
                            }
                        }
                    },
                    onDismiss = { dialog = TranslationsSettingsDialog.LanguagePreferences }
                )
                TranslationsSettingsDialog.NeverSites -> NeverTranslateSitesDialog(
                    sites = neverSites,
                    hideStatusBar = hideStatusBar,
                    hideSystemNavigation = hideSystemNavigation,
                    onRemove = { site ->
                        scope.launch {
                            if (ClintTranslations.removeNeverTranslateSite(site)) {
                                neverSites = neverSites - site
                            } else {
                                showFailure()
                            }
                        }
                    },
                    onDismiss = { dialog = null }
                )
                TranslationsSettingsDialog.DeleteAll -> ClintDialog(
                    title = stringResource(R.string.translation_delete_all_confirm_title),
                    hideStatusBar = hideStatusBar,
                    hideSystemNavigation = hideSystemNavigation,
                    onDismiss = { dialog = null },
                    footer = {
                        ClintDialogActionFooter(
                            onCancel = { dialog = null },
                            positiveLabel = stringResource(R.string.action_delete),
                            onPositive = {
                                dialog = null
                                deleteAll()
                            }
                        )
                    }
                ) {
                    Text(
                        stringResource(R.string.translation_delete_all_confirm_message),
                        color = colors.onSurface,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                null -> Unit
            }
        }
    ) {
        SettingsSection(colors.cardBackground) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                SettingsIconTile(Icons.Filled.PrivacyTip, colors)
                Text(
                    stringResource(R.string.translation_settings_privacy),
                    color = colors.secondaryText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.weight(1f).padding(start = 16.dp)
                )
            }
        }

        SettingsSectionLabel(stringResource(R.string.translation_section_general))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = Icons.Filled.Translate,
                title = stringResource(R.string.translation_offer_title),
                summary = stringResource(R.string.translation_offer_summary),
                colors = colors,
                onClick = {
                    offerEnabled = !offerEnabled
                    prefs.edit().putBoolean(PREF_TRANSLATION_OFFER, offerEnabled).apply()
                    ClintTranslations.applyPrefs(activity)
                },
                trailing = { ClintSwitch(checked = offerEnabled) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.SmartButton,
                title = stringResource(R.string.translation_button_title),
                summary = stringResource(R.string.translation_button_summary),
                colors = colors,
                onClick = {
                    showButton = !showButton
                    prefs.edit().putBoolean(PREF_TRANSLATION_SHOW_BUTTON, showButton).apply()
                },
                trailing = { ClintSwitch(checked = showButton) }
            )
        }

        if (!loading && !engineSupported) {
            SettingsSection(colors.cardBackground) {
                Text(
                    stringResource(R.string.translation_engine_unsupported),
                    color = colors.secondaryText,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            TranslationsManagementSections(
                loading = loading,
                controlsEnabled = controlsEnabled,
                hasPreferenceLanguages = preferenceLanguages.isNotEmpty(),
                neverSiteCount = neverSites.size,
                models = models,
                busyLanguages = busyLanguages,
                deletingAll = deletingAll,
                onLanguagePreferencesClick = { dialog = TranslationsSettingsDialog.LanguagePreferences },
                onNeverSitesClick = { dialog = TranslationsSettingsDialog.NeverSites },
                onModelClick = { toggleModel(it) },
                onDeleteAllClick = { dialog = TranslationsSettingsDialog.DeleteAll }
            )
        }
    }
}

@Composable
private fun TranslationsManagementSections(
    loading: Boolean,
    controlsEnabled: Boolean,
    hasPreferenceLanguages: Boolean,
    neverSiteCount: Int,
    models: List<TranslationModel>?,
    busyLanguages: Set<String>,
    deletingAll: Boolean,
    onLanguagePreferencesClick: () -> Unit,
    onNeverSitesClick: () -> Unit,
    onModelClick: (TranslationModel) -> Unit,
    onDeleteAllClick: () -> Unit
) {
    val colors = LocalClintColors.current
    Column(Modifier.fillMaxWidth()) {
        SettingsSectionLabel(stringResource(R.string.translation_section_automatic))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = Icons.Filled.Tune,
                title = stringResource(R.string.translation_language_preferences_title),
                summary = stringResource(R.string.translation_language_preferences_summary),
                colors = colors,
                enabled = controlsEnabled && hasPreferenceLanguages,
                onClick = {
                    if (controlsEnabled && hasPreferenceLanguages) onLanguagePreferencesClick()
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.Block,
                title = stringResource(R.string.translation_never_sites_title),
                summary = if (neverSiteCount == 0) {
                    stringResource(R.string.translation_never_sites_summary_none)
                } else {
                    pluralStringResource(R.plurals.translation_never_sites_count, neverSiteCount, neverSiteCount)
                },
                colors = colors,
                enabled = controlsEnabled,
                onClick = { if (controlsEnabled) onNeverSitesClick() }
            )
        }

        SettingsSectionLabel(stringResource(R.string.translation_section_languages))
        SettingsSection(colors.cardBackground) {
            val currentModels = models
            when {
                loading -> Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                }
                currentModels == null -> Text(
                    stringResource(R.string.translation_languages_unavailable),
                    color = colors.secondaryText,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(16.dp)
                )
                else -> {
                    Text(
                        stringResource(R.string.translation_languages_hint),
                        color = colors.secondaryText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                    currentModels.forEach { model ->
                        RowDivider(colors.divider)
                        TranslationModelRow(
                            model = model,
                            busy = deletingAll || model.language.code in busyLanguages,
                            onClick = { onModelClick(model) }
                        )
                    }
                    if (currentModels.any { it.isDownloaded }) {
                        RowDivider(colors.divider)
                        SettingsRow(
                            icon = Icons.Filled.DeleteSweep,
                            title = stringResource(R.string.translation_delete_all_title),
                            summary = stringResource(R.string.translation_delete_all_summary),
                            colors = colors,
                            enabled = !deletingAll,
                            onClick = { if (!deletingAll) onDeleteAllClick() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TranslationModelRow(model: TranslationModel, busy: Boolean, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    val name = model.language.name
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                name,
                color = colors.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                stringResource(
                    if (model.isDownloaded) R.string.translation_model_downloaded else R.string.translation_model_not_downloaded,
                    formatFileSize(model.size)
                ),
                color = colors.secondaryText,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (busy) {
                CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                IconButton(onClick = onClick) {
                    Icon(
                        imageVector = if (model.isDownloaded) Icons.Filled.Delete else Icons.Filled.Download,
                        contentDescription = stringResource(
                            if (model.isDownloaded) R.string.translation_model_delete else R.string.translation_model_download,
                            name
                        ),
                        tint = if (model.isDownloaded) colors.iconTint else colors.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun TranslationsCloseFooter(onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.action_close), color = colors.primary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun languageSettingLabel(setting: TranslationLanguageSetting): String = stringResource(
    when (setting) {
        TranslationLanguageSetting.OFFER -> R.string.translation_language_offer
        TranslationLanguageSetting.ALWAYS -> R.string.translation_language_always
        TranslationLanguageSetting.NEVER -> R.string.translation_language_never
    }
)

@Composable
private fun LanguagePreferencesDialog(
    languages: List<TranslationLanguage>,
    settings: Map<String, TranslationLanguageSetting>,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onLanguageClick: (TranslationLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    ClintDialog(
        title = stringResource(R.string.translation_language_preferences_title),
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = { TranslationsCloseFooter(onDismiss) }
    ) {
        languages.forEach { language ->
            val setting = settings[language.code] ?: TranslationLanguageSetting.OFFER
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable { onLanguageClick(language) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(language.name, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(
                    languageSettingLabel(setting),
                    color = if (setting == TranslationLanguageSetting.OFFER) colors.secondaryText else colors.primary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun LanguageSettingDialog(
    language: TranslationLanguage,
    current: TranslationLanguageSetting,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onSelect: (TranslationLanguageSetting) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val scrollState = rememberScrollState()
    ClintDialog(
        title = language.name,
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        scrollState = scrollState
    ) {
        TranslationLanguageSetting.entries.forEach { setting ->
            val selected = setting == current
            SelectableCard(
                selected = selected,
                onClick = { onSelect(setting) },
                cardBackground = colors.surfaceVariant,
                primary = colors.primary,
                contentPadding = SettingsPickerOptionContentPadding,
                bottomSpacing = SettingsPickerOptionBottomSpacing,
                modifier = Modifier.scrollToSelection(scrollState, selected)
            ) {
                ClintRadioButton(selected = selected)
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text(languageSettingLabel(setting), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(
                        stringResource(
                            when (setting) {
                                TranslationLanguageSetting.OFFER -> R.string.translation_language_offer_desc
                                TranslationLanguageSetting.ALWAYS -> R.string.translation_language_always_desc
                                TranslationLanguageSetting.NEVER -> R.string.translation_language_never_desc
                            }
                        ),
                        color = colors.secondaryText,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NeverTranslateSitesDialog(
    sites: List<String>,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    ClintDialog(
        title = stringResource(R.string.translation_never_sites_title),
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = { TranslationsCloseFooter(onDismiss) }
    ) {
        if (sites.isEmpty()) {
            Text(
                stringResource(R.string.translation_never_sites_empty),
                color = colors.secondaryText,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        sites.forEach { site ->
            val label = site.substringAfter("://")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 16.dp, end = 4.dp)
            ) {
                Text(
                    label,
                    color = colors.onSurface,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                IconButton(onClick = { onRemove(site) }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.translation_remove_site, label),
                        tint = colors.iconTint
                    )
                }
            }
        }
    }
}
