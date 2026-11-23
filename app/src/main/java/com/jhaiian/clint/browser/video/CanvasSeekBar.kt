package com.jhaiian.clint.browser.video

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun CanvasSeekBar(
    style: VideoSeekStyle,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = LocalClintColors.current.primary
    val track = Color.White.copy(alpha = 0.3f)
    val change by rememberUpdatedState(onValueChange)
    val finished by rememberUpdatedState(onValueChangeFinished)
    val phase by rememberInfiniteTransition(label = "seek").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "seekPhase"
    )
    Canvas(
        modifier
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures {
                    change((it.x / size.width).coerceIn(0f, 1f))
                    finished()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { change((it.x / size.width).coerceIn(0f, 1f)) },
                    onDragEnd = { finished() },
                    onDragCancel = { finished() },
                    onHorizontalDrag = { pointer, _ ->
                        pointer.consume()
                        change((pointer.position.x / size.width).coerceIn(0f, 1f))
                    }
                )
            }
    ) {
        val w = size.width
        val cy = size.height / 2f
        val progress = value.coerceIn(0f, 1f) * w
        when (style) {
            VideoSeekStyle.SQUIGGLE -> {
                val stroke = 4.dp.toPx()
                val amp = 4.dp.toPx()
                val wave = 22.dp.toPx()
                val step = 2.dp.toPx()
                drawLine(track, Offset(progress, cy), Offset(w, cy), stroke, StrokeCap.Round)
                val path = Path()
                path.moveTo(0f, cy)
                var x = 0f
                while (x <= progress) {
                    path.lineTo(x, cy + amp * sin(x / wave * 2f * PI.toFloat() - phase))
                    x += step
                }
                drawPath(path, primary, style = Stroke(width = stroke, cap = StrokeCap.Round))
                drawRoundRect(primary, Offset(progress - 2.dp.toPx(), cy - 10.dp.toPx()), Size(4.dp.toPx(), 20.dp.toPx()), CornerRadius(2.dp.toPx()))
            }
            else -> {
                val r = 2.dp.toPx()
                val thumb = 7.dp.toPx()
                var x = r
                while (x <= w - r) {
                    drawCircle(if (x <= progress) primary else track, r, Offset(x, cy))
                    x += 10.dp.toPx()
                }
                drawCircle(primary, thumb, Offset(progress.coerceIn(thumb, (w - thumb).coerceAtLeast(thumb)), cy))
            }
        }
    }
}
