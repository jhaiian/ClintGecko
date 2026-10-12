package com.jhaiian.clint.downloads

import com.jhaiian.clint.R

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.settings.common.dialogSectionBackground
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintDialogActionFooter
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.launch

@Composable
fun DownloadRenameDialog(item: DownloadItem, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalClintColors.current
    val scope = rememberCoroutineScope()

    val dot = item.filename.lastIndexOf('.')
    var filename by remember(item.id) { mutableStateOf(if (dot > 0) item.filename.substring(0, dot) else item.filename) }
    var extension by remember(item.id) { mutableStateOf(if (dot > 0) item.filename.substring(dot + 1) else "") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val resolvedName = if (extension.isNotBlank()) "${filename.trim()}.${extension.trim()}" else filename.trim()
    val canSubmit = !busy && filename.isNotBlank() && resolvedName != item.filename

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ClintDialog(
        title = stringResource(R.string.download_menu_rename),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            ClintDialogActionFooter(
                onCancel = onDismiss,
                positiveLabel = stringResource(R.string.download_rename_positive),
                positiveEnabled = canSubmit,
                onPositive = {
                    busy = true
                    scope.launch {
                        when (ClintDownloadManager.renameFile(context, item.id, resolvedName)) {
                            ClintDownloadManager.RenameResult.SUCCESS -> onDismiss()
                            ClintDownloadManager.RenameResult.EXISTS -> {
                                errorText = context.getString(R.string.download_rename_exists)
                                busy = false
                            }
                            ClintDownloadManager.RenameResult.MISSING -> {
                                android.widget.Toast.makeText(context, R.string.download_file_missing, android.widget.Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                            ClintDownloadManager.RenameResult.FAILED -> {
                                errorText = context.getString(R.string.download_rename_failed)
                                busy = false
                            }
                        }
                    }
                }
            )
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            SettingsSection(colors.dialogSectionBackground) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        ClintOutlinedTextField(
                            value = filename,
                            onValueChange = { filename = it; errorText = null },
                            modifier = Modifier.weight(1f).focusRequester(focusRequester),
                            label = { Text(stringResource(R.string.download_dialog_filename_hint)) },
                            singleLine = true,
                            isError = errorText != null
                        )
                        ClintOutlinedTextField(
                            value = extension,
                            onValueChange = { extension = it; errorText = null },
                            modifier = Modifier.width(96.dp).padding(start = 8.dp),
                            label = { Text(stringResource(R.string.download_dialog_extension_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            isError = errorText != null
                        )
                    }
                    errorText?.let {
                        Text(it, color = colors.colorError, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}
