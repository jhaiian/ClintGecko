package com.jhaiian.clint.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.listscreen.ListSortKey
import com.jhaiian.clint.ui.listscreen.ListSortOrder

class HistoryUiState {
    var items by mutableStateOf<List<HistoryItem>>(emptyList())
    var isLoading by mutableStateOf(true)

    var searchQuery by mutableStateOf("")
    var isSearchMode by mutableStateOf(false)
    var sortKey by mutableStateOf(ListSortKey.DATE_ADDED)
    var sortOrder by mutableStateOf(ListSortOrder.DESCENDING)

    var selectedKeys by mutableStateOf<Set<String>>(emptySet())
    var isInSelectionMode by mutableStateOf(false)

    var sortMenuOpen by mutableStateOf(false)
    var historyActionsMenuOpen by mutableStateOf(false)
    var selectionOptionsMenuOpen by mutableStateOf(false)

    var deleteConfirm by mutableStateOf<ConfirmDialogConfig?>(null)
    var clearAllConfirm by mutableStateOf<ConfirmDialogConfig?>(null)

    val selectedCount get() = selectedKeys.size

    fun toggleSelection(query: String) {
        selectedKeys = if (query in selectedKeys) selectedKeys - query else selectedKeys + query
    }

    fun enterSelectionWith(query: String) {
        isInSelectionMode = true
        selectedKeys = selectedKeys + query
    }

    fun selectAll(displayed: List<HistoryItem>) {
        selectedKeys = selectedKeys + displayed.map { it.query }
    }

    fun invertSelection(displayed: List<HistoryItem>) {
        val displayedKeys = displayed.map { it.query }.toSet()
        val keptOutsideView = selectedKeys - displayedKeys
        val invertedWithinView = displayedKeys - selectedKeys
        selectedKeys = keptOutsideView + invertedWithinView
    }

    fun deselectAll() {
        selectedKeys = emptySet()
    }

    fun exitSelectionMode() {
        isInSelectionMode = false
        selectedKeys = emptySet()
    }
}
