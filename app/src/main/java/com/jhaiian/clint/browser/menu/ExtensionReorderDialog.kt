package com.jhaiian.clint.browser.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.extensions.ClintExtensions
import com.jhaiian.clint.extensions.ExtensionIconTile
import com.jhaiian.clint.extensions.ExtensionInfo
import com.jhaiian.clint.extensions.ExtensionMenuOrder
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintDialogActionFooter
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
internal fun ExtensionReorderDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalClintColors.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val hideStatusBar = remember { prefs.getBoolean("hide_status_bar", false) }
    val hideSystemNavigation = remember { prefs.getBoolean("hide_system_navigation", false) }
    val items = remember {
        mutableStateListOf<ExtensionInfo>().apply { addAll(ExtensionMenuOrder.customSorted(ClintExtensions.extensions.toList())) }
    }
    val rowHeight = 56.dp
    val rowPx = with(LocalDensity.current) { rowHeight.toPx() }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    ClintDialog(
        title = stringResource(R.string.ext_menu_reorder_title),
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            ClintDialogActionFooter(
                onCancel = onDismiss,
                positiveLabel = stringResource(R.string.action_save),
                onPositive = {
                    ExtensionMenuOrder.setOrder(context, items.map { it.id })
                    ExtensionMenuOrder.setSort(context, ExtensionMenuOrder.SORT_CUSTOM)
                    onDismiss()
                }
            )
        }
    ) {
        Text(
            stringResource(R.string.ext_menu_reorder_desc),
            color = colors.secondaryText,
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )
        items.forEach { info ->
            key(info.id) {
                val dragging = draggingId == info.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer { translationY = if (dragging) dragOffset else 0f }
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (dragging) colors.surfaceVariant else Color.Transparent)
                        .height(rowHeight)
                        .padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExtensionIconTile(info, 32.dp)
                    Text(
                        info.name,
                        color = colors.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    )
                    Icon(
                        Icons.Filled.DragHandle,
                        contentDescription = stringResource(R.string.ext_menu_reorder_drag),
                        tint = if (dragging) colors.primary else colors.iconTint,
                        modifier = Modifier
                            .size(48.dp)
                            .pointerInput(info.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingId = info.id
                                        dragOffset = 0f
                                    },
                                    onDragEnd = {
                                        draggingId = null
                                        dragOffset = 0f
                                    },
                                    onDragCancel = {
                                        draggingId = null
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount.y
                                        val from = items.indexOfFirst { it.id == info.id }
                                        if (dragOffset > rowPx / 2 && from in 0 until items.lastIndex) {
                                            items.add(from + 1, items.removeAt(from))
                                            dragOffset -= rowPx
                                        } else if (dragOffset < -rowPx / 2 && from > 0) {
                                            items.add(from - 1, items.removeAt(from))
                                            dragOffset += rowPx
                                        }
                                    }
                                )
                            }
                            .padding(12.dp)
                    )
                }
            }
        }
    }
}
