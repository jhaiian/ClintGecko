package com.jhaiian.clint.downloads
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune

import com.jhaiian.clint.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.ui.listscreen.ListMenuItem
import com.jhaiian.clint.ui.listscreen.ListSortOrder
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun DownloadsSortMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    sortKey: DownloadSortKey,
    sortOrder: ListSortOrder,
    onSortByName: () -> Unit,
    onSortByDate: () -> Unit,
    onSortBySize: () -> Unit,
    onSortByStatus: () -> Unit,
    onSortAscending: () -> Unit,
    onSortDescending: () -> Unit
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke)
    ) {
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Abc, stringResource(R.string.downloads_sort_by_name), sortKey == DownloadSortKey.NAME) {
            onDismiss(); onSortByName()
        }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.DateRange, stringResource(R.string.downloads_sort_by_date), sortKey == DownloadSortKey.DATE) {
            onDismiss(); onSortByDate()
        }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.FormatSize, stringResource(R.string.downloads_sort_by_size), sortKey == DownloadSortKey.SIZE) {
            onDismiss(); onSortBySize()
        }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.SwapVert, stringResource(R.string.downloads_sort_by_status), sortKey == DownloadSortKey.STATUS) {
            onDismiss(); onSortByStatus()
        }
        HorizontalDivider(color = colors.divider)
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.ArrowUpward, stringResource(R.string.history_sort_ascending), sortOrder == ListSortOrder.ASCENDING) {
            onDismiss(); onSortAscending()
        }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.ArrowDownward, stringResource(R.string.history_sort_descending), sortOrder == ListSortOrder.DESCENDING) {
            onDismiss(); onSortDescending()
        }
    }
}

@Composable
fun DownloadsOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onResumeAll: () -> Unit,
    onPauseAll: () -> Unit
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke)
    ) {
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.PlayArrow, stringResource(R.string.downloads_resume_all), false) { onDismiss(); onResumeAll() }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Pause, stringResource(R.string.downloads_pause_all), false) { onDismiss(); onPauseAll() }
    }
}

data class DownloadItemActions(
    val onOpen: (DownloadItem) -> Unit,
    val onShare: (DownloadItem) -> Unit,
    val onOpenFolder: (DownloadItem) -> Unit,
    val onRename: (DownloadItem) -> Unit,
    val onRedownload: (DownloadItem) -> Unit,
    val onRedownloadOptions: (DownloadItem) -> Unit,
    val onChangeSettings: (DownloadItem) -> Unit,
    val onUpdateLink: (DownloadItem) -> Unit,
    val onUpdateLinkInBrowser: (DownloadItem) -> Unit,
    val onRemove: (DownloadItem) -> Unit,
    val onCopyLink: (DownloadItem) -> Unit,
    val onCopyFilename: (DownloadItem) -> Unit,
    val onCopyPath: (DownloadItem) -> Unit,
    val onProperties: (DownloadItem) -> Unit
)

@Composable
fun DownloadItemOptionsMenu(
    expanded: Boolean,
    item: DownloadItem,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onOpenFolder: () -> Unit,
    onRename: () -> Unit,
    onRedownload: () -> Unit,
    onRedownloadOptions: () -> Unit,
    onChangeSettings: () -> Unit,
    onUpdateLink: () -> Unit,
    onUpdateLinkInBrowser: () -> Unit,
    onRemove: () -> Unit,
    onCopyLink: () -> Unit,
    onCopyFilename: () -> Unit,
    onCopyPath: () -> Unit,
    onProperties: () -> Unit
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke)
    ) {
        ListMenuItem(androidx.compose.material.icons.Icons.AutoMirrored.Filled.OpenInNew, stringResource(R.string.action_open), false) { onDismiss(); onOpen() }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Share, stringResource(R.string.download_menu_share), false) { onDismiss(); onShare() }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Folder, stringResource(R.string.download_menu_open_folder), false) { onDismiss(); onOpenFolder() }
        if (item.status == DownloadStatus.COMPLETE) {
            ListMenuItem(androidx.compose.material.icons.Icons.Filled.Edit, stringResource(R.string.download_menu_rename), false) { onDismiss(); onRename() }
        }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Refresh, stringResource(R.string.download_menu_redownload), false) { onDismiss(); onRedownload() }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Download, stringResource(R.string.download_menu_redownload_options), false) { onDismiss(); onRedownloadOptions() }
        if (item.status in DownloadStatus.NOT_FINISHED) {
            ListMenuItem(androidx.compose.material.icons.Icons.Filled.Tune, stringResource(R.string.download_menu_change_settings), false) { onDismiss(); onChangeSettings() }
        }
        if (item.status != DownloadStatus.COMPLETE) {
            ListMenuItem(androidx.compose.material.icons.Icons.Filled.Link, stringResource(R.string.download_menu_update_link), false) { onDismiss(); onUpdateLink() }
            ListMenuItem(androidx.compose.material.icons.Icons.Filled.Language, stringResource(R.string.download_menu_update_link_in_browser), false) { onDismiss(); onUpdateLinkInBrowser() }
        }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Delete, stringResource(R.string.download_menu_remove), false) { onDismiss(); onRemove() }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.ContentCopy, stringResource(R.string.download_menu_copy_link), false) { onDismiss(); onCopyLink() }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.ContentCopy, stringResource(R.string.download_menu_copy_filename), false) { onDismiss(); onCopyFilename() }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.ContentCopy, stringResource(R.string.download_menu_copy_path), false) { onDismiss(); onCopyPath() }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Info, stringResource(R.string.download_menu_properties), false) { onDismiss(); onProperties() }
    }
}

@Composable
fun DownloadsMultiItemOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRedownload: () -> Unit,
    onRemove: () -> Unit,
    onCopyLink: () -> Unit,
    onCopyFilename: () -> Unit,
    onCopyPath: () -> Unit
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke)
    ) {
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Refresh, stringResource(R.string.download_menu_redownload), false) { onDismiss(); onRedownload() }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Delete, stringResource(R.string.download_menu_remove), false) { onDismiss(); onRemove() }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.ContentCopy, stringResource(R.string.download_menu_copy_link), false) { onDismiss(); onCopyLink() }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.ContentCopy, stringResource(R.string.download_menu_copy_filename), false) { onDismiss(); onCopyFilename() }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.ContentCopy, stringResource(R.string.download_menu_copy_path), false) { onDismiss(); onCopyPath() }
    }
}
