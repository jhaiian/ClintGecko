package com.jhaiian.clint.ui.status

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
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
import com.jhaiian.clint.ui.listscreen.ListMenuItem
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors

data class UpdateProgressUi(
    val title: String,
    val totalCount: Int,
    val processedCount: Int = 0,
    val statusText: String = "",
    val currentListName: String = ""
)

data class UpdateResultUi(
    val title: String,
    val message: String,
    val onCompile: (() -> Unit)? = null
)

fun formatElapsedSeconds(totalSeconds: Long): String {
    val m = totalSeconds / 60L
    val s = totalSeconds % 60L
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}

fun formatCompactCount(count: Long): String = when {
    count >= 1_000_000L -> "${count / 1_000_000L}M"
    count >= 1_000L -> "${count / 1_000L}k"
    else -> count.toString()
}

@Composable
fun UpdateProgressDialog(progress: UpdateProgressUi?, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onCancel: () -> Unit) {
    if (progress == null) return
    val colors = LocalClintColors.current
    ClintDialog(
        title = progress.title,
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
                progress = { progress.processedCount.toFloat() / progress.totalCount.toFloat().coerceAtLeast(1f) },
                modifier = Modifier.fillMaxWidth(),
                color = colors.primary,
                trackColor = colors.surfaceVariant
            )
            Text(
                stringResource(R.string.filter_list_update_progress_counter, progress.processedCount, progress.totalCount),
                color = colors.onSurface, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp)
            )
            Text(progress.statusText, color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            if (progress.currentListName.isNotEmpty()) {
                Text(progress.currentListName, color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
fun UpdateResultDialog(result: UpdateResultUi?, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    if (result == null) return
    val colors = LocalClintColors.current
    ClintDialog(
        title = result.title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                if (result.onCompile != null) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                    TextButton(onClick = { onDismiss(); result.onCompile.invoke() }) {
                        Text(stringResource(R.string.shared_back_dialog_compile), color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                } else {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_ok), color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    ) {
        Text(result.message, color = colors.onSurface, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@Composable
fun FilterListItemOptionsMenu(
    expanded: Boolean,
    isLocal: Boolean,
    onDismiss: () -> Unit,
    onCheckUpdate: () -> Unit,
    onForceUpdate: () -> Unit,
    onRemove: () -> Unit,
    onCopyName: () -> Unit,
    onCopyLink: () -> Unit,
    onShareLink: () -> Unit
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded, onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke)
    ) {
        if (!isLocal) {
            ListMenuItem(Icons.Filled.Update, stringResource(R.string.filter_list_menu_check_update), false) { onDismiss(); onCheckUpdate() }
            ListMenuItem(Icons.Filled.RestartAlt, stringResource(R.string.filter_list_menu_force_update), false) { onDismiss(); onForceUpdate() }
            HorizontalDivider(color = colors.divider)
        }
        ListMenuItem(Icons.Filled.Delete, stringResource(R.string.filter_list_menu_remove), false) { onDismiss(); onRemove() }
        HorizontalDivider(color = colors.divider)
        ListMenuItem(Icons.Filled.ContentCopy, stringResource(R.string.filter_list_menu_copy_name), false) { onDismiss(); onCopyName() }
        if (!isLocal) {
            ListMenuItem(Icons.Filled.ContentCopy, stringResource(R.string.filter_list_menu_copy_link), false) { onDismiss(); onCopyLink() }
            ListMenuItem(Icons.Filled.Share, stringResource(R.string.filter_list_menu_share_link), false) { onDismiss(); onShareLink() }
        }
    }
}
