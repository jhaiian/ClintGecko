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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.delegates.closeTabsWithUndo
import com.jhaiian.clint.browser.delegates.createTabGroupWithNewTab
import com.jhaiian.clint.browser.delegates.onTabGroupsChanged
import com.jhaiian.clint.browser.delegates.openNewTabInGroup
import com.jhaiian.clint.browser.delegates.saveTabs
import com.jhaiian.clint.browser.delegates.toggleTabMute
import com.jhaiian.clint.ui.TabMenuOverflowMenu
import com.jhaiian.clint.ui.TabSelectionOverflowMenu
import com.jhaiian.clint.ui.listscreen.ClintSearchField
import com.jhaiian.clint.ui.showClintTwoActionSnackbar
import com.jhaiian.clint.ui.theme.LocalClintColors

private val GridEdgePadding = 7.dp
private val CellPadding = 5.dp
private val CellMinWidth = 160.dp
private val ListCellMinWidth = 340.dp
private const val PREF_TAB_MENU_LIST_VIEW = "tab_menu_list_view"
private val GroupCorner = 18.dp

private sealed class TabMenuItem {
    abstract val key: String
    open val fullSpan: Boolean = false

    data class SectionHeader(val isIncognitoSection: Boolean) : TabMenuItem() {
        override val key: String get() = "section:$isIncognitoSection"
        override val fullSpan: Boolean get() = true
    }

    data class GroupHeader(val group: TabGroup, val tabs: List<TabPreview>) : TabMenuItem() {
        override val key: String get() = groupHeaderKey(group.id)
        override val fullSpan: Boolean get() = true
    }

    data class Tab(val preview: TabPreview, val group: TabGroup?) : TabMenuItem() {
        override val key: String get() = tabItemKey(preview.id)
    }

    data class GroupFiller(val group: TabGroup, val slot: Int) : TabMenuItem() {
        override val key: String get() = groupFillerKey(group.id, slot)
    }

    data class GroupFooter(val group: TabGroup) : TabMenuItem() {
        override val key: String get() = groupFooterKey(group.id)
        override val fullSpan: Boolean get() = true
    }
}

private sealed class TabGroupEditorRequest {
    data class CreateWithNewTab(val isIncognito: Boolean) : TabGroupEditorRequest()
    data class CreateFromTabs(val tabIds: List<String>) : TabGroupEditorRequest()
    data class Edit(val group: TabGroup) : TabGroupEditorRequest()
}

private fun buildRenderList(
    tabs: List<TabPreview>,
    groups: List<TabGroup>,
    columns: Int,
    forceCollapsed: Boolean = false,
    forceExpanded: Boolean = false
): List<TabMenuItem> {
    val hasBothSections = tabs.any { !it.isIncognito } && tabs.any { it.isIncognito }
    val groupsById = groups.associateBy { it.id }
    val result = mutableListOf<TabMenuItem>()
    for (incognitoSection in listOf(false, true)) {
        val sectionTabs = tabs.filter { it.isIncognito == incognitoSection }
        if (sectionTabs.isEmpty()) continue
        if (hasBothSections) result.add(TabMenuItem.SectionHeader(incognitoSection))
        val emitted = mutableSetOf<String>()
        val membersByGroup = sectionTabs.groupBy { it.groupId }
        sectionTabs.forEach { preview ->
            val group = preview.groupId?.let { groupsById[it] }?.takeIf { it.isIncognito == incognitoSection }
            if (group == null) {
                result.add(TabMenuItem.Tab(preview, null))
            } else if (emitted.add(group.id)) {
                val members = membersByGroup[group.id].orEmpty()
                val collapsed = forceCollapsed || (group.collapsed && !forceExpanded)
                result.add(TabMenuItem.GroupHeader(if (collapsed != group.collapsed) group.copy(collapsed = collapsed) else group, members))
                if (!collapsed) {
                    members.forEach { result.add(TabMenuItem.Tab(it, group)) }
                    val remainder = members.size % columns
                    if (remainder != 0) repeat(columns - remainder) { slot -> result.add(TabMenuItem.GroupFiller(group, slot)) }
                    result.add(TabMenuItem.GroupFooter(group))
                }
            }
        }
    }
    return result
}

private fun visibleGroups(tabs: List<TabPreview>, groups: List<TabGroup>): List<TabGroup> {
    val used = tabs.mapNotNull { it.groupId }.toSet()
    return groups.filter { it.id in used }.sortedBy { if (it.isIncognito) 1 else 0 }
}

private fun TabPreview.matchesQuery(query: String): Boolean =
    title.contains(query, ignoreCase = true) || url.contains(query, ignoreCase = true)

private fun normalizedTabUrl(url: String): String = url.substringBefore('#').trimEnd('/')

private fun duplicateTabIds(tabs: List<TabPreview>, activeTabId: String?): List<String> =
    tabs.filter { normalizedTabUrl(it.url).isNotEmpty() }
        .groupBy { Triple(it.isIncognito, it.profileId, normalizedTabUrl(it.url)) }
        .values
        .filter { it.size > 1 }
        .flatMap { copies ->
            val keeper = copies.firstOrNull { it.id == activeTabId } ?: copies.first()
            copies.filter { it.id != keeper.id }.map { it.id }
        }

private fun canMergeTabs(dragged: TabPreview, target: TabPreview): Boolean =
    dragged.id != target.id && dragged.isIncognito == target.isIncognito &&
        (target.groupId == null || target.groupId != dragged.groupId)

private fun suggestGroupName(urls: List<String>): String {
    val hosts = urls.mapNotNull { url ->
        runCatching { android.net.Uri.parse(url).host }.getOrNull()?.removePrefix("www.")?.takeIf { it.isNotBlank() }
    }
    if (hosts.isEmpty() || hosts.size != urls.size || hosts.distinct().size != 1) return ""
    return hosts.first().take(TAB_GROUP_NAME_MAX_LENGTH)
}

@Composable
fun TabMenuScreen(activity: MainActivity, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val density = LocalDensity.current
    val tabManager = activity.tabManager
    val uiState = remember { TabMenuUiState() }
    val tabs = remember { mutableStateListOf<TabPreview>().apply { addAll(tabManager.previews()) } }
    val groups = remember { mutableStateListOf<TabGroup>().apply { addAll(tabManager.groups) } }
    var activeTabId by remember { mutableStateOf(tabManager.activeTab?.id) }
    val prefs = remember { androidx.preference.PreferenceManager.getDefaultSharedPreferences(activity) }
    val hideStatusBar = remember { prefs.getBoolean("hide_status_bar", false) }
    val hideSystemNavigation = remember { prefs.getBoolean("hide_system_navigation", false) }
    var showGroupBar by remember { mutableStateOf(prefs.getBoolean(TabGroupPrefs.PREF_SHOW_GROUP_BAR, true)) }
    var editorRequest by remember { mutableStateOf<TabGroupEditorRequest?>(null) }
    var showMovePicker by remember { mutableStateOf(false) }
    var scrollToGroupId by remember { mutableStateOf<String?>(null) }
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var listView by remember { mutableStateOf(prefs.getBoolean(PREF_TAB_MENU_LIST_VIEW, false)) }
    val hasDuplicates by remember { derivedStateOf { duplicateTabIds(tabs, activeTabId).isNotEmpty() } }

    fun closeSearch() {
        searchActive = false
        searchQuery = ""
    }

    fun syncFromManager() {
        tabs.clear(); tabs.addAll(tabManager.previews())
        groups.clear(); groups.addAll(tabManager.groups)
        activeTabId = tabManager.activeTab?.id
        uiState.retainExisting(tabs.map { it.id })
        if (uiState.selectionMode && uiState.selectedIds.isEmpty() && tabs.isEmpty()) uiState.exitSelectionMode()
    }

    fun commitGroups(change: TabManager.() -> Unit) {
        tabManager.change()
        activity.onTabGroupsChanged()
        syncFromManager()
    }

    val focusGroupId = remember {
        val requested = activity.uiState.tabMenuFocusGroupId
        activity.uiState.tabMenuFocusGroupId = null
        if (requested != null && tabManager.group(requested)?.collapsed == true) {
            tabManager.updateGroup(requested) { it.copy(collapsed = false) }
            groups.clear(); groups.addAll(tabManager.groups)
            activity.saveTabs()
        }
        requested
    }

    LaunchedEffect(activity.uiState.tabsRevision) { syncFromManager() }

    fun openTab(tabId: String) {
        val idx = tabManager.tabs.indexOfFirst { it.id == tabId }
        if (idx < 0) return
        activity.onTabSelected(idx)
        onDismiss()
    }

    fun closeTabs(ids: List<String>, message: String? = null) {
        if (ids.isEmpty()) return
        val closingAll = ids.toSet().containsAll(tabs.map { it.id })
        activity.closeTabsWithUndo(
            ids,
            message ?: activity.resources.getQuantityString(R.plurals.tab_menu_closed_tabs, ids.size, ids.size)
        )
        if (closingAll) { onDismiss(); return }
        syncFromManager()
    }

    fun toggleCollapsed(group: TabGroup) {
        commitGroups { updateGroup(group.id) { it.copy(collapsed = !it.collapsed) } }
    }

    fun revealGroup(groupId: String) {
        if (tabManager.group(groupId)?.collapsed == true) {
            commitGroups { updateGroup(groupId) { it.copy(collapsed = false) } }
        }
        scrollToGroupId = groupId
    }

    fun setAllCollapsed(collapsed: Boolean) {
        val groupIds = groups.map { it.id }
        commitGroups { groupIds.forEach { id -> updateGroup(id) { it.copy(collapsed = collapsed) } } }
    }

    BackHandler {
        when {
            uiState.selectionMode -> uiState.exitSelectionMode()
            searchActive -> closeSearch()
            else -> onDismiss()
        }
    }

    var showProfilePicker by remember { mutableStateOf(false) }
    if (showProfilePicker) {
        com.jhaiian.clint.profiles.ProfilePickerDialog(
            profiles = remember { com.jhaiian.clint.profiles.ProfileRepository.all(activity) },
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onPick = { id -> showProfilePicker = false; activity.onNewTabInProfile(id); onDismiss() },
            onManage = {
                showProfilePicker = false
                activity.startActivity(android.content.Intent(activity, com.jhaiian.clint.profiles.ProfilesActivity::class.java))
                onDismiss()
            },
            onDismiss = { showProfilePicker = false }
        )
    }

    editorRequest?.let { request ->
        val usedColors = groups.map { it.color }
        val initialName = when (request) {
            is TabGroupEditorRequest.Edit -> request.group.name
            is TabGroupEditorRequest.CreateFromTabs -> remember(request) {
                suggestGroupName(request.tabIds.mapNotNull { id -> tabs.firstOrNull { it.id == id }?.url?.takeIf { url -> url.isNotEmpty() } })
            }
            is TabGroupEditorRequest.CreateWithNewTab -> ""
        }
        val initialColor = when (request) {
            is TabGroupEditorRequest.Edit -> request.group.color
            else -> TabGroupColor.next(usedColors)
        }
        TabGroupEditorDialog(
            title = stringResource(if (request is TabGroupEditorRequest.Edit) R.string.tab_group_edit_title else R.string.tab_group_new),
            positiveLabel = stringResource(if (request is TabGroupEditorRequest.Edit) R.string.action_save else R.string.tab_group_create),
            initialName = initialName,
            initialColor = initialColor,
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onDismiss = { editorRequest = null },
            onConfirm = { name, color ->
                editorRequest = null
                when (request) {
                    is TabGroupEditorRequest.Edit -> commitGroups { updateGroup(request.group.id) { it.copy(name = name, color = color) } }
                    is TabGroupEditorRequest.CreateFromTabs -> {
                        var createdId: String? = null
                        commitGroups { createdId = createGroup(name, color, request.tabIds)?.id }
                        uiState.exitSelectionMode()
                        createdId?.let { scrollToGroupId = it }
                    }
                    is TabGroupEditorRequest.CreateWithNewTab -> {
                        activity.createTabGroupWithNewTab(name, color, request.isIncognito)
                        onDismiss()
                    }
                }
            }
        )
    }

    if (showMovePicker) {
        val selected = uiState.selectedIds.toList()
        val selectedModes = tabs.filter { it.id in selected }.map { it.isIncognito }.toSet()
        val candidates = visibleGroups(tabs, groups).filter { it.isIncognito in selectedModes }
        TabGroupPickerDialog(
            groups = candidates,
            counts = tabs.groupingBy { it.groupId ?: "" }.eachCount(),
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onPick = { groupId ->
                showMovePicker = false
                commitGroups { addToGroup(selected, groupId) }
                uiState.exitSelectionMode()
                scrollToGroupId = groupId
            },
            onNewGroup = {
                showMovePicker = false
                editorRequest = TabGroupEditorRequest.CreateFromTabs(selected)
            },
            onDismiss = { showMovePicker = false }
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.surface).statusBarsPadding().navigationBarsPadding()) {
        val columns = ((maxWidth - GridEdgePadding * 2) / if (listView) ListCellMinWidth else CellMinWidth).toInt().coerceAtLeast(1)
        val initialIndex = remember {
            val renderList = buildRenderList(tabs, groups, columns)
            val activeGroupId = tabs.firstOrNull { it.id == activeTabId }?.groupId
            val focusIndex = focusGroupId?.let { id -> renderList.indexOfFirst { it.key == groupHeaderKey(id) } } ?: -1
            val activeIndex = renderList.indexOfFirst { it is TabMenuItem.Tab && it.preview.id == activeTabId }
            val activeGroupIndex = activeGroupId?.let { id -> renderList.indexOfFirst { it.key == groupHeaderKey(id) } } ?: -1
            when {
                focusIndex >= 0 -> focusIndex
                activeIndex >= 0 -> activeIndex
                else -> activeGroupIndex.coerceAtLeast(0)
            }
        }
        val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = initialIndex)
        val dragState = remember(gridState) { TabDragState(gridState) }
        val renderList by remember(columns) {
            derivedStateOf {
                val query = searchQuery.trim()
                buildRenderList(
                    if (query.isEmpty()) tabs.toList() else tabs.filter { it.matchesQuery(query) },
                    groups,
                    columns,
                    forceCollapsed = dragState.draggingGroupId != null,
                    forceExpanded = query.isNotEmpty()
                )
            }
        }
        val searching = searchQuery.isNotBlank()
        val dragAllowed = !uiState.selectionMode && !searching
        val draggingPreview = dragState.draggingId?.let { id -> tabs.find { it.id == id } }
        val collapsedGroupIds = groups.filter { it.collapsed }.map { it.id }.toSet()
        SideEffect {
            dragState.collapsedGroupIds = collapsedGroupIds
            dragState.columns = columns
        }
        val shownGroups = visibleGroups(tabs, groups)
        val groupCounts = tabs.groupingBy { it.groupId ?: "" }.eachCount()
        val activeGroupId = tabs.firstOrNull { it.id == activeTabId }?.groupId

        LaunchedEffect(scrollToGroupId, renderList.size) {
            val target = scrollToGroupId ?: return@LaunchedEffect
            val index = renderList.indexOfFirst { it.key == groupHeaderKey(target) }
            if (index >= 0) gridState.animateScrollToItem(index)
            scrollToGroupId = null
        }

        val haptic = LocalHapticFeedback.current

        fun onDragMoved() {
            val draggedId = dragState.draggingId ?: return
            val dragged = tabs.find { it.id == draggedId } ?: return
            val hoverKey = dragState.hoverKey as? String ?: return
            val others = tabs.filter { it.id != draggedId }
            var targetGroupId: String? = null
            fun sameModeGroup(groupId: String): Boolean =
                groups.any { it.id == groupId && it.isIncognito == dragged.isIncognito }
            val insertAt = when {
                hoverKey == END_DROP_KEY -> others.indexOfLast { it.isIncognito == dragged.isIncognito } + 1
                hoverKey.startsWith("before:") -> {
                    val groupId = hoverKey.removePrefix("before:")
                    if (!sameModeGroup(groupId)) return
                    others.indexOfFirst { it.groupId == groupId }.takeIf { it >= 0 } ?: return
                }
                hoverKey.startsWith("after:") -> {
                    val groupId = hoverKey.removePrefix("after:")
                    if (!sameModeGroup(groupId)) return
                    (others.indexOfLast { it.groupId == groupId }.takeIf { it >= 0 } ?: return) + 1
                }
                hoverKey.startsWith("tabBefore:") || hoverKey.startsWith("tabAfter:") -> {
                    val after = hoverKey.startsWith("tabAfter:")
                    val targetId = hoverKey.substringAfter(':')
                    val target = others.find { it.id == targetId } ?: return
                    if (target.isIncognito != dragged.isIncognito) return
                    targetGroupId = target.groupId
                    others.indexOf(target) + if (after) 1 else 0
                }
                else -> return
            }
            val arranged = others.toMutableList().apply { add(insertAt.coerceIn(0, size), dragged.copy(groupId = targetGroupId)) }
            if (arranged.map { it.id to it.groupId } != tabs.map { it.id to it.groupId }) {
                tabs.clear()
                tabs.addAll(arranged)
            }
        }

        fun commitDrag() {
            val draggedId = dragState.draggingId
            val dropKey = dragState.hoverKey
            dragState.end()
            if (draggedId == null) return
            val dragged = tabs.find { it.id == draggedId }
            val mergeTarget = mergeTargetTabId(dropKey)?.let { id -> tabs.find { it.id == id } }
                ?.takeIf { dragged != null && canMergeTabs(dragged, it) }
            val dropGroupId = dropTargetGroupId(dropKey) ?: mergeTarget?.groupId
            val previousOrder = tabManager.tabs.map { it.id }
            val previousAssignments = tabManager.tabs.associate { it.id to it.groupId }
            val previousGroups = tabManager.groups.toList()
            val orderedIds = tabs.map { it.id }
            val assignments = tabs.associate { it.id to it.groupId }
            var createdGroupId: String? = null
            commitGroups {
                applyArrangement(orderedIds, assignments)
                if (dropGroupId != null) {
                    addToGroup(listOf(draggedId), dropGroupId)
                } else if (mergeTarget != null && dragged != null) {
                    createdGroupId = createGroup(
                        suggestGroupName(listOf(mergeTarget.url, dragged.url).filter { it.isNotEmpty() }),
                        TabGroupColor.next(groups.map { it.color }),
                        listOf(mergeTarget.id, draggedId)
                    )?.id
                }
            }
            val newGroupId = createdGroupId
            if (newGroupId != null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scrollToGroupId = newGroupId
                activity.showClintTwoActionSnackbar(
                    message = activity.getString(R.string.tab_group_created_message),
                    actionLabel = activity.getString(R.string.action_undo),
                    onAction = {
                        commitGroups {
                            previousGroups.forEach { restoreGroup(it) }
                            val currentIds = this.tabs.map { it.id }
                            val currentSet = currentIds.toSet()
                            val previousSet = previousOrder.toSet()
                            applyArrangement(previousOrder.filter { it in currentSet } + currentIds.filter { it !in previousSet }, previousAssignments)
                        }
                    },
                    secondaryActionLabel = activity.getString(R.string.action_edit),
                    onSecondaryAction = { tabManager.group(newGroupId)?.let { editorRequest = TabGroupEditorRequest.Edit(it) } }
                )
            } else if (dropGroupId != null && tabManager.group(dropGroupId)?.collapsed == true) {
                scrollToGroupId = dropGroupId
            }
        }

        fun onGroupDragMoved() {
            val groupId = dragState.draggingGroupId ?: return
            val group = groups.find { it.id == groupId } ?: return
            val hoverKey = dragState.hoverKey as? String ?: return
            val members = tabs.filter { it.groupId == groupId }
            if (members.isEmpty()) return
            val others = tabs.filter { it.groupId != groupId }
            val insertAt = when {
                hoverKey == END_DROP_KEY -> others.indexOfLast { it.isIncognito == group.isIncognito } + 1
                hoverKey.startsWith("before:") -> {
                    val targetId = hoverKey.removePrefix("before:")
                    others.indexOfFirst { it.groupId == targetId && it.isIncognito == group.isIncognito }.takeIf { it >= 0 } ?: return
                }
                hoverKey.startsWith("after:") -> {
                    val targetId = hoverKey.removePrefix("after:")
                    (others.indexOfLast { it.groupId == targetId && it.isIncognito == group.isIncognito }.takeIf { it >= 0 } ?: return) + 1
                }
                hoverKey.startsWith("tabBefore:") || hoverKey.startsWith("tabAfter:") -> {
                    val targetId = hoverKey.substringAfter(':')
                    val target = others.find { it.id == targetId } ?: return
                    if (target.isIncognito != group.isIncognito || target.groupId != null) return
                    others.indexOf(target) + if (hoverKey.startsWith("tabAfter:")) 1 else 0
                }
                else -> return
            }
            val arranged = others.toMutableList().apply { addAll(insertAt.coerceIn(0, size), members) }
            if (arranged.map { it.id } != tabs.map { it.id }) {
                tabs.clear()
                tabs.addAll(arranged)
            }
        }

        fun commitGroupDrag() {
            val groupId = dragState.draggingGroupId
            dragState.end()
            if (groupId == null) return
            val orderedIds = tabs.map { it.id }
            val assignments = tabs.associate { it.id to it.groupId }
            commitGroups { applyArrangement(orderedIds, assignments) }
            scrollToGroupId = groupId
        }

        LaunchedEffect(dragState.draggingGroupId) {
            val groupId = dragState.draggingGroupId ?: return@LaunchedEffect
            val index = renderList.indexOfFirst { it.key == groupHeaderKey(groupId) }
            if (index < 0) return@LaunchedEffect
            gridState.scrollToItem(index)
            val info = gridState.layoutInfo.visibleItemsInfo.find { it.key == groupHeaderKey(groupId) } ?: return@LaunchedEffect
            gridState.scrollBy(info.offset.y - dragState.originOffset.y)
            dragState.drag(Offset.Zero)
        }

        val autoScrollEdgePx = with(density) { 72.dp.toPx() }
        val autoScrollMaxStepPx = with(density) { 20.dp.toPx() }
        LaunchedEffect(dragState.active) {
            if (!dragState.active) return@LaunchedEffect
            while (dragState.active) {
                withFrameNanos { }
                val step = dragState.autoScrollStep(autoScrollEdgePx, autoScrollMaxStepPx)
                if (step != 0f && gridState.scrollBy(step) != 0f) {
                    dragState.drag(Offset.Zero)
                    if (dragState.draggingGroupId != null) onGroupDragMoved() else onDragMoved()
                }
            }
        }

        LaunchedEffect(dragState.hoverKey) {
            val key = dragState.hoverKey as? String ?: return@LaunchedEffect
            val isDropZone = key.startsWith("group:") || key.startsWith("chip:") || key.startsWith("before:") ||
                key.startsWith("after:") || key.startsWith("merge:") || key == END_DROP_KEY
            if (dragState.active && isDropZone) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }

        fun cancelDrag() {
            dragState.end()
            syncFromManager()
        }

        fun Modifier.longPressDrag(
            originKey: String,
            isLifted: () -> Boolean,
            onLift: (Offset) -> Unit,
            onMove: () -> Unit,
            onDrop: () -> Unit,
            onLongPress: () -> Unit
        ): Modifier = onGloballyPositioned { dragState.handleOrigins[originKey] = it.positionInRoot() }
            .pointerInput(originKey) {
                var touchStart = Offset.Zero
                var pending = Offset.Zero
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        touchStart = offset
                        pending = Offset.Zero
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        if (isLifted()) {
                            dragState.drag(amount)
                            onMove()
                        } else {
                            pending += amount
                            if (pending.getDistance() > viewConfiguration.touchSlop) {
                                onLift(touchStart)
                                if (isLifted()) {
                                    dragState.drag(pending)
                                    onMove()
                                }
                            }
                        }
                    },
                    onDragEnd = { if (isLifted()) onDrop() else onLongPress() },
                    onDragCancel = { if (isLifted()) cancelDrag() }
                )
            }

        Column(Modifier.fillMaxSize()) {
            TabMenuTopBar(
                tabCount = tabs.size,
                selectionMode = uiState.selectionMode,
                selectedCount = uiState.selectedIds.size,
                hasGroups = shownGroups.isNotEmpty(),
                allGroupsCollapsed = shownGroups.isNotEmpty() && shownGroups.all { it.collapsed },
                showGroupBar = showGroupBar,
                searchActive = searchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onOpenSearch = { searchActive = true },
                onCloseSearch = { closeSearch() },
                listView = listView,
                onToggleListView = {
                    listView = !listView
                    prefs.edit().putBoolean(PREF_TAB_MENU_LIST_VIEW, listView).apply()
                },
                onCloseDuplicates = if (hasDuplicates) ({
                    val ids = duplicateTabIds(tabs, activeTabId)
                    closeTabs(ids, activity.resources.getQuantityString(R.plurals.tab_menu_closed_duplicates, ids.size, ids.size))
                }) else null,
                onToggleSelectionMode = { uiState.selectionMode = true },
                onSelectAll = { uiState.selectAll(tabs.map { it.id }) },
                onInvertSelection = { uiState.invertSelection(tabs.map { it.id }) },
                onDeleteSelected = { closeTabs(uiState.selectedIds.toList()); uiState.exitSelectionMode() },
                onNewTab = { activity.onNewTab(); onDismiss() },
                onNewIncognitoTab = { activity.onNewIncognitoTab(); onDismiss() },
                onNewTabGroup = { editorRequest = TabGroupEditorRequest.CreateWithNewTab(false) },
                onToggleGroupBar = {
                    showGroupBar = !showGroupBar
                    prefs.edit().putBoolean(TabGroupPrefs.PREF_SHOW_GROUP_BAR, showGroupBar).apply()
                },
                onCollapseAllGroups = { setAllCollapsed(true) },
                onExpandAllGroups = { setAllCollapsed(false) },
                onNewProfileTab = {
                    if (com.jhaiian.clint.profiles.ProfileRepository.all(activity).isEmpty()) {
                        activity.startActivity(android.content.Intent(activity, com.jhaiian.clint.profiles.ProfilesActivity::class.java))
                        onDismiss()
                    } else {
                        showProfilePicker = true
                    }
                },
                onCloseAllTabs = { closeTabs(tabs.map { it.id }) }
            )

            AnimatedVisibility(
                visible = !uiState.selectionMode && !searchActive,
                enter = fadeIn(tween(180)) + expandVertically(spring(dampingRatio = 0.85f)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(150))
            ) {
                Column {
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
                    TabGroupChipsRow(
                        groups = shownGroups,
                        counts = groupCounts,
                        activeGroupId = activeGroupId,
                        hoverKey = dragState.hoverKey,
                        onChipClick = { revealGroup(it.id) },
                        onNewGroup = { editorRequest = TabGroupEditorRequest.CreateWithNewTab(false) },
                        onChipBounds = { id, bounds -> if (bounds == null) dragState.chipBounds.remove(id) else dragState.chipBounds[id] = bounds },
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onGloballyPositioned { dragState.gridOriginInRoot = it.positionInRoot() }
            ) {
                if (tabs.isEmpty()) {
                    Text(stringResource(R.string.no_tabs), color = colors.secondaryText, modifier = Modifier.align(Alignment.Center))
                } else if (renderList.isEmpty()) {
                    Text(
                        stringResource(R.string.tab_menu_search_no_results),
                        color = colors.secondaryText,
                        modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(GridEdgePadding)
                    ) {
                        items(
                            items = renderList,
                            key = { it.key },
                            span = { item -> if (item.fullSpan) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
                            contentType = { it::class }
                        ) { item ->
                            AnimatedGridItem {
                                when (item) {
                                    is TabMenuItem.SectionHeader -> TabMenuSectionHeader(
                                        isIncognitoSection = item.isIncognitoSection,
                                        modifier = Modifier.padding(horizontal = CellPadding)
                                    )
                                    is TabMenuItem.GroupHeader -> {
                                        val group = item.group
                                        val groupTabIds = item.tabs.map { it.id }
                                        val isDraggingGroup = dragState.draggingGroupId == group.id
                                        val pinnableContainer = LocalPinnableContainer.current
                                        DisposableEffect(isDraggingGroup) {
                                            val pinHandle = if (isDraggingGroup) pinnableContainer?.pin() else null
                                            onDispose { pinHandle?.release() }
                                        }
                                        val headerAlpha by animateFloatAsState(if (isDraggingGroup) 0.25f else 1f, label = "groupGhostAlpha")
                                        TabGroupHeader(
                                            group = group,
                                            tabs = item.tabs,
                                            containsActive = groupTabIds.contains(activeTabId),
                                            highlighted = dragState.draggingId != null && dropTargetGroupId(dragState.hoverKey) == group.id,
                                            selectionMode = uiState.selectionMode,
                                            allSelected = groupTabIds.isNotEmpty() && groupTabIds.all { uiState.selectedIds.contains(it) },
                                            onToggleCollapsed = { toggleCollapsed(group) },
                                            onLongPress = { uiState.setSelected(groupTabIds, true) },
                                            onToggleSelection = {
                                                uiState.setSelected(groupTabIds, !groupTabIds.all { uiState.selectedIds.contains(it) })
                                            },
                                            onNewTab = { activity.openNewTabInGroup(group.id); onDismiss() },
                                            onEdit = { editorRequest = TabGroupEditorRequest.Edit(group) },
                                            onSelectTabs = { uiState.setSelected(groupTabIds, true) },
                                            onUngroup = { commitGroups { ungroup(group.id) } },
                                            onCloseGroup = {
                                                closeTabs(
                                                    groupTabIds,
                                                    activity.getString(R.string.tab_group_closed_message, group.name.ifBlank { activity.getString(R.string.tab_group_untitled) })
                                                )
                                            },
                                            modifier = Modifier
                                                .padding(top = 6.dp, bottom = if (group.collapsed) 6.dp else 0.dp)
                                                .alpha(headerAlpha),
                                            dragHandleModifier = if (searching) null else Modifier
                                                .onGloballyPositioned { dragState.handleOrigins[groupHeaderKey(group.id)] = it.positionInRoot() }
                                                .pointerInput(group.id) {
                                                    detectDragGestures(
                                                        onDragStart = { offset -> dragState.startGroup(group.id, offset) },
                                                        onDrag = { change, amount -> change.consume(); dragState.drag(amount); onGroupDragMoved() },
                                                        onDragEnd = { commitGroupDrag() },
                                                        onDragCancel = { cancelDrag() }
                                                    )
                                                },
                                            longPressDragModifier = if (dragAllowed) {
                                                val groupId = group.id
                                                Modifier.longPressDrag(
                                                    originKey = groupCardOriginKey(groupId),
                                                    isLifted = { dragState.draggingGroupId == groupId },
                                                    onLift = { offset -> dragState.startGroup(groupId, offset, groupCardOriginKey(groupId)) },
                                                    onMove = { onGroupDragMoved() },
                                                    onDrop = { commitGroupDrag() },
                                                    onLongPress = { uiState.setSelected(tabs.filter { it.groupId == groupId }.map { it.id }, true) }
                                                )
                                            } else {
                                                null
                                            }
                                        )
                                    }
                                    is TabMenuItem.GroupFiller -> Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .background(tabGroupTint(item.group.color, colors.isLight))
                                            .padding(CellPadding)
                                    ) {
                                        Spacer(if (listView) Modifier.fillMaxWidth().height(TabListRowHeight) else Modifier.fillMaxWidth().aspectRatio(0.76f))
                                    }
                                    is TabMenuItem.GroupFooter -> Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 6.dp)
                                            .height(10.dp)
                                            .background(
                                                tabGroupTint(item.group.color, colors.isLight),
                                                RoundedCornerShape(bottomStart = GroupCorner, bottomEnd = GroupCorner)
                                            )
                                    )
                                    is TabMenuItem.Tab -> {
                                        val preview = item.preview
                                        val isDragging = dragState.draggingId == preview.id
                                        val pinnableContainer = LocalPinnableContainer.current
                                        DisposableEffect(isDragging) {
                                            val pinHandle = if (isDragging) pinnableContainer?.pin() else null
                                            onDispose { pinHandle?.release() }
                                        }
                                        val ghostAlpha by animateFloatAsState(if (isDragging) 0.25f else 1f, label = "ghostAlpha")
                                        val mergeHighlighted = draggingPreview != null &&
                                            mergeTargetTabId(dragState.hoverKey) == preview.id &&
                                            canMergeTabs(draggingPreview, preview)
                                        val handleModifier = Modifier
                                            .onGloballyPositioned { dragState.handleOrigins[preview.id] = it.positionInRoot() }
                                            .pointerInput(preview.id) {
                                                detectDragGestures(
                                                    onDragStart = { offset -> dragState.start(preview.id, offset) },
                                                    onDrag = { change, amount -> change.consume(); dragState.drag(amount); onDragMoved() },
                                                    onDragEnd = { commitDrag() },
                                                    onDragCancel = { cancelDrag() }
                                                )
                                            }
                                        val cardDragModifier = if (dragAllowed) {
                                            val tabId = preview.id
                                            Modifier.longPressDrag(
                                                originKey = tabCardOriginKey(tabId),
                                                isLifted = { dragState.draggingId == tabId },
                                                onLift = { offset -> dragState.start(tabId, offset, tabCardOriginKey(tabId)) },
                                                onMove = { onDragMoved() },
                                                onDrop = { commitDrag() },
                                                onLongPress = { uiState.enterSelectionMode(tabId) }
                                            )
                                        } else {
                                            null
                                        }
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .then(item.group?.let { Modifier.background(tabGroupTint(it.color, colors.isLight)) } ?: Modifier)
                                                .padding(CellPadding)
                                        ) {
                                            SwipeToCloseContainer(
                                                enabled = !uiState.selectionMode && !dragState.active,
                                                onClose = { closeTabs(listOf(preview.id)) }
                                            ) {
                                                if (listView) {
                                                    TabMenuListRow(
                                                        preview = preview,
                                                        isActive = preview.id == activeTabId,
                                                        selectionMode = uiState.selectionMode,
                                                        selected = uiState.selectedIds.contains(preview.id),
                                                        dragHandleModifier = handleModifier,
                                                        onOpen = { openTab(preview.id) },
                                                        onEnterSelection = { uiState.enterSelectionMode(preview.id) },
                                                        onToggleSelect = { uiState.toggleSelected(preview.id) },
                                                        onClose = { closeTabs(listOf(preview.id)) },
                                                        onToggleMute = { activity.toggleTabMute(preview.id) },
                                                        mergeHighlighted = mergeHighlighted,
                                                        showDragHandle = !searching,
                                                        longPressDragModifier = cardDragModifier,
                                                        modifier = Modifier.alpha(ghostAlpha)
                                                    )
                                                } else {
                                                    val thumbnail by produceState(TabThumbnailCache.peek(preview.id), preview.id) {
                                                        if (value == null) value = withContext(Dispatchers.IO) { TabThumbnailCache.get(activity, preview.id) }
                                                    }
                                                    TabMenuCard(
                                                        preview = preview,
                                                        isActive = preview.id == activeTabId,
                                                        thumbnail = thumbnail,
                                                        selectionMode = uiState.selectionMode,
                                                        selected = uiState.selectedIds.contains(preview.id),
                                                        dragHandleModifier = handleModifier,
                                                        onOpen = { openTab(preview.id) },
                                                        onEnterSelection = { uiState.enterSelectionMode(preview.id) },
                                                        onToggleSelect = { uiState.toggleSelected(preview.id) },
                                                        onClose = { closeTabs(listOf(preview.id)) },
                                                        onToggleMute = { activity.toggleTabMute(preview.id) },
                                                        mergeHighlighted = mergeHighlighted,
                                                        showDragHandle = !searching,
                                                        longPressDragModifier = cardDragModifier,
                                                        modifier = Modifier.alpha(ghostAlpha)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val draggedPreview = draggingPreview
                    if (draggedPreview != null) {
                        val offsetX = with(density) { (dragState.originOffset.x + dragState.dragOffset.x).toDp() }
                        val offsetY = with(density) { (dragState.originOffset.y + dragState.dragOffset.y).toDp() }
                        val cardWidth = with(density) { dragState.originSize.width.toDp() }
                        val cardHeight = with(density) { dragState.originSize.height.toDp() }
                        val liftScale by animateFloatAsState(1.06f, animationSpec = spring(dampingRatio = 0.5f), label = "dragLiftScale")
                        Box(
                            Modifier
                                .offset(x = offsetX, y = offsetY)
                                .size(cardWidth, cardHeight)
                                .padding(CellPadding)
                                .graphicsLayer { scaleX = liftScale; scaleY = liftScale }
                                .zIndex(10f)
                                .shadow(12.dp, RoundedCornerShape(if (listView) 14.dp else 16.dp))
                        ) {
                            if (listView) {
                                TabMenuListRow(
                                    preview = draggedPreview,
                                    isActive = draggedPreview.id == activeTabId,
                                    selectionMode = false,
                                    selected = false,
                                    dragHandleModifier = Modifier,
                                    onOpen = {},
                                    onEnterSelection = {},
                                    onToggleSelect = {},
                                    onClose = {}
                                )
                            } else {
                                val thumbnail by produceState(TabThumbnailCache.peek(draggedPreview.id), draggedPreview.id) {
                                    if (value == null) value = withContext(Dispatchers.IO) { TabThumbnailCache.get(activity, draggedPreview.id) }
                                }
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

                    val draggedGroup = dragState.draggingGroupId?.let { id -> groups.find { it.id == id } }
                    if (draggedGroup != null) {
                        val members = tabs.filter { it.groupId == draggedGroup.id }
                        val offsetX = with(density) { (dragState.originOffset.x + dragState.dragOffset.x).toDp() }
                        val offsetY = with(density) { (dragState.originOffset.y + dragState.dragOffset.y).toDp() }
                        val headerWidth = with(density) { dragState.originSize.width.toDp() }
                        val liftScale by animateFloatAsState(1.03f, animationSpec = spring(dampingRatio = 0.5f), label = "groupDragLiftScale")
                        Box(
                            Modifier
                                .offset(x = offsetX, y = offsetY)
                                .width(headerWidth)
                                .padding(vertical = 6.dp)
                                .graphicsLayer { scaleX = liftScale; scaleY = liftScale }
                                .zIndex(10f)
                                .shadow(12.dp, RoundedCornerShape(18.dp))
                        ) {
                            TabGroupHeader(
                                group = draggedGroup.copy(collapsed = true),
                                tabs = members,
                                containsActive = members.any { it.id == activeTabId },
                                highlighted = false,
                                selectionMode = false,
                                allSelected = false,
                                onToggleCollapsed = {},
                                onLongPress = {},
                                onToggleSelection = {},
                                onNewTab = {},
                                onEdit = {},
                                onSelectTabs = {},
                                onUngroup = {},
                                onCloseGroup = {},
                                dragHandleModifier = Modifier
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.selectionMode,
                enter = fadeIn(tween(180)) + expandVertically(spring(dampingRatio = 0.85f)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(150))
            ) {
                val selectedIds = uiState.selectedIds.toList()
                TabSelectionActionBar(
                    hasSelection = selectedIds.isNotEmpty(),
                    canUngroup = tabs.any { it.id in selectedIds && it.groupId != null },
                    onGroup = { editorRequest = TabGroupEditorRequest.CreateFromTabs(selectedIds) },
                    onMove = { showMovePicker = true },
                    onUngroup = {
                        commitGroups { removeFromGroup(selectedIds) }
                        uiState.exitSelectionMode()
                    },
                    onClose = {
                        closeTabs(selectedIds)
                        uiState.exitSelectionMode()
                    }
                )
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
    hasGroups: Boolean,
    allGroupsCollapsed: Boolean,
    showGroupBar: Boolean,
    searchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    listView: Boolean,
    onToggleListView: () -> Unit,
    onCloseDuplicates: (() -> Unit)?,
    onToggleSelectionMode: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onNewTabGroup: () -> Unit,
    onToggleGroupBar: () -> Unit,
    onCollapseAllGroups: () -> Unit,
    onExpandAllGroups: () -> Unit,
    onNewProfileTab: () -> Unit,
    onCloseAllTabs: () -> Unit
) {
    val colors = LocalClintColors.current
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (searchActive && !selectionMode) {
            ClintSearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                hint = stringResource(R.string.tab_menu_search_hint),
                onClose = onCloseSearch
            )
        } else {
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
            if (!selectionMode) {
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.tab_menu_search_hint), tint = colors.primary)
                }
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
                            onNewProfileTab = onNewProfileTab,
                            onNewTabGroup = onNewTabGroup,
                            showGroupBar = showGroupBar,
                            onToggleGroupBar = onToggleGroupBar,
                            onCollapseAllGroups = if (hasGroups && !allGroupsCollapsed) onCollapseAllGroups else null,
                            onExpandAllGroups = if (hasGroups && allGroupsCollapsed) onExpandAllGroups else null,
                            onCloseDuplicates = onCloseDuplicates,
                            listView = listView,
                            onToggleListView = onToggleListView
                        )
                    }
                }
            }
        }
    }
}
