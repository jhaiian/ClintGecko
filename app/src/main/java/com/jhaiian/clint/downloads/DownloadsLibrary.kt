package com.jhaiian.clint.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatFileSize
import com.jhaiian.clint.util.formatRelativeTimestamp
import java.util.Calendar

private enum class LibraryBucket { TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, OLDER }

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

private fun libraryTimestamp(item: DownloadItem): Long = item.completedAt.takeIf { it > 0L } ?: item.startedAt

private fun isGalleryKind(item: DownloadItem): Boolean {
    val kind = downloadFileKind(item.filename)
    return (kind == DownloadFileKind.IMAGE || kind == DownloadFileKind.VIDEO) &&
        item.filename.substringAfterLast('.', "").lowercase() !in DownloadThumbnailLoader.SUBTITLE_EXTENSIONS
}

@Composable
internal fun DownloadsLibraryContent(
    state: DownloadsUiState,
    items: List<DownloadItem>,
    gridState: LazyGridState,
    maxContentWidth: Dp?,
    onItemClick: (DownloadItem) -> Unit,
    onItemLongClick: (DownloadItem) -> Unit,
    onShowSheet: (DownloadItem) -> Unit,
    onShare: (DownloadItem) -> Unit,
    onRemove: (DownloadItem) -> Unit
) {
    val colors = LocalClintColors.current
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
                grid = state.libraryGrid,
                onSelect = { state.libraryKind = it },
                onToggleGrid = { state.libraryGrid = !state.libraryGrid }
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
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 108.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 112.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item(key = "summary", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        pluralStringResource(R.plurals.downloads_library_summary, visible.size, visible.size, formatFileSize(visible.sumOf { it.bytesDownloaded })),
                        color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
                grouped.forEach { (bucket, groupItems) ->
                    if (bucket != null) {
                        item(key = "h_${bucket.name}", span = { GridItemSpan(maxLineSpan) }) {
                            Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 14.dp, bottom = 2.dp), verticalAlignment = Alignment.Bottom) {
                                Text(bucketLabel(bucket), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(
                                    pluralStringResource(R.plurals.downloads_library_summary, groupItems.size, groupItems.size, formatFileSize(groupItems.sumOf { it.bytesDownloaded })),
                                    color = colors.secondaryText, fontSize = 12.sp
                                )
                            }
                        }
                    }
                    val tiles = if (state.libraryGrid) groupItems.filter(::isGalleryKind) else emptyList()
                    val rows = if (state.libraryGrid) groupItems.filterNot(::isGalleryKind) else groupItems
                    items(tiles, key = { it.id }) { item ->
                        LibraryTile(
                            item = item,
                            isSelected = item.id in state.selectedIds,
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongClick(item) }
                        )
                    }
                    items(rows, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { item ->
                        LibraryRow(
                            item = item,
                            isSelected = item.id in state.selectedIds,
                            isInSelectionMode = state.isInSelectionMode,
                            onClick = { onItemClick(item) },
                            onLongClick = { onItemLongClick(item) },
                            onMore = { onShowSheet(item) },
                            onShare = { onShare(item) },
                            onRemove = { onRemove(item) }
                        )
                    }
                }
            }
        }
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
    grid: Boolean,
    onSelect: (DownloadFileKind?) -> Unit,
    onToggleGrid: () -> Unit
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
        IconButton(onClick = onToggleGrid, modifier = Modifier.padding(end = 6.dp)) {
            Icon(
                if (grid) Icons.AutoMirrored.Filled.ViewList else Icons.Filled.GridView,
                contentDescription = stringResource(if (grid) R.string.downloads_view_list else R.string.downloads_view_gallery),
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
private fun LibraryTile(item: DownloadItem, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val colors = LocalClintColors.current
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        DownloadThumb(
            item = item,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .then(if (isSelected) Modifier.border(2.dp, colors.primary, RoundedCornerShape(16.dp)) else Modifier),
            requestPx = rememberPx(160.dp),
            corner = 16.dp,
            iconSize = 30.dp,
            selected = isSelected
        )
        Text(
            item.filename,
            color = colors.onSurface, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, softWrap = false, overflow = TextOverflow.MiddleEllipsis,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 5.dp, bottom = 2.dp)
        )
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

@Composable
private fun LibraryRow(
    item: DownloadItem,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMore: () -> Unit,
    onShare: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = LocalClintColors.current
    val shape = RoundedCornerShape(18.dp)
    val requestPx = rememberPx(56.dp)
    val thumbnail = rememberDownloadThumbnail(item, requestPx)
    val title = thumbnail?.appLabel ?: thumbnail?.title?.takeIf { downloadFileKind(item.filename) == DownloadFileKind.AUDIO }
    val meta = (libraryMetaParts(item, thumbnail) + listOfNotNull(
        sourceHost(item),
        libraryTimestamp(item).takeIf { it > 0L }?.let { formatRelativeTimestamp(it) }
    )).joinToString("  •  ")
    SwipeActionsBox(
        enabled = !isInSelectionMode,
        shape = shape,
        startAction = SwipeAction(stringResource(R.string.download_menu_share), Icons.Filled.Share, colors.primary, colors.onPrimary, onShare),
        endAction = SwipeAction(stringResource(R.string.action_delete), Icons.Filled.Delete, colors.colorErrorContainer, colors.colorOnErrorContainer, onRemove),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (isSelected) colors.primary.copy(alpha = 0.16f) else colors.cardBackground)
                .then(if (isSelected) Modifier.border(1.5.dp, colors.primary, shape) else Modifier)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DownloadThumb(
                item = item,
                modifier = Modifier.size(56.dp),
                requestPx = requestPx,
                corner = 14.dp,
                iconSize = 26.dp,
                showDuration = false,
                selected = isSelected,
                thumbnail = thumbnail
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                if (title != null) {
                    Text(title, color = colors.onSurface, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    Text(item.filename, color = colors.secondaryText, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2)
                } else {
                    Text(item.filename, color = colors.onSurface, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 3)
                }
                Text(meta, color = colors.secondaryText, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 3.dp))
            }
            if (!isInSelectionMode) {
                IconButton(onClick = onMore) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.download_more_desc), tint = colors.iconTint)
                }
            }
        }
    }
}
