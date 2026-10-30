package com.jhaiian.clint.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.listscreen.ListMenuItem
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun TabMenuOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onCloseAllTabs: () -> Unit,
    onSelectTabs: () -> Unit,
    showProfileTab: Boolean = false,
    onNewProfileTab: () -> Unit = {},
    onNewTabGroup: (() -> Unit)? = null,
    showGroupBar: Boolean = true,
    onToggleGroupBar: (() -> Unit)? = null,
    onCollapseAllGroups: (() -> Unit)? = null,
    onExpandAllGroups: (() -> Unit)? = null,
    onCloseDuplicates: (() -> Unit)? = null,
    listView: Boolean = false,
    onToggleListView: (() -> Unit)? = null,
    buttonsAtBottom: Boolean = false,
    onToggleButtonsAtBottom: (() -> Unit)? = null,
    tabsAtBottom: Boolean = false,
    onToggleTabsAtBottom: (() -> Unit)? = null
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke)
    ) {
        ListMenuItem(Icons.Filled.Add, stringResource(R.string.new_tab), checked = false) {
            onDismiss(); onNewTab()
        }
        ListMenuItem(Icons.Filled.VisibilityOff, stringResource(R.string.new_incognito_tab), checked = false) {
            onDismiss(); onNewIncognitoTab()
        }
        if (showProfileTab) {
            ListMenuItem(androidx.compose.material.icons.Icons.Filled.AccountCircle, stringResource(R.string.profiles_new_tab_in), checked = false) {
                onDismiss(); onNewProfileTab()
            }
        }
        if (onNewTabGroup != null) {
            ListMenuItem(Icons.Filled.CreateNewFolder, stringResource(R.string.tab_group_new), checked = false) {
                onDismiss(); onNewTabGroup()
            }
        }
        ListMenuItem(Icons.Filled.Close, stringResource(R.string.tab_menu_close_all_tabs), checked = false) {
            onDismiss(); onCloseAllTabs()
        }
        onCloseDuplicates?.let { action ->
            ListMenuItem(Icons.Filled.FilterNone, stringResource(R.string.tab_menu_close_duplicates), checked = false) {
                onDismiss(); action()
            }
        }
        ListMenuItem(Icons.Filled.SelectAll, stringResource(R.string.tab_menu_select_mode_desc), checked = false) {
            onDismiss(); onSelectTabs()
        }
        if (onCollapseAllGroups != null || onExpandAllGroups != null || onToggleGroupBar != null || onToggleListView != null || onToggleButtonsAtBottom != null || onToggleTabsAtBottom != null) {
            HorizontalDivider(color = colors.divider)
        }
        onCollapseAllGroups?.let { action ->
            ListMenuItem(Icons.Filled.ExpandLess, stringResource(R.string.tab_group_collapse_all), checked = false) {
                onDismiss(); action()
            }
        }
        onExpandAllGroups?.let { action ->
            ListMenuItem(Icons.Filled.ExpandMore, stringResource(R.string.tab_group_expand_all), checked = false) {
                onDismiss(); action()
            }
        }
        onToggleGroupBar?.let { action ->
            ListMenuItem(Icons.Filled.Tab, stringResource(R.string.tab_group_bar_show), checked = showGroupBar) {
                onDismiss(); action()
            }
        }
        onToggleListView?.let { action ->
            ListMenuItem(Icons.AutoMirrored.Filled.ViewList, stringResource(R.string.tab_menu_list_view), checked = listView) {
                onDismiss(); action()
            }
        }
        onToggleButtonsAtBottom?.let { action ->
            ListMenuItem(Icons.Filled.VerticalAlignBottom, stringResource(R.string.tab_menu_buttons_at_bottom), checked = buttonsAtBottom) {
                onDismiss(); action()
            }
        }
        onToggleTabsAtBottom?.let { action ->
            ListMenuItem(Icons.Filled.KeyboardDoubleArrowDown, stringResource(R.string.tab_menu_tabs_at_bottom), checked = tabsAtBottom) {
                onDismiss(); action()
            }
        }
    }
}

@Composable
fun TabSelectionOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke)
    ) {
        ListMenuItem(Icons.Filled.SelectAll, stringResource(R.string.tab_menu_select_all), checked = false) {
            onDismiss(); onSelectAll()
        }
        ListMenuItem(Icons.Filled.SwapHoriz, stringResource(R.string.tab_menu_invert_selection), checked = false) {
            onDismiss(); onInvertSelection()
        }
        ListMenuItem(Icons.Filled.Delete, stringResource(R.string.tab_menu_delete_selected), checked = false) {
            onDismiss(); onDeleteSelected()
        }
    }
}
