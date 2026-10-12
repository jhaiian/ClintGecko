package com.jhaiian.clint.blocker
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Update

import com.jhaiian.clint.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.listscreen.ListMenuItem
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun WebsiteBlockerActionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCheckUpdateActive: () -> Unit,
    onCheckUpdateAll: () -> Unit,
    onForceUpdateActive: () -> Unit,
    onForceUpdateAll: () -> Unit,
    onRecompile: () -> Unit
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded, onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke)
    ) {
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Update, stringResource(R.string.filter_list_check_update_active), false) { onDismiss(); onCheckUpdateActive() }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Update, stringResource(R.string.filter_list_check_update_all), false) { onDismiss(); onCheckUpdateAll() }
        HorizontalDivider(color = colors.divider)
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.RestartAlt, stringResource(R.string.filter_list_force_update_active), false) { onDismiss(); onForceUpdateActive() }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.RestartAlt, stringResource(R.string.filter_list_force_update_all), false) { onDismiss(); onForceUpdateAll() }
        HorizontalDivider(color = colors.divider)
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Cached, stringResource(R.string.website_blocker_menu_recompile), false) { onDismiss(); onRecompile() }
    }
}
