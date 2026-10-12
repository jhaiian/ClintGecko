package com.jhaiian.clint.downloads

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

sealed interface DownloadCardProgress {
    data object None : DownloadCardProgress
    data class Determinate(val fraction: Float) : DownloadCardProgress
    data object Indeterminate : DownloadCardProgress
}

@Composable
fun Modifier.downloadProgressBackground(progress: DownloadCardProgress, tint: Color): Modifier {
    val fillColor = tint.copy(alpha = 0x40 / 255f)
    return when (progress) {
        is DownloadCardProgress.None -> this
        is DownloadCardProgress.Determinate -> drawBehind {
            drawRect(color = fillColor, size = Size(size.width * progress.fraction.coerceIn(0f, 1f), size.height))
        }
        is DownloadCardProgress.Indeterminate -> {
            val transition = rememberInfiniteTransition(label = "download_indeterminate")
            val pos by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(animation = tween(1400, easing = LinearEasing), repeatMode = RepeatMode.Restart),
                label = "download_indeterminate_pos"
            )
            drawBehind {
                val bandWidth = size.width * 0.45f
                val x = pos * (size.width + bandWidth) - bandWidth
                drawRect(color = fillColor, topLeft = Offset(x, 0f), size = Size(bandWidth, size.height))
            }
        }
    }
}
