package com.jhaiian.clint.browser.delegates

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.downloads.DEFAULT_SPEED_LIMIT_UNIT
import com.jhaiian.clint.downloads.DownloadRequestDialog
import com.jhaiian.clint.downloads.DownloadCategories
import com.jhaiian.clint.downloads.DownloadRequestSubmission
import com.jhaiian.clint.downloads.checkFat32FileSizeLimit
import com.jhaiian.clint.downloads.checkStorageAvailable
import com.jhaiian.clint.downloads.resolveSpeedLimitBytesPerSec
import com.jhaiian.clint.downloads.estimateBase64DecodedSize
import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.downloads.DownloadsActivity
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import com.jhaiian.clint.ui.showClintSnackbar
import com.jhaiian.clint.ui.theme.ClintComposeTheme

internal fun MainActivity.showDownloadDialog(
    url: String,
    filename: String,
    userAgent: String,
    referer: String,
    cookies: String,
    forceQuick: Boolean = false
) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val downloadManagerApp = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_MANAGER, DownloadSettingsKeys.DEFAULT_DOWNLOAD_MANAGER) ?: DownloadSettingsKeys.DEFAULT_DOWNLOAD_MANAGER
    if (downloadManagerApp != DownloadSettingsKeys.DEFAULT_DOWNLOAD_MANAGER &&
        com.jhaiian.clint.downloads.sendToExternalDownloadManager(this, downloadManagerApp, url, filename, userAgent, referer, cookies)
    ) {
        return
    }
    mountDownloadRequestDialog(
        forceQuick = forceQuick,
        url = url,
        onCopyLink = { copyDownloadRequestLink(url) },
        initialFilename = filename,
        contentLengthBytes = -1L,
        fetchUrl = url,
        fetchUserAgent = userAgent,
        checkStorage = true,
        showOptions = true,
        initialLocationMode = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_LOCATION_MODE, DownloadSettingsKeys.MODE_DEFAULT) ?: DownloadSettingsKeys.MODE_DEFAULT,
        initialCustomUri = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, null)?.let { Uri.parse(it) },
        initialRetryEnabled = prefs.getBoolean(DownloadSettingsKeys.PREF_RETRY_ENABLED, DownloadSettingsKeys.DEFAULT_RETRY_ENABLED),
        initialUnmeteredOnly = prefs.getBoolean(DownloadSettingsKeys.PREF_UNMETERED_ONLY, DownloadSettingsKeys.DEFAULT_UNMETERED_ONLY),
        initialSplitParts = prefs.getInt(DownloadSettingsKeys.PREF_SPLIT_PARTS, DownloadSettingsKeys.DEFAULT_SPLIT_PARTS),
        initialMultithreadingParts = prefs.getInt(DownloadSettingsKeys.PREF_MULTITHREADING_PARTS, DownloadSettingsKeys.DEFAULT_MULTITHREADING_PARTS),
        initialSpeedLimitAmount = prefs.getInt(DownloadSettingsKeys.PREF_SPEED_LIMIT_AMOUNT, DownloadSettingsKeys.DEFAULT_SPEED_LIMIT_AMOUNT),
        initialSpeedLimitUnit = prefs.getString(DownloadSettingsKeys.PREF_SPEED_LIMIT_UNIT, DEFAULT_SPEED_LIMIT_UNIT) ?: DEFAULT_SPEED_LIMIT_UNIT,
        onSubmit = { submission, dismiss, onRename ->
            initiateDownload(
                url, submission.filename, userAgent, referer, cookies,
                submission.retryEnabled, submission.unmeteredOnly, submission.splitParts, submission.multithreadingParts, submission.speedLimitBytesPerSec,
                submission.locationMode, submission.customLocationUri, submission.categorizeEnabled, submission.scheduledStartAtMillis,
                onDismiss = dismiss,
                onRename = onRename,
                onStarted = { showDownloadStartedSnackbar(submission.filename, submission.locationMode, submission.customLocationUri) }
            )
        }
    )
}

internal fun MainActivity.showDownloadDialogForBlob(
    base64: String,
    filename: String,
    mimeType: String,
    forceQuick: Boolean = false
) {
    val blobLabel = getString(R.string.download_dialog_blob_label)
    mountDownloadRequestDialog(
        forceQuick = forceQuick,
        url = blobLabel,
        onCopyLink = { copyDownloadRequestLink(blobLabel) },
        initialFilename = filename,
        contentLengthBytes = estimateBase64DecodedSize(base64),
        fileSizeDisplayOverride = getString(R.string.download_dialog_file_size_unknown),
        checkStorage = false,
        showOptions = false,
        showStorageInfo = true,
        initialLocationMode = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(DownloadSettingsKeys.PREF_DOWNLOAD_LOCATION_MODE, DownloadSettingsKeys.MODE_DEFAULT) ?: DownloadSettingsKeys.MODE_DEFAULT,
        initialCustomUri = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, null)?.let { Uri.parse(it) },
        onSubmit = { submission, dismiss, _ ->
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val locationMode = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_LOCATION_MODE, DownloadSettingsKeys.MODE_DEFAULT) ?: DownloadSettingsKeys.MODE_DEFAULT
            val customLocationUri = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, null)
            if (com.jhaiian.clint.downloads.DownloadFileHelper.isCustomLocationAccessible(this, locationMode, customLocationUri)) {
                showClintSnackbar(
                    message = getString(R.string.toast_downloading, submission.filename),
                    actionLabel = getString(R.string.download_started_view_action),
                    onAction = { DownloadsActivity.open(this) }
                )
            }
            dismiss()
            ClintDownloadManager.enqueueBlob(this, base64, submission.filename, mimeType)
        }
    )
}

internal fun MainActivity.showDownloadStartedSnackbar(filename: String, locationMode: String, customLocationUri: String?) {
    if (com.jhaiian.clint.downloads.DownloadFileHelper.isCustomLocationAccessible(this, locationMode, customLocationUri)) {
        showClintSnackbar(
            message = getString(R.string.toast_downloading, filename),
            actionLabel = getString(R.string.download_started_view_action),
            onAction = { DownloadsActivity.open(this) }
        )
    }
}

private fun MainActivity.copyDownloadRequestLink(text: String) {
    val clipboard = getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.download_dialog_link_clip_label), text))
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, getString(R.string.download_dialog_link_copied), Toast.LENGTH_SHORT).show()
    }
}

internal fun MainActivity.mountDownloadRequestDialog(
    forceQuick: Boolean = false,
    url: String,
    onCopyLink: () -> Unit,
    initialFilename: String,
    contentLengthBytes: Long,
    fileSizeDisplayOverride: String? = null,
    fetchUrl: String? = null,
    fetchUserAgent: String = "",
    checkStorage: Boolean,
    showOptions: Boolean,
    showStorageInfo: Boolean = showOptions,
    showSplitAndMultithreading: Boolean = showOptions,
    showConcurrentSegments: Boolean = false,
    initialConcurrentSegments: Int = 6,
    initialLocationMode: String,
    initialCustomUri: Uri?,
    initialRetryEnabled: Boolean = false,
    initialUnmeteredOnly: Boolean = false,
    initialSplitParts: Int = 1,
    initialMultithreadingParts: Int = 1,
    initialSpeedLimitAmount: Int = 0,
    initialSpeedLimitUnit: String = DEFAULT_SPEED_LIMIT_UNIT,
    onSubmit: (DownloadRequestSubmission, dismiss: () -> Unit, onRename: () -> Unit) -> Unit
) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val theme = prefs.getString("app_theme", "system") ?: "system"
    val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
    val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)

    val dismiss: () -> Unit = { overlayContent = null }

    fun mountFull() { overlayContent = {
        ClintComposeTheme(theme = theme) {
            DownloadRequestDialog(
                hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                url = url,
                onCopyLink = onCopyLink,
                initialFilename = initialFilename,
                contentLengthBytes = contentLengthBytes,
                fileSizeDisplayOverride = fileSizeDisplayOverride,
                fetchUrl = fetchUrl,
                fetchUserAgent = fetchUserAgent,
                checkStorage = checkStorage,
                showOptions = showOptions,
                showStorageInfo = showStorageInfo,
                showSplitAndMultithreading = showSplitAndMultithreading,
                showConcurrentSegments = showConcurrentSegments,
                initialConcurrentSegments = initialConcurrentSegments,
                initialLocationMode = initialLocationMode,
                initialCustomUri = initialCustomUri,
                initialRetryEnabled = initialRetryEnabled,
                initialUnmeteredOnly = initialUnmeteredOnly,
                initialSplitParts = initialSplitParts,
                initialMultithreadingParts = initialMultithreadingParts,
                initialSpeedLimitAmount = initialSpeedLimitAmount,
                initialSpeedLimitUnit = initialSpeedLimitUnit,
                fragmentManager = supportFragmentManager,
                onLaunchFolderPicker = { onPicked ->
                    downloadDialogFolderPickerCallback = { uri ->
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        onPicked(uri)
                    }
                    launchDownloadDialogFolderPicker()
                },
                onDismiss = dismiss,
                onSubmit = { submission, onDismissFromSubmit, onRename -> onSubmit(submission, onDismissFromSubmit, onRename) }
            )
        }
    } }

    if (forceQuick || prefs.getBoolean(DownloadSettingsKeys.PREF_QUICK_DOWNLOAD, DownloadSettingsKeys.DEFAULT_QUICK_DOWNLOAD)) {
        val storageError = checkStorageAvailable(this, contentLengthBytes, initialLocationMode, initialCustomUri)
        val fat32Error = checkFat32FileSizeLimit(this, contentLengthBytes, initialLocationMode, initialCustomUri)
        if (storageError == null && fat32Error == null) {
            val submission = DownloadRequestSubmission(
                filename = initialFilename,
                retryEnabled = initialRetryEnabled,
                unmeteredOnly = initialUnmeteredOnly,
                splitParts = initialSplitParts,
                multithreadingParts = initialMultithreadingParts,
                speedLimitBytesPerSec = resolveSpeedLimitBytesPerSec(this, initialSpeedLimitAmount, initialSpeedLimitUnit),
                locationMode = initialLocationMode,
                customLocationUri = initialCustomUri?.toString(),
                categorizeEnabled = DownloadCategories.isEnabled(this),
                scheduledStartAtMillis = 0L,
                concurrentSegments = initialConcurrentSegments
            )
            onSubmit(submission, dismiss) { mountFull() }
            return
        }
    }
    mountFull()
}

internal fun MainActivity.launchDownloadDialogFolderPicker() {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        )
    }
    downloadDialogFolderPickerLauncher.launch(intent)
}
