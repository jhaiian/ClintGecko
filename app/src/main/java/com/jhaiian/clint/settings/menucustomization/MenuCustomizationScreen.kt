package com.jhaiian.clint.settings.menucustomization

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.menu.BrowserMenuCustomizationStore
import com.jhaiian.clint.browser.menu.BrowserMenuPreview
import com.jhaiian.clint.browser.menu.CustomizableMenuItem
import com.jhaiian.clint.browser.menu.MenuNavSlot
import com.jhaiian.clint.browser.menu.MenuSection
import com.jhaiian.clint.browser.menu.descriptionRes
import com.jhaiian.clint.browser.menu.icon
import com.jhaiian.clint.browser.menu.titleRes
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun MenuCustomizationScreen(
    state: MenuCustomizationUiState,
    maxContentWidth: Dp?,
    onExit: () -> Unit,
    onChanged: () -> Unit,
    onResetClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val listState = rememberLazyListState()
    val dragState = remember { MenuCustomizeDragState(listState) }
    var previewExpanded by remember { mutableStateOf(false) }
    val rows = state.rows
    val rowSections = state.rowSections()

    fun onDragMoved() {
        val draggedId = dragState.draggingId ?: return
        val hoverKey = dragState.hoverKey ?: return
        val targetIndex = rows.indexOfFirst { it.key == hoverKey }
        val fromIndex = rows.indexOfFirst { it is MenuItemEntryRow && it.item.id == draggedId }
        if (targetIndex == -1 || fromIndex == -1 || targetIndex == fromIndex) return
        rows.add(targetIndex.coerceAtLeast(1), rows.removeAt(fromIndex))
    }

    fun commitDrag() {
        val draggedId = dragState.draggingId
        dragState.end()
        if (draggedId != null) onChanged()
    }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            MenuCustomizationToolbar(onBack = onExit, onResetClick = onResetClick)
            HorizontalDivider(color = colors.divider, thickness = 1.dp)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AdaptiveWidthContainer(maxContentWidth) {
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp)
                        ) {
                            item(key = "preview") { MenuPreviewCard(state) { previewExpanded = true } }
                            item(key = "layout") {
                                MenuLayoutCard(state, onChanged)
                            }
                            item(key = "nav") {
                                MenuNavSlotsCard(state, onChanged)
                            }
                            item(key = "arrange_intro") {
                                Text(
                                    stringResource(R.string.menu_customization_description),
                                    color = colors.secondaryText,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 20.dp)
                                )
                            }
                            itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
                                Box(
                                    Modifier.animateItem(
                                        fadeInSpec = tween(220),
                                        placementSpec = spring(dampingRatio = 0.82f, stiffness = 400f),
                                        fadeOutSpec = tween(150)
                                    )
                                ) {
                                    when (row) {
                                        is MenuSectionHeaderRow -> MenuSectionHeader(
                                            section = row.section,
                                            count = rowSections.indices.count { rowSections[it] == row.section && rows[it] is MenuItemEntryRow },
                                            isEmpty = rows.getOrNull(index + 1) !is MenuItemEntryRow
                                        )
                                        is MenuItemEntryRow -> {
                                            val isDragging = dragState.draggingId == row.item.id
                                            val ghostAlpha by animateFloatAsState(if (isDragging) 0.2f else 1f, label = "menuItemGhostAlpha")
                                            MenuCustomizeItemRow(
                                                item = row.item,
                                                section = rowSections[index],
                                                modifier = Modifier.alpha(ghostAlpha),
                                                onMove = { section ->
                                                    state.moveToSection(row.item, section)
                                                    onChanged()
                                                },
                                                dragHandleModifier = Modifier.pointerInput(row.item.id) {
                                                    detectDragGestures(
                                                        onDragStart = { dragState.start(row.item.id) },
                                                        onDrag = { change, amount -> change.consume(); dragState.drag(amount.y); onDragMoved() },
                                                        onDragEnd = { commitDrag() },
                                                        onDragCancel = { commitDrag() }
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            item(key = "pinned_settings_note") {
                                PinnedSettingsNote()
                            }
                        }

                        val draggedId = dragState.draggingId
                        if (draggedId != null) {
                            val draggedIndex = rows.indexOfFirst { it is MenuItemEntryRow && it.item.id == draggedId }
                            val draggedRow = rows.getOrNull(draggedIndex) as? MenuItemEntryRow
                            if (draggedRow != null) {
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
                                    MenuCustomizeItemRow(
                                        item = draggedRow.item,
                                        section = rowSections[draggedIndex],
                                        modifier = Modifier,
                                        onMove = {},
                                        dragHandleModifier = Modifier
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (previewExpanded) MenuPreviewOverlay(state) { previewExpanded = false }
    }
}

@Composable
private fun MenuCustomizationToolbar(onBack: () -> Unit, onResetClick: () -> Unit) {
    val colors = LocalClintColors.current
    Surface(color = colors.background, modifier = Modifier.statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = colors.onSurface)
            }
            Text(
                stringResource(R.string.menu_customization_title),
                color = colors.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
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
private fun MenuCardLabel(text: String) {
    Text(
        text,
        color = LocalClintColors.current.secondaryText,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp)
    )
}

private const val MENU_PREVIEW_WIDTH = 360
private const val MENU_PREVIEW_RATIO = 680f / MENU_PREVIEW_WIDTH

@Composable
private fun MenuPreviewFrame(state: MenuCustomizationUiState, width: Dp, scrollable: Boolean) {
    val colors = LocalClintColors.current
    val baseDensity = LocalDensity.current
    val scale = width.value / MENU_PREVIEW_WIDTH
    val scaledDensity = remember(baseDensity, scale) { Density(baseDensity.density * scale, baseDensity.fontScale) }
    val shape = RoundedCornerShape(width * 0.08f)
    val popup = state.menuStyle != "bottom_sheet"
    val layout = state.toLayout()
    Box(
        Modifier
            .size(width, width * MENU_PREVIEW_RATIO)
            .clip(shape)
            .background(colors.background)
            .border(1.dp, colors.divider, shape)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            val scroll = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
            Box(Modifier.fillMaxSize(), contentAlignment = if (popup) Alignment.TopEnd else Alignment.BottomCenter) {
                if (popup) {
                    BrowserMenuPreview(layout, popup = true, modifier = scroll.padding(12.dp))
                } else {
                    Column(
                        scroll
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(colors.popupBackground)
                            .padding(top = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .padding(bottom = 12.dp)
                                .width(32.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(colors.divider)
                        )
                        BrowserMenuPreview(layout, popup = false, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuPreviewCard(state: MenuCustomizationUiState, onExpand: () -> Unit) {
    val colors = LocalClintColors.current
    Surface(
        color = colors.cardBackground,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            MenuPreviewFrame(state, width = 72.dp, scrollable = false)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.homepage_preview_title),
                    color = colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.menu_customization_preview_hint),
                    color = colors.secondaryText, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            IconButton(onClick = onExpand) {
                Icon(
                    imageVector = Icons.Filled.OpenInFull,
                    contentDescription = stringResource(R.string.homepage_preview_expand),
                    tint = colors.primary
                )
            }
        }
    }
}

@Composable
private fun MenuPreviewOverlay(state: MenuCustomizationUiState, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        val byWidth = maxWidth - 48.dp
        val byHeight = (maxHeight - 64.dp) / MENU_PREVIEW_RATIO
        MenuPreviewFrame(state, width = if (byWidth < byHeight) byWidth else byHeight, scrollable = true)
    }
}

@Composable
private fun MenuLayoutCard(state: MenuCustomizationUiState, onChanged: () -> Unit) {
    val colors = LocalClintColors.current
    Column {
        MenuCardLabel(stringResource(R.string.menu_customization_layout))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardBackground)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MenuOptionGroup(stringResource(R.string.pref_menu_style_title)) {
                MenuSegmented(
                    options = listOf(
                        "bottom_sheet" to stringResource(R.string.menu_style_bottom_sheet),
                        "popup" to stringResource(R.string.menu_style_popup)
                    ),
                    selected = if (state.menuStyle == "bottom_sheet") "bottom_sheet" else "popup",
                    onSelect = { state.menuStyle = it; onChanged() }
                )
            }
            MenuOptionGroup(stringResource(R.string.menu_customization_grid_columns)) {
                MenuSegmented(
                    options = BrowserMenuCustomizationStore.GRID_COLUMN_OPTIONS.map { it to it.toString() },
                    selected = state.gridColumns,
                    onSelect = { state.gridColumns = it; onChanged() }
                )
            }
            MenuOptionGroup(stringResource(R.string.menu_customization_density)) {
                MenuSegmented(
                    options = listOf(
                        false to stringResource(R.string.menu_customization_density_comfortable),
                        true to stringResource(R.string.menu_customization_density_compact)
                    ),
                    selected = state.compact,
                    onSelect = { state.compact = it; onChanged() }
                )
            }
            MenuSwitchRow(
                title = stringResource(R.string.menu_customization_show_header),
                summary = stringResource(R.string.menu_customization_show_header_desc),
                checked = state.showHeader,
                onToggle = { state.showHeader = !state.showHeader; onChanged() }
            )
            MenuSwitchRow(
                title = stringResource(R.string.menu_customization_grid_labels),
                summary = stringResource(R.string.menu_customization_grid_labels_desc),
                checked = state.gridLabels,
                onToggle = { state.gridLabels = !state.gridLabels; onChanged() }
            )
        }
    }
}

@Composable
private fun MenuOptionGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = LocalClintColors.current.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        content()
    }
}

@Composable
private fun <T> MenuSegmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val colors = LocalClintColors.current
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceVariant)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(shape)
                    .background(if (isSelected) colors.primary else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(value) })
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (isSelected) colors.onPrimary else colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MenuSwitchRow(title: String, summary: String, checked: Boolean, onToggle: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = { onToggle() })
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(summary, color = colors.secondaryText, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp))
        }
        ClintSwitch(checked = checked)
    }
}

@Composable
private fun MenuNavSlotsCard(state: MenuCustomizationUiState, onChanged: () -> Unit) {
    val colors = LocalClintColors.current
    Column {
        MenuCardLabel(stringResource(R.string.menu_customization_nav_bar))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.cardBackground)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.padding(horizontal = 4.dp)) {
                MenuSwitchRow(
                    title = stringResource(R.string.menu_customization_show_nav_bar),
                    summary = stringResource(R.string.menu_customization_show_nav_bar_desc),
                    checked = state.showNavBar,
                    onToggle = { state.showNavBar = !state.showNavBar; onChanged() }
                )
            }
            if (state.showNavBar) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.navSlots.forEachIndexed { index, slot ->
                        MenuNavSlotButton(
                            slot = slot,
                            current = state.navSlots,
                            modifier = Modifier.weight(1f),
                            onPick = { picked ->
                                state.setNavSlot(index, picked)
                                onChanged()
                            }
                        )
                    }
                }
                Text(
                    stringResource(R.string.menu_customization_nav_bar_desc),
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MenuNavSlotButton(slot: MenuNavSlot, current: List<MenuNavSlot>, modifier: Modifier, onPick: (MenuNavSlot) -> Unit) {
    val colors = LocalClintColors.current
    var open by remember { mutableStateOf(false) }
    val label = stringResource(slot.titleRes())
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surfaceVariant)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.menu_customization_nav_bar_change)) { open = true }
                .padding(horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(slot.icon(), contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(22.dp))
            Text(
                label,
                color = colors.onSurface,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        MenuOptionsDropdown(
            expanded = open,
            onDismiss = { open = false },
            options = MenuNavSlot.entries.map { Triple(it.icon(), stringResource(it.titleRes()), it) },
            isSelected = { it == slot },
            isMarked = { it in current && it != slot },
            onPick = { open = false; onPick(it) }
        )
    }
}

@Composable
private fun <T> MenuOptionsDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    options: List<Triple<ImageVector, String, T>>,
    isSelected: (T) -> Boolean,
    isMarked: (T) -> Boolean,
    onPick: (T) -> Unit
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke)
    ) {
        options.forEach { (icon, label, value) ->
            DropdownMenuItem(
                text = {
                    Text(
                        label,
                        color = if (isMarked(value)) colors.secondaryText else colors.popupText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                leadingIcon = { Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp)) },
                trailingIcon = if (isSelected(value)) {
                    { Icon(Icons.Filled.Check, contentDescription = null, tint = colors.popupCheck, modifier = Modifier.size(18.dp)) }
                } else null,
                onClick = { onPick(value) }
            )
        }
    }
}

@Composable
private fun MenuSectionHeader(section: MenuSection, count: Int, isEmpty: Boolean) {
    val colors = LocalClintColors.current
    Column(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 18.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(section.titleRes()),
                color = colors.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                count.toString(),
                color = colors.secondaryText,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
            Text(
                stringResource(section.descriptionRes()),
                color = colors.secondaryText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
                textAlign = TextAlign.End
            )
        }
        if (isEmpty) {
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, colors.divider, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.menu_customization_section_empty), color = colors.secondaryText, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun MenuCustomizeItemRow(
    item: CustomizableMenuItem,
    section: MenuSection,
    modifier: Modifier,
    onMove: (MenuSection) -> Unit,
    dragHandleModifier: Modifier
) {
    val colors = LocalClintColors.current
    val visible = section != MenuSection.HIDDEN
    val label = stringResource(item.titleRes())
    var open by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground)
            .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).then(dragHandleModifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.DragIndicator,
                contentDescription = stringResource(R.string.menu_customization_drag_handle_desc),
                tint = colors.secondaryText,
                modifier = Modifier.size(22.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.primary.copy(alpha = if (visible) 0.12f else 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon(),
                contentDescription = null,
                tint = if (visible) colors.primary else colors.secondaryText,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = label,
            color = if (visible) colors.onSurface else colors.secondaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)
        )
        Box {
            Row(
                Modifier
                    .heightIn(min = 36.dp)
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, colors.divider, RoundedCornerShape(50))
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.menu_customization_move_item, label)) { open = true }
                    .padding(start = 12.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(section.titleRes()),
                    color = colors.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = colors.secondaryText, modifier = Modifier.size(18.dp))
            }
            MenuOptionsDropdown(
                expanded = open,
                onDismiss = { open = false },
                options = MenuSection.entries.map { Triple(sectionIcon(it), stringResource(it.titleRes()), it) },
                isSelected = { it == section },
                isMarked = { false },
                onPick = { open = false; if (it != section) onMove(it) }
            )
        }
    }
}

private fun sectionIcon(section: MenuSection): ImageVector = when (section) {
    MenuSection.GRID -> Icons.Filled.Apps
    MenuSection.TOGGLES -> Icons.Filled.ToggleOn
    MenuSection.LIST -> Icons.AutoMirrored.Filled.List
    MenuSection.HIDDEN -> Icons.Filled.VisibilityOff
}

@Composable
private fun PinnedSettingsNote() {
    val colors = LocalClintColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 4.dp, end = 4.dp)
            .navigationBarsPadding()
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
