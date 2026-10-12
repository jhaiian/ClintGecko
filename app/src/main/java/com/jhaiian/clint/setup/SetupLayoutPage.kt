package com.jhaiian.clint.setup
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.VisibilityOff

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.jhaiian.clint.ui.ClintSwitch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ThemeSwatchUtils
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun SetupLayoutPage(
    addressBarPosition: String,
    menuStyle: String,
    scrollHideMode: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    theme: String,
    accent: String,
    onAddressBarPositionSelected: (String) -> Unit,
    onMenuStyleSelected: (String) -> Unit,
    onScrollHideModeSelected: (String) -> Unit,
    onHideStatusBarToggled: (Boolean) -> Unit,
    onHideSystemNavigationToggled: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current

    val swatch = remember(theme, accent, colors.isLight) {
        ThemeSwatchUtils.resolveSwatchColors(context, theme, accent)
    }
    val bg = androidx.compose.ui.graphics.Color(swatch.bg)
    val surface = androidx.compose.ui.graphics.Color(swatch.surface)
    val onSurface = colors.onSurface
    val panelBg = colors.popupBackground

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp)) {
        SetupPageHeader(
            title = stringResource(R.string.setup_layout_title),
            subtitle = stringResource(R.string.setup_layout_subtitle)
        )

        data class AddrOption(val key: String, val titleRes: Int, val descRes: Int)
        val addrOptions = listOf(
            AddrOption("top", R.string.address_bar_position_top, R.string.address_bar_position_top_desc),
            AddrOption("bottom", R.string.address_bar_position_bottom, R.string.address_bar_position_bottom_desc),
            AddrOption("split", R.string.address_bar_position_split, R.string.address_bar_position_split_desc)
        )
        SetupSectionHeading(
            androidx.compose.material.icons.Icons.Filled.ViewAgenda,
            stringResource(R.string.setup_section_address_bar), colors.primary,
            Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            addrOptions.forEach { option ->
                OptionTile(
                    selected = addressBarPosition == option.key,
                    onClick = { onAddressBarPositionSelected(option.key) },
                    label = stringResource(option.titleRes),
                    cardBackground = colors.cardBackground, primary = colors.primary, onPrimary = colors.onPrimary
                ) {
                    AddressBarPreview(option.key, bg, surface, onSurface)
                }
            }
        }
        Crossfade(targetState = addressBarPosition, label = "addrCaption") { key ->
            addrOptions.firstOrNull { it.key == key }?.let { SetupCaption(stringResource(it.descRes)) }
        }

        data class MenuOption(val key: String, val variant: String, val titleRes: Int, val descRes: Int)
        val menuOptions = listOf(
            MenuOption("popup", "popup", R.string.menu_style_popup, R.string.menu_style_popup_desc),
            MenuOption("bottom_sheet", "sheet", R.string.menu_style_bottom_sheet, R.string.menu_style_bottom_sheet_desc)
        )
        SetupSectionHeading(
            androidx.compose.material.icons.Icons.Filled.Menu,
            stringResource(R.string.setup_section_menu_style), colors.primary,
            Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            menuOptions.forEach { option ->
                OptionTile(
                    selected = menuStyle == option.key,
                    onClick = { onMenuStyleSelected(option.key) },
                    label = stringResource(option.titleRes),
                    cardBackground = colors.cardBackground, primary = colors.primary, onPrimary = colors.onPrimary
                ) {
                    MenuStylePreview(option.variant, addressBarPosition, bg, surface, onSurface, panelBg)
                }
            }
        }
        Crossfade(targetState = menuStyle, label = "menuCaption") { key ->
            menuOptions.firstOrNull { it.key == key }?.let { SetupCaption(stringResource(it.descRes)) }
        }

        data class ScrollOption(val key: String, val titleRes: Int, val descRes: Int)
        val scrollOptions = listOf(
            ScrollOption("off", R.string.nested_scroll_off, R.string.nested_scroll_off_desc),
            ScrollOption("search_bar", R.string.nested_scroll_search_bar, R.string.nested_scroll_search_bar_desc),
            ScrollOption("navigation_bar", navBarSlotTitleRes(addressBarPosition), navBarSlotDescRes(addressBarPosition)),
            ScrollOption("both", R.string.nested_scroll_both, R.string.nested_scroll_both_desc)
        ).filter { scrollCardVisible(it.key, addressBarPosition) }
        SetupSectionHeading(
            androidx.compose.material.icons.Icons.Filled.SwapVert,
            stringResource(R.string.setup_section_nested_scroll), colors.primary,
            Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp)
        )
        Column(Modifier.fillMaxWidth()) {
            scrollOptions.chunked(2).forEach { rowItems ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { option ->
                        OptionTile(
                            selected = scrollHideMode == option.key,
                            onClick = { onScrollHideModeSelected(option.key) },
                            label = stringResource(option.titleRes),
                            cardBackground = colors.cardBackground, primary = colors.primary, onPrimary = colors.onPrimary
                        ) {
                            ScrollHidePreview(option.key, addressBarPosition, bg, surface, onSurface, animate = option.key != "off")
                        }
                    }
                }
            }
        }
        Crossfade(targetState = scrollHideMode, label = "scrollCaption") { key ->
            scrollOptions.firstOrNull { it.key == key }?.let { SetupCaption(stringResource(it.descRes)) }
        }

        SetupSectionHeading(
            androidx.compose.material.icons.Icons.Filled.VisibilityOff,
            stringResource(R.string.setup_section_status_bar), colors.primary,
            Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 12.dp)
        )
        Card(
            Modifier.fillMaxWidth().padding(bottom = 24.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().clickable { onHideStatusBarToggled(!hideStatusBar) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(stringResource(R.string.hide_status_bar), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.hide_status_bar_summary), color = colors.secondaryText, fontSize = 12.sp, lineHeight = 15.6.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    ClintSwitch(checked = hideStatusBar)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.surfaceVariant))
                Row(
                    Modifier.fillMaxWidth().clickable { onHideSystemNavigationToggled(!hideSystemNavigation) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(stringResource(R.string.hide_system_navigation), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(stringResource(R.string.hide_system_navigation_summary), color = colors.secondaryText, fontSize = 12.sp, lineHeight = 15.6.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    ClintSwitch(checked = hideSystemNavigation)
                }
            }
        }

        SetupPrimaryButton(stringResource(R.string.next), onNext, colors.buttonBackground, Modifier.padding(bottom = 24.dp))
    }
}
