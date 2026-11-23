package com.jhaiian.clint.settings.browser

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.suggestions.PREF_SUGGESTIONS_BOOKMARK_LIMIT
import com.jhaiian.clint.browser.suggestions.PREF_SUGGESTIONS_HISTORY_LIMIT
import com.jhaiian.clint.browser.suggestions.PREF_SUGGESTIONS_ORDER
import com.jhaiian.clint.browser.suggestions.PREF_SUGGESTIONS_SHOW_BOOKMARKS
import com.jhaiian.clint.browser.suggestions.PREF_SUGGESTIONS_SHOW_ENGINES
import com.jhaiian.clint.browser.suggestions.PREF_SUGGESTIONS_SHOW_FAVORITES
import com.jhaiian.clint.browser.suggestions.PREF_SUGGESTIONS_SHOW_FILL
import com.jhaiian.clint.browser.suggestions.PREF_SUGGESTIONS_SHOW_HEADERS
import com.jhaiian.clint.browser.suggestions.PREF_SUGGESTIONS_SHOW_HISTORY
import com.jhaiian.clint.browser.suggestions.PREF_SUGGESTIONS_SHOW_PASTE
import com.jhaiian.clint.browser.suggestions.PREF_SUGGESTIONS_SHOW_TABS
import com.jhaiian.clint.browser.suggestions.PREF_SUGGESTIONS_STYLE
import com.jhaiian.clint.browser.suggestions.PREF_SUGGESTIONS_TEXT_SIZE
import com.jhaiian.clint.browser.suggestions.PREF_SUGGESTIONS_URL_MODE
import com.jhaiian.clint.browser.suggestions.SUGGESTIONS_BOOKMARK_LIMIT_DEFAULT
import com.jhaiian.clint.browser.suggestions.SUGGESTIONS_HISTORY_LIMIT_DEFAULT
import com.jhaiian.clint.browser.suggestions.SUGGESTIONS_LIMIT_OPTIONS
import com.jhaiian.clint.browser.suggestions.SUGGESTIONS_ORDER_BOOKMARKS
import com.jhaiian.clint.browser.suggestions.SUGGESTIONS_ORDER_HISTORY
import com.jhaiian.clint.browser.suggestions.SUGGESTIONS_ORDER_MIXED
import com.jhaiian.clint.browser.suggestions.SUGGESTIONS_ORDER_WEB
import com.jhaiian.clint.browser.suggestions.SUGGESTIONS_STYLE_CARDS
import com.jhaiian.clint.browser.suggestions.SUGGESTIONS_STYLE_COMPACT
import com.jhaiian.clint.browser.suggestions.SUGGESTIONS_TEXT_DEFAULT
import com.jhaiian.clint.browser.suggestions.SUGGESTIONS_TEXT_LARGE
import com.jhaiian.clint.browser.suggestions.SUGGESTIONS_TEXT_SMALL
import com.jhaiian.clint.browser.suggestions.SUGGESTIONS_URL_DOMAIN
import com.jhaiian.clint.browser.suggestions.SUGGESTIONS_URL_FULL
import com.jhaiian.clint.browser.suggestions.SUGGESTIONS_URL_OFF
import com.jhaiian.clint.browser.suggestions.readSuggestionUrlMode
import com.jhaiian.clint.settings.common.SettingsPickerOptionBottomSpacing
import com.jhaiian.clint.settings.common.SettingsPickerOptionContentPadding
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.settings.common.SettingsSectionLabel
import com.jhaiian.clint.setup.DefaultChip
import com.jhaiian.clint.setup.SelectableCard
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintRadioButton
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.theme.LocalClintColors

private class SuggestionChoice<T>(val value: T, val label: String, val description: String? = null)

@Composable
fun SuggestionSettingsSection(hideStatusBar: Boolean, hideSystemNavigation: Boolean) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val showHistory = rememberBooleanPref(prefs, PREF_SUGGESTIONS_SHOW_HISTORY, true)
    val showBookmarks = rememberBooleanPref(prefs, PREF_SUGGESTIONS_SHOW_BOOKMARKS, true)

    SettingsSectionLabel(stringResource(R.string.pref_category_suggestions))
    SettingsSection(colors.cardBackground) {
        SuggestionChoiceRow(
            icon = Icons.Filled.ViewAgenda,
            title = stringResource(R.string.suggestions_style_title),
            initial = prefs.getString(PREF_SUGGESTIONS_STYLE, SUGGESTIONS_STYLE_COMPACT) ?: SUGGESTIONS_STYLE_COMPACT,
            defaultValue = SUGGESTIONS_STYLE_COMPACT,
            choices = listOf(
                SuggestionChoice(SUGGESTIONS_STYLE_COMPACT, stringResource(R.string.suggestions_style_compact), stringResource(R.string.suggestions_style_compact_desc)),
                SuggestionChoice(SUGGESTIONS_STYLE_CARDS, stringResource(R.string.suggestions_style_cards), stringResource(R.string.suggestions_style_cards_desc))
            ),
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            onSave = { prefs.edit().putString(PREF_SUGGESTIONS_STYLE, it).apply() }
        )
        SuggestionChoiceRow(
            icon = Icons.AutoMirrored.Filled.Sort,
            title = stringResource(R.string.suggestions_order_title),
            initial = prefs.getString(PREF_SUGGESTIONS_ORDER, SUGGESTIONS_ORDER_BOOKMARKS) ?: SUGGESTIONS_ORDER_BOOKMARKS,
            defaultValue = SUGGESTIONS_ORDER_BOOKMARKS,
            choices = listOf(
                SuggestionChoice(SUGGESTIONS_ORDER_BOOKMARKS, stringResource(R.string.suggestions_order_bookmarks)),
                SuggestionChoice(SUGGESTIONS_ORDER_HISTORY, stringResource(R.string.suggestions_order_history)),
                SuggestionChoice(SUGGESTIONS_ORDER_WEB, stringResource(R.string.suggestions_order_web)),
                SuggestionChoice(SUGGESTIONS_ORDER_MIXED, stringResource(R.string.suggestions_order_mixed), stringResource(R.string.suggestions_order_mixed_desc))
            ),
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            onSave = { prefs.edit().putString(PREF_SUGGESTIONS_ORDER, it).apply() }
        )
        SuggestionChoiceRow(
            icon = Icons.Filled.FormatSize,
            title = stringResource(R.string.suggestions_text_size_title),
            initial = prefs.getString(PREF_SUGGESTIONS_TEXT_SIZE, SUGGESTIONS_TEXT_DEFAULT) ?: SUGGESTIONS_TEXT_DEFAULT,
            defaultValue = SUGGESTIONS_TEXT_DEFAULT,
            choices = listOf(
                SuggestionChoice(SUGGESTIONS_TEXT_SMALL, stringResource(R.string.suggestions_text_size_small)),
                SuggestionChoice(SUGGESTIONS_TEXT_DEFAULT, stringResource(R.string.suggestions_text_size_default)),
                SuggestionChoice(SUGGESTIONS_TEXT_LARGE, stringResource(R.string.suggestions_text_size_large))
            ),
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            onSave = { prefs.edit().putString(PREF_SUGGESTIONS_TEXT_SIZE, it).apply() }
        )
        SuggestionChoiceRow(
            icon = Icons.Filled.Link,
            title = stringResource(R.string.suggestions_url_mode_title),
            initial = readSuggestionUrlMode(prefs),
            defaultValue = SUGGESTIONS_URL_DOMAIN,
            choices = listOf(
                SuggestionChoice(SUGGESTIONS_URL_DOMAIN, stringResource(R.string.suggestions_url_domain), stringResource(R.string.suggestions_url_domain_desc)),
                SuggestionChoice(SUGGESTIONS_URL_FULL, stringResource(R.string.suggestions_url_full), stringResource(R.string.suggestions_url_full_desc)),
                SuggestionChoice(SUGGESTIONS_URL_OFF, stringResource(R.string.suggestions_url_off))
            ),
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            onSave = { prefs.edit().putString(PREF_SUGGESTIONS_URL_MODE, it).apply() }
        )
        SuggestionSwitchRow(Icons.Filled.Title, R.string.suggestions_show_headers_title, R.string.suggestions_show_headers_summary, rememberBooleanPref(prefs, PREF_SUGGESTIONS_SHOW_HEADERS, true), prefs, PREF_SUGGESTIONS_SHOW_HEADERS)
        SuggestionSwitchRow(Icons.AutoMirrored.Filled.CallMade, R.string.suggestions_show_fill_title, R.string.suggestions_show_fill_summary, rememberBooleanPref(prefs, PREF_SUGGESTIONS_SHOW_FILL, true), prefs, PREF_SUGGESTIONS_SHOW_FILL)
    }

    SettingsSection(colors.cardBackground) {
        SuggestionSwitchRow(Icons.Filled.Bookmark, R.string.suggestions_show_bookmarks_title, R.string.suggestions_show_bookmarks_summary, showBookmarks, prefs, PREF_SUGGESTIONS_SHOW_BOOKMARKS)
        SuggestionLimitRow(
            icon = Icons.Filled.FormatListNumbered,
            title = stringResource(R.string.suggestions_bookmark_limit_title),
            prefs = prefs, key = PREF_SUGGESTIONS_BOOKMARK_LIMIT, defaultValue = SUGGESTIONS_BOOKMARK_LIMIT_DEFAULT,
            enabled = showBookmarks.value,
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation
        )
        SuggestionSwitchRow(Icons.Filled.History, R.string.suggestions_show_history_title, R.string.suggestions_show_history_summary, showHistory, prefs, PREF_SUGGESTIONS_SHOW_HISTORY)
        SuggestionLimitRow(
            icon = Icons.Filled.FormatListNumbered,
            title = stringResource(R.string.suggestions_history_limit_title),
            prefs = prefs, key = PREF_SUGGESTIONS_HISTORY_LIMIT, defaultValue = SUGGESTIONS_HISTORY_LIMIT_DEFAULT,
            enabled = showHistory.value,
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation
        )
        SuggestionSwitchRow(Icons.Filled.Tab, R.string.suggestions_show_tabs_title, R.string.suggestions_show_tabs_summary, rememberBooleanPref(prefs, PREF_SUGGESTIONS_SHOW_TABS, true), prefs, PREF_SUGGESTIONS_SHOW_TABS)
        SuggestionSwitchRow(Icons.Filled.Star, R.string.suggestions_show_favorites_title, R.string.suggestions_show_favorites_summary, rememberBooleanPref(prefs, PREF_SUGGESTIONS_SHOW_FAVORITES, true), prefs, PREF_SUGGESTIONS_SHOW_FAVORITES)
        SuggestionSwitchRow(Icons.Filled.ContentPaste, R.string.suggestions_show_paste_title, R.string.suggestions_show_paste_summary, rememberBooleanPref(prefs, PREF_SUGGESTIONS_SHOW_PASTE, true), prefs, PREF_SUGGESTIONS_SHOW_PASTE)
        SuggestionSwitchRow(Icons.Filled.TravelExplore, R.string.suggestions_show_engines_title, R.string.suggestions_show_engines_summary, rememberBooleanPref(prefs, PREF_SUGGESTIONS_SHOW_ENGINES, true), prefs, PREF_SUGGESTIONS_SHOW_ENGINES)
    }
}

@Composable
private fun rememberBooleanPref(prefs: SharedPreferences, key: String, defaultValue: Boolean): MutableState<Boolean> =
    remember(key) { mutableStateOf(prefs.getBoolean(key, defaultValue)) }

@Composable
private fun SuggestionSwitchRow(
    icon: ImageVector,
    titleRes: Int,
    summaryRes: Int,
    state: MutableState<Boolean>,
    prefs: SharedPreferences,
    key: String
) {
    SettingsRow(
        icon = icon,
        title = stringResource(titleRes),
        summary = stringResource(summaryRes),
        colors = LocalClintColors.current,
        onClick = {
            state.value = !state.value
            prefs.edit().putBoolean(key, state.value).apply()
        },
        trailing = { ClintSwitch(checked = state.value) }
    )
}

@Composable
private fun SuggestionLimitRow(
    icon: ImageVector,
    title: String,
    prefs: SharedPreferences,
    key: String,
    defaultValue: Int,
    enabled: Boolean,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean
) {
    val choices = SUGGESTIONS_LIMIT_OPTIONS.map { SuggestionChoice(it, stringResource(R.string.suggestions_limit_value, it)) }
    SuggestionChoiceRow(
        icon = icon,
        title = title,
        initial = prefs.getInt(key, defaultValue),
        defaultValue = defaultValue,
        choices = choices,
        enabled = enabled,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onSave = { prefs.edit().putInt(key, it).apply() }
    )
}

@Composable
private fun <T> SuggestionChoiceRow(
    icon: ImageVector,
    title: String,
    initial: T,
    defaultValue: T,
    choices: List<SuggestionChoice<T>>,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onSave: (T) -> Unit,
    enabled: Boolean = true
) {
    val colors = LocalClintColors.current
    var current by remember { mutableStateOf(initial) }
    var dialogOpen by remember { mutableStateOf(false) }

    SettingsRow(
        icon = icon,
        title = title,
        summary = choices.firstOrNull { it.value == current }?.label ?: "",
        colors = colors,
        enabled = enabled,
        onClick = { if (enabled) dialogOpen = true }
    )

    if (dialogOpen) {
        var selected by remember { mutableStateOf(current) }
        ClintDialog(
            title = title,
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            onDismiss = { dialogOpen = false },
            footer = {
                Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { dialogOpen = false }) {
                        Text(stringResource(android.R.string.cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                    TextButton(onClick = {
                        current = selected
                        onSave(selected)
                        dialogOpen = false
                    }) {
                        Text(stringResource(android.R.string.ok), color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        ) {
            choices.forEach { choice ->
                val sel = selected == choice.value
                SelectableCard(
                    selected = sel, onClick = { selected = choice.value },
                    cardBackground = colors.surfaceVariant, primary = colors.primary,
                    contentPadding = SettingsPickerOptionContentPadding, bottomSpacing = SettingsPickerOptionBottomSpacing
                ) {
                    ClintRadioButton(selected = sel)
                    Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                        Text(choice.label, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        if (choice.description != null) {
                            Text(choice.description, color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    if (choice.value == defaultValue) DefaultChip(stringResource(R.string.default_label), colors.primary)
                }
            }
        }
    }
}
