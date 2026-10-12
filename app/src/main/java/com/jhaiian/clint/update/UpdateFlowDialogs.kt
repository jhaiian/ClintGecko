package com.jhaiian.clint.update

import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.theme.LocalClintColors
import io.noties.markwon.Markwon

@Composable
fun UpdateFlowHost(
    state: UpdateFlowState,
    onDismiss: () -> Unit,
    onSkip: (Long) -> Unit,
    onDownload: (String, Long) -> Unit,
    onViewGithub: () -> Unit,
    onCancelDownload: () -> Unit
) {
    when (val step = state.step) {
        UpdateFlowStep.None -> Unit
        UpdateFlowStep.NoUpdate -> SimpleMessageDialog(
            title = stringResource(R.string.update_up_to_date_title),
            message = stringResource(R.string.update_up_to_date_message),
            hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
            onDismiss = onDismiss
        )
        UpdateFlowStep.CheckFailed -> SimpleMessageDialog(
            title = stringResource(R.string.update_check_failed_title),
            message = stringResource(R.string.update_check_failed_message),
            hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
            onDismiss = onDismiss
        )
        is UpdateFlowStep.Available -> UpdateAvailableDialog(
            step = step,
            hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
            onSkip = { onSkip(step.versionCode) },
            onLater = onDismiss,
            onAction = {
                if (!step.downloadUrl.isNullOrEmpty()) onDownload(step.downloadUrl, step.versionCode)
                else onViewGithub()
            }
        )
        UpdateFlowStep.Downloading -> DownloadProgressDialog(
            progress = state.download,
            hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
            onCancel = onCancelDownload
        )
    }
}

@Composable
private fun SimpleMessageDialog(title: String, message: String, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    ClintDialog(
        title = title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Text(
            message,
            color = colors.onSurface,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun UpdateAvailableDialog(
    step: UpdateFlowStep.Available,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onSkip: () -> Unit,
    onLater: () -> Unit,
    onAction: () -> Unit
) {
    val colors = LocalClintColors.current
    val channelLabel = if (step.isBeta) " (Beta)" else ""
    val onSurfaceArgb = colors.onSurface.toArgb()
    val linkArgb = colors.primary.toArgb()
    ClintDialog(
        title = stringResource(R.string.update_dialog_title, step.version, channelLabel),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onLater,
        cancelable = false,
        footer = {
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onSkip) {
                    Text(stringResource(R.string.update_dialog_skip), color = colors.secondaryText, fontWeight = FontWeight.Medium)
                }
                Row {
                    TextButton(onClick = onLater) {
                        Text(stringResource(R.string.action_later), color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                    TextButton(onClick = onAction) {
                        Text(
                            if (!step.downloadUrl.isNullOrEmpty()) stringResource(R.string.update_dialog_download)
                            else stringResource(R.string.update_dialog_view_github),
                            color = colors.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                TextView(ctx).apply {
                    setTextColor(onSurfaceArgb)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setLineSpacing(0f, 1.3f)
                }
            },
            update = { tv ->
                tv.setTextColor(onSurfaceArgb)
                tv.setLinkTextColor(linkArgb)
                Markwon.create(tv.context).setMarkdown(tv, step.changelog)
            }
        )
    }
}

@Composable
private fun DownloadProgressDialog(progress: DownloadProgressState, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onCancel: () -> Unit) {
    val colors = LocalClintColors.current
    ClintDialog(
        title = stringResource(R.string.update_download_dialog_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onCancel,
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
            Text(progress.statusText, color = colors.onSurface, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            if (progress.isIndeterminate) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.primary,
                    trackColor = colors.surfaceVariant
                )
            } else {
                LinearProgressIndicator(
                    progress = { progress.progressFraction },
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.primary,
                    trackColor = colors.surfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(progress.sizeText, color = colors.secondaryText, fontSize = 12.sp)
                Text(progress.speedText, color = colors.secondaryText, fontSize = 12.sp)
            }
        }
    }
}
