package com.jhaiian.clint.settings.backuprestore

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AppShortcut
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.backup.AuthGateResult
import com.jhaiian.clint.backup.BackupAuthGate
import com.jhaiian.clint.backup.BackupCategory
import com.jhaiian.clint.backup.BackupManager
import com.jhaiian.clint.backup.RestoreManager
import com.jhaiian.clint.backup.StageResult
import com.jhaiian.clint.backup.UnlockResult
import com.jhaiian.clint.settings.SettingsActivity
import com.jhaiian.clint.settings.common.RowDivider
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.ui.ClintCheckbox
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintDialogCancelFooter
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.ClintRadioButton
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.listscreen.ConfirmDialogHost
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun categoryTitleRes(category: BackupCategory): Int = when (category) {
    BackupCategory.SETTINGS -> R.string.backup_category_settings
    BackupCategory.TABS -> R.string.backup_category_tabs
    BackupCategory.DOWNLOADS -> R.string.backup_category_downloads
    BackupCategory.WEBSITE_BLOCKER -> R.string.backup_category_website_blocker
    BackupCategory.COOKIES -> R.string.backup_category_cookies
    BackupCategory.PROFILES -> R.string.backup_category_profiles
    BackupCategory.BOOKMARKS -> R.string.backup_category_bookmarks
    BackupCategory.SEARCH_HISTORY -> R.string.backup_category_search_history
    BackupCategory.SITE_PERMISSIONS -> R.string.backup_category_site_permissions
    BackupCategory.SHORTCUTS -> R.string.backup_category_shortcuts
    BackupCategory.EXTENSIONS -> R.string.backup_category_extensions
    BackupCategory.UPDATE_SETTINGS -> R.string.backup_category_update_settings
}

private fun categoryDescRes(category: BackupCategory): Int = when (category) {
    BackupCategory.SETTINGS -> R.string.backup_category_settings_desc
    BackupCategory.TABS -> R.string.backup_category_tabs_desc
    BackupCategory.DOWNLOADS -> R.string.backup_category_downloads_desc
    BackupCategory.WEBSITE_BLOCKER -> R.string.backup_category_website_blocker_desc
    BackupCategory.COOKIES -> R.string.backup_category_cookies_desc
    BackupCategory.PROFILES -> R.string.backup_category_profiles_desc
    BackupCategory.BOOKMARKS -> R.string.backup_category_bookmarks_desc
    BackupCategory.SEARCH_HISTORY -> R.string.backup_category_search_history_desc
    BackupCategory.SITE_PERMISSIONS -> R.string.backup_category_site_permissions_desc
    BackupCategory.SHORTCUTS -> R.string.backup_category_shortcuts_desc
    BackupCategory.EXTENSIONS -> R.string.backup_category_extensions_desc
    BackupCategory.UPDATE_SETTINGS -> R.string.backup_category_update_settings_desc
}

private fun categoryIcon(category: BackupCategory): ImageVector = when (category) {
    BackupCategory.SETTINGS -> Icons.Filled.Settings
    BackupCategory.TABS -> Icons.Filled.Tab
    BackupCategory.DOWNLOADS -> Icons.Filled.Download
    BackupCategory.WEBSITE_BLOCKER -> Icons.Filled.Shield
    BackupCategory.COOKIES -> Icons.Filled.Cookie
    BackupCategory.PROFILES -> Icons.Filled.SwitchAccount
    BackupCategory.BOOKMARKS -> Icons.Filled.Bookmark
    BackupCategory.SEARCH_HISTORY -> Icons.Filled.History
    BackupCategory.SITE_PERMISSIONS -> Icons.Filled.PrivacyTip
    BackupCategory.SHORTCUTS -> Icons.Filled.AppShortcut
    BackupCategory.EXTENSIONS -> Icons.Filled.Extension
    BackupCategory.UPDATE_SETTINGS -> Icons.Filled.SystemUpdate
}

private fun categoryNames(context: Context, categories: Collection<BackupCategory>): String =
    categories.joinToString { context.getString(categoryTitleRes(it)) }

@Composable
private fun PasswordVisibilityToggle(visible: Boolean, onToggle: () -> Unit) {
    val colors = LocalClintColors.current
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = stringResource(if (visible) R.string.backup_password_hide else R.string.backup_password_show),
            tint = colors.iconTint
        )
    }
}

@Composable
fun BackupRestorePane(activity: SettingsActivity) {
    val colors = LocalClintColors.current
    val scope = rememberCoroutineScope()
    val hideStatusBar = remember { PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("hide_status_bar", false) }
    val hideSystemNavigation = remember { PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("hide_system_navigation", false) }
    val uiState = remember { BackupRestoreUiState(hideStatusBar, hideSystemNavigation) }

    var simpleDialog by remember { mutableStateOf<ConfirmDialogConfig?>(null) }

    val okLabel = stringResource(R.string.backup_generic_ok)
    val cancelLabel = stringResource(R.string.action_cancel)
    val authTitle = stringResource(R.string.backup_auth_title)
    val authSubtitleBackup = stringResource(R.string.backup_auth_subtitle_backup)
    val authSubtitleRestore = stringResource(R.string.backup_auth_subtitle_restore)
    val authUnavailableTitle = stringResource(R.string.backup_auth_unavailable_title)
    val authUnavailableMessage = stringResource(R.string.backup_auth_unavailable_message)
    val backupResultSuccessTitle = stringResource(R.string.backup_result_success_title)
    val backupResultSuccessMessage = stringResource(R.string.backup_result_success_message)
    val backupResultSkippedNote = stringResource(R.string.backup_result_skipped_note)
    val backupResultErrorTitle = stringResource(R.string.backup_result_error_title)
    val backupResultErrorMessage = stringResource(R.string.backup_result_error_message)
    val restoreInvalidTitle = stringResource(R.string.restore_invalid_title)
    val restoreInvalidMessage = stringResource(R.string.restore_invalid_message)
    val restoreUnsupportedTitle = stringResource(R.string.restore_unsupported_title)
    val restoreUnsupportedMessage = stringResource(R.string.restore_unsupported_message)
    val restoreGenericErrorTitle = stringResource(R.string.restore_generic_error_title)
    val restoreGenericErrorMessage = stringResource(R.string.restore_generic_error_message)
    val restoreConfirmTitle = stringResource(R.string.restore_confirm_title)
    val restoreConfirmMessage = stringResource(R.string.restore_confirm_message)
    val restoreActionRestore = stringResource(R.string.restore_action_restore)
    val restoreResultSuccessTitle = stringResource(R.string.restore_result_success_title)
    val restoreResultSuccessMessage = stringResource(R.string.restore_result_success_message)
    val restoreResultPartialNote = stringResource(R.string.restore_result_partial_note)
    val restoreResultErrorTitle = stringResource(R.string.restore_result_error_title)
    val restoreResultErrorMessage = stringResource(R.string.restore_result_error_message)
    val restoreActionRestartNow = stringResource(R.string.restore_action_restart_now)
    val backupPasswordTooShort = stringResource(R.string.backup_password_too_short)
    val backupPasswordMismatch = stringResource(R.string.backup_password_mismatch)
    val restorePasswordWrong = stringResource(R.string.restore_password_wrong)

    fun requireAuthThen(subtitle: String, onSuccess: () -> Unit) {
        if (!BackupAuthGate.isAuthenticationAvailable(activity)) {
            simpleDialog = ConfirmDialogConfig(
                title = authUnavailableTitle,
                message = authUnavailableMessage,
                positiveLabel = okLabel
            )
            return
        }
        scope.launch {
            when (BackupAuthGate.authenticate(activity, authTitle, subtitle)) {
                is AuthGateResult.Success -> onSuccess()
                is AuthGateResult.NotAvailable -> {
                    simpleDialog = ConfirmDialogConfig(
                        title = authUnavailableTitle,
                        message = authUnavailableMessage,
                        positiveLabel = okLabel
                    )
                }
                else -> Unit
            }
        }
    }

    fun performRestoreNow() {
        val zipFile = uiState.plainZipFile.value ?: return
        val manifest = uiState.manifest.value ?: return
        uiState.stage.value = BackupRestoreStage.RESTORING
        scope.launch {
            runCatching {
                RestoreManager.performRestore(activity, zipFile, manifest, uiState.selectedRestoreCategories.toSet()) { completed, total ->
                    uiState.restoreProgressCompleted.value = completed
                    uiState.restoreProgressTotal.value = total
                }
            }.onSuccess { outcome ->
                uiState.restoredCategories.value = outcome.restoredCategories
                uiState.unavailableCategories.value = outcome.unavailableCategories
                uiState.stage.value = BackupRestoreStage.RESTORE_RESULT
                val message = buildString {
                    append(restoreResultSuccessMessage)
                    if (outcome.unavailableCategories.isNotEmpty()) {
                        append("\n\n")
                        append(String.format(restoreResultPartialNote, categoryNames(activity, outcome.unavailableCategories)))
                    }
                }
                simpleDialog = ConfirmDialogConfig(
                    title = restoreResultSuccessTitle,
                    message = message,
                    positiveLabel = restoreActionRestartNow,
                    onPositive = { activity.restartAppAfterRestore() },
                    cancelable = false
                )
            }.onFailure {
                uiState.stage.value = BackupRestoreStage.IDLE
                simpleDialog = ConfirmDialogConfig(
                    title = restoreResultErrorTitle,
                    message = restoreResultErrorMessage,
                    positiveLabel = okLabel,
                    onPositive = { uiState.resetRestoreFlow() }
                )
            }
        }
    }

    fun handleUnlockResult(result: UnlockResult) {
        when (result) {
            is UnlockResult.Ready -> {
                uiState.plainZipFile.value = result.plainZipFile
                uiState.manifest.value = result.manifest
                uiState.availableRestoreCategories.clear()
                uiState.availableRestoreCategories.addAll(result.availableCategories)
                uiState.selectedRestoreCategories.clear()
                uiState.selectedRestoreCategories.addAll(result.availableCategories)
                uiState.stage.value = BackupRestoreStage.SELECT_RESTORE_CATEGORIES
            }
            is UnlockResult.WrongPassword -> {
                uiState.restorePasswordError.value = restorePasswordWrong
                uiState.stage.value = BackupRestoreStage.RESTORE_PASSWORD
            }
            is UnlockResult.InvalidFile -> {
                uiState.stage.value = BackupRestoreStage.IDLE
                simpleDialog = ConfirmDialogConfig(title = restoreInvalidTitle, message = restoreInvalidMessage, positiveLabel = okLabel, onPositive = { uiState.resetRestoreFlow() })
            }
            is UnlockResult.UnsupportedVersion -> {
                uiState.stage.value = BackupRestoreStage.IDLE
                simpleDialog = ConfirmDialogConfig(title = restoreUnsupportedTitle, message = restoreUnsupportedMessage, positiveLabel = okLabel, onPositive = { uiState.resetRestoreFlow() })
            }
            is UnlockResult.Error -> {
                uiState.stage.value = BackupRestoreStage.IDLE
                simpleDialog = ConfirmDialogConfig(title = restoreGenericErrorTitle, message = restoreGenericErrorMessage, positiveLabel = okLabel, onPositive = { uiState.resetRestoreFlow() })
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uiState.resetRestoreFlow()
        uiState.stage.value = BackupRestoreStage.RESTORE_LOADING
        scope.launch {
            when (val stageResult = RestoreManager.stageFromUri(activity, uri)) {
                is StageResult.Staged -> {
                    uiState.stagedFile.value = stageResult.file
                    if (stageResult.file.header.encrypted) {
                        uiState.stage.value = BackupRestoreStage.RESTORE_PASSWORD
                    } else {
                        handleUnlockResult(RestoreManager.unlock(activity, stageResult.file, null))
                    }
                }
                is StageResult.InvalidFile -> {
                    uiState.stage.value = BackupRestoreStage.IDLE
                    simpleDialog = ConfirmDialogConfig(title = restoreInvalidTitle, message = restoreInvalidMessage, positiveLabel = okLabel)
                }
                is StageResult.Error -> {
                    uiState.stage.value = BackupRestoreStage.IDLE
                    simpleDialog = ConfirmDialogConfig(title = restoreGenericErrorTitle, message = restoreGenericErrorMessage, positiveLabel = okLabel)
                }
            }
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val categories = uiState.selectedBackupCategories.toSet()
        val password = if (uiState.encryptBackup.value) uiState.backupPassword.value.toCharArray() else null
        uiState.stage.value = BackupRestoreStage.CREATING_BACKUP
        scope.launch {
            runCatching {
                activity.contentResolver.openOutputStream(uri)?.use { out ->
                    BackupManager.createBackup(
                        activity, categories, password, out,
                        onProgress = { completed, total ->
                            uiState.backupProgressCompleted.value = completed
                            uiState.backupProgressTotal.value = total
                        },
                        onEncryptStart = { uiState.stage.value = BackupRestoreStage.ENCRYPTING_BACKUP }
                    )
                } ?: throw IllegalStateException("Unable to open output stream")
            }.onSuccess { result ->
                uiState.stage.value = BackupRestoreStage.IDLE
                val message = buildString {
                    append(backupResultSuccessMessage)
                    if (result.skippedCategories.isNotEmpty()) {
                        append("\n\n")
                        append(String.format(backupResultSkippedNote, categoryNames(activity, result.skippedCategories)))
                    }
                }
                simpleDialog = ConfirmDialogConfig(
                    title = backupResultSuccessTitle,
                    message = message,
                    positiveLabel = okLabel,
                    onPositive = { uiState.resetBackupFlow() }
                )
            }.onFailure {
                uiState.stage.value = BackupRestoreStage.SELECT_BACKUP_CATEGORIES
                simpleDialog = ConfirmDialogConfig(
                    title = backupResultErrorTitle,
                    message = backupResultErrorMessage,
                    positiveLabel = okLabel
                )
            }
        }
    }

    fun startBackupFlow() {
        requireAuthThen(authSubtitleBackup) {
            uiState.resetBackupFlow()
            uiState.stage.value = BackupRestoreStage.SELECT_BACKUP_CATEGORIES
        }
    }

    fun startRestoreFlow() {
        requireAuthThen(authSubtitleRestore) {
            openDocumentLauncher.launch(arrayOf("*/*"))
        }
    }

    fun launchBackupCreation() {
        if (uiState.encryptBackup.value) {
            val error = when {
                uiState.backupPassword.value.length < 6 -> backupPasswordTooShort
                uiState.backupPassword.value != uiState.backupPasswordConfirm.value -> backupPasswordMismatch
                else -> null
            }
            uiState.passwordError.value = error
            if (error != null) return
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        createDocumentLauncher.launch("clint_backup_$timestamp.clintbackup")
    }

    SettingsScreenScaffold(
        overlay = {
            ConfirmDialogHost(config = simpleDialog, hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = { simpleDialog = null })

            if (uiState.stage.value == BackupRestoreStage.SELECT_BACKUP_CATEGORIES) {
                BackupCategoryDialog(uiState, hideStatusBar, hideSystemNavigation, onDismiss = { uiState.resetBackupFlow() }, onCreate = { launchBackupCreation() })
            }

            if (uiState.stage.value == BackupRestoreStage.RESTORE_PASSWORD) {
                RestorePasswordDialog(
                    uiState = uiState,
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onDismiss = {
                        uiState.stagedFile.value?.let { RestoreManager.discard(it) }
                        uiState.resetRestoreFlow()
                    },
                    onContinue = {
                        val staged = uiState.stagedFile.value ?: return@RestorePasswordDialog
                        uiState.stage.value = BackupRestoreStage.RESTORE_LOADING
                        scope.launch {
                            handleUnlockResult(
                                RestoreManager.unlock(
                                    activity,
                                    staged,
                                    uiState.restorePassword.value.toCharArray(),
                                    onDecryptStart = { uiState.stage.value = BackupRestoreStage.DECRYPTING_BACKUP }
                                )
                            )
                        }
                    }
                )
            }

            if (uiState.stage.value == BackupRestoreStage.SELECT_RESTORE_CATEGORIES) {
                RestoreCategoryDialog(
                    uiState = uiState,
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onDismiss = {
                        uiState.plainZipFile.value?.let { RestoreManager.discardPlain(it) }
                        uiState.resetRestoreFlow()
                    },
                    onRestore = {
                        simpleDialog = ConfirmDialogConfig(
                            title = restoreConfirmTitle,
                            message = restoreConfirmMessage,
                            positiveLabel = restoreActionRestore,
                            onPositive = { performRestoreNow() },
                            negativeLabel = cancelLabel
                        )
                    }
                )
            }

            if (uiState.stage.value == BackupRestoreStage.CREATING_BACKUP ||
                uiState.stage.value == BackupRestoreStage.ENCRYPTING_BACKUP ||
                uiState.stage.value == BackupRestoreStage.RESTORE_LOADING ||
                uiState.stage.value == BackupRestoreStage.DECRYPTING_BACKUP ||
                uiState.stage.value == BackupRestoreStage.RESTORING
            ) {
                val completed: Int
                val total: Int
                when (uiState.stage.value) {
                    BackupRestoreStage.CREATING_BACKUP -> {
                        completed = uiState.backupProgressCompleted.value
                        total = uiState.backupProgressTotal.value
                    }
                    BackupRestoreStage.RESTORING -> {
                        completed = uiState.restoreProgressCompleted.value
                        total = uiState.restoreProgressTotal.value
                    }
                    else -> {
                        completed = 0
                        total = 0
                    }
                }
                ProgressDialog(
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    message = when (uiState.stage.value) {
                        BackupRestoreStage.CREATING_BACKUP -> stringResource(R.string.backup_progress_creating)
                        BackupRestoreStage.ENCRYPTING_BACKUP -> stringResource(R.string.backup_progress_encrypting)
                        BackupRestoreStage.DECRYPTING_BACKUP -> stringResource(R.string.backup_progress_decrypting)
                        BackupRestoreStage.RESTORING -> stringResource(R.string.backup_progress_restoring)
                        else -> stringResource(R.string.backup_progress_reading)
                    },
                    completed = completed,
                    total = total
                )
            }
        }
    ) {
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = Icons.Filled.Backup,
                title = stringResource(R.string.backup_row_title),
                summary = stringResource(R.string.backup_row_summary),
                colors = colors,
                onClick = { startBackupFlow() }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.Restore,
                title = stringResource(R.string.restore_row_title),
                summary = stringResource(R.string.restore_row_summary),
                colors = colors,
                onClick = { startRestoreFlow() }
            )
        }
    }
}

@Composable
fun ProgressDialog(hideStatusBar: Boolean, hideSystemNavigation: Boolean, message: String, completed: Int = 0, total: Int = 0) {
    val colors = LocalClintColors.current
    ClintDialog(title = message, hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = {}, cancelable = false, footer = {}) {
        if (total > 0) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
                LinearProgressIndicator(
                    progress = { completed.toFloat() / total.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.primary,
                    trackColor = colors.surfaceVariant
                )
                Text(
                    stringResource(R.string.filter_list_update_progress_counter, completed, total),
                    color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp)
                )
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.primary,
                    trackColor = colors.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun CategoryCheckboxRow(category: BackupCategory, checked: Boolean, onToggle: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(categoryIcon(category), contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            Text(stringResource(categoryTitleRes(category)), color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(stringResource(categoryDescRes(category)), color = colors.secondaryText, fontSize = 12.sp)
        }
        ClintCheckbox(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun BackupCategoryDialog(uiState: BackupRestoreUiState, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit, onCreate: () -> Unit) {
    val colors = LocalClintColors.current
    val categories = remember { BackupCategory.available() }
    val allSelected = uiState.selectedBackupCategories.size == categories.size

    ClintDialog(
        title = stringResource(R.string.backup_select_categories_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(
                    onClick = onCreate,
                    enabled = uiState.selectedBackupCategories.isNotEmpty() && (!uiState.encryptBackup.value || (uiState.backupPassword.value.isNotEmpty() && uiState.backupPasswordConfirm.value.isNotEmpty()))
                ) {
                    Text(stringResource(R.string.backup_action_create), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                TextButton(onClick = {
                    if (allSelected) uiState.selectedBackupCategories.clear() else {
                        uiState.selectedBackupCategories.clear()
                        uiState.selectedBackupCategories.addAll(categories)
                    }
                }) {
                    Text(
                        stringResource(if (allSelected) R.string.backup_deselect_all else R.string.backup_select_all),
                        color = colors.primary,
                        fontSize = 13.sp
                    )
                }
            }
            categories.forEach { category ->
                CategoryCheckboxRow(
                    category = category,
                    checked = category in uiState.selectedBackupCategories,
                    onToggle = {
                        if (category in uiState.selectedBackupCategories) {
                            uiState.selectedBackupCategories.remove(category)
                        } else {
                            uiState.selectedBackupCategories.add(category)
                        }
                    }
                )
            }
            RowDivider(colors.divider)
            Text(
                stringResource(R.string.backup_protect_title),
                color = colors.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            Row(Modifier.fillMaxWidth().clickable { uiState.encryptBackup.value = false }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                ClintRadioButton(selected = !uiState.encryptBackup.value)
                Text(stringResource(R.string.backup_protect_no_password), color = colors.onSurface, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp))
            }
            Row(Modifier.fillMaxWidth().clickable { uiState.encryptBackup.value = true }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                ClintRadioButton(selected = uiState.encryptBackup.value)
                Text(stringResource(R.string.backup_protect_with_password), color = colors.onSurface, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp))
            }
            if (uiState.encryptBackup.value) {
                ClintOutlinedTextField(
                    value = uiState.backupPassword.value,
                    onValueChange = { uiState.backupPassword.value = it; uiState.passwordError.value = null },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    label = { Text(stringResource(R.string.backup_password_label)) },
                    singleLine = true,
                    isError = uiState.passwordError.value != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (uiState.backupPasswordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        PasswordVisibilityToggle(uiState.backupPasswordVisible.value) {
                            uiState.backupPasswordVisible.value = !uiState.backupPasswordVisible.value
                        }
                    }
                )
                ClintOutlinedTextField(
                    value = uiState.backupPasswordConfirm.value,
                    onValueChange = { uiState.backupPasswordConfirm.value = it; uiState.passwordError.value = null },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    label = { Text(stringResource(R.string.backup_password_confirm_label)) },
                    singleLine = true,
                    isError = uiState.passwordError.value != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (uiState.backupPasswordConfirmVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        PasswordVisibilityToggle(uiState.backupPasswordConfirmVisible.value) {
                            uiState.backupPasswordConfirmVisible.value = !uiState.backupPasswordConfirmVisible.value
                        }
                    }
                )
                uiState.passwordError.value?.let { error ->
                    Text(error, color = colors.primary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Text(
                    stringResource(R.string.backup_password_hint),
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun RestorePasswordDialog(uiState: BackupRestoreUiState, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit, onContinue: () -> Unit) {
    val colors = LocalClintColors.current
    ClintDialog(
        title = stringResource(R.string.restore_password_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = onContinue, enabled = uiState.restorePassword.value.isNotEmpty()) {
                    Text(stringResource(R.string.restore_action_continue), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text(stringResource(R.string.restore_password_message), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            ClintOutlinedTextField(
                value = uiState.restorePassword.value,
                onValueChange = { uiState.restorePassword.value = it; uiState.restorePasswordError.value = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.backup_password_label)) },
                singleLine = true,
                isError = uiState.restorePasswordError.value != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (uiState.restorePasswordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    PasswordVisibilityToggle(uiState.restorePasswordVisible.value) {
                        uiState.restorePasswordVisible.value = !uiState.restorePasswordVisible.value
                    }
                }
            )
            uiState.restorePasswordError.value?.let { error ->
                Text(error, color = colors.primary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun RestoreCategoryDialog(uiState: BackupRestoreUiState, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit, onRestore: () -> Unit) {
    val colors = LocalClintColors.current
    ClintDialog(
        title = stringResource(R.string.restore_select_categories_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = onRestore, enabled = uiState.selectedRestoreCategories.isNotEmpty()) {
                    Text(stringResource(R.string.restore_action_restore), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            uiState.availableRestoreCategories.forEach { category ->
                CategoryCheckboxRow(
                    category = category,
                    checked = category in uiState.selectedRestoreCategories,
                    onToggle = {
                        if (category in uiState.selectedRestoreCategories) {
                            uiState.selectedRestoreCategories.remove(category)
                        } else {
                            uiState.selectedRestoreCategories.add(category)
                        }
                    }
                )
            }
        }
    }
}
