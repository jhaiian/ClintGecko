package com.jhaiian.clint.browser.delegates

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.downloads.DownloadFileHelper
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import com.jhaiian.clint.ui.showClintSnackbar
import java.io.File

internal const val PREF_BATTERY_OPT_ASKED = "battery_opt_asked"

internal fun MainActivity.initiateDownload(
    url: String,
    filename: String,
    userAgent: String,
    referer: String,
    cookies: String,
    retryEnabled: Boolean,
    unmeteredOnly: Boolean,
    splitParts: Int,
    multithreadingParts: Int,
    speedLimitBytesPerSec: Long,
    locationMode: String,
    customLocationUri: String?,
    categorizeEnabled: Boolean,
    scheduledStartAtMillis: Long = 0L,
    onDismiss: () -> Unit = {},
    onRename: () -> Unit = {},
    onStarted: () -> Unit = {}
) {
    if (unmeteredOnly && isNetworkMetered()) {
        uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
            title = getString(R.string.download_metered_warning_title),
            message = getString(R.string.download_metered_warning_message),
            positiveLabel = getString(R.string.action_yes),
            onPositive = {
                proceedWithDownload(url, filename, userAgent, referer, cookies, retryEnabled, false, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, categorizeEnabled, scheduledStartAtMillis, onDismiss, onRename, onStarted)
            },
            negativeLabel = getString(R.string.action_no),
            onNegative = {
                proceedWithDownload(url, filename, userAgent, referer, cookies, retryEnabled, true, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, categorizeEnabled, scheduledStartAtMillis, onDismiss, onRename, onStarted)
            },
            neutralLabel = getString(R.string.action_cancel)
        )
        return
    }
    proceedWithDownload(url, filename, userAgent, referer, cookies, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, categorizeEnabled, scheduledStartAtMillis, onDismiss, onRename, onStarted)
}

private fun MainActivity.proceedWithDownload(
    url: String,
    filename: String,
    userAgent: String,
    referer: String,
    cookies: String,
    retryEnabled: Boolean,
    unmeteredOnly: Boolean,
    splitParts: Int,
    multithreadingParts: Int,
    speedLimitBytesPerSec: Long,
    locationMode: String,
    customLocationUri: String?,
    categorizeEnabled: Boolean,
    scheduledStartAtMillis: Long,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onStarted: () -> Unit
) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val pm = getSystemService(PowerManager::class.java)
    if (!prefs.getBoolean(PREF_BATTERY_OPT_ASKED, false) &&
        !pm.isIgnoringBatteryOptimizations(packageName)
    ) {
        prefs.edit().putBoolean(PREF_BATTERY_OPT_ASKED, true).apply()
        uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
            title = getString(R.string.battery_opt_rationale_title),
            message = getString(R.string.battery_opt_rationale_message),
            cancelable = false,
            positiveLabel = getString(R.string.action_allow),
            onPositive = {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                doEnqueueDownload(url, filename, userAgent, referer, cookies, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, categorizeEnabled, scheduledStartAtMillis, onDismiss, onRename, onStarted)
            },
            negativeLabel = getString(R.string.action_not_now),
            onNegative = {
                doEnqueueDownload(url, filename, userAgent, referer, cookies, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, categorizeEnabled, scheduledStartAtMillis, onDismiss, onRename, onStarted)
            }
        )
        return
    }
    doEnqueueDownload(url, filename, userAgent, referer, cookies, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, categorizeEnabled, scheduledStartAtMillis, onDismiss, onRename, onStarted)
}

private fun MainActivity.doEnqueueDownload(
    url: String,
    filename: String,
    userAgent: String,
    referer: String,
    cookies: String,
    retryEnabled: Boolean,
    unmeteredOnly: Boolean,
    splitParts: Int,
    multithreadingParts: Int,
    speedLimitBytesPerSec: Long,
    locationMode: String,
    customLocationUri: String?,
    categorizeEnabled: Boolean,
    scheduledStartAtMillis: Long,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onStarted: () -> Unit
) {
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
        checkConflictAndEnqueue(url, filename, userAgent, referer, cookies, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, categorizeEnabled, scheduledStartAtMillis, onDismiss, onRename, onStarted)
        return
    }

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        == PackageManager.PERMISSION_GRANTED
    ) {
        checkConflictAndEnqueue(url, filename, userAgent, referer, cookies, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, categorizeEnabled, scheduledStartAtMillis, onDismiss, onRename, onStarted)
        return
    }

    pendingDownload = MainActivity.PendingDownload(url, filename, userAgent, referer, cookies)

    uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
        title = getString(R.string.download_storage_permission_title),
        message = getString(R.string.download_storage_permission_message),
        positiveLabel = getString(R.string.action_allow),
        onPositive = {
            onDismiss()
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        },
        negativeLabel = getString(R.string.action_cancel),
        onNegative = {
            pendingDownload = null
            onDismiss()
        }
    )
}

private fun MainActivity.checkConflictAndEnqueue(
    url: String,
    filename: String,
    userAgent: String,
    referer: String,
    cookies: String,
    retryEnabled: Boolean,
    unmeteredOnly: Boolean,
    splitParts: Int,
    multithreadingParts: Int,
    speedLimitBytesPerSec: Long,
    locationMode: String,
    customLocationUri: String?,
    categorizeEnabled: Boolean,
    scheduledStartAtMillis: Long,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onStarted: () -> Unit
) {
    val existing = ClintDownloadManager.findActiveDownloadForUrl(url)
    if (existing != null) {
        uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
            title = getString(R.string.download_already_active_title),
            message = getString(R.string.download_already_active_message, existing.filename),
            positiveLabel = getString(R.string.action_download_anyway),
            onPositive = {
                checkFilenameConflictAndEnqueue(url, filename, userAgent, referer, cookies, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, categorizeEnabled, scheduledStartAtMillis, onDismiss, onRename, onStarted)
            },
            negativeLabel = getString(R.string.action_cancel)
        )
        return
    }
    checkFilenameConflictAndEnqueue(url, filename, userAgent, referer, cookies, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, categorizeEnabled, scheduledStartAtMillis, onDismiss, onRename, onStarted)
}

private fun MainActivity.checkFilenameConflictAndEnqueue(
    url: String,
    filename: String,
    userAgent: String,
    referer: String,
    cookies: String,
    retryEnabled: Boolean,
    unmeteredOnly: Boolean,
    splitParts: Int,
    multithreadingParts: Int,
    speedLimitBytesPerSec: Long,
    locationMode: String,
    customLocationUri: String?,
    categorizeEnabled: Boolean,
    scheduledStartAtMillis: Long,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onStarted: () -> Unit
) {
    val pendingMatch = ClintDownloadManager.downloadsFlow.value.firstOrNull {
        it.status in com.jhaiian.clint.downloads.DownloadStatus.NOT_FINISHED && it.url == url
    }

    val isSaf = locationMode == DownloadSettingsKeys.MODE_CUSTOM
    val fileExists = if (isSaf) {
        val treeUri = customLocationUri?.let { Uri.parse(it) }
            ?: DownloadFileHelper.getSafTreeUri(this)
        treeUri?.let { DocumentFile.fromTreeUri(this, it)?.findFile(filename) } != null
    } else {
        File(DownloadFileHelper.resolveDownloadDir(), filename).exists()
    }

    if (!fileExists && pendingMatch == null) {
        onDismiss()
        ClintDownloadManager.enqueue(this, url, filename, userAgent, referer, cookies, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, scheduledStartAtMillis, categorizeEnabled)
        onStarted()
        return
    }

    uiState.conflictDialogRequest = com.jhaiian.clint.downloads.DownloadConflictDialogRequest(
        onAddDuplicate = {
            onDismiss()
            ClintDownloadManager.enqueue(this, url, filename, userAgent, referer, cookies, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, scheduledStartAtMillis, categorizeEnabled)
            onStarted()
        },
        onOverride = {
            if (pendingMatch != null) ClintDownloadManager.remove(this, pendingMatch.id, deleteFile = true)
            else deleteExistingDownload(filename, locationMode, customLocationUri)
            onDismiss()
            ClintDownloadManager.enqueue(this, url, filename, userAgent, referer, cookies, retryEnabled, unmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, scheduledStartAtMillis, categorizeEnabled)
            onStarted()
        },
        onRename = onRename,
        onUpdateLink = pendingMatch?.let { match ->
            {
                ClintDownloadManager.updateDownloadUrl(match.id, url)
                onDismiss()
                showClintSnackbar(message = getString(R.string.media_capture_duplicate_link_updated))
            }
        }
    )
}

private fun MainActivity.deleteExistingDownload(
    filename: String,
    locationMode: String,
    customLocationUri: String?
) {
    val matchingIds = ClintDownloadManager.downloadsFlow.value.filter { it.filename == filename }.map { it.id }
    matchingIds.forEach { ClintDownloadManager.remove(this, it, deleteFile = true) }

    val isSaf = locationMode == DownloadSettingsKeys.MODE_CUSTOM
    if (isSaf) {
        val treeUri = customLocationUri?.let { Uri.parse(it) }
            ?: DownloadFileHelper.getSafTreeUri(this)
        treeUri?.let { DocumentFile.fromTreeUri(this, it)?.findFile(filename)?.delete() }
    } else {
        File(DownloadFileHelper.resolveDownloadDir(), filename).delete()
    }
}
