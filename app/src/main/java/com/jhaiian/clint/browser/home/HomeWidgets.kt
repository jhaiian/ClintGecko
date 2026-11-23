package com.jhaiian.clint.browser.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.downloads.DownloadStatus
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.delay

@Composable
internal fun HomeWidgets(
    items: List<String>,
    dateText: String,
    continueTitle: String?,
    continueHost: String?,
    downloadsActive: Boolean,
    onContinue: () -> Unit,
    onDownloads: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { key ->
            when (key) {
                HOMEPAGE_WIDGET_CLOCK -> HomeClockWidget(dateText)
                HOMEPAGE_WIDGET_CONTINUE -> if (continueTitle != null) {
                    HomeInfoWidget(
                        icon = Icons.Filled.History,
                        label = stringResource(R.string.homepage_widget_continue),
                        title = continueTitle,
                        subtitle = continueHost.orEmpty(),
                        onClick = onContinue
                    )
                }
                else -> if (downloadsActive) HomeDownloadsWidget(onDownloads)
            }
        }
    }
}

@Composable
private fun HomeWidgetCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val colors = LocalClintColors.current
    val shape = RoundedCornerShape(LocalHomeCardCorner.current)
    Surface(
        color = colors.cardBackground.copy(alpha = LocalHomeCardAlpha.current),
        shape = shape,
        modifier = modifier.fillMaxWidth().clip(shape).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        content()
    }
}

@Composable
private fun HomeClockWidget(dateText: String) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            val current = System.currentTimeMillis()
            value = current
            delay(60_000L - current % 60_000L)
        }
    }
    val timeText = remember(now) { android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(now)) }
    HomeWidgetCard {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(text = timeText, color = colors.onSurface, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Text(text = dateText, color = colors.secondaryText, fontSize = 14.sp)
        }
    }
}

@Composable
private fun HomeInfoWidget(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, title: String, subtitle: String, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    HomeWidgetCard(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    text = label.uppercase(),
                    color = colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = title,
                    color = colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeDownloadsWidget(onClick: () -> Unit) {
    val colors = LocalClintColors.current
    val items by ClintDownloadManager.downloadsFlow.collectAsState()
    val working = items.filter { it.status in DownloadStatus.ACTIVELY_WORKING }
    val sized = working.filter { it.totalBytes > 0 }
    val progress = if (sized.isEmpty()) null else sized.sumOf { it.bytesDownloaded }.toFloat() / sized.sumOf { it.totalBytes }
    HomeWidgetCard(onClick = onClick) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Filled.Download, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(
                        text = stringResource(R.string.menu_downloads),
                        color = colors.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.homepage_widget_downloads_active, working.size),
                        color = colors.secondaryText,
                        fontSize = 12.sp
                    )
                }
            }
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    color = colors.primary,
                    trackColor = colors.surfaceVariant
                )
            }
        }
    }
}
