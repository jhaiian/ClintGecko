package com.jhaiian.clint.settings.lookandfeel

import com.jhaiian.clint.ui.theme.rememberIsLightTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.jhaiian.clint.ui.ClintRadioButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.scrollToSelection
import com.jhaiian.clint.settings.common.SettingsPickerOptionBottomSpacing
import com.jhaiian.clint.settings.common.SettingsPickerOptionContentPadding
import com.jhaiian.clint.setup.AccentSwatch
import com.jhaiian.clint.setup.AddressBarPreview
import com.jhaiian.clint.setup.CheckSlot
import com.jhaiian.clint.setup.DefaultChip
import com.jhaiian.clint.setup.DrawableImage
import com.jhaiian.clint.setup.MenuStylePreview
import com.jhaiian.clint.setup.ScrollHidePreview
import com.jhaiian.clint.setup.SelectableCard
import com.jhaiian.clint.setup.navBarSlotDescRes
import com.jhaiian.clint.setup.navBarSlotTitleRes
import com.jhaiian.clint.setup.rememberIntensitySwatchColors
import com.jhaiian.clint.setup.scrollCardVisible
import com.jhaiian.clint.ui.ThemeSwatchUtils
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.LocaleHelper

private val OptionContentPadding = SettingsPickerOptionContentPadding
private val OptionBottomSpacing = SettingsPickerOptionBottomSpacing

@Composable
private fun rememberBgSurface(theme: String, accent: String): Pair<Color, Color> {
    val context = LocalContext.current
    val isLight = rememberIsLightTheme(theme)
    return remember(theme, accent, isLight) {
        val swatch = ThemeSwatchUtils.resolveSwatchColors(context, theme, accent)
        Color(swatch.bg) to Color(swatch.surface)
    }
}

@Composable
fun ThemeSelectorDialog(current: String, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val scrollState = rememberScrollState()
    ClintDialog(title = stringResource(R.string.pref_app_theme_title), hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = onDismiss, scrollState = scrollState) {
        data class ThemeOption(val key: String, val titleRes: Int, val descRes: Int, val drawableRes: Int)
        listOf(
            ThemeOption("system", R.string.theme_system, R.string.theme_system_desc, R.drawable.theme_swatch_system),
            ThemeOption("dark", R.string.theme_dark, R.string.theme_dark_desc, R.drawable.theme_swatch_dark),
            ThemeOption("light", R.string.theme_light, R.string.theme_light_desc, R.drawable.theme_swatch_light)
        ).forEach { option ->
            SelectableCard(
                selected = current == option.key, onClick = { onSelect(option.key) },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
                modifier = Modifier.scrollToSelection(scrollState, current == option.key)
            ) {
                DrawableImage(option.drawableRes, modifier = Modifier.size(44.dp))
                Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                CheckSlot(current == option.key, colors.primary)
            }
        }
    }
}

@Composable
fun AccentColorDialog(current: String, theme: String, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    ClintDialog(title = stringResource(R.string.pref_accent_color_title), hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = onDismiss, scrollState = scrollState) {
        data class AccentOption(val key: String, val titleRes: Int, val descRes: Int)
        listOf(
            AccentOption("material_you", R.string.accent_material_you, R.string.accent_material_you_desc),
            AccentOption("purple", R.string.accent_purple, R.string.accent_purple_desc),
            AccentOption("deep_purple", R.string.accent_deep_purple, R.string.accent_deep_purple_desc),
            AccentOption("royal_purple", R.string.accent_royal_purple, R.string.accent_royal_purple_desc),
            AccentOption("amethyst", R.string.accent_amethyst, R.string.accent_amethyst_desc),
            AccentOption("lavender", R.string.accent_lavender, R.string.accent_lavender_desc),
            AccentOption("plum", R.string.accent_plum, R.string.accent_plum_desc),
            AccentOption("violet", R.string.accent_violet, R.string.accent_violet_desc),
            AccentOption("default", R.string.accent_default, R.string.accent_default_desc),
            AccentOption("charcoal", R.string.accent_charcoal, R.string.accent_charcoal_desc),
            AccentOption("slate", R.string.accent_slate, R.string.accent_slate_desc),
            AccentOption("graphite", R.string.accent_graphite, R.string.accent_graphite_desc),
            AccentOption("obsidian", R.string.accent_obsidian, R.string.accent_obsidian_desc),
            AccentOption("onyx", R.string.accent_onyx, R.string.accent_onyx_desc),
            AccentOption("titanium", R.string.accent_titanium, R.string.accent_titanium_desc),
            AccentOption("indigo", R.string.accent_indigo, R.string.accent_indigo_desc),
            AccentOption("blue", R.string.accent_blue, R.string.accent_blue_desc),
            AccentOption("midnight", R.string.accent_midnight, R.string.accent_midnight_desc),
            AccentOption("cyan", R.string.accent_cyan, R.string.accent_cyan_desc),
            AccentOption("sky", R.string.accent_sky, R.string.accent_sky_desc),
            AccentOption("teal", R.string.accent_teal, R.string.accent_teal_desc),
            AccentOption("azure", R.string.accent_azure, R.string.accent_azure_desc),
            AccentOption("yellow", R.string.accent_yellow, R.string.accent_yellow_desc),
            AccentOption("lemon", R.string.accent_lemon, R.string.accent_lemon_desc),
            AccentOption("gold", R.string.accent_gold, R.string.accent_gold_desc),
            AccentOption("amber", R.string.accent_amber, R.string.accent_amber_desc),
            AccentOption("sand", R.string.accent_sand, R.string.accent_sand_desc),
            AccentOption("sepia", R.string.accent_sepia, R.string.accent_sepia_desc),
            AccentOption("mustard", R.string.accent_mustard, R.string.accent_mustard_desc),
            AccentOption("red", R.string.accent_red, R.string.accent_red_desc),
            AccentOption("crimson", R.string.accent_crimson, R.string.accent_crimson_desc),
            AccentOption("ruby", R.string.accent_ruby, R.string.accent_ruby_desc),
            AccentOption("pink", R.string.accent_pink, R.string.accent_pink_desc),
            AccentOption("coral", R.string.accent_coral, R.string.accent_coral_desc),
            AccentOption("burgundy", R.string.accent_burgundy, R.string.accent_burgundy_desc),
            AccentOption("scarlet", R.string.accent_scarlet, R.string.accent_scarlet_desc),
            AccentOption("orange", R.string.accent_orange, R.string.accent_orange_desc),
            AccentOption("deep_orange", R.string.accent_deep_orange, R.string.accent_deep_orange_desc),
            AccentOption("tangerine", R.string.accent_tangerine, R.string.accent_tangerine_desc),
            AccentOption("apricot", R.string.accent_apricot, R.string.accent_apricot_desc),
            AccentOption("copper", R.string.accent_copper, R.string.accent_copper_desc),
            AccentOption("peach", R.string.accent_peach, R.string.accent_peach_desc),
            AccentOption("terracotta", R.string.accent_terracotta, R.string.accent_terracotta_desc),
            AccentOption("green", R.string.accent_green, R.string.accent_green_desc),
            AccentOption("emerald", R.string.accent_emerald, R.string.accent_emerald_desc),
            AccentOption("mint", R.string.accent_mint, R.string.accent_mint_desc),
            AccentOption("forest", R.string.accent_forest, R.string.accent_forest_desc),
            AccentOption("sage", R.string.accent_sage, R.string.accent_sage_desc),
            AccentOption("lime", R.string.accent_lime, R.string.accent_lime_desc),
            AccentOption("olive", R.string.accent_olive, R.string.accent_olive_desc)
        ).forEach { option ->
            val swatch = remember(theme, option.key, colors.isLight) {
                ThemeSwatchUtils.resolveSwatchColors(context, theme, option.key)
            }
            SelectableCard(
                selected = current == option.key, onClick = { onSelect(option.key) },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
                modifier = Modifier.scrollToSelection(scrollState, current == option.key)
            ) {
                AccentSwatch(Color(swatch.bg), Color(swatch.surface), Color(swatch.accent))
                Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                CheckSlot(current == option.key, colors.primary)
            }
        }
    }
}

@Composable
fun SurfaceIntensityDialog(
    current: String,
    theme: String,
    accent: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val strongVisible = accent in setOf("purple", "deep_purple", "royal_purple", "amethyst", "lavender", "teal", "pink", "indigo", "cyan", "amber", "mint", "crimson", "slate", "graphite", "obsidian", "onyx", "coral", "midnight", "sepia", "forest", "plum", "sand", "ruby", "sky", "charcoal", "peach", "emerald", "blue", "yellow", "lemon", "gold", "red", "green", "orange", "deep_orange", "tangerine", "apricot", "copper", "scarlet", "lime", "olive", "default", "material_you", "violet", "titanium", "azure", "mustard", "burgundy", "terracotta", "sage")
    val swatches = rememberIntensitySwatchColors(theme, accent)
    val scrollState = rememberScrollState()

    ClintDialog(title = stringResource(R.string.pref_surface_intensity_title), hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = onDismiss, scrollState = scrollState) {
        SelectableCard(
            selected = current == "no_tint", onClick = { onSelect("no_tint") },
            cardBackground = colors.surfaceVariant, primary = colors.primary,
            contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
            modifier = Modifier.scrollToSelection(scrollState, current == "no_tint")
        ) {
            AccentSwatch(swatches.noTintBg, swatches.noTintSurface, swatches.accent)
            Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                Text(stringResource(R.string.surface_intensity_no_tint), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.surface_intensity_no_tint_desc), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
            CheckSlot(current == "no_tint", colors.primary)
        }
        SelectableCard(
            selected = current == "soft_tint", onClick = { onSelect("soft_tint") },
            cardBackground = colors.surfaceVariant, primary = colors.primary,
            contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
            modifier = Modifier.scrollToSelection(scrollState, current == "soft_tint")
        ) {
            AccentSwatch(swatches.softBg, swatches.softSurface, swatches.accent)
            Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                Text(stringResource(R.string.surface_intensity_soft), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.surface_intensity_soft_desc), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
            CheckSlot(current == "soft_tint", colors.primary)
        }
        if (strongVisible) {
            SelectableCard(
                selected = current == "strong_tint", onClick = { onSelect("strong_tint") },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
                modifier = Modifier.scrollToSelection(scrollState, current == "strong_tint")
            ) {
                AccentSwatch(swatches.strongBg, swatches.strongSurface, swatches.accent)
                Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                    Text(stringResource(R.string.surface_intensity_strong), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.surface_intensity_strong_desc), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                CheckSlot(current == "strong_tint", colors.primary)
            }
        }
        SelectableCard(
            selected = current == "pure_mode", onClick = { onSelect("pure_mode") },
            cardBackground = colors.surfaceVariant, primary = colors.primary,
            contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
            modifier = Modifier.scrollToSelection(scrollState, current == "pure_mode")
        ) {
            AccentSwatch(swatches.pureBg, swatches.pureSurface, swatches.accent)
            Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                Text(stringResource(R.string.surface_intensity_pure), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(if (colors.isLight) R.string.surface_intensity_pure_light_desc else R.string.surface_intensity_pure_dark_desc),
                    color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp)
                )
            }
            CheckSlot(current == "pure_mode", colors.primary)
        }
        SelectableCard(
            selected = current == "amoled_no_tint", onClick = { onSelect("amoled_no_tint") },
            cardBackground = colors.surfaceVariant, primary = colors.primary,
            contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
            modifier = Modifier.scrollToSelection(scrollState, current == "amoled_no_tint")
        ) {
            AccentSwatch(swatches.pureBg, swatches.pureSurface, swatches.accent)
            Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
                Text(stringResource(R.string.surface_intensity_amoled_no_tint), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(if (colors.isLight) R.string.surface_intensity_amoled_no_tint_light_desc else R.string.surface_intensity_amoled_no_tint_dark_desc),
                    color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp)
                )
            }
            CheckSlot(current == "amoled_no_tint", colors.primary)
        }
    }
}

@Composable
fun AddressBarPositionDialog(
    current: String,
    theme: String,
    accent: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val (bg, surface) = rememberBgSurface(theme, accent)
    val onSurface = colors.onSurface
    val scrollState = rememberScrollState()

    ClintDialog(title = stringResource(R.string.pref_address_bar_position_title), hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = onDismiss, scrollState = scrollState) {
        data class AddrOption(val key: String, val titleRes: Int, val descRes: Int)
        listOf(
            AddrOption("top", R.string.address_bar_position_top, R.string.address_bar_position_top_desc),
            AddrOption("bottom", R.string.address_bar_position_bottom, R.string.address_bar_position_bottom_desc),
            AddrOption("split", R.string.address_bar_position_split, R.string.address_bar_position_split_desc)
        ).forEach { option ->
            SelectableCard(
                selected = current == option.key, onClick = { onSelect(option.key) },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
                modifier = Modifier.scrollToSelection(scrollState, current == option.key)
            ) {
                AddressBarPreview(option.key, bg, surface, onSurface)
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                CheckSlot(current == option.key, colors.primary)
            }
        }
    }
}

@Composable
fun MenuStyleDialog(
    current: String,
    addressBarPosition: String,
    theme: String,
    accent: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val (bg, surface) = rememberBgSurface(theme, accent)
    val onSurface = colors.onSurface
    val panelBg = colors.popupBackground
    val scrollState = rememberScrollState()

    ClintDialog(title = stringResource(R.string.pref_menu_style_title), hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = onDismiss, scrollState = scrollState) {
        data class MenuOption(val key: String, val variant: String, val titleRes: Int, val descRes: Int)
        listOf(
            MenuOption("popup", "popup", R.string.menu_style_popup, R.string.menu_style_popup_desc),
            MenuOption("bottom_sheet", "sheet", R.string.menu_style_bottom_sheet, R.string.menu_style_bottom_sheet_desc)
        ).forEach { option ->
            SelectableCard(
                selected = current == option.key, onClick = { onSelect(option.key) },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
                modifier = Modifier.scrollToSelection(scrollState, current == option.key)
            ) {
                MenuStylePreview(option.variant, addressBarPosition, bg, surface, onSurface, panelBg)
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
                CheckSlot(current == option.key, colors.primary)
            }
        }
    }
}

@Composable
fun ScrollHideModeDialog(
    current: String,
    addressBarPosition: String,
    theme: String,
    accent: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val (bg, surface) = rememberBgSurface(theme, accent)
    val onSurface = colors.onSurface
    val scrollState = rememberScrollState()

    ClintDialog(title = stringResource(R.string.pref_nested_scroll_title), hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = onDismiss, scrollState = scrollState) {
        listOf("off", "search_bar", "navigation_bar", "both").forEach { kind ->
            if (scrollCardVisible(kind, addressBarPosition)) {
                val (titleRes, descRes) = when (kind) {
                    "off" -> R.string.nested_scroll_off to R.string.nested_scroll_off_desc
                    "search_bar" -> R.string.nested_scroll_search_bar to R.string.nested_scroll_search_bar_desc
                    "navigation_bar" -> navBarSlotTitleRes(addressBarPosition) to navBarSlotDescRes(addressBarPosition)
                    else -> R.string.nested_scroll_both to R.string.nested_scroll_both_desc
                }
                val selected = when (kind) {
                    "search_bar" -> current == "search_bar" && addressBarPosition != "bottom"
                    "navigation_bar" -> (current == "search_bar" && addressBarPosition == "bottom") ||
                        (current == "navigation_bar" && addressBarPosition == "split")
                    else -> current == kind
                }
                SelectableCard(
                    selected = selected, onClick = { onSelect(kind) },
                    cardBackground = colors.surfaceVariant, primary = colors.primary,
                    contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
                    modifier = Modifier.scrollToSelection(scrollState, selected)
                ) {
                    ScrollHidePreview(kind, addressBarPosition, bg, surface, onSurface, animate = kind != "off")
                    Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                        Text(stringResource(titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(stringResource(descRes), color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    CheckSlot(selected, colors.primary)
                }
            }
        }
    }
}

@Composable
fun ExitConfirmationDialog(
    current: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var selected by remember(current) { mutableStateOf(current) }
    val scrollState = rememberScrollState()

    ClintDialog(
        title = stringResource(R.string.exit_confirmation_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        scrollState = scrollState,
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
        data class ExitOption(val key: String, val titleRes: Int, val descRes: Int, val showDefault: Boolean)
        listOf(
            ExitOption("off", R.string.exit_confirmation_off, R.string.exit_confirmation_off_desc, false),
            ExitOption("toast", R.string.exit_confirmation_toast, R.string.exit_confirmation_toast_desc, true),
            ExitOption("dialog", R.string.exit_confirmation_dialog, R.string.exit_confirmation_dialog_desc, false)
        ).forEach { option ->
            val sel = selected == option.key
            SelectableCard(
                selected = sel, onClick = { selected = option.key },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
                modifier = Modifier.scrollToSelection(scrollState, sel)
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
fun PopupAlertStyleDialog(
    current: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val scrollState = rememberScrollState()

    ClintDialog(
        title = stringResource(R.string.pref_popup_alert_style_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        scrollState = scrollState
    ) {
        data class StyleOption(val key: String, val titleRes: Int, val descRes: Int, val showDefault: Boolean)
        listOf(
            StyleOption("dialog", R.string.popup_alert_style_dialog, R.string.popup_alert_style_dialog_desc, true),
            StyleOption("snackbar", R.string.popup_alert_style_snackbar, R.string.popup_alert_style_snackbar_desc, false)
        ).forEach { option ->
            val sel = current == option.key
            SelectableCard(
                selected = sel, onClick = { onSelect(option.key) },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
                modifier = Modifier.scrollToSelection(scrollState, sel)
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
fun LanguageSelectorDialog(current: String, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val scrollState = rememberScrollState()

    ClintDialog(title = stringResource(R.string.pref_language_title), hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = onDismiss, scrollState = scrollState) {
        val systemSelected = current == LocaleHelper.LANGUAGE_SYSTEM
        SelectableCard(
            selected = systemSelected, onClick = { onSelect(LocaleHelper.LANGUAGE_SYSTEM) },
            cardBackground = colors.surfaceVariant, primary = colors.primary,
            contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
            modifier = Modifier.scrollToSelection(scrollState, systemSelected)
        ) {
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(stringResource(R.string.language_system), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(stringResource(R.string.language_system_desc), color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
            CheckSlot(systemSelected, colors.primary)
        }
        languageOptions.forEach { option ->
            val selected = current == option.tag
            SelectableCard(
                selected = selected, onClick = { onSelect(option.tag) },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing,
                modifier = Modifier.scrollToSelection(scrollState, selected)
            ) {
                Column(Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(stringResource(option.nameRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    if (option.tag == LocaleHelper.BASE_LANGUAGE_TAG) {
                        Text(
                            stringResource(R.string.language_base_desc),
                            color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                CheckSlot(selected, colors.primary)
            }
        }
    }
}
