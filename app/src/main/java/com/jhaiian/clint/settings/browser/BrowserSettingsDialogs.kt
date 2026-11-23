package com.jhaiian.clint.settings.browser
import com.jhaiian.clint.browser.home.HOMEPAGE_SEARCH_ENGINE
import com.jhaiian.clint.tabs.InactiveTabsPolicy
import com.jhaiian.clint.browser.home.HOMEPAGE_CLINT
import com.jhaiian.clint.browser.home.HOMEPAGE_CUSTOM
import com.jhaiian.clint.browser.home.normalizeCustomHomepageUrl
import com.jhaiian.clint.ui.ClintOutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.jhaiian.clint.browser.home.HOMEPAGE_DESIGN_CUSTOM_GRADIENT
import com.jhaiian.clint.browser.home.HOMEPAGE_DESIGN_GRADIENT
import com.jhaiian.clint.browser.home.HOMEPAGE_DESIGN_SOLID
import com.jhaiian.clint.browser.home.HOMEPAGE_DESIGN_IMAGE
import com.jhaiian.clint.browser.home.HOMEPAGE_DESIGN_PLAIN

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.jhaiian.clint.ui.ClintRadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.settings.common.SettingsPickerOptionBottomSpacing
import com.jhaiian.clint.settings.common.SettingsPickerOptionContentPadding
import com.jhaiian.clint.setup.DefaultChip
import com.jhaiian.clint.setup.SelectableCard
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
private fun SameAsNormalOption(selected: Boolean, descRes: Int, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    SelectableCard(
        selected = selected, onClick = onClick,
        cardBackground = colors.surfaceVariant, primary = colors.primary,
        contentPadding = SettingsPickerOptionContentPadding, bottomSpacing = SettingsPickerOptionBottomSpacing
    ) {
        ClintRadioButton(selected = selected)
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            Text(stringResource(R.string.search_same_as_normal), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(stringResource(descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }
        DefaultChip(stringResource(R.string.default_label), colors.primary)
    }
}

@Composable
fun SearchEngineDialog(
    current: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    titleRes: Int = R.string.choose_search_engine,
    showSameAsNormal: Boolean = false
) {
    val colors = LocalClintColors.current
    var selected by remember(current) { mutableStateOf(current) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { androidx.preference.PreferenceManager.getDefaultSharedPreferences(context) }
    var customEngines by remember { mutableStateOf(com.jhaiian.clint.browser.customSearchEngines(prefs)) }
    var editorTarget by remember { mutableStateOf<com.jhaiian.clint.browser.CustomSearchEngine?>(null) }
    var customEditorOpen by remember { mutableStateOf(false) }
    val fallback = if (showSameAsNormal) com.jhaiian.clint.browser.SameAsNormalMode else com.jhaiian.clint.browser.DefaultSearchEngine

    if (customEditorOpen) {
        CustomEngineEditorDialog(
            initial = editorTarget,
            forSuggestions = false,
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            onSave = { engine ->
                com.jhaiian.clint.browser.saveCustomSearchEngine(prefs, engine)
                customEngines = com.jhaiian.clint.browser.customSearchEngines(prefs)
                selected = engine.key
                customEditorOpen = false
            },
            onDismiss = { customEditorOpen = false }
        )
    }

    ClintDialog(
        title = stringResource(titleRes),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onConfirm(selected) }) {
                    Text(stringResource(android.R.string.ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        if (showSameAsNormal) {
            SameAsNormalOption(
                selected = selected == com.jhaiian.clint.browser.SameAsNormalMode,
                descRes = R.string.search_engine_same_as_normal_desc,
                onClick = { selected = com.jhaiian.clint.browser.SameAsNormalMode }
            )
        }
        data class EngineOption(val key: String, val titleRes: Int, val descRes: Int, val showDefault: Boolean)
        listOf(
            EngineOption("duckduckgo", R.string.engine_duckduckgo, R.string.engine_duckduckgo_desc, true),
            EngineOption("brave", R.string.engine_brave, R.string.engine_brave_desc, false),
            EngineOption("ecosia", R.string.engine_ecosia, R.string.engine_ecosia_desc, false),
            EngineOption("google", R.string.engine_google, R.string.engine_google_desc, false)
        ).forEach { option ->
            val sel = selected == option.key
            SelectableCard(
                selected = sel, onClick = { selected = option.key },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = SettingsPickerOptionContentPadding, bottomSpacing = SettingsPickerOptionBottomSpacing
            ) {
                ClintRadioButton(selected = sel)
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                if (option.showDefault && !showSameAsNormal) DefaultChip(stringResource(R.string.default_label), colors.primary)
            }
        }
        CustomEngineOptions(
            engines = customEngines.filter { it.hasSearch },
            selected = selected,
            forSuggestions = false,
            onSelect = { selected = it },
            onEdit = { editorTarget = it; customEditorOpen = true },
            onDelete = { engine ->
                com.jhaiian.clint.browser.removeCustomSearchEngineRole(prefs, engine, suggestions = false)
                customEngines = com.jhaiian.clint.browser.customSearchEngines(prefs)
                if (selected == engine.key) selected = fallback
            },
            onAdd = { editorTarget = null; customEditorOpen = true }
        )
    }
}

@Composable
fun SearchSuggestionsApiDialog(
    current: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    titleRes: Int = R.string.choose_search_suggestions_api,
    showSameAsNormal: Boolean = false
) {
    val colors = LocalClintColors.current
    var selected by remember(current) { mutableStateOf(current) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { androidx.preference.PreferenceManager.getDefaultSharedPreferences(context) }
    var customEngines by remember { mutableStateOf(com.jhaiian.clint.browser.customSearchEngines(prefs)) }
    var editorTarget by remember { mutableStateOf<com.jhaiian.clint.browser.CustomSearchEngine?>(null) }
    var customEditorOpen by remember { mutableStateOf(false) }
    val fallback = if (showSameAsNormal) com.jhaiian.clint.browser.SameAsNormalMode else com.jhaiian.clint.browser.DefaultSearchSuggestionsApi

    if (customEditorOpen) {
        CustomEngineEditorDialog(
            initial = editorTarget,
            forSuggestions = true,
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            onSave = { engine ->
                com.jhaiian.clint.browser.saveCustomSearchEngine(prefs, engine)
                customEngines = com.jhaiian.clint.browser.customSearchEngines(prefs)
                selected = engine.key
                customEditorOpen = false
            },
            onDismiss = { customEditorOpen = false }
        )
    }

    ClintDialog(
        title = stringResource(titleRes),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onConfirm(selected) }) {
                    Text(stringResource(android.R.string.ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        if (showSameAsNormal) {
            SameAsNormalOption(
                selected = selected == com.jhaiian.clint.browser.SameAsNormalMode,
                descRes = R.string.suggestions_api_same_as_normal_desc,
                onClick = { selected = com.jhaiian.clint.browser.SameAsNormalMode }
            )
        }
        data class SuggestionsApiOption(val key: String, val titleRes: Int, val descRes: Int, val showDefault: Boolean)
        listOf(
            SuggestionsApiOption("duckduckgo", R.string.engine_duckduckgo, R.string.suggestions_api_duckduckgo_desc, true),
            SuggestionsApiOption("google", R.string.engine_google, R.string.suggestions_api_google_desc, false),
            SuggestionsApiOption("off", R.string.suggestions_api_off, R.string.suggestions_api_off_desc, false)
        ).forEach { option ->
            val sel = selected == option.key
            SelectableCard(
                selected = sel, onClick = { selected = option.key },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = SettingsPickerOptionContentPadding, bottomSpacing = SettingsPickerOptionBottomSpacing
            ) {
                ClintRadioButton(selected = sel)
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                if (option.showDefault && !showSameAsNormal) DefaultChip(stringResource(R.string.default_label), colors.primary)
            }
        }
        CustomEngineOptions(
            engines = customEngines.filter { it.hasSuggestions },
            selected = selected,
            forSuggestions = true,
            onSelect = { selected = it },
            onEdit = { editorTarget = it; customEditorOpen = true },
            onDelete = { engine ->
                com.jhaiian.clint.browser.removeCustomSearchEngineRole(prefs, engine, suggestions = true)
                customEngines = com.jhaiian.clint.browser.customSearchEngines(prefs)
                if (selected == engine.key) selected = fallback
            },
            onAdd = { editorTarget = null; customEditorOpen = true }
        )
    }
}

@Composable
fun HomepageDialog(
    current: String,
    currentCustomUrl: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var selected by remember(current) { mutableStateOf(current) }
    var customUrl by remember(currentCustomUrl) { mutableStateOf(currentCustomUrl) }
    var customUrlError by remember { mutableStateOf(false) }

    ClintDialog(
        title = stringResource(R.string.choose_homepage),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(
                    onClick = {
                        if (selected == HOMEPAGE_CUSTOM) {
                            val normalized = normalizeCustomHomepageUrl(customUrl)
                            if (normalized == null) customUrlError = true else onConfirm(selected, normalized)
                        } else {
                            onConfirm(selected, currentCustomUrl)
                        }
                    }
                ) {
                    Text(stringResource(android.R.string.ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        data class HomepageOption(val key: String, val titleRes: Int, val descRes: Int, val showDefault: Boolean)
        listOf(
            HomepageOption(HOMEPAGE_CLINT, R.string.homepage_clint, R.string.homepage_clint_desc, true),
            HomepageOption(HOMEPAGE_SEARCH_ENGINE, R.string.homepage_search_engine, R.string.homepage_search_engine_desc, false),
            HomepageOption(HOMEPAGE_CUSTOM, R.string.homepage_custom, R.string.homepage_custom_desc, false)
        ).forEach { option ->
            val sel = selected == option.key
            SelectableCard(
                selected = sel, onClick = { selected = option.key },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = SettingsPickerOptionContentPadding, bottomSpacing = SettingsPickerOptionBottomSpacing
            ) {
                ClintRadioButton(selected = sel)
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                if (option.showDefault) DefaultChip(stringResource(R.string.default_label), colors.primary)
            }
            if (option.key == HOMEPAGE_CUSTOM && sel) {
                ClintOutlinedTextField(
                    value = customUrl,
                    onValueChange = { customUrl = it; customUrlError = false },
                    label = { Text(stringResource(R.string.homepage_custom_url_hint)) },
                    singleLine = true,
                    isError = customUrlError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    supportingText = {
                        Text(
                            stringResource(if (customUrlError) R.string.homepage_custom_url_error else R.string.homepage_custom_url_helper),
                            color = if (customUrlError) colors.colorError else colors.secondaryText,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
fun VideoSkipDialog(
    current: Int,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var selected by remember(current) { mutableStateOf(current) }

    ClintDialog(
        title = stringResource(R.string.video_player_skip_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onConfirm(selected) }) {
                    Text(stringResource(android.R.string.ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        com.jhaiian.clint.browser.video.VideoPlayerSettings.SKIP_OPTIONS.forEach { seconds ->
            val sel = selected == seconds
            SelectableCard(
                selected = sel, onClick = { selected = seconds },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = SettingsPickerOptionContentPadding, bottomSpacing = SettingsPickerOptionBottomSpacing
            ) {
                ClintRadioButton(selected = sel)
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text(stringResource(R.string.video_player_skip_value, seconds), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                if (seconds == com.jhaiian.clint.browser.video.VideoPlayerSettings.DEFAULT_SKIP_SECONDS) DefaultChip(stringResource(R.string.default_label), colors.primary)
            }
        }
    }
}

@Composable
fun VideoButtonsDialog(
    current: Set<String>,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var selected by remember(current) { mutableStateOf(current) }

    ClintDialog(
        title = stringResource(R.string.video_player_buttons_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onConfirm(selected) }) {
                    Text(stringResource(android.R.string.ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        com.jhaiian.clint.browser.video.VideoButton.entries.forEach { button ->
            val shown = button.key !in selected
            val toggle = { selected = if (shown) selected + button.key else selected - button.key }
            SelectableCard(
                selected = shown, onClick = toggle,
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = SettingsPickerOptionContentPadding, bottomSpacing = SettingsPickerOptionBottomSpacing
            ) {
                com.jhaiian.clint.ui.ClintCheckbox(checked = shown, onCheckedChange = { toggle() })
                Text(stringResource(button.label), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp))
            }
        }
    }
}

@Composable
fun VideoChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    defaultKey: String,
    current: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var selected by remember(current) { mutableStateOf(current) }

    ClintDialog(
        title = title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onConfirm(selected) }) {
                    Text(stringResource(android.R.string.ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        options.forEach { (key, label) ->
            val sel = selected == key
            SelectableCard(
                selected = sel, onClick = { selected = key },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = SettingsPickerOptionContentPadding, bottomSpacing = SettingsPickerOptionBottomSpacing
            ) {
                ClintRadioButton(selected = sel)
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text(label, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                if (key == defaultKey) DefaultChip(stringResource(R.string.default_label), colors.primary)
            }
        }
    }
}

@Composable
fun InactiveTabsDialog(
    current: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var selected by remember(current) { mutableStateOf(current) }

    ClintDialog(
        title = stringResource(R.string.delete_inactive_tabs_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onConfirm(selected) }) {
                    Text(stringResource(android.R.string.ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Text(
            stringResource(R.string.delete_inactive_tabs_dialog_desc),
            color = colors.secondaryText, fontSize = 13.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
        data class InactiveTabsOption(val key: String, val titleRes: Int)
        listOf(
            InactiveTabsOption(InactiveTabsPolicy.NEVER, R.string.delete_inactive_tabs_summary_never),
            InactiveTabsOption(InactiveTabsPolicy.AFTER_1_DAY, R.string.delete_inactive_tabs_summary_1_day),
            InactiveTabsOption(InactiveTabsPolicy.AFTER_1_WEEK, R.string.delete_inactive_tabs_summary_1_week),
            InactiveTabsOption(InactiveTabsPolicy.AFTER_1_MONTH, R.string.delete_inactive_tabs_summary_1_month)
        ).forEach { option ->
            val sel = selected == option.key
            SelectableCard(
                selected = sel, onClick = { selected = option.key },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = SettingsPickerOptionContentPadding, bottomSpacing = SettingsPickerOptionBottomSpacing
            ) {
                ClintRadioButton(selected = sel)
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                if (option.key == InactiveTabsPolicy.DEFAULT) DefaultChip(stringResource(R.string.default_label), colors.primary)
            }
        }
    }
}

@Composable
fun HomepageDesignDialog(
    current: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var selected by remember(current) { mutableStateOf(current) }

    ClintDialog(
        title = stringResource(R.string.choose_homepage_design),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onConfirm(selected) }) {
                    Text(stringResource(android.R.string.ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        data class DesignOption(val key: String, val titleRes: Int, val descRes: Int, val showDefault: Boolean)
        listOf(
            DesignOption(HOMEPAGE_DESIGN_PLAIN, R.string.homepage_design_plain, R.string.homepage_design_plain_desc, true),
            DesignOption(HOMEPAGE_DESIGN_GRADIENT, R.string.homepage_design_gradient, R.string.homepage_design_gradient_desc, false),
            DesignOption(HOMEPAGE_DESIGN_SOLID, R.string.homepage_design_solid, R.string.homepage_design_solid_desc, false),
            DesignOption(HOMEPAGE_DESIGN_CUSTOM_GRADIENT, R.string.homepage_design_custom_gradient, R.string.homepage_design_custom_gradient_desc, false),
            DesignOption(HOMEPAGE_DESIGN_IMAGE, R.string.homepage_design_image, R.string.homepage_design_image_desc, false)
        ).forEach { option ->
            val sel = selected == option.key
            SelectableCard(
                selected = sel, onClick = { selected = option.key },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = SettingsPickerOptionContentPadding, bottomSpacing = SettingsPickerOptionBottomSpacing
            ) {
                ClintRadioButton(selected = sel)
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                if (option.showDefault) DefaultChip(stringResource(R.string.default_label), colors.primary)
            }
        }
    }
}
