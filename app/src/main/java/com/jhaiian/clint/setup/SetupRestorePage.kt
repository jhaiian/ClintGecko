package com.jhaiian.clint.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.backup.AuthGateResult
import com.jhaiian.clint.backup.BackupAuthGate
import com.jhaiian.clint.backup.RestoreManager
import com.jhaiian.clint.backup.StageResult
import com.jhaiian.clint.backup.UnlockResult
import com.jhaiian.clint.settings.backuprestore.BackupRestoreStage
import com.jhaiian.clint.settings.backuprestore.BackupRestoreUiState
import com.jhaiian.clint.settings.backuprestore.CategoryCheckboxRow
import com.jhaiian.clint.settings.backuprestore.ProgressDialog
import com.jhaiian.clint.settings.backuprestore.RestoreCategoryDialog
import com.jhaiian.clint.settings.backuprestore.RestorePasswordDialog
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.listscreen.ConfirmDialogHost
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.launch

@Composable
fun SetupRestorePage(
    activity: SetupActivity,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onSkip: () -> Unit,
    onRestoreComplete: () -> Unit
) {
    val colors = LocalClintColors.current
    val scope = rememberCoroutineScope()
    val uiState = remember { BackupRestoreUiState(hideStatusBar, hideSystemNavigation) }

    var simpleDialog by remember { mutableStateOf<ConfirmDialogConfig?>(null) }

    val okLabel = stringResource(R.string.backup_generic_ok)
    val cancelLabel = stringResource(R.string.action_cancel)
    val authTitle = stringResource(R.string.backup_auth_title)
    val authSubtitleRestore = stringResource(R.string.backup_auth_subtitle_restore)
    val authUnavailableTitle = stringResource(R.string.backup_auth_unavailable_title)
    val authUnavailableMessage = stringResource(R.string.backup_auth_unavailable_message)
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
    val restoreResultErrorTitle = stringResource(R.string.restore_result_error_title)
    val restoreResultErrorMessage = stringResource(R.string.restore_result_error_message)
    val restoreActionRestartNow = stringResource(R.string.restore_action_restart_now)
    val restorePasswordWrong = stringResource(R.string.restore_password_wrong)

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

    fun performRestoreNow() {
        val zipFile = uiState.plainZipFile.value ?: return
        val manifest = uiState.manifest.value ?: return
        uiState.stage.value = BackupRestoreStage.RESTORING
        scope.launch {
            runCatching {
                RestoreManager.performRestore(activity, zipFile, manifest, uiState.selectedRestoreCategories.toSet())
            }.onSuccess {
                uiState.stage.value = BackupRestoreStage.RESTORE_RESULT
                simpleDialog = ConfirmDialogConfig(
                    title = restoreResultSuccessTitle,
                    message = restoreResultSuccessMessage,
                    positiveLabel = restoreActionRestartNow,
                    onPositive = { onRestoreComplete() },
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

    fun startRestoreFlow() {
        if (!BackupAuthGate.isAuthenticationAvailable(activity)) {
            simpleDialog = ConfirmDialogConfig(title = authUnavailableTitle, message = authUnavailableMessage, positiveLabel = okLabel)
            return
        }
        scope.launch {
            when (BackupAuthGate.authenticate(activity, authTitle, authSubtitleRestore)) {
                is AuthGateResult.Success -> openDocumentLauncher.launch(arrayOf("*/*"))
                is AuthGateResult.NotAvailable -> simpleDialog = ConfirmDialogConfig(title = authUnavailableTitle, message = authUnavailableMessage, positiveLabel = okLabel)
                else -> Unit
            }
        }
    }

    ConfirmDialogHost(config = simpleDialog, hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = { simpleDialog = null })

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

    if (uiState.stage.value == BackupRestoreStage.RESTORE_LOADING ||
        uiState.stage.value == BackupRestoreStage.DECRYPTING_BACKUP ||
        uiState.stage.value == BackupRestoreStage.RESTORING
    ) {
        ProgressDialog(
            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
            message = stringResource(
                when (uiState.stage.value) {
                    BackupRestoreStage.RESTORING -> R.string.backup_progress_restoring
                    BackupRestoreStage.DECRYPTING_BACKUP -> R.string.backup_progress_decrypting
                    else -> R.string.backup_progress_reading
                }
            )
        )
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SetupPageHeader(
            title = stringResource(R.string.setup_restore_title),
            subtitle = stringResource(R.string.setup_restore_subtitle)
        )

        Card(
            Modifier.fillMaxWidth().padding(bottom = 24.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Restore, null, tint = colors.primary, modifier = Modifier.size(36.dp))
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(stringResource(R.string.setup_restore_card_title), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.setup_restore_card_desc), color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }

        SetupPrimaryButton(
            text = stringResource(R.string.setup_restore_button),
            onClick = { startRestoreFlow() },
            backgroundColor = colors.buttonBackground
        )
        SetupPrimaryButton(
            text = stringResource(R.string.setup_restore_skip),
            onClick = onSkip,
            backgroundColor = colors.buttonBackground,
            modifier = Modifier.padding(top = 10.dp, bottom = 24.dp)
        )
    }
}
