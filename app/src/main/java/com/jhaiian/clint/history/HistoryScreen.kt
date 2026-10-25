package com.jhaiian.clint.history
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import com.jhaiian.clint.R

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val GroupOuterRadius = 20.dp
private val GroupInnerRadius = 6.dp

private enum class HistoryDateBucket { TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, OLDER }

private sealed interface HistoryEntry {
    val key: String
    val sectionLetter: String

    data class Header(override val key: String, val label: HistorySectionLabel, override val sectionLetter: String) : HistoryEntry

    data class Row(
        val item: HistoryItem,
        val isFirst: Boolean,
        val isLast: Boolean,
        override val sectionLetter: String
    ) : HistoryEntry {
        override val key: String get() = "row:" + item.query
    }
}

private sealed interface HistorySectionLabel {
    data class Bucket(val bucket: HistoryDateBucket) : HistorySectionLabel
    data class Letter(val letter: String) : HistorySectionLabel
}

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    maxContentWidth: Dp?,
    onExit: () -> Unit,
    onOpenItem: (HistoryItem) -> Unit,
    onDeleteItem: (HistoryItem) -> Unit,
    onDeleteSelectedClick: () -> Unit,
    onClearAllClick: () -> Unit,
    onDeleteBrowsingDataClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val displayed = remember(state.items, state.searchQuery, state.sortKey, state.sortOrder) {
        filterAndSortHistory(state.items, state.searchQuery, state.sortKey, state.sortOrder)
    }
    val entries = remember(displayed, state.sortKey) { buildHistoryEntries(displayed, state.sortKey) }
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
                onClearAllClick = onClearAllClick,
                onDeleteBrowsingDataClick = onDeleteBrowsingDataClick
            )

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading -> Unit
                    displayed.isEmpty() -> HistoryEmptyState(
                        query = state.searchQuery.trim(),
                        modifier = Modifier.align(Alignment.Center)
                    )
                    else -> {
                        AdaptiveWidthContainer(maxContentWidth) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp)
                            ) {
                                items(entries, key = { it.key }, contentType = { it::class }) { entry ->
                                    when (entry) {
                                        is HistoryEntry.Header -> HistorySectionHeader(
                                            label = entry.label,
                                            modifier = Modifier.animateItem()
                                        )
                                        is HistoryEntry.Row -> HistoryRow(
                                            item = entry.item,
                                            showTimeOnly = state.sortKey == ListSortKey.DATE_ADDED,
                                            shape = groupShape(entry.isFirst, entry.isLast),
                                            isSelected = entry.item.query in state.selectedKeys,
                                            isInSelectionMode = state.isInSelectionMode,
                                            onClick = {
                                                if (state.isInSelectionMode) state.toggleSelection(entry.item.query) else onOpenItem(entry.item)
                                            },
                                            onLongClick = {
                                                if (!state.isInSelectionMode) {
                                                    state.enterSelectionWith(entry.item.query)
                                                } else if (entry.item.query !in state.selectedKeys) {
                                                    state.selectedKeys = state.selectedKeys + entry.item.query
                                                }
                                            },
                                            onDelete = { onDeleteItem(entry.item) },
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
                            }
                            ListFastScroller(
                                listState = listState,
                                itemCount = entries.size,
                                isInteractive = fastScrollerInteractive,
                                sectionLetterAt = { index -> entries.getOrNull(index)?.sectionLetter ?: "#" },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                if (showDeleteFab) {
                    ExtendedFloatingActionButton(
                        onClick = onDeleteSelectedClick,
                        containerColor = colors.buttonBackground,
                        contentColor = colors.buttonIconTint,
                        icon = {
                            Icon(
                                androidx.compose.material.icons.Icons.Filled.Delete,
                                contentDescription = null
                            )
                        },
                        text = { Text(stringResource(R.string.history_delete_selected), fontWeight = FontWeight.Medium) },
                        modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 24.dp, end = 20.dp)
                    )
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
    onClearAllClick: () -> Unit,
    onDeleteBrowsingDataClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val showToolbarIcons = !state.isInSelectionMode && !state.isSearchMode
    val toolbarColor by animateColorAsState(
        if (state.isInSelectionMode) lerp(colors.background, colors.primary, 0.14f) else colors.background,
        label = "historyToolbarColor"
    )

    Surface(color = toolbarColor, modifier = Modifier.statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    color = colors.onSurface, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
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
                        Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.history_sort), tint = colors.iconTint)
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
                        onClearAllClick = onClearAllClick,
                        onDeleteBrowsingDataClick = onDeleteBrowsingDataClick
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
private fun HistoryActionsMenu(expanded: Boolean, onDismiss: () -> Unit, onClearAllClick: () -> Unit, onDeleteBrowsingDataClick: () -> Unit) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke)
    ) {
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.DeleteSweep, stringResource(R.string.delete_data_title), checked = false) {
            onDismiss(); onDeleteBrowsingDataClick()
        }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Delete, stringResource(R.string.history_clear_all), checked = false) {
            onDismiss(); onClearAllClick()
        }
    }
}

@Composable
private fun HistorySectionHeader(label: HistorySectionLabel, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val text = when (label) {
        is HistorySectionLabel.Letter -> label.letter
        is HistorySectionLabel.Bucket -> stringResource(
            when (label.bucket) {
                HistoryDateBucket.TODAY -> R.string.downloads_group_today
                HistoryDateBucket.YESTERDAY -> R.string.downloads_group_yesterday
                HistoryDateBucket.THIS_WEEK -> R.string.downloads_group_this_week
                HistoryDateBucket.THIS_MONTH -> R.string.downloads_group_this_month
                HistoryDateBucket.OLDER -> R.string.downloads_group_older
            }
        )
    }
    Text(
        text = text,
        color = colors.primary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.3.sp,
        modifier = modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun HistoryRow(
    item: HistoryItem,
    showTimeOnly: Boolean,
    shape: Shape,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val isUrl = isHistoryUrl(item)
    val readableUrl = remember(item.query) { readableHistoryUrl(item.query) }
    val title = historyDisplayTitle(item)
    val timeLabel = remember(item.timestamp, showTimeOnly, locale) {
        if (item.timestamp > 0L) historyTimeLabel(context, item.timestamp, showTimeOnly, locale) else ""
    }
    val subtitle = listOfNotNull(
        readableUrl.substringBefore('/').takeIf { isUrl && it.isNotEmpty() },
        timeLabel.takeIf { it.isNotEmpty() }
    ).joinToString(" · ")
    val cardColor by animateColorAsState(
        if (isSelected) lerp(colors.cardBackground, colors.primary, 0.2f) else colors.cardBackground,
        label = "historyRowColor"
    )

    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(shape)
            .background(cardColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .heightIn(min = 64.dp)
            .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val favicon = if (isUrl) rememberClintFavicon(item.query) else null
        Box(
            Modifier
                .size(40.dp)
                .clip(if (isSelected) CircleShape else RoundedCornerShape(12.dp))
                .background(if (isSelected) colors.primary else colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            when {
                isSelected -> Icon(
                    androidx.compose.material.icons.Icons.Filled.Check,
                    contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(22.dp)
                )
                favicon != null -> Image(
                    bitmap = favicon.asImageBitmap(), contentDescription = null,
                    modifier = Modifier.size(22.dp).clip(RoundedCornerShape(4.dp))
                )
                else -> Icon(
                    if (isUrl) androidx.compose.material.icons.Icons.Filled.Public else androidx.compose.material.icons.Icons.Filled.Search,
                    contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 4.dp)) {
            Text(
                title,
                color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    color = colors.secondaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
        if (isInSelectionMode) {
            Spacer(Modifier.width(12.dp))
        } else {
            IconButton(onClick = onDelete) {
                Icon(
                    androidx.compose.material.icons.Icons.Filled.Close,
                    contentDescription = stringResource(R.string.history_delete_desc),
                    tint = colors.secondaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun HistoryEmptyState(query: String, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val isSearching = query.isNotEmpty()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 32.dp)
    ) {
        Box(
            Modifier.size(88.dp).clip(CircleShape).background(lerp(colors.background, colors.primary, 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isSearching) androidx.compose.material.icons.Icons.Filled.SearchOff else androidx.compose.material.icons.Icons.Filled.History,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            if (isSearching) stringResource(R.string.history_no_results, query) else stringResource(R.string.history_empty),
            color = colors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        if (!isSearching) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.history_empty_hint),
                color = colors.secondaryText, fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun groupShape(isFirst: Boolean, isLast: Boolean): Shape = RoundedCornerShape(
    topStart = if (isFirst) GroupOuterRadius else GroupInnerRadius,
    topEnd = if (isFirst) GroupOuterRadius else GroupInnerRadius,
    bottomStart = if (isLast) GroupOuterRadius else GroupInnerRadius,
    bottomEnd = if (isLast) GroupOuterRadius else GroupInnerRadius
)

private fun buildHistoryEntries(displayed: List<HistoryItem>, sortKey: ListSortKey): List<HistoryEntry> {
    if (displayed.isEmpty()) return emptyList()
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val groups = mutableListOf<Pair<HistorySectionLabel, MutableList<HistoryItem>>>()
    for (item in displayed) {
        val label = when (sortKey) {
            ListSortKey.TITLE -> HistorySectionLabel.Letter(sectionLetterForHistoryItem(item))
            ListSortKey.DATE_ADDED -> HistorySectionLabel.Bucket(historyBucketFor(item.timestamp, todayStart))
        }
        if (groups.lastOrNull()?.first == label) groups.last().second.add(item) else groups.add(label to mutableListOf(item))
    }
    val entries = mutableListOf<HistoryEntry>()
    groups.forEachIndexed { groupIndex, (label, items) ->
        val letter = items.firstOrNull()?.let(::sectionLetterForHistoryItem) ?: "#"
        entries.add(HistoryEntry.Header("header:$groupIndex:$label", label, letter))
        items.forEachIndexed { index, item ->
            entries.add(
                HistoryEntry.Row(
                    item = item,
                    isFirst = index == 0,
                    isLast = index == items.lastIndex,
                    sectionLetter = sectionLetterForHistoryItem(item)
                )
            )
        }
    }
    return entries
}

private fun historyBucketFor(timestamp: Long, todayStart: Long): HistoryDateBucket {
    val day = 24L * 60L * 60L * 1000L
    return when {
        timestamp >= todayStart -> HistoryDateBucket.TODAY
        timestamp >= todayStart - day -> HistoryDateBucket.YESTERDAY
        timestamp >= todayStart - 6 * day -> HistoryDateBucket.THIS_WEEK
        timestamp >= todayStart - 29 * day -> HistoryDateBucket.THIS_MONTH
        else -> HistoryDateBucket.OLDER
    }
}

private fun historyTimeLabel(context: Context, millis: Long, timeOnlyWhenRecent: Boolean, locale: Locale): String {
    val date = Date(millis)
    val time = DateFormat.getTimeFormat(context).format(date)
    val itemCal = Calendar.getInstance().apply { timeInMillis = millis }
    val nowCal = Calendar.getInstance()
    val sameDay = itemCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
        itemCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
    if (sameDay || (timeOnlyWhenRecent && isYesterday(itemCal, nowCal))) return time
    val skeleton = if (itemCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) "EEEMMMd" else "MMMdyyyy"
    val datePart = DateFormat.format(DateFormat.getBestDateTimePattern(locale, skeleton), date).toString()
    return "$datePart · $time"
}

private fun isYesterday(itemCal: Calendar, nowCal: Calendar): Boolean {
    val yesterday = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    return itemCal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
        itemCal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
}

private fun isHistoryUrl(item: HistoryItem): Boolean =
    item.query.startsWith("http://") || item.query.startsWith("https://")

private fun historyDisplayTitle(item: HistoryItem): String =
    if (isHistoryUrl(item)) item.title.ifBlank { readableHistoryUrl(item.query) } else item.query

private fun readableHistoryUrl(url: String): String =
    url.removePrefix("https://").removePrefix("http://").removePrefix("www.").removeSuffix("/")

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
        ListSortKey.TITLE -> filtered.sortedBy { historyDisplayTitle(it).lowercase() }
        ListSortKey.DATE_ADDED -> filtered.sortedBy { it.timestamp }
    }
    return if (sortOrder == ListSortOrder.DESCENDING) sorted.reversed() else sorted
}

private fun sectionLetterForHistoryItem(item: HistoryItem): String {
    val display = historyDisplayTitle(item).trimStart()
    val first = display.firstOrNull() ?: return "#"
    return if (first.isLetter()) first.uppercaseChar().toString() else if (first.isDigit()) first.toString() else "#"
}
