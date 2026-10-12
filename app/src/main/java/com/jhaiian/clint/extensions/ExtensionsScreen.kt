package com.jhaiian.clint.extensions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintDialogActionFooter
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.theme.LocalClintColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExtensionsScreen(
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onExit: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onPickFile: () -> Unit,
    onGetAddons: () -> Unit
) {
    val colors = LocalClintColors.current
    val extensions = ClintExtensions.extensions
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var showLink by remember { mutableStateOf(false) }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val onCount = extensions.count { it.status == ExtensionStatus.ON }
    val offCount = extensions.size - onCount
    val filtered = when (filter) {
        1 -> extensions.filter { it.status == ExtensionStatus.ON }
        2 -> extensions.filter { it.status != ExtensionStatus.ON }
        else -> extensions.toList()
    }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Surface(color = colors.surface, shadowElevation = 4.dp, modifier = Modifier.statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().height(56.dp).padding(start = 4.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.onSurface)
                    }
                    Text(
                        stringResource(R.string.ext_title),
                        color = colors.onSurface,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                    if (ClintExtensions.checkingAll) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 14.dp).size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = colors.primary
                        )
                    } else {
                        IconButton(onClick = { ClintExtensions.checkAllUpdates() }, enabled = extensions.isNotEmpty()) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.ext_check_updates),
                                tint = if (extensions.isNotEmpty()) colors.iconTint else colors.secondaryText.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 330.dp),
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 1200.dp),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 20.dp + navBottom),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        HeroCard(
                            onCount = onCount,
                            offCount = offCount,
                            onGetAddons = onGetAddons,
                            onPickFile = onPickFile,
                            onLink = { showLink = true }
                        )
                    }
                    if (extensions.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterPill(stringResource(R.string.ext_filter_all), extensions.size, filter == 0) { filter = 0 }
                                FilterPill(stringResource(R.string.ext_filter_on), onCount, filter == 1) { filter = 1 }
                                FilterPill(stringResource(R.string.ext_filter_off), offCount, filter == 2) { filter = 2 }
                            }
                        }
                    }
                    if (!ClintExtensions.loaded) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = colors.primary, strokeWidth = 3.dp)
                            }
                        }
                    } else if (extensions.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) { EmptyState(onGetAddons) }
                    } else if (filtered.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                stringResource(R.string.ext_empty_filtered),
                                color = colors.secondaryText,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp)
                            )
                        }
                    }
                    items(filtered, key = { it.id }) { info ->
                        ExtensionCard(info, onClick = { onOpenDetail(info.id) })
                    }
                }
            }
        }
    }

    if (showLink) {
        InstallLinkDialog(
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onDismiss = { showLink = false },
            onInstall = { url ->
                showLink = false
                ClintExtensions.install(url)
            }
        )
    }
}

@Composable
private fun HeroCard(
    onCount: Int,
    offCount: Int,
    onGetAddons: () -> Unit,
    onPickFile: () -> Unit,
    onLink: () -> Unit
) {
    val colors = LocalClintColors.current
    val start = lerp(colors.primary, Color.Black, 0.32f)
    val end = lerp(colors.primary, Color.Black, 0.66f)
    val deep = lerp(colors.primary, Color.Black, 0.45f)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(start, end)))
    ) {
        Icon(
            Icons.Filled.Extension,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 36.dp, y = (-26).dp)
                .size(170.dp)
                .rotate(-14f)
                .alpha(0.10f)
        )
        Icon(
            Icons.Filled.Extension,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = 34.dp)
                .size(110.dp)
                .rotate(18f)
                .alpha(0.06f)
        )
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                stringResource(R.string.ext_title),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (onCount + offCount == 0) stringResource(R.string.ext_hero_tagline)
                else stringResource(R.string.ext_hero_summary, onCount, offCount),
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .clickable(onClick = onGetAddons)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.Storefront, contentDescription = null, tint = deep, modifier = Modifier.size(22.dp))
                Text(
                    stringResource(R.string.ext_get_addons),
                    color = deep,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroSecondary(Icons.Filled.Folder, stringResource(R.string.ext_install_file), onPickFile, Modifier.weight(1f))
                HeroSecondary(Icons.Filled.Link, stringResource(R.string.ext_install_link), onLink, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroSecondary(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Text(
            label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun FilterPill(label: String, count: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (selected) colors.primary else colors.cardBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = if (selected) colors.onPrimary else colors.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            count.toString(),
            color = if (selected) colors.onPrimary.copy(alpha = 0.8f) else colors.secondaryText,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 7.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExtensionCard(info: ExtensionInfo, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    val busy = info.id in ClintExtensions.busyIds
    val dimmed = info.status != ExtensionStatus.ON
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.cardBackground,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick)
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(riskColor(info.risk).copy(alpha = if (dimmed) 0.35f else 1f))
            )
            Column(Modifier.weight(1f).padding(start = 14.dp, end = 8.dp, top = 14.dp, bottom = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExtensionIconTile(info, 52.dp, Modifier.alpha(if (dimmed) 0.55f else 1f))
                    Column(Modifier.weight(1f).padding(start = 14.dp, end = 8.dp)) {
                        Text(
                            info.name,
                            color = colors.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            buildString {
                                append(stringResource(R.string.ext_version_fmt, info.version))
                                info.author?.let { append(" · ").append(stringResource(R.string.ext_by_fmt, it)) }
                            },
                            color = colors.secondaryText,
                            fontSize = 12.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Box(
                        Modifier
                            .size(width = 64.dp, height = 48.dp)
                            .clickable(enabled = info.canToggle && !busy) { ClintExtensions.setEnabled(info, !info.enabled) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = colors.primary)
                        } else {
                            ClintSwitch(checked = info.enabled)
                        }
                    }
                }
                if (info.description.isNotBlank()) {
                    Text(
                        info.description,
                        color = colors.secondaryText,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 12.dp, end = 8.dp)
                    )
                }
                FlowRow(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusPill(riskLabel(info.risk), riskColor(info.risk))
                    statusText(info.status)?.let { text ->
                        val tint = when (info.status) {
                            ExtensionStatus.OFF -> colors.secondaryText
                            else -> colors.colorError
                        }
                        StatusPill(text, tint, dot = false)
                    }
                    if (info.allowedInPrivate) {
                        StatusPill(stringResource(R.string.ext_private_on), colors.primary, dot = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onGetAddons: () -> Unit) {
    val colors = LocalClintColors.current
    Column(
        Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(132.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.07f)))
            Box(Modifier.size(96.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.12f)))
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Extension, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(32.dp))
            }
        }
        Text(
            stringResource(R.string.ext_empty_title),
            color = colors.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            stringResource(R.string.ext_empty_message),
            color = colors.secondaryText,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, start = 12.dp, end = 12.dp)
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun InstallLinkDialog(
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onInstall: (String) -> Unit
) {
    val colors = LocalClintColors.current
    var url by remember { mutableStateOf("") }
    val valid = url.trim().startsWith("https://", true) || url.trim().startsWith("http://", true)
    ClintDialog(
        title = stringResource(R.string.ext_link_title),
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            ClintDialogActionFooter(
                onCancel = onDismiss,
                positiveLabel = stringResource(R.string.ext_link_action),
                onPositive = { onInstall(url.trim()) },
                positiveEnabled = valid
            )
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                stringResource(R.string.ext_link_message),
                color = colors.secondaryText,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            ClintOutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.ext_link_hint)) },
                singleLine = true
            )
        }
    }
}
