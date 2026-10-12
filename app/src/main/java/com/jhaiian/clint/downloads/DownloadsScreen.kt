package com.jhaiian.clint.downloads
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import com.jhaiian.clint.R

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.ClintCheckbox
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.listscreen.ClintSearchField
import com.jhaiian.clint.ui.listscreen.ListSortOrder
import com.jhaiian.clint.ui.listscreen.SelectionOptionsMenu
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun DownloadsScreen(
    state: DownloadsUiState,
    allItems: List<DownloadItem>,
    tick: Long,
    maxContentWidth: Dp?,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onExit: () -> Unit,
    onOpenItem: (DownloadItem) -> Unit,
    onDownloadSettingsClick: () -> Unit,
    onPause: (Int) -> Unit,
    onResume: (Int) -> Unit,
    onRetry: (Int) -> Unit,
    itemActions: DownloadItemActions,
    onAddClick: () -> Unit,
    onDeleteSelectedClick: (List<DownloadItem>) -> Unit,
    onDeleteConfirmed: (List<DownloadItem>, Boolean) -> Unit,
    onMultiRedownload: (List<DownloadItem>) -> Unit,
    onMultiCopyLink: (List<DownloadItem>) -> Unit,
    onMultiCopyFilename: (List<DownloadItem>) -> Unit,
    onMultiCopyPath: (List<DownloadItem>) -> Unit,
    onSubmitManualDownload: (ManualDownloadSubmission, onDismiss: () -> Unit, onRename: () -> Unit) -> Unit
) {
    val colors = LocalClintColors.current

    val tabItems = remember(allItems, state.sortKey, state.sortOrder, state.searchQuery) {
        filterAndSortDownloads(allItems, "", state.sortKey, state.sortOrder)
    }
    val displayed = remember(allItems, state.currentTab, state.searchQuery, state.sortKey, state.sortOrder) {
        val forTab = if (state.currentTab == DownloadsTabType.ALL) tabItems else tabItems.filter { itemMatchesTab(it, state.currentTab) }
        if (state.searchQuery.isBlank()) forTab else {
            val q = state.searchQuery.trim().lowercase()
            forTab.filter { it.filename.lowercase().contains(q) }
        }
    }
    val selectedItems = remember(allItems, state.selectedIds) { allItems.filter { it.id in state.selectedIds } }
    val listState = rememberLazyListState()
    val showDeleteFab = state.isInSelectionMode && state.selectedIds.isNotEmpty()

    fun handleBack() {
        when {
            state.isSearchMode -> { state.isSearchMode = false; state.searchQuery = "" }
            state.isInSelectionMode -> state.exitSelectionMode()
            else -> onExit()
        }
    }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            DownloadsToolbar(
                state = state,
                selectedItems = selectedItems,
                itemActions = itemActions,
                onBack = ::handleBack,
                onDownloadSettingsClick = onDownloadSettingsClick,
                onSelectAll = { state.selectAll(displayed) },
                onInvertSelection = { state.invertSelection(displayed) },
                onMultiRedownload = { onMultiRedownload(selectedItems) },
                onMultiRemove = { onDeleteSelectedClick(selectedItems) },
                onMultiCopyLink = { onMultiCopyLink(selectedItems) },
                onMultiCopyFilename = { onMultiCopyFilename(selectedItems) },
                onMultiCopyPath = { onMultiCopyPath(selectedItems) },
                onResumeAll = { allItems.filter { it.status == DownloadStatus.PAUSED }.forEach { onResume(it.id) } },
                onPauseAll = { allItems.filter { it.status in DownloadStatus.ACTIVELY_WORKING || it.status == DownloadStatus.QUEUED }.forEach { onPause(it.id) } }
            )

            if (!state.isSearchMode) {
                PrimaryTabRow(
                    selectedTabIndex = DownloadsTabType.values().indexOf(state.currentTab),
                    containerColor = colors.surface,
                    contentColor = colors.primary,
                    divider = {}
                ) {
                    DownloadsTabType.values().forEach { type ->
                        val count = if (type == DownloadsTabType.ALL) tabItems.size else tabItems.count { itemMatchesTab(it, type) }
                        Tab(
                            selected = state.currentTab == type,
                            onClick = { state.currentTab = type },
                            text = {
                                Text(
                                    stringResource(R.string.downloads_tab_label_format, tabLabelFor(type), count),
                                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (displayed.isEmpty()) {
                    Text(
                        stringResource(R.string.downloads_empty),
                        color = colors.secondaryText, fontSize = 15.sp,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp)
                    )
                } else {
                    AdaptiveWidthContainer(maxContentWidth) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp)
                        ) {
                            items(displayed, key = { it.id }) { item ->
                                DownloadRow(
                                    item = item,
                                    tick = tick,
                                    isSelected = item.id in state.selectedIds,
                                    isInSelectionMode = state.isInSelectionMode,
                                    onClick = {
                                        if (state.isInSelectionMode) state.toggleSelection(item.id) else onOpenItem(item)
                                    },
                                    onLongClick = {
                                        if (!state.isInSelectionMode) {
                                            state.enterSelectionWith(item.id)
                                        } else if (item.id !in state.selectedIds) {
                                            state.selectedIds = state.selectedIds + item.id
                                        }
                                    },
                                    onPause = onPause, onResume = onResume, onRetry = onRetry,
                                    itemActions = itemActions
                                )
                            }
                        }
                    }
                }

                if (!state.isInSelectionMode) {
                    FloatingActionButton(
                        onClick = onAddClick,
                        containerColor = colors.buttonBackground, contentColor = colors.buttonIconTint,
                        modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 24.dp, end = 20.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Add, contentDescription = stringResource(R.string.download_manual_fab_desc))
                    }
                }
                if (showDeleteFab) {
                    FloatingActionButton(
                        onClick = { onDeleteSelectedClick(selectedItems) },
                        containerColor = colors.buttonBackground, contentColor = colors.buttonIconTint,
                        modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 24.dp, end = 20.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Delete, contentDescription = stringResource(R.string.downloads_delete_selected_desc))
                    }
                }
            }
        }
    }

    val pendingDelete = state.deleteConfirmItems
    if (pendingDelete != null) {
        DownloadsDeleteConfirmDialog(
            count = pendingDelete.size,
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            onDismiss = { state.deleteConfirmItems = null },
            onConfirm = { deleteFromStorage ->
                state.deleteConfirmItems = null
                onDeleteConfirmed(pendingDelete, deleteFromStorage)
            }
        )
    }

    val progress = state.deleteProgress
    if (progress != null) {
        DownloadsDeleteProgressDialog(progress, hideStatusBar, hideSystemNavigation)
    }

    state.propertiesItem?.let { snapshot ->
        val liveItem = allItems.firstOrNull { it.id == snapshot.id }
        if (liveItem == null) {
            LaunchedEffect(snapshot.id) { state.propertiesItem = null }
        } else {
            DownloadPropertiesDialog(
                item = liveItem,
                tick = tick,
                hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                onDismiss = { state.propertiesItem = null },
                onShare = { itemActions.onShare(it) },
                onOpen = { itemActions.onOpen(it) }
            )
        }
    }
    state.renameItem?.let { item ->
        DownloadRenameDialog(item = item, hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = { state.renameItem = null })
    }
    state.changeSettingsItem?.let { item ->
        DownloadChangeSettingsDialog(item = item, hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = { state.changeSettingsItem = null })
    }
    state.updateLinkItem?.let { item ->
        DownloadUpdateLinkDialog(item = item, hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = { state.updateLinkItem = null })
    }
    if (state.manualDownloadDialogOpen) {
        DownloadManualDialog(
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            prefillUrl = state.manualDownloadPrefillUrl,
            onDismiss = { state.manualDownloadDialogOpen = false; state.manualDownloadPrefillUrl = null },
            onSubmit = onSubmitManualDownload
        )
    }
}

@Composable
private fun DownloadsToolbar(
    state: DownloadsUiState,
    selectedItems: List<DownloadItem>,
    itemActions: DownloadItemActions,
    onBack: () -> Unit,
    onDownloadSettingsClick: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onMultiRedownload: () -> Unit,
    onMultiRemove: () -> Unit,
    onMultiCopyLink: () -> Unit,
    onMultiCopyFilename: () -> Unit,
    onMultiCopyPath: () -> Unit,
    onResumeAll: () -> Unit,
    onPauseAll: () -> Unit
) {
    val colors = LocalClintColors.current
    val showToolbarIcons = !state.isInSelectionMode && !state.isSearchMode

    Surface(color = colors.surface, modifier = Modifier.statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    if (state.isInSelectionMode) androidx.compose.material.icons.Icons.Filled.Close else androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(if (state.isInSelectionMode) R.string.history_cancel_selection_desc else R.string.back),
                    tint = colors.onSurface
                )
            }

            if (state.isSearchMode) {
                ClintSearchField(
                    query = state.searchQuery,
                    onQueryChange = { state.searchQuery = it },
                    hint = stringResource(R.string.downloads_search_hint),
                    onClose = { state.isSearchMode = false; state.searchQuery = "" }
                )
            } else {
                Text(
                    text = if (state.isInSelectionMode) stringResource(R.string.downloads_selected_count, state.selectedCount) else stringResource(R.string.downloads_title),
                    color = colors.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }

            if (showToolbarIcons) {
                IconButton(onClick = { state.isSearchMode = true }) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Search, contentDescription = stringResource(R.string.downloads_search), tint = colors.iconTint)
                }
                Box {
                    IconButton(onClick = { state.sortMenuOpen = true }) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.downloads_sort), tint = colors.primary)
                    }
                    DownloadsSortMenu(
                        expanded = state.sortMenuOpen,
                        onDismiss = { state.sortMenuOpen = false },
                        sortKey = state.sortKey, sortOrder = state.sortOrder,
                        onSortByName = { state.sortKey = DownloadSortKey.NAME; state.sortOrder = ListSortOrder.ASCENDING },
                        onSortByDate = { state.sortKey = DownloadSortKey.DATE; state.sortOrder = ListSortOrder.DESCENDING },
                        onSortBySize = { state.sortKey = DownloadSortKey.SIZE; state.sortOrder = ListSortOrder.DESCENDING },
                        onSortByStatus = { state.sortKey = DownloadSortKey.STATUS; state.sortOrder = ListSortOrder.ASCENDING },
                        onSortAscending = { state.sortOrder = ListSortOrder.ASCENDING },
                        onSortDescending = { state.sortOrder = ListSortOrder.DESCENDING }
                    )
                }
                IconButton(onClick = onDownloadSettingsClick) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Settings, contentDescription = stringResource(R.string.download_settings_title), tint = colors.iconTint)
                }
                Box {
                    IconButton(onClick = { state.overflowMenuOpen = true }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, contentDescription = stringResource(R.string.downloads_overflow_menu_desc), tint = colors.iconTint)
                    }
                    DownloadsOverflowMenu(
                        expanded = state.overflowMenuOpen,
                        onDismiss = { state.overflowMenuOpen = false },
                        onResumeAll = onResumeAll,
                        onPauseAll = onPauseAll
                    )
                }
            }
            if (state.isInSelectionMode) {
                if (!state.isSearchMode) {
                    IconButton(onClick = { state.isSearchMode = true }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Search, contentDescription = stringResource(R.string.downloads_search), tint = colors.iconTint)
                    }
                }
                Box {
                    IconButton(onClick = { state.selectionOptionsMenuOpen = true }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Checklist, contentDescription = stringResource(R.string.downloads_more_options), tint = colors.primary)
                    }
                    SelectionOptionsMenu(
                        expanded = state.selectionOptionsMenuOpen,
                        onDismiss = { state.selectionOptionsMenuOpen = false },
                        onSelectAll = onSelectAll, onInvertSelection = onInvertSelection,
                        onDeselectAll = { state.deselectAll() }
                    )
                }
                Box {
                    IconButton(onClick = { state.multiItemOptionsMenuOpen = true }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, contentDescription = stringResource(R.string.download_multi_options_desc), tint = colors.iconTint)
                    }
                    val singleItem = selectedItems.singleOrNull()
                    if (singleItem != null) {
                        DownloadItemOptionsMenu(
                            expanded = state.multiItemOptionsMenuOpen,
                            item = singleItem,
                            onDismiss = { state.multiItemOptionsMenuOpen = false },
                            onOpen = { itemActions.onOpen(singleItem) },
                            onShare = { itemActions.onShare(singleItem) },
                            onOpenFolder = { itemActions.onOpenFolder(singleItem) },
                        onRename = { itemActions.onRename(singleItem) },
                            onRedownload = { itemActions.onRedownload(singleItem) },
                            onRedownloadOptions = { itemActions.onRedownloadOptions(singleItem) },
                            onChangeSettings = { itemActions.onChangeSettings(singleItem) },
                            onUpdateLink = { itemActions.onUpdateLink(singleItem) },
                            onUpdateLinkInBrowser = { itemActions.onUpdateLinkInBrowser(singleItem) },
                            onRemove = { itemActions.onRemove(singleItem) },
                            onCopyLink = { itemActions.onCopyLink(singleItem) },
                            onCopyFilename = { itemActions.onCopyFilename(singleItem) },
                            onCopyPath = { itemActions.onCopyPath(singleItem) },
                            onProperties = { itemActions.onProperties(singleItem) }
                        )
                    } else {
                        DownloadsMultiItemOptionsMenu(
                            expanded = state.multiItemOptionsMenuOpen,
                            onDismiss = { state.multiItemOptionsMenuOpen = false },
                            onRedownload = onMultiRedownload,
                            onRemove = onMultiRemove,
                            onCopyLink = onMultiCopyLink,
                            onCopyFilename = onMultiCopyFilename,
                            onCopyPath = onMultiCopyPath
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsDeleteConfirmDialog(count: Int, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit, onConfirm: (Boolean) -> Unit) {
    val colors = LocalClintColors.current
    var deleteFromStorage by remember { mutableStateOf(false) }
    ClintDialog(
        title = stringResource(R.string.downloads_delete_confirm_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                androidx.compose.material3.TextButton(onClick = { onConfirm(deleteFromStorage) }) {
                    Text(stringResource(R.string.action_delete), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.downloads_delete_confirm_message, count), color = colors.onSurface, fontSize = 14.sp)
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp).clickable { deleteFromStorage = !deleteFromStorage },
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClintCheckbox(checked = deleteFromStorage, onCheckedChange = { deleteFromStorage = it })
                Text(stringResource(R.string.downloads_delete_also_from_storage), color = colors.onSurface, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
private fun DownloadsDeleteProgressDialog(progress: DeleteProgress, hideStatusBar: Boolean, hideSystemNavigation: Boolean) {
    val colors = LocalClintColors.current
    ClintDialog(
        title = stringResource(R.string.downloads_deleting_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = {},
        cancelable = false,
        footer = {}
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            LinearProgressIndicator(
                progress = { if (progress.total > 0) progress.done.toFloat() / progress.total else 0f },
                modifier = Modifier.fillMaxWidth(),
                color = colors.primary, trackColor = colors.surfaceVariant
            )
            Text(
                stringResource(R.string.downloads_deleting_progress, progress.done, progress.total),
                color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun tabLabelFor(type: DownloadsTabType): String = when (type) {
    DownloadsTabType.ALL -> stringResource(R.string.downloads_tab_all)
    DownloadsTabType.DOWNLOADING -> stringResource(R.string.downloads_tab_downloading)
    DownloadsTabType.FINISHED -> stringResource(R.string.downloads_tab_finished)
    DownloadsTabType.ERROR -> stringResource(R.string.downloads_tab_error)
}

private fun itemMatchesTab(item: DownloadItem, type: DownloadsTabType): Boolean = when (type) {
    DownloadsTabType.ALL -> true
    DownloadsTabType.DOWNLOADING -> item.status in DownloadsTabType.ACTIVE_STATUSES
    DownloadsTabType.FINISHED -> item.status == DownloadStatus.COMPLETE
    DownloadsTabType.ERROR -> item.status == DownloadStatus.FAILED
}

private fun filterAndSortDownloads(
    items: List<DownloadItem>,
    query: String,
    sortKey: DownloadSortKey,
    sortOrder: ListSortOrder
): List<DownloadItem> {
    val filtered = if (query.isBlank()) items else {
        val q = query.trim().lowercase()
        items.filter { it.filename.lowercase().contains(q) }
    }
    val statusPriority = mapOf(
        DownloadStatus.DOWNLOADING to 0, DownloadStatus.CONNECTING to 0, DownloadStatus.ALLOCATING to 0,
        DownloadStatus.COPYING_TEMP to 0, DownloadStatus.DELETING_TEMP to 0,
        DownloadStatus.RETRYING to 1, DownloadStatus.QUEUED to 2, DownloadStatus.PAUSED to 3,
        DownloadStatus.FAILED to 4, DownloadStatus.COMPLETE to 5
    )
    val sorted = when (sortKey) {
        DownloadSortKey.NAME -> filtered.sortedBy { it.filename.lowercase() }
        DownloadSortKey.DATE -> filtered.sortedBy { it.startedAt }
        DownloadSortKey.SIZE -> filtered.sortedBy { it.totalBytes }
        DownloadSortKey.STATUS -> filtered.sortedBy { statusPriority[it.status] ?: 99 }
    }
    return if (sortOrder == ListSortOrder.DESCENDING && sortKey != DownloadSortKey.STATUS) sorted.reversed() else sorted
}
