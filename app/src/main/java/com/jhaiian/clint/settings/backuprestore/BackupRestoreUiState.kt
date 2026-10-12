package com.jhaiian.clint.settings.backuprestore

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.jhaiian.clint.backup.BackupCategory
import com.jhaiian.clint.backup.BackupManifest
import com.jhaiian.clint.backup.StagedBackupFile
import java.io.File

enum class BackupRestoreStage {
    IDLE,
    SELECT_BACKUP_CATEGORIES,
    CREATING_BACKUP,
    ENCRYPTING_BACKUP,
    BACKUP_RESULT,
    RESTORE_LOADING,
    DECRYPTING_BACKUP,
    RESTORE_PASSWORD,
    SELECT_RESTORE_CATEGORIES,
    RESTORE_CONFIRM,
    RESTORING,
    RESTORE_RESULT
}

class BackupRestoreUiState(val hideStatusBar: Boolean, val hideSystemNavigation: Boolean) {
    var stage = mutableStateOf(BackupRestoreStage.IDLE)

    val selectedBackupCategories = mutableStateListOf<BackupCategory>().apply { addAll(BackupCategory.available()) }
    var encryptBackup = mutableStateOf(false)
    var backupPassword = mutableStateOf("")
    var backupPasswordConfirm = mutableStateOf("")
    var passwordError = mutableStateOf<String?>(null)
    var backupPasswordVisible = mutableStateOf(false)
    var backupPasswordConfirmVisible = mutableStateOf(false)

    var backupSuccessCategories = mutableStateOf<Set<BackupCategory>>(emptySet())
    var backupSkippedCategories = mutableStateOf<Set<BackupCategory>>(emptySet())
    var backupErrorMessage = mutableStateOf<String?>(null)

    var stagedFile = mutableStateOf<StagedBackupFile?>(null)
    var restorePassword = mutableStateOf("")
    var restorePasswordError = mutableStateOf<String?>(null)
    var restorePasswordVisible = mutableStateOf(false)
    var restoreOpenError = mutableStateOf<RestoreOpenError?>(null)

    var plainZipFile = mutableStateOf<File?>(null)
    var manifest = mutableStateOf<BackupManifest?>(null)
    val availableRestoreCategories = mutableStateListOf<BackupCategory>()
    val selectedRestoreCategories = mutableStateListOf<BackupCategory>()

    var restoredCategories = mutableStateOf<Set<BackupCategory>>(emptySet())
    var unavailableCategories = mutableStateOf<Set<BackupCategory>>(emptySet())
    var restoreErrorMessage = mutableStateOf<String?>(null)

    var backupProgressCompleted = mutableStateOf(0)
    var backupProgressTotal = mutableStateOf(0)
    var restoreProgressCompleted = mutableStateOf(0)
    var restoreProgressTotal = mutableStateOf(0)

    fun resetBackupFlow() {
        stage.value = BackupRestoreStage.IDLE
        selectedBackupCategories.clear()
        selectedBackupCategories.addAll(BackupCategory.available())
        encryptBackup.value = false
        backupPassword.value = ""
        backupPasswordConfirm.value = ""
        passwordError.value = null
        backupPasswordVisible.value = false
        backupPasswordConfirmVisible.value = false
        backupErrorMessage.value = null
        backupProgressCompleted.value = 0
        backupProgressTotal.value = 0
    }

    fun resetRestoreFlow() {
        stage.value = BackupRestoreStage.IDLE
        stagedFile.value = null
        restorePassword.value = ""
        restorePasswordError.value = null
        restorePasswordVisible.value = false
        restoreOpenError.value = null
        plainZipFile.value = null
        manifest.value = null
        availableRestoreCategories.clear()
        selectedRestoreCategories.clear()
        restoreErrorMessage.value = null
        restoreProgressCompleted.value = 0
        restoreProgressTotal.value = 0
    }
}

enum class RestoreOpenError { INVALID_FILE, UNSUPPORTED_VERSION, GENERIC }
