package com.jhaiian.clint.profiles

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.RowDivider
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintDialogActionFooter
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.listscreen.ListMenuItem
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
internal fun ProfileAvatar(name: String, color: Int, sizeDp: Int) {
    Box(
        Modifier.size(sizeDp.dp).clip(CircleShape).background(Color(color)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.trim().firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontSize = (sizeDp * 0.45f).sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ProfilesScreen(
    profiles: List<BrowserProfile>,
    supported: Boolean,
    maxContentWidth: Dp?,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onExit: () -> Unit,
    onCreate: (String, Int) -> Unit,
    onUpdate: (String, String, Int) -> Unit,
    onClearData: (BrowserProfile) -> Unit,
    onDelete: (BrowserProfile) -> ProfileDeleteResult
) {
    val colors = LocalClintColors.current
    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BrowserProfile?>(null) }
    var clearing by remember { mutableStateOf<BrowserProfile?>(null) }
    var deleting by remember { mutableStateOf<BrowserProfile?>(null) }
    var inUse by remember { mutableStateOf<BrowserProfile?>(null) }

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Surface(color = colors.surface, shadowElevation = 4.dp, modifier = Modifier.statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().height(56.dp).padding(start = 4.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.onSurface)
                    }
                    Text(
                        text = stringResource(R.string.profiles_title),
                        color = colors.onSurface,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                }
            }
            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Box(Modifier.weight(1f).fillMaxWidth()) {
                AdaptiveWidthContainer(maxContentWidth) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        if (!supported) {
                            SettingsSection(colors.cardBackground, bottomSpacing = 12.dp) {
                                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Filled.Warning, contentDescription = null, tint = colors.colorError, modifier = Modifier.size(24.dp))
                                    Column(Modifier.padding(start = 16.dp)) {
                                        Text(
                                            stringResource(R.string.profiles_unsupported_title),
                                            color = colors.onSurface,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            stringResource(R.string.profiles_unsupported_message),
                                            color = colors.secondaryText,
                                            fontSize = 14.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            stringResource(R.string.profiles_info),
                            color = colors.secondaryText,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp)
                        )
                        SettingsSection(colors.cardBackground, bottomSpacing = 12.dp) {
                            ProfileRow(
                                name = stringResource(R.string.profiles_default_name),
                                summary = stringResource(R.string.profiles_default_summary),
                                color = ProfileRepository.defaultColor,
                                menu = null
                            )
                            profiles.forEach { profile ->
                                RowDivider(colors.divider)
                                ProfileRow(
                                    name = profile.name,
                                    summary = null,
                                    color = profile.color,
                                    onClick = { editing = profile },
                                    menu = {
                                        ProfileMenu(
                                            onEdit = { editing = profile },
                                            onClear = { clearing = profile },
                                            onDelete = { deleting = profile }
                                        )
                                    }
                                )
                            }
                            RowDivider(colors.divider)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .alpha(if (supported) 1f else 0.45f)
                                    .clickable(enabled = supported) { showCreate = true }
                                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(40.dp).clip(CircleShape).background(colors.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = colors.primary)
                                }
                                Text(
                                    stringResource(R.string.profiles_add),
                                    color = colors.primary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                    }
                }
            }
        }
    }

    if (showCreate) {
        ProfileEditorDialog(
            title = stringResource(R.string.profiles_dialog_create_title),
            positiveLabel = stringResource(R.string.profiles_action_create),
            initialName = "",
            initialColor = ProfileRepository.palette[profiles.size % ProfileRepository.palette.size],
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onDismiss = { showCreate = false },
            onConfirm = { name, color -> showCreate = false; onCreate(name, color) }
        )
    }

    editing?.let { target ->
        ProfileEditorDialog(
            title = stringResource(R.string.profiles_dialog_edit_title),
            positiveLabel = stringResource(R.string.action_save),
            initialName = target.name,
            initialColor = target.color,
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onDismiss = { editing = null },
            onConfirm = { name, color -> editing = null; onUpdate(target.id, name, color) }
        )
    }

    clearing?.let { target ->
        ProfileMessageDialog(
            title = stringResource(R.string.profiles_clear_title),
            message = stringResource(R.string.profiles_clear_message, target.name),
            positiveLabel = stringResource(R.string.profiles_menu_clear_data),
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onDismiss = { clearing = null },
            onConfirm = { clearing = null; onClearData(target) }
        )
    }

    deleting?.let { target ->
        ProfileMessageDialog(
            title = stringResource(R.string.profiles_delete_title),
            message = stringResource(R.string.profiles_delete_message, target.name),
            positiveLabel = stringResource(R.string.action_delete),
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onDismiss = { deleting = null },
            onConfirm = {
                deleting = null
                if (onDelete(target) == ProfileDeleteResult.IN_USE) inUse = target
            }
        )
    }

    inUse?.let { target ->
        ProfileMessageDialog(
            title = stringResource(R.string.profiles_in_use_title),
            message = stringResource(R.string.profiles_in_use_message, target.name),
            positiveLabel = null,
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onDismiss = { inUse = null },
            onConfirm = { inUse = null }
        )
    }
}

@Composable
private fun ProfileRow(
    name: String,
    summary: String?,
    color: Int,
    onClick: (() -> Unit)? = null,
    menu: (@Composable () -> Unit)?
) {
    val colors = LocalClintColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(start = 16.dp, end = if (menu != null) 4.dp else 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileAvatar(name, color, 40)
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(
                name,
                color = colors.onSurface,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (summary != null) {
                Text(summary, color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        menu?.invoke()
    }
}

@Composable
private fun ProfileMenu(onEdit: () -> Unit, onClear: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalClintColors.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = null, tint = colors.iconTint)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = PopupShape,
            containerColor = colors.popupBackground,
            border = BorderStroke(1.dp, colors.popupStroke)
        ) {
            ListMenuItem(Icons.Filled.Edit, stringResource(R.string.action_edit), checked = false) {
                expanded = false; onEdit()
            }
            ListMenuItem(Icons.Filled.CleaningServices, stringResource(R.string.profiles_menu_clear_data), checked = false) {
                expanded = false; onClear()
            }
            ListMenuItem(Icons.Filled.Delete, stringResource(R.string.action_delete), checked = false) {
                expanded = false; onDelete()
            }
        }
    }
}

@Composable
private fun ProfileEditorDialog(
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
                onPositive = { onConfirm(name.trim(), color) },
                positiveEnabled = name.isNotBlank()
            )
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            ClintOutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 30) name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.profiles_name_hint)) },
                singleLine = true
            )
            Text(
                stringResource(R.string.profiles_color_label),
                color = colors.secondaryText,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 10.dp)
            )
            ProfileRepository.palette.chunked(4).forEach { rowColors ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    rowColors.forEach { swatch ->
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(swatch))
                                .clickable { color = swatch },
                            contentAlignment = Alignment.Center
                        ) {
                            if (swatch == color) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMessageDialog(
    title: String,
    message: String,
    positiveLabel: String?,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = LocalClintColors.current
    ClintDialog(
        title = title,
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            if (positiveLabel != null) {
                ClintDialogActionFooter(onCancel = onDismiss, positiveLabel = positiveLabel, onPositive = onConfirm)
            } else {
                Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onConfirm) {
                        Text(stringResource(R.string.action_ok), color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    ) {
        Text(
            message,
            color = colors.secondaryText,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
