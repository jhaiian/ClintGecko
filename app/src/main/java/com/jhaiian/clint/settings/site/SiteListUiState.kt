package com.jhaiian.clint.settings.site

import com.jhaiian.clint.ui.listscreen.ListSortKey
import com.jhaiian.clint.ui.listscreen.ListSortOrder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SiteListUiState {
    var allItems by mutableStateOf<List<SiteEntry>>(emptyList())
    var searchQuery by mutableStateOf("")
    var isSearchMode by mutableStateOf(false)
    var sortKey by mutableStateOf(ListSortKey.DATE_ADDED)
    var sortOrder by mutableStateOf(ListSortOrder.DESCENDING)
    var selectedOrigins by mutableStateOf<Set<String>>(emptySet())
    var isInSelectionMode by mutableStateOf(false)
    var sortMenuOpen by mutableStateOf(false)
    var moreOptionsMenuOpen by mutableStateOf(false)
    var addDialogOpen by mutableStateOf(false)
    var deleteConfirmOpen by mutableStateOf(false)

    fun toggleSelection(origin: String) {
        selectedOrigins = if (origin in selectedOrigins) selectedOrigins - origin else selectedOrigins + origin
    }

    fun enterSelectionWith(origin: String) {
        isInSelectionMode = true
        selectedOrigins = selectedOrigins + origin
    }

    fun selectAll(displayed: List<SiteEntry>) {
        selectedOrigins = selectedOrigins + displayed.map { it.origin }
    }

    fun invertSelection(displayed: List<SiteEntry>) {
        val displayedOrigins = displayed.map { it.origin }.toSet()
        val keptOutsideView = selectedOrigins - displayedOrigins
        val invertedWithinView = displayedOrigins - selectedOrigins
        selectedOrigins = keptOutsideView + invertedWithinView
    }

    fun deselectAll() {
        selectedOrigins = emptySet()
    }

    fun exitSelectionMode() {
        isInSelectionMode = false
        selectedOrigins = emptySet()
    }

    fun removeSelectedItems() {
        allItems = allItems.filterNot { it.origin in selectedOrigins }
        isInSelectionMode = false
        selectedOrigins = emptySet()
    }
}
