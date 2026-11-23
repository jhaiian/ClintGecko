package com.jhaiian.clint.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

interface SnackbarHostActivity {
    val snackbarHostState: SnackbarHostState
}

const val UNDO_SNACKBAR_TIMEOUT_MS = 3000L

private fun Any.recommendedSnackbarTimeout(timeoutMs: Long): Long {
    val context = this as? android.content.Context ?: return timeoutMs
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) return timeoutMs
    val manager = context.getSystemService(android.view.accessibility.AccessibilityManager::class.java) ?: return timeoutMs
    return manager.getRecommendedTimeoutMillis(
        timeoutMs.toInt(),
        android.view.accessibility.AccessibilityManager.FLAG_CONTENT_TEXT or android.view.accessibility.AccessibilityManager.FLAG_CONTENT_CONTROLS
    ).toLong()
}

fun <T> T.showClintSnackbar(
    message: String,
    actionLabel: String? = null,
    timeoutMs: Long? = null,
    onAction: (() -> Unit)? = null
) where T : SnackbarHostActivity, T : LifecycleOwner {
    lifecycleScope.launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        val timeout = timeoutMs?.let { ms ->
            val effective = recommendedSnackbarTimeout(ms)
            launch {
                delay(effective)
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        }
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = if (timeoutMs != null) SnackbarDuration.Indefinite else SnackbarDuration.Long
        )
        timeout?.cancel()
        if (result == SnackbarResult.ActionPerformed) onAction?.invoke()
    }
}

class ClintDestinationSnackbarVisuals(
    override val message: String,
    val destination: String,
    override val actionLabel: String
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Indefinite
}

class ClintTwoActionSnackbarVisuals(
    override val message: String,
    override val actionLabel: String,
    val secondaryActionLabel: String,
    val onSecondaryAction: () -> Unit
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Indefinite
}

fun <T> T.showClintTwoActionSnackbar(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    secondaryActionLabel: String,
    onSecondaryAction: () -> Unit
) where T : SnackbarHostActivity, T : LifecycleOwner {
    lifecycleScope.launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        val timeout = launch {
            delay(recommendedSnackbarTimeout(UNDO_SNACKBAR_TIMEOUT_MS))
            snackbarHostState.currentSnackbarData?.dismiss()
        }
        val result = snackbarHostState.showSnackbar(
            ClintTwoActionSnackbarVisuals(message, actionLabel, secondaryActionLabel, onSecondaryAction)
        )
        timeout.cancel()
        if (result == SnackbarResult.ActionPerformed) onAction()
    }
}

private const val DESTINATION_SNACKBAR_TIMEOUT_MS = 3000L

fun <T> T.showClintDestinationSnackbar(
    message: String,
    destination: String,
    actionLabel: String,
    onAction: () -> Unit,
    onDismissed: () -> Unit = {}
) where T : SnackbarHostActivity, T : LifecycleOwner {
    lifecycleScope.launch {
        var acted = false
        try {
            snackbarHostState.currentSnackbarData?.dismiss()
            val timeout = launch {
                delay(DESTINATION_SNACKBAR_TIMEOUT_MS)
                snackbarHostState.currentSnackbarData?.dismiss()
            }
            val result = snackbarHostState.showSnackbar(
                ClintDestinationSnackbarVisuals(message, destination, actionLabel)
            )
            timeout.cancel()
            if (result == SnackbarResult.ActionPerformed) {
                acted = true
                onAction()
            }
        } finally {
            if (!acted) onDismissed()
        }
    }
}

@Composable
private fun SwipeToDismissSnackbar(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var widthPx by remember { mutableFloatStateOf(0f) }
    val flingVelocityPx = with(LocalDensity.current) { 500.dp.toPx() }
    Box(
        modifier = Modifier
            .onSizeChanged { widthPx = it.width.toFloat() }
            .graphicsLayer {
                translationX = offsetX
                alpha = if (widthPx > 0f) (1f - abs(offsetX) / widthPx).coerceIn(0f, 1f) else 1f
            }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta -> offsetX += delta },
                onDragStopped = { velocity ->
                    val shouldDismiss = abs(offsetX) > widthPx * 0.35f ||
                        (abs(velocity) > flingVelocityPx && velocity * offsetX > 0f)
                    if (shouldDismiss) {
                        val target = if (offsetX >= 0f) widthPx else -widthPx
                        try {
                            animate(offsetX, target, animationSpec = tween(150)) { value, _ -> offsetX = value }
                        } finally {
                            onDismiss()
                        }
                    } else {
                        animate(offsetX, 0f, animationSpec = spring()) { value, _ -> offsetX = value }
                    }
                }
            )
    ) {
        content()
    }
}

@Composable
fun ClintSnackbarHost(hostState: SnackbarHostState) {
    val colors = LocalClintColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        SnackbarHost(hostState) { data ->
            val visuals = data.visuals
            SwipeToDismissSnackbar(onDismiss = { data.dismiss() }) {
                if (visuals is ClintDestinationSnackbarVisuals) {
                    Snackbar(
                        action = {
                            TextButton(onClick = { data.performAction() }) {
                                Text(visuals.actionLabel, color = colors.primary, fontWeight = FontWeight.Medium)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        containerColor = colors.popupBackground,
                        contentColor = colors.popupText
                    ) {
                        Column {
                            Text(visuals.message, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                visuals.destination,
                                color = colors.primary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else if (visuals is ClintTwoActionSnackbarVisuals) {
                    Snackbar(
                        action = {
                            Row {
                                TextButton(onClick = { data.dismiss(); visuals.onSecondaryAction() }) {
                                    Text(visuals.secondaryActionLabel, color = colors.primary, fontWeight = FontWeight.Medium)
                                }
                                TextButton(onClick = { data.performAction() }) {
                                    Text(visuals.actionLabel, color = colors.primary, fontWeight = FontWeight.Medium)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        containerColor = colors.popupBackground,
                        contentColor = colors.popupText
                    ) {
                        Text(visuals.message, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    Snackbar(
                        snackbarData = data,
                        shape = RoundedCornerShape(12.dp),
                        containerColor = colors.popupBackground,
                        contentColor = colors.popupText,
                        actionColor = colors.primary
                    )
                }
            }
        }
    }
}
