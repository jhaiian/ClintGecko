package com.jhaiian.clint.customtheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.menu.CustomizableMenuItem
import com.jhaiian.clint.browser.menu.icon
import com.jhaiian.clint.browser.menu.titleRes
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsRowDividerInset
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.theme.LocalClintColors
import java.util.Calendar
import java.util.Locale

private data class ContrastPair(val labelRes: Int, val foreground: ThemeSlot, val background: ThemeSlot)

private val contrastPairs = listOf(
    ContrastPair(R.string.custom_theme_contrast_text_background, ThemeSlot.ON_SURFACE, ThemeSlot.BACKGROUND),
    ContrastPair(R.string.custom_theme_contrast_secondary_background, ThemeSlot.SECONDARY_TEXT, ThemeSlot.BACKGROUND),
    ContrastPair(R.string.custom_theme_contrast_text_card, ThemeSlot.ON_SURFACE, ThemeSlot.CARD_BACKGROUND),
    ContrastPair(R.string.custom_theme_contrast_on_primary, ThemeSlot.ON_PRIMARY, ThemeSlot.PRIMARY),
    ContrastPair(R.string.custom_theme_contrast_button, ThemeSlot.BUTTON_TEXT, ThemeSlot.BUTTON_BACKGROUND),
    ContrastPair(R.string.custom_theme_contrast_popup, ThemeSlot.POPUP_TEXT, ThemeSlot.POPUP_BACKGROUND),
    ContrastPair(R.string.custom_theme_contrast_error, ThemeSlot.ON_ERROR_CONTAINER, ThemeSlot.ERROR_CONTAINER)
)

internal fun formatArgb(argb: Int): String {
    val alpha = (argb ushr 24) and 0xFF
    return if (alpha == 0xFF) {
        String.format(Locale.ROOT, "#%06X", argb and 0xFFFFFF)
    } else {
        String.format(Locale.ROOT, "#%08X", argb)
    }
}

private fun barIconColor(bar: Color): Color =
    if (bar.luminance() > 0.5f) Color(0xFF1B1B1F) else Color.White

private enum class PreviewScreen(val labelRes: Int) {
    HOME(R.string.home),
    MENU(R.string.custom_theme_preview_screen_menu),
    SETTINGS(R.string.settings),
    DIALOG(R.string.custom_theme_preview_screen_dialog),
    ERROR(R.string.custom_theme_preview_screen_error)
}

@Composable
fun ThemePreviewPage(palette: CustomPalette, isLight: Boolean, modifier: Modifier = Modifier) {
    val appColors = LocalClintColors.current
    var screenIndex by remember { mutableIntStateOf(0) }
    val draft = remember(palette, isLight) { palette.toClintColors(isLight) }
    val labels = PreviewScreen.entries.map { stringResource(it.labelRes) }
    Column(modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.custom_theme_preview_title),
            color = appColors.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.custom_theme_preview_subtitle),
            color = appColors.secondaryText,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
        )
        PreviewTabs(options = labels, selected = screenIndex, onSelect = { screenIndex = it })
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            CompositionLocalProvider(LocalClintColors provides draft) {
                PhoneFrame(PreviewScreen.entries[screenIndex], palette, appColors.divider)
            }
        }
        PreviewSectionTitle(R.string.custom_theme_palette_title, null)
        PaletteGrid(palette)
        PreviewSectionTitle(R.string.custom_theme_contrast_title, R.string.custom_theme_contrast_subtitle)
        ContrastCard(palette)
    }
}

@Composable
private fun PreviewTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceVariant)
            .horizontalScroll(rememberScrollState())
            .padding(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) colors.primary else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (isSelected) colors.onPrimary else colors.secondaryText,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ErrorScreenMock(palette: CustomPalette) {
    val colors = LocalClintColors.current
    Column(Modifier.fillMaxSize().background(colors.background)) {
        MockStatusStrip(palette.color(ThemeSlot.STATUS_BAR))
        Row(
            Modifier.fillMaxWidth().background(colors.colorErrorContainer).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Warning, null, tint = colors.colorError, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.custom_theme_preview_error_banner),
                color = colors.colorOnErrorContainer,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().clipToBounds().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MockErrorField()
            MockSslCard()
            MockDeleteRow()
        }
        MockGestureStrip(palette.color(ThemeSlot.NAVIGATION_BAR))
    }
}

@Composable
private fun MockErrorField() {
    val colors = LocalClintColors.current
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(shape)
                .border(2.dp, colors.colorError, shape)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.shortcut_manager_url_hint),
                    color = colors.colorError,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    stringResource(R.string.custom_theme_preview_site_1_title),
                    color = colors.onSurface,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Filled.Warning, null, tint = colors.colorError, modifier = Modifier.size(20.dp))
        }
        Text(
            stringResource(R.string.shortcut_manager_url_invalid),
            color = colors.colorError,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
        )
    }
}

@Composable
private fun MockSslCard() {
    val colors = LocalClintColors.current
    Surface(color = colors.popupBackground, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(top = 20.dp)) {
            Row(
                Modifier.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Warning, null, tint = colors.colorError, modifier = Modifier.size(22.dp))
                Text(
                    stringResource(R.string.ssl_warning_title),
                    color = colors.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            Text(
                stringResource(R.string.ssl_warning_message, stringResource(R.string.custom_theme_preview_site_1_host)),
                color = colors.onSurface,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
            )
            Surface(
                color = colors.cardBackground,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text(
                    stringResource(R.string.ssl_error_untrusted),
                    color = colors.onSurface,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(end = 12.dp, top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {}, modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        stringResource(R.string.ssl_warning_proceed),
                        color = colors.colorError,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End
                    )
                }
                TextButton(onClick = {}) {
                    Text(
                        stringResource(R.string.action_cancel),
                        color = colors.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun MockDeleteRow() {
    val colors = LocalClintColors.current
    Surface(color = colors.cardBackground, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Delete, null, tint = colors.colorError, modifier = Modifier.size(22.dp))
            Text(
                stringResource(R.string.action_delete),
                color = colors.colorError,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 14.dp)
            )
        }
    }
}

@Composable
private fun PreviewSectionTitle(titleRes: Int, subtitleRes: Int?) {
    val colors = LocalClintColors.current
    Column(Modifier.padding(top = 24.dp, bottom = 10.dp)) {
        Text(
            stringResource(titleRes),
            color = colors.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        if (subtitleRes != null) {
            Text(
                stringResource(subtitleRes),
                color = colors.secondaryText,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun PhoneFrame(screen: PreviewScreen, palette: CustomPalette, borderColor: Color) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 340.dp)
            .height(720.dp)
            .clip(shape)
            .border(2.dp, borderColor, shape)
    ) {
        when (screen) {
            PreviewScreen.HOME -> BrowserScreenMock(palette, showMenu = false)
            PreviewScreen.MENU -> BrowserScreenMock(palette, showMenu = true)
            PreviewScreen.SETTINGS -> SettingsScreenMock(palette, showDialog = false)
            PreviewScreen.DIALOG -> SettingsScreenMock(palette, showDialog = true)
            PreviewScreen.ERROR -> ErrorScreenMock(palette)
        }
    }
}

@Composable
private fun MockStatusStrip(bar: Color) {
    val ink = barIconColor(bar)
    Row(
        Modifier
            .fillMaxWidth()
            .background(bar)
            .padding(horizontal = 18.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.custom_theme_preview_time),
            color = ink,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Filled.SignalCellularAlt, null, tint = ink, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Filled.Wifi, null, tint = ink, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Filled.BatteryFull, null, tint = ink, modifier = Modifier.size(13.dp))
    }
}

@Composable
private fun MockGestureStrip(bar: Color) {
    Box(
        Modifier.fillMaxWidth().background(bar).padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(96.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(barIconColor(bar))
        )
    }
}

@Composable
private fun BrowserScreenMock(palette: CustomPalette, showMenu: Boolean) {
    val colors = LocalClintColors.current
    Box(Modifier.fillMaxSize().background(colors.surface)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().background(colors.surface)) {
                MockStatusStrip(palette.color(ThemeSlot.STATUS_BAR))
                MockAddressBarRow()
                Box(Modifier.fillMaxWidth().height(4.dp))
            }
            MockHome(Modifier.weight(1f))
            MockGestureStrip(palette.color(ThemeSlot.NAVIGATION_BAR))
        }
        if (showMenu) {
            Box(Modifier.align(Alignment.TopEnd).padding(top = 78.dp, end = 8.dp)) {
                MockMenu()
            }
        }
    }
}

@Composable
private fun MockAddressBarRow() {
    val colors = LocalClintColors.current
    val hint = stringResource(R.string.search_bar_hint, stringResource(R.string.engine_duckduckgo))
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.padding(end = 6.dp).size(40.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Home, null, tint = colors.iconTint, modifier = Modifier.size(22.dp))
        }
        Surface(
            color = colors.addressBarColor,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.weight(1f).height(48.dp)
        ) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Search, null, tint = colors.iconTint, modifier = Modifier.size(20.dp))
                Text(
                    hint,
                    color = colors.secondaryText,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 12.dp).weight(1f)
                )
            }
        }
        Box(
            Modifier
                .padding(start = 10.dp)
                .size(width = 34.dp, height = 26.dp)
                .border(2.dp, colors.primary, RoundedCornerShape(5.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("1", color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Box(Modifier.padding(start = 4.dp).size(40.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.MoreVert, null, tint = colors.iconTint)
        }
    }
}

@Composable
private fun MockHome(modifier: Modifier) {
    val colors = LocalClintColors.current
    val topTint = colors.primary.copy(alpha = if (colors.isLight) 0.10f else 0.18f)
    val greetingRes = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> R.string.home_greeting_morning
            in 12..17 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        }
    }
    Column(
        modifier
            .fillMaxWidth()
            .clipToBounds()
            .background(colors.background)
            .background(Brush.verticalGradient(listOf(topTint, Color.Transparent)))
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        MockHero(stringResource(greetingRes))
        Spacer(Modifier.height(16.dp))
        MockSearchPill()
        Spacer(Modifier.height(16.dp))
        MockQuickActions()
        Spacer(Modifier.height(20.dp))
        MockSectionTitle(stringResource(R.string.home_favorites))
        MockEmptyFavorites()
        Spacer(Modifier.height(20.dp))
        MockSectionTitle(stringResource(R.string.home_recent))
        MockRecentCard()
    }
}

@Composable
private fun MockHero(greeting: String) {
    val colors = LocalClintColors.current
    val shape = RoundedCornerShape(22.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(listOf(colors.primary.copy(alpha = 0.30f), Color.Transparent)),
                        CircleShape
                    )
            )
            Box(
                Modifier
                    .size(52.dp)
                    .clip(shape)
                    .background(colors.cardBackground)
                    .border(1.dp, colors.primary.copy(alpha = 0.25f), shape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_clint_logo),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colors.primary),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(greeting, color = colors.secondaryText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(
            stringResource(R.string.app_name),
            color = colors.onSurface,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )
    }
}

@Composable
private fun MockSearchPill() {
    val colors = LocalClintColors.current
    val hint = stringResource(R.string.search_bar_hint, stringResource(R.string.engine_duckduckgo))
    Surface(
        color = colors.addressBarColor,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, colors.divider),
        shadowElevation = if (colors.isLight) 2.dp else 0.dp,
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 18.dp, end = 6.dp)
        ) {
            Icon(Icons.Filled.Search, null, tint = colors.primary, modifier = Modifier.size(22.dp))
            Text(
                hint,
                color = colors.secondaryText,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)
            )
            IconButton(onClick = {}) {
                Icon(Icons.Filled.Mic, null, tint = colors.iconTint)
            }
        }
    }
}

@Composable
private fun MockQuickActions() {
    val colors = LocalClintColors.current
    val actions = listOf(
        Icons.Filled.Bookmark to R.string.menu_bookmarks,
        Icons.Filled.History to R.string.menu_history,
        Icons.Filled.Download to R.string.menu_downloads,
        Icons.Filled.VisibilityOff to R.string.incognito
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        actions.forEach { (icon, labelRes) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp)
            ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(colors.cardBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = colors.primary, modifier = Modifier.size(24.dp))
                }
                Text(
                    stringResource(labelRes),
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun MockSectionTitle(text: String) {
    val colors = LocalClintColors.current
    Text(
        text.uppercase(),
        color = colors.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 10.dp)
    )
}

@Composable
private fun MockEmptyFavorites() {
    val colors = LocalClintColors.current
    Surface(
        color = colors.cardBackground,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Icon(Icons.Filled.Bookmark, null, tint = colors.primary, modifier = Modifier.size(24.dp))
            Text(
                stringResource(R.string.home_favorites_empty),
                color = colors.secondaryText,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 14.dp)
            )
        }
    }
}

@Composable
private fun MockRecentCard() {
    val colors = LocalClintColors.current
    val sites = listOf(
        stringResource(R.string.custom_theme_preview_site_1_title) to stringResource(R.string.custom_theme_preview_site_1_host),
        stringResource(R.string.custom_theme_preview_site_2_title) to stringResource(R.string.custom_theme_preview_site_2_host)
    )
    Surface(
        color = colors.cardBackground,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            sites.forEachIndexed { index, (title, host) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            title.take(1).uppercase(),
                            color = colors.primary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(
                            title,
                            color = colors.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            host,
                            color = colors.secondaryText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (index < sites.lastIndex) {
                    HorizontalDivider(
                        color = colors.divider,
                        thickness = 1.dp,
                        modifier = Modifier.padding(start = 68.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MockMenu() {
    val colors = LocalClintColors.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .width(240.dp)
            .shadow(3.dp, shape)
            .clip(shape)
            .background(colors.popupBackground)
            .border(1.dp, colors.popupStroke, shape)
            .padding(vertical = 6.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceVariant)
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MockMenuNavIcon(Icons.AutoMirrored.Filled.ArrowBack, true)
            MockMenuNavIcon(Icons.AutoMirrored.Filled.ArrowForward, false)
            MockMenuNavIcon(Icons.Filled.Home, true)
            MockMenuNavIcon(Icons.Filled.Refresh, true)
            MockMenuNavIcon(Icons.Filled.BookmarkBorder, true)
        }
        MockMenuDivider()
        MockMenuItem(CustomizableMenuItem.NEW_TAB.icon(), stringResource(CustomizableMenuItem.NEW_TAB.titleRes()), false)
        MockMenuItem(
            CustomizableMenuItem.NEW_INCOGNITO_TAB.icon(),
            stringResource(CustomizableMenuItem.NEW_INCOGNITO_TAB.titleRes()),
            false
        )
        MockMenuItem(CustomizableMenuItem.BOOKMARKS.icon(), stringResource(CustomizableMenuItem.BOOKMARKS.titleRes()), false)
        MockMenuItem(CustomizableMenuItem.HISTORY.icon(), stringResource(CustomizableMenuItem.HISTORY.titleRes()), false)
        MockMenuItem(
            CustomizableMenuItem.DESKTOP_MODE.icon(),
            stringResource(CustomizableMenuItem.DESKTOP_MODE.titleRes()),
            true
        )
        MockMenuDivider()
        MockMenuItem(Icons.Filled.Settings, stringResource(R.string.settings), false)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.MockMenuNavIcon(icon: ImageVector, enabled: Boolean) {
    val colors = LocalClintColors.current
    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = colors.iconTint, modifier = Modifier.alpha(if (enabled) 1f else 0.38f))
    }
}

@Composable
private fun MockMenuDivider() {
    HorizontalDivider(
        color = LocalClintColors.current.popupStroke,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
    )
}

@Composable
private fun MockMenuItem(icon: ImageVector, label: String, checked: Boolean) {
    val colors = LocalClintColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp)
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = colors.primary, modifier = Modifier.size(18.dp))
        }
        Text(
            label,
            color = colors.popupText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 14.dp)
        )
        if (checked) {
            Icon(Icons.Filled.Check, null, tint = colors.popupCheck, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SettingsScreenMock(palette: CustomPalette, showDialog: Boolean) {
    val colors = LocalClintColors.current
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(colors.background)) {
            MockStatusStrip(palette.color(ThemeSlot.STATUS_BAR))
            Surface(color = colors.background) {
                Row(
                    Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.onSurface)
                    }
                    Text(
                        stringResource(R.string.settings),
                        color = colors.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth().clipToBounds().padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
                SettingsSection(colors.cardBackground) {
                    SettingsRow(
                        icon = Icons.Filled.Palette,
                        title = stringResource(R.string.pref_accent_color_title),
                        summary = stringResource(R.string.accent_material_you),
                        colors = colors,
                        onClick = {}
                    )
                    MockRowDivider()
                    SettingsRow(
                        icon = Icons.Filled.ColorLens,
                        title = stringResource(R.string.custom_themes_title),
                        summary = stringResource(R.string.custom_themes_row_summary),
                        colors = colors,
                        onClick = {}
                    )
                    MockRowDivider()
                    SettingsRow(
                        icon = Icons.Filled.PlayArrow,
                        title = stringResource(R.string.background_playback_title),
                        summary = stringResource(R.string.background_playback_summary),
                        colors = colors,
                        onClick = {},
                        trailing = { ClintSwitch(checked = true) }
                    )
                }
                SettingsSection(colors.cardBackground) {
                    SettingsRow(
                        icon = Icons.Filled.Code,
                        title = stringResource(R.string.javascript_enabled),
                        summary = stringResource(R.string.javascript_enabled_summary),
                        colors = colors,
                        onClick = {},
                        trailing = { ClintSwitch(checked = false) }
                    )
                    MockRowDivider()
                    SettingsRow(
                        icon = Icons.Filled.Extension,
                        title = stringResource(R.string.ext_title),
                        summary = stringResource(R.string.ext_settings_summary),
                        colors = colors,
                        onClick = {}
                    )
                    MockRowDivider()
                    SettingsRow(
                        icon = Icons.Filled.Person,
                        title = stringResource(R.string.profiles_title),
                        summary = stringResource(R.string.profiles_settings_summary),
                        colors = colors,
                        onClick = {}
                    )
                }
            }
            MockGestureStrip(palette.color(ThemeSlot.NAVIGATION_BAR))
        }
        if (showDialog) MockDialogOverlay()
    }
}

@Composable
private fun MockRowDivider() {
    val divider = LocalClintColors.current.divider
    Box(Modifier.fillMaxWidth().padding(start = SettingsRowDividerInset, end = 16.dp).height(1.dp).background(divider.copy(alpha = divider.alpha * 0.6f)))
}

@Composable
private fun MockDialogOverlay() {
    val colors = LocalClintColors.current
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.popupBackground,
            modifier = Modifier.padding(horizontal = 28.dp).fillMaxWidth()
        ) {
            Column {
                Text(
                    stringResource(R.string.custom_theme_preview_dialog_title),
                    color = colors.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)
                )
                Text(
                    stringResource(R.string.custom_theme_preview_dialog_message),
                    color = colors.onSurface,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                Row(
                    Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {}) {
                        Text(
                            stringResource(R.string.action_cancel),
                            color = colors.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    TextButton(onClick = {}) {
                        Text(
                            stringResource(R.string.action_delete),
                            color = colors.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteGrid(p: CustomPalette) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 520.dp) 3 else 2
        Column {
            ThemeSlot.entries.chunked(columns).forEach { rowSlots ->
                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowSlots.forEach { slot ->
                        PaletteChip(slot, p, Modifier.weight(1f).fillMaxHeight())
                    }
                    repeat(columns - rowSlots.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteChip(slot: ThemeSlot, p: CustomPalette, modifier: Modifier) {
    val colors = LocalClintColors.current
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .clip(shape)
            .background(colors.cardBackground)
            .border(1.dp, colors.divider, shape)
    ) {
        Box(Modifier.fillMaxWidth().height(44.dp).background(p.color(slot)))
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                stringResource(slot.titleRes),
                color = colors.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                formatArgb(p[slot]),
                color = colors.secondaryText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun ContrastCard(p: CustomPalette) {
    val colors = LocalClintColors.current
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardBackground)
    ) {
        contrastPairs.forEachIndexed { index, pair ->
            if (index > 0) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
            }
            ContrastRow(p, pair)
        }
    }
}

@Composable
private fun ContrastRow(p: CustomPalette, pair: ContrastPair) {
    val colors = LocalClintColors.current
    val foreground = p.color(pair.foreground)
    val background = p.color(pair.background)
    val ratio = contrastRatio(foreground, background)
    val good = ratio >= 4.5f
    val sampleShape = RoundedCornerShape(8.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 46.dp, height = 32.dp)
                .clip(sampleShape)
                .background(background)
                .border(1.dp, colors.divider, sampleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.custom_theme_contrast_sample),
                color = foreground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            stringResource(pair.labelRes),
            color = colors.onSurface,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                String.format(Locale.getDefault(), "%.1f:1", ratio),
                color = colors.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                Modifier
                    .padding(top = 3.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (good) colors.primary else colors.colorErrorContainer)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    stringResource(
                        if (good) R.string.custom_theme_contrast_good else R.string.custom_theme_contrast_low
                    ),
                    color = if (good) colors.onPrimary else colors.colorOnErrorContainer,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
