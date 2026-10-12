package com.jhaiian.clint.downloads

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jhaiian.clint.ui.listscreen.ListSortOrder

enum class DownloadSortKey { NAME, DATE, SIZE, STATUS }

class DownloadsUiState {
    var currentTab by mutableStateOf(DownloadsTabType.ALL)

    var searchQuery by mutableStateOf("")
    var isSearchMode by mutableStateOf(false)
    var sortKey by mutableStateOf(DownloadSortKey.DATE)
    var sortOrder by mutableStateOf(ListSortOrder.DESCENDING)

    var selectedIds by mutableStateOf<Set<Int>>(emptySet())
    var isInSelectionMode by mutableStateOf(false)

    var sortMenuOpen by mutableStateOf(false)
    var overflowMenuOpen by mutableStateOf(false)
    var selectionOptionsMenuOpen by mutableStateOf(false)
    var multiItemOptionsMenuOpen by mutableStateOf(false)

    var deleteConfirmItems by mutableStateOf<List<DownloadItem>?>(null)

    var deleteProgress by mutableStateOf<DeleteProgress?>(null)

    var renameItem by mutableStateOf<DownloadItem?>(null)
    var propertiesItem by mutableStateOf<DownloadItem?>(null)
    var changeSettingsItem by mutableStateOf<DownloadItem?>(null)
    var updateLinkItem by mutableStateOf<DownloadItem?>(null)
    var manualDownloadDialogOpen by mutableStateOf(false)
    var manualDownloadPrefillUrl by mutableStateOf<String?>(null)

    var confirmDialogConfig by mutableStateOf<com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig?>(null)
    var conflictDialogRequest by mutableStateOf<DownloadConflictDialogRequest?>(null)

    val selectedCount get() = selectedIds.size

    fun toggleSelection(id: Int) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    fun enterSelectionWith(id: Int) {
        isInSelectionMode = true
        selectedIds = selectedIds + id
    }

    fun selectAll(displayed: List<DownloadItem>) {
        selectedIds = selectedIds + displayed.map { it.id }
    }

    fun invertSelection(displayed: List<DownloadItem>) {
        val displayedIds = displayed.map { it.id }.toSet()
        val keptOutsideView = selectedIds - displayedIds
        val invertedWithinView = displayedIds - selectedIds
        selectedIds = keptOutsideView + invertedWithinView
    }

    fun deselectAll() {
        selectedIds = emptySet()
    }

    fun exitSelectionMode() {
        isInSelectionMode = false
        selectedIds = emptySet()
    }
}

data class DeleteProgress(val done: Int, val total: Int)
