package com.jhaiian.clint.bookmarks
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Storage

import com.jhaiian.clint.R

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintDialogCancelFooter
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun CreateBookmarkFolderDialog(
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = LocalClintColors.current
    var name by remember { mutableStateOf("") }

    ClintDialog(
        title = stringResource(R.string.bookmarks_new_folder_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                    Text(stringResource(R.string.bookmarks_folder_create), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        ClintOutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.bookmarks_folder_name_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun CreateBookmarkDialog(
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (title: String, url: String) -> Unit
) {
    val colors = LocalClintColors.current
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    ClintDialog(
        title = stringResource(R.string.bookmarks_new_bookmark_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { if (url.isNotBlank()) onConfirm(title.trim(), url.trim()) }, enabled = url.isNotBlank()) {
                    Text(stringResource(R.string.action_add), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Column(Modifier.fillMaxWidth()) {
            ClintOutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.bookmarks_bookmark_title_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            ClintOutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(stringResource(R.string.bookmarks_bookmark_url_hint)) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun RenameBookmarkFolderDialog(
    folder: BookmarkFolder,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = LocalClintColors.current
    var name by remember(folder.id) { mutableStateOf(folder.name) }

    ClintDialog(
        title = stringResource(R.string.bookmarks_rename_folder),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                    Text(stringResource(R.string.action_save), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        ClintOutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.bookmarks_folder_name_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun MoveToFolderDialog(
    tree: List<FolderTreeEntry>,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Long?) -> Unit,
    title: String = stringResource(R.string.bookmarks_move_to_title)
) {
    ClintDialog(
        title = title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = { ClintDialogCancelFooter(onDismiss) }
    ) {
        Column(Modifier.fillMaxWidth()) {
            MoveToFolderRow(name = stringResource(R.string.bookmarks_title), depth = 0) { onSelect(null) }
            tree.forEach { entry ->
                MoveToFolderRow(name = entry.folder.name, depth = entry.depth + 1) { onSelect(entry.folder.id) }
            }
        }
    }
}

@Composable
fun BookmarksFormatPickerDialog(
    title: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onSelectHtml: () -> Unit,
    onSelectSqlite: () -> Unit
) {
    ClintDialog(
        title = title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = { ClintDialogCancelFooter(onDismiss) }
    ) {
        Column(Modifier.fillMaxWidth()) {
            BookmarksFormatRow(
                icon = androidx.compose.material.icons.Icons.Filled.Description,
                name = stringResource(R.string.bookmarks_format_html),
                description = stringResource(R.string.bookmarks_format_html_desc),
                onClick = onSelectHtml
            )
            BookmarksFormatRow(
                icon = androidx.compose.material.icons.Icons.Filled.Storage,
                name = stringResource(R.string.bookmarks_format_sqlite),
                description = stringResource(R.string.bookmarks_format_sqlite_desc),
                onClick = onSelectSqlite
            )
        }
    }
}

@Composable
private fun BookmarksFormatRow(icon: androidx.compose.ui.graphics.vector.ImageVector, name: String, description: String, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(22.dp))
        Column(Modifier.padding(start = 14.dp)) {
            Text(name, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(description, color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun MoveToFolderRow(name: String, depth: Int, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = (16 + depth * 20).dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Icon(androidx.compose.material.icons.Icons.Filled.Folder, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(20.dp))
        Text(
            name, color = colors.onSurface, fontSize = 14.sp,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
