package com.jhaiian.clint.settings.find

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.preference.PreferenceManager
import androidx.compose.ui.res.stringResource
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.find.PREF_FIND_ADDRESS_BAR_SUGGESTION
import com.jhaiian.clint.browser.find.PREF_FIND_BAR_TOP
import com.jhaiian.clint.browser.find.PREF_FIND_DIM_PAGE
import com.jhaiian.clint.browser.find.PREF_FIND_RECENT
import com.jhaiian.clint.browser.find.PREF_FIND_REMEMBER_OPTIONS
import com.jhaiian.clint.browser.find.PREF_FIND_SAVE_RECENT
import com.jhaiian.clint.browser.find.PREF_FIND_SCROLL_MARKERS
import com.jhaiian.clint.settings.SettingsActivity
import com.jhaiian.clint.settings.common.RowDivider
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.settings.common.SettingsSectionLabel
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun FindSettingsPane(activity: SettingsActivity) {
    val colors = LocalClintColors.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(activity) }
    var barTop by remember { mutableStateOf(prefs.getBoolean(PREF_FIND_BAR_TOP, false)) }
    var dimPage by remember { mutableStateOf(prefs.getBoolean(PREF_FIND_DIM_PAGE, false)) }
    var scrollMarkers by remember { mutableStateOf(prefs.getBoolean(PREF_FIND_SCROLL_MARKERS, true)) }
    var rememberOptions by remember { mutableStateOf(prefs.getBoolean(PREF_FIND_REMEMBER_OPTIONS, true)) }
    var addressBarSuggestion by remember { mutableStateOf(prefs.getBoolean(PREF_FIND_ADDRESS_BAR_SUGGESTION, true)) }
    var saveRecent by remember { mutableStateOf(prefs.getBoolean(PREF_FIND_SAVE_RECENT, true)) }
    val clearedMessage = stringResource(R.string.find_recent_cleared)

    SettingsScreenScaffold {
        SettingsSectionLabel(stringResource(R.string.find_section_bar))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = Icons.Filled.VerticalAlignTop,
                title = stringResource(R.string.find_bar_top_title),
                summary = stringResource(R.string.find_bar_top_summary),
                colors = colors,
                onClick = {
                    barTop = !barTop
                    prefs.edit().putBoolean(PREF_FIND_BAR_TOP, barTop).apply()
                },
                trailing = { ClintSwitch(checked = barTop) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.Contrast,
                title = stringResource(R.string.find_dim_page_title),
                summary = stringResource(R.string.find_dim_page_summary),
                colors = colors,
                onClick = {
                    dimPage = !dimPage
                    prefs.edit().putBoolean(PREF_FIND_DIM_PAGE, dimPage).apply()
                },
                trailing = { ClintSwitch(checked = dimPage) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.LinearScale,
                title = stringResource(R.string.find_scroll_markers_title),
                summary = stringResource(R.string.find_scroll_markers_summary),
                colors = colors,
                onClick = {
                    scrollMarkers = !scrollMarkers
                    prefs.edit().putBoolean(PREF_FIND_SCROLL_MARKERS, scrollMarkers).apply()
                },
                trailing = { ClintSwitch(checked = scrollMarkers) }
            )
        }

        SettingsSectionLabel(stringResource(R.string.find_section_search))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = Icons.Filled.Restore,
                title = stringResource(R.string.find_remember_options_title),
                summary = stringResource(R.string.find_remember_options_summary),
                colors = colors,
                onClick = {
                    rememberOptions = !rememberOptions
                    prefs.edit().putBoolean(PREF_FIND_REMEMBER_OPTIONS, rememberOptions).apply()
                },
                trailing = { ClintSwitch(checked = rememberOptions) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.Search,
                title = stringResource(R.string.find_address_bar_title),
                summary = stringResource(R.string.find_address_bar_summary),
                colors = colors,
                onClick = {
                    addressBarSuggestion = !addressBarSuggestion
                    prefs.edit().putBoolean(PREF_FIND_ADDRESS_BAR_SUGGESTION, addressBarSuggestion).apply()
                },
                trailing = { ClintSwitch(checked = addressBarSuggestion) }
            )
        }

        SettingsSectionLabel(stringResource(R.string.find_section_history))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = Icons.Filled.History,
                title = stringResource(R.string.find_save_recent_title),
                summary = stringResource(R.string.find_save_recent_summary),
                colors = colors,
                onClick = {
                    saveRecent = !saveRecent
                    prefs.edit().putBoolean(PREF_FIND_SAVE_RECENT, saveRecent).apply()
                },
                trailing = { ClintSwitch(checked = saveRecent) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.DeleteSweep,
                title = stringResource(R.string.find_clear_recent_title),
                summary = stringResource(R.string.find_clear_recent_summary),
                colors = colors,
                onClick = {
                    prefs.edit().remove(PREF_FIND_RECENT).apply()
                    Toast.makeText(activity, clearedMessage, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
