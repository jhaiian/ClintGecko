package com.jhaiian.clint.downloads
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Save

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.sheets.ActionSheetRow
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintDialogCancelFooter

data class DownloadConflictDialogRequest(
    val onAddDuplicate: () -> Unit,
    val onOverride: () -> Unit,
    val onRename: () -> Unit,
    val onUpdateLink: (() -> Unit)? = null
)

@Composable
internal fun DownloadConflictDialog(request: DownloadConflictDialogRequest, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    ClintDialog(
        title = stringResource(R.string.download_conflict_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = { ClintDialogCancelFooter(onDismiss) }
    ) {
        request.onUpdateLink?.let { onUpdateLink ->
            ActionSheetRow(androidx.compose.material.icons.Icons.Filled.Link, stringResource(R.string.download_conflict_update_link)) { onDismiss(); onUpdateLink() }
        }
        ActionSheetRow(androidx.compose.material.icons.Icons.Filled.Download, stringResource(R.string.download_conflict_add_duplicate)) { onDismiss(); request.onAddDuplicate() }
        ActionSheetRow(androidx.compose.material.icons.Icons.Filled.Save, stringResource(R.string.download_conflict_override)) { onDismiss(); request.onOverride() }
        ActionSheetRow(androidx.compose.material.icons.Icons.Filled.FormatSize, stringResource(R.string.download_conflict_rename)) { onDismiss(); request.onRename() }
    }
}
