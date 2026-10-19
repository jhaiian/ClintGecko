package com.jhaiian.clint.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
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
    val totalHistory by DownloadSpeedHistory.total.collectAsState()
    val perItemHistory by DownloadSpeedHistory.perItem.collectAsState()

    val unfinished = remember(allItems) { allItems.filter { it.status != DownloadStatus.COMPLETE } }
    val finished = remember(allItems, state.sortKey, state.sortOrder) {
        sortDownloads(allItems.filter { it.status == DownloadStatus.COMPLETE }, state.sortKey, state.sortOrder)
    }
    val query = state.searchQuery.trim().lowercase()
    val queueItems = remember(unfinished, query) { if (query.isBlank()) unfinished else unfinished.filter { it.filename.lowercase().contains(query) } }
    val libraryItems = remember(finished, query) { if (query.isBlank()) finished else finished.filter { it.filename.lowercase().contains(query) } }
    val displayed = if (state.section == DownloadsSection.QUEUE) queueItems else libraryItems
    val selectedItems = remember(allItems, state.selectedIds) { allItems.filter { it.id in state.selectedIds } }
    val queueGridState = rememberLazyGridState()
    val libraryGridState = rememberLazyGridState()
    val activeGridState = if (state.section == DownloadsSection.QUEUE) queueGridState else libraryGridState
    val fabExpanded by remember(activeGridState) { derivedStateOf { activeGridState.firstVisibleItemIndex == 0 } }

    LaunchedEffect(allItems.isNotEmpty()) {
        if (allItems.isNotEmpty() && !state.sectionInitialized) {
            state.section = if (unfinished.isNotEmpty()) DownloadsSection.QUEUE else DownloadsSection.LIBRARY
            state.sectionInitialized = true
        }
    }

    val resumeAll = { allItems.filter { it.status == DownloadStatus.PAUSED }.forEach { onResume(it.id) } }
    val pauseAll = { allItems.filter { it.status in DownloadStatus.ACTIVELY_WORKING || it.status == DownloadStatus.QUEUED }.forEach { onPause(it.id) } }
    val onItemClick: (DownloadItem) -> Unit = { item ->
        when {
            state.isInSelectionMode -> state.toggleSelection(item.id)
            item.status == DownloadStatus.COMPLETE -> onOpenItem(item)
            item.status == DownloadStatus.QUEUED -> state.sheetItemId = item.id
            else -> state.toggleExpanded(item.id)
        }
    }
    val onItemLongClick: (DownloadItem) -> Unit = { item ->
        if (!state.isInSelectionMode) state.enterSelectionWith(item.id)
        else if (item.id !in state.selectedIds) state.selectedIds = state.selectedIds + item.id
    }

    fun handleBack() {
        when {
            state.isSearchMode -> { state.isSearchMode = false; state.searchQuery = "" }
            state.isInSelectionMode -> state.exitSelectionMode()
            else -> onExit()
        }
    }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.background(colors.background).statusBarsPadding()) {
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
                    onResumeAll = resumeAll,
                    onPauseAll = pauseAll
                )
                SectionSwitch(
                    section = state.section,
                    queueCount = unfinished.size,
                    libraryCount = allItems.size - unfinished.size,
                    activeCount = allItems.count { it.status in DownloadStatus.ACTIVELY_WORKING },
                    onSelect = { state.section = it }
                )
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (state.section == DownloadsSection.QUEUE) {
                    DownloadsQueueContent(
                        state = state,
                        items = queueItems,
                        allItems = allItems,
                        tick = tick,
                        gridState = queueGridState,
                        maxContentWidth = maxContentWidth,
                        showPulse = unfinished.isNotEmpty() && query.isBlank(),
                        totalHistory = totalHistory,
                        perItemHistory = perItemHistory,
                        onPause = onPause, onResume = onResume, onRetry = onRetry,
                        onPauseAll = pauseAll,
                        onResumeAll = resumeAll,
                        onOpenLibrary = { state.section = DownloadsSection.LIBRARY },
                        onItemClick = onItemClick,
                        onItemLongClick = onItemLongClick,
                        onShowSheet = { state.sheetItemId = it.id },
                        onChangeSettings = { itemActions.onChangeSettings(it) },
                        onRemove = { itemActions.onRemove(it) }
                    )
                } else {
                    DownloadsLibraryContent(
                        state = state,
                        items = libraryItems,
                        gridState = libraryGridState,
                        maxContentWidth = maxContentWidth,
                        onItemClick = onItemClick,
                        onItemLongClick = onItemLongClick,
                        onShowSheet = { state.sheetItemId = it.id },
                        onShare = { itemActions.onShare(it) },
                        onRemove = { itemActions.onRemove(it) }
                    )
                }

                if (state.isInSelectionMode) {
                    SelectionActionBar(
                        enabled = selectedItems.isNotEmpty(),
                        onRedownload = { onMultiRedownload(selectedItems) },
                        onCopyLink = { onMultiCopyLink(selectedItems) },
                        onCopyName = { onMultiCopyFilename(selectedItems) },
                        onDelete = { onDeleteSelectedClick(selectedItems) },
                        onMore = {
                            val single = selectedItems.singleOrNull()
                            if (single != null) state.sheetItemId = single.id else state.selectionSheetOpen = true
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                } else {
                    ExtendedFloatingActionButton(
                        onClick = onAddClick,
                        expanded = fabExpanded,
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.download_manual_fab_desc), fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = colors.primary, contentColor = colors.onPrimary,
                        modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 20.dp, end = 16.dp)
                    )
                }
            }
        }
    }

    state.sheetItemId?.let { id ->
        val liveItem = allItems.firstOrNull { it.id == id }
        if (liveItem == null) {
            LaunchedEffect(id) { state.sheetItemId = null }
        } else {
            DownloadItemSheet(
                item = liveItem,
                hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                itemActions = itemActions,
                onPause = onPause, onResume = onResume, onRetry = onRetry,
                onViewLog = { state.logViewerId = liveItem.id },
                onDismiss = { state.sheetItemId = null }
            )
        }
    }

    if (state.selectionSheetOpen && state.isInSelectionMode && selectedItems.size > 1) {
        DownloadsSelectionSheet(
            count = selectedItems.size,
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            onRedownload = { onMultiRedownload(selectedItems) },
            onCopyLink = { onMultiCopyLink(selectedItems) },
            onCopyName = { onMultiCopyFilename(selectedItems) },
            onCopyPath = { onMultiCopyPath(selectedItems) },
            onDelete = { onDeleteSelectedClick(selectedItems) },
            onDismiss = { state.selectionSheetOpen = false }
        )
    } else if (state.selectionSheetOpen) {
        LaunchedEffect(Unit) { state.selectionSheetOpen = false }
    }

    state.logViewerId?.let { logId ->
        DownloadLogViewer(
            logId = logId,
            title = if (logId == DownloadLog.GLOBAL_ID) stringResource(R.string.download_log_title)
            else stringResource(R.string.download_log_item_title, allItems.firstOrNull { it.id == logId }?.filename ?: "#$logId"),
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            onDismiss = { state.logViewerId = null }
        )
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val showToolbarIcons = !state.isInSelectionMode && !state.isSearchMode

    Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(
                if (state.isInSelectionMode) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack,
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
                color = colors.onSurface, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
        }

        if (showToolbarIcons) {
            IconButton(onClick = { state.isSearchMode = true }) {
                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.downloads_search), tint = colors.iconTint)
            }
            if (state.section == DownloadsSection.LIBRARY) {
                Box {
                    IconButton(onClick = { state.sortMenuOpen = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.downloads_sort), tint = colors.iconTint)
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
            }
            Box {
                IconButton(onClick = { state.overflowMenuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.downloads_overflow_menu_desc), tint = colors.iconTint)
                }
                DownloadsOverflowMenu(
                    expanded = state.overflowMenuOpen,
                    onDismiss = { state.overflowMenuOpen = false },
                    onResumeAll = onResumeAll,
                    onPauseAll = onPauseAll,
                    onSettings = onDownloadSettingsClick,
                    onDownloadLog = if (DownloadLog.enabled || DownloadLog.hasLog(context, DownloadLog.GLOBAL_ID)) {
                        { state.logViewerId = DownloadLog.GLOBAL_ID }
                    } else null
                )
            }
        }
        if (state.isInSelectionMode) {
            if (!state.isSearchMode) {
                IconButton(onClick = { state.isSearchMode = true }) {
                    Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.downloads_search), tint = colors.iconTint)
                }
            }
            Box {
                IconButton(onClick = { state.selectionOptionsMenuOpen = true }) {
                    Icon(Icons.Filled.Checklist, contentDescription = stringResource(R.string.downloads_more_options), tint = colors.primary)
                }
                SelectionOptionsMenu(
                    expanded = state.selectionOptionsMenuOpen,
                    onDismiss = { state.selectionOptionsMenuOpen = false },
                    onSelectAll = onSelectAll, onInvertSelection = onInvertSelection,
                    onDeselectAll = { state.deselectAll() }
                )
            }
        }
    }
}

@Composable
private fun SectionSwitch(
    section: DownloadsSection,
    queueCount: Int,
    libraryCount: Int,
    activeCount: Int,
    onSelect: (DownloadsSection) -> Unit
) {
    val colors = LocalClintColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 10.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(colors.cardBackground)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SectionTab(
            label = stringResource(R.string.downloads_section_queue),
            count = queueCount,
            live = activeCount > 0,
            selected = section == DownloadsSection.QUEUE,
            onClick = { onSelect(DownloadsSection.QUEUE) },
            modifier = Modifier.weight(1f)
        )
        SectionTab(
            label = stringResource(R.string.downloads_section_library),
            count = libraryCount,
            live = false,
            selected = section == DownloadsSection.LIBRARY,
            onClick = { onSelect(DownloadsSection.LIBRARY) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SectionTab(label: String, count: Int, live: Boolean, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = LocalClintColors.current
    val content = if (selected) colors.onPrimary else colors.onSurface
    Row(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(21.dp))
            .background(if (selected) colors.primary else colors.cardBackground)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (live) {
            Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(if (selected) colors.onPrimary else colors.primary))
            Spacer(Modifier.width(7.dp))
        }
        Text(label, color = content, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.width(7.dp))
        Text(count.toString(), color = content.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun SelectionActionBar(
    enabled: Boolean,
    onRedownload: () -> Unit,
    onCopyLink: () -> Unit,
    onCopyName: () -> Unit,
    onDelete: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    Row(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.popupBackground)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SelectionAction(Icons.Filled.Refresh, stringResource(R.string.downloads_sheet_redownload), enabled, false, onRedownload, Modifier.weight(1f))
        SelectionAction(Icons.Filled.Link, stringResource(R.string.downloads_sheet_copy_link), enabled, false, onCopyLink, Modifier.weight(1f))
        SelectionAction(Icons.Filled.ContentCopy, stringResource(R.string.downloads_sheet_copy_name), enabled, false, onCopyName, Modifier.weight(1f))
        SelectionAction(Icons.Filled.Delete, stringResource(R.string.action_delete), enabled, true, onDelete, Modifier.weight(1f))
        SelectionAction(Icons.Filled.MoreHoriz, stringResource(R.string.downloads_selection_more), enabled, false, onMore, Modifier.weight(1f))
    }
}

@Composable
private fun SelectionAction(icon: ImageVector, label: String, enabled: Boolean, danger: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = LocalClintColors.current
    val tint = when {
        !enabled -> colors.secondaryText.copy(alpha = 0.5f)
        danger -> colors.colorError
        else -> colors.onSurface
    }
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).clickable(enabled = enabled, onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, color = tint, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
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

private val statusPriority = mapOf(
    DownloadStatus.DOWNLOADING to 0, DownloadStatus.CONNECTING to 0, DownloadStatus.ALLOCATING to 0,
    DownloadStatus.DECRYPTING to 0, DownloadStatus.MUXING to 0, DownloadStatus.CONVERTING to 0,
    DownloadStatus.COPYING_TEMP to 0, DownloadStatus.DELETING_TEMP to 0,
    DownloadStatus.RETRYING to 1, DownloadStatus.QUEUED to 2, DownloadStatus.PAUSED to 3,
    DownloadStatus.FAILED to 4, DownloadStatus.COMPLETE to 5
)

private fun sortDownloads(
    items: List<DownloadItem>,
    sortKey: DownloadSortKey,
    sortOrder: ListSortOrder
): List<DownloadItem> {
    val sorted = when (sortKey) {
        DownloadSortKey.NAME -> items.sortedBy { it.filename.lowercase() }
        DownloadSortKey.DATE -> items.sortedBy { it.startedAt }
        DownloadSortKey.SIZE -> items.sortedBy { it.totalBytes }
        DownloadSortKey.STATUS -> items.sortedBy { statusPriority[it.status] ?: 99 }
    }
    return if (sortOrder == ListSortOrder.DESCENDING && sortKey != DownloadSortKey.STATUS) sorted.reversed() else sorted
}
