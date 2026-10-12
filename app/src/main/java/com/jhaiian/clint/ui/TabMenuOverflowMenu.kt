package com.jhaiian.clint.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
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
    onNewProfileTab: () -> Unit = {}
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
        ListMenuItem(Icons.Filled.Close, stringResource(R.string.tab_menu_close_all_tabs), checked = false) {
            onDismiss(); onCloseAllTabs()
        }
        ListMenuItem(Icons.Filled.SelectAll, stringResource(R.string.tab_menu_select_mode_desc), checked = false) {
            onDismiss(); onSelectTabs()
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
