package com.jhaiian.clint.settings.browser
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.AutoDelete
import com.jhaiian.clint.tabs.InactiveTabsPolicy
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.ArrowDropDownCircle
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.VerticalAlignCenter
import androidx.compose.material.icons.filled.VisibilityOff

import com.jhaiian.clint.ui.ClintSwitch
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.settings.common.SettingsSectionLabel
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun BrowserSettingsScreen(
    state: BrowserSettingsUiState,
    onHomepageRowClicked: () -> Unit,
    onSuggestionsRowClicked: () -> Unit,
    onDeleteInactiveTabsConfirmed: (String) -> Unit,
    onSearchEngineConfirmed: (String) -> Unit,
    onSearchSuggestionsApiConfirmed: (String) -> Unit,
    onIncognitoSearchEngineConfirmed: (String) -> Unit,
    onIncognitoSearchSuggestionsApiConfirmed: (String) -> Unit,
    onJavascriptRowClicked: () -> Unit,
    onBackgroundPlaybackRowClicked: () -> Unit,
    onVideoPlayerRowClicked: () -> Unit,
    onAboutConfigRowClicked: () -> Unit,
    onShortcutManagerRowClicked: () -> Unit,
    onWebsiteBlockerRowClicked: () -> Unit,
    onIncognitoSearchHistoryRowClicked: () -> Unit,
    onRememberBrowsingModeRowClicked: () -> Unit,
    onProfilesRowClicked: () -> Unit,
    onExtensionsRowClicked: () -> Unit,
    onTranslationsRowClicked: () -> Unit,
    onFindInPageRowClicked: () -> Unit,
    onBookmarksRowClicked: () -> Unit
) {
    val colors = LocalClintColors.current

    SettingsScreenScaffold(
        overlay = {
            if (state.deleteInactiveTabsDialogOpen) {
                InactiveTabsDialog(
                    current = state.deleteInactiveTabs,
                    hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onConfirm = onDeleteInactiveTabsConfirmed,
                    onDismiss = { state.deleteInactiveTabsDialogOpen = false }
                )
            }
            if (state.searchEngineDialogOpen) {
                SearchEngineDialog(
                    current = state.searchEngine,
                    hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onConfirm = onSearchEngineConfirmed,
                    onDismiss = { state.searchEngineDialogOpen = false }
                )
            }
            if (state.searchSuggestionsApiDialogOpen) {
                SearchSuggestionsApiDialog(
                    current = state.searchSuggestionsApi,
                    hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onConfirm = onSearchSuggestionsApiConfirmed,
                    onDismiss = { state.searchSuggestionsApiDialogOpen = false }
                )
            }
            if (state.incognitoSearchEngineDialogOpen) {
                SearchEngineDialog(
                    current = state.incognitoSearchEngine,
                    hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onConfirm = onIncognitoSearchEngineConfirmed,
                    onDismiss = { state.incognitoSearchEngineDialogOpen = false },
                    titleRes = R.string.incognito_search_engine_title,
                    showSameAsNormal = true
                )
            }
            if (state.incognitoSearchSuggestionsApiDialogOpen) {
                SearchSuggestionsApiDialog(
                    current = state.incognitoSearchSuggestionsApi,
                    hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onConfirm = onIncognitoSearchSuggestionsApiConfirmed,
                    onDismiss = { state.incognitoSearchSuggestionsApiDialogOpen = false },
                    titleRes = R.string.incognito_search_suggestions_api_title,
                    showSameAsNormal = true
                )
            }
        }
    ) {
        SettingsSectionLabel(stringResource(R.string.pref_category_search))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Search,
                title = stringResource(R.string.search_engine),
                summary = engineSummaryText(state.searchEngine),
                colors = colors,
                onClick = { state.searchEngineDialogOpen = true }
            )
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ManageSearch,
                title = stringResource(R.string.search_suggestions_api),
                summary = if (state.searchSuggestionsApi == "off") stringResource(R.string.suggestions_api_off) else engineSummaryText(state.searchSuggestionsApi),
                colors = colors,
                onClick = { state.searchSuggestionsApiDialogOpen = true }
            )
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.ViewAgenda,
                title = stringResource(R.string.pref_category_suggestions),
                summary = stringResource(R.string.suggestions_settings_summary),
                colors = colors,
                onClick = onSuggestionsRowClicked
            )
        }

        SettingsSectionLabel(stringResource(R.string.pref_category_homepage))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Home,
                title = stringResource(R.string.homepage_title),
                summary = stringResource(R.string.homepage_settings_summary),
                colors = colors,
                onClick = onHomepageRowClicked
            )
        }

        SettingsSectionLabel(stringResource(R.string.pref_category_tabs))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.AutoDelete,
                title = stringResource(R.string.delete_inactive_tabs_title),
                summary = deleteInactiveTabsSummaryText(state.deleteInactiveTabs),
                colors = colors,
                onClick = { state.deleteInactiveTabsDialogOpen = true }
            )
        }

        SettingsSectionLabel(stringResource(R.string.pref_category_web_content))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.DesktopWindows,
                title = stringResource(R.string.javascript_enabled),
                summary = stringResource(R.string.javascript_enabled_summary),
                colors = colors,
                onClick = onJavascriptRowClicked,
                trailing = {
                    ClintSwitch(checked = state.javascriptEnabled)
                }
            )
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Headphones,
                title = stringResource(R.string.background_playback_title),
                summary = stringResource(R.string.background_playback_summary),
                colors = colors,
                onClick = onBackgroundPlaybackRowClicked,
                trailing = {
                    ClintSwitch(checked = state.backgroundPlayback)
                }
            )
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.SmartDisplay,
                title = stringResource(R.string.video_player_title),
                summary = stringResource(R.string.video_player_summary),
                colors = colors,
                onClick = onVideoPlayerRowClicked
            )
        }

        SettingsSectionLabel(stringResource(R.string.pref_category_tools))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Bookmark,
                title = stringResource(R.string.bookmarks_title),
                summary = stringResource(R.string.bookmarks_settings_summary),
                colors = colors,
                onClick = onBookmarksRowClicked
            )
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Translate,
                title = stringResource(R.string.translation_settings_title),
                summary = stringResource(R.string.translation_settings_summary),
                colors = colors,
                onClick = onTranslationsRowClicked
            )
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.FindInPage,
                title = stringResource(R.string.find_settings_title),
                summary = stringResource(R.string.find_settings_summary),
                colors = colors,
                onClick = onFindInPageRowClicked
            )
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Extension,
                title = stringResource(R.string.ext_title),
                summary = stringResource(R.string.ext_settings_summary),
                colors = colors,
                onClick = onExtensionsRowClicked
            )
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.AccountCircle,
                title = stringResource(R.string.profiles_title),
                summary = stringResource(R.string.profiles_settings_summary),
                colors = colors,
                onClick = onProfilesRowClicked
            )
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.AddToHomeScreen,
                title = stringResource(R.string.shortcut_manager_title),
                summary = stringResource(R.string.shortcut_manager_summary),
                colors = colors,
                onClick = onShortcutManagerRowClicked
            )
        }

        SettingsSectionLabel(stringResource(R.string.pref_category_protection))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Shield,
                title = stringResource(R.string.website_blocker_title),
                summary = stringResource(R.string.website_blocker_settings_summary),
                colors = colors,
                onClick = onWebsiteBlockerRowClicked
            )
        }

        SettingsSectionLabel(stringResource(R.string.pref_category_incognito))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Search,
                title = stringResource(R.string.incognito_search_engine_title),
                summary = if (state.incognitoSearchEngine == com.jhaiian.clint.browser.SameAsNormalMode) {
                    stringResource(R.string.search_same_as_normal_summary, engineSummaryText(state.searchEngine))
                } else {
                    engineSummaryText(state.incognitoSearchEngine)
                },
                colors = colors,
                onClick = { state.incognitoSearchEngineDialogOpen = true }
            )
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ManageSearch,
                title = stringResource(R.string.incognito_search_suggestions_api_title),
                summary = when (state.incognitoSearchSuggestionsApi) {
                    com.jhaiian.clint.browser.SameAsNormalMode -> stringResource(
                        R.string.search_same_as_normal_summary,
                        if (state.searchSuggestionsApi == "off") stringResource(R.string.suggestions_api_off) else engineSummaryText(state.searchSuggestionsApi)
                    )
                    "off" -> stringResource(R.string.suggestions_api_off)
                    else -> engineSummaryText(state.incognitoSearchSuggestionsApi)
                },
                colors = colors,
                onClick = { state.incognitoSearchSuggestionsApiDialogOpen = true }
            )
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.VisibilityOff,
                title = stringResource(R.string.incognito_search_history_title),
                summary = stringResource(R.string.incognito_search_history_summary),
                colors = colors,
                onClick = onIncognitoSearchHistoryRowClicked,
                trailing = {
                    ClintSwitch(checked = state.incognitoSearchHistory)
                }
            )
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Restore,
                title = stringResource(R.string.remember_browsing_mode_title),
                summary = stringResource(R.string.remember_browsing_mode_summary),
                colors = colors,
                onClick = onRememberBrowsingModeRowClicked,
                trailing = {
                    ClintSwitch(checked = state.rememberBrowsingMode)
                }
            )
        }

        SettingsSectionLabel(stringResource(R.string.settings_group_advanced))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Tune,
                title = stringResource(R.string.about_config_title),
                summary = stringResource(R.string.about_config_summary),
                colors = colors,
                onClick = onAboutConfigRowClicked,
                trailing = {
                    ClintSwitch(checked = state.aboutConfigEnabled)
                }
            )
        }
    }
}

@Composable
private fun engineSummaryText(engine: String): String {
    val context = androidx.compose.ui.platform.LocalContext.current
    return when (engine) {
        "brave" -> stringResource(R.string.engine_brave)
        "ecosia" -> stringResource(R.string.engine_ecosia)
        "google" -> stringResource(R.string.engine_google)
        else -> com.jhaiian.clint.browser.customSearchEngineFor(androidx.preference.PreferenceManager.getDefaultSharedPreferences(context), engine)?.name
            ?: stringResource(R.string.engine_duckduckgo)
    }
}

@Composable
private fun deleteInactiveTabsSummaryText(value: String): String = when (value) {
    InactiveTabsPolicy.AFTER_1_DAY -> stringResource(R.string.delete_inactive_tabs_summary_1_day)
    InactiveTabsPolicy.AFTER_1_WEEK -> stringResource(R.string.delete_inactive_tabs_summary_1_week)
    InactiveTabsPolicy.AFTER_1_MONTH -> stringResource(R.string.delete_inactive_tabs_summary_1_month)
    else -> stringResource(R.string.delete_inactive_tabs_summary_never)
}
