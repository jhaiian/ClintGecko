package com.jhaiian.clint.crash
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy

import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.setup.SectionLabel
import com.jhaiian.clint.setup.SetupPrimaryButton
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.theme.ClintColors
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
private fun CrashReportCard(
    item: CrashReportItem,
    colors: ClintColors,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(color = colors.cardBackground, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(androidx.compose.material.icons.Icons.Filled.BugReport, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text(item.title, color = colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            IconButton(onClick = onCopy) {
                Icon(androidx.compose.material.icons.Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.crash_copy), tint = colors.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(androidx.compose.material.icons.Icons.Filled.Close, contentDescription = stringResource(R.string.crash_delete), tint = colors.secondaryText)
            }
        }
    }
}

@Composable
private fun CrashStepRow(number: Int, text: String, colors: ClintColors) {
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Text("$number.", color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(20.dp))
        Text(text, color = colors.secondaryText, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ClearAllConfirmDialog(hideStatusBar: Boolean, hideSystemNavigation: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    ClintDialog(
        title = stringResource(R.string.crash_clear_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.secondaryText, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.crash_clear_confirm), color = colors.colorError, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Text(
            stringResource(R.string.crash_clear_message),
            color = colors.onSurface,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun CrashDetailDialog(
    item: CrashReportItem,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val onSurfaceArgb = colors.onSurface.toArgb()
    val displayContent = if (item.content.length > MAX_CRASH_CLIP_CHARS) {
        item.content.take(MAX_CRASH_CLIP_CHARS) + "\n" + stringResource(R.string.crash_log_truncated)
    } else {
        item.content
    }

    ClintDialog(
        title = item.title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        cancelable = false,
        footer = {
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.action_delete), color = colors.colorError, fontWeight = FontWeight.Bold)
                }
                Row {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.back), color = colors.secondaryText, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onCopy) {
                        Text(stringResource(R.string.action_copy), color = colors.primary, fontWeight = FontWeight.Bold)
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
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    typeface = android.graphics.Typeface.MONOSPACE
                    setTextIsSelectable(true)
                    setPadding(64, 24, 64, 8)
                }
            },
            update = { tv ->
                tv.setTextColor(onSurfaceArgb)
                tv.text = displayContent
            }
        )
    }
}

@Composable
fun CrashReportScreen(
    state: CrashUiState,
    onOpenReport: (CrashReportItem) -> Unit,
    onCopyReport: (CrashReportItem) -> Unit,
    onDeleteReport: (CrashReportItem) -> Unit,
    onClearAllClick: () -> Unit,
    onClearAllConfirm: () -> Unit,
    onCopyTemplate: () -> Unit,
    onOpenGithub: () -> Unit
) {
    val colors = LocalClintColors.current

    SettingsScreenScaffold(
        overlay = {
            if (state.clearAllConfirmOpen) {
                ClearAllConfirmDialog(
                    hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onConfirm = onClearAllConfirm,
                    onDismiss = { state.clearAllConfirmOpen = false }
                )
            }
            state.detailReport?.let { item ->
                CrashDetailDialog(
                    item = item,
                    hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onCopy = {
                        onCopyReport(item)
                        state.detailReport = null
                    },
                    onDelete = { onDeleteReport(item) },
                    onDismiss = { state.detailReport = null }
                )
            }
        }
    ) {
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(stringResource(R.string.crash_reports_title), colors.primary, Modifier.weight(1f))
            TextButton(onClick = onClearAllClick, enabled = state.reports.isNotEmpty()) {
                Text(stringResource(R.string.crash_clear_all), color = colors.secondaryText, fontSize = 12.sp)
            }
        }

        if (!state.isLoading && state.reports.isEmpty()) {
            Text(
                stringResource(R.string.crash_no_reports),
                color = colors.secondaryText,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            )
        }

        state.reports.forEach { item ->
            CrashReportCard(
                item = item,
                colors = colors,
                onOpen = { onOpenReport(item) },
                onCopy = { onCopyReport(item) },
                onDelete = { onDeleteReport(item) }
            )
            Spacer(Modifier.height(8.dp))
        }

        HorizontalDivider(color = colors.divider, thickness = 1.dp, modifier = Modifier.padding(vertical = 20.dp))

        SectionLabel(stringResource(R.string.crash_report_title), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        Text(
            stringResource(R.string.crash_report_instructions),
            color = colors.secondaryText,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Surface(color = colors.cardBackground, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.crash_steps_title),
                    color = colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                val steps = listOf(
                    R.string.crash_step_reproduce,
                    R.string.crash_step_expand,
                    R.string.crash_step_copy,
                    R.string.crash_step_open_github,
                    R.string.crash_step_new_issue,
                    R.string.crash_step_attach,
                    R.string.crash_step_submit
                )
                steps.forEachIndexed { index, res -> CrashStepRow(index + 1, stringResource(res), colors) }
            }
        }

        Surface(color = colors.cardBackground, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.crash_template_title),
                        color = colors.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onCopyTemplate) {
                        Icon(androidx.compose.material.icons.Icons.Filled.ContentCopy, contentDescription = stringResource(R.string.crash_copy_template), tint = colors.primary)
                    }
                }
                Text(
                    state.reportTemplate,
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )
            }
        }

        SetupPrimaryButton(
            text = stringResource(R.string.crash_open_github),
            onClick = onOpenGithub,
            backgroundColor = colors.buttonBackground,
            textColor = colors.buttonTextColor,
            icon = androidx.compose.material.icons.Icons.Filled.Code,
            iconTint = colors.buttonIconTint,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}
