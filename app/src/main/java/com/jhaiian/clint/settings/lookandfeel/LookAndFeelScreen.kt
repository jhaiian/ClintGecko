package com.jhaiian.clint.settings.lookandfeel
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WebAsset

import androidx.compose.foundation.layout.padding
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.jhaiian.clint.ui.theme.ClintColors
import com.jhaiian.clint.ui.ClintSwitch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.RowDivider
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.setup.SectionLabel
import com.jhaiian.clint.ui.ThemeSwatchUtils
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun LookAndFeelScreen(
    state: LookAndFeelUiState,
    onThemeSelected: (String) -> Unit,
    onAccentSelected: (String) -> Unit,
    onIntensitySelected: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onAddressBarPositionSelected: (String) -> Unit,
    onMenuStyleSelected: (String) -> Unit,
    onScrollHideModeSelected: (String) -> Unit,
    onHideStatusBarRowClicked: () -> Unit,
    onHideSystemNavigationRowClicked: () -> Unit,
    onShowHomeButtonRowClicked: () -> Unit,
    onCustomizeMenuRowClicked: () -> Unit,
    onExitConfirmationConfirmed: (String) -> Unit,
    onPopupAlertStyleSelected: (String) -> Unit
) {
    val colors = LocalClintColors.current
    val intensityEnabled = ThemeSwatchUtils.isSurfaceIntensityEnabled(state.theme, state.accent)

    SettingsScreenScaffold(
        overlay = {
            when (state.openDialog) {
                LookAndFeelDialog.THEME -> ThemeSelectorDialog(
                    current = state.theme, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onSelect = onThemeSelected, onDismiss = { state.openDialog = null }
                )
                LookAndFeelDialog.ACCENT -> AccentColorDialog(
                    current = state.accent, theme = state.theme, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onSelect = onAccentSelected, onDismiss = { state.openDialog = null }
                )
                LookAndFeelDialog.SURFACE_INTENSITY -> SurfaceIntensityDialog(
                    current = state.intensity, theme = state.theme, accent = state.accent, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onSelect = onIntensitySelected, onDismiss = { state.openDialog = null }
                )
                LookAndFeelDialog.ADDRESS_BAR_POSITION -> AddressBarPositionDialog(
                    current = state.addressBarPosition, theme = state.theme, accent = state.accent, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onSelect = onAddressBarPositionSelected, onDismiss = { state.openDialog = null }
                )
                LookAndFeelDialog.MENU_STYLE -> MenuStyleDialog(
                    current = state.menuStyle, addressBarPosition = state.addressBarPosition, theme = state.theme, accent = state.accent,
                    hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation, onSelect = onMenuStyleSelected, onDismiss = { state.openDialog = null }
                )
                LookAndFeelDialog.SCROLL_HIDE_MODE -> ScrollHideModeDialog(
                    current = state.scrollHideMode, addressBarPosition = state.addressBarPosition, theme = state.theme, accent = state.accent,
                    hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation, onSelect = onScrollHideModeSelected, onDismiss = { state.openDialog = null }
                )
                LookAndFeelDialog.EXIT_CONFIRMATION -> ExitConfirmationDialog(
                    current = state.exitConfirmation, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onConfirm = onExitConfirmationConfirmed, onDismiss = { state.openDialog = null }
                )
                LookAndFeelDialog.POPUP_ALERT_STYLE -> PopupAlertStyleDialog(
                    current = state.popupAlertStyle, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onSelect = onPopupAlertStyleSelected, onDismiss = { state.openDialog = null }
                )
                LookAndFeelDialog.LANGUAGE -> LanguageSelectorDialog(
                    current = state.language, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onSelect = onLanguageSelected, onDismiss = { state.openDialog = null }
                )
                null -> {}
            }
        }
    ) {
        SectionLabel(stringResource(R.string.pref_category_language).uppercase(), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Translate,
                title = stringResource(R.string.pref_language_title),
                summary = languageSummaryText(state.language),
                colors = colors,
                onClick = { state.openDialog = LookAndFeelDialog.LANGUAGE }
            )
        }

        SectionLabel(stringResource(R.string.pref_category_appearance).uppercase(), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Palette,
                title = stringResource(R.string.pref_app_theme_title),
                summary = stringResource(R.string.pref_app_theme_summary),
                colors = colors,
                onClick = { state.openDialog = LookAndFeelDialog.THEME }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Brush,
                title = stringResource(R.string.pref_accent_color_title),
                summary = stringResource(accentSummaryRes(state.accent)),
                colors = colors,
                onClick = { state.openDialog = LookAndFeelDialog.ACCENT }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Contrast,
                title = stringResource(R.string.pref_surface_intensity_title),
                summary = if (intensityEnabled) stringResource(intensitySummaryRes(state.intensity)) else stringResource(R.string.pref_surface_intensity_disabled_summary),
                colors = colors,
                enabled = intensityEnabled,
                onClick = { if (intensityEnabled) state.openDialog = LookAndFeelDialog.SURFACE_INTENSITY }
            )
        }
        DarkReaderColorHint(colors)

        SectionLabel(stringResource(R.string.pref_category_layout).uppercase(), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.SwapVert,
                title = stringResource(R.string.pref_nested_scroll_title),
                summary = stringResource(nestedScrollSummaryRes(state.scrollHideMode)),
                colors = colors,
                onClick = { state.openDialog = LookAndFeelDialog.SCROLL_HIDE_MODE }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.WebAsset,
                title = stringResource(R.string.pref_address_bar_position_title),
                summary = stringResource(addressBarPositionSummaryRes(state.addressBarPosition)),
                colors = colors,
                onClick = { state.openDialog = LookAndFeelDialog.ADDRESS_BAR_POSITION }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Home,
                title = stringResource(R.string.pref_show_home_button_title),
                summary = stringResource(R.string.pref_show_home_button_summary),
                colors = colors,
                onClick = onShowHomeButtonRowClicked,
                trailing = {
                    ClintSwitch(checked = state.showHomeButton)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.MoreVert,
                title = stringResource(R.string.pref_menu_style_title),
                summary = stringResource(menuStyleSummaryRes(state.menuStyle)),
                colors = colors,
                onClick = { state.openDialog = LookAndFeelDialog.MENU_STYLE }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Reorder,
                title = stringResource(R.string.menu_customization_title),
                summary = stringResource(R.string.menu_customization_settings_summary),
                colors = colors,
                onClick = onCustomizeMenuRowClicked
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Fullscreen,
                title = stringResource(R.string.hide_status_bar),
                summary = stringResource(R.string.hide_status_bar_summary),
                colors = colors,
                onClick = onHideStatusBarRowClicked,
                trailing = {
                    ClintSwitch(checked = state.hideStatusBar)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Fullscreen,
                title = stringResource(R.string.hide_system_navigation),
                summary = stringResource(R.string.hide_system_navigation_summary),
                colors = colors,
                onClick = onHideSystemNavigationRowClicked,
                trailing = {
                    ClintSwitch(checked = state.hideSystemNavigation)
                }
            )
        }

        SectionLabel(stringResource(R.string.pref_category_navigation).uppercase(), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Close,
                title = stringResource(R.string.exit_confirmation_title),
                summary = stringResource(exitConfirmationSummaryRes(state.exitConfirmation)),
                colors = colors,
                onClick = { state.openDialog = LookAndFeelDialog.EXIT_CONFIRMATION }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Layers,
                title = stringResource(R.string.pref_popup_alert_style_title),
                summary = stringResource(popupAlertStyleSummaryRes(state.popupAlertStyle)),
                colors = colors,
                onClick = { state.openDialog = LookAndFeelDialog.POPUP_ALERT_STYLE }
            )
        }
    }
}

private fun accentSummaryRes(accent: String): Int = when (accent) {
    "material_you" -> R.string.accent_material_you
    "purple" -> R.string.accent_purple
    "deep_purple" -> R.string.accent_deep_purple
    "royal_purple" -> R.string.accent_royal_purple
    "amethyst" -> R.string.accent_amethyst
    "lavender" -> R.string.accent_lavender
    "teal" -> R.string.accent_teal
    "azure" -> R.string.accent_azure
    "pink" -> R.string.accent_pink
    "indigo" -> R.string.accent_indigo
    "cyan" -> R.string.accent_cyan
    "amber" -> R.string.accent_amber
    "mint" -> R.string.accent_mint
    "crimson" -> R.string.accent_crimson
    "slate" -> R.string.accent_slate
    "graphite" -> R.string.accent_graphite
    "obsidian" -> R.string.accent_obsidian
    "onyx" -> R.string.accent_onyx
    "titanium" -> R.string.accent_titanium
    "coral" -> R.string.accent_coral
    "burgundy" -> R.string.accent_burgundy
    "scarlet" -> R.string.accent_scarlet
    "midnight" -> R.string.accent_midnight
    "sepia" -> R.string.accent_sepia
    "mustard" -> R.string.accent_mustard
    "forest" -> R.string.accent_forest
    "sage" -> R.string.accent_sage
    "lime" -> R.string.accent_lime
    "olive" -> R.string.accent_olive
    "plum" -> R.string.accent_plum
    "violet" -> R.string.accent_violet
    "sand" -> R.string.accent_sand
    "ruby" -> R.string.accent_ruby
    "sky" -> R.string.accent_sky
    "charcoal" -> R.string.accent_charcoal
    "peach" -> R.string.accent_peach
    "terracotta" -> R.string.accent_terracotta
    "emerald" -> R.string.accent_emerald
    "blue" -> R.string.accent_blue
    "yellow" -> R.string.accent_yellow
    "lemon" -> R.string.accent_lemon
    "gold" -> R.string.accent_gold
    "red" -> R.string.accent_red
    "green" -> R.string.accent_green
    "orange" -> R.string.accent_orange
    "deep_orange" -> R.string.accent_deep_orange
    "tangerine" -> R.string.accent_tangerine
    "apricot" -> R.string.accent_apricot
    "copper" -> R.string.accent_copper
    else -> R.string.accent_default
}

private fun intensitySummaryRes(intensity: String): Int = when (intensity) {
    "amoled_no_tint" -> R.string.surface_intensity_amoled_no_tint
    "no_tint" -> R.string.surface_intensity_no_tint
    "strong_tint" -> R.string.surface_intensity_strong
    "pure_mode" -> R.string.surface_intensity_pure
    else -> R.string.surface_intensity_soft
}

private fun nestedScrollSummaryRes(mode: String): Int = when (mode) {
    "off" -> R.string.nested_scroll_off
    "navigation_bar" -> R.string.nested_scroll_nav_bar
    "both" -> R.string.nested_scroll_both
    else -> R.string.nested_scroll_search_bar
}

private fun addressBarPositionSummaryRes(position: String): Int = when (position) {
    "top" -> R.string.address_bar_position_top
    "bottom" -> R.string.address_bar_position_bottom
    else -> R.string.address_bar_position_split
}

private fun menuStyleSummaryRes(style: String): Int =
    if (style == "bottom_sheet") R.string.menu_style_bottom_sheet else R.string.menu_style_popup

private fun exitConfirmationSummaryRes(value: String): Int = when (value) {
    "off" -> R.string.exit_confirmation_off
    "dialog" -> R.string.exit_confirmation_dialog
    else -> R.string.exit_confirmation_toast
}

private fun popupAlertStyleSummaryRes(value: String): Int =
    if (value == "snackbar") R.string.popup_alert_style_snackbar else R.string.popup_alert_style_dialog

@Composable
fun languageSummaryText(language: String): String {
    val option = languageOptions.firstOrNull { it.tag == language }
    return stringResource(option?.nameRes ?: R.string.language_system)
}

private fun Color.toHex(): String = String.format(java.util.Locale.ROOT, "#%06X", toArgb() and 0xFFFFFF)

@Composable
private fun DarkReaderColorHint(colors: ClintColors) {
    val context = LocalContext.current
    val copiedText = stringResource(R.string.download_props_copied)
    val entries = listOf(
        stringResource(R.string.pref_dark_reader_label_background) to colors.background,
        stringResource(R.string.pref_dark_reader_label_text) to colors.onSurface,
        stringResource(R.string.pref_dark_reader_label_scrollbar) to colors.primary,
        stringResource(R.string.pref_dark_reader_label_selection) to colors.primary
    )
    Text(
        text = stringResource(R.string.pref_dark_reader_theme_hint),
        color = colors.secondaryText,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 6.dp)
    )
    SettingsSection(colors.cardBackground, bottomSpacing = 10.dp) {
        entries.forEachIndexed { index, (label, color) ->
            if (index > 0) RowDivider(colors.divider)
            val hex = color.toHex()
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText(label, hex))
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, colors.divider, CircleShape)
                )
                Text(
                    text = label,
                    color = colors.onSurface,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f).padding(start = 14.dp)
                )
                Text(
                    text = hex,
                    color = colors.secondaryText,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = null,
                    tint = colors.iconTint,
                    modifier = Modifier.padding(start = 12.dp).size(18.dp)
                )
            }
        }
    }
    Text(
        text = stringResource(R.string.pref_dark_reader_detect_hint),
        color = colors.secondaryText,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 24.dp)
    )
}
