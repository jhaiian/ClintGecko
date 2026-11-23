package com.jhaiian.clint.setup

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.rememberClintFavicon
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.BreakIterator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

private const val TAU = (2.0 * PI).toFloat()

@Composable
internal fun rememberLoopClock(periodMillis: Int, label: String): State<Float> {
    val transition = rememberInfiniteTransition(label = label)
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing)),
        label = label
    )
}

@Composable
private fun rememberEntrances(count: Int): List<Animatable<Float, AnimationVector1D>> {
    val entrance = LocalSetupEntrance.current
    return remember { List(count) { Animatable(if (entrance) 0f else 1f) } }
}

private fun ease(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

private fun window(t: Float, start: Float, end: Float): Float = ((t - start) / (end - start)).coerceIn(0f, 1f)

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

@Composable
internal fun FittedBox(designWidth: Dp, designHeight: Dp, modifier: Modifier = Modifier, fill: Float = 0.9f, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val scale = min(maxWidth.value * fill / designWidth.value, maxHeight.value * fill / designHeight.value).coerceIn(0.4f, 1.25f)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .requiredSize(designWidth, designHeight)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            content()
        }
    }
}

private fun DrawScope.drawClintMark(
    center: Offset,
    radius: Float,
    color: Color,
    circle: Float = 1f,
    axis: Float = 1f,
    equator: Float = 1f,
    meridians: Float = 1f,
    pointer: Float = 1f,
    chevron: Float = 1f,
    spin: Float = 0f
) {
    val u = radius / 22f
    if (circle > 0f) {
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * circle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = 3.8f * u, cap = StrokeCap.Round)
        )
    }
    if (equator > 0f) {
        drawLine(color, Offset(center.x - radius * equator, center.y), Offset(center.x + radius * equator, center.y), 2.7f * u, StrokeCap.Round)
    }
    if (meridians > 0f) {
        for (k in 0 until 3) {
            val raw = spin * TAU + k * TAU / 3f + PI.toFloat() / 5f
            val lambda = ((raw + PI.toFloat()) % TAU + TAU) % TAU - PI.toFloat()
            val facing = cos(lambda)
            if (facing <= 0f) continue
            val offset = 2f * radius * sin(lambda)
            val path = Path().apply {
                moveTo(center.x, center.y - radius)
                quadraticTo(center.x + offset, center.y, center.x, center.y + radius)
            }
            drawPath(path, color.copy(alpha = color.alpha * meridians * facing.pow(0.6f)), style = Stroke(width = 2.7f * u, cap = StrokeCap.Round))
        }
    }
    if (axis > 0f) {
        drawLine(color, Offset(center.x, center.y - 22f * u * axis), Offset(center.x, center.y + 26f * u * axis), 2.7f * u, StrokeCap.Round)
    }
    if (pointer > 0f) {
        scale(pointer, Offset(center.x, center.y - 24f * u)) {
            val tri = Path().apply {
                moveTo(center.x, center.y - 32f * u)
                lineTo(center.x - 9f * u, center.y - 22f * u)
                lineTo(center.x + 9f * u, center.y - 22f * u)
                close()
            }
            drawPath(tri, color)
        }
    }
    if (chevron > 0f) {
        val path = Path().apply {
            moveTo(center.x - 4f * u, center.y + 26f * u)
            lineTo(center.x, center.y + 30f * u)
            lineTo(center.x + 4f * u, center.y + 26f * u)
        }
        val measure = PathMeasure()
        measure.setPath(path, false)
        val segment = Path()
        measure.getSegment(0f, measure.length * chevron.coerceIn(0f, 1f), segment, true)
        drawPath(segment, color, style = Stroke(width = 2.4f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
internal fun ClintMarkHero(modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val parts = rememberEntrances(8)
    val spin = rememberLoopClock(14000, "mark_spin")
    val pulse = rememberLoopClock(3200, "mark_pulse")
    val orbit = rememberLoopClock(9000, "mark_orbit")
    val float = rememberLoopClock(5200, "mark_float")
    LaunchedEffect(Unit) {
        if (parts.all { it.value >= 1f }) return@LaunchedEffect
        launch { parts[0].animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 160f)) }
        launch { parts[1].animateTo(1f, tween(760, easing = FastOutSlowInEasing)) }
        launch { delay(360); parts[2].animateTo(1f, tween(460, easing = FastOutSlowInEasing)) }
        launch { delay(520); parts[3].animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
        launch { delay(660); parts[4].animateTo(1f, tween(520)) }
        launch { delay(820); parts[5].animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 320f)) }
        launch { delay(940); parts[6].animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
        launch { delay(1040); parts[7].animateTo(1f, tween(640, easing = LinearOutSlowInEasing)) }
    }
    val primary = colors.primary
    Canvas(modifier) {
        val radius = size.minDimension * 0.24f
        val bob = sin(float.value * TAU) * radius * 0.05f
        val c = Offset(center.x, center.y + radius * 0.12f + bob)
        val glow = parts[0].value
        val orbitIn = parts[7].value
        drawCircle(primary.copy(alpha = 0.10f), radius * 1.6f * glow, c)
        drawCircle(primary.copy(alpha = 0.06f), radius * 2.1f * glow, c)
        for (i in 0 until 2) {
            val p = (pulse.value + i * 0.5f) % 1f
            val alpha = (1f - p) * 0.35f * orbitIn
            if (alpha > 0f) drawCircle(primary.copy(alpha = alpha), radius * (1.1f + 0.9f * p), c, style = Stroke(1.5.dp.toPx()))
        }
        val rx = radius * 1.62f
        val ry = radius * 0.46f
        val tilt = -18f
        val satellites = List(2) { i -> orbit.value * TAU + i * PI.toFloat() }
        fun satellite(angle: Float, front: Boolean) {
            val behind = sin(angle) < 0f
            if (behind == front) return
            val x = cos(angle) * rx
            val y = sin(angle) * ry
            rotate(tilt, c) {
                drawCircle(primary.copy(alpha = orbitIn * if (front) 1f else 0.45f), radius * (if (front) 0.11f else 0.08f), Offset(c.x + x, c.y + y))
            }
        }
        if (orbitIn > 0f) {
            rotate(tilt, c) {
                drawOval(
                    color = primary.copy(alpha = 0.22f * orbitIn),
                    topLeft = Offset(c.x - rx, c.y - ry),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(1.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())))
                )
            }
            satellites.forEach { satellite(it, front = false) }
        }
        drawClintMark(
            center = c,
            radius = radius,
            color = primary,
            circle = parts[1].value,
            axis = parts[2].value,
            equator = parts[3].value,
            meridians = parts[4].value,
            pointer = parts[5].value,
            chevron = parts[6].value,
            spin = spin.value
        )
        if (orbitIn > 0f) satellites.forEach { satellite(it, front = true) }
    }
}

@Composable
internal fun ClintMarkSmall(modifier: Modifier = Modifier, color: Color = LocalClintColors.current.primary) {
    val spin = rememberLoopClock(14000, "mark_small_spin")
    Canvas(modifier) {
        val radius = size.minDimension / 2f * (22f / 32f)
        drawClintMark(center = Offset(center.x, center.y + radius * 0.06f), radius = radius, color = color, spin = spin.value)
    }
}

@Composable
internal fun FloatingIconsHero(icon: ImageVector, satellites: List<ImageVector>, modifier: Modifier = Modifier, inward: Boolean = true) {
    val colors = LocalClintColors.current
    val clock = rememberLoopClock(6400, "floating_icons")
    val ring = rememberLoopClock(2600, "floating_ring")
    val pop = rememberEntrances(1)[0]
    LaunchedEffect(Unit) { if (pop.value < 1f) pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 180f)) }
    val primary = colors.primary
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val tile = (minOf(maxWidth, maxHeight) * 0.34f).coerceIn(72.dp, 150.dp)
        satellites.forEachIndexed { index, vector ->
            val angle = index * TAU / satellites.size - PI.toFloat() / 2f
            Icon(
                imageVector = vector,
                contentDescription = null,
                tint = if (index % 2 == 0) colors.primary else colors.secondaryText,
                modifier = Modifier
                    .size(tile * 0.26f)
                    .graphicsLayer {
                        val p = (clock.value + index.toFloat() / satellites.size) % 1f
                        val travel = if (inward) 1f - p else p
                        val distance = tile.toPx() * (0.62f + 0.95f * travel)
                        val swirl = angle + (if (inward) p else -p) * 0.9f
                        translationX = cos(swirl) * distance
                        translationY = sin(swirl) * distance
                        alpha = sin(p * PI.toFloat()).coerceAtLeast(0f).pow(0.8f) * pop.value.coerceIn(0f, 1f)
                        val s = if (inward) 1.1f - 0.45f * p else 0.7f + 0.4f * p
                        scaleX = s
                        scaleY = s
                    }
            )
        }
        Box(
            Modifier
                .size(tile * 1.28f)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                    rotationZ = ring.value * 360f
                }
                .drawBehind {
                    drawArc(
                        color = primary.copy(alpha = 0.55f),
                        startAngle = 0f,
                        sweepAngle = 110f,
                        useCenter = false,
                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = primary.copy(alpha = 0.25f),
                        startAngle = 180f,
                        sweepAngle = 70f,
                        useCenter = false,
                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(tile)
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                }
                .clip(RoundedCornerShape(tile * 0.3f))
                .background(colors.primary)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(tile * 0.48f)
            )
        }
    }
}

internal val RestoreSatellites = listOf(
    Icons.Outlined.Bookmark,
    Icons.Outlined.History,
    Icons.Outlined.Tab,
    Icons.Outlined.Password,
    Icons.Outlined.Download,
    Icons.Outlined.Settings
)

@Composable
private fun SkeletonLine(widthFraction: Float, color: Color, shimmer: Float, height: Dp = 8.dp, modifier: Modifier = Modifier) {
    val highlight = LocalClintColors.current.primary
    Box(
        modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(CircleShape)
            .background(color)
            .drawBehind {
                val x = (shimmer * 1.6f - 0.3f) * size.width
                drawRect(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(Color.Transparent, highlight.copy(alpha = 0.18f), Color.Transparent),
                        startX = x - size.width * 0.25f,
                        endX = x + size.width * 0.25f
                    )
                )
            }
    )
}

@Composable
internal fun BrowserPreviewHero(modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val loading = rememberLoopClock(3600, "preview_loading")
    val shimmer = rememberLoopClock(2400, "preview_shimmer")
    val pop = rememberEntrances(1)[0]
    LaunchedEffect(Unit) { if (pop.value < 1f) pop.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 160f)) }
    val primary = colors.primary
    val line = colors.onSurface.copy(alpha = 0.12f)
    FittedBox(270.dp, 300.dp, modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val v = pop.value
                    scaleX = 0.9f + 0.1f * v
                    scaleY = 0.9f + 0.1f * v
                    alpha = v.coerceIn(0f, 1f)
                }
                .clip(RoundedCornerShape(28.dp))
                .background(colors.cardBackground)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceVariant)
                        .padding(horizontal = 10.dp)
                ) {
                    Icon(Icons.Outlined.Lock, null, tint = primary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(8.dp))
                    SkeletonLine(0.7f, line, shimmer.value)
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(22.dp)
                        .border(2.dp, colors.secondaryText, RoundedCornerShape(6.dp))
                )
                Icon(Icons.Outlined.MoreVert, null, tint = colors.secondaryText, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .drawBehind {
                        val t = loading.value
                        val w = size.width * ease(window(t, 0f, 0.7f))
                        val fade = 1f - window(t, 0.75f, 0.95f)
                        drawRoundRect(primary.copy(alpha = fade), size = Size(w, size.height), cornerRadius = CornerRadius(size.height / 2f))
                    }
            )
            Spacer(Modifier.height(10.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(primary.copy(alpha = 0.16f))
            ) {
                ClintMarkSmall(Modifier.fillMaxSize(0.62f))
            }
            Spacer(Modifier.height(12.dp))
            SkeletonLine(0.85f, colors.onSurface.copy(alpha = 0.2f), shimmer.value, height = 10.dp)
            Spacer(Modifier.height(8.dp))
            SkeletonLine(1f, line, shimmer.value)
            Spacer(Modifier.height(6.dp))
            SkeletonLine(0.6f, line, shimmer.value)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                repeat(2) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (i == 0) primary.copy(alpha = 0.22f) else colors.surfaceVariant)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = colors.secondaryText, modifier = Modifier.size(20.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = colors.secondaryText.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(primary)
                ) {
                    Icon(Icons.Outlined.Home, null, tint = colors.onPrimary, modifier = Modifier.size(20.dp))
                }
                Icon(Icons.Outlined.Tab, null, tint = colors.secondaryText, modifier = Modifier.size(20.dp))
                Icon(Icons.Outlined.Settings, null, tint = colors.secondaryText, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
internal fun LayoutPreviewHero(addressBarPosition: String, menuStyle: String, hideOnScroll: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val clock = rememberLoopClock(7600, "layout_clock")
    val pop = rememberEntrances(1)[0]
    LaunchedEffect(Unit) { if (pop.value < 1f) pop.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = 170f)) }
    val springSpec = spring<Float>(dampingRatio = 0.72f, stiffness = 260f)
    val topVisible by animateFloatAsState(if (addressBarPosition != "bottom") 1f else 0f, springSpec, label = "layout_top")
    val bottomAddress by animateFloatAsState(if (addressBarPosition == "bottom") 1f else 0f, springSpec, label = "layout_bottom")
    val bottomNav by animateFloatAsState(if (addressBarPosition == "split") 1f else 0f, springSpec, label = "layout_nav")
    val hideFactor by animateFloatAsState(if (hideOnScroll) 1f else 0f, tween(420), label = "layout_hide")
    val sheet by animateFloatAsState(if (menuStyle == "bottom_sheet") 1f else 0f, tween(360), label = "layout_sheet")
    val background = colors.background
    val bar = colors.cardBackground
    val pill = colors.surfaceVariant
    val line = colors.onSurface.copy(alpha = 0.12f)
    val primary = colors.primary
    val frame = colors.divider
    val popup = colors.popupBackground
    val secondary = colors.secondaryText
    FittedBox(180.dp, 330.dp, modifier, fill = 0.92f) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val v = pop.value
                    alpha = v.coerceIn(0f, 1f)
                    translationY = (1f - v) * 40.dp.toPx()
                }
        ) {
            val dp = 1.dp.toPx()
            val w = size.width
            val h = size.height
            val corner = 30f * dp
            drawRoundRect(frame, size = size, cornerRadius = CornerRadius(corner))
            val inset = 5f * dp
            val sw = w - inset * 2f
            val sh = h - inset * 2f
            val t = clock.value
            val scroll = ease(window(t, 0.02f, 0.30f)) - ease(window(t, 0.38f, 0.62f))
            val hide = scroll * hideFactor
            val menuT = ease(window(t, 0.70f, 0.78f)) - ease(window(t, 0.88f, 0.96f))
            translate(inset, inset) {
                val screenPath = Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, sw, sh, CornerRadius(corner - inset)))
                }
                clipPath(screenPath) {
                    drawRect(background, size = Size(sw, sh))
                    val statusH = 18f * dp
                    val barH = 34f * dp
                    val topSpace = statusH + barH * topVisible * (1f - hide)
                    val contentShift = -scroll * 160f * dp
                    clipRect(0f, statusH, sw, sh) {
                        var y = topSpace + 10f * dp + contentShift
                        val pad = 12f * dp
                        repeat(4) { block ->
                            drawRoundRect(
                                if (block % 2 == 0) primary.copy(alpha = 0.16f) else pill,
                                topLeft = Offset(pad, y),
                                size = Size(sw - pad * 2f, 78f * dp),
                                cornerRadius = CornerRadius(14f * dp)
                            )
                            y += 88f * dp
                            listOf(0.8f, 1f, 0.55f).forEach { f ->
                                drawRoundRect(line, topLeft = Offset(pad, y), size = Size((sw - pad * 2f) * f, 7f * dp), cornerRadius = CornerRadius(4f * dp))
                                y += 13f * dp
                            }
                            y += 10f * dp
                        }
                    }
                    fun addressBar(top: Float, alpha: Float) {
                        if (alpha <= 0f) return
                        drawRect(bar.copy(alpha = bar.alpha * alpha), topLeft = Offset(0f, top), size = Size(sw, barH))
                        val pillH = 24f * dp
                        val pillTop = top + (barH - pillH) / 2f
                        drawRoundRect(pill.copy(alpha = pill.alpha * alpha), topLeft = Offset(10f * dp, pillTop), size = Size(sw - 44f * dp, pillH), cornerRadius = CornerRadius(pillH / 2f))
                        drawCircle(primary.copy(alpha = alpha), 4f * dp, Offset(22f * dp, pillTop + pillH / 2f))
                        drawRoundRect(line.copy(alpha = line.alpha * alpha), topLeft = Offset(32f * dp, pillTop + pillH / 2f - 3f * dp), size = Size(sw * 0.38f, 6f * dp), cornerRadius = CornerRadius(3f * dp))
                        for (d in 0 until 3) {
                            drawCircle(secondary.copy(alpha = alpha), 1.8f * dp, Offset(sw - 18f * dp, pillTop + pillH / 2f + (d - 1) * 5f * dp))
                        }
                    }
                    val topBarY = statusH - barH * (1f - topVisible) - barH * hide
                    addressBar(topBarY, topVisible)
                    val bottomBarY = sh - barH + barH * (1f - bottomAddress) + barH * hide
                    addressBar(bottomBarY, bottomAddress)
                    if (bottomNav > 0f) {
                        val navY = sh - barH * bottomNav + barH * hide
                        drawRect(bar, topLeft = Offset(0f, navY), size = Size(sw, barH))
                        for (i in 0 until 4) {
                            val cx = sw * (i + 0.5f) / 4f
                            drawCircle(if (i == 0) primary else secondary.copy(alpha = 0.6f), 5f * dp, Offset(cx, navY + barH / 2f))
                        }
                    }
                    drawRect(background, size = Size(sw, statusH))
                    drawRoundRect(secondary.copy(alpha = 0.5f), topLeft = Offset(14f * dp, 6f * dp), size = Size(22f * dp, 6f * dp), cornerRadius = CornerRadius(3f * dp))
                    drawRoundRect(secondary.copy(alpha = 0.5f), topLeft = Offset(sw - 36f * dp, 6f * dp), size = Size(22f * dp, 6f * dp), cornerRadius = CornerRadius(3f * dp))
                    if (menuT > 0f) {
                        val popupAlpha = (1f - sheet) * menuT
                        if (popupAlpha > 0f) {
                            val atTop = addressBarPosition != "bottom"
                            val mw = sw * 0.56f
                            val mh = 120f * dp
                            val mx = sw - mw - 8f * dp
                            val my = if (atTop) statusH + barH + 4f * dp else sh - barH - mh - 4f * dp
                            val pivot = Offset(sw - 12f * dp, if (atTop) my else my + mh)
                            scale(0.6f + 0.4f * menuT, pivot) {
                                drawRoundRect(popup.copy(alpha = popupAlpha), topLeft = Offset(mx, my), size = Size(mw, mh), cornerRadius = CornerRadius(14f * dp))
                                drawRoundRect(frame.copy(alpha = frame.alpha * popupAlpha), topLeft = Offset(mx, my), size = Size(mw, mh), cornerRadius = CornerRadius(14f * dp), style = Stroke(1f * dp))
                                for (r in 0 until 5) {
                                    val ry = my + 14f * dp + r * 21f * dp
                                    drawCircle(primary.copy(alpha = popupAlpha), 4f * dp, Offset(mx + 14f * dp, ry + 3f * dp))
                                    drawRoundRect(line.copy(alpha = line.alpha * popupAlpha), topLeft = Offset(mx + 26f * dp, ry), size = Size(mw * (0.6f - (r % 2) * 0.15f), 6f * dp), cornerRadius = CornerRadius(3f * dp))
                                }
                            }
                        }
                        val sheetAlpha = sheet * menuT
                        if (sheetAlpha > 0f) {
                            drawRect(Color.Black.copy(alpha = 0.28f * sheetAlpha), size = Size(sw, sh))
                            val sheetH = sh * 0.42f
                            val top = sh - sheetH * menuT
                            drawRoundRect(popup, topLeft = Offset(0f, top), size = Size(sw, sheetH + 30f * dp), cornerRadius = CornerRadius(22f * dp))
                            drawRoundRect(secondary.copy(alpha = 0.5f), topLeft = Offset(sw / 2f - 16f * dp, top + 8f * dp), size = Size(32f * dp, 4f * dp), cornerRadius = CornerRadius(2f * dp))
                            for (row in 0 until 2) {
                                for (col in 0 until 4) {
                                    val cx = sw * (col + 0.5f) / 4f
                                    val cy = top + 40f * dp + row * 52f * dp
                                    drawCircle(if (row == 0 && col == 0) primary else pill, 13f * dp, Offset(cx, cy))
                                    drawRoundRect(line, topLeft = Offset(cx - 12f * dp, cy + 18f * dp), size = Size(24f * dp, 5f * dp), cornerRadius = CornerRadius(2.5f * dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun graphemeList(text: String): List<String> {
    val iterator = BreakIterator.getCharacterInstance()
    iterator.setText(text)
    val parts = ArrayList<String>()
    var start = iterator.first()
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        parts.add(text.substring(start, end))
        start = end
        end = iterator.next()
    }
    return parts
}

@Composable
internal fun SearchHero(engineName: String, engineUrl: String, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val clock = rememberLoopClock(6400, "search_clock")
    val blink = rememberLoopClock(900, "search_blink")
    val pop = rememberEntrances(1)[0]
    LaunchedEffect(Unit) { if (pop.value < 1f) pop.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 170f)) }
    val query = stringResource(R.string.setup_preview_search_query)
    val glyphs = remember(query) { graphemeList(query) }
    val typed by remember(glyphs) {
        derivedStateOf {
            val t = clock.value
            val shown = (glyphs.size * window(t, 0.05f, 0.38f)).toInt()
            val erase = (glyphs.size * window(t, 0.88f, 0.98f)).toInt()
            glyphs.take((shown - erase).coerceAtLeast(0)).joinToString("")
        }
    }
    val primary = colors.primary
    val initial = engineName.trim().take(1).uppercase()
    val favicon = rememberClintFavicon(engineUrl)
    FittedBox(300.dp, 250.dp, modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val v = pop.value
                    scaleX = 0.9f + 0.1f * v
                    scaleY = 0.9f + 0.1f * v
                    alpha = v.coerceIn(0f, 1f)
                },
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(colors.cardBackground)
                    .border(2.dp, primary, CircleShape)
                    .padding(start = 16.dp, end = 8.dp)
            ) {
                Icon(Icons.Outlined.Search, null, tint = primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = typed,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    Box(
                        Modifier
                            .padding(start = 2.dp)
                            .width(2.dp)
                            .height(20.dp)
                            .graphicsLayer { alpha = if (blink.value < 0.5f) 1f else 0f }
                            .background(primary)
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (favicon != null) colors.surfaceVariant else primary)
                ) {
                    if (favicon != null) {
                        Image(
                            bitmap = favicon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    } else {
                        Text(initial, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onPrimary)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.cardBackground)
                    .padding(vertical = 8.dp)
            ) {
                listOf(0.72f, 0.55f, 0.84f).forEachIndexed { i, fraction ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(horizontal = 16.dp)
                            .graphicsLayer {
                                val t = clock.value
                                val v = ease(window(t, 0.22f + i * 0.06f, 0.34f + i * 0.06f)) * (1f - ease(window(t, 0.86f, 0.94f)))
                                alpha = v
                                translationX = (1f - v) * 24.dp.toPx()
                            }
                    ) {
                        Icon(Icons.Outlined.Search, null, tint = colors.secondaryText, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Box(
                            Modifier
                                .fillMaxWidth(fraction)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(if (i == 0) primary.copy(alpha = 0.5f) else colors.onSurface.copy(alpha = 0.14f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun PrivacyHero(modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val parts = rememberEntrances(2)
    val clock = rememberLoopClock(3000, "privacy_clock")
    val breathe = rememberLoopClock(4200, "privacy_breathe")
    LaunchedEffect(Unit) {
        if (parts.all { it.value >= 1f }) return@LaunchedEffect
        launch { parts[0].animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 190f)) }
        delay(320)
        parts[1].animateTo(1f, tween(520, easing = FastOutSlowInEasing))
    }
    val trackers = remember {
        List(8) { i ->
            val random = Random(i * 31 + 7)
            Triple(i * TAU / 8f + (random.nextFloat() - 0.5f) * 0.5f, random.nextFloat(), 0.8f + random.nextFloat() * 0.5f)
        }
    }
    val primary = colors.primary
    val onPrimary = colors.onPrimary
    val danger = colors.colorError
    Canvas(modifier) {
        val s = size.minDimension * 0.2f
        val c = center
        val shield = parts[0].value
        val check = parts[1].value
        val dp = 1.dp.toPx()
        drawCircle(primary.copy(alpha = 0.08f), s * 2.1f * shield, c)
        var hit = 0f
        trackers.forEach { (angle, offset, speed) ->
            val p = (clock.value * speed + offset) % 1f
            val dir = Offset(cos(angle), sin(angle))
            val far = s * 2.4f
            val near = s * 1.42f
            val approach = window(p, 0f, 0.55f)
            val bounce = window(p, 0.55f, 1f)
            val r = if (p < 0.55f) lerp(far, near, approach * approach) else lerp(near, near + s * 0.9f, LinearOutSlowInEasing.transform(bounce))
            val alpha = if (p < 0.55f) ease(window(p, 0f, 0.15f)) else 1f - bounce
            if (p in 0.55f..0.7f) hit = maxOf(hit, 1f - window(p, 0.55f, 0.7f))
            val pos = c + dir * r
            drawCircle(danger.copy(alpha = alpha * check), 4.2f * dp, pos)
            if (p > 0.55f) {
                drawCircle(danger.copy(alpha = (1f - bounce) * 0.4f * check), 4.2f * dp + bounce * 9f * dp, c + dir * near, style = Stroke(1.2f * dp))
            }
        }
        val ringScale = 1.42f + 0.04f * sin(breathe.value * TAU)
        drawCircle(primary.copy(alpha = (0.25f + 0.45f * hit) * check), s * ringScale, c, style = Stroke((1.5f + 1.5f * hit) * dp))
        scale(shield, c) {
            val path = Path().apply {
                moveTo(c.x, c.y - 1.15f * s)
                cubicTo(c.x + 0.35f * s, c.y - 0.92f * s, c.x + 0.7f * s, c.y - 0.85f * s, c.x + 1.0f * s, c.y - 0.82f * s)
                lineTo(c.x + 1.0f * s, c.y)
                cubicTo(c.x + 1.0f * s, c.y + 0.65f * s, c.x + 0.55f * s, c.y + 1.0f * s, c.x, c.y + 1.22f * s)
                cubicTo(c.x - 0.55f * s, c.y + 1.0f * s, c.x - 1.0f * s, c.y + 0.65f * s, c.x - 1.0f * s, c.y)
                lineTo(c.x - 1.0f * s, c.y - 0.82f * s)
                cubicTo(c.x - 0.7f * s, c.y - 0.85f * s, c.x - 0.35f * s, c.y - 0.92f * s, c.x, c.y - 1.15f * s)
                close()
            }
            drawPath(path, primary)
            if (check > 0f) {
                val tick = Path().apply {
                    moveTo(c.x - 0.42f * s, c.y + 0.02f * s)
                    lineTo(c.x - 0.1f * s, c.y + 0.34f * s)
                    lineTo(c.x + 0.46f * s, c.y - 0.3f * s)
                }
                val measure = PathMeasure()
                measure.setPath(tick, false)
                val segment = Path()
                measure.getSegment(0f, measure.length * check, segment, true)
                drawPath(segment, onPrimary, style = Stroke(0.16f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

private val PartSpeeds = listOf(1.25f, 1.05f, 1.4f, 1.15f)

@Composable
internal fun NotificationHero(modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val progress = rememberLoopClock(7000, "notif_progress")
    val drop = rememberEntrances(1)[0]
    LaunchedEffect(Unit) {
        if (drop.value < 1f) {
            delay(150)
            drop.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = 140f))
        }
    }
    val percent by remember { derivedStateOf { (PartSpeeds.sumOf { (progress.value * it).coerceIn(0f, 1f).toDouble() } / PartSpeeds.size * 100).toInt().coerceIn(0, 100) } }
    val primary = colors.primary
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val cardWidth = (maxWidth * 0.86f).coerceIn(220.dp, 360.dp)
        Column(
            modifier = Modifier
                .widthIn(max = cardWidth)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .graphicsLayer {
                    val v = drop.value
                    translationY = (1f - v) * -size.height
                    alpha = v.coerceIn(0f, 1f)
                }
                .clip(RoundedCornerShape(24.dp))
                .background(colors.cardBackground)
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ClintMarkSmall(Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.secondaryText
                )
                Box(
                    Modifier
                        .padding(horizontal = 6.dp)
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(colors.secondaryText)
                )
                Text(
                    text = stringResource(R.string.downloads_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(primary.copy(alpha = 0.18f))
                ) {
                    Icon(Icons.Outlined.Movie, null, tint = primary, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.setup_preview_download_name),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.setup_preview_download_progress, percent),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(primary)
                ) {
                    Icon(Icons.Outlined.Pause, null, tint = colors.onPrimary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(colors.divider)
                    .drawBehind {
                        val gap = 2.dp.toPx()
                        val segment = (size.width - gap * (PartSpeeds.size - 1)) / PartSpeeds.size
                        PartSpeeds.forEachIndexed { i, speed ->
                            val fill = (progress.value * speed).coerceIn(0f, 1f)
                            drawRoundRect(
                                primary,
                                topLeft = Offset(i * (segment + gap), 0f),
                                size = Size(segment * fill, size.height),
                                cornerRadius = CornerRadius(size.height / 2f)
                            )
                        }
                    }
            )
        }
    }
}

@Composable
internal fun DefaultBrowserHero(modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val clock = rememberLoopClock(4800, "default_links")
    val pulse = rememberLoopClock(2400, "default_pulse")
    val pop = rememberEntrances(1)[0]
    LaunchedEffect(Unit) { if (pop.value < 1f) pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 170f)) }
    val apps = listOf(
        Icons.Outlined.Email,
        Icons.AutoMirrored.Outlined.Chat,
        Icons.Outlined.QrCode2,
        Icons.AutoMirrored.Outlined.Article,
        Icons.Outlined.Forum,
        Icons.Outlined.Sms
    )
    val primary = colors.primary
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val tile = (minOf(maxWidth, maxHeight) * 0.3f).coerceIn(64.dp, 130.dp)
        val orbit = tile * 1.35f
        val bubble = tile * 0.5f
        Canvas(Modifier.fillMaxSize()) {
            val r = orbit.toPx()
            apps.indices.forEach { i ->
                val angle = i * TAU / apps.size - PI.toFloat() / 2f
                val end = center + Offset(cos(angle), sin(angle)) * r * pop.value
                drawLine(
                    primary.copy(alpha = 0.25f * pop.value),
                    center,
                    end,
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 6.dp.toPx()), -clock.value * 22.dp.toPx())
                )
            }
            val p = pulse.value
            drawCircle(primary.copy(alpha = (1f - p) * 0.3f * pop.value), tile.toPx() / 2f * (1f + 0.6f * p), center, style = Stroke(2.dp.toPx()))
        }
        apps.forEachIndexed { i, vector ->
            val angle = i * TAU / apps.size - PI.toFloat() / 2f
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(bubble)
                    .graphicsLayer {
                        val d = orbit.toPx() * pop.value
                        translationX = cos(angle) * d
                        translationY = sin(angle) * d
                        alpha = pop.value.coerceIn(0f, 1f)
                    }
                    .clip(CircleShape)
                    .background(colors.cardBackground)
            ) {
                Icon(vector, null, tint = colors.secondaryText, modifier = Modifier.size(bubble * 0.5f))
            }
        }
        apps.indices.forEach { i ->
            val angle = i * TAU / apps.size - PI.toFloat() / 2f
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(bubble * 0.62f)
                    .graphicsLayer {
                        val p = (clock.value + i.toFloat() / apps.size) % 1f
                        val travel = FastOutSlowInEasing.transform(p)
                        val d = orbit.toPx() * (1f - travel)
                        translationX = cos(angle) * d
                        translationY = sin(angle) * d
                        alpha = sin(p * PI.toFloat()).coerceAtLeast(0f) * pop.value.coerceIn(0f, 1f)
                        val s = 1f - 0.4f * travel
                        scaleX = s
                        scaleY = s
                    }
                    .clip(CircleShape)
                    .background(primary)
            ) {
                Icon(Icons.Outlined.Link, null, tint = colors.onPrimary, modifier = Modifier.size(bubble * 0.36f))
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(tile)
                .graphicsLayer {
                    val beat = 1f + 0.04f * sin(pulse.value * TAU)
                    scaleX = pop.value * beat
                    scaleY = pop.value * beat
                }
                .clip(CircleShape)
                .background(primary)
        ) {
            ClintMarkSmall(Modifier.fillMaxSize(0.72f), color = colors.onPrimary)
        }
    }
}

@Composable
internal fun SecurityHero(modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val parts = rememberEntrances(2)
    val flow = rememberLoopClock(4200, "security_flow")
    val glow = rememberLoopClock(3600, "security_glow")
    LaunchedEffect(Unit) {
        if (parts.all { it.value >= 1f }) return@LaunchedEffect
        launch { parts[0].animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 190f)) }
        delay(380)
        parts[1].animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 260f))
    }
    val primary = colors.primary
    val onPrimary = colors.onPrimary
    val plain = colors.secondaryText
    val lane = colors.divider
    Canvas(modifier) {
        val s = size.minDimension * 0.2f
        val c = Offset(center.x, center.y + s * 0.2f)
        val dp = 1.dp.toPx()
        val lock = parts[0].value
        val shackle = parts[1].value
        val bodyW = s * 1.5f
        val bodyH = s * 1.15f
        val bodyTop = c.y - bodyH * 0.35f
        val half = bodyW / 2f
        val span = size.width * 0.5f
        drawCircle(primary.copy(alpha = 0.08f), s * 2.2f * lock, c)
        val lanes = listOf(-0.32f, 0f, 0.32f)
        lanes.forEachIndexed { li, ly ->
            val y = bodyTop + bodyH * 0.5f + ly * bodyH
            drawLine(
                lane.copy(alpha = lane.alpha * lock),
                Offset(c.x - span, y),
                Offset(c.x + span, y),
                strokeWidth = 1.2f * dp,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f * dp, 6f * dp), -flow.value * 20f * dp)
            )
            for (k in 0 until 4) {
                val p = (flow.value + k / 4f + li * 0.13f) % 1f
                val x = c.x - span + p * span * 2f
                val edgeFade = (1f - abs(p - 0.5f) * 2f).coerceIn(0f, 1f).pow(0.4f) * lock
                if (x < c.x - half) {
                    drawRoundRect(
                        plain.copy(alpha = 0.7f * edgeFade),
                        topLeft = Offset(x - 9f * dp, y - 3.5f * dp),
                        size = Size(18f * dp, 7f * dp),
                        cornerRadius = CornerRadius(3.5f * dp)
                    )
                } else if (x > c.x + half) {
                    drawCircle(primary.copy(alpha = edgeFade), 4.5f * dp, Offset(x, y))
                    drawCircle(primary.copy(alpha = 0.25f * edgeFade), 8f * dp, Offset(x, y))
                }
            }
        }
        scale(lock, c) {
            val shackleR = bodyW * 0.3f
            val lift = (1f - shackle) * s * 0.45f
            val shackleTop = bodyTop - shackleR * 1.1f - lift
            val arc = Path().apply {
                moveTo(c.x - shackleR, bodyTop + 2f * dp)
                lineTo(c.x - shackleR, shackleTop + shackleR)
                arcTo(
                    androidx.compose.ui.geometry.Rect(c.x - shackleR, shackleTop, c.x + shackleR, shackleTop + shackleR * 2f),
                    180f,
                    180f,
                    false
                )
                lineTo(c.x + shackleR, bodyTop + 2f * dp - lift * 0.4f)
            }
            drawPath(arc, primary, style = Stroke(s * 0.2f, cap = StrokeCap.Round))
            val pulse = 0.5f + 0.5f * sin(glow.value * TAU)
            drawRoundRect(
                primary.copy(alpha = 0.25f * pulse),
                topLeft = Offset(c.x - half - 6f * dp, bodyTop - 6f * dp),
                size = Size(bodyW + 12f * dp, bodyH + 12f * dp),
                cornerRadius = CornerRadius(s * 0.36f)
            )
            drawRoundRect(primary, topLeft = Offset(c.x - half, bodyTop), size = Size(bodyW, bodyH), cornerRadius = CornerRadius(s * 0.3f))
            val keyY = bodyTop + bodyH * 0.45f
            drawCircle(onPrimary, s * 0.15f, Offset(c.x, keyY))
            drawRoundRect(onPrimary, topLeft = Offset(c.x - s * 0.06f, keyY), size = Size(s * 0.12f, s * 0.3f), cornerRadius = CornerRadius(s * 0.06f))
        }
    }
}

private class Confetti(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val spin: Float,
    val tone: Int,
    val round: Boolean
)

@Composable
internal fun CelebrationHero(modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val parts = rememberEntrances(3)
    val badge = parts[0]
    val check = parts[1]
    val burst = parts[2]
    val float = rememberLoopClock(5200, "celebrate_float")
    LaunchedEffect(Unit) {
        if (parts.all { it.value >= 1f }) return@LaunchedEffect
        launch { badge.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 200f)) }
        launch { burst.animateTo(1f, tween(2400, easing = LinearOutSlowInEasing)) }
        delay(240)
        check.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
    }
    val pieces = remember {
        List(64) { i ->
            val random = Random(i * 17 + 3)
            Confetti(
                angle = random.nextFloat() * TAU,
                speed = 0.35f + random.nextFloat() * 0.7f,
                size = 3f + random.nextFloat() * 4f,
                spin = (random.nextFloat() - 0.5f) * 4f,
                tone = random.nextInt(3),
                round = random.nextBoolean()
            )
        }
    }
    val primary = colors.primary
    val onPrimary = colors.onPrimary
    val tones = listOf(primary, colors.secondaryText, colors.onSurface.copy(alpha = 0.7f))
    Canvas(modifier) {
        val s = size.minDimension
        val bob = sin(float.value * TAU) * s * 0.012f
        val c = Offset(center.x, center.y + bob)
        val r = s * 0.2f
        val b = burst.value
        val dp = 1.dp.toPx()
        drawCircle(primary.copy(alpha = 0.10f), r * 1.7f * badge.value.coerceIn(0f, 1.2f), c)
        pieces.forEach { piece ->
            val distance = piece.speed * b * s * 0.6f
            val x = c.x + cos(piece.angle) * distance
            val y = c.y + sin(piece.angle) * distance + b * b * s * 0.25f
            val alpha = if (b > 0.01f && b < 1f) (1f - b.pow(1.6f)).coerceIn(0f, 1f) else 0f
            if (alpha > 0f) {
                val tint = tones[piece.tone].copy(alpha = alpha)
                rotate(piece.spin * b * 540f, Offset(x, y)) {
                    if (piece.round) {
                        drawCircle(tint, piece.size * dp * 0.6f, Offset(x, y))
                    } else {
                        drawRect(
                            tint,
                            topLeft = Offset(x - piece.size * dp / 2f, y - piece.size * dp * 0.3f),
                            size = Size(piece.size * dp, piece.size * dp * 0.6f)
                        )
                    }
                }
            }
        }
        val badgeRadius = r * badge.value
        if (badgeRadius > 0f) drawCircle(primary, badgeRadius, c)
        if (check.value > 0f) {
            scale(badge.value.coerceIn(0f, 1.2f), c) {
                val path = Path().apply {
                    moveTo(c.x - r * 0.42f, c.y + r * 0.02f)
                    lineTo(c.x - r * 0.12f, c.y + r * 0.32f)
                    lineTo(c.x + r * 0.46f, c.y - r * 0.3f)
                }
                val measure = PathMeasure()
                measure.setPath(path, false)
                val segment = Path()
                measure.getSegment(0f, measure.length * check.value, segment, true)
                drawPath(segment, onPrimary, style = Stroke(r * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
