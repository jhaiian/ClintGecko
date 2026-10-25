package com.jhaiian.clint.tabs

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.listscreen.ListMenuItem
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors

object TabStripPrefs {
    const val PREF_SHOW_TAB_STRIP = "show_tab_strip"

    fun isEnabled(context: Context, prefs: SharedPreferences): Boolean =
        prefs.getBoolean(PREF_SHOW_TAB_STRIP, context.resources.configuration.smallestScreenWidthDp >= 600)
}

data class TabStripState(
    val tabs: List<TabPreview>,
    val groups: List<TabGroup>,
    val activeTabId: String?,
    val isIncognito: Boolean,
    val profileColors: Map<String, Int> = emptyMap(),
    val groupsEnabled: Boolean = true
)

private sealed class TabStripItem {
    abstract val key: String

    data class GroupChip(val group: TabGroup, val count: Int) : TabStripItem() {
        override val key: String get() = "strip-group:${group.id}"
    }

    data class Tab(val preview: TabPreview, val group: TabGroup?) : TabStripItem() {
        override val key: String get() = "strip-tab:${preview.id}"
    }
}

private fun buildTabStripItems(state: TabStripState): List<TabStripItem> {
    val groupsById = state.groups.associateBy { it.id }
    val emitted = mutableSetOf<String>()
    val items = mutableListOf<TabStripItem>()
    state.tabs.forEach { tab ->
        val group = tab.groupId?.let { groupsById[it] }
        if (group != null && emitted.add(group.id)) {
            items.add(TabStripItem.GroupChip(group, state.tabs.count { it.groupId == group.id }))
        }
        if (group == null || !group.collapsed || tab.id == state.activeTabId) {
            items.add(TabStripItem.Tab(tab, group))
        }
    }
    return items
}

@Composable
internal fun TabStrip(
    state: TabStripState,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onCloseOtherTabs: (String) -> Unit,
    onNewTab: () -> Unit,
    onNewTabInGroup: (String) -> Unit,
    onAddToNewGroup: (String) -> Unit,
    onRemoveFromGroup: (String) -> Unit,
    onToggleGroup: (String) -> Unit,
    onOpenGroup: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val items = buildTabStripItems(state)
    val listState = rememberLazyListState()
    val activeIndex = items.indexOfFirst { it is TabStripItem.Tab && it.preview.id == state.activeTabId }
    LaunchedEffect(state.activeTabId, items.size) {
        if (activeIndex < 0) return@LaunchedEffect
        val info = listState.layoutInfo
        val visible = info.visibleItemsInfo.firstOrNull { it.index == activeIndex }
        val fullyVisible = visible != null &&
            visible.offset >= info.viewportStartOffset &&
            visible.offset + visible.size <= info.viewportEndOffset
        if (!fullyVisible) listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
    }
    BoxWithConstraints(modifier.fillMaxWidth().height(40.dp)) {
        val tabCount = items.count { it is TabStripItem.Tab }.coerceAtLeast(1)
        val tabWidth = ((maxWidth - 44.dp) / tabCount).coerceIn(104.dp, 196.dp)
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            LazyRow(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentPadding = PaddingValues(start = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
                    when (item) {
                        is TabStripItem.GroupChip -> TabStripGroupChip(
                            group = item.group,
                            count = item.count,
                            onToggle = { onToggleGroup(item.group.id) },
                            onOpen = { onOpenGroup(item.group.id) }
                        )
                        is TabStripItem.Tab -> {
                            val active = item.preview.id == state.activeTabId
                            val previous = items.getOrNull(index - 1)
                            val showDivider = !active && previous is TabStripItem.Tab && previous.preview.id != state.activeTabId
                            TabStripTab(
                                preview = item.preview,
                                group = item.group,
                                active = active,
                                width = tabWidth,
                                showDivider = showDivider,
                                onSelect = { onSelectTab(item.preview.id) },
                                onClose = { onCloseTab(item.preview.id) },
                                onCloseOthers = { onCloseOtherTabs(item.preview.id) },
                                onNewTabInGroup = { item.group?.let { onNewTabInGroup(it.id) } },
                                onAddToNewGroup = { onAddToNewGroup(item.preview.id) },
                                onRemoveFromGroup = { onRemoveFromGroup(item.preview.id) },
                                canGroup = state.groupsEnabled,
                                canCloseOthers = state.tabs.size > 1,
                                profileColor = state.profileColors[item.preview.profileId]
                            )
                        }
                    }
                }
            }
            IconButton(onClick = onNewTab, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(if (state.isIncognito) R.string.new_incognito_tab else R.string.new_tab),
                    tint = colors.iconTint
                )
            }
        }
    }
}

@Composable
private fun TabStripGroupChip(group: TabGroup, count: Int, onToggle: () -> Unit, onOpen: () -> Unit) {
    val base = Color(group.color)
    val content = if (base.luminance() > 0.55f) Color(0xFF1B1B1F) else Color.White
    val name = tabGroupDisplayName(group)
    val toggleLabel = stringResource(if (group.collapsed) R.string.tab_group_expand else R.string.tab_group_collapse)
    Row(
        Modifier
            .widthIn(max = 150.dp)
            .padding(horizontal = 4.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(base)
            .combinedClickable(onClick = onToggle, onLongClick = onOpen)
            .semantics { contentDescription = "$name, $toggleLabel" }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            color = content,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (group.collapsed) {
            Text(
                count.toString(),
                color = content.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
private fun TabStripTab(
    preview: TabPreview,
    group: TabGroup?,
    active: Boolean,
    width: Dp,
    showDivider: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onCloseOthers: () -> Unit,
    onNewTabInGroup: () -> Unit,
    onAddToNewGroup: () -> Unit,
    onRemoveFromGroup: () -> Unit,
    canGroup: Boolean,
    canCloseOthers: Boolean,
    profileColor: Int?
) {
    val colors = LocalClintColors.current
    val favicon = rememberTabFavicon(preview)
    var menuExpanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    val title = preview.title.ifBlank { stringResource(R.string.new_tab) }
    Box(Modifier.width(width).fillMaxHeight().padding(vertical = 3.dp)) {
        if (showDivider) {
            Box(Modifier.align(Alignment.CenterStart).width(1.dp).height(16.dp).background(colors.divider))
        }
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp)
                .clip(shape)
                .background(if (active) colors.surfaceVariant else Color.Transparent)
                .combinedClickable(onClick = onSelect, onLongClick = { menuExpanded = true })
                .semantics {
                    contentDescription = title
                    selected = active
                }
                .padding(start = 10.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(19.dp)) {
                if (favicon != null && !preview.isIncognito) {
                    Image(favicon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(16.dp).align(Alignment.TopStart))
                } else {
                    Icon(
                        if (preview.isIncognito) Icons.Filled.VisibilityOff else Icons.Filled.Public,
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier.size(16.dp).align(Alignment.TopStart)
                    )
                }
                if (profileColor != null) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (active) colors.surfaceVariant else colors.surface)
                            .padding(1.5.dp)
                            .clip(CircleShape)
                            .background(Color(profileColor))
                    )
                }
            }
            Text(
                title,
                color = if (active) colors.onSurface else colors.secondaryText,
                fontSize = 12.sp,
                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            if (active || width >= 128.dp) {
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close_tab),
                        tint = colors.secondaryText,
                        modifier = Modifier.size(14.dp)
                    )
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }
        }
        if (group != null) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(group.color))
            )
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            shape = PopupShape,
            containerColor = colors.popupBackground,
            border = BorderStroke(1.dp, colors.popupStroke)
        ) {
            ListMenuItem(Icons.Filled.Close, stringResource(R.string.close_tab), checked = false) {
                menuExpanded = false; onClose()
            }
            if (canCloseOthers) {
                ListMenuItem(Icons.Filled.DeleteSweep, stringResource(R.string.tab_strip_close_others), checked = false) {
                    menuExpanded = false; onCloseOthers()
                }
            }
            if (group != null) {
                ListMenuItem(Icons.Filled.Add, stringResource(R.string.tab_group_new_tab_in_group), checked = false) {
                    menuExpanded = false; onNewTabInGroup()
                }
                ListMenuItem(Icons.Filled.LinkOff, stringResource(R.string.tab_strip_remove_from_group), checked = false) {
                    menuExpanded = false; onRemoveFromGroup()
                }
            } else if (canGroup) {
                ListMenuItem(Icons.Filled.CreateNewFolder, stringResource(R.string.tab_strip_add_to_new_group), checked = false) {
                    menuExpanded = false; onAddToNewGroup()
                }
            }
        }
    }
}
