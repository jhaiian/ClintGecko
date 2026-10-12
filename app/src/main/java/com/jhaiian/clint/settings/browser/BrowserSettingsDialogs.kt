package com.jhaiian.clint.settings.browser
import com.jhaiian.clint.browser.home.HOMEPAGE_SEARCH_ENGINE
import com.jhaiian.clint.tabs.InactiveTabsPolicy
import com.jhaiian.clint.browser.home.HOMEPAGE_CLINT
import com.jhaiian.clint.browser.home.HOMEPAGE_DESIGN_GRADIENT
import com.jhaiian.clint.browser.home.HOMEPAGE_DESIGN_IMAGE
import com.jhaiian.clint.browser.home.HOMEPAGE_DESIGN_PLAIN

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Edit
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
fun SearchEngineDialog(
    current: String,
    customName: String,
    customUrl: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: (String) -> Unit,
    onCustomSearchEngineSaved: (name: String, url: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var selected by remember(current) { mutableStateOf(current) }
    var customEditorOpen by remember { mutableStateOf(false) }
    val hasCustomEngine = customName.isNotBlank() && customUrl.isNotBlank()

    if (customEditorOpen) {
        com.jhaiian.clint.ui.CustomSearchEngineDialog(
            initialName = customName,
            initialUrl = customUrl,
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            onConfirm = { name, url ->
                onCustomSearchEngineSaved(name, url)
                selected = "custom"
                customEditorOpen = false
            },
            onDismiss = { customEditorOpen = false }
        )
    }

    ClintDialog(
        title = stringResource(R.string.choose_search_engine),
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
                if (option.showDefault) DefaultChip(stringResource(R.string.default_label), colors.primary)
            }
        }
        val customSel = selected == "custom"
        SelectableCard(
            selected = customSel,
            onClick = { if (hasCustomEngine) selected = "custom" else customEditorOpen = true },
            cardBackground = colors.surfaceVariant, primary = colors.primary,
            contentPadding = SettingsPickerOptionContentPadding, bottomSpacing = SettingsPickerOptionBottomSpacing
        ) {
            ClintRadioButton(selected = customSel)
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                Text(
                    if (hasCustomEngine) customName else stringResource(R.string.engine_custom),
                    color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium
                )
                Text(
                    if (hasCustomEngine) customUrl else stringResource(R.string.engine_custom_desc),
                    color = colors.secondaryText, fontSize = 13.sp, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            androidx.compose.material3.IconButton(onClick = { customEditorOpen = true }) {
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.action_edit),
                    tint = colors.iconTint
                )
            }
        }
    }
}

@Composable
fun SearchSuggestionsApiDialog(
    current: String,
    customName: String,
    customUrl: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: (String) -> Unit,
    onCustomSearchSuggestionsApiSaved: (name: String, url: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var selected by remember(current) { mutableStateOf(current) }
    var customEditorOpen by remember { mutableStateOf(false) }
    val hasCustomApi = customName.isNotBlank() && customUrl.isNotBlank()

    if (customEditorOpen) {
        com.jhaiian.clint.ui.CustomSearchSuggestionsApiDialog(
            initialName = customName,
            initialUrl = customUrl,
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            onConfirm = { name, url ->
                onCustomSearchSuggestionsApiSaved(name, url)
                selected = "custom"
                customEditorOpen = false
            },
            onDismiss = { customEditorOpen = false }
        )
    }

    ClintDialog(
        title = stringResource(R.string.choose_search_suggestions_api),
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
                if (option.showDefault) DefaultChip(stringResource(R.string.default_label), colors.primary)
            }
        }
        val customSel = selected == "custom"
        SelectableCard(
            selected = customSel,
            onClick = { if (hasCustomApi) selected = "custom" else customEditorOpen = true },
            cardBackground = colors.surfaceVariant, primary = colors.primary,
            contentPadding = SettingsPickerOptionContentPadding, bottomSpacing = SettingsPickerOptionBottomSpacing
        ) {
            ClintRadioButton(selected = customSel)
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                Text(
                    if (hasCustomApi) customName else stringResource(R.string.engine_custom),
                    color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium
                )
                Text(
                    if (hasCustomApi) customUrl else stringResource(R.string.suggestions_api_custom_desc),
                    color = colors.secondaryText, fontSize = 13.sp, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            androidx.compose.material3.IconButton(onClick = { customEditorOpen = true }) {
                androidx.compose.material3.Icon(
                    androidx.compose.material.icons.Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.action_edit),
                    tint = colors.iconTint
                )
            }
        }
    }
}

@Composable
fun HomepageDialog(
    current: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var selected by remember(current) { mutableStateOf(current) }

    ClintDialog(
        title = stringResource(R.string.choose_homepage),
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
        data class HomepageOption(val key: String, val titleRes: Int, val descRes: Int, val showDefault: Boolean)
        listOf(
            HomepageOption(HOMEPAGE_CLINT, R.string.homepage_clint, R.string.homepage_clint_desc, true),
            HomepageOption(HOMEPAGE_SEARCH_ENGINE, R.string.homepage_search_engine, R.string.homepage_search_engine_desc, false)
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
