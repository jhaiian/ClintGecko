package com.jhaiian.clint.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.rememberSheetFlingConnection
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatRelativeTimestamp

private class SheetAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun DownloadItemSheet(
    item: DownloadItem,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    itemActions: DownloadItemActions,
    onPause: (Int) -> Unit,
    onResume: (Int) -> Unit,
    onRetry: (Int) -> Unit,
    onViewLog: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isComplete = item.status == DownloadStatus.COMPLETE
    val kind = downloadFileKind(item.filename)
    val requestPx = rememberPx(360.dp)
    val thumbnail = rememberDownloadThumbnail(item, requestPx)
    val info = downloadStatusInfo(item, onPause, onResume, onRetry)
    fun dismissThen(action: () -> Unit): () -> Unit = { onDismiss(); action() }

    val available = buildMap<SheetActionId, SheetAction> {
        if (isComplete) {
            put(SheetActionId.FOLDER, SheetAction(stringResource(SheetActionId.FOLDER.label), SheetActionId.FOLDER.icon, dismissThen { itemActions.onOpenFolder(item) }))
            put(SheetActionId.RENAME, SheetAction(stringResource(SheetActionId.RENAME.label), SheetActionId.RENAME.icon, dismissThen { itemActions.onRename(item) }))
        } else {
            if (item.status == DownloadStatus.QUEUED) {
                put(SheetActionId.DOWNLOAD_NEXT, SheetAction(stringResource(SheetActionId.DOWNLOAD_NEXT.label), SheetActionId.DOWNLOAD_NEXT.icon, dismissThen { ClintDownloadManager.moveToFrontOfQueue(item.id) }))
            }
            put(SheetActionId.SETTINGS, SheetAction(stringResource(SheetActionId.SETTINGS.label), SheetActionId.SETTINGS.icon, dismissThen { itemActions.onChangeSettings(item) }))
            if (item.mega == null) {
                put(SheetActionId.UPDATE_LINK, SheetAction(stringResource(SheetActionId.UPDATE_LINK.label), SheetActionId.UPDATE_LINK.icon, dismissThen { itemActions.onUpdateLink(item) }))
                put(SheetActionId.UPDATE_LINK_BROWSER, SheetAction(stringResource(SheetActionId.UPDATE_LINK_BROWSER.label), SheetActionId.UPDATE_LINK_BROWSER.icon, dismissThen { itemActions.onUpdateLinkInBrowser(item) }))
            }
        }
        put(SheetActionId.REDOWNLOAD, SheetAction(stringResource(SheetActionId.REDOWNLOAD.label), SheetActionId.REDOWNLOAD.icon, dismissThen { itemActions.onRedownload(item) }))
        put(SheetActionId.REDOWNLOAD_CUSTOM, SheetAction(stringResource(SheetActionId.REDOWNLOAD_CUSTOM.label), SheetActionId.REDOWNLOAD_CUSTOM.icon, dismissThen { itemActions.onRedownloadOptions(item) }))
        put(SheetActionId.COPY_LINK, SheetAction(stringResource(SheetActionId.COPY_LINK.label), SheetActionId.COPY_LINK.icon, dismissThen { itemActions.onCopyLink(item) }))
        put(SheetActionId.COPY_NAME, SheetAction(stringResource(SheetActionId.COPY_NAME.label), SheetActionId.COPY_NAME.icon, dismissThen { itemActions.onCopyFilename(item) }))
        put(SheetActionId.COPY_PATH, SheetAction(stringResource(SheetActionId.COPY_PATH.label), SheetActionId.COPY_PATH.icon, dismissThen { itemActions.onCopyPath(item) }))
        put(SheetActionId.PROPERTIES, SheetAction(stringResource(SheetActionId.PROPERTIES.label), SheetActionId.PROPERTIES.icon, dismissThen { itemActions.onProperties(item) }))
        if (DownloadLog.enabled || DownloadLog.hasLog(context, item.id)) {
            put(SheetActionId.LOG, SheetAction(stringResource(SheetActionId.LOG.label), SheetActionId.LOG.icon, dismissThen(onViewLog)))
        }
    }
    val actions = LocalDownloadAppearance.current.sheetActions.mapNotNull { available[it] }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.popupBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.divider) }
    ) {
        com.jhaiian.clint.ui.ClintDialogStatusBarEffect(hideStatusBar, hideSystemNavigation)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .nestedScroll(rememberSheetFlingConnection(sheetState, consumeFling = false))
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val wide = kind == DownloadFileKind.VIDEO || kind == DownloadFileKind.IMAGE || (kind == DownloadFileKind.DOCUMENT && thumbnail?.bitmap != null)
                if (isComplete && thumbnail?.bitmap != null && wide) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(if (kind == DownloadFileKind.DOCUMENT) 1.4f else 16f / 9f)
                            .clip(RoundedCornerShape(22.dp)).clickable(onClick = dismissThen { itemActions.onOpen(item) }),
                        contentAlignment = Alignment.Center
                    ) {
                        DownloadThumb(item = item, modifier = Modifier.fillMaxSize(), requestPx = requestPx, corner = 22.dp, iconSize = 48.dp, thumbnail = thumbnail)
                        if (kind == DownloadFileKind.VIDEO) {
                            Box(Modifier.size(58.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.action_open), tint = Color.White, modifier = Modifier.size(30.dp))
                            }
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isComplete) {
                            DownloadThumb(item = item, modifier = Modifier.size(96.dp), requestPx = requestPx, corner = 22.dp, iconSize = 40.dp, thumbnail = thumbnail)
                        } else {
                            Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                                ProgressRing(
                                    progress = info.progress,
                                    color = if (info.isError) colors.colorError else colors.primary,
                                    modifier = Modifier.fillMaxSize(),
                                    strokeWidth = 5.dp
                                )
                                Text(
                                    info.percent?.let { "$it%" } ?: "",
                                    color = colors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                        Column(Modifier.weight(1f).padding(start = 16.dp)) {
                            val heading = thumbnail?.appLabel ?: thumbnail?.title
                            if (heading != null) {
                                Text(heading, color = colors.onSurface, fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Text(
                                if (isComplete) thumbnail?.artist ?: kindLabel(kind) else info.headline,
                                color = if (info.isError) colors.colorError else colors.secondaryText, fontSize = 14.sp, lineHeight = 19.sp
                            )
                        }
                    }
                }

                Text(item.filename, color = colors.onSurface, fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold)

                val chips = buildList {
                    item.filename.substringAfterLast('.', "").takeIf { it.isNotBlank() && it.length <= 6 }?.let { add(it.uppercase()) }
                    addAll(libraryMetaParts(item, if (isComplete) thumbnail else null, includeSize = isComplete))
                }
                if (chips.isNotEmpty() || !isComplete) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        chips.forEach { StatChip(it) }
                        if (!isComplete) info.chips.forEach { StatChip(it.text, it.icon, it.emphasized) }
                    }
                }

                if (!isComplete && info.detail != null) {
                    Text(
                        info.detail, color = if (info.isError) colors.colorError else colors.secondaryText, fontSize = 13.sp, lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (info.isError) colors.colorError.copy(alpha = 0.10f) else colors.primary.copy(alpha = 0.06f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                val provenance = listOfNotNull(
                    sourceHost(item),
                    (item.completedAt.takeIf { it > 0L } ?: item.startedAt).takeIf { it > 0L }?.let { formatRelativeTimestamp(it) },
                    if (isComplete && item.activeElapsedMs >= 1000L) stringResource(R.string.downloads_sheet_took, formatElapsed(item.activeElapsedMs / 1000L)) else null
                )
                if (provenance.isNotEmpty()) {
                    Text(provenance.joinToString("  •  "), color = colors.secondaryText, fontSize = 13.sp, lineHeight = 18.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isComplete) {
                        PillButton(stringResource(R.string.action_open), Icons.AutoMirrored.Filled.OpenInNew, filled = true, onClick = dismissThen { itemActions.onOpen(item) }, modifier = Modifier.weight(1f))
                        PillButton(stringResource(R.string.download_menu_share), Icons.Filled.Share, filled = false, onClick = dismissThen { itemActions.onShare(item) }, modifier = Modifier.weight(1f))
                    } else {
                        val action = info.action
                        if (action != null) {
                            PillButton(info.actionLabel, info.actionIcon, filled = true, onClick = action, modifier = Modifier.weight(1f))
                        }
                        PillButton(stringResource(R.string.download_menu_open_folder), Icons.Filled.FolderOpen, filled = false, onClick = dismissThen { itemActions.onOpenFolder(item) }, modifier = Modifier.weight(1f))
                    }
                }

                actions.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { action -> SheetActionTile(action, Modifier.weight(1f)) }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }

                PillButton(
                    label = stringResource(R.string.downloads_sheet_delete),
                    icon = Icons.Filled.Delete,
                    filled = false,
                    danger = true,
                    onClick = dismissThen { itemActions.onRemove(item) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DownloadsSelectionSheet(
    count: Int,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onRedownload: () -> Unit,
    onCopyLink: () -> Unit,
    onCopyName: () -> Unit,
    onCopyPath: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    fun dismissThen(action: () -> Unit): () -> Unit = { onDismiss(); action() }
    val actions = listOf(
        SheetAction(stringResource(R.string.downloads_sheet_redownload), Icons.Filled.Refresh, dismissThen(onRedownload)),
        SheetAction(stringResource(R.string.downloads_sheet_copy_link), Icons.Filled.Link, dismissThen(onCopyLink)),
        SheetAction(stringResource(R.string.downloads_sheet_copy_name), Icons.Filled.ContentCopy, dismissThen(onCopyName)),
        SheetAction(stringResource(R.string.downloads_sheet_copy_path), Icons.Filled.ContentCopy, dismissThen(onCopyPath))
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.popupBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.divider) }
    ) {
        com.jhaiian.clint.ui.ClintDialogStatusBarEffect(hideStatusBar, hideSystemNavigation)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .nestedScroll(rememberSheetFlingConnection(sheetState, consumeFling = false))
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    stringResource(R.string.downloads_selected_count, count),
                    color = colors.onSurface, fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.ExtraBold
                )
                Row(Modifier.fillMaxWidth()) {
                    actions.forEach { action -> SheetActionTile(action, Modifier.weight(1f)) }
                }
                PillButton(
                    label = stringResource(R.string.action_delete),
                    icon = Icons.Filled.Delete,
                    filled = false,
                    danger = true,
                    onClick = dismissThen(onDelete),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SheetActionTile(action: SheetAction, modifier: Modifier) {
    val colors = LocalClintColors.current
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = action.onClick).padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(50.dp).clip(RoundedCornerShape(17.dp)).background(colors.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(action.icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
        }
        Text(
            action.label,
            color = colors.onSurface, fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
