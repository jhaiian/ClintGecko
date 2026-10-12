package com.jhaiian.clint.downloads

import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.delegates.PREF_BATTERY_OPT_ASKED
import com.jhaiian.clint.mediacapture.download.StreamContainerFormat
import com.jhaiian.clint.mediacapture.download.StreamDownloadRequest
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import java.io.File

internal fun resolveFilename(url: String, contentDisposition: String, contentType: String): String {
    if (contentDisposition.isNotBlank()) {
        val cdFilename = DownloadFileHelper.extractFilenameFromContentDisposition(contentDisposition)
        if (!cdFilename.isNullOrBlank()) return cdFilename
    }
    val guessed = URLUtil.guessFileName(url, contentDisposition, contentType)
    if (!guessed.isNullOrBlank() && guessed != "downloadfile") return guessed
    val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(contentType)
    val nameFromUrl = url.substringAfterLast('/').substringBefore('?').substringBefore('#')
        .ifBlank { "download" }
    return if (ext != null && !nameFromUrl.contains('.')) "$nameFromUrl.$ext" else nameFromUrl
}

internal fun DownloadsActivity.performManualDownload(
    submission: ManualDownloadSubmission,
    userAgent: String,
    onDismiss: () -> Unit,
    onRename: () -> Unit
) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val pm = getSystemService(android.os.PowerManager::class.java)
    if (!prefs.getBoolean(PREF_BATTERY_OPT_ASKED, false) && !pm.isIgnoringBatteryOptimizations(packageName)) {
        prefs.edit().putBoolean(PREF_BATTERY_OPT_ASKED, true).apply()
        uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
            title = getString(R.string.battery_opt_rationale_title),
            message = getString(R.string.battery_opt_rationale_message),
            cancelable = false,
            positiveLabel = getString(R.string.action_allow),
            onPositive = {
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
                continueManualDownload(submission, userAgent, onDismiss, onRename)
            },
            negativeLabel = getString(R.string.action_not_now),
            onNegative = {
                continueManualDownload(submission, userAgent, onDismiss, onRename)
            }
        )
        return
    }
    continueManualDownload(submission, userAgent, onDismiss, onRename)
}

internal fun DownloadsActivity.confirmMeteredWarningThenProceed(
    unmeteredOnly: Boolean,
    onDecided: (effectiveUnmeteredOnly: Boolean) -> Unit
) {
    val cm = getSystemService(android.net.ConnectivityManager::class.java)
    val isMetered = cm?.isActiveNetworkMetered ?: false
    if (unmeteredOnly && isMetered) {
        uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
            title = getString(R.string.download_metered_warning_title),
            message = getString(R.string.download_metered_warning_message),
            positiveLabel = getString(R.string.action_yes),
            onPositive = { onDecided(false) },
            negativeLabel = getString(R.string.action_no),
            onNegative = { onDecided(true) },
            neutralLabel = getString(R.string.action_cancel)
        )
        return
    }
    onDecided(unmeteredOnly)
}

private fun DownloadsActivity.continueManualDownload(
    submission: ManualDownloadSubmission,
    userAgent: String,
    onDismiss: () -> Unit,
    onRename: () -> Unit
) {
    confirmMeteredWarningThenProceed(submission.unmeteredOnly) { effectiveUnmeteredOnly ->
        checkConflictAndEnqueueManual(submission.copy(unmeteredOnly = effectiveUnmeteredOnly), userAgent, onDismiss, onRename)
    }
}

private fun DownloadsActivity.checkConflictAndEnqueueManual(
    submission: ManualDownloadSubmission,
    userAgent: String,
    onDismiss: () -> Unit,
    onRename: () -> Unit
) {
    val existing = ClintDownloadManager.findActiveDownloadForUrl(submission.url)
    if (existing != null) {
        uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
            title = getString(R.string.download_already_active_title),
            message = getString(R.string.download_already_active_message, existing.filename),
            positiveLabel = getString(R.string.action_download_anyway),
            onPositive = {
                checkFilenameConflictAndEnqueueManual(submission, userAgent, onDismiss, onRename)
            },
            negativeLabel = getString(R.string.action_cancel)
        )
        return
    }
    checkFilenameConflictAndEnqueueManual(submission, userAgent, onDismiss, onRename)
}

private fun DownloadsActivity.enqueueManualDownload(
    submission: ManualDownloadSubmission,
    userAgent: String,
    onDismiss: () -> Unit
) {
    if (DownloadFileHelper.isCustomLocationAccessible(this, submission.locationMode, submission.customLocationUri)) onDismiss()
    if (submission.isStream) {
        val request = StreamDownloadRequest(
            format = submission.streamFormat ?: StreamContainerFormat.HLS,
            videoUrl = submission.streamVideoUrl,
            audioUrl = submission.streamAudioUrl,
            subtitleUrl = null,
            pageUrl = "",
            referer = "",
            cookies = "",
            userAgent = userAgent,
            filename = submission.filename,
            estimatedTotalBytes = submission.estimatedTotalBytes,
            speedLimitBytesPerSec = submission.speedLimitBytesPerSec,
            concurrentSegments = submission.concurrentSegments,
            convertTsToMp4 = submission.convertTsToMp4
        )
        ClintDownloadManager.enqueueStream(
            context = this,
            request = request,
            videoWidth = submission.streamVideoWidth,
            videoHeight = submission.streamVideoHeight,
            videoBandwidth = submission.streamVideoBandwidth,
            audioBandwidth = submission.streamAudioBandwidth,
            primaryIsAudio = submission.streamPrimaryIsAudio,
            locationMode = submission.locationMode,
            customLocationUri = submission.customLocationUri,
            categorizeEnabled = submission.categorizeEnabled
        )
        return
    }
    ClintDownloadManager.enqueue(
        this, submission.url, submission.filename, userAgent, "", "",
        submission.retryEnabled, submission.unmeteredOnly, submission.splitParts, submission.multithreadingParts,
        submission.speedLimitBytesPerSec, submission.locationMode, submission.customLocationUri, submission.scheduledStartAtMillis,
        submission.categorizeEnabled
    )
}

private fun DownloadsActivity.checkFilenameConflictAndEnqueueManual(
    submission: ManualDownloadSubmission,
    userAgent: String,
    onDismiss: () -> Unit,
    onRename: () -> Unit
) {
    val isSaf = submission.locationMode == DownloadSettingsKeys.MODE_CUSTOM
    val fileExists = if (isSaf) {
        val treeUri = submission.customLocationUri?.let { Uri.parse(it) } ?: DownloadFileHelper.getSafTreeUri(this)
        val docDir = treeUri?.let { DocumentFile.fromTreeUri(this, it) }
        val targetDir = docDir?.let { DownloadCategories.findExistingSafDir(submission.categorizeEnabled, it, submission.filename) }
        targetDir?.findFile(submission.filename) != null
    } else {
        val targetDir = DownloadCategories.resolveDir(submission.categorizeEnabled, DownloadFileHelper.resolveDownloadDir(), submission.filename)
        File(targetDir, submission.filename).exists()
    }
    if (!fileExists) {
        enqueueManualDownload(submission, userAgent, onDismiss)
        return
    }
    uiState.conflictDialogRequest = DownloadConflictDialogRequest(
        onAddDuplicate = {
            enqueueManualDownload(submission, userAgent, onDismiss)
        },
        onOverride = {
            deleteExistingManual(submission.filename, submission.locationMode, submission.customLocationUri, submission.categorizeEnabled)
            enqueueManualDownload(submission, userAgent, onDismiss)
        },
        onRename = onRename
    )
}

private fun DownloadsActivity.deleteExistingManual(
    filename: String,
    locationMode: String,
    customLocationUri: String?,
    categorizeEnabled: Boolean
) {
    val matchingIds = ClintDownloadManager.downloadsFlow.value.filter { it.filename == filename }.map { it.id }
    matchingIds.forEach { ClintDownloadManager.remove(this, it, deleteFile = true) }
    val isSaf = locationMode == DownloadSettingsKeys.MODE_CUSTOM
    if (isSaf) {
        val treeUri = customLocationUri?.let { Uri.parse(it) } ?: DownloadFileHelper.getSafTreeUri(this)
        val docDir = treeUri?.let { DocumentFile.fromTreeUri(this, it) }
        val targetDir = docDir?.let { DownloadCategories.findExistingSafDir(categorizeEnabled, it, filename) }
        targetDir?.findFile(filename)?.delete()
    } else {
        val targetDir = DownloadCategories.resolveDir(categorizeEnabled, DownloadFileHelper.resolveDownloadDir(), filename)
        File(targetDir, filename).delete()
    }
}
