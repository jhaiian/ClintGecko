package com.jhaiian.clint.downloads

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.launch

@Composable
internal fun DownloadLogViewer(
    logId: Int,
    title: String,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lines by remember { mutableStateOf<List<String>?>(null) }
    var problemsOnly by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val shareTitle = stringResource(R.string.download_log_share_title)

    LaunchedEffect(logId, reload) {
        lines = DownloadLog.readLines(context, logId)
    }
    val visible = remember(lines, problemsOnly) {
        val all = lines.orEmpty()
        if (!problemsOnly) all else all.filter { it.contains("  WARN   ") || it.contains("  ERROR  ") }
    }
    LaunchedEffect(visible.size) {
        if (visible.isNotEmpty()) listState.scrollToItem(visible.lastIndex)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        com.jhaiian.clint.ui.ClintDialogStatusBarEffect(hideStatusBar, hideSystemNavigation)
        Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_cancel), tint = colors.onSurface)
                    }
                    Text(
                        title, color = colors.onSurface, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                    IconButton(onClick = {
                        scope.launch {
                            val file = DownloadLog.export(context, logId) ?: return@launch
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, file.name)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching { context.startActivity(Intent.createChooser(send, shareTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.download_log_share), tint = colors.iconTint)
                    }
                    IconButton(onClick = {
                        scope.launch {
                            DownloadLog.clear(logId)
                            reload++
                        }
                    }) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = stringResource(R.string.download_log_clear), tint = colors.iconTint)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChipPill(stringResource(R.string.download_log_filter_all), lines?.size, null, !problemsOnly) { problemsOnly = false }
                    FilterChipPill(stringResource(R.string.download_log_filter_problems), null, null, problemsOnly) { problemsOnly = true }
                }
                if (!DownloadLog.enabled) {
                    Text(
                        stringResource(R.string.download_log_disabled_notice),
                        color = colors.secondaryText, fontSize = 13.sp, lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp)).background(colors.primary.copy(alpha = 0.08f)).padding(12.dp)
                    )
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        lines == null -> Unit
                        visible.isEmpty() -> Text(
                            stringResource(R.string.download_log_empty),
                            color = colors.secondaryText, fontSize = 14.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(32.dp)
                        )
                        else -> LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            itemsIndexed(visible) { _, line ->
                                val color = when {
                                    line.contains("  ERROR  ") -> colors.colorError
                                    line.contains("  WARN   ") -> colors.primary
                                    line.contains("  DEBUG  ") -> colors.secondaryText
                                    else -> colors.onSurface
                                }
                                val isHeading = line.contains("──── ")
                                Text(
                                    line,
                                    color = color,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp, lineHeight = 15.sp,
                                    fontWeight = if (isHeading) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = if (isHeading) 6.dp else 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
