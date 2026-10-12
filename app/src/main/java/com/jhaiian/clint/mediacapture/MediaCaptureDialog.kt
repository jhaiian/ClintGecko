package com.jhaiian.clint.mediacapture

import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.downloads.formatElapsed
import com.jhaiian.clint.ui.ClintCheckbox
import com.jhaiian.clint.ui.ClintDialogStatusBarEffect
import com.jhaiian.clint.ui.listscreen.ListMenuItem
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatFileSize
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaCaptureDialog(
    tabId: String,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onCopyLink: (String) -> Unit,
    onPreviewMediaStarted: () -> Unit,
    onDownload: (DetectedMedia, List<DetectedMedia>, Long?, String?) -> Unit
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val detectedItems by MediaCaptureStore.observe(tabId).collectAsState()
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    var convertTsToMp4 by remember { mutableStateOf(prefs.getBoolean(MEDIA_CAPTURE_CONVERT_TS_TO_MP4_PREF, MEDIA_CAPTURE_CONVERT_TS_TO_MP4_DEFAULT)) }

    var previewMedia by remember { mutableStateOf<DetectedMedia?>(null) }

    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val maxSheetHeight = if (isPortrait) {
        (configuration.screenHeightDp.dp * 0.6f).coerceAtLeast(360.dp)
    } else {
        (configuration.screenHeightDp.dp * 0.6f).coerceAtLeast(280.dp)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.popupBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.divider) }
    ) {
        ClintDialogStatusBarEffect(hideStatusBar, hideSystemNavigation)
        Column(Modifier.fillMaxWidth().heightIn(max = maxSheetHeight)) {
            MediaCaptureHeader(
                detectedItems.size,
                MediaCaptureSortState.mode,
                { MediaCaptureSortState.mode = it },
                onDismiss
            )
            HorizontalDivider(color = colors.popupStroke)
            MediaCaptureConvertTsRow(convertTsToMp4) { checked ->
                convertTsToMp4 = checked
                prefs.edit().putBoolean(MEDIA_CAPTURE_CONVERT_TS_TO_MP4_PREF, checked).apply()
            }
            if (detectedItems.isEmpty()) {
                MediaCaptureEmptyState()
            } else {
                val sortMode = MediaCaptureSortState.mode
                val video = sortMediaCapture(detectedItems.filter { it.kind == MediaKind.VIDEO }, sortMode)
                val bestIds = remember(detectedItems) { bestQualityIds(detectedItems) }
                val audio = sortMediaCapture(detectedItems.filter { it.kind == MediaKind.AUDIO }, sortMode)
                val subtitles = sortMediaCapture(detectedItems.filter { it.kind == MediaKind.SUBTITLE }, sortMode)
                LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false).padding(bottom = 8.dp)) {
                    if (video.isNotEmpty()) {
                        item(key = "header_video") { MediaCaptureSectionHeader(stringResource(R.string.media_capture_section_video)) }
                        items(video, key = { it.id }) { media ->
                            MediaCaptureRow(media, onCopyLink, isBest = media.id in bestIds, onPreview = { onPreviewMediaStarted(); VideoPreviewActivity.start(context, tabId, media.id) }) { estimatedBytes, containerExt -> onDownload(media, detectedItems, estimatedBytes, containerExt) }
                        }
                    }
                    if (audio.isNotEmpty()) {
                        item(key = "header_audio") { MediaCaptureSectionHeader(stringResource(R.string.media_capture_section_audio)) }
                        items(audio, key = { it.id }) { media ->
                            MediaCaptureRow(media, onCopyLink, onPreview = { onPreviewMediaStarted(); previewMedia = media }) { estimatedBytes, containerExt -> onDownload(media, detectedItems, estimatedBytes, containerExt) }
                        }
                    }
                    if (subtitles.isNotEmpty()) {
                        item(key = "header_subtitles") { MediaCaptureSectionHeader(stringResource(R.string.media_capture_section_subtitles)) }
                        items(subtitles, key = { it.id }) { media ->
                            MediaCaptureRow(media, onCopyLink, onPreview = { previewMedia = media }) { estimatedBytes, containerExt -> onDownload(media, detectedItems, estimatedBytes, containerExt) }
                        }
                    }
                }
            }
        }
    }
    previewMedia?.let { target ->
        when (target.kind) {
            MediaKind.AUDIO -> AudioPreviewDialog(
                media = target,
                hideStatusBar = hideStatusBar,
                hideSystemNavigation = hideSystemNavigation,
                onDismiss = { previewMedia = null }
            )
            else -> SubtitlePreviewDialog(
                media = target,
                hideStatusBar = hideStatusBar,
                hideSystemNavigation = hideSystemNavigation,
                onDismiss = { previewMedia = null }
            )
        }
    }
}

fun pickPairedAudio(media: DetectedMedia, all: List<DetectedMedia>): DetectedMedia? {
    if (media.kind == MediaKind.VIDEO && media.hasAudio != false) return null
    val group = media.groupUrl ?: return null
    val sameGroup = all.filter { it.kind == MediaKind.AUDIO && it.groupUrl == group }
    if (sameGroup.isEmpty()) return null
    val ref = media.audioGroupRef
    val scoped = if (ref != null) sameGroup.filter { it.mediaGroupId == ref } else sameGroup
    val candidates = scoped.ifEmpty { sameGroup }
    return candidates.firstOrNull { it.isDefaultTrack } ?: candidates.first()
}

fun pickSoleSubtitle(media: DetectedMedia, all: List<DetectedMedia>): DetectedMedia? {
    val group = media.groupUrl ?: return null
    val subtitles = all.filter { it.kind == MediaKind.SUBTITLE && it.groupUrl == group }
    return subtitles.singleOrNull()
}

fun suggestMediaCaptureFilename(media: DetectedMedia, pageTitle: String, containerExtension: String? = null): String {
    val fromUrl = runCatching { Uri.parse(media.url).lastPathSegment }.getOrNull()
    if (!fromUrl.isNullOrBlank() && fromUrl.contains('.') && !fromUrl.endsWith(".m3u8") && !fromUrl.endsWith(".mpd")) {
        return fromUrl
    }
    val ext = when {
        media.kind == MediaKind.SUBTITLE && (media.isHlsPlaylist || media.format.equals("HLS", true)) -> "vtt"
        media.format.equals("HLS", true) || media.format.equals("DASH", true) -> containerExtension ?: "mp4"
        else -> media.format.lowercase()
    }
    return "${sanitizeMediaCaptureFilenameBase(pageTitle)}.$ext"
}

private fun sanitizeMediaCaptureFilenameBase(title: String): String {
    val cleaned = title.replace(Regex("[/\\\\:*?\"<>|]"), "_").trim().take(80).trim()
    return cleaned.ifBlank { "media" }
}

fun isMediaCaptureManifestBased(media: DetectedMedia): Boolean =
    media.kind != MediaKind.SUBTITLE && media.groupUrl != null &&
        (media.format.equals("HLS", true) || media.format.equals("DASH", true))

@Composable
private fun MediaCaptureHeader(
    count: Int,
    sortMode: MediaCaptureSort,
    onSortSelected: (MediaCaptureSort) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var sortMenuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.media_capture_dialog_title),
                color = colors.popupText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                if (count > 0) {
                    stringResource(R.string.media_capture_count_label, count)
                } else {
                    stringResource(R.string.media_capture_scanning)
                },
                color = colors.secondaryText,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (count > 1) {
            Box {
                IconButton(onClick = { sortMenuOpen = true }) {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(R.string.downloads_sort),
                        tint = colors.iconTint
                    )
                }
                DropdownMenu(
                    expanded = sortMenuOpen,
                    onDismissRequest = { sortMenuOpen = false },
                    shape = PopupShape,
                    containerColor = colors.popupBackground,
                    border = BorderStroke(1.dp, colors.popupStroke)
                ) {
                    ListMenuItem(Icons.Filled.HighQuality, stringResource(R.string.media_capture_sort_quality), sortMode == MediaCaptureSort.QUALITY) {
                        sortMenuOpen = false
                        onSortSelected(MediaCaptureSort.QUALITY)
                    }
                    ListMenuItem(Icons.Filled.FormatSize, stringResource(R.string.downloads_sort_by_size), sortMode == MediaCaptureSort.SIZE) {
                        sortMenuOpen = false
                        onSortSelected(MediaCaptureSort.SIZE)
                    }
                    ListMenuItem(Icons.Filled.ArrowDownward, stringResource(R.string.media_capture_sort_newest), sortMode == MediaCaptureSort.NEWEST) {
                        sortMenuOpen = false
                        onSortSelected(MediaCaptureSort.NEWEST)
                    }
                    ListMenuItem(Icons.Filled.ArrowUpward, stringResource(R.string.media_capture_sort_oldest), sortMode == MediaCaptureSort.OLDEST) {
                        sortMenuOpen = false
                        onSortSelected(MediaCaptureSort.OLDEST)
                    }
                }
            }
        }
        IconButton(onClick = onDismiss) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.media_capture_close),
                tint = colors.iconTint
            )
        }
    }
}

@Composable
private fun MediaCaptureConvertTsRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(start = 5.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClintCheckbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            stringResource(R.string.media_capture_convert_ts_to_mp4),
            color = colors.onSurface,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f).padding(start = 4.dp)
        )
    }
}

@Composable
private fun MediaCaptureEmptyState() {
    val colors = LocalClintColors.current
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.VideoLibrary,
            contentDescription = null,
            tint = colors.secondaryText.copy(alpha = 0.5f),
            modifier = Modifier.size(40.dp)
        )
        Text(
            stringResource(R.string.media_capture_empty_state),
            color = colors.secondaryText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun MediaCaptureSectionHeader(text: String) {
    Text(
        text,
        color = LocalClintColors.current.secondaryText,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
    )
}

private fun mediaKindIcon(kind: MediaKind): ImageVector = when (kind) {
    MediaKind.VIDEO -> Icons.Filled.VideoFile
    MediaKind.AUDIO -> Icons.Filled.AudioFile
    MediaKind.SUBTITLE -> Icons.Filled.Subtitles
}

private fun formatBitrate(bitsPerSec: Long): String {
    val mbps = bitsPerSec / 1_000_000.0
    return if (mbps >= 1.0) {
        String.format(Locale.US, "%.1f Mbps", mbps)
    } else {
        String.format(Locale.US, "%.0f Kbps", bitsPerSec / 1000.0)
    }
}

@Composable
internal fun mediaCaptureTitle(media: DetectedMedia): String {
    media.label?.takeIf { it.isNotBlank() }?.let { return it }
    if (media.width != null && media.height != null) return "${media.width}\u00D7${media.height}"
    val name = remember(media.url) { runCatching { Uri.parse(media.url).lastPathSegment }.getOrNull() }
    return name?.takeIf { it.isNotBlank() } ?: media.format
}

private fun bestQualityIds(items: List<DetectedMedia>): Set<String> =
    setOfNotNull(sortMediaCapture(items.filter { it.kind == MediaKind.VIDEO }, MediaCaptureSort.QUALITY).firstOrNull()?.id)

private fun mediaCaptureLanguage(media: DetectedMedia): String? {
    if (media.kind == MediaKind.VIDEO) return null
    val tag = media.language?.takeIf { it.isNotBlank() } ?: return null
    val locale = Locale.forLanguageTag(tag.replace('_', '-'))
    val localName = locale.getDisplayName(Locale.getDefault()).takeIf { it.isNotBlank() && !it.equals(tag, ignoreCase = true) }
    val englishName = locale.getDisplayName(Locale.ENGLISH).takeIf { it.isNotBlank() && !it.equals(tag, ignoreCase = true) }
    val name = localName ?: tag.uppercase(Locale.ROOT)
    val label = media.label
    if (label != null && listOfNotNull(localName, englishName).any { label.contains(it, ignoreCase = true) }) return null
    return name
}

@Composable
private fun mediaCaptureSubtitle(media: DetectedMedia, estimate: HlsSegmentEstimate?): String {
    val parts = mutableListOf<String>()
    parts += media.format
    mediaCaptureLanguage(media)?.let { parts += it }
    media.bandwidthBitsPerSec?.takeIf { it > 0L }?.let { parts += formatBitrate(it) }
    media.durationSeconds?.takeIf { it > 0.0 }?.let { parts += formatElapsed(it.toLong()) }
    media.cueCount?.takeIf { it > 0 }?.let { parts += stringResource(R.string.media_capture_cue_count, it) }
    if (estimate != null) {
        parts += stringResource(R.string.media_capture_segments_count, estimate.segmentCount)
        estimate.estimatedBytes?.takeIf { it > 0L }?.let {
            parts += stringResource(R.string.media_capture_estimated_size, formatFileSize(it))
        }
    }
    media.sizeBytes?.takeIf { it > 0L }?.let { parts += formatFileSize(it) }
    if (media.isLive) parts += stringResource(R.string.media_capture_live_badge)
    if (media.kind == MediaKind.VIDEO && media.hasAudio == false) {
        parts += stringResource(R.string.media_capture_no_audio_badge)
    }
    return parts.joinToString("  \u2022  ")
}

private fun startExternalStream(context: android.content.Context, media: DetectedMedia) {
    val mime = media.mimeType ?: when (media.kind) {
        MediaKind.AUDIO -> "audio/*"
        MediaKind.SUBTITLE -> "text/*"
        MediaKind.VIDEO -> "video/*"
    }
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(media.url), mime)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, media.url)) }
}

private fun shareDownloadLink(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, context.getString(R.string.link_share))) }
}

@Composable
private fun MediaCaptureRow(media: DetectedMedia, onCopyLink: (String) -> Unit, isBest: Boolean = false, onPreview: (() -> Unit)? = null, onDownload: (Long?, String?) -> Unit) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val estimate = media.estimate
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground)
            .clickable(onClick = { onDownload(estimate?.estimatedBytes, estimate?.containerExtension) })
            .padding(start = 14.dp, end = 2.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(
                mediaKindIcon(media.kind),
                contentDescription = null,
                tint = colors.iconTint,
                modifier = Modifier.size(22.dp)
            )
            if (media.kind == MediaKind.VIDEO && media.hasAudio == false) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(colors.cardBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = stringResource(R.string.media_capture_no_audio_badge),
                        tint = colors.colorError,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    mediaCaptureTitle(media),
                    color = colors.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isBest) {
                    Text(
                        stringResource(R.string.media_capture_best_badge),
                        color = colors.primary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.primary.copy(alpha = 0.16f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                mediaCaptureSubtitle(media, estimate),
                color = colors.secondaryText,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.media_capture_more_options),
                    tint = colors.iconTint.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                shape = PopupShape,
                containerColor = colors.popupBackground,
                border = BorderStroke(1.dp, colors.popupStroke)
            ) {
                ListMenuItem(Icons.Filled.PlayCircle, stringResource(R.string.media_capture_menu_stream), checked = false) {
                    menuExpanded = false
                    startExternalStream(context, media)
                }
                if (onPreview != null) {
                    ListMenuItem(Icons.Filled.Visibility, stringResource(R.string.media_capture_menu_preview), checked = false) {
                        menuExpanded = false
                        onPreview()
                    }
                }
                ListMenuItem(Icons.Filled.Download, stringResource(R.string.media_capture_download), checked = false) {
                    menuExpanded = false
                    onDownload(estimate?.estimatedBytes, estimate?.containerExtension)
                }
                ListMenuItem(Icons.Filled.ContentCopy, stringResource(R.string.media_capture_menu_copy_link), checked = false) {
                    menuExpanded = false
                    onCopyLink(media.url)
                }
                ListMenuItem(Icons.Filled.Share, stringResource(R.string.media_capture_menu_share_link), checked = false) {
                    menuExpanded = false
                    shareDownloadLink(context, media.url)
                }
            }
        }
    }
}
