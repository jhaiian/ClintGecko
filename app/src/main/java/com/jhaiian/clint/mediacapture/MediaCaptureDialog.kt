package com.jhaiian.clint.mediacapture

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.downloads.formatElapsed
import com.jhaiian.clint.ui.ClintCheckbox
import com.jhaiian.clint.ui.ClintDialogStatusBarEffect
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.ClintSheetWindowInsets
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.listscreen.ListMenuItem
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.rememberSheetFlingConnection
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatFileSize
import java.util.Locale

private const val INFO_SEPARATOR = "  •  "

private val SIZE_PRESETS = listOf(
    100 to MEDIA_CAPTURE_MIN_SIZE_UNIT_KB,
    500 to MEDIA_CAPTURE_MIN_SIZE_UNIT_KB,
    1 to MEDIA_CAPTURE_MIN_SIZE_UNIT_MB,
    5 to MEDIA_CAPTURE_MIN_SIZE_UNIT_MB,
    10 to MEDIA_CAPTURE_MIN_SIZE_UNIT_MB
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaCaptureDialog(
    tabId: String,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onCopyLink: (String) -> Unit,
    onPreviewMediaStarted: () -> Unit,
    onDownload: (DetectedMedia, DetectedMedia?, List<DetectedMedia>, Long?, String?) -> Unit,
    onDownloadSelected: (List<MediaCaptureEntry>, List<DetectedMedia>) -> Unit,
    isYouTube: Boolean = false,
    onGetYtdlnis: () -> Unit = {}
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val detectedItems by MediaCaptureStore.observe(tabId).collectAsState()
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    var convertTsToMp4 by remember { mutableStateOf(prefs.getBoolean(MEDIA_CAPTURE_CONVERT_TS_TO_MP4_PREF, MEDIA_CAPTURE_CONVERT_TS_TO_MP4_DEFAULT)) }
    var sizeFilter by remember { mutableStateOf(readMediaCaptureSizeFilter(prefs)) }
    var settingsOpen by remember { mutableStateOf(false) }

    var previewMedia by remember { mutableStateOf<DetectedMedia?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }

    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val maxSheetHeight = if (isPortrait) {
        (configuration.screenHeightDp.dp * 0.8f).coerceAtLeast(360.dp)
    } else {
        (configuration.screenHeightDp.dp * 0.8f).coerceAtLeast(280.dp)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val sortMode = MediaCaptureSortState.mode
    val minBytes = remember(sizeFilter) { mediaCaptureMinBytes(sizeFilter, prefs) }
    val allVideoOptions = remember(detectedItems, sortMode) {
        val video = sortMediaCapture(detectedItems.filter { it.kind == MediaKind.VIDEO }, sortMode)
        val options = buildVideoOptions(video, detectedItems)
        if (sortMode == MediaCaptureSort.SIZE) options.sortedByDescending { optionEstimatedBytes(it) ?: -1L } else options
    }
    val allAudio = remember(detectedItems, sortMode) {
        sortMediaCapture(detectedItems.filter { it.kind == MediaKind.AUDIO }, sortMode)
    }
    val subtitles = remember(detectedItems, sortMode) {
        sortMediaCapture(detectedItems.filter { it.kind == MediaKind.SUBTITLE }, sortMode)
    }
    val videoOptions = remember(allVideoOptions, minBytes) { allVideoOptions.filter { optionPassesMinSize(it, minBytes) } }
    val audio = remember(allAudio, minBytes) { allAudio.filter { mediaPassesMinSize(it, minBytes) } }
    val totalRows = allVideoOptions.size + allAudio.size + subtitles.size
    val visibleRows = videoOptions.size + audio.size + subtitles.size
    val hiddenRows = totalRows - visibleRows
    val bestKey = remember(videoOptions) {
        val bestVideo = sortMediaCapture(videoOptions.map { it.video }.distinctBy { it.id }, MediaCaptureSort.QUALITY).firstOrNull()
        videoOptions.firstOrNull { it.video.id == bestVideo?.id }?.key
    }
    val entries = remember(videoOptions, audio, subtitles) {
        buildMap<String, MediaCaptureEntry> {
            videoOptions.forEach { put(it.key, mediaCaptureEntry(it.video, it.audio)) }
            audio.forEach { put(it.id, mediaCaptureEntry(it)) }
            subtitles.forEach { put(it.id, mediaCaptureEntry(it)) }
        }
    }
    LaunchedEffect(entries) { selected.removeAll { it !in entries } }
    val selectedEntries = entries.filterKeys { it in selected }.values.toList()
    val toggle: (String) -> Unit = { key -> if (!selected.remove(key)) selected.add(key) }
    val longPress: (String) -> Unit = { key ->
        selectionMode = true
        if (key !in selected) selected.add(key)
    }
    val updateFilter: (MediaCaptureSizeFilter) -> Unit = { updated ->
        sizeFilter = updated
        writeMediaCaptureSizeFilter(prefs, updated)
    }

    val selectedBytes = selectedEntries.sumOf { mediaCaptureEntryBytes(it) ?: 0L }
    val statusText = when {
        isYouTube -> null
        selectionMode -> {
            val base = stringResource(R.string.media_capture_selected_count, selectedEntries.size)
            if (selectedBytes > 0L) {
                base + INFO_SEPARATOR + stringResource(R.string.media_capture_estimated_size, formatFileSize(selectedBytes))
            } else {
                base
            }
        }
        detectedItems.isEmpty() -> stringResource(R.string.media_capture_scanning)
        hiddenRows > 0 -> stringResource(R.string.media_capture_showing_count, visibleRows, totalRows)
        else -> stringResource(R.string.media_capture_count_label, detectedItems.size)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { ClintSheetWindowInsets },
        containerColor = colors.popupBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.divider) }
    ) {
        ClintDialogStatusBarEffect(hideStatusBar, hideSystemNavigation)
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .nestedScroll(rememberSheetFlingConnection(sheetState, consumeFling = false))
                .navigationBarsPadding()
                .imePadding()
        ) {
            MediaCaptureHeader(
                count = if (isYouTube) 0 else detectedItems.size,
                statusText = statusText,
                sortMode = sortMode,
                onSortSelected = { MediaCaptureSortState.mode = it },
                onDismiss = onDismiss,
                showActions = !isYouTube,
                selectionMode = selectionMode,
                onToggleSelection = {
                    selectionMode = !selectionMode
                    selected.clear()
                },
                settingsOpen = settingsOpen,
                filterActive = sizeFilter.enabled,
                onToggleSettings = { settingsOpen = !settingsOpen }
            )
            HorizontalDivider(color = colors.popupStroke)
            if (!isYouTube) {
                AnimatedVisibility(visible = settingsOpen) {
                    Column {
                        MediaCaptureSettingsPanel(
                            convertTsToMp4 = convertTsToMp4,
                            onConvertTsChange = { checked ->
                                convertTsToMp4 = checked
                                prefs.edit().putBoolean(MEDIA_CAPTURE_CONVERT_TS_TO_MP4_PREF, checked).apply()
                            },
                            filter = sizeFilter,
                            onFilterChange = updateFilter,
                            modifier = Modifier
                                .heightIn(max = maxSheetHeight * 0.6f)
                                .verticalScroll(rememberScrollState())
                        )
                        HorizontalDivider(color = colors.popupStroke)
                    }
                }
                if (sizeFilter.enabled && hiddenRows > 0) {
                    MediaCaptureFilterBanner(sizeFilter) { updateFilter(sizeFilter.copy(enabled = false)) }
                }
            }
            if (isYouTube) {
                MediaCaptureYouTubeState(Modifier.weight(1f, fill = false), onGetYtdlnis)
            } else if (detectedItems.isEmpty()) {
                MediaCaptureEmptyState()
            } else if (visibleRows == 0) {
                MediaCaptureAllHiddenState(
                    Modifier.weight(1f, fill = false),
                    sizeFilter
                ) { updateFilter(sizeFilter.copy(enabled = false)) }
            } else {
                LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false).padding(bottom = 8.dp)) {
                    if (videoOptions.isNotEmpty()) {
                        item(key = "header_video") { MediaCaptureSectionHeader(stringResource(R.string.media_capture_section_video)) }
                        items(videoOptions, key = { it.key }) { option ->
                            val media = option.video
                            MediaCaptureRow(
                                media,
                                onCopyLink,
                                audio = option.audio,
                                audioOrdinal = option.audioOrdinal,
                                isBest = option.key == bestKey,
                                selectionMode = selectionMode,
                                selected = option.key in selected,
                                onToggleSelect = { toggle(option.key) },
                                onLongClick = { longPress(option.key) },
                                onPreview = { onPreviewMediaStarted(); VideoPreviewActivity.start(context, tabId, media.id, option.audio?.id) }
                            ) { estimatedBytes, containerExt -> onDownload(media, option.audio, detectedItems, estimatedBytes, containerExt) }
                        }
                    }
                    if (audio.isNotEmpty()) {
                        item(key = "header_audio") { MediaCaptureSectionHeader(stringResource(R.string.media_capture_section_audio)) }
                        items(audio, key = { it.id }) { media ->
                            MediaCaptureRow(
                                media,
                                onCopyLink,
                                selectionMode = selectionMode,
                                selected = media.id in selected,
                                onToggleSelect = { toggle(media.id) },
                                onLongClick = { longPress(media.id) },
                                onPreview = { onPreviewMediaStarted(); previewMedia = media }
                            ) { estimatedBytes, containerExt -> onDownload(media, null, detectedItems, estimatedBytes, containerExt) }
                        }
                    }
                    if (subtitles.isNotEmpty()) {
                        item(key = "header_subtitles") { MediaCaptureSectionHeader(stringResource(R.string.media_capture_section_subtitles)) }
                        items(subtitles, key = { it.id }) { media ->
                            MediaCaptureRow(
                                media,
                                onCopyLink,
                                selectionMode = selectionMode,
                                selected = media.id in selected,
                                onToggleSelect = { toggle(media.id) },
                                onLongClick = { longPress(media.id) },
                                onPreview = { previewMedia = media }
                            ) { estimatedBytes, containerExt -> onDownload(media, null, detectedItems, estimatedBytes, containerExt) }
                        }
                    }
                }
                if (selectionMode) {
                    val allSelected = selectedEntries.size == entries.size
                    HorizontalDivider(color = colors.popupStroke)
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                selected.clear()
                                if (!allSelected) selected.addAll(entries.keys)
                            },
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                stringResource(if (allSelected) R.string.media_capture_deselect_all else R.string.media_capture_select_all),
                                color = colors.primary,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { onDownloadSelected(selectedEntries, detectedItems) },
                            enabled = selectedEntries.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary,
                                disabledContainerColor = colors.primary.copy(alpha = 0.3f),
                                disabledContentColor = colors.onPrimary.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                stringResource(R.string.media_capture_download_selected, selectedEntries.size),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
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

data class MediaCaptureEntry(
    val media: DetectedMedia,
    val audio: DetectedMedia?,
    val estimatedBytes: Long?,
    val containerExtension: String?
)

private fun mediaCaptureEntry(media: DetectedMedia, audio: DetectedMedia? = null) = MediaCaptureEntry(
    media,
    audio,
    if (audio != null) optionEstimatedBytes(MediaCaptureOption(media, audio)) else media.estimate?.estimatedBytes,
    media.estimate?.containerExtension
)

fun mediaCaptureEntryBytes(entry: MediaCaptureEntry): Long? =
    optionEstimatedBytes(MediaCaptureOption(entry.media, entry.audio))

fun pickPairedAudio(media: DetectedMedia, all: List<DetectedMedia>): DetectedMedia? =
    compatibleAudioTracks(media, all).firstOrNull()

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
    statusText: String?,
    sortMode: MediaCaptureSort,
    onSortSelected: (MediaCaptureSort) -> Unit,
    onDismiss: () -> Unit,
    showActions: Boolean,
    selectionMode: Boolean,
    onToggleSelection: () -> Unit,
    settingsOpen: Boolean,
    filterActive: Boolean,
    onToggleSettings: () -> Unit
) {
    val colors = LocalClintColors.current
    var sortMenuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 4.dp)) {
            Text(
                stringResource(R.string.media_capture_dialog_title),
                color = colors.popupText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (statusText != null) {
                Text(
                    statusText,
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (showActions && count > 0) {
            IconButton(onClick = onToggleSelection, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.Checklist,
                    contentDescription = stringResource(R.string.media_capture_select),
                    tint = if (selectionMode) colors.primary else colors.iconTint
                )
            }
        }
        if (showActions) {
            IconButton(onClick = onToggleSettings, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.Tune,
                    contentDescription = stringResource(R.string.media_capture_settings),
                    tint = if (settingsOpen || filterActive) colors.primary else colors.iconTint
                )
            }
        }
        if (count > 1) {
            Box {
                IconButton(onClick = { sortMenuOpen = true }, modifier = Modifier.size(40.dp)) {
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
        IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.media_capture_close),
                tint = colors.iconTint
            )
        }
    }
}

@Composable
private fun mediaCaptureFilterSizeLabel(filter: MediaCaptureSizeFilter): String =
    "${filter.amount} " + stringResource(
        if (filter.unit == MEDIA_CAPTURE_MIN_SIZE_UNIT_MB) R.string.media_capture_unit_mb else R.string.media_capture_unit_kb
    )

@Composable
private fun MediaCaptureSettingsPanel(
    convertTsToMp4: Boolean,
    onConvertTsChange: (Boolean) -> Unit,
    filter: MediaCaptureSizeFilter,
    onFilterChange: (MediaCaptureSizeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    var amountText by remember { mutableStateOf(filter.amount.toString()) }
    val kbLabel = stringResource(R.string.media_capture_unit_kb)
    val mbLabel = stringResource(R.string.media_capture_unit_mb)
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        MediaCaptureToggleRow(
            title = stringResource(R.string.media_capture_filter_title),
            summary = stringResource(R.string.media_capture_filter_summary),
            checked = filter.enabled
        ) { onFilterChange(filter.copy(enabled = !filter.enabled)) }
        if (filter.enabled) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                ClintOutlinedTextField(
                    value = amountText,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(5)
                        amountText = digits
                        digits.toIntOrNull()
                            ?.takeIf { it in 1..MEDIA_CAPTURE_MIN_SIZE_MAX_AMOUNT }
                            ?.let { onFilterChange(filter.copy(amount = it)) }
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.media_capture_filter_min_size_hint)) },
                    singleLine = true,
                    isError = (amountText.toIntOrNull() ?: 0) < 1,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.width(8.dp))
                val toggleShape = RoundedCornerShape(12.dp)
                Row(
                    Modifier
                        .height(56.dp)
                        .clip(toggleShape)
                        .border(1.dp, colors.divider, toggleShape)
                ) {
                    listOf(
                        MEDIA_CAPTURE_MIN_SIZE_UNIT_KB to kbLabel,
                        MEDIA_CAPTURE_MIN_SIZE_UNIT_MB to mbLabel
                    ).forEach { (unit, label) ->
                        val active = filter.unit == unit
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .background(if (active) colors.primary else Color.Transparent)
                                .clickable { onFilterChange(filter.copy(unit = unit)) }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                color = if (active) colors.onPrimary else colors.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            MediaCaptureFlowRow(Modifier.padding(top = 10.dp, start = 4.dp), spacing = 8) {
                SIZE_PRESETS.forEach { (amount, unit) ->
                    val active = filter.amount == amount && filter.unit == unit
                    Text(
                        "$amount " + if (unit == MEDIA_CAPTURE_MIN_SIZE_UNIT_MB) mbLabel else kbLabel,
                        color = if (active) colors.primary else colors.onSurface,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (active) colors.primary.copy(alpha = 0.16f) else colors.onSurface.copy(alpha = 0.07f))
                            .clickable {
                                amountText = amount.toString()
                                onFilterChange(filter.copy(amount = amount, unit = unit))
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }
        HorizontalDivider(color = colors.popupStroke, modifier = Modifier.padding(vertical = 8.dp))
        MediaCaptureToggleRow(
            title = stringResource(R.string.media_capture_convert_ts_to_mp4),
            summary = null,
            checked = convertTsToMp4
        ) { onConvertTsChange(!convertTsToMp4) }
    }
}

@Composable
private fun MediaCaptureToggleRow(title: String, summary: String?, checked: Boolean, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (summary != null) {
                Text(summary, color = colors.secondaryText, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        ClintSwitch(checked = checked)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MediaCaptureFlowRow(modifier: Modifier = Modifier, spacing: Int = 4, content: @Composable () -> Unit) {
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.dp)
    ) { content() }
}

@Composable
private fun MediaCaptureFilterBanner(filter: MediaCaptureSizeFilter, onShowAll: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Tune,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            stringResource(R.string.media_capture_filter_active, mediaCaptureFilterSizeLabel(filter)),
            color = colors.secondaryText,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f).padding(start = 8.dp, top = 6.dp, bottom = 6.dp)
        )
        TextButton(onClick = onShowAll) {
            Text(stringResource(R.string.media_capture_filter_show_all), color = colors.primary, fontSize = 12.sp)
        }
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
private fun MediaCaptureAllHiddenState(modifier: Modifier, filter: MediaCaptureSizeFilter, onShowAll: () -> Unit) {
    val colors = LocalClintColors.current
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.VideoLibrary,
            contentDescription = null,
            tint = colors.secondaryText.copy(alpha = 0.5f),
            modifier = Modifier.size(40.dp)
        )
        Text(
            stringResource(R.string.media_capture_filter_all_hidden, mediaCaptureFilterSizeLabel(filter)),
            color = colors.secondaryText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 480.dp).padding(top = 12.dp)
        )
        TextButton(onClick = onShowAll, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.media_capture_filter_show_all), color = colors.primary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun MediaCaptureYouTubeState(modifier: Modifier, onGetYtdlnis: () -> Unit) {
    val colors = LocalClintColors.current
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.VideoLibrary,
            contentDescription = null,
            tint = colors.secondaryText.copy(alpha = 0.5f),
            modifier = Modifier.size(40.dp)
        )
        Text(
            stringResource(R.string.media_capture_youtube_title),
            color = colors.popupText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            stringResource(R.string.media_capture_youtube_message),
            color = colors.secondaryText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 480.dp).padding(top = 8.dp)
        )
        Button(
            onClick = onGetYtdlnis,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(top = 20.dp)
        ) {
            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.media_capture_youtube_get_ytdlnis),
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
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
    if (media.width != null && media.height != null) return "${media.width}×${media.height}"
    val name = remember(media.url) { runCatching { Uri.parse(media.url).lastPathSegment }.getOrNull() }
    return name?.takeIf { it.isNotBlank() } ?: media.format
}

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

private enum class MediaInfoChipKind { NORMAL, SIZE, ALERT }

private data class MediaInfoChip(val text: String, val kind: MediaInfoChipKind = MediaInfoChipKind.NORMAL)

private data class MediaOptionDetails(val videoLine: String?, val audioLine: String?, val chips: List<MediaInfoChip>)

@Composable
private fun mediaCaptureInfoChips(media: DetectedMedia, estimate: HlsSegmentEstimate?): List<MediaInfoChip> {
    val chips = mutableListOf<MediaInfoChip>()
    val exactBytes = media.sizeBytes?.takeIf { it > 0L }
    if (exactBytes != null) {
        chips += MediaInfoChip(formatFileSize(exactBytes), MediaInfoChipKind.SIZE)
    } else {
        mediaEstimatedBytes(media)?.takeIf { it > 0L }?.let {
            chips += MediaInfoChip(stringResource(R.string.media_capture_estimated_size, formatFileSize(it)), MediaInfoChipKind.SIZE)
        }
    }
    chips += MediaInfoChip(media.format)
    mediaCaptureLanguage(media)?.let { chips += MediaInfoChip(it) }
    mediaCaptureCodecParts(media).forEach { chips += MediaInfoChip(it) }
    media.bandwidthBitsPerSec?.takeIf { it > 0L }?.let { chips += MediaInfoChip(formatBitrate(it)) }
    media.durationSeconds?.takeIf { it > 0.0 }?.let { chips += MediaInfoChip(formatElapsed(it.toLong())) }
    media.cueCount?.takeIf { it > 0 }?.let { chips += MediaInfoChip(stringResource(R.string.media_capture_cue_count, it)) }
    if (estimate != null) chips += MediaInfoChip(stringResource(R.string.media_capture_segments_count, estimate.segmentCount))
    if (media.isLive) chips += MediaInfoChip(stringResource(R.string.media_capture_live_badge), MediaInfoChipKind.ALERT)
    if (media.kind == MediaKind.VIDEO && media.hasAudio == false) {
        chips += MediaInfoChip(stringResource(R.string.media_capture_no_audio_badge), MediaInfoChipKind.ALERT)
    }
    return chips
}

@Composable
private fun mediaCaptureCodecParts(media: DetectedMedia): List<String> {
    val parts = mutableListOf<String>()
    when (media.kind) {
        MediaKind.VIDEO -> {
            MediaCodecNames.videoLabel(media.codecs)?.let { parts += it }
            media.frameRate?.let { parts += stringResource(R.string.media_capture_fps, formatFrameRate(it)) }
        }
        MediaKind.AUDIO -> {
            MediaCodecNames.audioLabel(media.codecs)?.let { parts += it }
            mediaCaptureChannels(media.channels)?.let { parts += it }
        }
        MediaKind.SUBTITLE -> Unit
    }
    return parts
}

private fun formatFrameRate(rate: Double): String {
    val rounded = Math.round(rate)
    return if (kotlin.math.abs(rate - rounded) < 0.01) rounded.toString() else String.format(Locale.getDefault(), "%.2f", rate)
}

@Composable
private fun mediaCaptureChannels(channels: Int?): String? = when (channels) {
    null, 0 -> null
    1 -> stringResource(R.string.media_capture_channels_mono)
    2 -> stringResource(R.string.media_capture_channels_stereo)
    6 -> "5.1"
    8 -> "7.1"
    else -> stringResource(R.string.media_capture_channels_count, channels)
}

@Composable
private fun mediaCaptureOptionDetails(video: DetectedMedia, audio: DetectedMedia, estimatedBytes: Long?): MediaOptionDetails {
    val videoParts = mutableListOf<String>()
    MediaCodecNames.videoLabel(video.codecs)?.let { videoParts += it }
    video.frameRate?.let { videoParts += stringResource(R.string.media_capture_fps, formatFrameRate(it)) }
    video.bandwidthBitsPerSec?.takeIf { it > 0L }?.let { videoParts += formatBitrate(it) }
    val audioParts = mutableListOf<String>()
    MediaCodecNames.audioLabel(audio.codecs)?.let { audioParts += it }
    mediaCaptureChannels(audio.channels)?.let { audioParts += it }
    audio.bandwidthBitsPerSec?.takeIf { it > 0L }?.let { audioParts += formatBitrate(it) }
    val chips = mutableListOf<MediaInfoChip>()
    estimatedBytes?.takeIf { it > 0L }?.let {
        chips += MediaInfoChip(stringResource(R.string.media_capture_estimated_size, formatFileSize(it)), MediaInfoChipKind.SIZE)
    }
    chips += MediaInfoChip(video.format)
    (video.durationSeconds ?: audio.durationSeconds)?.takeIf { it > 0.0 }?.let { chips += MediaInfoChip(formatElapsed(it.toLong())) }
    if (video.isLive) chips += MediaInfoChip(stringResource(R.string.media_capture_live_badge), MediaInfoChipKind.ALERT)
    return MediaOptionDetails(
        videoLine = videoParts.takeIf { it.isNotEmpty() }
            ?.let { stringResource(R.string.media_capture_option_video_info, it.joinToString(INFO_SEPARATOR)) },
        audioLine = audioParts.takeIf { it.isNotEmpty() }
            ?.let { stringResource(R.string.media_capture_option_audio_info, it.joinToString(INFO_SEPARATOR)) },
        chips = chips
    )
}

@Composable
private fun MediaInfoChipView(chip: MediaInfoChip) {
    val colors = LocalClintColors.current
    val accent = when (chip.kind) {
        MediaInfoChipKind.SIZE -> colors.primary
        MediaInfoChipKind.ALERT -> colors.colorError
        MediaInfoChipKind.NORMAL -> colors.secondaryText
    }
    val highlighted = chip.kind != MediaInfoChipKind.NORMAL
    Text(
        chip.text,
        color = accent,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (highlighted) accent.copy(alpha = 0.14f) else colors.onSurface.copy(alpha = 0.07f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
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
private fun MediaCaptureRow(
    media: DetectedMedia,
    onCopyLink: (String) -> Unit,
    audio: DetectedMedia? = null,
    audioOrdinal: Int = 0,
    isBest: Boolean = false,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onPreview: (() -> Unit)? = null,
    onDownload: (Long?, String?) -> Unit
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val estimate = media.estimate
    val optionBytes = if (audio != null) optionEstimatedBytes(MediaCaptureOption(media, audio)) else estimate?.estimatedBytes
    val audioName = audio?.let { audioTrackName(it) ?: stringResource(R.string.media_capture_audio_track_number, audioOrdinal.coerceAtLeast(1)) }
    val details = if (audio != null) mediaCaptureOptionDetails(media, audio, optionBytes) else null
    val chips = details?.chips ?: mediaCaptureInfoChips(media, estimate)
    val title = mediaCaptureTitle(media).let { if (audioName != null) "$it + $audioName" else it }
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) colors.primary.copy(alpha = 0.14f) else colors.cardBackground)
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelect() else onDownload(optionBytes, estimate?.containerExtension) },
                onLongClick = onLongClick
            )
            .padding(start = if (selectionMode) 6.dp else 14.dp, end = if (selectionMode) 14.dp else 2.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (selectionMode) {
            ClintCheckbox(checked = selected, onCheckedChange = { onToggleSelect() })
        }
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
                    if (audio != null) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(12.dp)
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = stringResource(R.string.media_capture_no_audio_badge),
                            tint = colors.colorError,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
        Column(Modifier.weight(1f).padding(start = 10.dp, end = 4.dp)) {
            MediaCaptureFlowRow(Modifier.padding(top = 2.dp), spacing = 6) {
                Text(
                    title,
                    color = colors.onSurface,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                if (isBest) {
                    Text(
                        stringResource(R.string.media_capture_best_badge),
                        color = colors.primary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.primary.copy(alpha = 0.16f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            details?.videoLine?.let { line ->
                Text(
                    line,
                    color = colors.secondaryText,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            details?.audioLine?.let { line ->
                Text(
                    line,
                    color = colors.secondaryText,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (chips.isNotEmpty()) {
                MediaCaptureFlowRow(Modifier.padding(top = 6.dp)) {
                    chips.forEach { MediaInfoChipView(it) }
                }
            }
        }
        if (!selectionMode) Box {
            IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(40.dp)) {
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
                    onDownload(optionBytes, estimate?.containerExtension)
                }
                if (audio != null) {
                    ListMenuItem(Icons.Filled.ContentCopy, stringResource(R.string.media_capture_menu_copy_audio_link), checked = false) {
                        menuExpanded = false
                        onCopyLink(audio.url)
                    }
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
