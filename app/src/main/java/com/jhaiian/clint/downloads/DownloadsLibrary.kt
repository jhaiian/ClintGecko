package com.jhaiian.clint.downloads

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatFileSize
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

private enum class LibraryBucket { TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, OLDER }

private class LibrarySectionStats(val bucket: LibraryBucket?, val count: Int, val bytes: Long)

private fun bucketFor(timestamp: Long, todayStart: Long): LibraryBucket {
    val day = 24L * 60L * 60L * 1000L
    return when {
        timestamp >= todayStart -> LibraryBucket.TODAY
        timestamp >= todayStart - day -> LibraryBucket.YESTERDAY
        timestamp >= todayStart - 6 * day -> LibraryBucket.THIS_WEEK
        timestamp >= todayStart - 29 * day -> LibraryBucket.THIS_MONTH
        else -> LibraryBucket.OLDER
    }
}

internal fun libraryTimestamp(item: DownloadItem): Long = item.completedAt.takeIf { it > 0L } ?: item.startedAt

@Composable
internal fun DownloadsLibraryContent(
    state: DownloadsUiState,
    items: List<DownloadItem>,
    gridState: LazyGridState,
    maxContentWidth: Dp?,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onItemClick: (DownloadItem) -> Unit,
    onItemLongClick: (DownloadItem) -> Unit,
    onShowSheet: (DownloadItem) -> Unit,
    onShare: (DownloadItem) -> Unit,
    onRemove: (DownloadItem) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val scope = rememberCoroutineScope()
    val morph = remember(scope) { ViewModeMorphState(scope) }
    val kindCounts = remember(items) { items.groupingBy { downloadFileKind(it.filename) }.eachCount() }
    val kind = state.libraryKind
    val visible = remember(items, kind) { if (kind == null) items else items.filter { downloadFileKind(it.filename) == kind } }
    val grouped = remember(visible, state.sortKey) {
        if (state.sortKey != DownloadSortKey.DATE) listOf<Pair<LibraryBucket?, List<DownloadItem>>>(null to visible)
        else {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val buckets = visible.groupBy { bucketFor(libraryTimestamp(it), todayStart) }
            LibraryBucket.values().mapNotNull { bucket -> buckets[bucket]?.let { bucket to it } }
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (items.isNotEmpty()) {
            LibraryFilterRow(
                kindCounts = kindCounts,
                total = items.size,
                selected = kind,
                viewMode = state.libraryViewMode,
                onSelect = { state.libraryKind = it },
                onOpenViewModes = { state.libraryViewModeDialogOpen = true }
            )
        }
        if (visible.isEmpty()) {
            DownloadsEmptyState(
                title = stringResource(if (items.isEmpty() && state.searchQuery.isBlank()) R.string.downloads_library_empty_title else R.string.downloads_empty_filtered),
                hint = if (items.isEmpty() && state.searchQuery.isBlank()) stringResource(R.string.downloads_library_empty_hint) else null,
                actionLabel = null,
                onAction = {}
            )
            return@Column
        }
        AdaptiveWidthContainer(maxContentWidth) {
            LibraryMorphList(
                state = state,
                morph = morph,
                prefs = prefs,
                grouped = grouped,
                gridState = gridState,
                onItemClick = onItemClick,
                onItemLongClick = onItemLongClick,
                onShowSheet = onShowSheet,
                onShare = onShare,
                onRemove = onRemove
            )
        }
    }

    if (state.libraryViewModeDialogOpen) {
        LibraryViewModeDialog(
            current = state.libraryViewMode,
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onDismiss = { state.libraryViewModeDialogOpen = false },
            onSelect = { mode ->
                state.libraryViewModeDialogOpen = false
                if (!morph.morphTo(mode)) {
                    state.libraryViewMode = mode
                    LibraryViewModeStore.save(prefs, mode)
                }
            }
        )
    }
}

@Composable
private fun LibraryMorphList(
    state: DownloadsUiState,
    morph: ViewModeMorphState,
    prefs: SharedPreferences,
    grouped: List<Pair<LibraryBucket?, List<DownloadItem>>>,
    gridState: LazyGridState,
    onItemClick: (DownloadItem) -> Unit,
    onItemLongClick: (DownloadItem) -> Unit,
    onShowSheet: (DownloadItem) -> Unit,
    onShare: (DownloadItem) -> Unit,
    onRemove: (DownloadItem) -> Unit
) {
    val colors = LocalClintColors.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val tiles = remember(grouped) { grouped.flatMap { it.second } }
    val stats = remember(grouped) { grouped.map { (bucket, list) -> LibrarySectionStats(bucket, list.size, list.sumOf { it.bytesDownloaded }) } }
    val sectionCounts = remember(stats) { stats.map { it.count } }
    val hasHeaders = stats.firstOrNull()?.bucket != null
    val totalBytes = remember(stats) { stats.sumOf { it.bytes } }
    val mode = state.libraryViewMode
    val bottomPx = with(density) { LibraryBottomPadding.roundToPx() }

    BoxWithConstraints(Modifier.fillMaxSize().viewModePinch(morph)) {
        val viewportWidth = constraints.maxWidth
        val viewportHeight = constraints.maxHeight
        val geometries = remember(viewportWidth, viewportHeight, bottomPx, sectionCounts, hasHeaders, density) {
            LibraryViewMode.entries.associateWith { entry ->
                computeModeGeometry(entry, viewportWidth, viewportHeight, bottomPx, sectionCounts, hasHeaders, density)
            }
        }
        val geometry = geometries.getValue(mode)
        val style = geometry.style
        val staticFrame = remember(style) { TileFrame(style, 0, 0) }
        val sidePadding = with(density) { geometry.horizontalPadding.toDp() }

        SideEffect {
            morph.pinchEnabled = !state.libraryViewModeDialogOpen
            morph.currentMode = { state.libraryViewMode }
            morph.onStep = { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
            morph.captureSession = capture@{
                if (tiles.isEmpty()) return@capture null
                val current = geometries.getValue(state.libraryViewMode)
                val scroll = current.scrollFromLazy(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset)
                scope.launch { gridState.stopScroll() }
                val anchor = current.anchorAt(scroll)
                MorphSession(
                    tiles = tiles,
                    anchorIndex = anchor,
                    anchorViewportY = (current.contentTopOf(anchor) - scroll).toFloat()
                )
            }
            morph.commit = { target ->
                val targetGeometry = geometries.getValue(target)
                val session = morph.session
                val scroll = if (session != null) targetGeometry.scrollFor(session).roundToInt() else 0
                val (index, offset) = targetGeometry.lazyPosition(scroll)
                gridState.requestScrollToItem(index, offset)
                state.libraryViewMode = target
                LibraryViewModeStore.save(prefs, target)
            }
        }

        Box(Modifier.fillMaxSize().graphicsLayer { alpha = if (morph.session != null) 0f else 1f }) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(geometry.columns),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = sidePadding, end = sidePadding, bottom = LibraryBottomPadding),
                horizontalArrangement = Arrangement.spacedBy(style.gridSpacing),
                verticalArrangement = Arrangement.spacedBy(style.gridSpacing)
            ) {
                item(key = "summary", span = { GridItemSpan(maxLineSpan) }) {
                    LibrarySummaryRow(tiles.size, totalBytes, LibraryHeaderInset - sidePadding)
                }
                stats.forEachIndexed { section, sectionStats ->
                    val groupItems = grouped[section].second
                    if (sectionStats.bucket != null) {
                        item(key = "h_${sectionStats.bucket.name}", span = { GridItemSpan(maxLineSpan) }) {
                            LibrarySectionHeaderRow(sectionStats.bucket, sectionStats.count, sectionStats.bytes, LibraryHeaderInset - sidePadding)
                        }
                    }
                    items(groupItems, key = { it.id }) { item ->
                        val isSelected = item.id in state.selectedIds
                        if (geometry.isGrid) {
                            LibraryTileView(
                                item = item,
                                selected = isSelected,
                                from = staticFrame,
                                to = null,
                                fraction = StaticFraction,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(style.artCorner))
                                    .combinedClickable(onClick = { onItemClick(item) }, onLongClick = { onItemLongClick(item) })
                            )
                        } else {
                            SwipeActionsBox(
                                enabled = !state.isInSelectionMode,
                                shape = RectangleShape,
                                startAction = SwipeAction(stringResource(R.string.download_menu_share), Icons.Filled.Share, colors.primary, colors.onPrimary) { onShare(item) },
                                endAction = SwipeAction(stringResource(R.string.action_delete), Icons.Filled.Delete, colors.colorErrorContainer, colors.colorOnErrorContainer) { onRemove(item) },
                                modifier = Modifier.exactSize(geometry.rowWidth, geometry.rowHeight)
                            ) {
                                Box(Modifier.fillMaxSize()) {
                                    LibraryTileView(
                                        item = item,
                                        selected = isSelected,
                                        from = staticFrame,
                                        to = null,
                                        fraction = StaticFraction,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .combinedClickable(onClick = { onItemClick(item) }, onLongClick = { onItemLongClick(item) })
                                    )
                                    if (!state.isInSelectionMode) {
                                        val moreSize = with(density) { minOf(style.trailingReserve, geometry.rowHeight.toDp()) }
                                        IconButton(
                                            onClick = { onShowSheet(item) },
                                            modifier = Modifier.align(Alignment.CenterEnd).size(moreSize)
                                        ) {
                                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.download_more_desc), tint = colors.iconTint)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        morph.session?.let { session ->
            ViewModeMorphOverlay(
                morph = morph,
                session = session,
                geometries = geometries,
                selectedIds = state.selectedIds,
                sectionCount = if (hasHeaders) stats.size else 0,
                summary = { LibrarySummaryRow(tiles.size, totalBytes, LibraryHeaderInset) },
                sectionHeader = { section ->
                    val sectionStats = stats[section]
                    LibrarySectionHeaderRow(sectionStats.bucket!!, sectionStats.count, sectionStats.bytes, LibraryHeaderInset)
                }
            )
        }
    }
}

private fun Modifier.exactSize(width: Int, height: Int): Modifier = layout { measurable, _ ->
    val placeable = measurable.measure(Constraints.fixed(width, height))
    layout(width, height) { placeable.place(0, 0) }
}

@Composable
private fun LibrarySummaryRow(count: Int, bytes: Long, inset: Dp) {
    val colors = LocalClintColors.current
    Box(
        Modifier.fillMaxWidth().height(LibrarySummaryHeight).padding(horizontal = inset),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            pluralStringResource(R.plurals.downloads_library_summary, count, count, formatFileSize(bytes)),
            color = colors.secondaryText, fontSize = 12.sp
        )
    }
}

@Composable
private fun LibrarySectionHeaderRow(bucket: LibraryBucket, count: Int, bytes: Long, inset: Dp) {
    val colors = LocalClintColors.current
    Row(
        Modifier.fillMaxWidth().height(LibrarySectionHeaderHeight).padding(start = inset, end = inset, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Text(bucketLabel(bucket), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(
            pluralStringResource(R.plurals.downloads_library_summary, count, count, formatFileSize(bytes)),
            color = colors.secondaryText, fontSize = 12.sp
        )
    }
}

@Composable
private fun bucketLabel(bucket: LibraryBucket): String = stringResource(
    when (bucket) {
        LibraryBucket.TODAY -> R.string.downloads_group_today
        LibraryBucket.YESTERDAY -> R.string.downloads_group_yesterday
        LibraryBucket.THIS_WEEK -> R.string.downloads_group_this_week
        LibraryBucket.THIS_MONTH -> R.string.downloads_group_this_month
        LibraryBucket.OLDER -> R.string.downloads_group_older
    }
)

@Composable
internal fun kindLabel(kind: DownloadFileKind): String = stringResource(
    when (kind) {
        DownloadFileKind.VIDEO -> R.string.downloads_type_video
        DownloadFileKind.AUDIO -> R.string.downloads_type_audio
        DownloadFileKind.IMAGE -> R.string.downloads_type_image
        DownloadFileKind.DOCUMENT -> R.string.downloads_type_document
        DownloadFileKind.ARCHIVE -> R.string.downloads_type_archive
        DownloadFileKind.APP -> R.string.downloads_type_app
        DownloadFileKind.OTHER -> R.string.downloads_type_other
    }
)

@Composable
private fun LibraryFilterRow(
    kindCounts: Map<DownloadFileKind, Int>,
    total: Int,
    selected: DownloadFileKind?,
    viewMode: LibraryViewMode,
    onSelect: (DownloadFileKind?) -> Unit,
    onOpenViewModes: () -> Unit
) {
    val colors = LocalClintColors.current
    val kinds = DownloadFileKind.values().filter { (kindCounts[it] ?: 0) > 0 || it == selected }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        LazyRow(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "all") {
                FilterChipPill(stringResource(R.string.downloads_filter_all), total, null, selected == null) { onSelect(null) }
            }
            kinds.forEach { kind ->
                item(key = kind.name) {
                    FilterChipPill(kindLabel(kind), kindCounts[kind] ?: 0, downloadKindIcon(kind), selected == kind) {
                        onSelect(if (selected == kind) null else kind)
                    }
                }
            }
        }
        IconButton(onClick = onOpenViewModes, modifier = Modifier.padding(end = 6.dp)) {
            Icon(
                if (viewMode.isGrid) Icons.Filled.GridView else Icons.AutoMirrored.Filled.ViewList,
                contentDescription = stringResource(R.string.downloads_view_mode),
                tint = colors.iconTint
            )
        }
    }
}

@Composable
internal fun FilterChipPill(label: String, count: Int?, icon: ImageVector?, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    val shape = RoundedCornerShape(50)
    val content = if (selected) colors.onPrimary else colors.onSurface
    Row(
        Modifier
            .height(36.dp)
            .clip(shape)
            .background(if (selected) colors.primary else colors.cardBackground)
            .then(if (selected) Modifier else Modifier.border(1.dp, colors.divider, shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = content, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text(count.toString(), color = content.copy(alpha = 0.65f), fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
internal fun libraryMetaParts(item: DownloadItem, thumbnail: DownloadThumbnail?, includeSize: Boolean = true): List<String> {
    val kind = downloadFileKind(item.filename)
    val details = mutableListOf<String>()
    if (includeSize) details += formatFileSize(item.bytesDownloaded)
    if (thumbnail != null) {
        when (kind) {
            DownloadFileKind.VIDEO -> {
                if (thumbnail.width > 0 && thumbnail.height > 0) details += "${thumbnail.width} × ${thumbnail.height}"
                if (thumbnail.durationMs > 0L) details += formatMediaDuration(thumbnail.durationMs)
            }
            DownloadFileKind.AUDIO -> {
                thumbnail.artist?.let { details += it }
                if (thumbnail.durationMs > 0L) details += formatMediaDuration(thumbnail.durationMs)
            }
            DownloadFileKind.IMAGE -> if (thumbnail.width > 0 && thumbnail.height > 0) details += "${thumbnail.width} × ${thumbnail.height}"
            DownloadFileKind.APP -> thumbnail.appVersion?.let { details += stringResource(R.string.downloads_meta_version, it) }
            else -> if (thumbnail.pageCount > 0) details += pluralStringResource(R.plurals.downloads_meta_pages, thumbnail.pageCount, thumbnail.pageCount)
        }
    }
    return details
}
