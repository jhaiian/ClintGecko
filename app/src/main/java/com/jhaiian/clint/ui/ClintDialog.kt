package com.jhaiian.clint.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.jhaiian.clint.R
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

val ClintDialogContentMaxHeight = 440.dp

private val ClintDialogChromeHeight = 140.dp

@Composable
internal fun ClintDialogStatusBarEffect(hideStatusBar: Boolean, hideSystemNavigation: Boolean) {
    val view = LocalView.current
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val activity = context as? ClintActivity
        activity?.trackDialogShown()
        onDispose { activity?.trackDialogDismissed() }
    }

    DisposableEffect(hideStatusBar, hideSystemNavigation) {
        val window = (view.parent as? DialogWindowProvider)?.window
        if (window != null) {
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (hideStatusBar) {
                controller.hide(WindowInsetsCompat.Type.statusBars())
            } else {
                controller.show(WindowInsetsCompat.Type.statusBars())
            }
            if (hideSystemNavigation) {
                controller.hide(WindowInsetsCompat.Type.navigationBars())
            } else {
                controller.show(WindowInsetsCompat.Type.navigationBars())
            }
        }
        onDispose {}
    }
}

fun Modifier.scrollToSelection(scrollState: ScrollState, selected: Boolean): Modifier = composed {
    val coroutineScope = rememberCoroutineScope()
    var hasScrolled by remember { mutableStateOf(false) }
    if (selected) {
        onGloballyPositioned { coordinates ->
            if (!hasScrolled) {
                hasScrolled = true
                val targetY = coordinates.positionInParent().y.roundToInt().coerceAtLeast(0)
                coroutineScope.launch { scrollState.scrollTo(targetY) }
            }
        }
    } else this
}

@Composable
fun ClintDialogCancelFooter(onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ClintDialogActionFooter(
    onCancel: () -> Unit,
    positiveLabel: String,
    onPositive: () -> Unit,
    positiveEnabled: Boolean = true,
    cancelLabel: String = stringResource(R.string.action_cancel)
) {
    val colors = LocalClintColors.current
    Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onCancel) {
            Text(cancelLabel, color = colors.primary, fontWeight = FontWeight.Medium)
        }
        TextButton(onClick = onPositive, enabled = positiveEnabled) {
            Text(
                positiveLabel,
                color = if (positiveEnabled) colors.primary else colors.secondaryText,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ClintDialog(
    title: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    cancelable: Boolean = true,
    footer: @Composable () -> Unit = { ClintDialogCancelFooter(onDismiss) },
    scrollState: ScrollState = rememberScrollState(),
    compact: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalClintColors.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
            dismissOnBackPress = cancelable,
            dismissOnClickOutside = cancelable
        )
    ) {
        ClintDialogStatusBarEffect(hideStatusBar, hideSystemNavigation)
        BoxWithConstraints {
            val maxContentHeight = (maxHeight - ClintDialogChromeHeight)
                .coerceIn(0.dp, ClintDialogContentMaxHeight)
            Surface(shape = RoundedCornerShape(if (compact) 20.dp else 24.dp), color = colors.popupBackground) {
                Column {
                    Text(
                        title,
                        color = colors.onSurface,
                        fontSize = if (compact) 16.sp else 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = if (compact) {
                            Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp)
                        } else {
                            Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)
                        }
                    )
                    Column(
                        Modifier
                            .heightIn(max = maxContentHeight)
                            .verticalScroll(scrollState)
                            .padding(horizontal = 8.dp)
                    ) { content() }
                    footer()
                }
            }
        }
    }
}

@Composable
fun ClintTitlelessDialog(
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    cancelable: Boolean = true,
    footer: @Composable () -> Unit = { ClintDialogCancelFooter(onDismiss) },
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalClintColors.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
            dismissOnBackPress = cancelable,
            dismissOnClickOutside = cancelable
        )
    ) {
        ClintDialogStatusBarEffect(hideStatusBar, hideSystemNavigation)
        BoxWithConstraints {
            val maxContentHeight = (maxHeight - ClintDialogChromeHeight)
                .coerceIn(0.dp, ClintDialogContentMaxHeight)
            Surface(shape = RoundedCornerShape(24.dp), color = colors.popupBackground) {
                Column {
                    Column(
                        Modifier
                            .heightIn(max = maxContentHeight)
                            .verticalScroll(scrollState)
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    ) { content() }
                    footer()
                }
            }
        }
    }
}
