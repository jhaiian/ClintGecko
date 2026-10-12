package com.jhaiian.clint.bookmarks

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.listscreen.ListSortOrder

enum class BookmarksSortKey { TITLE, LAST_VISIT, DATE_ADDED }
enum class BookmarksImportExportMode { NONE, IMPORT, EXPORT }

sealed class BookmarkEntryKey {
    data class Folder(val id: Long) : BookmarkEntryKey()
    data class Item(val url: String) : BookmarkEntryKey()
}

class BookmarksUiState {
    var allBookmarks by mutableStateOf<List<Bookmark>>(emptyList())
    var allFolders by mutableStateOf<List<BookmarkFolder>>(emptyList())
    var currentFolderId by mutableStateOf<Long?>(null)
    var isLoading by mutableStateOf(true)

    var searchQuery by mutableStateOf("")
    var isSearchMode by mutableStateOf(false)
    var sortKey by mutableStateOf(BookmarksSortKey.LAST_VISIT)
    var sortOrder by mutableStateOf(ListSortOrder.DESCENDING)

    var selectedKeys by mutableStateOf<Set<BookmarkEntryKey>>(emptySet())
    var isInSelectionMode by mutableStateOf(false)

    var sortMenuOpen by mutableStateOf(false)
    var selectionOptionsMenuOpen by mutableStateOf(false)
    var selectionItemOptionsMenuOpen by mutableStateOf(false)
    var moreMenuOpen by mutableStateOf(false)
    var importExportMode by mutableStateOf(BookmarksImportExportMode.NONE)

    var isFabMenuOpen by mutableStateOf(false)
    var newFolderDialogOpen by mutableStateOf(false)
    var newBookmarkDialogOpen by mutableStateOf(false)
    var renameFolderTarget by mutableStateOf<BookmarkFolder?>(null)
    var moveDialogOpen by mutableStateOf(false)
    var moveDialogTree by mutableStateOf<List<FolderTreeEntry>>(emptyList())

    var deleteConfirm by mutableStateOf<ConfirmDialogConfig?>(null)

    val selectedCount get() = selectedKeys.size

    val selectedFolderIds get() = selectedKeys.filterIsInstance<BookmarkEntryKey.Folder>().map { it.id }.toSet()
    val selectedItemUrls get() = selectedKeys.filterIsInstance<BookmarkEntryKey.Item>().map { it.url }.toSet()

    fun toggleSelection(key: BookmarkEntryKey) {
        selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key
    }

    fun enterSelectionWith(key: BookmarkEntryKey) {
        isInSelectionMode = true
        selectedKeys = selectedKeys + key
    }

    fun selectAll(displayedFolders: List<BookmarkFolder>, displayedItems: List<Bookmark>) {
        selectedKeys = selectedKeys + displayedFolders.map { BookmarkEntryKey.Folder(it.id) } +
            displayedItems.map { BookmarkEntryKey.Item(it.url) }
    }

    fun invertSelection(displayedFolders: List<BookmarkFolder>, displayedItems: List<Bookmark>) {
        val displayedKeys = (displayedFolders.map { BookmarkEntryKey.Folder(it.id) } +
            displayedItems.map { BookmarkEntryKey.Item(it.url) }).toSet()
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

    fun navigateUp() {
        val current = currentFolderId ?: return
        currentFolderId = allFolders.find { it.id == current }?.parentId
    }
}
