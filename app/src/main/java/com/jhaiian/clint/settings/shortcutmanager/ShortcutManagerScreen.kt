package com.jhaiian.clint.settings.shortcutmanager

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.shortcuts.ShortcutIconStore
import com.jhaiian.clint.shortcuts.WebAppShortcut
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.listscreen.ClintSearchField
import com.jhaiian.clint.ui.listscreen.ListFastScroller
import com.jhaiian.clint.ui.listscreen.ListMenuItem
import com.jhaiian.clint.ui.listscreen.ListSortKey
import com.jhaiian.clint.ui.listscreen.ListSortOrder
import com.jhaiian.clint.ui.listscreen.SelectionOptionsMenu
import com.jhaiian.clint.ui.listscreen.SortMenu
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatRelativeTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ShortcutManagerScreen(
    state: ShortcutManagerUiState,
    maxContentWidth: Dp?,
    onExit: () -> Unit,
    onOpenSettings: (WebAppShortcut) -> Unit,
    onCreateShortcut: (WebAppShortcut) -> Unit,
    onDelete: (WebAppShortcut) -> Unit,
    onDeleteSelectedClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val displayed = remember(state.shortcuts, state.searchQuery, state.sortKey, state.sortOrder) {
        filterAndSortShortcuts(state.shortcuts, state.searchQuery, state.sortKey, state.sortOrder)
    }
    val listState = rememberLazyListState()
    val fastScrollerInteractive = !state.isSearchMode && state.sortKey == ListSortKey.TITLE

    val showDeleteFab = state.isInSelectionMode && state.selectedIds.isNotEmpty()

    fun handleBack() {
        when {
            state.isSearchMode -> state.closeSearch()
            state.isInSelectionMode -> state.exitSelectionMode()
            else -> onExit()
        }
    }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ShortcutManagerToolbar(
                state = state,
                onBack = ::handleBack,
                onSelectAll = { state.selectAll(displayed) },
                onInvertSelection = { state.invertSelection(displayed) }
            )
            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (displayed.isEmpty()) {
                    Text(
                        text = stringResource(
                            if (state.shortcuts.isEmpty()) R.string.shortcut_manager_empty else R.string.shortcut_manager_no_results
                        ),
                        color = colors.secondaryText,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp)
                    )
                } else {
                    AdaptiveWidthContainer(maxContentWidth) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (showDeleteFab) 80.dp else 0.dp)
                        ) {
                            items(displayed, key = { it.id }) { shortcut ->
                                ShortcutCard(
                                    shortcut = shortcut,
                                    iconRevision = state.iconRevision,
                                    isSelected = shortcut.id in state.selectedIds,
                                    isInSelectionMode = state.isInSelectionMode,
                                    onClick = { if (state.isInSelectionMode) state.toggleSelection(shortcut.id) },
                                    onLongClick = {
                                        if (!state.isInSelectionMode) {
                                            state.enterSelectionWith(shortcut.id)
                                        } else if (shortcut.id !in state.selectedIds) {
                                            state.selectedIds = state.selectedIds + shortcut.id
                                        }
                                    },
                                    onOpenSettings = { onOpenSettings(shortcut) },
                                    onCreateShortcut = { onCreateShortcut(shortcut) },
                                    onDelete = { onDelete(shortcut) }
                                )
                            }
                            item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)) }
                        }
                        ListFastScroller(
                            listState = listState,
                            itemCount = displayed.size,
                            isInteractive = fastScrollerInteractive,
                            sectionLetterAt = { index -> sectionLetterForShortcut(displayed[index]) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                if (showDeleteFab) {
                    FloatingActionButton(
                        onClick = onDeleteSelectedClick,
                        containerColor = colors.buttonBackground,
                        contentColor = colors.buttonIconTint,
                        modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 24.dp, end = 20.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.shortcut_manager_delete_selected_desc))
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutManagerToolbar(
    state: ShortcutManagerUiState,
    onBack: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit
) {
    val colors = LocalClintColors.current

    Surface(color = colors.surface, shadowElevation = 4.dp, modifier = Modifier.statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    hint = stringResource(R.string.shortcut_manager_search_hint),
                    onClose = { state.closeSearch() }
                )
            } else {
                Text(
                    text = if (state.isInSelectionMode) {
                        stringResource(R.string.history_selected_count, state.selectedCount)
                    } else {
                        stringResource(R.string.shortcut_manager_title)
                    },
                    color = colors.onSurface,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }
            if (!state.isInSelectionMode && !state.isSearchMode) {
                IconButton(onClick = { state.isSearchMode = true }) {
                    Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.history_search), tint = colors.iconTint)
                }
                Box {
                    IconButton(onClick = { state.sortMenuOpen = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.history_sort), tint = colors.primary)
                    }
                    SortMenu(
                        expanded = state.sortMenuOpen,
                        onDismiss = { state.sortMenuOpen = false },
                        sortKey = state.sortKey,
                        sortOrder = state.sortOrder,
                        onSortByTitle = { state.sortKey = ListSortKey.TITLE; state.sortOrder = ListSortOrder.ASCENDING },
                        onSortByDateAdded = { state.sortKey = ListSortKey.DATE_ADDED; state.sortOrder = ListSortOrder.DESCENDING },
                        onSortAscending = { state.sortOrder = ListSortOrder.ASCENDING },
                        onSortDescending = { state.sortOrder = ListSortOrder.DESCENDING },
                        secondarySortLabel = stringResource(R.string.shortcut_manager_sort_by_created)
                    )
                }
            }
            if (state.isInSelectionMode) {
                Box {
                    IconButton(onClick = { state.selectionOptionsMenuOpen = true }) {
                        Icon(Icons.Filled.Checklist, contentDescription = stringResource(R.string.history_more_options), tint = colors.primary)
                    }
                    SelectionOptionsMenu(
                        expanded = state.selectionOptionsMenuOpen,
                        onDismiss = { state.selectionOptionsMenuOpen = false },
                        onSelectAll = onSelectAll,
                        onInvertSelection = onInvertSelection,
                        onDeselectAll = { state.deselectAll() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortcutCard(
    shortcut: WebAppShortcut,
    iconRevision: Int,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onCreateShortcut: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalClintColors.current
    var menuOpen by remember { mutableStateOf(false) }
    val icon by produceState<Bitmap?>(initialValue = null, shortcut.iconPath, iconRevision) {
        value = withContext(Dispatchers.IO) { ShortcutIconStore.load(shortcut.iconPath) }
    }

    val cardColor = if (isSelected) lerp(colors.cardBackground, colors.primary, 0.22f) else colors.cardBackground

    SettingsSection(
        cardColor,
        modifier = Modifier
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        bottomSpacing = 0.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = icon
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Filled.Public, contentDescription = null, tint = colors.iconTint)
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(
                    text = shortcut.name,
                    color = colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = shortcut.url,
                    color = colors.secondaryText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (shortcut.createdAt > 0L) {
                    Text(
                        text = stringResource(R.string.shortcut_manager_created, formatRelativeTimestamp(shortcut.createdAt)),
                        color = colors.secondaryText,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (isInSelectionMode) {
                Spacer(Modifier.width(12.dp))
            } else {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.shortcut_manager_more_options),
                            tint = colors.iconTint
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        shape = PopupShape,
                        containerColor = colors.popupBackground,
                        border = BorderStroke(1.dp, colors.popupStroke)
                    ) {
                        ListMenuItem(Icons.Filled.Settings, stringResource(R.string.settings), checked = false) {
                            menuOpen = false
                            onOpenSettings()
                        }
                        HorizontalDivider(color = colors.divider)
                        ListMenuItem(Icons.AutoMirrored.Filled.AddToHomeScreen, stringResource(R.string.menu_create_shortcut), checked = false) {
                            menuOpen = false
                            onCreateShortcut()
                        }
                        HorizontalDivider(color = colors.divider)
                        ListMenuItem(Icons.Filled.Delete, stringResource(R.string.action_delete), checked = false) {
                            menuOpen = false
                            onDelete()
                        }
                    }
                }
            }
        }
    }
}

private fun filterAndSortShortcuts(
    items: List<WebAppShortcut>,
    query: String,
    sortKey: ListSortKey,
    sortOrder: ListSortOrder
): List<WebAppShortcut> {
    val q = query.trim().lowercase()
    val filtered = if (q.isEmpty()) items else items.filter {
        it.name.lowercase().contains(q) || it.url.lowercase().contains(q)
    }
    val sorted = when (sortKey) {
        ListSortKey.TITLE -> filtered.sortedBy { it.name.lowercase() }
        ListSortKey.DATE_ADDED -> filtered.sortedBy { it.createdAt }
    }
    return if (sortOrder == ListSortOrder.DESCENDING) sorted.reversed() else sorted
}

private fun sectionLetterForShortcut(shortcut: WebAppShortcut): String {
    val first = shortcut.name.trimStart().firstOrNull() ?: return "#"
    return if (first.isLetter()) first.uppercaseChar().toString() else if (first.isDigit()) first.toString() else "#"
}
