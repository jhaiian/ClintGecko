package com.jhaiian.clint.tabs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintCheckbox
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintDialogActionFooter
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.listscreen.ListMenuItem
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors

object TabGroupPrefs {
    const val PREF_SHOW_GROUP_BAR = "show_tab_group_bar"
}

data class TabGroupBarState(
    val group: TabGroup,
    val tabs: List<TabPreview>,
    val activeTabId: String
)

internal const val TAB_GROUP_NAME_MAX_LENGTH = 40

internal fun tabGroupTint(color: Int, isLight: Boolean, strong: Boolean = false): Color =
    Color(color).copy(alpha = if (strong) (if (isLight) 0.26f else 0.34f) else (if (isLight) 0.12f else 0.18f))

@Composable
internal fun tabGroupDisplayName(group: TabGroup): String =
    group.name.ifBlank { stringResource(R.string.tab_group_untitled) }

@Composable
internal fun TabGroupDot(color: Int, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(Color(color)))
}

@Composable
internal fun TabGroupBar(
    state: TabGroupBarState,
    onSelectTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onOpenGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val listState = rememberLazyListState()
    val activeIndex = state.tabs.indexOfFirst { it.id == state.activeTabId }
    LaunchedEffect(state.activeTabId, state.tabs.size) {
        if (activeIndex >= 0) listState.animateScrollToItem(activeIndex)
    }
    val groupName = tabGroupDisplayName(state.group)
    val openDescription = stringResource(R.string.tab_group_bar_open_desc, groupName)
    Row(
        modifier.fillMaxWidth().height(44.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier
                .widthIn(max = 150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(tabGroupTint(state.group.color, colors.isLight))
                .clickable(onClick = onOpenGroup)
                .semantics { contentDescription = openDescription }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabGroupDot(state.group.color, 10.dp)
            Text(
                groupName,
                color = colors.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp).weight(1f, fill = false)
            )
            Text(
                state.tabs.size.toString(),
                color = colors.secondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
        LazyRow(
            state = listState,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(state.tabs, key = { it.id }) { tab ->
                TabGroupBarItem(
                    tab = tab,
                    active = tab.id == state.activeTabId,
                    groupColor = state.group.color,
                    onClick = { onSelectTab(tab.id) }
                )
            }
        }
        IconButton(onClick = onNewTab, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.tab_group_new_tab_in_group), tint = colors.iconTint)
        }
    }
}

@Composable
private fun TabGroupBarItem(tab: TabPreview, active: Boolean, groupColor: Int, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    val favicon = rememberTabFavicon(tab)
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .size(34.dp)
            .clip(shape)
            .background(if (active) tabGroupTint(groupColor, colors.isLight, strong = true) else colors.surfaceVariant)
            .border(if (active) 2.dp else 0.dp, if (active) Color(groupColor) else Color.Transparent, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (favicon != null && !tab.isIncognito) {
            Image(favicon.asImageBitmap(), contentDescription = tab.title, modifier = Modifier.size(18.dp))
        } else {
            Icon(
                if (tab.isIncognito) Icons.Filled.VisibilityOff else Icons.Filled.Public,
                contentDescription = tab.title,
                tint = colors.secondaryText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
internal fun TabGroupChipsRow(
    groups: List<TabGroup>,
    counts: Map<String, Int>,
    activeGroupId: String?,
    hoverKey: Any?,
    onChipClick: (TabGroup) -> Unit,
    onNewGroup: () -> Unit,
    onChipBounds: (String, Rect?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(groups, key = { it.id }) { group ->
            val hovered = hoverKey == groupChipKey(group.id)
            val highlighted = hovered || group.id == activeGroupId
            val scale by animateFloatAsState(if (hovered) 1.08f else 1f, tween(140), label = "chipScale")
            DisposableEffect(group.id) { onDispose { onChipBounds(group.id, null) } }
            Row(
                Modifier
                    .onGloballyPositioned { onChipBounds(group.id, it.boundsInRoot()) }
                    .widthIn(max = 200.dp)
                    .height(36.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(18.dp))
                    .background(tabGroupTint(group.color, colors.isLight, strong = hovered))
                    .border(1.5.dp, if (highlighted) Color(group.color) else Color.Transparent, RoundedCornerShape(18.dp))
                    .clickable { onChipClick(group) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabGroupDot(group.color, 10.dp)
                Text(
                    tabGroupDisplayName(group),
                    color = colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 7.dp).weight(1f, fill = false)
                )
                Text(
                    (counts[group.id] ?: 0).toString(),
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 7.dp)
                )
            }
        }
        item(key = "chip:new") {
            Row(
                Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, colors.divider, RoundedCornerShape(18.dp))
                    .clickable(onClick = onNewGroup)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                Text(
                    stringResource(R.string.tab_group_new),
                    color = colors.onSurface,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
internal fun TabGroupHeader(
    group: TabGroup,
    tabs: List<TabPreview>,
    containsActive: Boolean,
    highlighted: Boolean,
    selectionMode: Boolean,
    allSelected: Boolean,
    onToggleCollapsed: () -> Unit,
    onLongPress: () -> Unit,
    onToggleSelection: () -> Unit,
    onNewTab: () -> Unit,
    onEdit: () -> Unit,
    onSelectTabs: () -> Unit,
    onUngroup: () -> Unit,
    onCloseGroup: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier? = null,
    longPressDragModifier: Modifier? = null
) {
    val colors = LocalClintColors.current
    val dragHandle = dragHandleModifier?.takeIf { !selectionMode }
    val shape: Shape = if (group.collapsed) RoundedCornerShape(18.dp) else RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    val chevronRotation by animateFloatAsState(if (group.collapsed) 0f else 180f, tween(200), label = "groupChevron")
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(tabGroupTint(group.color, colors.isLight, strong = highlighted))
            .border(if (highlighted) 2.dp else 0.dp, if (highlighted) Color(group.color) else Color.Transparent, shape)
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelection() else onToggleCollapsed() },
                onLongClick = if (longPressDragModifier == null) ({ if (!selectionMode) onLongPress() }) else null
            )
            .then(longPressDragModifier ?: Modifier)
            .padding(start = if (dragHandle != null) 6.dp else 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dragHandle != null) {
            Icon(
                Icons.Filled.DragIndicator,
                contentDescription = stringResource(R.string.tab_menu_drag_handle_desc),
                tint = colors.iconTint,
                modifier = dragHandle.padding(end = 6.dp).size(24.dp).padding(2.dp)
            )
        }
        if (selectionMode) {
            ClintCheckbox(checked = allSelected, onCheckedChange = { onToggleSelection() }, modifier = Modifier.size(20.dp))
        } else {
            TabGroupDot(group.color, 14.dp)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                tabGroupDisplayName(group),
                color = colors.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    pluralStringResource(R.plurals.tab_menu_tab_count, tabs.size, tabs.size),
                    color = colors.secondaryText,
                    fontSize = 12.sp
                )
                if (containsActive) {
                    Box(Modifier.padding(horizontal = 6.dp).size(3.dp).clip(CircleShape).background(colors.secondaryText))
                    Text(
                        stringResource(R.string.tab_group_current),
                        color = Color(group.color),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (group.collapsed) {
            TabGroupFaviconStack(tabs.take(4), modifier = Modifier.padding(start = 8.dp))
        }
        if (!selectionMode) {
            IconButton(onClick = onNewTab, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.tab_group_new_tab_in_group), tint = colors.iconTint, modifier = Modifier.size(20.dp))
            }
        }
        Icon(
            Icons.Filled.ExpandMore,
            contentDescription = stringResource(if (group.collapsed) R.string.tab_group_expand else R.string.tab_group_collapse),
            tint = colors.iconTint,
            modifier = Modifier.size(22.dp).rotate(chevronRotation)
        )
        if (!selectionMode) {
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.tab_group_options_desc), tint = colors.iconTint, modifier = Modifier.size(20.dp))
                }
                TabGroupMenu(
                    expanded = menuExpanded,
                    collapsed = group.collapsed,
                    onDismiss = { menuExpanded = false },
                    onNewTab = onNewTab,
                    onEdit = onEdit,
                    onToggleCollapsed = onToggleCollapsed,
                    onSelectTabs = onSelectTabs,
                    onUngroup = onUngroup,
                    onCloseGroup = onCloseGroup
                )
            }
        } else {
            Spacer(Modifier.width(10.dp))
        }
    }
}

@Composable
private fun TabGroupFaviconStack(tabs: List<TabPreview>, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    if (tabs.isEmpty()) return
    val step = 14.dp
    val itemSize = 24.dp
    Box(modifier.width(itemSize + step * (tabs.size - 1)).height(itemSize)) {
        tabs.forEachIndexed { index, tab ->
            val favicon = rememberTabFavicon(tab)
            Box(
                Modifier
                    .offset(x = step * index)
                    .size(itemSize)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(1.5.dp, colors.surface, CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (favicon != null && !tab.isIncognito) {
                    Image(favicon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(16.dp))
                } else {
                    Icon(
                        if (tab.isIncognito) Icons.Filled.VisibilityOff else Icons.Filled.Public,
                        contentDescription = null,
                        tint = colors.secondaryText,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TabGroupMenu(
    expanded: Boolean,
    collapsed: Boolean,
    onDismiss: () -> Unit,
    onNewTab: () -> Unit,
    onEdit: () -> Unit,
    onToggleCollapsed: () -> Unit,
    onSelectTabs: () -> Unit,
    onUngroup: () -> Unit,
    onCloseGroup: () -> Unit
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke)
    ) {
        ListMenuItem(Icons.Filled.Add, stringResource(R.string.tab_group_new_tab_in_group), checked = false) { onDismiss(); onNewTab() }
        ListMenuItem(Icons.Filled.Edit, stringResource(R.string.tab_group_edit), checked = false) { onDismiss(); onEdit() }
        ListMenuItem(
            if (collapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
            stringResource(if (collapsed) R.string.tab_group_expand else R.string.tab_group_collapse),
            checked = false
        ) { onDismiss(); onToggleCollapsed() }
        ListMenuItem(Icons.Filled.Checklist, stringResource(R.string.tab_group_select_tabs), checked = false) { onDismiss(); onSelectTabs() }
        ListMenuItem(Icons.Filled.LinkOff, stringResource(R.string.tab_group_ungroup), checked = false) { onDismiss(); onUngroup() }
        ListMenuItem(Icons.Filled.Close, stringResource(R.string.tab_group_close), checked = false) { onDismiss(); onCloseGroup() }
    }
}

@Composable
internal fun TabSelectionActionBar(
    hasSelection: Boolean,
    canUngroup: Boolean,
    onGroup: () -> Unit,
    onMove: () -> Unit,
    onUngroup: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        color = colors.cardBackground,
        shadowElevation = 6.dp
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            SelectionAction(Icons.Filled.CreateNewFolder, stringResource(R.string.tab_selection_group), hasSelection, onGroup)
            SelectionAction(Icons.AutoMirrored.Filled.DriveFileMove, stringResource(R.string.tab_selection_move), hasSelection, onMove)
            SelectionAction(Icons.Filled.LinkOff, stringResource(R.string.tab_group_ungroup), hasSelection && canUngroup, onUngroup)
            SelectionAction(Icons.Filled.Close, stringResource(R.string.action_close), hasSelection, onClose)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SelectionAction(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.38f)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
        Text(
            label,
            color = colors.onSurface,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
        )
    }
}

@Composable
internal fun TabGroupEditorDialog(
    title: String,
    positiveLabel: String,
    initialName: String,
    initialColor: Int,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    val colors = LocalClintColors.current
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableIntStateOf(initialColor) }
    ClintDialog(
        title = title,
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            ClintDialogActionFooter(
                onCancel = onDismiss,
                positiveLabel = positiveLabel,
                onPositive = { onConfirm(name.trim(), color) }
            )
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(tabGroupTint(color, colors.isLight))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabGroupDot(color, 12.dp)
                Text(
                    name.trim().ifBlank { stringResource(R.string.tab_group_untitled) },
                    color = colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            ClintOutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= TAB_GROUP_NAME_MAX_LENGTH) name = it },
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                label = { Text(stringResource(R.string.tab_group_name_label)) },
                singleLine = true
            )
            Text(
                stringResource(R.string.tab_group_color_label),
                color = colors.secondaryText,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 10.dp)
            )
            com.jhaiian.clint.ui.ClintColorSwatchGrid(
                presets = TabGroupColor.entries.map { it.argb },
                labels = TabGroupColor.entries.map { stringResource(it.labelRes) },
                selected = color,
                hideStatusBar = hideStatusBar,
                hideSystemNavigation = hideSystemNavigation,
                onSelect = { color = it }
            )
        }
    }
}

@Composable
internal fun TabGroupPickerDialog(
    groups: List<TabGroup>,
    counts: Map<String, Int>,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onPick: (String) -> Unit,
    onNewGroup: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    ClintDialog(
        title = stringResource(R.string.tab_group_move_title),
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss
    ) {
        groups.forEach { group ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onPick(group.id) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabGroupDot(group.color, 14.dp)
                Text(
                    tabGroupDisplayName(group),
                    color = colors.onSurface,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 14.dp)
                )
                if (group.isIncognito) {
                    Icon(Icons.Filled.VisibilityOff, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.padding(start = 8.dp).size(16.dp))
                }
                Text(
                    pluralStringResource(R.plurals.tab_menu_tab_count, counts[group.id] ?: 0, counts[group.id] ?: 0),
                    color = colors.secondaryText,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onNewGroup)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.tab_group_new),
                color = colors.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}
