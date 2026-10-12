package com.jhaiian.clint.ui.listscreen
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.SwapVert

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.theme.LocalClintColors

val PopupCornerRadius = 16.dp
val PopupShape = RoundedCornerShape(PopupCornerRadius)

@Composable
fun SortMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    sortKey: ListSortKey,
    sortOrder: ListSortOrder,
    onSortByTitle: () -> Unit,
    onSortByDateAdded: () -> Unit,
    onSortAscending: () -> Unit,
    onSortDescending: () -> Unit,
    secondarySortLabel: String = stringResource(R.string.bookmarks_sort_by_date_added)
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke)
    ) {
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Abc, stringResource(R.string.history_sort_by_title), sortKey == ListSortKey.TITLE) {
            onDismiss(); onSortByTitle()
        }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.DateRange, secondarySortLabel, sortKey == ListSortKey.DATE_ADDED) {
            onDismiss(); onSortByDateAdded()
        }
        HorizontalDivider(color = colors.divider)
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.ArrowUpward, stringResource(R.string.history_sort_ascending), sortOrder == ListSortOrder.ASCENDING) {
            onDismiss(); onSortAscending()
        }
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.ArrowDownward, stringResource(R.string.history_sort_descending), sortOrder == ListSortOrder.DESCENDING) {
            onDismiss(); onSortDescending()
        }
    }
}

@Composable
fun SelectionOptionsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onDeselectAll: () -> Unit
) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke)
    ) {
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Check, stringResource(R.string.history_select_all), checked = false) {
            onDismiss(); onSelectAll()
        }
        HorizontalDivider(color = colors.divider)
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.SwapVert, stringResource(R.string.history_invert_selection), checked = false) {
            onDismiss(); onInvertSelection()
        }
        HorizontalDivider(color = colors.divider)
        ListMenuItem(androidx.compose.material.icons.Icons.Filled.Close, stringResource(R.string.history_deselect_all), checked = false) {
            onDismiss(); onDeselectAll()
        }
    }
}

@Composable
fun ListMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, checked: Boolean, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    DropdownMenuItem(
        text = { Text(label, color = colors.popupText, fontSize = 14.sp) },
        leadingIcon = {
            Icon(
                icon, contentDescription = null,
                tint = colors.primary, modifier = Modifier.size(20.dp).alpha(0.85f)
            )
        },
        trailingIcon = if (checked) {
            {
                Icon(
                    androidx.compose.material.icons.Icons.Filled.Check, contentDescription = null,
                    tint = colors.primary, modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        onClick = onClick
    )
}
