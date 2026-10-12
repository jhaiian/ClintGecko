package com.jhaiian.clint.tabs

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.delegates.saveTabs
import com.jhaiian.clint.browser.delegates.toggleTabMute
import com.jhaiian.clint.ui.TabMenuOverflowMenu
import com.jhaiian.clint.ui.TabSelectionOverflowMenu
import com.jhaiian.clint.ui.theme.LocalClintColors

private sealed class TabMenuItem {
    data class SectionHeader(val isIncognitoSection: Boolean) : TabMenuItem()
    data class Tab(val preview: TabPreview) : TabMenuItem()
}

private fun buildRenderList(tabs: List<TabPreview>): List<TabMenuItem> {
    val hasBothSections = tabs.any { !it.isIncognito } && tabs.any { it.isIncognito }
    val result = mutableListOf<TabMenuItem>()
    for (incognitoSection in listOf(false, true)) {
        val sectionTabs = tabs.filter { it.isIncognito == incognitoSection }
        if (sectionTabs.isEmpty()) continue
        if (hasBothSections) result.add(TabMenuItem.SectionHeader(incognitoSection))
        sectionTabs.forEach { result.add(TabMenuItem.Tab(it)) }
    }
    return result
}

@Composable
fun TabMenuScreen(activity: MainActivity, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val density = LocalDensity.current
    val uiState = remember { TabMenuUiState() }
    val tabs = remember { mutableStateListOf<TabPreview>().apply { addAll(activity.tabManager.previews()) } }
    val activeTabId = activity.tabManager.activeTab?.id
    val initialIndex = remember {
        buildRenderList(tabs)
            .indexOfFirst { it is TabMenuItem.Tab && it.preview.id == activeTabId }
            .coerceAtLeast(0)
    }
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = initialIndex)
    val dragState = remember { TabDragState(gridState) }

    fun syncFromManager() {
        tabs.clear(); tabs.addAll(activity.tabManager.previews())
    }

    fun openTab(tabId: String) {
        val idx = activity.tabManager.tabs.indexOfFirst { it.id == tabId }
        if (idx < 0) return
        activity.onTabSelected(idx)
        onDismiss()
    }

    fun closeTabs(ids: List<String>) {
        val closingAll = ids.isNotEmpty() && ids.toSet() == tabs.map { it.id }.toSet()
        ids.forEach { id ->
            val idx = activity.tabManager.tabs.indexOfFirst { it.id == id }
            if (idx >= 0) activity.onTabClosed(idx)
        }
        if (closingAll) { onDismiss(); return }
        syncFromManager()
    }

    fun closeAllTabs() {
        tabs.map { it.id }.forEach { id ->
            val idx = activity.tabManager.tabs.indexOfFirst { it.id == id }
            if (idx >= 0) activity.onTabClosed(idx)
        }
        onDismiss()
    }

    fun onDragMoved() {
        val draggedId = dragState.draggingId ?: return
        val draggedPreview = tabs.find { it.id == draggedId } ?: return
        val hoverKey = dragState.hoverKey ?: return
        val targetTab = tabs.find { tabItemKey(it.id) == hoverKey } ?: return
        if (targetTab.id == draggedId || targetTab.isIncognito != draggedPreview.isIncognito) return
        val from = tabs.indexOfFirst { it.id == draggedId }
        val to = tabs.indexOfFirst { it.id == targetTab.id }
        if (from != -1 && to != -1 && from != to) {
            tabs.add(to, tabs.removeAt(from))
        }
    }

    fun commitDrag() {
        val draggedId = dragState.draggingId
        dragState.end()
        if (draggedId == null) return
        activity.tabManager.reorderTo(tabs.map { it.id })
        activity.saveTabs()
        syncFromManager()
    }

    BackHandler {
        if (uiState.selectionMode) uiState.exitSelectionMode() else onDismiss()
    }

    var showProfilePicker by remember { mutableStateOf(false) }
    if (showProfilePicker) {
        val dialogPrefs = remember { androidx.preference.PreferenceManager.getDefaultSharedPreferences(activity) }
        com.jhaiian.clint.profiles.ProfilePickerDialog(
            profiles = remember { com.jhaiian.clint.profiles.ProfileRepository.all(activity) },
            hideStatusBar = dialogPrefs.getBoolean("hide_status_bar", false),
            hideSystemNavigation = dialogPrefs.getBoolean("hide_system_navigation", false),
            onPick = { id -> showProfilePicker = false; activity.onNewTabInProfile(id); onDismiss() },
            onManage = {
                showProfilePicker = false
                activity.startActivity(android.content.Intent(activity, com.jhaiian.clint.profiles.ProfilesActivity::class.java))
                onDismiss()
            },
            onDismiss = { showProfilePicker = false }
        )
    }

    Column(Modifier.fillMaxSize().background(colors.surface).statusBarsPadding().navigationBarsPadding()) {
        TabMenuTopBar(
            tabCount = tabs.size,
            selectionMode = uiState.selectionMode,
            selectedCount = uiState.selectedIds.size,
            onToggleSelectionMode = { uiState.selectionMode = true },
            onSelectAll = { uiState.selectAll(tabs.map { it.id }) },
            onInvertSelection = { uiState.invertSelection(tabs.map { it.id }) },
            onDeleteSelected = { closeTabs(uiState.selectedIds.toList()); uiState.exitSelectionMode() },
            onNewTab = { activity.onNewTab(); onDismiss() },
            onNewIncognitoTab = { activity.onNewIncognitoTab(); onDismiss() },
            onNewProfileTab = {
                if (com.jhaiian.clint.profiles.ProfileRepository.all(activity).isEmpty()) {
                    activity.startActivity(android.content.Intent(activity, com.jhaiian.clint.profiles.ProfilesActivity::class.java))
                    onDismiss()
                } else {
                    showProfilePicker = true
                }
            },
            onCloseAllTabs = { closeAllTabs() }
        )

        AnimatedVisibility(
            visible = !uiState.selectionMode,
            enter = fadeIn(tween(180)) + expandVertically(spring(dampingRatio = 0.85f)),
            exit = fadeOut(tween(120)) + shrinkVertically(tween(150))
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 10.dp)) {
                NewTabButton(
                    text = stringResource(R.string.new_tab),
                    iconRes = Icons.Filled.Add,
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                    onClick = { activity.onNewTab(); onDismiss() }
                )
                NewTabButton(
                    text = stringResource(R.string.new_incognito_tab),
                    iconRes = Icons.Filled.VisibilityOff,
                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                    onClick = { activity.onNewIncognitoTab(); onDismiss() }
                )
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (tabs.isEmpty()) {
                Text(stringResource(R.string.no_tabs), color = colors.secondaryText, modifier = Modifier.align(Alignment.Center))
            } else {
                val renderList = buildRenderList(tabs)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = renderList,
                        key = { item ->
                            when (item) {
                                is TabMenuItem.SectionHeader -> "section:${item.isIncognitoSection}"
                                is TabMenuItem.Tab -> tabItemKey(item.preview.id)
                            }
                        },
                        span = { item ->
                            when (item) {
                                is TabMenuItem.SectionHeader -> GridItemSpan(maxLineSpan)
                                is TabMenuItem.Tab -> GridItemSpan(1)
                            }
                        }
                    ) { item ->
                        AnimatedGridItem {
                            when (item) {
                                is TabMenuItem.SectionHeader -> TabMenuSectionHeader(isIncognitoSection = item.isIncognitoSection)
                                is TabMenuItem.Tab -> {
                                    val preview = item.preview
                                    val thumbnail by produceState(TabThumbnailCache.peek(preview.id), preview.id) {
                                        if (value == null) value = withContext(Dispatchers.IO) { TabThumbnailCache.get(activity, preview.id) }
                                    }
                                    val isDragging = dragState.draggingId == preview.id
                                    val ghostAlpha by animateFloatAsState(if (isDragging) 0.25f else 1f, label = "ghostAlpha")
                                    TabMenuCard(
                                        preview = preview,
                                        isActive = preview.id == activeTabId,
                                        thumbnail = thumbnail,
                                        selectionMode = uiState.selectionMode,
                                        selected = uiState.selectedIds.contains(preview.id),
                                        dragHandleModifier = Modifier.pointerInput(preview.id) {
                                            detectDragGestures(
                                                onDragStart = { dragState.start(preview.id) },
                                                onDrag = { change, amount -> change.consume(); dragState.drag(amount); onDragMoved() },
                                                onDragEnd = { commitDrag() },
                                                onDragCancel = { dragState.end() }
                                            )
                                        },
                                        onOpen = { openTab(preview.id) },
                                        onEnterSelection = { uiState.enterSelectionMode(preview.id) },
                                        onToggleSelect = { uiState.toggleSelected(preview.id) },
                                        onClose = { closeTabs(listOf(preview.id)) },
                                        onToggleMute = { activity.toggleTabMute(preview.id) },
                                        modifier = Modifier.alpha(ghostAlpha)
                                    )
                                }
                            }
                        }
                    }
                }

                val draggedId = dragState.draggingId
                if (draggedId != null) {
                    val draggedPreview = tabs.find { it.id == draggedId }
                    if (draggedPreview != null) {
                        val thumbnail by produceState(TabThumbnailCache.peek(draggedPreview.id), draggedPreview.id) {
                            if (value == null) value = withContext(Dispatchers.IO) { TabThumbnailCache.get(activity, draggedPreview.id) }
                        }
                        val offsetX = with(density) { (dragState.originOffset.x + dragState.dragOffset.x).toDp() }
                        val offsetY = with(density) { (dragState.originOffset.y + dragState.dragOffset.y).toDp() }
                        val cardWidth = with(density) { dragState.originSize.width.toDp() }
                        val cardHeight = with(density) { dragState.originSize.height.toDp() }
                        val liftScale by animateFloatAsState(1.06f, animationSpec = spring(dampingRatio = 0.5f), label = "dragLiftScale")
                        Box(
                            Modifier
                                .offset(x = offsetX, y = offsetY)
                                .size(cardWidth, cardHeight)
                                .graphicsLayer { scaleX = liftScale; scaleY = liftScale }
                                .zIndex(10f)
                                .shadow(12.dp, RoundedCornerShape(16.dp))
                        ) {
                            TabMenuCard(
                                preview = draggedPreview,
                                isActive = draggedPreview.id == activeTabId,
                                thumbnail = thumbnail,
                                selectionMode = false,
                                selected = false,
                                dragHandleModifier = Modifier,
                                onOpen = {},
                                onEnterSelection = {},
                                onToggleSelect = {},
                                onClose = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LazyGridItemScope.AnimatedGridItem(content: @Composable () -> Unit) {
    Box(
        Modifier.animateItem(
            fadeInSpec = tween(220),
            placementSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
            fadeOutSpec = tween(150)
        )
    ) {
        content()
    }
}

@Composable
private fun TabMenuTopBar(
    tabCount: Int,
    selectionMode: Boolean,
    selectedCount: Int,
    onToggleSelectionMode: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onNewProfileTab: () -> Unit,
    onCloseAllTabs: () -> Unit
) {
    val colors = LocalClintColors.current
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = selectionMode,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                (fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 3 }) togetherWith
                    (fadeOut(tween(120)) + slideOutVertically(tween(120)) { -it / 3 })
            },
            label = "tabMenuTitle"
        ) { inSelectionMode ->
            Text(
                if (inSelectionMode) {
                    pluralStringResource(R.plurals.tab_menu_selected_count, selectedCount, selectedCount)
                } else if (tabCount == 0) {
                    stringResource(R.string.no_tabs)
                } else {
                    pluralStringResource(R.plurals.tab_menu_tab_count, tabCount, tabCount)
                },
                color = colors.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        AnimatedContent(
            targetState = selectionMode,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "tabMenuActions"
        ) { inSelectionMode ->
            Box {
                IconButton(onClick = { overflowMenuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.tab_menu_overflow_desc), tint = colors.primary)
                }
                if (inSelectionMode) {
                    TabSelectionOverflowMenu(
                        expanded = overflowMenuExpanded,
                        onDismiss = { overflowMenuExpanded = false },
                        onSelectAll = onSelectAll,
                        onInvertSelection = onInvertSelection,
                        onDeleteSelected = onDeleteSelected
                    )
                } else {
                    TabMenuOverflowMenu(
                        expanded = overflowMenuExpanded,
                        onDismiss = { overflowMenuExpanded = false },
                        onNewTab = onNewTab,
                        onNewIncognitoTab = onNewIncognitoTab,
                        onCloseAllTabs = onCloseAllTabs,
                        onSelectTabs = onToggleSelectionMode,
                        showProfileTab = com.jhaiian.clint.profiles.WebProfiles.isSupported(),
                        onNewProfileTab = onNewProfileTab
                    )
                }
            }
        }
    }
}
