package com.jhaiian.clint.bookmarks
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.SwapVert

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
fun BookmarksSortMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    sortKey: BookmarksSortKey,
    sortOrder: ListSortOrder,
    onSortByTitle: () -> Unit,
    onSortByLastVisit: () -> Unit,
    onSortByDateAdded: () -> Unit,
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
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Abc, stringResource(R.string.history_sort_by_title), sortKey == BookmarksSortKey.TITLE) {
            onDismiss(); onSortByTitle()
        }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.DateRange, stringResource(R.string.history_sort_by_last_visit), sortKey == BookmarksSortKey.LAST_VISIT) {
            onDismiss(); onSortByLastVisit()
        }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.BookmarkBorder, stringResource(R.string.bookmarks_sort_by_date_added), sortKey == BookmarksSortKey.DATE_ADDED) {
            onDismiss(); onSortByDateAdded()
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
fun BookmarksMoreMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke)
    ) {
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.FileUpload, stringResource(R.string.bookmarks_import), checked = false) {
            onDismiss(); onImportClick()
        }
        HorizontalDivider(color = colors.divider)
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.FileDownload, stringResource(R.string.bookmarks_export), checked = false) {
            onDismiss(); onExportClick()
        }
    }
}

@Composable
fun BookmarksSelectionOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onDeselectAll: () -> Unit
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke)
    ) {
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Check, stringResource(R.string.history_select_all), checked = false) {
            onDismiss(); onSelectAll()
        }
        HorizontalDivider(color = colors.divider)
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.SwapVert, stringResource(R.string.history_invert_selection), checked = false) {
            onDismiss(); onInvertSelection()
        }
        HorizontalDivider(color = colors.divider)
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Close, stringResource(R.string.history_deselect_all), checked = false) {
            onDismiss(); onDeselectAll()
        }
    }
}

@Composable
fun BookmarksSelectionItemOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onMoveTo: () -> Unit,
    showRename: Boolean,
    onRename: () -> Unit
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke)
    ) {
        ListMenuItem(androidx.compose.material.icons.Icons.AutoMirrored.Filled.DriveFileMove, stringResource(R.string.bookmarks_move_to), checked = false) {
            onDismiss(); onMoveTo()
        }
        if (showRename) {
            HorizontalDivider(color = colors.divider)
            ListMenuItem(androidx.compose.material.icons.Icons.Filled.Edit, stringResource(R.string.bookmarks_rename_folder), checked = false) {
                onDismiss(); onRename()
            }
        }
    }
}
