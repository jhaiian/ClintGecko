package com.jhaiian.clint.blocker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatFileSize

@Composable
fun WebsiteBlockerDownloadProgressDialog(progress: WebsiteBlockerDownloadProgress?, categoryName: String?, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onCancel: () -> Unit) {
    if (progress == null || categoryName == null) return
    val colors = LocalClintColors.current
    ClintDialog(
        title = stringResource(R.string.shared_download_dialog_title, categoryName),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = {},
        cancelable = false,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            LinearProgressIndicator(
                progress = {
                    if (progress.totalCount <= 0) 0f
                    else {
                        val itemFraction = if (progress.contentLength > 0L) (progress.bytesRead.toFloat() / progress.contentLength.toFloat()).coerceIn(0f, 1f) else 0f
                        ((progress.processedCount.toFloat() + itemFraction) / progress.totalCount.toFloat()).coerceIn(0f, 1f)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                color = colors.primary,
                trackColor = colors.surfaceVariant
            )
            if (progress.totalCount > 0) {
                Text(
                    stringResource(R.string.filter_list_update_progress_counter, progress.processedCount, progress.totalCount),
                    color = colors.onSurface, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp)
                )
            }
            Text(websiteBlockerDownloadStatusText(progress), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun websiteBlockerDownloadStatusText(progress: WebsiteBlockerDownloadProgress): String = when {
    progress.bytesRead == 0L && progress.contentLength == 0L -> stringResource(R.string.shared_download_progress_starting)
    progress.contentLength > 0L -> {
        val percent = ((progress.bytesRead * 100) / progress.contentLength).toInt()
        stringResource(R.string.shared_download_progress_known, formatFileSize(progress.bytesRead), formatFileSize(progress.contentLength), percent)
    }
    else -> stringResource(R.string.shared_download_progress_unknown, formatFileSize(progress.bytesRead))
}

@Composable
fun WebsiteBlockerCompileProgressDialog(progress: WebsiteBlockerCompileProgressUi?, hideStatusBar: Boolean, hideSystemNavigation: Boolean) {
    if (progress == null) return
    val colors = LocalClintColors.current
    ClintDialog(
        title = stringResource(R.string.website_blocker_compile_progress_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = {},
        cancelable = false,
        footer = {}
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(progress.counterText, color = colors.onSurface, fontSize = 13.sp)
            Text(progress.stageText, color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            Text(progress.elapsedText, color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun WebsiteBlockerCompileResultDialog(result: WebsiteBlockerCompileResultUi?, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    if (result == null) return
    val colors = LocalClintColors.current
    ClintDialog(
        title = result.title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                if (result.onRetry != null) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.shared_compile_action_dismiss), color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                    TextButton(onClick = { onDismiss(); result.onRetry.invoke() }) {
                        Text(stringResource(R.string.shared_compile_action_retry), color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                } else {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_ok), color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            result.rows.forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(row.label, color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(row.value, color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            result.failureDetail?.let {
                Text(it, color = colors.onSurface, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}
