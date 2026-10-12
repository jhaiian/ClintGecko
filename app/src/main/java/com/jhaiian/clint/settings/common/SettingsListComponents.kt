package com.jhaiian.clint.settings.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.ui.rememberMaxContentWidth
import kotlinx.coroutines.delay
import com.jhaiian.clint.ui.theme.ClintColors
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun SettingsScreenScaffold(
    overlay: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalClintColors.current
    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        val maxContentWidth = rememberMaxContentWidth(LocalContext.current)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .then(if (maxContentWidth != null) Modifier.widthIn(max = maxContentWidth) else Modifier.fillMaxWidth())
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                content = content
            )
        }
        overlay()
    }
}

val ClintColors.dialogSectionBackground: Color
    get() = lerp(popupBackground, Color.White, 0.08f)

@Composable
fun SettingsSection(background: Color, modifier: Modifier = Modifier, elevation: Dp = 0.dp, bottomSpacing: Dp = 24.dp, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth().padding(bottom = bottomSpacing),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(content = content)
    }
}

@Composable
fun RowDivider(color: Color) {
    Box(Modifier.fillMaxWidth().height(1.dp).background(color))
}

class SettingsSearchTarget(val title: String) {
    var consumed = false
}

val LocalSettingsSearchTarget = compositionLocalOf<SettingsSearchTarget?> { null }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.settingsSearchHighlight(title: String, highlightColor: Color): Modifier {
    val target = LocalSettingsSearchTarget.current
    val requester = remember { BringIntoViewRequester() }
    val flash = remember { Animatable(0f) }
    LaunchedEffect(target) {
        if (target != null && !target.consumed && target.title == title) {
            target.consumed = true
            delay(350)
            requester.bringIntoView()
            flash.snapTo(1f)
            flash.animateTo(0f, tween(durationMillis = 1600))
        }
    }
    return this
        .bringIntoViewRequester(requester)
        .drawBehind { drawRect(highlightColor.copy(alpha = 0.22f * flash.value)) }
}

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    summary: String,
    colors: ClintColors,
    onClick: () -> Unit,
    enabled: Boolean = true,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .settingsSearchHighlight(title, colors.primary)
            .clickable(onClick = onClick)
            .alpha(if (enabled) 1f else 0.45f)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(start = 16.dp, end = 12.dp)) {
            Text(title, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(summary, color = colors.secondaryText, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
        }
        trailing()
    }
}
