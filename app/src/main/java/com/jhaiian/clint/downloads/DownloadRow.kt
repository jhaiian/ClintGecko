package com.jhaiian.clint.downloads
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoFile

import com.jhaiian.clint.R

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatFileSize
import com.jhaiian.clint.util.formatRelativeTimestamp

@Composable
fun DownloadRow(
    item: DownloadItem,
    tick: Long,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPause: (Int) -> Unit,
    onResume: (Int) -> Unit,
    onRetry: (Int) -> Unit,
    itemActions: DownloadItemActions
) {
    val colors = LocalClintColors.current
    var optionsMenuOpen by remember { mutableStateOf(false) }
    val cardColor = if (isSelected) lerp(colors.cardBackground, colors.primary, 0.55f) else colors.cardBackground

    val display = downloadRowDisplay(item, tick, onPause, onResume, onRetry)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cardColor)
            .downloadProgressBackground(if (isSelected) DownloadCardProgress.None else display.cardProgress, colors.primary)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(
                    fileTypeIconRes(item.filename),
                    contentDescription = stringResource(R.string.download_icon_desc),
                    tint = colors.secondaryText,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.filename,
                        color = colors.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                        maxLines = 1, softWrap = false, overflow = TextOverflow.MiddleEllipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (display.resumableText != null) {
                        Text(
                            display.resumableText, color = colors.secondaryText, fontSize = 9.sp, maxLines = 1,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
                    Text(
                        display.statusText, color = colors.secondaryText, fontSize = 9.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                    if (display.metaText != null) {
                        Text(
                            display.metaText, color = colors.secondaryText, fontSize = 9.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
                if (display.retryHintText != null) {
                    Text(
                        display.retryHintText, color = colors.secondaryText, fontSize = 9.sp,
                        lineHeight = 12.sp, modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
            if (display.pauseIconRes != null && display.pauseAction != null) {
                IconButton(onClick = display.pauseAction, modifier = Modifier.padding(start = 4.dp)) {
                    Icon(display.pauseIconRes, contentDescription = display.pauseContentDesc, tint = colors.iconTint.copy(alpha = 0.6f))
                }
            }
            if (display.moreVisible) {
                Box {
                    IconButton(onClick = { optionsMenuOpen = true }, modifier = Modifier.padding(start = 4.dp)) {
                        Icon(
                            androidx.compose.material.icons.Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.download_more_desc),
                            tint = colors.iconTint.copy(alpha = 0.6f)
                        )
                    }
                    DownloadItemOptionsMenu(
                        expanded = optionsMenuOpen,
                        item = item,
                        onDismiss = { optionsMenuOpen = false },
                        onOpen = { itemActions.onOpen(item) },
                        onShare = { itemActions.onShare(item) },
                        onOpenFolder = { itemActions.onOpenFolder(item) },
                        onRename = { itemActions.onRename(item) },
                        onRedownload = { itemActions.onRedownload(item) },
                        onRedownloadOptions = { itemActions.onRedownloadOptions(item) },
                        onChangeSettings = { itemActions.onChangeSettings(item) },
                        onUpdateLink = { itemActions.onUpdateLink(item) },
                        onUpdateLinkInBrowser = { itemActions.onUpdateLinkInBrowser(item) },
                        onRemove = { itemActions.onRemove(item) },
                        onCopyLink = { itemActions.onCopyLink(item) },
                        onCopyFilename = { itemActions.onCopyFilename(item) },
                        onCopyPath = { itemActions.onCopyPath(item) },
                        onProperties = { itemActions.onProperties(item) }
                    )
                }
            }
        }
    }
}

private data class RowDisplay(
    val statusText: String,
    val metaText: String?,
    val retryHintText: String?,
    val resumableText: String?,
    val cardProgress: DownloadCardProgress,
    val pauseIconRes: androidx.compose.ui.graphics.vector.ImageVector?,
    val pauseContentDesc: String,
    val pauseAction: (() -> Unit)?,
    val moreVisible: Boolean
)

@Composable
private fun downloadRowDisplay(
    item: DownloadItem,
    tick: Long,
    onPause: (Int) -> Unit,
    onResume: (Int) -> Unit,
    onRetry: (Int) -> Unit
): RowDisplay {
    val context = LocalContext.current
    val pauseDesc = stringResource(R.string.download_pause_desc)
    val resumeDesc = stringResource(R.string.download_resume_desc)

    fun resumableText(): String = if (item.resumable) context.getString(R.string.download_resumable_yes) else context.getString(R.string.download_resumable_no)

    return when (item.status) {
        DownloadStatus.QUEUED -> RowDisplay(
            statusText = "",
            metaText = context.getString(R.string.download_status_queued),
            retryHintText = null,
            resumableText = null,
            cardProgress = DownloadCardProgress.None,
            pauseIconRes = androidx.compose.material.icons.Icons.Filled.Pause, pauseContentDesc = pauseDesc, pauseAction = { onPause(item.id) },
            moreVisible = false
        )
        DownloadStatus.ALLOCATING -> {
            val pct = item.allocationProgress
            RowDisplay(
                statusText = if (item.totalBytes > 0) formatFileSize(item.totalBytes) else "",
                metaText = context.getString(R.string.download_status_allocating, pct),
                retryHintText = null, resumableText = null,
                cardProgress = if (pct > 0) DownloadCardProgress.Determinate(pct / 100f) else DownloadCardProgress.Indeterminate,
                pauseIconRes = null, pauseContentDesc = "", pauseAction = null,
                moreVisible = false
            )
        }
        DownloadStatus.CONNECTING -> RowDisplay(
            statusText = if (item.totalBytes > 0) formatFileSize(item.totalBytes) else "",
            metaText = context.getString(R.string.download_status_connecting),
            retryHintText = null, resumableText = null,
            cardProgress = DownloadCardProgress.Indeterminate,
            pauseIconRes = androidx.compose.material.icons.Icons.Filled.Pause, pauseContentDesc = pauseDesc, pauseAction = { onPause(item.id) },
            moreVisible = false
        )
        DownloadStatus.RETRYING -> {
            val delaySec = item.retryDelaySec
            val pct = item.progressPercent
            val statusText = when {
                !item.errorMessage.isNullOrBlank() -> item.errorMessage
                item.bytesDownloaded > 0 && pct >= 0 && item.totalBytes > 0 ->
                    context.getString(R.string.download_status_progress, pct, formatFileSize(item.bytesDownloaded), formatFileSize(item.totalBytes))
                item.bytesDownloaded > 0 && pct >= 0 ->
                    context.getString(R.string.download_status_progress_unknown_total, pct, formatFileSize(item.bytesDownloaded))
                item.bytesDownloaded > 0 -> context.getString(R.string.download_status_progress_indeterminate, formatFileSize(item.bytesDownloaded))
                item.totalBytes > 0 -> formatFileSize(item.totalBytes)
                else -> ""
            }
            RowDisplay(
                statusText = statusText,
                metaText = if (delaySec > 0) context.getString(R.string.download_status_retrying_in, delaySec) else context.getString(R.string.download_status_retrying),
                retryHintText = if (item.retryAttempt >= 5 || item.lastErrorWasServerError) context.getString(R.string.download_retry_hint) else null,
                resumableText = null,
                cardProgress = DownloadCardProgress.Indeterminate,
                pauseIconRes = androidx.compose.material.icons.Icons.Filled.Pause, pauseContentDesc = pauseDesc, pauseAction = { onPause(item.id) },
                moreVisible = false
            )
        }
        DownloadStatus.DOWNLOADING -> {
            val isSegmented = item.segmentsTotal > 0
            val pct = if (isSegmented) (item.segmentsCompleted * 100 / item.segmentsTotal) else item.progressPercent
            val downloaded = formatFileSize(item.bytesDownloaded)
            val statusText = if (pct >= 0) {
                if (item.totalBytes > 0) context.getString(R.string.download_status_progress, pct, downloaded, formatFileSize(item.totalBytes))
                else context.getString(R.string.download_status_progress_unknown_total, pct, downloaded)
            } else context.getString(R.string.download_status_progress_indeterminate, downloaded)
            val speedEtaText = buildSpeedEtaText(context, item)
            val segmentText = if (isSegmented) context.getString(R.string.download_status_segment_progress, item.segmentsCompleted, item.segmentsTotal) else null
            val combinedMeta = listOfNotNull(segmentText, speedEtaText)
            RowDisplay(
                statusText = statusText,
                metaText = if (combinedMeta.isEmpty()) null else combinedMeta.joinToString("  \u2022  "),
                retryHintText = null,
                resumableText = resumableText(),
                cardProgress = if (pct >= 0) DownloadCardProgress.Determinate(pct / 100f) else DownloadCardProgress.Indeterminate,
                pauseIconRes = if (item.resumable) androidx.compose.material.icons.Icons.Filled.Pause else null, pauseContentDesc = pauseDesc,
                pauseAction = if (item.resumable) { { onPause(item.id) } } else null,
                moreVisible = false
            )
        }
        DownloadStatus.DECRYPTING -> {
            val total = item.segmentsTotal.coerceAtLeast(1)
            val pct = item.segmentsCompleted * 100 / total
            RowDisplay(
                statusText = context.getString(R.string.download_status_segment_progress, item.segmentsCompleted, item.segmentsTotal),
                metaText = context.getString(R.string.download_status_decrypting),
                retryHintText = null, resumableText = null,
                cardProgress = DownloadCardProgress.Determinate(pct / 100f),
                pauseIconRes = null, pauseContentDesc = "", pauseAction = null,
                moreVisible = false
            )
        }
        DownloadStatus.MUXING -> {
            RowDisplay(
                statusText = if (item.totalBytes > 0) formatFileSize(item.totalBytes) else "",
                metaText = context.getString(R.string.download_status_muxing),
                retryHintText = null, resumableText = null,
                cardProgress = DownloadCardProgress.Indeterminate,
                pauseIconRes = null, pauseContentDesc = "", pauseAction = null,
                moreVisible = false
            )
        }
        DownloadStatus.CONVERTING -> {
            val pct = item.muxProgress
            RowDisplay(
                statusText = if (item.totalBytes > 0) formatFileSize(item.totalBytes) else "",
                metaText = context.getString(R.string.download_status_converting, pct),
                retryHintText = null, resumableText = null,
                cardProgress = if (pct > 0) DownloadCardProgress.Determinate(pct / 100f) else DownloadCardProgress.Indeterminate,
                pauseIconRes = null, pauseContentDesc = "", pauseAction = null,
                moreVisible = false
            )
        }
        DownloadStatus.PAUSED -> {
            val pct = item.progressPercent
            val statusText = if (pct >= 0) {
                if (item.totalBytes > 0) context.getString(R.string.download_status_progress, pct, formatFileSize(item.bytesDownloaded), formatFileSize(item.totalBytes))
                else context.getString(R.string.download_status_progress_unknown_total, pct, formatFileSize(item.bytesDownloaded))
            } else context.getString(R.string.download_status_progress_indeterminate, formatFileSize(item.bytesDownloaded))
            val pausedLabel = when {
                item.waitingForNetwork -> context.getString(R.string.download_paused_waiting_network)
                item.waitingForUnmetered -> context.getString(R.string.download_paused_waiting_unmetered)
                item.waitingForSchedule -> context.getString(R.string.download_paused_waiting_schedule)
                item.waitingForCustomSchedule -> context.getString(R.string.download_paused_waiting_custom_schedule, formatScheduledDateTime(context, item.scheduledStartAtMillis))
                else -> context.getString(R.string.download_paused)
            }
            val elapsedSec = item.activeElapsedMs / 1000L
            val waiting = item.waitingForUnmetered || item.waitingForNetwork || item.waitingForSchedule || item.waitingForCustomSchedule
            RowDisplay(
                statusText = statusText,
                metaText = if (elapsedSec >= 1L) "$pausedLabel  \u2022  ${formatElapsed(elapsedSec)}" else pausedLabel,
                retryHintText = null,
                resumableText = resumableText(),
                cardProgress = if (pct >= 0) DownloadCardProgress.Determinate(pct / 100f) else DownloadCardProgress.None,
                pauseIconRes = if (waiting) androidx.compose.material.icons.Icons.Filled.Pause else androidx.compose.material.icons.Icons.Filled.PlayArrow,
                pauseContentDesc = if (waiting) pauseDesc else resumeDesc,
                pauseAction = if (waiting) { { onPause(item.id) } } else { { onResume(item.id) } },
                moreVisible = false
            )
        }
        DownloadStatus.COMPLETE -> {
            val sizeStr = formatFileSize(item.bytesDownloaded)
            val elapsedSec = item.activeElapsedMs / 1000L
            RowDisplay(
                statusText = if (item.startedAt > 0L) context.getString(R.string.download_info_with_time, sizeStr, formatRelativeTimestamp(item.startedAt)) else sizeStr,
                metaText = if (elapsedSec >= 1L) "${context.getString(R.string.download_status_complete_label)}  \u2022  ${formatElapsed(elapsedSec)}" else context.getString(R.string.download_status_complete_label),
                retryHintText = null, resumableText = null,
                cardProgress = DownloadCardProgress.Determinate(1f),
                pauseIconRes = null, pauseContentDesc = "", pauseAction = null,
                moreVisible = true
            )
        }
        DownloadStatus.FAILED -> {
            val errorStr = item.errorMessage ?: context.getString(R.string.download_error_unknown)
            RowDisplay(
                statusText = if (item.startedAt > 0L) context.getString(R.string.download_info_with_time, errorStr, formatRelativeTimestamp(item.startedAt)) else errorStr,
                metaText = context.getString(R.string.download_status_failed_label),
                retryHintText = if (item.lastErrorWasServerError) context.getString(R.string.download_retry_hint) else null,
                resumableText = null,
                cardProgress = DownloadCardProgress.None,
                pauseIconRes = androidx.compose.material.icons.Icons.Filled.PlayArrow, pauseContentDesc = resumeDesc, pauseAction = { onRetry(item.id) },
                moreVisible = false
            )
        }
        DownloadStatus.COPYING_TEMP -> {
            val pct = item.copyProgress
            RowDisplay(
                statusText = if (item.totalBytes > 0) formatFileSize(item.totalBytes) else "",
                metaText = context.getString(R.string.download_status_copying_temp, pct),
                retryHintText = null, resumableText = null,
                cardProgress = DownloadCardProgress.Determinate(pct / 100f),
                pauseIconRes = null, pauseContentDesc = "", pauseAction = null,
                moreVisible = false
            )
        }
        DownloadStatus.DELETING_TEMP -> RowDisplay(
            statusText = if (item.totalBytes > 0) formatFileSize(item.totalBytes) else "",
            metaText = context.getString(R.string.download_status_deleting_temp),
            retryHintText = null, resumableText = null,
            cardProgress = DownloadCardProgress.Indeterminate,
            pauseIconRes = null, pauseContentDesc = "", pauseAction = null,
            moreVisible = false
        )
    }
}

private fun buildSpeedEtaText(context: android.content.Context, item: DownloadItem): String? {
    val speed = item.speedBytesPerSec
    val elapsedMs = item.activeElapsedMs + if (item.activeStartedAt > 0L) System.currentTimeMillis() - item.activeStartedAt else 0L
    val elapsedStr = if (elapsedMs >= 1000L) formatElapsed(elapsedMs / 1000L) else null
    if (speed <= 0L) return elapsedStr
    val remaining = item.totalBytes - item.bytesDownloaded
    val speedEta = if (item.totalBytes <= 0L || remaining <= 0L) {
        context.getString(R.string.download_speed_only, formatFileSize(speed))
    } else {
        val etaSpeed = item.averageSpeedBytesPerSec().takeIf { it > 0L } ?: speed
        context.getString(R.string.download_speed_eta, formatFileSize(speed), formatEta(context, remaining / etaSpeed))
    }
    return if (elapsedStr != null) "$speedEta  \u2022  $elapsedStr" else speedEta
}

internal fun formatElapsed(seconds: Long): String = when {
    seconds < 60 -> "${seconds}s"
    seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
    else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
}

private fun formatEta(context: android.content.Context, seconds: Long): String = when {
    seconds < 60 -> context.getString(R.string.download_eta_seconds, seconds)
    seconds < 3600 -> context.getString(R.string.download_eta_minutes, seconds / 60, seconds % 60)
    else -> context.getString(R.string.download_eta_hours, seconds / 3600, (seconds % 3600) / 60)
}

@Composable
private fun fileTypeIconRes(filename: String): androidx.compose.ui.graphics.painter.Painter {
    val ext = filename.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "tiff", "tif",
        "heic", "heif", "avif", "svg", "ico", "raw", "cr2", "nef",
        "orf", "arw", "dng", "jfif", "jp2", "tga" -> androidx.compose.material.icons.Icons.Filled.Image.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
        "mp4", "mkv", "webm", "avi", "mov", "wmv", "flv", "ts",
        "m4v", "3gp", "3g2", "rmvb", "vob", "ogv", "mts", "m2ts",
        "divx", "xvid", "f4v", "asf", "mpg", "mpeg", "m2v",
        "mxf", "ogm" -> androidx.compose.material.icons.Icons.Filled.VideoFile.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
        "apk", "apks", "apkm", "xapk", "apkz", "aab" -> androidx.compose.material.icons.Icons.Filled.InstallMobile.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
        "mp3", "wav", "flac", "ogg", "m4a", "aac", "opus", "wma",
        "aiff", "aif", "alac", "ape", "mka", "mid", "midi",
        "amr", "caf", "dsd", "dsf", "dff", "ra", "rm",
        "spx", "voc" -> androidx.compose.material.icons.Icons.Filled.AudioFile.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
        "zip", "7z", "rar", "gz", "tar", "bz2", "xz", "lz4",
        "zst", "br", "cab", "iso", "tgz", "tbz2", "txz",
        "z", "lzma", "lzh", "arj", "ace", "sit",
        "cpio", "xar" -> androidx.compose.material.icons.Icons.Filled.FolderZip.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
        "pdf" -> androidx.compose.material.icons.Icons.Filled.PictureAsPdf.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
        "xls", "xlsx", "xlsm", "ods", "csv", "tsv",
        "numbers" -> androidx.compose.material.icons.Icons.Filled.TableChart.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
        "ppt", "pptx", "pptm", "odp", "key" -> androidx.compose.material.icons.Icons.Filled.Slideshow.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
        "doc", "docx", "docm", "odt", "rtf", "pages", "txt",
        "md", "markdown", "log", "wpd",
        "odf" -> androidx.compose.material.icons.Icons.Filled.Description.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
        "epub", "mobi", "azw", "azw3", "azw4", "fb2", "djvu",
        "cbz", "cbr" -> androidx.compose.material.icons.Icons.AutoMirrored.Filled.MenuBook.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
        "srt", "vtt", "ass", "sub", "ssa", "sbv",
        "smi" -> androidx.compose.material.icons.Icons.Filled.Subtitles.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
        "kt", "kts", "java", "py", "js", "mjs", "jsx", "tsx",
        "c", "cpp", "cc", "h", "hpp", "cs", "go", "rs", "rb",
        "php", "swift", "sh", "ps1", "lua", "sql", "yaml", "yml",
        "toml", "ini", "cfg", "conf", "gradle", "json", "xml",
        "html", "htm", "css", "scss", "less", "vue", "dart",
        "tex" -> androidx.compose.material.icons.Icons.Filled.Code.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
        "ttf", "otf", "woff", "woff2",
        "eot" -> androidx.compose.material.icons.Icons.Filled.FontDownload.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
        "psd", "ai", "xd", "sketch", "fig", "indd", "eps",
        "odg" -> androidx.compose.material.icons.Icons.Filled.Palette.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
        "db", "sqlite", "sqlite3", "mdb", "accdb",
        "dbf" -> androidx.compose.material.icons.Icons.Filled.Storage.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
        "exe", "msi", "msix", "dmg", "pkg", "deb", "rpm",
        "appimage", "run", "bat", "com",
        "jar" -> androidx.compose.material.icons.Icons.Filled.Apps.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
        "pem", "crt", "cer", "pfx", "p12", "der", "csr",
        "gpg", "pgp", "asc" -> androidx.compose.material.icons.Icons.Filled.Lock.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
        "ics", "ical", "vcs", "vcf" -> androidx.compose.material.icons.Icons.Filled.Event.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
        "torrent" -> androidx.compose.material.icons.Icons.Filled.CloudDownload.let { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
        else -> androidx.compose.ui.res.painterResource(R.drawable.ic_file_other_24)
    }
}
