package com.jhaiian.clint.browser.toolbar

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SquigglyProgress(progress: Float, color: Color, trackColor: Color, modifier: Modifier = Modifier) {
    val phase by rememberInfiniteTransition(label = "squiggle").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "squigglePhase"
    )
    Canvas(modifier) {
        val strokeWidth = 2.5.dp.toPx()
        val amplitude = 2.dp.toPx()
        val wavelength = 18.dp.toPx()
        val step = 2.dp.toPx()
        val centerY = size.height / 2f
        val end = (size.width * progress).coerceIn(0f, size.width)
        if (end < size.width) {
            drawLine(trackColor, Offset(end, centerY), Offset(size.width, centerY), strokeWidth, StrokeCap.Round)
        }
        if (end > 0f) {
            val path = Path()
            var x = 0f
            while (true) {
                val y = centerY + amplitude * sin(x / wavelength * 2f * PI.toFloat() - phase)
                if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                if (x >= end) break
                x = minOf(x + step, end)
            }
            drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
