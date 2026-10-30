package com.jhaiian.clint.downloads.mega

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jhaiian.clint.R
import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.downloads.DownloadStatus
import com.jhaiian.clint.downloads.MegaDownloadEntry
import com.jhaiian.clint.downloads.resolveStorageInfoText
import com.jhaiian.clint.downloads.uriToDisplayPath
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.settings.common.dialogSectionBackground
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import com.jhaiian.clint.ui.ClintCheckbox
import com.jhaiian.clint.ui.ClintDialogStatusBarEffect
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatFileSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible

internal class MegaFolderSubmission(
    val entries: List<MegaDownloadEntry>,
    val skipped: Int,
    val locationMode: String,
    val customLocationUri: String?,
    val unmeteredOnly: Boolean,
    val startPaused: Boolean = false
)

private sealed interface MegaDialogState {
    data object Loading : MegaDialogState
    class Failed(val message: String) : MegaDialogState
    class Ready(val folder: MegaFolder, val rows: List<MegaTreeRow>) : MegaDialogState
}

private class MegaTreeRow(val node: MegaNode, val depth: Int, val path: String, val files: List<MegaNode>)

private const val MAX_INDENT_DEPTH = 6

private fun buildRows(folder: MegaFolder): List<MegaTreeRow> {
    val children = folder.nodes.groupBy { it.parent }.mapValues { (_, list) ->
        list.sortedWith(compareBy<MegaNode>({ !it.isFolder }, { it.name.lowercase() }))
    }
    val focus = folder.link.focusHandle?.let { handle -> folder.nodes.firstOrNull { it.handle == handle } } ?: folder.root
    fun descendants(node: MegaNode): List<MegaNode> {
        if (!node.isFolder) return listOf(node)
        val result = ArrayList<MegaNode>()
        val stack = ArrayDeque(listOf(node))
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            for (child in children[current.handle].orEmpty()) {
                if (child.isFolder) stack.addLast(child) else result.add(child)
            }
        }
        return result
    }
    val rows = ArrayList<MegaTreeRow>()
    fun visit(node: MegaNode, depth: Int, parentPath: String) {
        rows.add(MegaTreeRow(node, depth, parentPath, descendants(node)))
        if (!node.isFolder) return
        val childPath = if (parentPath.isEmpty()) node.name else "$parentPath/${node.name}"
        for (child in children[node.handle].orEmpty()) visit(child, depth + 1, childPath)
    }
    visit(focus, 0, "")
    return rows
}

@Composable
internal fun MegaDownloadDialog(
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    link: MegaLink,
    userAgent: String,
    initialLocationMode: String,
    initialCustomUri: Uri?,
    initialUnmeteredOnly: Boolean,
    onLaunchFolderPicker: (onPicked: (Uri) -> Unit) -> Unit,
    onFileResolved: (MegaFileInfo) -> Unit,
    onDownloadFolder: (MegaFolderSubmission) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalClintColors.current
    val prefs = remember { androidx.preference.PreferenceManager.getDefaultSharedPreferences(context) }
    var state by remember { mutableStateOf<MegaDialogState>(MegaDialogState.Loading) }
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(link, attempt) {
        state = MegaDialogState.Loading
        state = try {
            when (link) {
                is MegaLink.File -> {
                    val info = runInterruptible(Dispatchers.IO) { MegaApi.fileInfo(link, userAgent) }
                    onFileResolved(info)
                    return@LaunchedEffect
                }
                is MegaLink.Folder -> {
                    val folder = runInterruptible(Dispatchers.IO) { MegaApi.folder(link, userAgent) }
                    MegaDialogState.Ready(folder, buildRows(folder))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            MegaDialogState.Failed(MegaErrors.message(context, e))
        }
    }

    var locationMode by remember { mutableStateOf(initialLocationMode) }
    var customUri by remember { mutableStateOf(initialCustomUri) }
    var locationMenuOpen by remember { mutableStateOf(false) }
    var keepStructure by remember { mutableStateOf(true) }
    var skipExisting by remember { mutableStateOf(true) }
    var unmeteredOnly by remember { mutableStateOf(initialUnmeteredOnly) }
    val ready = state as? MegaDialogState.Ready
    val allFiles = remember(ready) { ready?.rows?.firstOrNull()?.files.orEmpty() }
    var selected by remember(ready) { mutableStateOf(allFiles.mapTo(HashSet()) { it.handle }.toSet()) }
    val storageInfoText = remember(locationMode, customUri) { resolveStorageInfoText(context, locationMode, customUri) }

    fun pickCustomFolder() {
        onLaunchFolderPicker { uri ->
            prefs.edit().putString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, uri.toString()).apply()
            customUri = uri
        }
    }

    fun submit(startPaused: Boolean = false) {
        val current = ready ?: return
        val existing = if (skipExisting) {
            ClintDownloadManager.downloadsFlow.value
                .filter { it.status != DownloadStatus.FAILED }
                .mapNotNullTo(HashSet()) { it.mega?.handle }
        } else emptySet()
        val chosen = current.rows.filter { !it.node.isFolder && it.node.handle in selected }
        val entries = chosen.filter { it.node.handle !in existing }.map { row ->
            MegaDownloadEntry(
                source = MegaSource(
                    link = current.folder.link.url,
                    handle = row.node.handle,
                    key = MegaCrypto.encodeBase64(row.node.key),
                    folder = current.folder.link.handle,
                    path = if (keepStructure) row.path else ""
                ),
                filename = row.node.name,
                size = row.node.size
            )
        }
        onDownloadFolder(
            MegaFolderSubmission(
                entries = entries,
                skipped = chosen.size - entries.size,
                locationMode = locationMode,
                customLocationUri = if (locationMode == DownloadSettingsKeys.MODE_CUSTOM) customUri?.toString() else null,
                unmeteredOnly = unmeteredOnly,
                startPaused = startPaused
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        ClintDialogStatusBarEffect(hideStatusBar, hideSystemNavigation)
        BoxWithConstraints {
            val maxContentHeight = (maxHeight - 140.dp).coerceIn(120.dp, 560.dp)
            Surface(shape = RoundedCornerShape(24.dp), color = colors.popupBackground) {
                Column {
                    Text(
                        if (link is MegaLink.Folder) stringResource(R.string.mega_dialog_title_folder) else stringResource(R.string.mega_dialog_title_file),
                        color = colors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)
                    )
                    when (val current = state) {
                        MegaDialogState.Loading -> Row(
                            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = colors.primary)
                            Text(
                                stringResource(R.string.mega_dialog_loading), color = colors.secondaryText, fontSize = 14.sp,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                        is MegaDialogState.Failed -> Text(
                            current.message, color = colors.onSurface, fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                        is MegaDialogState.Ready -> LazyColumn(
                            Modifier.heightIn(max = maxContentHeight).padding(horizontal = 8.dp)
                        ) {
                            item(key = "summary") {
                                val selectedFiles = allFiles.filter { it.handle in selected }
                                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                    Text(
                                        stringResource(R.string.mega_folder_selected_count, selectedFiles.size, allFiles.size),
                                        color = colors.secondaryText, fontSize = 13.sp
                                    )
                                    Text(
                                        stringResource(
                                            R.string.mega_folder_selected_size,
                                            formatFileSize(selectedFiles.sumOf { it.size }),
                                            formatFileSize(allFiles.sumOf { it.size })
                                        ),
                                        color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                            item(key = "options") {
                                Column(Modifier.padding(horizontal = 6.dp, vertical = 6.dp)) {
                                    SectionLabel(stringResource(R.string.download_dialog_section_location))
                                    SettingsSection(colors.dialogSectionBackground, bottomSpacing = 8.dp) {
                                        Column(Modifier.padding(12.dp)) {
                                            var anchorWidthPx by remember { mutableIntStateOf(0) }
                                            val density = LocalDensity.current
                                            Box(Modifier.onGloballyPositioned { anchorWidthPx = it.size.width }) {
                                                Row(
                                                    Modifier.fillMaxWidth().clickable { locationMenuOpen = true }.padding(vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text(stringResource(R.string.download_location_label), color = colors.secondaryText, fontSize = 11.sp)
                                                        Text(
                                                            if (locationMode == DownloadSettingsKeys.MODE_CUSTOM) stringResource(R.string.download_location_option_custom)
                                                            else stringResource(R.string.download_location_option_default),
                                                            color = colors.onSurface, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp)
                                                        )
                                                    }
                                                    Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = colors.iconTint)
                                                }
                                                DropdownMenu(
                                                    expanded = locationMenuOpen, onDismissRequest = { locationMenuOpen = false },
                                                    modifier = Modifier.width(with(density) { anchorWidthPx.toDp() }),
                                                    shape = PopupShape, containerColor = colors.popupBackground, border = BorderStroke(1.dp, colors.popupStroke)
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.download_location_option_default), color = colors.onSurface) },
                                                        onClick = { locationMode = DownloadSettingsKeys.MODE_DEFAULT; locationMenuOpen = false }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text(stringResource(R.string.download_location_option_custom), color = colors.onSurface) },
                                                        onClick = {
                                                            locationMode = DownloadSettingsKeys.MODE_CUSTOM; locationMenuOpen = false
                                                            pickCustomFolder()
                                                        }
                                                    )
                                                }
                                            }
                                            if (locationMode == DownloadSettingsKeys.MODE_CUSTOM) {
                                                Row(
                                                    Modifier.fillMaxWidth().clickable { pickCustomFolder() }.padding(vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Filled.Folder, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(20.dp))
                                                    Text(
                                                        customUri?.let { uriToDisplayPath(it) } ?: stringResource(R.string.download_location_tap_to_choose),
                                                        color = colors.onSurface, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp)
                                                    )
                                                }
                                            }
                                            if (storageInfoText.isNotEmpty()) {
                                                Text(storageInfoText, color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                                            }
                                        }
                                    }
                                    SectionLabel(stringResource(R.string.download_dialog_section_options))
                                    SettingsSection(colors.dialogSectionBackground, bottomSpacing = 8.dp) {
                                        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                            ToggleRow(
                                                stringResource(R.string.mega_keep_structure_title),
                                                stringResource(R.string.mega_keep_structure_summary),
                                                keepStructure
                                            ) { keepStructure = !keepStructure }
                                            ToggleRow(
                                                stringResource(R.string.mega_skip_existing_title),
                                                stringResource(R.string.mega_skip_existing_summary),
                                                skipExisting
                                            ) { skipExisting = !skipExisting }
                                            ToggleRow(
                                                stringResource(R.string.download_unmetered_only_title),
                                                stringResource(R.string.download_dialog_unmetered_summary),
                                                unmeteredOnly
                                            ) { unmeteredOnly = !unmeteredOnly }
                                        }
                                    }
                                }
                            }
                            if (allFiles.isEmpty()) {
                                item(key = "empty") {
                                    Text(
                                        stringResource(R.string.mega_folder_empty), color = colors.secondaryText, fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            } else {
                                item(key = "select_all") {
                                    val allSelected = selected.size == allFiles.size
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .clickable { selected = if (allSelected) emptySet() else allFiles.mapTo(HashSet()) { it.handle } }
                                            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            stringResource(R.string.mega_select_all), color = colors.onSurface, fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f)
                                        )
                                        ClintCheckbox(
                                            checked = allSelected,
                                            onCheckedChange = { checked -> selected = if (checked) allFiles.mapTo(HashSet()) { it.handle } else emptySet() }
                                        )
                                    }
                                    HorizontalDivider(color = colors.divider)
                                }
                                items(current.rows, key = { it.node.handle }) { row ->
                                    val handles = row.files.map { it.handle }
                                    val checked = handles.isNotEmpty() && handles.all { it in selected }
                                    fun toggle(value: Boolean) {
                                        selected = if (value) selected + handles else selected - handles.toSet()
                                    }
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .clickable(enabled = handles.isNotEmpty()) { toggle(!checked) }
                                            .padding(start = 12.dp + 14.dp * minOf(row.depth, MAX_INDENT_DEPTH), end = 4.dp, top = 2.dp, bottom = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (row.node.isFolder) Icons.Filled.Folder else Icons.Filled.Description,
                                            contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(22.dp)
                                        )
                                        Column(Modifier.weight(1f).padding(start = 12.dp, top = 6.dp, bottom = 6.dp)) {
                                            Text(
                                                row.node.name, color = colors.onSurface, fontSize = 14.sp,
                                                maxLines = 2, overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                if (row.node.isFolder) {
                                                    pluralStringResource(R.plurals.mega_folder_file_count, row.files.size, row.files.size, formatFileSize(row.files.sumOf { it.size }))
                                                } else formatFileSize(row.node.size),
                                                color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                        ClintCheckbox(checked = checked, onCheckedChange = { toggle(it) })
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp, top = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (state is MegaDialogState.Ready) {
                            val addEnabled = selected.isNotEmpty()
                            TextButton(onClick = { submit(startPaused = true) }, enabled = addEnabled) {
                                Text(
                                    stringResource(R.string.action_add),
                                    color = if (addEnabled) colors.primary else colors.secondaryText,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                        }
                        when (state) {
                            is MegaDialogState.Failed -> TextButton(onClick = { attempt++ }) {
                                Text(stringResource(R.string.mega_action_try_again), color = colors.primary, fontWeight = FontWeight.Medium)
                            }
                            is MegaDialogState.Ready -> {
                                val enabled = selected.isNotEmpty()
                                TextButton(onClick = { submit() }, enabled = enabled) {
                                    Text(
                                        stringResource(R.string.action_download),
                                        color = if (enabled) colors.primary else colors.secondaryText,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            MegaDialogState.Loading -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalClintColors.current
    Text(text, color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
}

@Composable
private fun ToggleRow(title: String, summary: String, checked: Boolean, onToggle: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(summary, color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        ClintSwitch(checked = checked)
    }
}
