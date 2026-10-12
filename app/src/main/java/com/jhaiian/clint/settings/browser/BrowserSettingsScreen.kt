package com.jhaiian.clint.settings.browser
import com.jhaiian.clint.browser.home.HOMEPAGE_SEARCH_ENGINE
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.AutoDelete
import com.jhaiian.clint.tabs.InactiveTabsPolicy
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import com.jhaiian.clint.browser.home.HOMEPAGE_DESIGN_GRADIENT
import com.jhaiian.clint.browser.home.HOMEPAGE_DESIGN_IMAGE
import com.jhaiian.clint.browser.home.HOMEPAGE_DESIGN_PLAIN
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
import androidx.compose.material.icons.filled.VerticalAlignCenter
import androidx.compose.material.icons.filled.VisibilityOff

import androidx.compose.foundation.layout.padding
import com.jhaiian.clint.ui.ClintSwitch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.setup.SectionLabel
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun BrowserSettingsScreen(
    state: BrowserSettingsUiState,
    onHomepageConfirmed: (String) -> Unit,
    onHomepageDesignConfirmed: (String) -> Unit,
    onHomepageImageRowClicked: () -> Unit,
    onHomepageShowFavoritesClicked: () -> Unit,
    onHomepageShowRecentClicked: () -> Unit,
    onHomepageCenterContentClicked: () -> Unit,
    onDeleteInactiveTabsConfirmed: (String) -> Unit,
    onSearchEngineConfirmed: (String) -> Unit,
    onCustomSearchEngineSaved: (name: String, url: String) -> Unit,
    onSearchSuggestionsApiConfirmed: (String) -> Unit,
    onCustomSearchSuggestionsApiSaved: (name: String, url: String) -> Unit,
    onJavascriptRowClicked: () -> Unit,
    onBackgroundPlaybackRowClicked: () -> Unit,
    onShortcutManagerRowClicked: () -> Unit,
    onWebsiteBlockerRowClicked: () -> Unit,
    onIncognitoSearchHistoryRowClicked: () -> Unit,
    onProfilesRowClicked: () -> Unit,
    onExtensionsRowClicked: () -> Unit
) {
    val colors = LocalClintColors.current
    val homepageOptionsEnabled = state.homepage != HOMEPAGE_SEARCH_ENGINE

    SettingsScreenScaffold(
        overlay = {
            if (state.homepageDialogOpen) {
                HomepageDialog(
                    current = state.homepage,
                    hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onConfirm = onHomepageConfirmed,
                    onDismiss = { state.homepageDialogOpen = false }
                )
            }
            if (state.homepageDesignDialogOpen) {
                HomepageDesignDialog(
                    current = state.homepageDesign,
                    hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onConfirm = onHomepageDesignConfirmed,
                    onDismiss = { state.homepageDesignDialogOpen = false }
                )
            }
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
                    customName = state.customSearchEngineName,
                    customUrl = state.customSearchEngineUrl,
                    hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onConfirm = onSearchEngineConfirmed,
                    onCustomSearchEngineSaved = onCustomSearchEngineSaved,
                    onDismiss = { state.searchEngineDialogOpen = false }
                )
            }
            if (state.searchSuggestionsApiDialogOpen) {
                SearchSuggestionsApiDialog(
                    current = state.searchSuggestionsApi,
                    customName = state.customSearchSuggestionsApiName,
                    customUrl = state.customSearchSuggestionsApiUrl,
                    hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onConfirm = onSearchSuggestionsApiConfirmed,
                    onCustomSearchSuggestionsApiSaved = onCustomSearchSuggestionsApiSaved,
                    onDismiss = { state.searchSuggestionsApiDialogOpen = false }
                )
            }
        }
    ) {
        SectionLabel(stringResource(R.string.pref_category_homepage).uppercase(), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Home,
                title = stringResource(R.string.homepage_title),
                summary = homepageSummaryText(state.homepage),
                colors = colors,
                onClick = { state.homepageDialogOpen = true }
            )
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Gradient,
                title = stringResource(R.string.homepage_design_title),
                summary = homepageDesignSummaryText(state.homepageDesign),
                colors = colors,
                enabled = homepageOptionsEnabled,
                onClick = { if (homepageOptionsEnabled) state.homepageDesignDialogOpen = true }
            )
            if (state.homepageDesign == HOMEPAGE_DESIGN_IMAGE) {
                SettingsRow(
                    icon = androidx.compose.material.icons.Icons.Filled.Image,
                    title = stringResource(R.string.homepage_image_title),
                    summary = stringResource(R.string.homepage_image_summary),
                    colors = colors,
                    enabled = homepageOptionsEnabled,
                    onClick = { if (homepageOptionsEnabled) onHomepageImageRowClicked() }
                )
            }
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Bookmark,
                title = stringResource(R.string.homepage_show_favorites_title),
                summary = stringResource(R.string.homepage_show_favorites_summary),
                colors = colors,
                enabled = homepageOptionsEnabled,
                onClick = { if (homepageOptionsEnabled) onHomepageShowFavoritesClicked() },
                trailing = {
                    ClintSwitch(checked = state.homepageShowFavorites && homepageOptionsEnabled)
                }
            )
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.History,
                title = stringResource(R.string.homepage_show_recent_title),
                summary = stringResource(R.string.homepage_show_recent_summary),
                colors = colors,
                enabled = homepageOptionsEnabled,
                onClick = { if (homepageOptionsEnabled) onHomepageShowRecentClicked() },
                trailing = {
                    ClintSwitch(checked = state.homepageShowRecent && homepageOptionsEnabled)
                }
            )
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.VerticalAlignCenter,
                title = stringResource(R.string.homepage_center_content_title),
                summary = stringResource(R.string.homepage_center_content_summary),
                colors = colors,
                enabled = homepageOptionsEnabled,
                onClick = { if (homepageOptionsEnabled) onHomepageCenterContentClicked() },
                trailing = {
                    ClintSwitch(checked = state.homepageCenterContent && homepageOptionsEnabled)
                }
            )
        }

        SectionLabel(stringResource(R.string.pref_category_search).uppercase(), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Search,
                title = stringResource(R.string.search_engine),
                summary = engineSummaryText(state.searchEngine, state.customSearchEngineName),
                colors = colors,
                onClick = { state.searchEngineDialogOpen = true }
            )
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ManageSearch,
                title = stringResource(R.string.search_suggestions_api),
                summary = if (state.searchSuggestionsApi == "off") stringResource(R.string.suggestions_api_off) else engineSummaryText(state.searchSuggestionsApi, state.customSearchSuggestionsApiName),
                colors = colors,
                onClick = { state.searchSuggestionsApiDialogOpen = true }
            )
        }

        SectionLabel(stringResource(R.string.pref_category_tabs).uppercase(), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.AutoDelete,
                title = stringResource(R.string.delete_inactive_tabs_title),
                summary = deleteInactiveTabsSummaryText(state.deleteInactiveTabs),
                colors = colors,
                onClick = { state.deleteInactiveTabsDialogOpen = true }
            )
        }

        SectionLabel(stringResource(R.string.pref_category_protection).uppercase(), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Shield,
                title = stringResource(R.string.website_blocker_title),
                summary = stringResource(R.string.website_blocker_settings_summary),
                colors = colors,
                onClick = onWebsiteBlockerRowClicked
            )
        }

        SectionLabel(stringResource(R.string.pref_category_incognito).uppercase(), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
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
        }

        SectionLabel(stringResource(R.string.browser_settings).uppercase(), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
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
                icon = androidx.compose.material.icons.Icons.Filled.AccountCircle,
                title = stringResource(R.string.profiles_title),
                summary = stringResource(R.string.profiles_settings_summary),
                colors = colors,
                onClick = onProfilesRowClicked
            )
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Extension,
                title = stringResource(R.string.ext_title),
                summary = stringResource(R.string.ext_settings_summary),
                colors = colors,
                onClick = onExtensionsRowClicked
            )
        }

        SectionLabel(stringResource(R.string.pref_category_shortcuts).uppercase(), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.AddToHomeScreen,
                title = stringResource(R.string.shortcut_manager_title),
                summary = stringResource(R.string.shortcut_manager_summary),
                colors = colors,
                onClick = onShortcutManagerRowClicked
            )
        }
    }
}

@Composable
private fun engineSummaryText(engine: String, customName: String): String = when (engine) {
    "brave" -> stringResource(R.string.engine_brave)
    "ecosia" -> stringResource(R.string.engine_ecosia)
    "google" -> stringResource(R.string.engine_google)
    "custom" -> customName.ifBlank { stringResource(R.string.engine_custom) }
    else -> stringResource(R.string.engine_duckduckgo)
}

@Composable
private fun deleteInactiveTabsSummaryText(value: String): String = when (value) {
    InactiveTabsPolicy.AFTER_1_DAY -> stringResource(R.string.delete_inactive_tabs_summary_1_day)
    InactiveTabsPolicy.AFTER_1_WEEK -> stringResource(R.string.delete_inactive_tabs_summary_1_week)
    InactiveTabsPolicy.AFTER_1_MONTH -> stringResource(R.string.delete_inactive_tabs_summary_1_month)
    else -> stringResource(R.string.delete_inactive_tabs_summary_never)
}

@Composable
private fun homepageSummaryText(homepage: String): String = when (homepage) {
    HOMEPAGE_SEARCH_ENGINE -> stringResource(R.string.homepage_search_engine)
    else -> stringResource(R.string.homepage_clint)
}

@Composable
private fun homepageDesignSummaryText(design: String): String = when (design) {
    HOMEPAGE_DESIGN_GRADIENT -> stringResource(R.string.homepage_design_gradient)
    HOMEPAGE_DESIGN_IMAGE -> stringResource(R.string.homepage_design_image)
    else -> stringResource(R.string.homepage_design_plain)
}
