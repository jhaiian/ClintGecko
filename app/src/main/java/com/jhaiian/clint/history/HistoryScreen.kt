package com.jhaiian.clint.history
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import com.jhaiian.clint.R

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.rememberClintFavicon
import com.jhaiian.clint.ui.listscreen.ClintSearchField
import com.jhaiian.clint.ui.listscreen.ListFastScroller
import com.jhaiian.clint.ui.listscreen.ListMenuItem
import com.jhaiian.clint.ui.listscreen.ListSortKey
import com.jhaiian.clint.ui.listscreen.ListSortOrder
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.listscreen.SelectionOptionsMenu
import com.jhaiian.clint.ui.listscreen.SortMenu
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatRelativeTimestamp

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    maxContentWidth: Dp?,
    onExit: () -> Unit,
    onOpenItem: (HistoryItem) -> Unit,
    onDeleteSelectedClick: () -> Unit,
    onClearAllClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val displayed = remember(state.items, state.searchQuery, state.sortKey, state.sortOrder) {
        filterAndSortHistory(state.items, state.searchQuery, state.sortKey, state.sortOrder)
    }
    val listState = rememberLazyListState()
    val fastScrollerInteractive = !state.isSearchMode && state.sortKey == ListSortKey.TITLE
    val showDeleteFab = state.isInSelectionMode && state.selectedKeys.isNotEmpty()

    fun handleBack() {
        when {
            state.isSearchMode -> { state.isSearchMode = false; state.searchQuery = "" }
            state.isInSelectionMode -> state.exitSelectionMode()
            else -> onExit()
        }
    }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            HistoryToolbar(
                state = state,
                onBack = ::handleBack,
                onSelectAll = { state.selectAll(displayed) },
                onInvertSelection = { state.invertSelection(displayed) },
                onClearAllClick = onClearAllClick
            )
            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading -> Unit
                    displayed.isEmpty() -> Text(
                        stringResource(R.string.history_empty),
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
                                items(displayed, key = { it.query }) { item ->
                                    HistoryRow(
                                        item = item,
                                        isSelected = item.query in state.selectedKeys,
                                        isInSelectionMode = state.isInSelectionMode,
                                        onClick = {
                                            if (state.isInSelectionMode) state.toggleSelection(item.query) else onOpenItem(item)
                                        },
                                        onLongClick = {
                                            if (!state.isInSelectionMode) {
                                                state.enterSelectionWith(item.query)
                                            } else if (item.query !in state.selectedKeys) {
                                                state.selectedKeys = state.selectedKeys + item.query
                                            }
                                        }
                                    )
                                }
                            }
                            ListFastScroller(
                                listState = listState,
                                itemCount = displayed.size,
                                isInteractive = fastScrollerInteractive,
                                sectionLetterAt = { index -> sectionLetterForHistoryItem(displayed[index]) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                if (showDeleteFab) {
                    FloatingActionButton(
                        onClick = onDeleteSelectedClick,
                        containerColor = colors.buttonBackground, contentColor = colors.buttonIconTint,
                        modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 24.dp, end = 20.dp)
                    ) {
                        Icon(
                            androidx.compose.material.icons.Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.history_delete_selected_desc)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryToolbar(
    state: HistoryUiState,
    onBack: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onClearAllClick: () -> Unit
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
                    hint = stringResource(R.string.history_search_hint),
                    onClose = { state.isSearchMode = false; state.searchQuery = "" }
                )
            } else {
                Text(
                    text = if (state.isInSelectionMode) stringResource(R.string.history_selected_count, state.selectedCount) else stringResource(R.string.history_title),
                    color = colors.onSurface, fontSize = 19.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }

            if (showToolbarIcons) {
                IconButton(onClick = { state.isSearchMode = true }) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Search, contentDescription = stringResource(R.string.history_search), tint = colors.iconTint)
                }
                Box {
                    IconButton(onClick = { state.sortMenuOpen = true }) {
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.history_sort), tint = colors.primary)
                    }
                    SortMenu(
                        expanded = state.sortMenuOpen,
                        onDismiss = { state.sortMenuOpen = false },
                        sortKey = state.sortKey, sortOrder = state.sortOrder,
                        onSortByTitle = { state.sortKey = ListSortKey.TITLE; state.sortOrder = ListSortOrder.ASCENDING },
                        onSortByDateAdded = { state.sortKey = ListSortKey.DATE_ADDED; state.sortOrder = ListSortOrder.DESCENDING },
                        onSortAscending = { state.sortOrder = ListSortOrder.ASCENDING },
                        onSortDescending = { state.sortOrder = ListSortOrder.DESCENDING },
                        secondarySortLabel = stringResource(R.string.history_sort_by_last_visit)
                    )
                }
                Box {
                    IconButton(onClick = { state.historyActionsMenuOpen = true }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.MoreVert, contentDescription = stringResource(R.string.history_actions_desc), tint = colors.iconTint)
                    }
                    HistoryActionsMenu(
                        expanded = state.historyActionsMenuOpen,
                        onDismiss = { state.historyActionsMenuOpen = false },
                        onClearAllClick = onClearAllClick
                    )
                }
            }
            if (state.isInSelectionMode) {
                Box {
                    IconButton(onClick = { state.selectionOptionsMenuOpen = true }) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Checklist, contentDescription = stringResource(R.string.history_more_options), tint = colors.primary)
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
}

@Composable
private fun HistoryActionsMenu(expanded: Boolean, onDismiss: () -> Unit, onClearAllClick: () -> Unit) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke)
    ) {
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Delete, stringResource(R.string.history_clear_all), checked = false) {
            onDismiss(); onClearAllClick()
        }
    }
}

@Composable
private fun HistoryRow(
    item: HistoryItem,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val isUrl = item.query.startsWith("http")
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
        val favicon = if (isUrl) rememberClintFavicon(item.query) else null
        Box(Modifier.size(40.dp).clip(CircleShape).background(colors.surfaceVariant), contentAlignment = Alignment.Center) {
            if (favicon != null) {
                Image(bitmap = favicon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(22.dp))
            } else {
                Icon(
                    if (isUrl) androidx.compose.material.icons.Icons.Filled.Public else androidx.compose.material.icons.Icons.Filled.Search,
                    contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                if (isUrl) item.title.ifBlank { item.query } else item.query,
                color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (isUrl) {
                Text(
                    item.query.removePrefix("https://").removePrefix("http://"),
                    color = colors.secondaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (item.timestamp > 0L) {
                Text(
                    formatRelativeTimestamp(item.timestamp),
                    color = colors.secondaryText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

private fun filterAndSortHistory(
    items: List<HistoryItem>,
    query: String,
    sortKey: ListSortKey,
    sortOrder: ListSortOrder
): List<HistoryItem> {
    val filtered = if (query.isBlank()) items else {
        val q = query.trim().lowercase()
        items.filter { it.title.lowercase().contains(q) || it.query.lowercase().contains(q) }
    }
    val sorted = when (sortKey) {
        ListSortKey.TITLE -> filtered.sortedBy { it.title.ifBlank { it.query }.lowercase() }
        ListSortKey.DATE_ADDED -> filtered.sortedBy { it.timestamp }
    }
    return if (sortOrder == ListSortOrder.DESCENDING) sorted.reversed() else sorted
}

private fun sectionLetterForHistoryItem(item: HistoryItem): String {
    val display = item.title.ifBlank { item.query }.trimStart()
    val first = display.firstOrNull() ?: return "#"
    return if (first.isLetter()) first.uppercaseChar().toString() else if (first.isDigit()) first.toString() else "#"
}
