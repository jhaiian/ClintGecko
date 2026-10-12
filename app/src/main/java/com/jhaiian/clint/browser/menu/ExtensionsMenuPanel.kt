package com.jhaiian.clint.browser.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.extensions.ClintExtensions
import com.jhaiian.clint.extensions.ExtensionIconTile
import com.jhaiian.clint.extensions.ExtensionInfo
import com.jhaiian.clint.extensions.ExtensionStatus
import com.jhaiian.clint.extensions.statusText
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
    onManage: () -> Unit
) {
    val colors = LocalClintColors.current
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val list = ClintExtensions.extensions.sortedWith(
            compareByDescending<ExtensionInfo> { it.status == ExtensionStatus.ON }.thenBy { it.name.lowercase() }
        )
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
                onClick = { onExtensionClick(info.id, actionable) },
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
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
