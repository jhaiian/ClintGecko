package com.jhaiian.clint.downloads

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatFileSize
import com.jhaiian.clint.util.formatRelativeTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt

private const val TileLineHeight = 1.3f
private const val PinchStepScale = 1.35f
private const val VelocityWindowMillis = 120L
private const val FlingProjectionSeconds = 0.12f

internal val LibrarySummaryHeight = 28.dp
internal val LibrarySectionHeaderHeight = 40.dp
internal val LibraryBottomPadding = 112.dp
internal val LibraryHeaderInset = 20.dp

private val ListPaddingH = 20.dp
private val ListSubtitleGap = 2.dp
private val MetaChipGap = 4.dp
private val MoreReserve = 40.dp
private val GridHorizontalPadding = 16.dp
private val GridTextInset = 8.dp
private val GridTextBottom = 6.dp
private val GridScrimPadding = 12.dp
private val SelectedBorderWidth = 2.dp

private val ListArtSizes = intArrayOf(0, 40, 52, 66, 80, 104, 124)
private val ListArtCorners = intArrayOf(0, 8, 10, 12, 15, 18, 20)
private val ListPaddingV = intArrayOf(2, 2, 3, 5, 8, 12, 14)
private val ListArtSpacing = intArrayOf(0, 8, 10, 12, 14, 18, 20)
private val ListTrailingSpacing = intArrayOf(6, 6, 8, 10, 12, 14, 16)
private val ListTitleSizes = intArrayOf(12, 12, 14, 16, 18, 20, 22)
private val ListSubtitleSizes = intArrayOf(10, 10, 11, 12, 14, 15, 16)
private val ListMetaSizes = intArrayOf(10, 10, 11, 12, 14, 15, 16)
private val ListChipSizes = intArrayOf(8, 8, 9, 10, 11, 12, 13)
private val ListChipPaddingH = intArrayOf(4, 4, 5, 6, 8, 10, 11)
private val ListChipPaddingV = intArrayOf(1, 1, 1, 2, 3, 4, 5)
private val ListIconSizes = intArrayOf(14, 14, 18, 22, 26, 30, 34)
private val GridMinSizes = intArrayOf(150, 110, 80)
private val GridCorners = intArrayOf(16, 12, 8)
private val GridSpacings = intArrayOf(12, 9, 6)
private val GridTitleSizes = intArrayOf(14, 13, 11)
private val GridSubtitleSizes = intArrayOf(12, 11, 10)

private val TileLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

internal val StaticFraction: () -> Float = { 0f }

private const val ArtId = "art"
private const val ScrimId = "scrim"
private const val TextFromId = "text_from"
private const val TextToId = "text_to"
private const val MetaFromId = "meta_from"
private const val MetaToId = "meta_to"

private fun mix(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction

private fun IntArray.lowerBound(key: Int): Int {
    var low = 0
    var high = size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (this[mid] < key) low = mid + 1 else high = mid
    }
    return low
}

private fun IntArray.upperBound(key: Int): Int {
    var low = 0
    var high = size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (this[mid] <= key) low = mid + 1 else high = mid
    }
    return low
}

internal class TileStyle(
    val isGrid: Boolean,
    val artSize: Dp,
    val artCorner: Dp,
    val paddingH: Dp,
    val paddingV: Dp,
    val artSpacing: Dp,
    val trailingSpacing: Dp,
    val trailingReserve: Dp,
    val titleSize: TextUnit,
    val subtitleSize: TextUnit,
    val metaSize: TextUnit,
    val chipSize: TextUnit,
    val chipPaddingH: Dp,
    val chipPaddingV: Dp,
    val iconSize: Dp,
    val gridMinSize: Dp,
    val gridSpacing: Dp
) {
    val containerCorner: Dp get() = if (isGrid) artCorner else 0.dp
}

internal class TileFrame(val style: TileStyle, val width: Int, val height: Int)

private fun listLevel(mode: LibraryViewMode): Int = when (mode) {
    LibraryViewMode.LIST_NO_IMAGE -> 0
    LibraryViewMode.LIST_COMPACT -> 1
    LibraryViewMode.LIST_EXTRA_SMALL -> 2
    LibraryViewMode.LIST_SMALL -> 3
    LibraryViewMode.LIST -> 4
    LibraryViewMode.LIST_LARGE -> 5
    LibraryViewMode.LIST_BIG -> 6
    else -> 3
}

private fun gridLevel(mode: LibraryViewMode): Int = when (mode) {
    LibraryViewMode.GRID -> 0
    LibraryViewMode.GRID_SMALL -> 1
    LibraryViewMode.GRID_EXTRA_SMALL -> 2
    else -> 1
}

private fun buildTileStyle(mode: LibraryViewMode): TileStyle = if (mode.isGrid) {
    val level = gridLevel(mode)
    TileStyle(
        isGrid = true,
        artSize = 0.dp,
        artCorner = GridCorners[level].dp,
        paddingH = GridTextInset,
        paddingV = 0.dp,
        artSpacing = 0.dp,
        trailingSpacing = 0.dp,
        trailingReserve = 0.dp,
        titleSize = GridTitleSizes[level].sp,
        subtitleSize = GridSubtitleSizes[level].sp,
        metaSize = GridSubtitleSizes[level].sp,
        chipSize = GridSubtitleSizes[level].sp,
        chipPaddingH = 0.dp,
        chipPaddingV = 0.dp,
        iconSize = 32.dp,
        gridMinSize = GridMinSizes[level].dp,
        gridSpacing = GridSpacings[level].dp
    )
} else {
    val level = listLevel(mode)
    TileStyle(
        isGrid = false,
        artSize = ListArtSizes[level].dp,
        artCorner = ListArtCorners[level].dp,
        paddingH = ListPaddingH,
        paddingV = ListPaddingV[level].dp,
        artSpacing = ListArtSpacing[level].dp,
        trailingSpacing = ListTrailingSpacing[level].dp,
        trailingReserve = MoreReserve,
        titleSize = ListTitleSizes[level].sp,
        subtitleSize = ListSubtitleSizes[level].sp,
        metaSize = ListMetaSizes[level].sp,
        chipSize = ListChipSizes[level].sp,
        chipPaddingH = ListChipPaddingH[level].dp,
        chipPaddingV = ListChipPaddingV[level].dp,
        iconSize = ListIconSizes[level].dp,
        gridMinSize = 0.dp,
        gridSpacing = 0.dp
    )
}

private val TileStyles = HashMap<LibraryViewMode, TileStyle>()

internal fun tileStyleFor(mode: LibraryViewMode): TileStyle = TileStyles.getOrPut(mode) { buildTileStyle(mode) }

private fun TextStyle.tileSized(size: TextUnit): TextStyle = copy(
    fontSize = size,
    lineHeight = (size.value * TileLineHeight).sp,
    lineHeightStyle = TileLineHeightStyle
)

private fun Density.lineHeightPx(size: TextUnit): Int = ceil((size.value * TileLineHeight).sp.toPx()).toInt()

private fun Density.listRowHeight(style: TileStyle): Int {
    val text = lineHeightPx(style.titleSize) + ListSubtitleGap.roundToPx() + lineHeightPx(style.subtitleSize)
    val meta = lineHeightPx(style.metaSize) + MetaChipGap.roundToPx() +
        lineHeightPx(style.chipSize) + style.chipPaddingV.roundToPx() * 2
    val content = maxOf(style.artSize.roundToPx(), text, meta)
    return content + style.paddingV.roundToPx() * 2
}

internal class MorphSession(
    val tiles: List<DownloadItem>,
    val anchorIndex: Int,
    val anchorViewportY: Float
)

internal class ModeGeometry(
    val style: TileStyle,
    val count: Int,
    val columns: Int,
    val horizontalPadding: Int,
    private val columnWidths: IntArray,
    private val columnLefts: IntArray,
    val rowHeight: Int,
    private val lineHeight: Int,
    private val itemTops: IntArray,
    private val itemColumns: IntArray,
    private val sectionTops: IntArray,
    private val sectionHeight: Int,
    private val lazyTops: IntArray,
    private val maxScroll: Int,
    private val viewportHeight: Int
) {
    val isGrid: Boolean get() = style.isGrid
    val rowWidth: Int get() = columnWidths[0]

    fun itemLeft(index: Int): Int = columnLefts[itemColumns[index]]

    fun itemWidth(index: Int): Int = columnWidths[itemColumns[index]]

    fun itemHeight(index: Int): Int = if (isGrid) columnWidths[itemColumns[index]] else rowHeight

    fun itemTop(index: Int): Int = itemTops[index]

    fun sectionTop(section: Int): Int = sectionTops[section]

    fun sectionVisible(section: Int, scroll: Float): Boolean {
        val top = sectionTops[section]
        return top + sectionHeight > scroll && top < scroll + viewportHeight
    }

    fun contentTopOf(anchor: Int): Int = if (anchor < 0 || count == 0) 0 else itemTops[anchor.coerceAtMost(count - 1)]

    fun scrollFor(session: MorphSession): Float =
        (contentTopOf(session.anchorIndex) - session.anchorViewportY).coerceIn(0f, maxScroll.toFloat())

    fun anchorAt(scroll: Int): Int {
        if (count == 0 || scroll < itemTops[0]) return -1
        return itemTops.upperBound(scroll - lineHeight).coerceAtMost(count - 1)
    }

    fun scrollFromLazy(index: Int, offset: Int): Int = lazyTops[index.coerceIn(0, lazyTops.lastIndex)] + offset

    fun lazyPosition(scroll: Int): Pair<Int, Int> {
        val last = (lazyTops.upperBound(scroll) - 1).coerceAtLeast(0)
        val top = lazyTops[last]
        return lazyTops.lowerBound(top) to (scroll - top)
    }

    fun visibleRange(scroll: Float): IntRange {
        if (count == 0) return IntRange.EMPTY
        val first = itemTops.upperBound(floor(scroll).toInt() - lineHeight)
        val last = itemTops.lowerBound(ceil(scroll + viewportHeight).toInt()) - 1
        return if (first > last) IntRange.EMPTY else first..last
    }
}

internal fun computeModeGeometry(
    mode: LibraryViewMode,
    viewportWidth: Int,
    viewportHeight: Int,
    bottomPadding: Int,
    sectionCounts: List<Int>,
    hasHeaders: Boolean,
    density: Density
): ModeGeometry = with(density) {
    val style = tileStyleFor(mode)
    val summaryHeight = LibrarySummaryHeight.roundToPx()
    val headerHeight = LibrarySectionHeaderHeight.roundToPx()
    val count = sectionCounts.sum()
    val columns: Int
    val padding: Int
    val spacing: Int
    val widths: IntArray
    val lefts: IntArray
    val itemHeight: Int
    if (style.isGrid) {
        padding = GridHorizontalPadding.roundToPx()
        spacing = style.gridSpacing.roundToPx()
        val available = (viewportWidth - padding * 2).coerceAtLeast(1)
        columns = ((available + spacing) / (style.gridMinSize.roundToPx() + spacing)).coerceAtLeast(1)
        val withoutSpacing = (available - spacing * (columns - 1)).coerceAtLeast(0)
        val slot = withoutSpacing / columns
        val remaining = withoutSpacing % columns
        widths = IntArray(columns) { slot + if (it < remaining) 1 else 0 }
        lefts = IntArray(columns)
        var x = padding
        for (column in 0 until columns) {
            lefts[column] = x
            x += widths[column] + spacing
        }
        itemHeight = widths[0]
    } else {
        padding = 0
        spacing = 0
        columns = 1
        widths = intArrayOf(viewportWidth.coerceAtLeast(1))
        lefts = intArrayOf(0)
        itemHeight = listRowHeight(style).coerceAtLeast(1)
    }
    val itemTops = IntArray(count)
    val itemColumns = IntArray(count)
    val sectionTops = IntArray(sectionCounts.size)
    val lazyTops = IntArray(1 + (if (hasHeaders) sectionCounts.size else 0) + count)
    var y = 0
    var lazy = 0
    var item = 0
    lazyTops[lazy++] = y
    y += summaryHeight + spacing
    sectionCounts.forEachIndexed { section, sectionCount ->
        if (hasHeaders) {
            sectionTops[section] = y
            lazyTops[lazy++] = y
            y += headerHeight + spacing
        }
        var placed = 0
        while (placed < sectionCount) {
            val inLine = minOf(columns, sectionCount - placed)
            for (column in 0 until inLine) {
                itemTops[item] = y
                itemColumns[item] = column
                lazyTops[lazy++] = y
                item++
            }
            placed += inLine
            y += itemHeight + spacing
        }
    }
    val content = (y - spacing).coerceAtLeast(0) + bottomPadding
    ModeGeometry(
        style = style,
        count = count,
        columns = columns,
        horizontalPadding = padding,
        columnWidths = widths,
        columnLefts = lefts,
        rowHeight = itemHeight,
        lineHeight = itemHeight,
        itemTops = itemTops,
        itemColumns = itemColumns,
        sectionTops = sectionTops,
        sectionHeight = headerHeight,
        lazyTops = lazyTops,
        maxScroll = (content - viewportHeight).coerceAtLeast(0),
        viewportHeight = viewportHeight
    )
}

@Stable
internal class ViewModeMorphState(private val scope: CoroutineScope) {
    var session by mutableStateOf<MorphSession?>(null)
        private set
    var fromMode by mutableStateOf(LibraryViewMode.Default)
        private set
    var toMode by mutableStateOf(LibraryViewMode.Default)
        private set
    var fraction by mutableFloatStateOf(0f)
        private set

    var pinchEnabled = true
    var currentMode: () -> LibraryViewMode = { LibraryViewMode.Default }
    var captureSession: () -> MorphSession? = { null }
    var commit: suspend (LibraryViewMode) -> Unit = {}
    var onStep: () -> Unit = {}

    private val maxIndex = LibraryViewMode.entries.size - 1
    private var position = 0f
    private var lastStep = 0
    private var directMorph = false
    private var job: Job? = null
    private val samples = ArrayDeque<Pair<Long, Float>>()

    fun startPinch(): Boolean {
        if (directMorph) return false
        if (session == null && !pinchEnabled) return false
        job?.cancel()
        job = null
        if (session == null) {
            val captured = captureSession() ?: return false
            session = captured
            position = currentMode().ordinal.toFloat()
            lastStep = position.roundToInt()
            applyPosition()
        }
        samples.clear()
        return true
    }

    fun pinchBy(zoom: Float, timeMillis: Long) {
        if (session == null || directMorph || zoom <= 0f) return
        position = (position - ln(zoom) / ln(PinchStepScale)).coerceIn(0f, maxIndex.toFloat())
        samples.addLast(timeMillis to position)
        while (samples.size > 2 && timeMillis - samples.first().first > VelocityWindowMillis) {
            samples.removeFirst()
        }
        applyPosition()
    }

    fun endPinch(timeMillis: Long) {
        if (session == null || directMorph) return
        val velocity = pinchVelocity(timeMillis)
        val nearest = position.roundToInt()
        val target = (position + velocity * FlingProjectionSeconds).roundToInt()
            .coerceIn(nearest - 1, nearest + 1)
            .coerceIn(0, maxIndex)
        val start = position
        job = scope.launch {
            animate(
                initialValue = start,
                targetValue = target.toFloat(),
                initialVelocity = velocity,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
            ) { value, _ ->
                position = value.coerceIn(0f, maxIndex.toFloat())
                applyPosition()
            }
            position = target.toFloat()
            applyPosition()
            finish(LibraryViewMode.entries[target])
        }
    }

    fun morphTo(target: LibraryViewMode): Boolean {
        if (session != null) return true
        val from = currentMode()
        if (from == target) return true
        val captured = captureSession() ?: return false
        session = captured
        directMorph = true
        fromMode = from
        toMode = target
        fraction = 0f
        job = scope.launch {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 240f)
            ) { value, _ ->
                fraction = value.coerceIn(0f, 1f)
            }
            fraction = 1f
            finish(target)
        }
        return true
    }

    private suspend fun finish(mode: LibraryViewMode) {
        commit(mode)
        withFrameNanos { }
        withFrameNanos { }
        session = null
        directMorph = false
        position = mode.ordinal.toFloat()
    }

    private fun pinchVelocity(timeMillis: Long): Float {
        if (samples.size < 2) return 0f
        val first = samples.first()
        val last = samples.last()
        if (timeMillis - last.first > VelocityWindowMillis) return 0f
        val seconds = (last.first - first.first) / 1000f
        return if (seconds > 0f) (last.second - first.second) / seconds else 0f
    }

    private fun applyPosition() {
        val lower = floor(position).toInt().coerceIn(0, maxIndex - 1)
        fromMode = LibraryViewMode.entries[lower]
        toMode = LibraryViewMode.entries[lower + 1]
        fraction = (position - lower).coerceIn(0f, 1f)
        val step = position.roundToInt()
        if (step != lastStep) {
            lastStep = step
            onStep()
        }
    }
}

internal fun Modifier.viewModePinch(morph: ViewModeMorphState): Modifier = pointerInput(morph) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var pinching = false
        var lastTime = 0L
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressed = event.changes.count { it.pressed }
            lastTime = event.changes.firstOrNull()?.uptimeMillis ?: lastTime
            if (!pinching && pressed >= 2) {
                pinching = morph.startPinch()
            }
            if (pinching) {
                if (pressed >= 2) {
                    val zoom = event.calculateZoom()
                    if (zoom != 1f) morph.pinchBy(zoom, lastTime)
                }
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
        if (pinching) morph.endPinch(lastTime)
    }
}

private class TileRects(
    val artX: Float,
    val artY: Float,
    val artWidth: Float,
    val artHeight: Float,
    val textX: Float,
    val textCenterY: Float,
    val metaRight: Float,
    val metaCenterY: Float
)

private fun Density.tileRects(style: TileStyle, width: Int, height: Int, textHeight: Int): TileRects {
    val inset = style.paddingH.toPx()
    return if (style.isGrid) {
        TileRects(
            artX = 0f,
            artY = 0f,
            artWidth = width.toFloat(),
            artHeight = height.toFloat(),
            textX = inset,
            textCenterY = height - GridTextBottom.toPx() - textHeight / 2f,
            metaRight = width - inset,
            metaCenterY = height / 2f
        )
    } else {
        val art = style.artSize.toPx()
        TileRects(
            artX = inset,
            artY = (height - art) / 2f,
            artWidth = art,
            artHeight = art,
            textX = inset + art + style.artSpacing.toPx(),
            textCenterY = height / 2f,
            metaRight = width - inset - style.trailingReserve.toPx(),
            metaCenterY = height / 2f
        )
    }
}

private fun Density.textMaxWidth(style: TileStyle, width: Int, metaWidth: Int): Int {
    val inset = style.paddingH.roundToPx() * 2
    val available = if (style.isGrid) {
        width - inset
    } else {
        val meta = if (metaWidth > 0) style.trailingSpacing.roundToPx() + metaWidth else 0
        width - inset - style.artSize.roundToPx() - style.artSpacing.roundToPx() - style.trailingReserve.roundToPx() - meta
    }
    return available.coerceAtLeast(0)
}

private fun textOutAlpha(fraction: Float, crossKind: Boolean): Float =
    if (crossKind) (1f - fraction / 0.6f).coerceIn(0f, 1f) else 1f - fraction * fraction

private fun textInAlpha(fraction: Float, crossKind: Boolean): Float =
    if (crossKind) ((fraction - 0.4f) / 0.6f).coerceIn(0f, 1f) else fraction

private class TileTexts(
    val title: String,
    val gridSubtitle: String,
    val listSubtitle: String,
    val meta: String,
    val chips: List<String>
)

@Composable
private fun rememberTileTexts(item: DownloadItem, thumbnail: DownloadThumbnail?): TileTexts {
    val kind = downloadFileKind(item.filename)
    val details = libraryMetaParts(item, thumbnail, includeSize = false)
    val size = formatFileSize(item.bytesDownloaded)
    val timestamp = libraryTimestamp(item).takeIf { it > 0L }?.let { formatRelativeTimestamp(it) }
    val duration = thumbnail?.durationMs?.takeIf { it > 0L }?.let { formatMediaDuration(it) }
    val extension = item.filename.substringAfterLast('.', "").uppercase()
    return TileTexts(
        title = thumbnail?.appLabel ?: thumbnail?.title?.takeIf { kind == DownloadFileKind.AUDIO } ?: item.filename,
        gridSubtitle = listOfNotNull(size, duration).joinToString("  •  "),
        listSubtitle = (details + listOfNotNull(sourceHost(item), timestamp)).joinToString("  •  "),
        meta = size,
        chips = if (extension.isNotEmpty() && extension.length <= 5) listOf(extension) else emptyList()
    )
}

@Composable
internal fun LibraryTileView(
    item: DownloadItem,
    selected: Boolean,
    from: TileFrame,
    to: TileFrame?,
    fraction: () -> Float,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val startOrigin = if (rtl) TransformOrigin(1f, 0f) else TransformOrigin(0f, 0f)
    val endOrigin = if (rtl) TransformOrigin(0f, 0f) else TransformOrigin(1f, 0f)
    val fromStyle = from.style
    val toStyle = to?.style
    val targetStyle = toStyle ?: fromStyle
    val gridFrom = if (fromStyle.isGrid) 1f else 0f
    val gridTo = if (targetStyle.isGrid) 1f else 0f
    val crossKind = fromStyle.isGrid != targetStyle.isGrid
    val titleRatio = targetStyle.titleSize.value / fromStyle.titleSize.value
    val metaRatio = if (!fromStyle.isGrid && !targetStyle.isGrid) {
        targetStyle.metaSize.value / fromStyle.metaSize.value
    } else {
        1f
    }
    val listBackground = if (selected) colors.primary.copy(alpha = 0.16f) else colors.cardBackground.copy(alpha = 0f)
    val gridBackground = colors.cardBackground
    val selectedBorder = colors.primary
    val progress: () -> Float = if (toStyle == null) StaticFraction else fraction
    val thumbnailPx = rememberPx(160.dp)
    val thumbnail = rememberDownloadThumbnail(item, thumbnailPx)
    val texts = rememberTileTexts(item, thumbnail)

    Layout(
        content = {
            Box(
                modifier = Modifier
                    .layoutId(ArtId)
                    .graphicsLayer {
                        val t = progress().coerceIn(0f, 1f)
                        shape = RoundedCornerShape(mix(fromStyle.artCorner.toPx(), targetStyle.artCorner.toPx(), t))
                        clip = true
                    },
                contentAlignment = Alignment.Center
            ) {
                DownloadThumb(
                    item = item,
                    modifier = Modifier.fillMaxSize(),
                    requestPx = thumbnailPx,
                    corner = 0.dp,
                    iconSize = fromStyle.iconSize,
                    showDuration = false,
                    selected = selected,
                    thumbnail = thumbnail
                )
            }
            Box(
                modifier = Modifier
                    .layoutId(ScrimId)
                    .graphicsLayer { alpha = mix(gridFrom, gridTo, progress().coerceIn(0f, 1f)) }
                    .background(Color.Black.copy(alpha = 0.55f))
            )
            TileText(
                texts = texts,
                selected = selected,
                style = fromStyle,
                modifier = Modifier
                    .layoutId(TextFromId)
                    .graphicsLayer {
                        val t = progress().coerceIn(0f, 1f)
                        val scale = mix(1f, titleRatio, t)
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = startOrigin
                        alpha = textOutAlpha(t, crossKind)
                    }
            )
            if (!fromStyle.isGrid) {
                TileMeta(
                    texts = texts,
                    style = fromStyle,
                    modifier = Modifier
                        .layoutId(MetaFromId)
                        .graphicsLayer {
                            val t = progress().coerceIn(0f, 1f)
                            val scale = mix(1f, metaRatio, t)
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = endOrigin
                            alpha = when {
                                toStyle == null -> 1f
                                targetStyle.isGrid -> (1f - t * 2f).coerceIn(0f, 1f)
                                else -> 1f - t * t
                            }
                        }
                )
            }
            if (toStyle != null) {
                TileText(
                    texts = texts,
                    selected = selected,
                    style = toStyle,
                    modifier = Modifier
                        .layoutId(TextToId)
                        .graphicsLayer {
                            val t = progress().coerceIn(0f, 1f)
                            val scale = mix(1f / titleRatio, 1f, t)
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = startOrigin
                            alpha = textInAlpha(t, crossKind)
                        }
                )
                if (!toStyle.isGrid) {
                    TileMeta(
                        texts = texts,
                        style = toStyle,
                        modifier = Modifier
                            .layoutId(MetaToId)
                            .graphicsLayer {
                                val t = progress().coerceIn(0f, 1f)
                                val scale = mix(1f / metaRatio, 1f, t)
                                scaleX = scale
                                scaleY = scale
                                transformOrigin = endOrigin
                                alpha = if (fromStyle.isGrid) (t * 2f - 1f).coerceIn(0f, 1f) else t
                            }
                    )
                }
            }
        },
        modifier = modifier
            .graphicsLayer {
                val t = progress().coerceIn(0f, 1f)
                shape = RoundedCornerShape(mix(fromStyle.containerCorner.toPx(), targetStyle.containerCorner.toPx(), t))
                clip = true
            }
            .drawWithContent {
                val t = progress().coerceIn(0f, 1f)
                val gridWeight = mix(gridFrom, gridTo, t)
                val radius = mix(fromStyle.containerCorner.toPx(), targetStyle.containerCorner.toPx(), t)
                drawRoundRect(
                    color = lerp(listBackground, gridBackground, gridWeight),
                    cornerRadius = CornerRadius(radius, radius)
                )
                drawContent()
                if (selected && gridWeight > 0f) {
                    val stroke = SelectedBorderWidth.toPx()
                    val inner = (radius - stroke / 2f).coerceAtLeast(0f)
                    drawRoundRect(
                        color = selectedBorder,
                        topLeft = Offset(stroke / 2f, stroke / 2f),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius(inner, inner),
                        alpha = gridWeight,
                        style = Stroke(width = stroke)
                    )
                }
            }
    ) { measurables, constraints ->
        val t = progress().coerceIn(0f, 1f)
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val fromWidth = if (from.width > 0) from.width else width
        val fromHeight = if (from.height > 0) from.height else height
        val toWidth = if (to != null && to.width > 0) to.width else width
        val toHeight = if (to != null && to.height > 0) to.height else height

        val metaFrom = measurables.firstOrNull { it.layoutId == MetaFromId }?.measure(Constraints())
        val metaTo = measurables.firstOrNull { it.layoutId == MetaToId }?.measure(Constraints())
        val textFrom = measurables.first { it.layoutId == TextFromId }
            .measure(Constraints(maxWidth = textMaxWidth(fromStyle, fromWidth, metaFrom?.width ?: 0)))
        val textTo = measurables.firstOrNull { it.layoutId == TextToId }
            ?.measure(Constraints(maxWidth = textMaxWidth(targetStyle, toWidth, metaTo?.width ?: 0)))

        val fromRects = tileRects(fromStyle, fromWidth, fromHeight, textFrom.height)
        val toRects = if (textTo != null) tileRects(targetStyle, toWidth, toHeight, textTo.height) else fromRects

        val art = measurables.first { it.layoutId == ArtId }.measure(
            Constraints.fixed(
                mix(fromRects.artWidth, toRects.artWidth, t).roundToInt().coerceAtLeast(0),
                mix(fromRects.artHeight, toRects.artHeight, t).roundToInt().coerceAtLeast(0)
            )
        )

        val scrimPadding = GridScrimPadding.roundToPx()
        val scrimFrom = if (fromStyle.isGrid) textFrom.height + scrimPadding else 0
        val scrimTo = if (targetStyle.isGrid) (textTo?.height ?: textFrom.height) + scrimPadding else 0
        val scrimHeight = when {
            fromStyle.isGrid && targetStyle.isGrid -> mix(scrimFrom.toFloat(), scrimTo.toFloat(), t).roundToInt()
            fromStyle.isGrid -> scrimFrom
            targetStyle.isGrid -> scrimTo
            else -> 0
        }.coerceIn(0, height)
        val scrim = measurables.first { it.layoutId == ScrimId }.measure(Constraints.fixed(width, scrimHeight))

        val textX = mix(fromRects.textX, toRects.textX, t).roundToInt()
        val textCenter = mix(fromRects.textCenterY, toRects.textCenterY, t)
        val textScaleFrom = mix(1f, titleRatio, t)
        val textScaleTo = mix(1f / titleRatio, 1f, t)
        val metaRight = mix(fromRects.metaRight, toRects.metaRight, t)
        val metaCenter = mix(fromRects.metaCenterY, toRects.metaCenterY, t)
        val metaScaleFrom = mix(1f, metaRatio, t)
        val metaScaleTo = mix(1f / metaRatio, 1f, t)

        layout(width, height) {
            art.placeRelative(
                mix(fromRects.artX, toRects.artX, t).roundToInt(),
                mix(fromRects.artY, toRects.artY, t).roundToInt()
            )
            scrim.placeRelative(0, height - scrim.height)
            textFrom.placeRelative(textX, (textCenter - textFrom.height * textScaleFrom / 2f).roundToInt())
            if (textTo != null) {
                textTo.placeRelative(textX, (textCenter - textTo.height * textScaleTo / 2f).roundToInt())
            }
            if (metaFrom != null) {
                metaFrom.placeRelative(
                    (metaRight - metaFrom.width).roundToInt(),
                    (metaCenter - metaFrom.height * metaScaleFrom / 2f).roundToInt()
                )
            }
            if (metaTo != null) {
                metaTo.placeRelative(
                    (metaRight - metaTo.width).roundToInt(),
                    (metaCenter - metaTo.height * metaScaleTo / 2f).roundToInt()
                )
            }
        }
    }
}

@Composable
private fun TileText(
    texts: TileTexts,
    selected: Boolean,
    style: TileStyle,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val typography = MaterialTheme.typography
    val titleColor = when {
        style.isGrid -> Color.White
        selected -> colors.primary
        else -> colors.onSurface
    }
    val subtitleColor = if (style.isGrid) Color.White.copy(alpha = 0.78f) else colors.secondaryText
    val subtitle = if (style.isGrid) texts.gridSubtitle else texts.listSubtitle
    Column(modifier = modifier) {
        Text(
            text = texts.title,
            style = typography.bodyLarge.tileSized(style.titleSize),
            fontWeight = FontWeight.SemiBold,
            color = titleColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.MiddleEllipsis
        )
        Text(
            text = subtitle,
            style = typography.bodySmall.tileSized(style.subtitleSize),
            color = subtitleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (style.isGrid) Modifier else Modifier.padding(top = ListSubtitleGap)
        )
    }
}

@Composable
private fun TileMeta(
    texts: TileTexts,
    style: TileStyle,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        Text(
            text = texts.meta,
            style = MaterialTheme.typography.labelMedium.tileSized(style.metaSize),
            color = colors.secondaryText,
            maxLines = 1
        )
        if (texts.chips.isNotEmpty()) {
            Row(modifier = Modifier.padding(top = MetaChipGap)) {
                texts.chips.forEachIndexed { position, chip ->
                    if (position > 0) Spacer(modifier = Modifier.width(4.dp))
                    TileChip(text = chip, style = style)
                }
            }
        }
    }
}

@Composable
private fun TileChip(text: String, style: TileStyle) {
    val colors = LocalClintColors.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.tileSized(style.chipSize),
        fontWeight = FontWeight.Bold,
        color = colors.primary,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.primary.copy(alpha = 0.14f))
            .padding(horizontal = style.chipPaddingH, vertical = style.chipPaddingV)
    )
}

@Composable
internal fun ViewModeMorphOverlay(
    morph: ViewModeMorphState,
    session: MorphSession,
    geometries: Map<LibraryViewMode, ModeGeometry>,
    selectedIds: Set<Int>,
    sectionCount: Int,
    summary: @Composable () -> Unit,
    sectionHeader: @Composable (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val fromGeometry = geometries.getValue(morph.fromMode)
    val toGeometry = geometries.getValue(morph.toMode)
    val fromScroll = fromGeometry.scrollFor(session)
    val toScroll = toGeometry.scrollFor(session)
    val fromRange = fromGeometry.visibleRange(fromScroll)
    val toRange = toGeometry.visibleRange(toScroll)
    val tiles = session.tiles
    val fraction: () -> Float = { morph.fraction }
    val ordered = remember(fromRange, toRange) {
        val fading = (fromRange.toList() + toRange.toList())
            .distinct()
            .filter { (it in fromRange) != (it in toRange) }
        val moving = fromRange.filter { it in toRange }
        fading + moving
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, mix(-fromScroll, -toScroll, morph.fraction).roundToInt()) }
        ) {
            summary()
        }
        for (section in 0 until sectionCount) {
            if (!fromGeometry.sectionVisible(section, fromScroll) && !toGeometry.sectionVisible(section, toScroll)) continue
            val fromTop = fromGeometry.sectionTop(section) - fromScroll
            val toTop = toGeometry.sectionTop(section) - toScroll
            key(section) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, mix(fromTop, toTop, morph.fraction).roundToInt()) }
                ) {
                    sectionHeader(section)
                }
            }
        }
        for (index in ordered) {
            val item = tiles.getOrNull(index) ?: continue
            val isSelected = item.id in selectedIds
            val inFrom = index in fromRange
            val inTo = index in toRange
            key(item.id) {
                if (inFrom && inTo) {
                    val fromX = fromGeometry.itemLeft(index).toFloat()
                    val fromY = fromGeometry.itemTop(index) - fromScroll
                    val fromWidth = fromGeometry.itemWidth(index)
                    val fromHeight = fromGeometry.itemHeight(index)
                    val toX = toGeometry.itemLeft(index).toFloat()
                    val toY = toGeometry.itemTop(index) - toScroll
                    val toWidth = toGeometry.itemWidth(index)
                    val toHeight = toGeometry.itemHeight(index)
                    LibraryTileView(
                        item = item,
                        selected = isSelected,
                        from = TileFrame(fromGeometry.style, fromWidth, fromHeight),
                        to = TileFrame(toGeometry.style, toWidth, toHeight),
                        fraction = fraction,
                        modifier = Modifier.layout { measurable, constraints ->
                            val t = morph.fraction
                            val placeable = measurable.measure(
                                Constraints.fixed(
                                    mix(fromWidth.toFloat(), toWidth.toFloat(), t).roundToInt().coerceAtLeast(0),
                                    mix(fromHeight.toFloat(), toHeight.toFloat(), t).roundToInt().coerceAtLeast(0)
                                )
                            )
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.placeRelative(mix(fromX, toX, t).roundToInt(), mix(fromY, toY, t).roundToInt())
                            }
                        }
                    )
                } else {
                    val geometry = if (inFrom) fromGeometry else toGeometry
                    val scroll = if (inFrom) fromScroll else toScroll
                    val x = geometry.itemLeft(index)
                    val y = (geometry.itemTop(index) - scroll).roundToInt()
                    val width = geometry.itemWidth(index)
                    val height = geometry.itemHeight(index)
                    LibraryTileView(
                        item = item,
                        selected = isSelected,
                        from = TileFrame(geometry.style, width, height),
                        to = null,
                        fraction = StaticFraction,
                        modifier = Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(Constraints.fixed(width, height))
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.placeRelativeWithLayer(x, y) {
                                    val t = morph.fraction
                                    val visibility = if (inFrom) 1f - t else t
                                    alpha = visibility
                                    scaleX = 0.92f + 0.08f * visibility
                                    scaleY = 0.92f + 0.08f * visibility
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
