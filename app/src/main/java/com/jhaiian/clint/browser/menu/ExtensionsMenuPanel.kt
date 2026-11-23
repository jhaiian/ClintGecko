package com.jhaiian.clint.browser.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.extensions.ClintExtensions
import com.jhaiian.clint.extensions.ExtensionIconTile
import com.jhaiian.clint.extensions.ExtensionInfo
import com.jhaiian.clint.extensions.ExtensionMenuOrder
import com.jhaiian.clint.extensions.ExtensionStatus
import com.jhaiian.clint.extensions.statusText
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
internal fun ExtensionsChevron(expanded: Boolean) {
    val colors = LocalClintColors.current
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "extensionsChevron")
    Icon(
        imageVector = Icons.Filled.ExpandMore,
        contentDescription = stringResource(if (expanded) R.string.ext_menu_collapse else R.string.ext_menu_expand),
        tint = colors.iconTint,
        modifier = Modifier.size(20.dp).rotate(rotation)
    )
}

@Composable
internal fun ExtensionsMenuPanel(
    expanded: Boolean,
    onExtensionClick: (String, Boolean) -> Unit,
    onExtensionSettings: (String) -> Unit,
    onManage: () -> Unit,
    topIds: Set<String> = emptySet()
) {
    val colors = LocalClintColors.current
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val context = LocalContext.current
        ExtensionMenuOrder.ensureLoaded(context)
        val list = ExtensionMenuOrder.apply(ClintExtensions.extensions.toList(), topIds)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceVariant)
                .animateContentSize()
                .padding(vertical = 4.dp)
        ) {
            if (list.isEmpty()) {
                Text(
                    stringResource(R.string.ext_empty_message),
                    color = colors.secondaryText,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            list.forEach { info ->
                ExtensionPanelRow(info, onExtensionClick, onExtensionSettings)
            }
            ExtensionManageRow(onManage)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExtensionPanelRow(
    info: ExtensionInfo,
    onExtensionClick: (String, Boolean) -> Unit,
    onExtensionSettings: (String) -> Unit
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val on = info.status == ExtensionStatus.ON
    val action = if (on) ClintExtensions.actionFor(info.id) else null
    val actionable = action != null && action.enabled != false
    val subtitle = when {
        !on -> statusText(info.status).orEmpty()
        action != null -> action.title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.ext_quick_has_button)
        else -> stringResource(R.string.ext_quick_no_button)
    }
    val badge = action?.badgeText?.takeIf { it.isNotBlank() }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {
                    ExtensionMenuOrder.markUsed(context, info.id)
                    onExtensionClick(info.id, actionable)
                },
                onLongClick = { onExtensionSettings(info.id) }
            )
            .padding(start = 10.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExtensionIconTile(info, 34.dp, Modifier.alpha(if (on) 1f else 0.5f))
        Column(Modifier.weight(1f).padding(start = 10.dp, end = 4.dp)) {
            Text(
                info.name,
                color = if (on) colors.popupText else colors.secondaryText,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                color = colors.secondaryText,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (badge != null) {
            Text(
                badge,
                color = colors.onPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.primary)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            )
        }
        val pinned = info.id in ClintExtensions.pinnedIds
        val pinLimit = ClintExtensions.pinnedLimit(LocalConfiguration.current.screenWidthDp)
        IconButton(onClick = { ClintExtensions.setPinned(info.id, !pinned, pinLimit) }, modifier = Modifier.size(36.dp)) {
            Icon(
                if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = stringResource(if (pinned) R.string.ext_unpin_search_bar else R.string.ext_pin_search_bar),
                tint = if (pinned) colors.primary else colors.iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = { onExtensionSettings(info.id) }, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = null,
                tint = colors.iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ExtensionManageRow(onManage: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onManage)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Settings,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            stringResource(R.string.ext_quick_manage),
            color = colors.primary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 12.dp).weight(1f)
        )
        ExtensionSortButton()
        val pinned = ClintExtensions.buttonPinned
        IconButton(onClick = { ClintExtensions.setExtensionsButtonPinned(!pinned) }, modifier = Modifier.size(36.dp)) {
            Icon(
                if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = stringResource(if (pinned) R.string.ext_unpin_button_search_bar else R.string.ext_pin_button_search_bar),
                tint = if (pinned) colors.primary else colors.iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ExtensionSortButton() {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    var reorderOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.Sort,
                contentDescription = stringResource(R.string.ext_menu_sort),
                tint = colors.iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = PopupShape,
            containerColor = colors.popupBackground,
            border = BorderStroke(1.dp, colors.popupStroke)
        ) {
            Text(
                stringResource(R.string.ext_menu_sort),
                color = colors.secondaryText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            ExtensionMenuOrder.SORT_MODES.forEach { mode ->
                val selected = ExtensionMenuOrder.sortMode == mode
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(sortLabel(mode)),
                            color = if (selected) colors.primary else colors.popupText,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    trailingIcon = {
                        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    },
                    onClick = {
                        open = false
                        if (mode == ExtensionMenuOrder.SORT_CUSTOM && ExtensionMenuOrder.customOrder.isEmpty()) {
                            reorderOpen = true
                        } else {
                            ExtensionMenuOrder.setSort(context, mode)
                        }
                    }
                )
            }
            HorizontalDivider(color = colors.divider, modifier = Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = { Text(stringResource(R.string.ext_menu_reorder), color = colors.popupText, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Filled.DragHandle, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(18.dp)) },
                onClick = {
                    open = false
                    reorderOpen = true
                }
            )
        }
    }
    if (reorderOpen) ExtensionReorderDialog(onDismiss = { reorderOpen = false })
}

private fun sortLabel(mode: String): Int = when (mode) {
    ExtensionMenuOrder.SORT_NAME -> R.string.ext_menu_sort_name
    ExtensionMenuOrder.SORT_RECENT -> R.string.ext_menu_sort_recent
    ExtensionMenuOrder.SORT_CUSTOM -> R.string.ext_menu_sort_custom
    else -> R.string.ext_menu_sort_enabled
}

@Composable
internal fun ExtensionsSearchBarButton(activity: MainActivity, topIds: Set<String> = emptySet()) {
    val colors = LocalClintColors.current
    var expanded by remember { mutableStateOf(false) }
    val maxMenuHeight = LocalConfiguration.current.screenHeightDp.dp * 0.9f
    val actions = remember(activity) { activity.buildMenuActions(dismiss = { expanded = false }) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(start = 10.dp)
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = { expanded = true })
    ) {
        Icon(
            Icons.Filled.Extension,
            contentDescription = stringResource(R.string.ext_title),
            tint = colors.iconTint,
            modifier = Modifier.size(20.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(PopupMenuWidth).heightIn(max = maxMenuHeight),
            shape = PopupShape,
            containerColor = colors.popupBackground,
            border = BorderStroke(1.dp, colors.popupStroke)
        ) {
            Text(
                stringResource(R.string.ext_title),
                color = colors.popupText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            ExtensionsMenuPanel(
                expanded = true,
                onExtensionClick = actions.onExtensionClick,
                onExtensionSettings = actions.onOpenExtensionSettings,
                onManage = actions.onOpenExtensionsSettings,
                topIds = topIds
            )
        }
    }
}
