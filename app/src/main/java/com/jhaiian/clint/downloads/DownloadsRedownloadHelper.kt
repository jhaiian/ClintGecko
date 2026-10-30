package com.jhaiian.clint.downloads

import android.net.Uri
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import com.jhaiian.clint.ui.showClintSnackbar
import com.jhaiian.clint.ui.theme.ClintComposeTheme

internal fun DownloadsActivity.showRedownloadDialog(item: DownloadItem) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val (initialSpeedLimitAmount, initialSpeedLimitUnit) = speedLimitBytesToAmountAndUnit(this, item.speedLimitBytesPerSec)

    val theme = prefs.getString("app_theme", "system") ?: "system"
    val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
    val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)

    val dismiss: () -> Unit = { overlayContent = null }
    val displayUrl = item.mega?.link?.takeIf { it.isNotBlank() } ?: item.url

    overlayContent = {
        ClintComposeTheme(theme = theme) {
            DownloadRequestDialog(
                hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                url = displayUrl,
                onCopyLink = { copyRedownloadLink(displayUrl) },
                initialFilename = item.filename,
                contentLengthBytes = item.totalBytes,
                checkStorage = false,
                showOptions = true,
                showSchedule = false,
                showStorageInfo = false,
                initialLocationMode = item.locationMode.ifBlank {
                    prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_LOCATION_MODE, DownloadSettingsKeys.MODE_DEFAULT) ?: DownloadSettingsKeys.MODE_DEFAULT
                },
                initialCustomUri = (item.customLocationUri ?: prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, null))?.let { Uri.parse(it) },
                initialCategorizeEnabled = item.categorizeEnabled,
                initialRetryEnabled = item.retryEnabled,
                initialUnmeteredOnly = item.unmeteredOnly,
                initialSplitParts = item.splitParts,
                initialMultithreadingParts = item.multithreadingParts,
                initialSpeedLimitAmount = initialSpeedLimitAmount,
                initialSpeedLimitUnit = initialSpeedLimitUnit,
                fragmentManager = supportFragmentManager,
                onLaunchFolderPicker = { onPicked -> launchManualFolderPicker(onPicked) },
                onDismiss = dismiss,
                onSubmit = { submission, _, _ ->
                    performRedownload(
                        item = item,
                        filename = submission.filename,
                        retryEnabled = submission.retryEnabled,
                        unmeteredOnly = submission.unmeteredOnly,
                        splitParts = submission.splitParts,
                        multithreadingParts = submission.multithreadingParts,
                        speedLimitBytesPerSec = submission.speedLimitBytesPerSec,
                        locationMode = submission.locationMode,
                        customLocationUri = submission.customLocationUri,
                        categorizeEnabled = submission.categorizeEnabled,
                        startPaused = submission.startPaused,
                        onDismiss = {
                            this@showRedownloadDialog.showClintSnackbar(
                                message = getString(
                                    if (submission.startPaused) R.string.download_added_paused else R.string.toast_downloading,
                                    submission.filename
                                ),
                                actionLabel = getString(R.string.download_started_view_action),
                                onAction = { DownloadsActivity.open(this@showRedownloadDialog) }
                            )
                            dismiss()
                            ClintDownloadManager.remove(this@showRedownloadDialog, item.id, true)
                            lastRefreshMs = 0L
                        }
                    )
                }
            )
        }
    }
}

private fun DownloadsActivity.copyRedownloadLink(url: String) {
    val clipboard = getSystemService(android.content.ClipboardManager::class.java)
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText(getString(R.string.download_dialog_link_clip_label), url))
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, getString(R.string.download_dialog_link_copied), Toast.LENGTH_SHORT).show()
    }
}

private fun DownloadsActivity.performRedownload(
    item: DownloadItem,
    filename: String,
    retryEnabled: Boolean,
    unmeteredOnly: Boolean,
    splitParts: Int,
    multithreadingParts: Int,
    speedLimitBytesPerSec: Long,
    locationMode: String,
    customLocationUri: String?,
    categorizeEnabled: Boolean,
    startPaused: Boolean,
    onDismiss: () -> Unit
) {
    fun startRedownload(effectiveUnmeteredOnly: Boolean) {
        if (DownloadFileHelper.isCustomLocationAccessible(this, locationMode, customLocationUri)) onDismiss()
        if (item.isStream) {
            ClintDownloadManager.enqueueStreamFromItem(this, item, filename, speedLimitBytesPerSec, locationMode, customLocationUri, categorizeEnabled, startPaused)
            return
        }
        val mega = item.mega
        if (mega != null) {
            ClintDownloadManager.enqueueMega(
                this, listOf(MegaDownloadEntry(mega, filename, item.totalBytes, item.url)), item.userAgent,
                retryEnabled, effectiveUnmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec,
                locationMode, customLocationUri, categorizeEnabled = categorizeEnabled, startPaused = startPaused
            )
            return
        }
        ClintDownloadManager.enqueue(this, item.url, filename, item.userAgent, item.referer, item.cookies, retryEnabled, effectiveUnmeteredOnly, splitParts, multithreadingParts, speedLimitBytesPerSec, locationMode, customLocationUri, categorizeEnabled = categorizeEnabled, pageUrl = item.pageUrl, startPaused = startPaused)
    }
    if (startPaused) {
        startRedownload(unmeteredOnly)
        return
    }
    confirmMeteredWarningThenProceed(unmeteredOnly) { effectiveUnmeteredOnly ->
        startRedownload(effectiveUnmeteredOnly)
    }
}
