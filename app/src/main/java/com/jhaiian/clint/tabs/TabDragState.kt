package com.jhaiian.clint.tabs

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

internal fun tabItemKey(tabId: String): String = "tab:$tabId"

@Stable
internal class TabDragState(private val gridState: LazyGridState) {
    var draggingId by mutableStateOf<String?>(null)
        private set
    var dragOffset by mutableStateOf(Offset.Zero)
        private set
    var originOffset: Offset = Offset.Zero
        private set
    var originSize: IntSize = IntSize.Zero
        private set

    var hoverKey: Any? = null
        private set

    fun start(tabId: String) {
        val info = gridState.layoutInfo.visibleItemsInfo.find { it.key == tabItemKey(tabId) } ?: return
        draggingId = tabId
        dragOffset = Offset.Zero
        originOffset = Offset(info.offset.x.toFloat(), info.offset.y.toFloat())
        originSize = info.size
        hoverKey = null
    }

    fun drag(delta: Offset) {
        if (draggingId == null) return
        dragOffset += delta
        val center = Offset(
            originOffset.x + originSize.width / 2f + dragOffset.x,
            originOffset.y + originSize.height / 2f + dragOffset.y
        )
        hoverKey = gridState.layoutInfo.visibleItemsInfo.firstOrNull { candidate ->
            center.x >= candidate.offset.x && center.x <= candidate.offset.x + candidate.size.width &&
                center.y >= candidate.offset.y && center.y <= candidate.offset.y + candidate.size.height
        }?.key
    }

    fun end() {
        draggingId = null
        dragOffset = Offset.Zero
        hoverKey = null
    }
}
