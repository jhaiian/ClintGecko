package com.jhaiian.clint.downloads

import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatFileSize
import com.jhaiian.clint.util.formatRelativeTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ChipSpec(val text: String, val icon: ImageVector?, val emphasized: Boolean = false)

internal class StatusInfo(
    val headline: String,
    val detail: String?,
    val hint: String?,
    val chips: List<ChipSpec>,
    val progress: DownloadRowProgress,
    val percent: Int?,
    val muted: Boolean,
    val isError: Boolean,
    val actionIcon: ImageVector?,
    val actionLabel: String,
    val action: (() -> Unit)?
)

private const val DOT = "  •  "

@Composable
internal fun downloadStatusInfo(
    item: DownloadItem,
    onPause: (Int) -> Unit,
    onResume: (Int) -> Unit,
    onRetry: (Int) -> Unit
): StatusInfo {
    val context = LocalContext.current
    val pauseLabel = stringResource(R.string.downloads_action_pause)
    val resumeLabel = stringResource(R.string.downloads_action_resume)
    val retryLabel = stringResource(R.string.downloads_action_retry)
    val sizeText = if (item.totalBytes > 0) formatFileSize(item.totalBytes) else null
    val bytesText = if (item.totalBytes > 0) {
        context.getString(R.string.downloads_bytes_of, formatFileSize(item.bytesDownloaded), formatFileSize(item.totalBytes))
    } else formatFileSize(item.bytesDownloaded)
    val elapsedMs = item.activeElapsedMs + if (item.activeStartedAt > 0L) System.currentTimeMillis() - item.activeStartedAt else 0L
    val elapsedChip = if (elapsedMs >= 1000L) ChipSpec(formatElapsed(elapsedMs / 1000L), Icons.Filled.Timer) else null
    val resumableChip = if (item.resumable) ChipSpec(context.getString(R.string.downloads_chip_resumable), Icons.Filled.Autorenew)
    else ChipSpec(context.getString(R.string.downloads_chip_not_resumable), Icons.Filled.Block)
    val pause = { onPause(item.id) }

    fun info(
        headline: String,
        progress: DownloadRowProgress = DownloadRowProgress.None,
        chips: List<ChipSpec> = emptyList(),
        detail: String? = null,
        hint: String? = null,
        percent: Int? = null,
        muted: Boolean = false,
        isError: Boolean = false,
        actionIcon: ImageVector? = null,
        actionLabel: String = "",
        action: (() -> Unit)? = null
    ) = StatusInfo(headline, detail, hint, chips, progress, percent, muted, isError, actionIcon, actionLabel, action)

    fun fraction(pct: Int): DownloadRowProgress = if (pct > 0) DownloadRowProgress.Determinate(pct / 100f) else DownloadRowProgress.Indeterminate

    return when (item.status) {
        DownloadStatus.QUEUED -> info(
            headline = listOfNotNull(context.getString(R.string.download_status_queued), sizeText).joinToString(DOT),
            actionIcon = Icons.Filled.Pause, actionLabel = pauseLabel, action = pause
        )
        DownloadStatus.CONNECTING -> info(
            headline = context.getString(R.string.download_status_connecting),
            progress = DownloadRowProgress.Indeterminate,
            chips = listOfNotNull(sizeText?.let { ChipSpec(it, Icons.Filled.Download) }),
            actionIcon = Icons.Filled.Pause, actionLabel = pauseLabel, action = pause
        )
        DownloadStatus.ALLOCATING -> info(
            headline = context.getString(R.string.download_status_allocating, item.allocationProgress),
            progress = fraction(item.allocationProgress),
            chips = listOfNotNull(sizeText?.let { ChipSpec(it, Icons.Filled.Download) })
        )
        DownloadStatus.RETRYING -> info(
            headline = if (item.retryDelaySec > 0) context.getString(R.string.download_status_retrying_in, item.retryDelaySec)
            else context.getString(R.string.download_status_retrying),
            progress = DownloadRowProgress.Indeterminate,
            chips = listOfNotNull(if (item.bytesDownloaded > 0) ChipSpec(bytesText, Icons.Filled.Download) else null, elapsedChip),
            detail = item.errorMessage?.takeIf { it.isNotBlank() },
            hint = if (item.retryAttempt >= 5 || item.lastErrorWasServerError) context.getString(R.string.download_retry_hint) else null,
            muted = true,
            actionIcon = Icons.Filled.Pause, actionLabel = pauseLabel, action = pause
        )
        DownloadStatus.DOWNLOADING -> {
            val isSegmented = item.segmentsTotal > 0
            val pct = if (isSegmented) item.segmentsCompleted * 100 / item.segmentsTotal else item.progressPercent
            val speed = item.speedBytesPerSec
            val remaining = item.totalBytes - item.bytesDownloaded
            val etaSpeed = item.averageSpeedBytesPerSec().takeIf { it > 0L } ?: speed
            val chips = listOfNotNull(
                if (speed > 0L) ChipSpec(context.getString(R.string.download_speed_only, formatFileSize(speed)), Icons.Filled.Bolt, emphasized = true) else null,
                if (speed > 0L && item.totalBytes > 0L && remaining > 0L && etaSpeed > 0L)
                    ChipSpec(context.getString(R.string.downloads_eta_left, formatEta(context, remaining / etaSpeed)), Icons.Filled.Schedule, emphasized = true) else null,
                streamStageLabel(context, item)?.let { ChipSpec(it, Icons.Filled.Layers) },
                elapsedChip,
                resumableChip
            )
            info(
                headline = bytesText,
                progress = if (pct >= 0) DownloadRowProgress.Determinate(pct / 100f) else DownloadRowProgress.Indeterminate,
                chips = chips,
                percent = pct.takeIf { it >= 0 },
                actionIcon = if (item.resumable) Icons.Filled.Pause else null, actionLabel = pauseLabel,
                action = if (item.resumable) pause else null
            )
        }
        DownloadStatus.DECRYPTING -> {
            val total = item.segmentsTotal.coerceAtLeast(1)
            info(
                headline = context.getString(R.string.download_status_decrypting),
                progress = DownloadRowProgress.Determinate(item.segmentsCompleted.toFloat() / total),
                chips = listOf(ChipSpec(context.getString(R.string.download_status_segment_progress, item.segmentsCompleted, item.segmentsTotal), Icons.Filled.Layers))
            )
        }
        DownloadStatus.MUXING -> info(
            headline = muxStatusLabel(context, item),
            progress = muxProgressFraction(item)?.let { DownloadRowProgress.Determinate(it) } ?: DownloadRowProgress.Indeterminate,
            chips = listOfNotNull(sizeText?.let { ChipSpec(it, Icons.Filled.Download) })
        )
        DownloadStatus.CONVERTING -> info(
            headline = context.getString(R.string.download_status_converting, item.muxProgress),
            progress = fraction(item.muxProgress),
            chips = listOfNotNull(sizeText?.let { ChipSpec(it, Icons.Filled.Download) })
        )
        DownloadStatus.COPYING_TEMP -> info(
            headline = context.getString(R.string.download_status_copying_temp, item.copyProgress),
            progress = DownloadRowProgress.Determinate(item.copyProgress / 100f),
            chips = listOfNotNull(sizeText?.let { ChipSpec(it, Icons.Filled.Download) })
        )
        DownloadStatus.DELETING_TEMP -> info(
            headline = context.getString(R.string.download_status_deleting_temp),
            progress = DownloadRowProgress.Indeterminate
        )
        DownloadStatus.PAUSED -> {
            val pct = item.progressPercent
            val waiting = item.waitingForUnmetered || item.waitingForNetwork || item.waitingForSchedule || item.waitingForCustomSchedule
            val label = when {
                item.waitingForNetwork -> context.getString(R.string.download_paused_waiting_network)
                item.waitingForUnmetered -> context.getString(R.string.download_paused_waiting_unmetered)
                item.waitingForSchedule -> context.getString(R.string.download_paused_waiting_schedule)
                item.waitingForCustomSchedule -> context.getString(R.string.download_paused_waiting_custom_schedule, formatScheduledDateTime(context, item.scheduledStartAtMillis))
                else -> context.getString(R.string.download_paused)
            }
            info(
                headline = label,
                progress = if (pct >= 0) DownloadRowProgress.Determinate(pct / 100f) else DownloadRowProgress.None,
                chips = listOfNotNull(ChipSpec(bytesText, Icons.Filled.Download), elapsedChip, resumableChip),
                percent = pct.takeIf { it >= 0 },
                muted = true,
                actionIcon = if (waiting) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                actionLabel = if (waiting) pauseLabel else resumeLabel,
                action = if (waiting) pause else { { onResume(item.id) } }
            )
        }
        DownloadStatus.COMPLETE -> {
            val sizeStr = formatFileSize(item.bytesDownloaded)
            info(
                headline = if (item.startedAt > 0L) context.getString(R.string.download_info_with_time, sizeStr, formatRelativeTimestamp(item.startedAt)) else sizeStr,
                progress = DownloadRowProgress.Determinate(1f)
            )
        }
        DownloadStatus.FAILED -> {
            val failed = context.getString(R.string.download_status_failed_label)
            info(
                headline = if (item.startedAt > 0L) context.getString(R.string.download_info_with_time, failed, formatRelativeTimestamp(item.startedAt)) else failed,
                detail = item.errorMessage ?: context.getString(R.string.download_error_unknown),
                hint = if (item.lastErrorWasServerError) context.getString(R.string.download_retry_hint) else null,
                chips = listOfNotNull(if (item.bytesDownloaded > 0) ChipSpec(bytesText, Icons.Filled.Download) else null),
                isError = true,
                actionIcon = Icons.Filled.Refresh, actionLabel = retryLabel, action = { onRetry(item.id) }
            )
        }
    }
}

@Composable
internal fun DownloadsQueueContent(
    state: DownloadsUiState,
    items: List<DownloadItem>,
    allItems: List<DownloadItem>,
    tick: Long,
    gridState: LazyGridState,
    maxContentWidth: Dp?,
    showPulse: Boolean,
    totalHistory: List<Long>,
    perItemHistory: Map<Int, List<Long>>,
    onPause: (Int) -> Unit,
    onResume: (Int) -> Unit,
    onRetry: (Int) -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onOpenLibrary: () -> Unit,
    onItemClick: (DownloadItem) -> Unit,
    onItemLongClick: (DownloadItem) -> Unit,
    onShowSheet: (DownloadItem) -> Unit,
    onChangeSettings: (DownloadItem) -> Unit,
    onRemove: (DownloadItem) -> Unit
) {
    val working = items.filter { it.status in DownloadStatus.ACTIVELY_WORKING }
    val queued = ClintDownloadManager.queuedInOrder(items)
    val paused = items.filter { it.status == DownloadStatus.PAUSED }
    val failed = items.filter { it.status == DownloadStatus.FAILED }

    if (items.isEmpty() && !showPulse) {
        DownloadsEmptyState(
            title = stringResource(if (state.searchQuery.isBlank()) R.string.downloads_queue_empty_title else R.string.downloads_empty_filtered),
            hint = if (state.searchQuery.isBlank()) stringResource(R.string.downloads_queue_empty_hint) else null,
            actionLabel = if (state.searchQuery.isBlank()) stringResource(R.string.downloads_open_library) else null,
            onAction = onOpenLibrary
        )
        return
    }

    val cardAction: @Composable (DownloadItem) -> Unit = { item ->
        DownloadCard(
            item = item,
            tick = tick,
            expanded = item.id in state.expandedIds,
            isSelected = item.id in state.selectedIds,
            isInSelectionMode = state.isInSelectionMode,
            history = perItemHistory[item.id].orEmpty(),
            onClick = { onItemClick(item) },
            onLongClick = { onItemLongClick(item) },
            onToggleExpand = { state.toggleExpanded(item.id) },
            onPause = onPause, onResume = onResume, onRetry = onRetry,
            onMore = { onShowSheet(item) },
            onChangeSettings = { onChangeSettings(item) },
            onRemove = { onRemove(item) }
        )
    }

    AdaptiveWidthContainer(maxContentWidth) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 340.dp),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 112.dp)
        ) {
            if (showPulse) {
                item(key = "pulse", span = { GridItemSpan(maxLineSpan) }) {
                    PulseCard(allItems = allItems, tick = tick, history = totalHistory, onPauseAll = onPauseAll, onResumeAll = onResumeAll)
                }
            }
            if (items.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(R.string.downloads_empty_filtered),
                        color = LocalClintColors.current.secondaryText, fontSize = 14.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp)
                    )
                }
            }
            if (working.isNotEmpty()) {
                item(key = "h_working", span = { GridItemSpan(maxLineSpan) }) { GroupHeader(stringResource(R.string.downloads_group_downloading), working.size) }
                items(working, key = { it.id }) { cardAction(it) }
            }
            if (queued.isNotEmpty()) {
                item(key = "h_queued", span = { GridItemSpan(maxLineSpan) }) {
                    GroupHeader(stringResource(R.string.downloads_group_up_next), queued.size, stringResource(R.string.downloads_up_next_hint))
                }
                items(queued, key = { it.id }) { item ->
                    UpNextRow(
                        item = item,
                        position = queued.indexOf(item) + 1,
                        isFirst = queued.firstOrNull()?.id == item.id,
                        isLast = queued.lastOrNull()?.id == item.id,
                        isSelected = item.id in state.selectedIds,
                        isInSelectionMode = state.isInSelectionMode,
                        onClick = { onItemClick(item) },
                        onLongClick = { onItemLongClick(item) },
                        onMoveUp = { ClintDownloadManager.moveInQueue(item.id, -1) },
                        onMoveDown = { ClintDownloadManager.moveInQueue(item.id, 1) },
                        onPause = { onPause(item.id) },
                        onRemove = { onRemove(item) },
                        onMore = { onShowSheet(item) }
                    )
                }
            }
            if (paused.isNotEmpty()) {
                item(key = "h_paused", span = { GridItemSpan(maxLineSpan) }) { GroupHeader(stringResource(R.string.downloads_group_paused), paused.size) }
                items(paused, key = { it.id }) { cardAction(it) }
            }
            if (failed.isNotEmpty()) {
                item(key = "h_failed", span = { GridItemSpan(maxLineSpan) }) { GroupHeader(stringResource(R.string.downloads_group_failed), failed.size) }
                items(failed, key = { it.id }) { cardAction(it) }
            }
        }
    }
}

@Composable
internal fun GroupHeader(title: String, count: Int?, trailing: String? = null) {
    val colors = LocalClintColors.current
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (count != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                count.toString(),
                color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(colors.primary.copy(alpha = 0.12f)).padding(horizontal = 7.dp, vertical = 1.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, color = colors.secondaryText, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
private fun PulseCard(
    allItems: List<DownloadItem>,
    tick: Long,
    history: List<Long>,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val workingCount = allItems.count { it.status in DownloadStatus.ACTIVELY_WORKING }
    val transferring = allItems.any { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.CONNECTING || it.status == DownloadStatus.RETRYING }
    val queuedCount = allItems.count { it.status == DownloadStatus.QUEUED }
    val pausedCount = allItems.count { it.status == DownloadStatus.PAUSED }
    val speed = allItems.sumOf { if (it.status == DownloadStatus.DOWNLOADING) it.speedBytesPerSec else 0L }
    val remaining = allItems.filter { it.status in DownloadStatus.NOT_FINISHED && it.totalBytes > 0L }
        .sumOf { (it.totalBytes - it.bytesDownloaded).coerceAtLeast(0L) }
    val peak = history.maxOrNull() ?: 0L
    val freeBytes by produceState(initialValue = -1L, tick / 15L) {
        value = withContext(Dispatchers.IO) {
            runCatching { StatFs(Environment.getExternalStorageDirectory().path).availableBytes }.getOrDefault(-1L)
        }
    }
    val speedText = formatFileSize(speed)
    val speedNumber = speedText.substringBeforeLast(' ', speedText)
    val speedUnit = context.getString(R.string.download_speed_only, speedText.substringAfterLast(' ', "")).trim()

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(colors.cardBackground)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (transferring) colors.primary else colors.secondaryText))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(if (transferring) R.string.downloads_pulse_live else R.string.downloads_pulse_idle),
                color = colors.secondaryText, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
                modifier = Modifier.weight(1f)
            )
            if (freeBytes >= 0L) {
                Text(
                    stringResource(R.string.downloads_pulse_free_space, formatFileSize(freeBytes)),
                    color = colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(colors.primary.copy(alpha = 0.10f)).padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(speedNumber, color = colors.onSurface, fontSize = 44.sp, lineHeight = 46.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.2).sp)
            Spacer(Modifier.width(6.dp))
            Text(speedUnit, color = colors.secondaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
            Spacer(Modifier.weight(1f))
            if (peak > 0L) {
                Text(
                    stringResource(R.string.downloads_pulse_peak, formatFileSize(peak)),
                    color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        SpeedGraph(samples = history, color = colors.primary, modifier = Modifier.fillMaxWidth().height(56.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PulseStat(
                value = pluralStringResource(R.plurals.downloads_pulse_active, workingCount, workingCount),
                label = pluralStringResource(R.plurals.downloads_pulse_queued, queuedCount, queuedCount),
                modifier = Modifier.weight(1f)
            )
            PulseStat(
                value = if (remaining > 0L) formatFileSize(remaining) else "—",
                label = stringResource(R.string.downloads_pulse_left),
                modifier = Modifier.weight(1f)
            )
            PulseStat(
                value = if (speed > 0L && remaining > 0L) formatEta(context, remaining / speed) else "—",
                label = stringResource(R.string.downloads_pulse_eta),
                modifier = Modifier.weight(1f)
            )
        }
        if (workingCount + queuedCount > 0 || pausedCount > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (workingCount + queuedCount > 0) {
                    PillButton(
                        label = stringResource(R.string.downloads_pause_all),
                        icon = Icons.Filled.Pause,
                        filled = true,
                        onClick = onPauseAll,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pausedCount > 0) {
                    PillButton(
                        label = stringResource(R.string.downloads_resume_all),
                        icon = Icons.Filled.PlayArrow,
                        filled = workingCount + queuedCount == 0,
                        onClick = onResumeAll,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PulseStat(value: String, label: String, modifier: Modifier) {
    val colors = LocalClintColors.current
    Column(modifier) {
        Text(value, color = colors.onSurface, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = colors.secondaryText, fontSize = 11.sp, lineHeight = 14.sp)
    }
}

@Composable
internal fun PillButton(
    label: String,
    icon: ImageVector?,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    danger: Boolean = false
) {
    val colors = LocalClintColors.current
    val background = when {
        danger -> colors.colorErrorContainer
        filled -> colors.primary
        else -> colors.primary.copy(alpha = 0.12f)
    }
    val content = when {
        danger -> colors.colorOnErrorContainer
        filled -> colors.onPrimary
        else -> colors.onSurface
    }
    Row(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = content, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DownloadCard(
    item: DownloadItem,
    tick: Long,
    expanded: Boolean,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    history: List<Long>,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleExpand: () -> Unit,
    onPause: (Int) -> Unit,
    onResume: (Int) -> Unit,
    onRetry: (Int) -> Unit,
    onMore: () -> Unit,
    onChangeSettings: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = LocalClintColors.current
    val info = downloadStatusInfo(item, onPause, onResume, onRetry)
    val parts = downloadPartFractions(item)
    val shape = RoundedCornerShape(22.dp)
    val accent = if (info.isError) colors.colorError else if (info.muted) colors.secondaryText else colors.primary

    SwipeActionsBox(
        enabled = !isInSelectionMode,
        shape = shape,
        startAction = info.action?.let { action ->
            SwipeAction(info.actionLabel, info.actionIcon ?: Icons.Filled.PlayArrow, colors.primary, colors.onPrimary, action)
        },
        endAction = SwipeAction(stringResource(R.string.action_delete), Icons.Filled.Delete, colors.colorErrorContainer, colors.colorOnErrorContainer, onRemove),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (isSelected) colors.primary.copy(alpha = 0.16f) else colors.cardBackground)
                .then(if (isSelected) Modifier.border(1.5.dp, colors.primary, shape) else Modifier)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                    ProgressRing(progress = info.progress, color = accent, modifier = Modifier.fillMaxSize())
                    DownloadThumb(
                        item = item,
                        modifier = Modifier.size(42.dp),
                        requestPx = rememberPx(42.dp),
                        corner = 12.dp,
                        iconSize = 22.dp,
                        showDuration = false,
                        selected = isSelected
                    )
                }
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text(item.filename, color = colors.onSurface, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, maxLines = 3)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                        if (info.percent != null) {
                            Text("${info.percent}%", color = accent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(info.headline, color = if (info.isError) colors.colorError else colors.secondaryText, fontSize = 13.sp, lineHeight = 17.sp)
                    }
                }
                if (info.action != null && info.actionIcon != null) {
                    Box(
                        Modifier.size(46.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)).clickable(onClick = info.action),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(info.actionIcon, contentDescription = info.actionLabel, tint = accent, modifier = Modifier.size(22.dp))
                    }
                }
            }
            if (info.detail != null) {
                Text(
                    info.detail,
                    color = if (info.isError) colors.colorError else colors.secondaryText, fontSize = 13.sp, lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(
                        if (info.isError) colors.colorError.copy(alpha = 0.10f) else colors.primary.copy(alpha = 0.06f)
                    ).padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            if (info.hint != null) {
                Text(info.hint, color = colors.secondaryText, fontSize = 12.sp, lineHeight = 16.sp)
            }
            if (info.chips.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    info.chips.forEach { StatChip(it.text, it.icon, it.emphasized) }
                }
            }
            if (parts != null && !expanded) {
                val strip = remember(parts) { SegmentMap.downsample(parts, SegmentMap.STRIP_BUCKETS) }
                PartsStrip(
                    fractions = strip, color = accent, cellHeight = 8.dp,
                    gap = when {
                        strip.size > 64 -> 0.dp
                        strip.size > 32 -> 1.dp
                        else -> 2.dp
                    }
                )
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onToggleExpand).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (parts != null && item.isStream) stringResource(R.string.downloads_segments_summary, item.segmentsCompleted, item.segmentsTotal)
                    else if (parts != null) stringResource(
                        R.string.downloads_parts_summary,
                        if (DownloadAutoTuner.isAuto(item.multithreadingParts)) item.activeConnections else item.multithreadingParts,
                        parts.count { it >= 1f }, parts.size
                    )
                    else stringResource(if (expanded) R.string.downloads_collapse else R.string.downloads_expand),
                    color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(if (expanded) R.string.downloads_collapse else R.string.downloads_expand),
                    tint = colors.secondaryText, modifier = Modifier.size(20.dp)
                )
            }
            AnimatedVisibility(visible = expanded) {
                ExpandedDetails(item = item, info = info, parts = parts, history = history, accent = accent, onMore = onMore, onChangeSettings = onChangeSettings, onRemove = onRemove)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpandedDetails(
    item: DownloadItem,
    info: StatusInfo,
    parts: List<Float>?,
    history: List<Long>,
    accent: androidx.compose.ui.graphics.Color,
    onMore: () -> Unit,
    onChangeSettings: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (history.size >= 2) {
            val nonZero = history.filter { it > 0L }
            val average = if (nonZero.isNotEmpty()) nonZero.sum() / nonZero.size else 0L
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.primary.copy(alpha = 0.06f)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row {
                    Text(stringResource(R.string.downloads_speed_graph_title), color = colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.downloads_speed_avg_peak, formatFileSize(average), formatFileSize(history.max())),
                        color = colors.secondaryText, fontSize = 12.sp
                    )
                }
                SpeedGraph(samples = history, color = accent, modifier = Modifier.fillMaxWidth().height(72.dp))
            }
        }
        if (parts != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (item.isStream) stringResource(R.string.downloads_segments_summary, item.segmentsCompleted, item.segmentsTotal)
                    else stringResource(R.string.downloads_parts_title),
                    color = colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
                if (item.isStream && parts.size > 16) {
                    val cell = when {
                        parts.size > 1024 -> 5.dp
                        parts.size > 256 -> 6.dp
                        else -> 8.dp
                    }
                    val gap = if (parts.size > 256) 1.dp else 2.dp
                    PartsStrip(fractions = parts, color = accent, cellHeight = cell, gap = gap, minCellWidth = cell, maxHeight = 240.dp)
                } else {
                    PartsStrip(fractions = parts, color = accent, columns = 16, cellHeight = 14.dp, gap = 3.dp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    LegendDot(accent, stringResource(R.string.downloads_parts_done))
                    LegendDot(accent.copy(alpha = 0.55f), stringResource(R.string.downloads_parts_active))
                    LegendDot(colors.primary.copy(alpha = 0.12f), stringResource(R.string.downloads_parts_waiting))
                }
            }
        }
        val host = sourceHost(item)
        if (host != null) {
            Text(host, color = colors.secondaryText, fontSize = 12.sp)
        }
        if (item.status in DownloadStatus.NOT_FINISHED) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SettingChip(
                    text = if (item.speedLimitBytesPerSec > 0L) context.getString(R.string.download_speed_only, formatFileSize(item.speedLimitBytesPerSec))
                    else stringResource(R.string.downloads_setting_no_limit),
                    icon = Icons.Filled.Speed, active = item.speedLimitBytesPerSec > 0L, onClick = onChangeSettings
                )
                SettingChip(
                    text = stringResource(R.string.downloads_setting_wifi_only),
                    icon = Icons.Filled.Wifi, active = item.unmeteredOnly, onClick = onChangeSettings
                )
                if (!item.isStream) {
                    SettingChip(
                        text = when {
                            !DownloadAutoTuner.isAuto(item.multithreadingParts) ->
                                pluralStringResource(R.plurals.downloads_setting_connections, item.multithreadingParts, item.multithreadingParts)
                            item.activeConnections > 0 -> stringResource(
                                R.string.downloads_setting_auto_value,
                                pluralStringResource(R.plurals.downloads_setting_connections, item.activeConnections, item.activeConnections)
                            )
                            else -> stringResource(R.string.download_setting_auto)
                        },
                        icon = Icons.Filled.Layers, active = false, onClick = onChangeSettings
                    )
                }
                SettingChip(text = stringResource(R.string.downloads_setting_more), icon = Icons.Filled.Tune, active = false, onClick = onChangeSettings)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (info.action != null) {
                PillButton(label = info.actionLabel, icon = info.actionIcon, filled = true, onClick = info.action, modifier = Modifier.weight(1f))
            }
            PillButton(
                label = stringResource(if (item.status == DownloadStatus.FAILED) R.string.action_delete else R.string.action_cancel),
                icon = null, filled = false, onClick = onRemove, modifier = Modifier.weight(1f)
            )
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(colors.primary.copy(alpha = 0.12f)).clickable(onClick = onMore),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.download_more_desc), tint = colors.onSurface)
            }
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    val colors = LocalClintColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, color = colors.secondaryText, fontSize = 11.sp)
    }
}

@Composable
private fun SettingChip(text: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (active) colors.primary.copy(alpha = 0.18f) else colors.primary.copy(alpha = 0.07f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (active) colors.primary else colors.secondaryText, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = if (active) colors.primary else colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun UpNextRow(
    item: DownloadItem,
    position: Int,
    isFirst: Boolean,
    isLast: Boolean,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onPause: () -> Unit,
    onRemove: () -> Unit,
    onMore: () -> Unit
) {
    val colors = LocalClintColors.current
    val shape = RoundedCornerShape(18.dp)
    val meta = listOfNotNull(
        if (item.totalBytes > 0) formatFileSize(item.totalBytes) else null,
        if (item.unmeteredOnly) stringResource(R.string.downloads_setting_wifi_only) else null,
        sourceHost(item)
    ).joinToString(DOT)
    SwipeActionsBox(
        enabled = !isInSelectionMode,
        shape = shape,
        startAction = SwipeAction(stringResource(R.string.downloads_action_pause), Icons.Filled.Pause, colors.primary, colors.onPrimary, onPause),
        endAction = SwipeAction(stringResource(R.string.action_delete), Icons.Filled.Delete, colors.colorErrorContainer, colors.colorOnErrorContainer, onRemove),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (isSelected) colors.primary.copy(alpha = 0.16f) else colors.cardBackground)
                .then(if (isSelected) Modifier.border(1.5.dp, colors.primary, shape) else Modifier)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(start = 10.dp, end = 2.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                position.toString(),
                color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
                modifier = Modifier.width(22.dp)
            )
            DownloadThumb(item = item, modifier = Modifier.size(42.dp), requestPx = rememberPx(42.dp), iconSize = 20.dp, selected = isSelected)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(item.filename, color = colors.onSurface, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(
                    listOf(stringResource(R.string.download_status_queued), meta).filter { it.isNotBlank() }.joinToString(DOT),
                    color = colors.secondaryText, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (!isInSelectionMode) {
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.downloads_move_up), tint = if (isFirst) colors.divider else colors.iconTint)
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.downloads_move_down), tint = if (isLast) colors.divider else colors.iconTint)
                }
                IconButton(onClick = onMore) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.download_more_desc), tint = colors.iconTint)
                }
            }
        }
    }
}

@Composable
internal fun DownloadsEmptyState(title: String, hint: String?, actionLabel: String?, onAction: () -> Unit) {
    val colors = LocalClintColors.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(84.dp).clip(RoundedCornerShape(28.dp)).background(colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, tint = colors.primary, modifier = Modifier.size(38.dp))
            }
            Text(
                title, color = colors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp)
            )
            if (hint != null) {
                Text(
                    hint, color = colors.secondaryText, fontSize = 14.sp, lineHeight = 19.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (actionLabel != null) {
                PillButton(label = actionLabel, icon = null, filled = false, onClick = onAction, modifier = Modifier.padding(top = 20.dp))
            }
        }
    }
}
