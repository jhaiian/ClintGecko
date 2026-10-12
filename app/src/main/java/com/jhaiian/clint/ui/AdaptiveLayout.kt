package com.jhaiian.clint.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberMaxContentWidth(activity: Activity): Dp? {
    val windowSizeClass = calculateWindowSizeClass(activity)
    return when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Medium -> 700.dp
        WindowWidthSizeClass.Expanded -> 960.dp
        else -> null
    }
}

@Composable
fun rememberMaxContentWidth(context: Context): Dp? {
    val activity = context.findActivity() ?: return null
    return rememberMaxContentWidth(activity)
}

@Composable
fun AdaptiveWidthContainer(
    maxContentWidth: Dp?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .then(if (maxContentWidth != null) Modifier.widthIn(max = maxContentWidth) else Modifier),
            content = content
        )
    }
}
