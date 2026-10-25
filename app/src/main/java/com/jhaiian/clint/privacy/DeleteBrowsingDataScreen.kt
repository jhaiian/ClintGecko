package com.jhaiian.clint.privacy

import android.text.format.Formatter
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintCheckbox
import com.jhaiian.clint.ui.listscreen.ListMenuItem
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors

private val TileOuterRadius = 24.dp
private val TileInnerRadius = 6.dp

@Composable
fun DeleteBrowsingDataScreen(
    range: BrowsingDataRange,
    selection: Set<BrowsingDataType>,
    summary: BrowsingDataSummary?,
    isDeleting: Boolean,
    maxContentWidth: Dp?,
    onBack: () -> Unit,
    onRangeChange: (BrowsingDataRange) -> Unit,
    onToggle: (BrowsingDataType) -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalClintColors.current
    val tabsAvailable = (summary?.openTabs ?: 0) > 0
    val effectiveSelection = if (tabsAvailable) selection else selection - BrowsingDataType.TABS
    val types = BrowsingDataType.entries

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Surface(color = colors.background, modifier = Modifier.statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.onSurface)
                    }
                    Text(
                        stringResource(R.string.delete_data_title),
                        color = colors.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .then(if (maxContentWidth != null) Modifier.widthIn(max = maxContentWidth) else Modifier)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    TimeRangeRow(range = range, enabled = !isDeleting, onRangeChange = onRangeChange)
                    Spacer(Modifier.height(12.dp))
                    types.forEachIndexed { index, type ->
                        val enabled = !isDeleting && (type != BrowsingDataType.TABS || tabsAvailable)
                        DataTypeRow(
                            type = type,
                            summary = summary,
                            checked = type in effectiveSelection,
                            enabled = enabled,
                            shape = RoundedCornerShape(
                                topStart = if (index == 0) TileOuterRadius else TileInnerRadius,
                                topEnd = if (index == 0) TileOuterRadius else TileInnerRadius,
                                bottomStart = if (index == types.lastIndex) TileOuterRadius else TileInnerRadius,
                                bottomEnd = if (index == types.lastIndex) TileOuterRadius else TileInnerRadius
                            ),
                            onToggle = { onToggle(type) }
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    val rangeLimited = range != BrowsingDataRange.ALL_TIME && effectiveSelection.any {
                        it == BrowsingDataType.COOKIES || it == BrowsingDataType.CACHE || it == BrowsingDataType.SITE_SETTINGS
                    }
                    if (rangeLimited) {
                        InfoNote(stringResource(R.string.delete_data_all_time_note))
                        Spacer(Modifier.height(10.dp))
                    }
                    InfoNote(stringResource(R.string.delete_data_footer))
                    Spacer(Modifier.height(24.dp))
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Button(
                    onClick = onDelete,
                    enabled = effectiveSelection.isNotEmpty() && !isDeleting,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                        disabledContainerColor = colors.primary.copy(alpha = 0.3f),
                        disabledContentColor = colors.onPrimary.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.heightIn(min = 52.dp)
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            color = colors.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(10.dp))
                    }
                    Text(
                        stringResource(if (isDeleting) R.string.delete_data_deleting else R.string.delete_data_action),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeRangeRow(range: BrowsingDataRange, enabled: Boolean, onRangeChange: (BrowsingDataRange) -> Unit) {
    val colors = LocalClintColors.current
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 4.dp)
    ) {
        Icon(Icons.Filled.Schedule, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(22.dp))
        Text(
            stringResource(R.string.delete_data_time_range),
            color = colors.onSurface,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f).padding(start = 14.dp)
        )
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.cardBackground)
                    .clickable(enabled = enabled) { menuOpen = true }
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Text(
                    stringResource(rangeLabelRes(range)),
                    color = colors.primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = colors.primary)
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                shape = PopupShape,
                containerColor = colors.popupBackground,
                border = BorderStroke(1.dp, colors.popupStroke)
            ) {
                BrowsingDataRange.entries.forEach { option ->
                    ListMenuItem(Icons.Filled.Schedule, stringResource(rangeLabelRes(option)), checked = option == range) {
                        menuOpen = false
                        onRangeChange(option)
                    }
                }
            }
        }
    }
}

@Composable
private fun DataTypeRow(
    type: BrowsingDataType,
    summary: BrowsingDataSummary?,
    checked: Boolean,
    enabled: Boolean,
    shape: Shape,
    onToggle: () -> Unit
) {
    val colors = LocalClintColors.current
    val background by animateColorAsState(
        if (checked && enabled) androidx.compose.ui.graphics.lerp(colors.cardBackground, colors.primary, 0.08f) else colors.cardBackground,
        label = "deleteDataRowBackground"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(shape)
            .background(background)
            .clickable(enabled = enabled, onClick = onToggle)
            .heightIn(min = 76.dp)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
            .alpha(if (enabled) 1f else 0.45f)
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(typeIcon(type), contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp), verticalArrangement = Arrangement.Center) {
            Text(stringResource(typeTitleRes(type)), color = colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(
                typeSummary(type, summary),
                color = colors.secondaryText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        ClintCheckbox(checked = checked, onCheckedChange = { if (enabled) onToggle() })
    }
}

@Composable
private fun InfoNote(text: String) {
    val colors = LocalClintColors.current
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.padding(top = 1.dp).size(18.dp))
        Text(
            text,
            color = colors.secondaryText,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

@Composable
private fun typeSummary(type: BrowsingDataType, summary: BrowsingDataSummary?): String {
    val context = LocalContext.current
    if (summary == null) return stringResource(R.string.delete_data_calculating)
    return when (type) {
        BrowsingDataType.HISTORY -> when {
            summary.historyCount == 0 -> stringResource(R.string.delete_data_none)
            summary.historyFirstSite == null -> pluralStringResource(R.plurals.delete_data_history_items, summary.historyCount, summary.historyCount)
            summary.historyOtherSites == 0 -> stringResource(R.string.delete_data_history_from_site, summary.historyFirstSite)
            else -> pluralStringResource(
                R.plurals.delete_data_history_from_sites,
                summary.historyOtherSites,
                summary.historyFirstSite,
                summary.historyOtherSites
            )
        }
        BrowsingDataType.COOKIES -> stringResource(R.string.delete_data_cookies_summary)
        BrowsingDataType.CACHE -> if (summary.cacheBytes <= 0L) {
            stringResource(R.string.delete_data_none)
        } else {
            stringResource(R.string.delete_data_cache_summary, Formatter.formatShortFileSize(context, summary.cacheBytes))
        }
        BrowsingDataType.TABS -> if (summary.openTabs == 0) {
            stringResource(R.string.delete_data_none)
        } else {
            pluralStringResource(R.plurals.delete_data_tabs_summary, summary.openTabs, summary.openTabs)
        }
        BrowsingDataType.SITE_SETTINGS -> if (summary.siteSettingsCount == 0) {
            stringResource(R.string.delete_data_none)
        } else {
            pluralStringResource(R.plurals.delete_data_site_settings_summary, summary.siteSettingsCount, summary.siteSettingsCount)
        }
    }
}

private fun typeIcon(type: BrowsingDataType): ImageVector = when (type) {
    BrowsingDataType.HISTORY -> Icons.Filled.History
    BrowsingDataType.COOKIES -> Icons.Filled.Cookie
    BrowsingDataType.CACHE -> Icons.Filled.PhotoLibrary
    BrowsingDataType.TABS -> Icons.Filled.Tab
    BrowsingDataType.SITE_SETTINGS -> Icons.Filled.Tune
}

private fun typeTitleRes(type: BrowsingDataType): Int = when (type) {
    BrowsingDataType.HISTORY -> R.string.delete_data_history
    BrowsingDataType.COOKIES -> R.string.delete_data_cookies
    BrowsingDataType.CACHE -> R.string.delete_data_cache
    BrowsingDataType.TABS -> R.string.delete_data_tabs
    BrowsingDataType.SITE_SETTINGS -> R.string.delete_data_site_settings
}

private fun rangeLabelRes(range: BrowsingDataRange): Int = when (range) {
    BrowsingDataRange.LAST_HOUR -> R.string.delete_data_range_hour
    BrowsingDataRange.LAST_DAY -> R.string.delete_data_range_day
    BrowsingDataRange.LAST_WEEK -> R.string.delete_data_range_week
    BrowsingDataRange.LAST_FOUR_WEEKS -> R.string.delete_data_range_four_weeks
    BrowsingDataRange.ALL_TIME -> R.string.delete_data_range_all_time
}
