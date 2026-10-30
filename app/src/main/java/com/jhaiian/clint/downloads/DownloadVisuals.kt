package com.jhaiian.clint.downloads

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

sealed interface DownloadRowProgress {
    data object None : DownloadRowProgress
    data class Determinate(val fraction: Float) : DownloadRowProgress
    data object Indeterminate : DownloadRowProgress
}

@Composable
internal fun SpeedGraph(samples: List<Long>, color: Color, modifier: Modifier = Modifier) {
    val fill = color.copy(alpha = 0.14f)
    val baseline = LocalClintColors.current.divider
    Canvas(modifier) {
        val stroke = 2.5.dp.toPx()
        val usableHeight = size.height - stroke * 2
        drawLine(baseline, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), strokeWidth = 1.dp.toPx())
        if (samples.size < 2) return@Canvas
        val maxValue = samples.max().coerceAtLeast(1L).toFloat()
        val slots = DownloadSpeedHistory.CAPACITY - 1
        val startSlot = slots - (samples.size - 1)
        val step = size.width / slots
        val points = samples.mapIndexed { index, value ->
            Offset((startSlot + index) * step, stroke + usableHeight * (1f - value / maxValue))
        }
        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }
        drawPath(area, fill)
        drawPath(line, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, radius = stroke * 1.6f, center = points.last())
    }
}

@Composable
internal fun PartsStrip(
    fractions: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    columns: Int = fractions.size,
    cellHeight: Dp = 10.dp,
    gap: Dp = 2.dp,
    minCellWidth: Dp? = null,
    maxHeight: Dp? = null
) {
    val track = LocalClintColors.current.primary.copy(alpha = 0.12f)
    val partial = color.copy(alpha = 0.55f)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val safeColumns = if (minCellWidth != null) {
            ((maxWidth + gap) / (minCellWidth + gap)).toInt().coerceIn(1, fractions.size.coerceAtLeast(1))
        } else columns.coerceAtLeast(1)
        val rows = (fractions.size + safeColumns - 1) / safeColumns
        val gridHeight = cellHeight * rows + gap * (rows - 1).coerceAtLeast(0)
        val viewport = maxHeight?.takeIf { gridHeight > it }
        val frame = if (viewport != null) Modifier.fillMaxWidth().height(viewport).verticalScroll(rememberScrollState()) else Modifier.fillMaxWidth()
        Canvas(frame.height(gridHeight)) {
            val gapPx = gap.toPx()
            val cellW = (size.width - gapPx * (safeColumns - 1)) / safeColumns
            val cellH = cellHeight.toPx()
            val radius = CornerRadius(minOf(cellW, cellH) * 0.3f)
            fractions.forEachIndexed { index, fraction ->
                val x = (index % safeColumns) * (cellW + gapPx)
                val y = (index / safeColumns) * (cellH + gapPx)
                drawRoundRect(track, Offset(x, y), Size(cellW, cellH), radius)
                when {
                    fraction >= 1f -> drawRoundRect(color, Offset(x, y), Size(cellW, cellH), radius)
                    fraction > 0f -> drawRoundRect(partial, Offset(x, y), Size(cellW * fraction, cellH), radius)
                }
            }
        }
    }
}

@Composable
internal fun ProgressRing(progress: DownloadRowProgress, color: Color, modifier: Modifier = Modifier, strokeWidth: Dp = 4.dp) {
    val track = LocalClintColors.current.primary.copy(alpha = 0.14f)
    val sweepStart = if (progress is DownloadRowProgress.Indeterminate) {
        val transition = rememberInfiniteTransition(label = "ring")
        val angle by transition.animateFloat(
            initialValue = 0f, targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
            label = "ring_angle"
        )
        angle
    } else 0f
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(stroke / 2, stroke / 2)
        drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        when (progress) {
            is DownloadRowProgress.Determinate -> if (progress.fraction > 0f) {
                drawArc(color, -90f, 360f * progress.fraction.coerceIn(0f, 1f), false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            is DownloadRowProgress.Indeterminate -> drawArc(color, sweepStart - 90f, 90f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            is DownloadRowProgress.None -> Unit
        }
    }
}

@Composable
internal fun DownloadThumb(
    item: DownloadItem,
    modifier: Modifier,
    requestPx: Int,
    corner: Dp = 12.dp,
    iconSize: Dp = 24.dp,
    showDuration: Boolean = true,
    selected: Boolean = false,
    thumbnail: DownloadThumbnail? = rememberDownloadThumbnail(item, requestPx)
) {
    val colors = LocalClintColors.current
    val bitmap = thumbnail?.bitmap
    Box(
        modifier.clip(RoundedCornerShape(corner)).background(colors.primary.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = stringResource(R.string.download_icon_desc), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(
                fileTypePainter(item.filename),
                contentDescription = stringResource(R.string.download_icon_desc),
                tint = if (item.status == DownloadStatus.FAILED) colors.colorError else colors.primary,
                modifier = Modifier.size(iconSize)
            )
        }
        val durationMs = thumbnail?.durationMs ?: 0L
        if (showDuration && durationMs > 0L) {
            Text(
                formatMediaDuration(durationMs),
                color = Color.White, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
        if (selected) {
            Box(Modifier.fillMaxSize().background(colors.primary.copy(alpha = 0.82f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(iconSize))
            }
        }
    }
}

@Composable
internal fun StatChip(text: String, icon: ImageVector? = null, emphasized: Boolean = false, error: Boolean = false) {
    val colors = LocalClintColors.current
    val tint = when {
        error -> colors.colorError
        emphasized -> colors.primary
        else -> colors.secondaryText
    }
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(colors.primary.copy(alpha = 0.08f)).padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(text, color = if (error) colors.colorError else colors.onSurface, fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

internal class SwipeAction(
    val label: String,
    val icon: ImageVector,
    val background: Color,
    val content: Color,
    val onTrigger: () -> Unit
)

@Composable
internal fun SwipeActionsBox(
    enabled: Boolean,
    shape: Shape,
    startAction: SwipeAction?,
    endAction: SwipeAction?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    var widthPx by remember { mutableIntStateOf(0) }
    val latestStart by rememberUpdatedState(startAction)
    val latestEnd by rememberUpdatedState(endAction)
    Box(modifier.onSizeChanged { widthPx = it.width }.clip(shape)) {
        val current = offset.value
        val action = when {
            current > 0f -> startAction
            current < 0f -> endAction
            else -> null
        }
        if (action != null) {
            Box(
                Modifier.matchParentSize().background(action.background).padding(horizontal = 24.dp),
                contentAlignment = if (current > 0f) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(action.icon, contentDescription = null, tint = action.content, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(action.label, color = action.content, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .then(
                    if (!enabled) Modifier else Modifier.pointerInput(startAction != null, endAction != null) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val value = offset.value
                                val threshold = widthPx * 0.28f
                                val triggered = when {
                                    value > threshold -> latestStart
                                    value < -threshold -> latestEnd
                                    else -> null
                                }
                                scope.launch { offset.animateTo(0f, tween(220)) }
                                triggered?.onTrigger?.invoke()
                            },
                            onDragCancel = { scope.launch { offset.animateTo(0f, tween(220)) } },
                            onHorizontalDrag = { change, delta ->
                                val limit = widthPx * 0.55f
                                val next = (offset.value + delta).coerceIn(
                                    if (latestEnd != null) -limit else 0f,
                                    if (latestStart != null) limit else 0f
                                )
                                change.consume()
                                scope.launch { offset.snapTo(next) }
                            }
                        )
                    }
                )
        ) {
            content()
        }
    }
}

internal fun downloadPartFractions(item: DownloadItem): List<Float>? {
    if (item.isStream) {
        val total = item.segmentsTotal
        if (total <= 1) return null
        if (item.partProgress.size == SegmentMap.bucketCount(total)) return item.partProgress
        val done = item.segmentsCompleted.coerceIn(0, total)
        return SegmentMap.build(total) { index -> if (index < done) 1f else 0f }
    }
    if (item.partProgress.size > 1) return item.partProgress
    PartMap.fractions(item.partMap, item.totalBytes)?.takeIf { it.size > 1 }?.let { return it }
    if (item.totalBytes <= 0L || item.splitParts <= 1) return null
    if (item.completedPartsMask == 0L && item.partOffsets.isBlank()) return null
    val parts = item.splitParts.coerceAtMost(64)
    val partSize = item.totalBytes / parts
    if (partSize <= 0L) return null
    val offsets = item.partOffsets.split(",").mapNotNull { pair ->
        val index = pair.substringBefore(":").toIntOrNull()
        val bytes = pair.substringAfter(":", "").toLongOrNull()
        if (index != null && bytes != null) index to bytes else null
    }.toMap()
    return List(parts) { index ->
        when {
            (item.completedPartsMask shr index) and 1L == 1L -> 1f
            else -> {
                val length = if (index == parts - 1) item.totalBytes - index.toLong() * partSize else partSize
                ((offsets[index] ?: 0L).toFloat() / length.coerceAtLeast(1L)).coerceIn(0f, 1f)
            }
        }
    }
}

internal fun formatMediaDuration(durationMs: Long): String {
    val total = durationMs / 1000L
    val hours = total / 3600L
    val minutes = (total % 3600L) / 60L
    val seconds = (total % 60L).toString().padStart(2, '0')
    return if (hours > 0L) "$hours:${minutes.toString().padStart(2, '0')}:$seconds" else "$minutes:$seconds"
}

internal fun formatElapsed(seconds: Long): String = when {
    seconds < 60 -> "${seconds}s"
    seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
    else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
}

internal fun formatEta(context: android.content.Context, seconds: Long): String = when {
    seconds < 60 -> context.getString(R.string.download_eta_seconds, seconds)
    seconds < 3600 -> context.getString(R.string.download_eta_minutes, seconds / 60, seconds % 60)
    else -> context.getString(R.string.download_eta_hours, seconds / 3600, (seconds % 3600) / 60)
}

internal fun sourceHost(item: DownloadItem): String? {
    val raw = item.streamPageUrl.ifBlank { item.mega?.link?.takeIf { it.isNotBlank() } ?: item.url }
    return runCatching { android.net.Uri.parse(raw).host }.getOrNull()?.removePrefix("www.")?.takeIf { it.isNotBlank() }
}

internal fun downloadKindIcon(kind: DownloadFileKind): ImageVector = when (kind) {
    DownloadFileKind.VIDEO -> Icons.Filled.Movie
    DownloadFileKind.AUDIO -> Icons.Filled.MusicNote
    DownloadFileKind.IMAGE -> Icons.Filled.Image
    DownloadFileKind.DOCUMENT -> Icons.Filled.Description
    DownloadFileKind.ARCHIVE -> Icons.Filled.FolderZip
    DownloadFileKind.APP -> Icons.Filled.Apps
    DownloadFileKind.OTHER -> Icons.AutoMirrored.Filled.InsertDriveFile
}

@Composable
internal fun fileTypePainter(filename: String): Painter {
    val ext = filename.substringAfterLast('.', "").lowercase()
    val vector: ImageVector? = when (ext) {
        "apk", "apks", "apkm", "xapk", "apkz", "aab" -> Icons.Filled.InstallMobile
        "pdf" -> Icons.Filled.PictureAsPdf
        "xls", "xlsx", "xlsm", "ods", "csv", "tsv", "numbers" -> Icons.Filled.TableChart
        "ppt", "pptx", "pptm", "odp", "key" -> Icons.Filled.Slideshow
        "epub", "mobi", "azw", "azw3", "azw4", "fb2", "djvu", "cbz", "cbr" -> Icons.AutoMirrored.Filled.MenuBook
        "srt", "vtt", "ass", "sub", "ssa", "sbv", "smi" -> Icons.Filled.Subtitles
        "kt", "kts", "java", "py", "js", "mjs", "jsx", "tsx",
        "c", "cpp", "cc", "h", "hpp", "cs", "go", "rs", "rb",
        "php", "swift", "sh", "ps1", "lua", "sql", "yaml", "yml",
        "toml", "ini", "cfg", "conf", "gradle", "json", "xml",
        "html", "htm", "css", "scss", "less", "vue", "dart", "tex" -> Icons.Filled.Code
        "ttf", "otf", "woff", "woff2", "eot" -> Icons.Filled.FontDownload
        "psd", "ai", "xd", "sketch", "fig", "indd", "eps", "odg" -> Icons.Filled.Palette
        "db", "sqlite", "sqlite3", "mdb", "accdb", "dbf" -> Icons.Filled.Storage
        "pem", "crt", "cer", "pfx", "p12", "der", "csr", "gpg", "pgp", "asc" -> Icons.Filled.Lock
        "ics", "ical", "vcs", "vcf" -> Icons.Filled.Event
        "torrent" -> Icons.Filled.CloudDownload
        else -> when (val kind = downloadFileKind(filename)) {
            DownloadFileKind.OTHER -> null
            else -> downloadKindIcon(kind)
        }
    }
    return if (vector != null) rememberVectorPainter(vector) else painterResource(R.drawable.ic_file_other_24)
}

@Composable
internal fun rememberPx(dp: Dp): Int = with(LocalDensity.current) { dp.roundToPx() }
