package com.jhaiian.clint.settings.site
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import com.jhaiian.clint.ui.listscreen.ClintSearchField
import com.jhaiian.clint.ui.listscreen.ListFastScroller
import com.jhaiian.clint.ui.listscreen.ListSortKey
import com.jhaiian.clint.ui.listscreen.ListSortOrder
import com.jhaiian.clint.ui.listscreen.SelectionOptionsMenu
import com.jhaiian.clint.ui.listscreen.SortMenu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.rememberClintFavicon
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun SiteListScreen(
    state: SiteListUiState,
    maxContentWidth: Dp?,
    title: String,
    searchHint: String,
    emptyText: String,
    stateLabel: @Composable (state: String) -> Pair<String, Color>,
    onExit: () -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: () -> Unit,
    header: (@Composable ColumnScope.() -> Unit)? = null
) {
    val colors = LocalClintColors.current
    val displayed = remember(state.allItems, state.searchQuery, state.sortKey, state.sortOrder) {
        filterAndSortSites(state.allItems, state.searchQuery, state.sortKey, state.sortOrder)
    }
    val listState = rememberLazyListState()
    val fastScrollerInteractive = !state.isSearchMode && state.sortKey == ListSortKey.TITLE
    val showDeleteFab = state.isInSelectionMode && state.selectedOrigins.isNotEmpty()
    val showAddFab = !state.isInSelectionMode

    fun handleBack() {
        when {
            state.isSearchMode -> { state.isSearchMode = false; state.searchQuery = "" }
            state.isInSelectionMode -> state.exitSelectionMode()
            else -> onExit()
        }
    }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            SiteListToolbar(
                state = state,
                title = title,
                searchHint = searchHint,
                onBack = ::handleBack,
                onSelectAll = { state.selectAll(displayed) },
                onInvertSelection = { state.invertSelection(displayed) }
            )
            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Box(Modifier.weight(1f).fillMaxWidth()) {
                AdaptiveWidthContainer(maxContentWidth) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp)
                    ) {
                        header?.let { header -> item { Column(content = header) } }
                        if (displayed.isEmpty()) {
                            item {
                                Text(
                                    emptyText,
                                    color = colors.secondaryText,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp)
                                )
                            }
                        } else {
                            items(displayed, key = { it.origin }) { entry ->
                                SiteListRow(
                                    entry = entry,
                                    isSelected = entry.origin in state.selectedOrigins,
                                    stateLabel = stateLabel,
                                    onClick = { if (state.isInSelectionMode) state.toggleSelection(entry.origin) },
                                    onLongClick = {
                                        if (!state.isInSelectionMode) {
                                            state.enterSelectionWith(entry.origin)
                                        } else if (entry.origin !in state.selectedOrigins) {
                                            state.selectedOrigins = state.selectedOrigins + entry.origin
                                        }
                                    }
                                )
                            }
                        }
                    }
                    if (displayed.isNotEmpty()) {
                        ListFastScroller(
                            listState = listState,
                            itemCount = displayed.size,
                            isInteractive = fastScrollerInteractive,
                            sectionLetterAt = { index -> sectionLetterFor(displayed[index], state.sortKey) },
                            headerItemCount = if (header != null) 1 else 0,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                if (showDeleteFab) {
                    FloatingActionButton(
                        onClick = onDeleteClick,
                        containerColor = colors.buttonBackground,
                        contentColor = colors.buttonIconTint,
                        modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 24.dp, end = 20.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Delete, contentDescription = null)
                    }
                }
                if (showAddFab) {
                    FloatingActionButton(
                        onClick = onAddClick,
                        containerColor = colors.buttonBackground,
                        contentColor = colors.buttonIconTint,
                        modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 24.dp, end = 20.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Add, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun SiteListToolbar(
    state: SiteListUiState,
    title: String,
    searchHint: String,
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
                    if (state.isInSelectionMode) androidx.compose.material.icons.Icons.Filled.Close else androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null, tint = colors.onSurface
                )
            }

            if (state.isSearchMode) {
                ClintSearchField(
                    query = state.searchQuery,
                    onQueryChange = { state.searchQuery = it },
                    hint = searchHint,
                    onClose = { state.isSearchMode = false; state.searchQuery = "" }
                )
            } else {
                Text(
                    text = if (state.isInSelectionMode) stringResourceSelectedCount(state.selectedOrigins.size) else title,
                    color = colors.onSurface,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
                if (!state.isInSelectionMode) {
                    Box {
                        IconButton(onClick = { state.sortMenuOpen = true }) {
                            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = colors.iconTint)
                        }
                        SortMenu(
                            expanded = state.sortMenuOpen,
                            onDismiss = { state.sortMenuOpen = false },
                            sortKey = state.sortKey,
                            sortOrder = state.sortOrder,
                            onSortByTitle = { state.sortKey = ListSortKey.TITLE; state.sortOrder = ListSortOrder.ASCENDING },
                            onSortByDateAdded = { state.sortKey = ListSortKey.DATE_ADDED; state.sortOrder = ListSortOrder.DESCENDING },
                            onSortAscending = { state.sortOrder = ListSortOrder.ASCENDING },
                            onSortDescending = { state.sortOrder = ListSortOrder.DESCENDING }
                        )
                    }
                }
                if (state.isInSelectionMode) {
                    Box {
                        IconButton(onClick = { state.moreOptionsMenuOpen = true }) {
                            Icon(androidx.compose.material.icons.Icons.Filled.Checklist, contentDescription = null, tint = colors.iconTint)
                        }
                        SelectionOptionsMenu(
                            expanded = state.moreOptionsMenuOpen,
                            onDismiss = { state.moreOptionsMenuOpen = false },
                            onSelectAll = onSelectAll,
                            onInvertSelection = onInvertSelection,
                            onDeselectAll = { state.deselectAll() }
                        )
                    }
                }
                IconButton(onClick = { state.isSearchMode = true }) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Search, contentDescription = null, tint = colors.iconTint)
                }
            }
        }
    }
}

@Composable
private fun stringResourceSelectedCount(count: Int): String =
    stringResource(R.string.history_selected_count, count)

@Composable
private fun SiteListRow(
    entry: SiteEntry,
    isSelected: Boolean,
    stateLabel: @Composable (String) -> Pair<String, Color>,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val favicon = rememberClintFavicon("https://${entry.origin}")
    val (label, labelColor) = stateLabel(entry.state)
    val cardColor = if (isSelected) lerp(colors.cardBackground, colors.primary, 0.22f) else colors.cardBackground

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(colors.surfaceVariant), contentAlignment = Alignment.Center) {
            if (favicon != null) {
                Image(bitmap = favicon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(22.dp))
            } else {
                Icon(androidx.compose.material.icons.Icons.Filled.Public, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(22.dp))
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(entry.origin, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, color = labelColor, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
