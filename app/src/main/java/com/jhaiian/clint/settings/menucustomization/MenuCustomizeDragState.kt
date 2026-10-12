package com.jhaiian.clint.settings.menucustomization

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal fun menuItemDragKey(id: String): String = "menu_item:$id"

@Stable
internal class MenuCustomizeDragState(private val listState: LazyListState) {
    var draggingId by mutableStateOf<String?>(null)
        private set
    var dragOffsetY by mutableStateOf(0f)
        private set
    var originOffsetY: Int = 0
        private set
    var originHeight: Int = 0
        private set

    var hoverKey: Any? = null
        private set

    fun start(id: String) {
        val info = listState.layoutInfo.visibleItemsInfo.find { it.key == menuItemDragKey(id) } ?: return
        draggingId = id
        dragOffsetY = 0f
        originOffsetY = info.offset
        originHeight = info.size
        hoverKey = null
    }

    fun drag(deltaY: Float) {
        if (draggingId == null) return
        dragOffsetY += deltaY
        val center = originOffsetY + originHeight / 2f + dragOffsetY
        hoverKey = listState.layoutInfo.visibleItemsInfo.firstOrNull { candidate ->
            center >= candidate.offset && center <= candidate.offset + candidate.size
        }?.key
    }

    fun end() {
        draggingId = null
        dragOffsetY = 0f
        hoverKey = null
    }
}
