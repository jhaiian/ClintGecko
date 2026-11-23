package com.jhaiian.clint.setup

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ThemeSwatchUtils
import com.jhaiian.clint.ui.theme.LocalClintColors

private data class LayoutChoice(val key: String, val titleRes: Int, val descRes: Int)

private val AddressBarChoices = listOf(
    LayoutChoice("top", R.string.address_bar_position_top, R.string.address_bar_position_top_desc),
    LayoutChoice("bottom", R.string.address_bar_position_bottom, R.string.address_bar_position_bottom_desc),
    LayoutChoice("split", R.string.address_bar_position_split, R.string.address_bar_position_split_desc)
)

private val MenuChoices = listOf(
    LayoutChoice("popup", R.string.menu_style_popup, R.string.menu_style_popup_desc),
    LayoutChoice("bottom_sheet", R.string.menu_style_bottom_sheet, R.string.menu_style_bottom_sheet_desc)
)

internal fun fullScrollHideMode(position: String): String = when (position) {
    "bottom" -> "navigation_bar"
    "split" -> "both"
    else -> "search_bar"
}

@Composable
internal fun SetupLayoutPage(state: SetupUiState, metrics: PageMetrics, scrollState: ScrollState) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val haptics = rememberSetupHaptics()
    val swatchTheme = if (colors.isLight) "light" else "dark"
    val swatch = remember(swatchTheme, state.accent) {
        ThemeSwatchUtils.resolveSwatchColors(context, swatchTheme, state.accent)
    }
    val bg = Color(swatch.bg)
    val surface = Color(swatch.surface)
    val onSurface = colors.onSurface
    val panelBg = colors.popupBackground
    val hideOnScroll = state.scrollHideMode != "off"

    SetupPage(
        metrics,
        scrollState,
        hero = { LayoutPreviewHero(state.addressBarPosition, state.menuStyle, hideOnScroll, it) }
    ) {
        PageHeader(stringResource(R.string.setup_layout_title), stringResource(R.string.setup_layout_subtitle), metrics)
        Spacer(Modifier.height(24.dp))
        SectionTitle(stringResource(R.string.setup_section_address_bar), metrics, Modifier.rise(2))
        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .rise(3)
        ) {
            AddressBarChoices.forEach { choice ->
                ChoiceCard(
                    label = stringResource(choice.titleRes),
                    selected = state.addressBarPosition == choice.key,
                    onClick = {
                        if (state.addressBarPosition != choice.key) {
                            haptics.tick()
                            state.addressBarPosition = choice.key
                            state.scrollHideMode = if (hideOnScroll) fullScrollHideMode(choice.key) else "off"
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    AddressBarPreview(choice.key, bg, surface, onSurface)
                }
            }
        }
        Crossfade(targetState = state.addressBarPosition, label = "address_caption", modifier = Modifier.rise(3)) { key ->
            AddressBarChoices.firstOrNull { it.key == key }?.let { SetupCaptionText(stringResource(it.descRes), metrics) }
        }
        Spacer(Modifier.height(22.dp))
        SectionTitle(stringResource(R.string.setup_section_menu_style), metrics, Modifier.rise(4))
        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .rise(5)
        ) {
            MenuChoices.forEach { choice ->
                ChoiceCard(
                    label = stringResource(choice.titleRes),
                    selected = state.menuStyle == choice.key,
                    onClick = {
                        if (state.menuStyle != choice.key) {
                            haptics.tick()
                            state.menuStyle = choice.key
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    MenuStylePreview(if (choice.key == "popup") "popup" else "sheet", state.addressBarPosition, bg, surface, onSurface, panelBg)
                }
            }
        }
        Crossfade(targetState = state.menuStyle, label = "menu_caption", modifier = Modifier.rise(5)) { key ->
            MenuChoices.firstOrNull { it.key == key }?.let { SetupCaptionText(stringResource(it.descRes), metrics) }
        }
        Spacer(Modifier.height(22.dp))
        SetupToggleCard(
            icon = Icons.Outlined.UnfoldLess,
            title = stringResource(R.string.setup_layout_hide_on_scroll_title),
            body = stringResource(R.string.setup_layout_hide_on_scroll),
            checked = hideOnScroll,
            onCheckedChange = { enabled ->
                haptics.toggle(enabled)
                state.scrollHideMode = if (enabled) fullScrollHideMode(state.addressBarPosition) else "off"
            },
            modifier = Modifier.rise(6)
        )
    }
}
