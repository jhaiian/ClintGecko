package com.jhaiian.clint.bookmarks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import com.jhaiian.clint.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.listscreen.ListFastScroller
import com.jhaiian.clint.ui.listscreen.ListSortOrder
import com.jhaiian.clint.ui.rememberClintFavicon
import com.jhaiian.clint.ui.listscreen.ClintSearchField
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatRelativeTimestamp

private sealed class BookmarksEntry {
    data class FolderItem(val folder: BookmarkFolder) : BookmarksEntry()
    data class BookmarkItem(val bookmark: Bookmark) : BookmarksEntry()
}

@Composable
fun BookmarksScreen(
    state: BookmarksUiState,
    maxContentWidth: Dp?,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onExit: () -> Unit,
    onOpenItem: (Bookmark) -> Unit,
    onDeleteSelectedClick: () -> Unit,
    onCreateFolder: (String) -> Unit,
    onAddBookmark: (String, String) -> Unit,
    onRenameFolder: (BookmarkFolder, String) -> Unit,
    onMoveConfirm: (Long?) -> Unit,
    onImportHtml: (android.net.Uri) -> Unit,
    onImportSqlite: (android.net.Uri) -> Unit,
    onExportHtml: (android.net.Uri) -> Unit,
    onExportSqlite: (android.net.Uri) -> Unit
) {
    val colors = LocalClintColors.current

    val importHtmlLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri -> uri?.let(onImportHtml) }
    val importSqliteLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri -> uri?.let(onImportSqlite) }
    val exportHtmlLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/html")) { uri -> uri?.let(onExportHtml) }
    val exportSqliteLauncher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")) { uri -> uri?.let(onExportSqlite) }

    val currentFolders = remember(state.allFolders, state.currentFolderId) {
        state.allFolders.filter { it.parentId == state.currentFolderId }.sortedBy { it.name.lowercase() }
    }
    val currentBookmarks = remember(state.allBookmarks, state.currentFolderId, state.sortKey, state.sortOrder) {
        sortBookmarks(state.allBookmarks.filter { it.folderId == state.currentFolderId }, state.sortKey, state.sortOrder)
    }
    val searchResults = remember(state.allBookmarks, state.searchQuery, state.sortKey, state.sortOrder) {
        filterAndSortBookmarks(state.allBookmarks, state.searchQuery, state.sortKey, state.sortOrder)
    }
    val breadcrumbPath = remember(state.allFolders, state.currentFolderId) {
        folderPathFor(state.allFolders, state.currentFolderId)
    }

    val displayedEntries: List<BookmarksEntry> = if (state.isSearchMode) {
        searchResults.map { BookmarksEntry.BookmarkItem(it) }
    } else {
        currentFolders.map { BookmarksEntry.FolderItem(it) } + currentBookmarks.map { BookmarksEntry.BookmarkItem(it) }
    }
    val selectableFolders = if (state.isSearchMode) emptyList() else currentFolders
    val selectableItems = if (state.isSearchMode) searchResults else currentBookmarks

    val folderItemCounts = remember(state.allFolders, state.allBookmarks) {
        val folderCounts = state.allFolders.groupingBy { it.parentId }.eachCount()
        val bookmarkCounts = state.allBookmarks.groupingBy { it.folderId }.eachCount()
        (folderCounts.keys + bookmarkCounts.keys).filterNotNull()
            .associateWith { id -> (folderCounts[id] ?: 0) + (bookmarkCounts[id] ?: 0) }
    }

    val singleSelectedFolder = if (state.selectedKeys.size == 1) {
        (state.selectedKeys.first() as? BookmarkEntryKey.Folder)?.let { key -> state.allFolders.find { it.id == key.id } }
    } else null

    val listState = rememberLazyListState()
    val fastScrollerInteractive = !state.isSearchMode && state.sortKey == BookmarksSortKey.TITLE
    val showDeleteFab = state.isInSelectionMode && state.selectedKeys.isNotEmpty()

    fun handleBack() {
        when {
            state.isSearchMode -> { state.isSearchMode = false; state.searchQuery = "" }
            state.isInSelectionMode -> state.exitSelectionMode()
            state.currentFolderId != null -> state.navigateUp()
            else -> onExit()
        }
    }

    fun openMoveDialog() {
        val excludeIds = state.selectedFolderIds + collectDescendantIds(state.allFolders, state.selectedFolderIds)
        state.moveDialogTree = buildFolderTree(state.allFolders, excludeIds)
        state.moveDialogOpen = true
    }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BookmarksToolbar(
                state = state,
                onBack = ::handleBack,
                onSelectAll = { state.selectAll(selectableFolders, selectableItems) },
                onInvertSelection = { state.invertSelection(selectableFolders, selectableItems) },
                onMoveToClick = ::openMoveDialog,
                showRename = singleSelectedFolder != null,
                onRenameClick = { state.renameFolderTarget = singleSelectedFolder },
                onImportClick = { state.importExportMode = BookmarksImportExportMode.IMPORT },
                onExportClick = { state.importExportMode = BookmarksImportExportMode.EXPORT }
            )
            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            if (!state.isSearchMode && breadcrumbPath.isNotEmpty()) {
                BookmarksBreadcrumb(
                    path = breadcrumbPath,
                    onRootClick = { state.currentFolderId = null },
                    onFolderClick = { id -> state.currentFolderId = id }
                )
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading -> Unit
                    displayedEntries.isEmpty() -> Text(
                        stringResource(
                            if (state.currentFolderId != null && !state.isSearchMode) R.string.bookmarks_folder_empty
                            else R.string.bookmarks_empty
                        ),
                        color = colors.secondaryText, fontSize = 15.sp,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp)
                    )
                    else -> {
                        AdaptiveWidthContainer(maxContentWidth) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp)
                            ) {
                                items(
                                    displayedEntries,
                                    key = { entry ->
                                        when (entry) {
                                            is BookmarksEntry.FolderItem -> "folder:${entry.folder.id}"
                                            is BookmarksEntry.BookmarkItem -> "bookmark:${entry.bookmark.url}"
                                        }
                                    }
                                ) { entry ->
                                    when (entry) {
                                        is BookmarksEntry.FolderItem -> {
                                            val key = BookmarkEntryKey.Folder(entry.folder.id)
                                            FolderRow(
                                                folder = entry.folder,
                                                itemCount = folderItemCounts[entry.folder.id] ?: 0,
                                                isSelected = key in state.selectedKeys,
                                                isInSelectionMode = state.isInSelectionMode,
                                                onClick = {
                                                    if (state.isInSelectionMode) state.toggleSelection(key) else state.currentFolderId = entry.folder.id
                                                },
                                                onLongClick = {
                                                    if (!state.isInSelectionMode) state.enterSelectionWith(key)
                                                    else if (key !in state.selectedKeys) state.selectedKeys = state.selectedKeys + key
                                                }
                                            )
                                        }
                                        is BookmarksEntry.BookmarkItem -> {
                                            val key = BookmarkEntryKey.Item(entry.bookmark.url)
                                            BookmarkRow(
                                                item = entry.bookmark,
                                                isSelected = key in state.selectedKeys,
                                                isInSelectionMode = state.isInSelectionMode,
                                                onClick = {
                                                    if (state.isInSelectionMode) state.toggleSelection(key) else onOpenItem(entry.bookmark)
                                                },
                                                onLongClick = {
                                                    if (!state.isInSelectionMode) state.enterSelectionWith(key)
                                                    else if (key !in state.selectedKeys) state.selectedKeys = state.selectedKeys + key
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            ListFastScroller(
                                listState = listState,
                                itemCount = displayedEntries.size,
                                isInteractive = fastScrollerInteractive,
                                sectionLetterAt = { index -> sectionLetterForEntry(displayedEntries[index]) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                BookmarksFabMenu(
                    isOpen = state.isFabMenuOpen,
                    showDeleteFab = showDeleteFab,
                    showPrimaryFab = !state.isInSelectionMode,
                    onScrimClick = { state.isFabMenuOpen = false },
                    onToggleMenu = { state.isFabMenuOpen = !state.isFabMenuOpen },
                    onDeleteClick = onDeleteSelectedClick,
                    onAddFolderClick = { state.isFabMenuOpen = false; state.newFolderDialogOpen = true },
                    onAddBookmarkClick = { state.isFabMenuOpen = false; state.newBookmarkDialogOpen = true }
                )
            }
        }
    }

    if (state.newFolderDialogOpen) {
        CreateBookmarkFolderDialog(
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            onDismiss = { state.newFolderDialogOpen = false },
            onConfirm = { name -> state.newFolderDialogOpen = false; onCreateFolder(name) }
        )
    }
    if (state.newBookmarkDialogOpen) {
        CreateBookmarkDialog(
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            onDismiss = { state.newBookmarkDialogOpen = false },
            onConfirm = { title, url -> state.newBookmarkDialogOpen = false; onAddBookmark(title, url) }
        )
    }
    if (state.importExportMode == BookmarksImportExportMode.IMPORT) {
        BookmarksFormatPickerDialog(
            title = stringResource(R.string.bookmarks_import_title),
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            onDismiss = { state.importExportMode = BookmarksImportExportMode.NONE },
            onSelectHtml = { state.importExportMode = BookmarksImportExportMode.NONE; importHtmlLauncher.launch(arrayOf("text/html", "text/plain", "*/*")) },
            onSelectSqlite = { state.importExportMode = BookmarksImportExportMode.NONE; importSqliteLauncher.launch(arrayOf("application/octet-stream", "application/vnd.sqlite3", "*/*")) }
        )
    }
    if (state.importExportMode == BookmarksImportExportMode.EXPORT) {
        BookmarksFormatPickerDialog(
            title = stringResource(R.string.bookmarks_export_title),
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            onDismiss = { state.importExportMode = BookmarksImportExportMode.NONE },
            onSelectHtml = { state.importExportMode = BookmarksImportExportMode.NONE; exportHtmlLauncher.launch("bookmarks.html") },
            onSelectSqlite = { state.importExportMode = BookmarksImportExportMode.NONE; exportSqliteLauncher.launch("bookmarks_backup.db") }
        )
    }
    state.renameFolderTarget?.let { folder ->
        RenameBookmarkFolderDialog(
            folder = folder,
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            onDismiss = { state.renameFolderTarget = null },
            onConfirm = { newName -> state.renameFolderTarget = null; onRenameFolder(folder, newName) }
        )
    }
    if (state.moveDialogOpen) {
        MoveToFolderDialog(
            tree = state.moveDialogTree,
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            onDismiss = { state.moveDialogOpen = false },
            onSelect = { targetId -> onMoveConfirm(targetId) }
        )
    }
}

@Composable
private fun BookmarksBreadcrumb(
    path: List<BookmarkFolder>,
    onRootClick: () -> Unit,
    onFolderClick: (Long) -> Unit
) {
    val colors = LocalClintColors.current
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.bookmarks_title),
            color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.combinedClickable(onClick = onRootClick, onLongClick = {})
        )
        path.forEachIndexed { index, folder ->
            Icon(
                androidx.compose.material.icons.Icons.Filled.ChevronRight, contentDescription = null,
                tint = colors.secondaryText, modifier = Modifier.size(16.dp).padding(horizontal = 2.dp)
            )
            val isLast = index == path.lastIndex
            Text(
                folder.name,
                color = if (isLast) colors.onSurface else colors.primary,
                fontSize = 13.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = if (isLast) Modifier else Modifier.combinedClickable(onClick = { onFolderClick(folder.id) }, onLongClick = {})
            )
        }
    }
}

@Composable
private fun BookmarksToolbar(
    state: BookmarksUiState,
    onBack: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onMoveToClick: () -> Unit,
    showRename: Boolean,
    onRenameClick: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val showToolbarIcons = !state.isInSelectionMode && !state.isSearchMode

    Surface(color = colors.surface, shadowElevation = 4.dp, modifier = Modifier.statusBarsPadding()) {
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
                    hint = stringResource(R.string.bookmarks_search_hint),
                    onClose = { state.isSearchMode = false; state.searchQuery = "" }
                )
            } else {
                Text(
                    text = if (state.isInSelectionMode) stringResource(R.string.bookmarks_selected_count, state.selectedCount) else stringResource(R.string.bookmarks_title),
                    color = colors.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }

            if (showToolbarIcons) {
                IconButton(onClick = { state.isSearchMode = true }) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Search, contentDescription = stringResource(R.string.bookmarks_search), tint = colors.iconTint)
                }
                Box {
                    IconButton(onClick = { state.sortMenuOpen = true }) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.history_sort), tint = colors.primary)
                    }
                    BookmarksSortMenu(
                        expanded = state.sortMenuOpen,
                        onDismiss = { state.sortMenuOpen = false },
                        sortKey = state.sortKey, sortOrder = state.sortOrder,
                        onSortByTitle = { state.sortKey = BookmarksSortKey.TITLE; state.sortOrder = ListSortOrder.ASCENDING },
                        onSortByLastVisit = { state.sortKey = BookmarksSortKey.LAST_VISIT; state.sortOrder = ListSortOrder.DESCENDING },
                        onSortByDateAdded = { state.sortKey = BookmarksSortKey.DATE_ADDED; state.sortOrder = ListSortOrder.DESCENDING },
                        onSortAscending = { state.sortOrder = ListSortOrder.ASCENDING },
                        onSortDescending = { state.sortOrder = ListSortOrder.DESCENDING }
                    )
                }
                Box {
                    IconButton(onClick = { state.moreMenuOpen = true }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, contentDescription = stringResource(R.string.history_more_options), tint = colors.iconTint)
                    }
                    BookmarksMoreMenu(
                        expanded = state.moreMenuOpen,
                        onDismiss = { state.moreMenuOpen = false },
                        onImportClick = onImportClick,
                        onExportClick = onExportClick
                    )
                }
            }
            if (state.isInSelectionMode) {
                Box {
                    IconButton(onClick = { state.selectionOptionsMenuOpen = true }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Checklist, contentDescription = stringResource(R.string.history_more_options), tint = colors.primary)
                    }
                    BookmarksSelectionOptionsMenu(
                        expanded = state.selectionOptionsMenuOpen,
                        onDismiss = { state.selectionOptionsMenuOpen = false },
                        onSelectAll = onSelectAll, onInvertSelection = onInvertSelection,
                        onDeselectAll = { state.deselectAll() }
                    )
                }
                Box {
                    IconButton(onClick = { state.selectionItemOptionsMenuOpen = true }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, contentDescription = stringResource(R.string.history_more_options), tint = colors.primary)
                    }
                    BookmarksSelectionItemOptionsMenu(
                        expanded = state.selectionItemOptionsMenuOpen,
                        onDismiss = { state.selectionItemOptionsMenuOpen = false },
                        onMoveTo = onMoveToClick,
                        showRename = showRename,
                        onRename = onRenameClick
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderRow(
    folder: BookmarkFolder,
    itemCount: Int,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val cardColor = if (isSelected) lerp(colors.cardBackground, colors.primary, 0.22f) else colors.cardBackground

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(colors.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(androidx.compose.material.icons.Icons.Filled.Folder, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                folder.name,
                color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                stringResource(R.string.bookmarks_folder_item_count, itemCount),
                color = colors.secondaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun BookmarkRow(
    item: Bookmark,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val cardColor = if (isSelected) lerp(colors.cardBackground, colors.primary, 0.22f) else colors.cardBackground

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val favicon = rememberClintFavicon(item.url, item.faviconUrl)
        Box(Modifier.size(40.dp).clip(CircleShape).background(colors.surfaceVariant), contentAlignment = Alignment.Center) {
            if (favicon != null) {
                Image(bitmap = favicon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(22.dp))
            } else {
                Icon(androidx.compose.material.icons.Icons.Filled.Public, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                item.title.ifBlank { item.url },
                color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                item.url.removePrefix("https://").removePrefix("http://"),
                color = colors.secondaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (item.lastVisit > 0L) {
                Text(
                    formatRelativeTimestamp(item.lastVisit),
                    color = colors.secondaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun BoxScope.BookmarksFabMenu(
    isOpen: Boolean,
    showDeleteFab: Boolean,
    showPrimaryFab: Boolean,
    onScrimClick: () -> Unit,
    onToggleMenu: () -> Unit,
    onDeleteClick: () -> Unit,
    onAddFolderClick: () -> Unit,
    onAddBookmarkClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val menuOpen = isOpen && showPrimaryFab
    val rotation by animateFloatAsState(if (menuOpen) 45f else 0f, label = "bookmarksFabMenuRotation")

    if (menuOpen) {
        Box(Modifier.fillMaxSize().background(colors.background.copy(alpha = 0.6f)).clickable(onClick = onScrimClick))
    }

    if (showDeleteFab) {
        FloatingActionButton(
            onClick = onDeleteClick,
            containerColor = colors.buttonBackground, contentColor = colors.buttonIconTint,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 24.dp, end = 20.dp)
        ) {
            Icon(androidx.compose.material.icons.Icons.Filled.Delete, contentDescription = stringResource(R.string.bookmarks_delete_selected_desc))
        }
    }

    if (showPrimaryFab) {
        AnimatedVisibility(
            visible = menuOpen,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 96.dp, end = 20.dp),
            enter = fadeIn(), exit = fadeOut()
        ) {
            Column(horizontalAlignment = Alignment.End) {
                FabMenuPill(text = stringResource(R.string.bookmarks_fab_add_folder), icon = androidx.compose.material.icons.Icons.Filled.CreateNewFolder, onClick = onAddFolderClick)
                Spacer(Modifier.height(10.dp))
                FabMenuPill(text = stringResource(R.string.bookmarks_fab_add_bookmark), icon = androidx.compose.material.icons.Icons.Filled.BookmarkAdd, onClick = onAddBookmarkClick)
            }
        }

        FloatingActionButton(
            onClick = onToggleMenu,
            containerColor = colors.buttonBackground, contentColor = colors.buttonIconTint,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 24.dp, end = 20.dp)
        ) {
            Icon(
                androidx.compose.material.icons.Icons.Filled.Add,
                contentDescription = stringResource(R.string.bookmarks_add_fab_desc),
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

@Composable
private fun FabMenuPill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.buttonBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = colors.buttonTextColor, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(end = 10.dp))
        Icon(icon, contentDescription = null, tint = colors.buttonIconTint, modifier = Modifier.size(18.dp))
    }
}

private fun sortBookmarks(items: List<Bookmark>, sortKey: BookmarksSortKey, sortOrder: ListSortOrder): List<Bookmark> {
    val sorted = when (sortKey) {
        BookmarksSortKey.TITLE -> items.sortedBy { it.title.ifBlank { it.url }.lowercase() }
        BookmarksSortKey.LAST_VISIT -> items.sortedBy { it.lastVisit }
        BookmarksSortKey.DATE_ADDED -> items.sortedBy { it.addedAt }
    }
    return if (sortOrder == ListSortOrder.DESCENDING) sorted.reversed() else sorted
}

private fun filterAndSortBookmarks(
    items: List<Bookmark>,
    query: String,
    sortKey: BookmarksSortKey,
    sortOrder: ListSortOrder
): List<Bookmark> {
    val filtered = if (query.isBlank()) items else {
        val q = query.trim().lowercase()
        items.filter { it.title.lowercase().contains(q) || it.url.lowercase().contains(q) }
    }
    return sortBookmarks(filtered, sortKey, sortOrder)
}

private fun sectionLetterForEntry(entry: BookmarksEntry): String {
    val display = when (entry) {
        is BookmarksEntry.FolderItem -> entry.folder.name
        is BookmarksEntry.BookmarkItem -> entry.bookmark.title.ifBlank { entry.bookmark.url }
    }.trimStart()
    val first = display.firstOrNull() ?: return "#"
    return if (first.isLetter()) first.uppercaseChar().toString() else if (first.isDigit()) first.toString() else "#"
}
