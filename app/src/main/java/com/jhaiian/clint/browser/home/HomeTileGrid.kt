package com.jhaiian.clint.browser.home

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.launch

private const val REORDER_DELAY_MS = 180L

@Composable
internal fun HomeTileGrid(
    tiles: List<HomeTile>,
    columns: Int,
    desktop: Boolean,
    iconVersion: Int,
    allowMerge: Boolean,
    showAdd: Boolean,
    onOpen: (HomeTile) -> Unit,
    onDrop: (order: List<HomeTile>, draggedId: String, mergeTargetId: String?) -> Unit,
    onAdd: () -> Unit,
    menu: @Composable (tile: HomeTile, onDismiss: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var order by remember { mutableStateOf<List<HomeTile>?>(null) }
    var dragId by remember { mutableStateOf<String?>(null) }
    var mergeId by remember { mutableStateOf<String?>(null) }
    var menuId by remember { mutableStateOf<String?>(null) }
    var pointer by remember { mutableStateOf(Offset.Zero) }
    var grab by remember { mutableStateOf(Offset.Zero) }
    val slots = remember { ArrayList<Rect>() }
    val shown = order ?: tiles
    val currentTiles by rememberUpdatedState(tiles)
    val currentAllowMerge by rememberUpdatedState(allowMerge)
    val currentOnDrop by rememberUpdatedState(onDrop)
    val gap = if (desktop) 12.dp else 0.dp

    fun slotAt(position: Offset, count: Int): Int? =
        (0 until minOf(count, slots.size)).firstOrNull { slots[it].contains(position) }

    val gestures = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val startTiles = currentTiles
            val startIndex = slotAt(down.position, startTiles.size) ?: return@awaitEachGesture
            val endedEarly = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                    if (change == null || !change.pressed || change.isConsumed ||
                        (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                    ) break
                }
            }
            if (endedEarly != null) return@awaitEachGesture
            val working = startTiles.toMutableList()
            val draggedId = working[startIndex].id
            var started = false
            var hoverIndex = -1
            var hoverSince = 0L
            try {
                while (true) {
                    val event = if (started && hoverIndex >= 0) {
                        val wait = (REORDER_DELAY_MS - (SystemClock.uptimeMillis() - hoverSince)).coerceAtLeast(1L)
                        withTimeoutOrNull(wait) { awaitPointerEvent() }
                    } else {
                        awaitPointerEvent()
                    }
                    if (event != null) {
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (!started) {
                            if ((change.position - down.position).getDistance() <= viewConfiguration.touchSlop) continue
                            started = true
                            menuId = null
                            order = working.toList()
                            dragId = draggedId
                            grab = down.position - (slots.getOrNull(startIndex)?.topLeft ?: Offset.Zero)
                        }
                        change.consume()
                        pointer = change.position
                    }
                    val from = working.indexOfFirst { it.id == draggedId }
                    val over = slotAt(pointer, working.size)
                    if (from < 0 || over == null || over == from) {
                        mergeId = null
                        hoverIndex = -1
                        continue
                    }
                    val slot = slots[over]
                    val center = Rect(
                        slot.left + slot.width * 0.25f,
                        slot.top + slot.height * 0.12f,
                        slot.right - slot.width * 0.25f,
                        slot.top + slot.height * 0.62f
                    )
                    if (currentAllowMerge && working[from] is HomeSiteTile && center.contains(pointer)) {
                        val target = working[over].id
                        if (mergeId != target) {
                            mergeId = target
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        hoverIndex = -1
                        continue
                    }
                    mergeId = null
                    val now = SystemClock.uptimeMillis()
                    if (hoverIndex != over) {
                        hoverIndex = over
                        hoverSince = now
                        continue
                    }
                    if (now - hoverSince >= REORDER_DELAY_MS) {
                        working.add(over, working.removeAt(from))
                        order = working.toList()
                        hoverIndex = -1
                    }
                }
                if (started) currentOnDrop(working.toList(), draggedId, mergeId)
            } finally {
                if (started) {
                    dragId = null
                    mergeId = null
                    order = null
                }
            }
        }
    }

    Layout(
        modifier = modifier.fillMaxWidth().then(gestures),
        content = {
            shown.forEachIndexed { index, tile ->
                key(tile.id) {
                    val dragging = tile.id == dragId
                    Box(
                        Modifier
                            .animatePlacement(!dragging)
                            .zIndex(if (dragging) 1f else 0f)
                            .graphicsLayer {
                                if (dragging) {
                                    val slot = slots.getOrNull(index)
                                    if (slot != null) {
                                        translationX = pointer.x - grab.x - slot.left
                                        translationY = pointer.y - grab.y - slot.top
                                    }
                                    scaleX = 1.08f
                                    scaleY = 1.08f
                                    alpha = if (mergeId != null) 0.75f else 0.94f
                                }
                            }
                    ) {
                        HomeTileCell(
                            tile = tile,
                            desktop = desktop,
                            iconVersion = iconVersion,
                            highlighted = tile.id == mergeId,
                            onClick = { onOpen(tile) },
                            onLongClick = { menuId = tile.id }
                        ) {
                            if (menuId == tile.id) menu(tile) { menuId = null }
                        }
                    }
                }
            }
            if (showAdd && dragId == null) {
                HomeAddCell(desktop = desktop, onClick = onAdd)
            }
        }
    ) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val width = constraints.maxWidth
        val cell = ((width - gapPx * (columns - 1)) / columns).coerceAtLeast(0)
        val placeables = measurables.map { it.measure(Constraints.fixedWidth(cell)) }
        val rows = placeables.chunked(columns)
        val rowHeights = rows.map { row -> row.maxOf { it.height } }
        val height = rowHeights.sum() + gapPx * (rows.size - 1).coerceAtLeast(0)
        val rtl = layoutDirection == LayoutDirection.Rtl
        layout(width, height) {
            slots.clear()
            var y = 0
            rows.forEachIndexed { rowIndex, row ->
                row.forEachIndexed { column, placeable ->
                    val start = column * (cell + gapPx)
                    val x = if (rtl) width - start - cell else start
                    slots.add(Rect(x.toFloat(), y.toFloat(), (x + cell).toFloat(), (y + rowHeights[rowIndex]).toFloat()))
                    placeable.place(x, y)
                }
                y += rowHeights[rowIndex] + gapPx
            }
        }
    }
}

private fun Modifier.animatePlacement(enabled: Boolean): Modifier = composed {
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf(IntOffset.Zero) }
    var animatable by remember { mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null) }
    onPlaced { target = it.positionInParent().round() }
        .offset {
            val anim = animatable ?: Animatable(target, IntOffset.VectorConverter).also { animatable = it }
            if (!enabled) {
                if (anim.targetValue != target || anim.value != target) scope.launch { anim.snapTo(target) }
                IntOffset.Zero
            } else {
                if (anim.targetValue != target) {
                    scope.launch { anim.animateTo(target, spring(stiffness = Spring.StiffnessMediumLow)) }
                }
                anim.value - target
            }
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeTileCell(
    tile: HomeTile,
    desktop: Boolean,
    iconVersion: Int,
    highlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    menuSlot: @Composable () -> Unit
) {
    val colors = LocalClintColors.current
    val cardAlpha = LocalHomeCardAlpha.current
    val label = when (tile) {
        is HomeSiteTile -> tile.label()
        is HomeFolderTile -> tile.name.ifBlank { stringResource(R.string.home_folder_default_name) }
    }
    val scale by animateFloatAsState(if (highlighted) 1.12f else 1f, label = "tileHighlight")
    val iconModifier = Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
    if (desktop) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardBackground.copy(alpha = cardAlpha))
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 8.dp, vertical = 16.dp)
        ) {
            Box {
                HomeTileVisual(tile, 52.dp, 16.dp, colors.surfaceVariant, iconVersion, highlighted, iconModifier)
                menuSlot()
            }
            Text(
                text = label,
                color = colors.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(vertical = 8.dp)
        ) {
            Box {
                HomeTileVisual(tile, 56.dp, 18.dp, colors.cardBackground.copy(alpha = cardAlpha), iconVersion, highlighted, iconModifier)
                menuSlot()
            }
            Text(
                text = label,
                color = colors.onSurface,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

@Composable
internal fun HomeTileVisual(
    tile: HomeTile,
    size: Dp,
    corner: Dp,
    background: Color,
    iconVersion: Int,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val shape = RoundedCornerShape(corner)
    val outline = when {
        highlighted -> Modifier.border(2.dp, colors.primary, shape)
        tile is HomeFolderTile -> Modifier.border(1.dp, colors.primary.copy(alpha = 0.25f), shape)
        else -> Modifier
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(background)
            .then(outline),
        contentAlignment = Alignment.Center
    ) {
        when (tile) {
            is HomeSiteTile -> HomeSiteIcon(tile, size, iconVersion)
            is HomeFolderTile -> {
                val inner = (size - 12.dp) / 2
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    tile.sites.take(4).chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            row.forEach { site ->
                                Box(
                                    modifier = Modifier
                                        .size(inner)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    HomeSiteIcon(site, inner, iconVersion)
                                }
                            }
                            if (row.size < 2) Spacer(Modifier.size(inner))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSiteIcon(site: HomeSiteTile, size: Dp, iconVersion: Int) {
    val colors = LocalClintColors.current
    val icon = rememberHomeTileIcon(site, iconVersion)
    when {
        icon == null -> Text(
            text = site.label().take(1).uppercase(),
            color = colors.primary,
            fontSize = (size.value * 0.36f).sp,
            fontWeight = FontWeight.SemiBold
        )
        icon.fill -> Image(
            bitmap = icon.bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.High,
            modifier = Modifier.fillMaxSize()
        )
        else -> Image(
            bitmap = icon.bitmap,
            contentDescription = null,
            filterQuality = FilterQuality.High,
            modifier = Modifier.size(size * 0.5f).clip(RoundedCornerShape(size * 0.1f))
        )
    }
}

@Composable
private fun HomeAddCell(desktop: Boolean, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    val cardAlpha = LocalHomeCardAlpha.current
    val iconSize = if (desktop) 52.dp else 56.dp
    val shape = RoundedCornerShape(if (desktop) 16.dp else 18.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (desktop) 20.dp else 16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = if (desktop) 16.dp else 8.dp, horizontal = if (desktop) 8.dp else 0.dp)
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(shape)
                .background(colors.cardBackground.copy(alpha = cardAlpha * 0.6f))
                .border(1.dp, colors.primary.copy(alpha = 0.35f), shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = stringResource(R.string.action_add),
            color = colors.secondaryText,
            fontSize = if (desktop) 13.sp else 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = if (desktop) 10.dp else 6.dp)
        )
    }
}
