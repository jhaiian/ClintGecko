package com.jhaiian.clint.settings.menucustomization

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.menu.icon
import com.jhaiian.clint.browser.menu.titleRes
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun MenuCustomizationScreen(
    state: MenuCustomizationUiState,
    maxContentWidth: Dp?,
    onExit: () -> Unit,
    onToggleVisible: (String) -> Unit,
    onCommitReorder: (List<String>) -> Unit,
    onResetClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val listState = rememberLazyListState()
    val dragState = remember { MenuCustomizeDragState(listState) }
    val entries = state.entries

    fun onDragMoved() {
        val draggedId = dragState.draggingId ?: return
        val hoverKey = dragState.hoverKey ?: return
        val targetIndex = entries.indexOfFirst { menuItemDragKey(it.item.id) == hoverKey }
        val fromIndex = entries.indexOfFirst { it.item.id == draggedId }
        if (targetIndex == -1 || fromIndex == -1 || targetIndex == fromIndex) return
        entries.add(targetIndex, entries.removeAt(fromIndex))
    }

    fun commitDrag() {
        val draggedId = dragState.draggingId
        dragState.end()
        if (draggedId == null) return
        onCommitReorder(entries.map { it.item.id })
    }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            MenuCustomizationToolbar(onBack = onExit, onResetClick = onResetClick)
            HorizontalDivider(color = colors.divider, thickness = 1.dp)
            Text(
                stringResource(R.string.menu_customization_description),
                color = colors.secondaryText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AdaptiveWidthContainer(maxContentWidth) {
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            items(entries, key = { menuItemDragKey(it.item.id) }) { entry ->
                                val isDragging = dragState.draggingId == entry.item.id
                                val ghostAlpha by animateFloatAsState(if (isDragging) 0.2f else 1f, label = "menuItemGhostAlpha")
                                Box(
                                    Modifier.animateItem(
                                        fadeInSpec = tween(220),
                                        placementSpec = spring(dampingRatio = 0.82f, stiffness = 400f),
                                        fadeOutSpec = tween(150)
                                    )
                                ) {
                                    MenuCustomizeRow(
                                        entry = entry,
                                        modifier = Modifier.alpha(ghostAlpha),
                                        onToggleVisible = { onToggleVisible(entry.item.id) },
                                        dragHandleModifier = Modifier.pointerInput(entry.item.id) {
                                            detectDragGestures(
                                                onDragStart = { dragState.start(entry.item.id) },
                                                onDrag = { change, amount -> change.consume(); dragState.drag(amount.y); onDragMoved() },
                                                onDragEnd = { commitDrag() },
                                                onDragCancel = { dragState.end() }
                                            )
                                        }
                                    )
                                }
                            }
                            item(key = "pinned_settings_note") {
                                PinnedSettingsNote()
                            }
                        }

                        val draggedId = dragState.draggingId
                        if (draggedId != null) {
                            val draggedEntry = entries.find { it.item.id == draggedId }
                            if (draggedEntry != null) {
                                val offsetYPx = dragState.originOffsetY + dragState.dragOffsetY
                                val density = LocalDensity.current
                                val rowHeight = with(density) { dragState.originHeight.toDp() }
                                val liftScale by animateFloatAsState(1.03f, animationSpec = spring(dampingRatio = 0.5f), label = "menuItemDragLiftScale")
                                Box(
                                    Modifier
                                        .padding(horizontal = 12.dp)
                                        .fillMaxWidth()
                                        .height(rowHeight)
                                        .graphicsLayer {
                                            translationY = offsetYPx
                                            scaleX = liftScale
                                            scaleY = liftScale
                                        }
                                        .zIndex(10f)
                                        .shadow(10.dp, RoundedCornerShape(16.dp))
                                ) {
                                    MenuCustomizeRow(
                                        entry = draggedEntry,
                                        modifier = Modifier,
                                        onToggleVisible = {},
                                        dragHandleModifier = Modifier
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuCustomizationToolbar(onBack: () -> Unit, onResetClick: () -> Unit) {
    val colors = LocalClintColors.current
    Surface(color = colors.surface, shadowElevation = 4.dp, modifier = Modifier.statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.onSurface)
            }
            Text(
                stringResource(R.string.menu_customization_title),
                color = colors.onSurface,
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            IconButton(onClick = onResetClick) {
                Icon(Icons.Filled.RestartAlt, contentDescription = stringResource(R.string.menu_customization_reset_desc), tint = colors.iconTint)
            }
        }
    }
}

@Composable
private fun MenuCustomizeRow(
    entry: MenuCustomizationEntry,
    modifier: Modifier,
    onToggleVisible: () -> Unit,
    dragHandleModifier: Modifier
) {
    val colors = LocalClintColors.current
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.weight(1f).clickable(onClick = onToggleVisible),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.primary.copy(alpha = if (entry.visible) 0.12f else 0.06f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = entry.item.icon(),
                        contentDescription = null,
                        tint = if (entry.visible) colors.primary else colors.secondaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = stringResource(entry.item.titleRes()),
                color = if (entry.visible) colors.onSurface else colors.secondaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 14.dp)
            )
            ClintSwitch(checked = entry.visible)
        }
        Icon(
            imageVector = Icons.Filled.DragHandle,
            contentDescription = stringResource(R.string.menu_customization_drag_handle_desc),
            tint = colors.secondaryText,
            modifier = Modifier.padding(start = 10.dp).size(24.dp).then(dragHandleModifier)
        )
    }
}

@Composable
private fun PinnedSettingsNote() {
    val colors = LocalClintColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceVariant)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(18.dp))
        Column(Modifier.padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(14.dp))
                Text(
                    stringResource(R.string.settings),
                    color = colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            Text(
                stringResource(R.string.menu_customization_pinned_note),
                color = colors.secondaryText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
