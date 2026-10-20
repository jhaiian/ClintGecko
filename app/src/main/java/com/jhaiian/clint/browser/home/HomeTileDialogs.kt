package com.jhaiian.clint.browser.home

import android.graphics.Bitmap
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintDialogActionFooter
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.listscreen.ListMenuItem
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors

internal class HomeMenuEntry(val icon: ImageVector, val label: String, val onClick: () -> Unit)

@Composable
internal fun HomeTileMenu(entries: List<HomeMenuEntry>, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        shape = PopupShape,
        containerColor = colors.popupBackground,
        border = BorderStroke(1.dp, colors.popupStroke)
    ) {
        entries.forEach { entry ->
            ListMenuItem(entry.icon, entry.label, checked = false) {
                onDismiss()
                entry.onClick()
            }
        }
    }
}

internal class HomeEditRequest(val site: HomeSiteTile?, val url: String = "", val title: String = "")

@Composable
internal fun HomeSiteEditDialog(
    activity: MainActivity,
    request: HomeEditRequest,
    iconVersion: Int,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onSave: (url: String, title: String, customIcon: Bitmap?, resetIcon: Boolean) -> Unit
) {
    val colors = LocalClintColors.current
    val site = request.site
    var name by remember(request) { mutableStateOf(site?.title ?: request.title) }
    var address by remember(request) { mutableStateOf(site?.url ?: request.url) }
    var pickedIcon by remember(request) { mutableStateOf<Bitmap?>(null) }
    var resetIcon by remember(request) { mutableStateOf(false) }
    val normalized = normalizeCustomHomepageUrl(address)
    val showError = address.isNotBlank() && normalized == null
    val hasCustom = pickedIcon != null || (site?.customIcon == true && !resetIcon)

    DisposableEffect(Unit) {
        onDispose { activity.pendingShortcutIconCallback = null }
    }

    ClintDialog(
        title = stringResource(if (site == null) R.string.home_tile_add_title else R.string.home_tile_edit_title),
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            ClintDialogActionFooter(
                onCancel = onDismiss,
                positiveLabel = stringResource(if (site == null) R.string.action_add else R.string.action_save),
                onPositive = {
                    normalized?.let { url ->
                        onDismiss()
                        onSave(url, name.trim(), pickedIcon, resetIcon && pickedIcon == null)
                    }
                },
                positiveEnabled = normalized != null
            )
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surfaceVariant)
                    .clickable {
                        activity.pendingShortcutIconCallback = { picked ->
                            if (picked != null) {
                                pickedIcon = picked
                                resetIcon = false
                            }
                        }
                        activity.shortcutIconPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                val picked = pickedIcon
                when {
                    picked != null -> Image(
                        bitmap = picked.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.High,
                        modifier = Modifier.size(64.dp)
                    )
                    site != null && normalized == site.url -> HomeTileVisual(
                        tile = site.copy(title = name, customIcon = site.customIcon && !resetIcon),
                        size = 64.dp,
                        corner = 18.dp,
                        background = colors.surfaceVariant,
                        iconVersion = iconVersion
                    )
                    else -> Icon(Icons.Filled.Public, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(28.dp))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.create_shortcut_change_icon),
                        tint = colors.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                ClintOutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.home_tile_name_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
            }
        }
        ClintOutlinedTextField(
            value = address,
            onValueChange = { address = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            label = { Text(stringResource(R.string.home_tile_url_hint)) },
            singleLine = true,
            isError = showError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done)
        )
        if (showError) {
            Text(
                text = stringResource(R.string.home_tile_url_invalid),
                color = colors.colorError,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
        if (hasCustom) {
            TextButton(
                onClick = {
                    pickedIcon = null
                    resetIcon = true
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(stringResource(R.string.home_tile_reset_icon), color = colors.primary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
internal fun HomeFolderDialog(
    folder: HomeFolderTile,
    iconVersion: Int,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onOpen: (HomeSiteTile) -> Unit,
    onReorder: (List<HomeSiteTile>) -> Unit,
    onRename: () -> Unit,
    siteMenu: (HomeSiteTile) -> List<HomeMenuEntry>
) {
    val colors = LocalClintColors.current
    ClintDialog(
        title = folder.name.ifBlank { stringResource(R.string.home_folder_default_name) },
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
                TextButton(onClick = onRename) {
                    Text(stringResource(R.string.home_folder_rename), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                Box(Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_close), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            val columns = (maxWidth / 84.dp).toInt().coerceIn(3, 5)
            HomeTileGrid(
                tiles = folder.sites,
                columns = columns,
                desktop = false,
                iconVersion = iconVersion,
                allowMerge = false,
                showAdd = false,
                onOpen = { tile -> (tile as? HomeSiteTile)?.let(onOpen) },
                onDrop = { order, _, _ -> onReorder(order.filterIsInstance<HomeSiteTile>()) },
                onAdd = {},
                menu = { tile, dismiss ->
                    (tile as? HomeSiteTile)?.let { HomeTileMenu(siteMenu(it), dismiss) }
                }
            )
        }
    }
}

@Composable
internal fun HomeFolderNameDialog(
    initialName: String,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    ClintDialog(
        title = stringResource(R.string.home_folder_rename),
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            ClintDialogActionFooter(
                onCancel = onDismiss,
                positiveLabel = stringResource(R.string.action_save),
                onPositive = {
                    onDismiss()
                    onSave(name.trim())
                },
                positiveEnabled = name.isNotBlank()
            )
        }
    ) {
        ClintOutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            label = { Text(stringResource(R.string.home_folder_name_hint)) },
            singleLine = true
        )
    }
}

@Composable
internal fun HomeMoveToFolderDialog(
    folders: List<HomeFolderTile>,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onPick: (folderId: String?) -> Unit
) {
    val colors = LocalClintColors.current
    ClintDialog(
        title = stringResource(R.string.home_tile_move_to_folder),
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss
    ) {
        folders.forEach { folder ->
            HomeFolderRow(Icons.Filled.Folder, folder.name.ifBlank { stringResource(R.string.home_folder_default_name) }) {
                onDismiss()
                onPick(folder.id)
            }
        }
        HomeFolderRow(Icons.Filled.CreateNewFolder, stringResource(R.string.home_folder_new), colors.primary) {
            onDismiss()
            onPick(null)
        }
    }
}

@Composable
private fun HomeFolderRow(
    icon: ImageVector,
    label: String,
    textColor: androidx.compose.ui.graphics.Color = LocalClintColors.current.onSurface,
    onClick: () -> Unit
) {
    val colors = LocalClintColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
