package com.jhaiian.clint.ui.listscreen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.launch

@Composable
fun ListFastScroller(
    listState: LazyListState,
    itemCount: Int,
    isInteractive: Boolean,
    sectionLetterAt: (Int) -> String,
    modifier: Modifier = Modifier,
    headerItemCount: Int = 0
) {
    val colors = LocalClintColors.current
    val coroutineScope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf(0f) }
    var currentLetter by remember { mutableStateOf("") }

    val thumbWidth = 4.dp
    val thumbHeight = 44.dp
    val thumbPaddingEnd = 6.dp
    val thumbHitWidth = 32.dp
    val trackPaddingV = 20.dp
    val bubbleRadius = 26.dp
    val bubbleGap = 8.dp

    fun passiveFraction(): Float {
        if (itemCount <= 1) return 0f
        val firstIndex = (listState.firstVisibleItemIndex - headerItemCount).coerceAtLeast(0)
        val firstItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == listState.firstVisibleItemIndex }
        val itemSize = firstItem?.size?.takeIf { it > 0 } ?: 1
        val approxIndex = firstIndex + (listState.firstVisibleItemScrollOffset.toFloat() / itemSize)
        val visibleCount = listState.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1)
        val maxIndex = (itemCount - visibleCount).coerceAtLeast(1)
        return (approxIndex / maxIndex).coerceIn(0f, 1f)
    }

    fun jumpTo(fraction: Float) {
        if (itemCount == 0) return
        val index = (fraction * (itemCount - 1)).toInt().coerceIn(0, itemCount - 1)
        currentLetter = sectionLetterAt(index)
        coroutineScope.launch { listState.scrollToItem(index + headerItemCount) }
    }

    if (!isInteractive) {
        DrawFastScrollerTrack(
            modifier = modifier,
            fraction = passiveFraction(),
            isDragging = false,
            currentLetter = "",
            thumbWidth = thumbWidth, thumbHeight = thumbHeight, thumbPaddingEnd = thumbPaddingEnd,
            trackPaddingV = trackPaddingV, bubbleRadius = bubbleRadius, bubbleGap = bubbleGap,
            thumbColor = colors.primary, bubbleColor = colors.primary, textColor = colors.background,
            textMeasurer = textMeasurer
        )
        return
    }

    val fraction = if (isDragging) dragFraction else passiveFraction()

    Box(modifier = modifier) {
        DrawFastScrollerTrack(
            modifier = Modifier.fillMaxSize(),
            fraction = fraction,
            isDragging = isDragging,
            currentLetter = currentLetter,
            thumbWidth = thumbWidth, thumbHeight = thumbHeight, thumbPaddingEnd = thumbPaddingEnd,
            trackPaddingV = trackPaddingV, bubbleRadius = bubbleRadius, bubbleGap = bubbleGap,
            thumbColor = colors.primary, bubbleColor = colors.primary, textColor = colors.background,
            textMeasurer = textMeasurer
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(thumbHitWidth)
                .pointerInput(itemCount) {
                    val trackTopPx = (trackPaddingV + thumbHeight / 2f).toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        val bottom = size.height - (trackPaddingV + thumbHeight / 2f).toPx()
                        val range = (bottom - trackTopPx).coerceAtLeast(1f)
                        isDragging = true
                        dragFraction = (down.position.y.coerceIn(trackTopPx, bottom) - trackTopPx) / range
                        jumpTo(dragFraction)

                        drag(down.id) { change ->
                            change.consume()
                            val clamped = change.position.y.coerceIn(trackTopPx, bottom)
                            dragFraction = (clamped - trackTopPx) / range
                            jumpTo(dragFraction)
                        }
                        isDragging = false
                    }
                }
        )
    }
}

@Composable
private fun DrawFastScrollerTrack(
    modifier: Modifier,
    fraction: Float,
    isDragging: Boolean,
    currentLetter: String,
    thumbWidth: Dp,
    thumbHeight: Dp,
    thumbPaddingEnd: Dp,
    trackPaddingV: Dp,
    bubbleRadius: Dp,
    bubbleGap: Dp,
    thumbColor: Color,
    bubbleColor: Color,
    textColor: Color,
    textMeasurer: TextMeasurer
) {
    Canvas(modifier = modifier) {
        val trackTop = (trackPaddingV + thumbHeight / 2f).toPx()
        val trackBottom = size.height - (trackPaddingV + thumbHeight / 2f).toPx()
        val cy = trackTop + fraction * (trackBottom - trackTop).coerceAtLeast(1f)

        val thumbWPx = thumbWidth.toPx()
        val thumbHPx = thumbHeight.toPx()
        val thumbRight = size.width - thumbPaddingEnd.toPx()
        val thumbLeft = thumbRight - thumbWPx

        drawRoundRect(
            color = thumbColor.copy(alpha = if (isDragging) 1f else 0.63f),
            topLeft = Offset(thumbLeft, cy - thumbHPx / 2f),
            size = Size(thumbWPx, thumbHPx),
            cornerRadius = CornerRadius(thumbWPx / 2f, thumbWPx / 2f)
        )

        if (isDragging && currentLetter.isNotEmpty()) {
            val bubbleRadiusPx = bubbleRadius.toPx()
            val cx = thumbLeft - bubbleGap.toPx() - bubbleRadiusPx
            drawCircle(color = bubbleColor, radius = bubbleRadiusPx, center = Offset(cx, cy))

            val tail = Path().apply {
                val baseX = cx + bubbleRadiusPx - 2.dp.toPx()
                val tipX = thumbLeft - bubbleGap.toPx() + 2.dp.toPx()
                moveTo(baseX, cy - 6.dp.toPx())
                lineTo(tipX, cy)
                lineTo(baseX, cy + 6.dp.toPx())
                close()
            }
            drawPath(tail, color = bubbleColor)

            val layout = textMeasurer.measure(
                currentLetter,
                style = TextStyle(color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            )
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(cx - layout.size.width / 2f, cy - layout.size.height / 2f)
            )
        }
    }
}
