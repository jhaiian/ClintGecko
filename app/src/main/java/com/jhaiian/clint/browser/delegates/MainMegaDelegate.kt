package com.jhaiian.clint.browser.delegates

import android.content.Intent
import android.net.Uri
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.webview.ClintWebView
import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.downloads.DEFAULT_SPEED_LIMIT_UNIT
import com.jhaiian.clint.downloads.DownloadCategories
import com.jhaiian.clint.downloads.DownloadFileHelper
import com.jhaiian.clint.downloads.DownloadsActivity
import com.jhaiian.clint.downloads.MegaDownloadEntry
import com.jhaiian.clint.downloads.mega.MegaDownloadDialog
import com.jhaiian.clint.downloads.mega.MegaFileInfo
import com.jhaiian.clint.downloads.mega.MegaFolderSubmission
import com.jhaiian.clint.downloads.mega.MegaLink
import com.jhaiian.clint.downloads.mega.MegaSource
import com.jhaiian.clint.downloads.resolveSpeedLimitBytesPerSec
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import com.jhaiian.clint.ui.showClintSnackbar
import com.jhaiian.clint.ui.theme.ClintComposeTheme
import java.util.WeakHashMap

private val offeredMegaLinks = WeakHashMap<ClintWebView, String>()

internal fun MainActivity.maybeOfferMegaDownload(webView: ClintWebView, url: String) {
    val link = MegaLink.parse(url)
    if (link == null) {
        offeredMegaLinks.remove(webView)
        return
    }
    if (tabManager.activeTab?.webView !== webView) return
    if (offeredMegaLinks[webView] == link.identity) return
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    if (!prefs.getBoolean(DownloadSettingsKeys.PREF_DETECT_MEGA_LINKS, DownloadSettingsKeys.DEFAULT_DETECT_MEGA_LINKS)) return
    val manager = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_MANAGER, DownloadSettingsKeys.DEFAULT_DOWNLOAD_MANAGER)
    if (manager != null && manager != DownloadSettingsKeys.DEFAULT_DOWNLOAD_MANAGER) return
    if (overlayContent != null) return
    offeredMegaLinks[webView] = link.identity
    showMegaDownload(link, webView.settings.userAgentString, allowQuick = false)
}

internal fun MainActivity.showMegaDownload(link: MegaLink, userAgent: String, allowQuick: Boolean = true) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val theme = prefs.getString("app_theme", "system") ?: "system"
    val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
    val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)
    val locationMode = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_LOCATION_MODE, DownloadSettingsKeys.MODE_DEFAULT) ?: DownloadSettingsKeys.MODE_DEFAULT
    val customUri = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, null)?.let { Uri.parse(it) }
    val unmeteredOnly = prefs.getBoolean(DownloadSettingsKeys.PREF_UNMETERED_ONLY, DownloadSettingsKeys.DEFAULT_UNMETERED_ONLY)

    overlayContent = {
        ClintComposeTheme(theme = theme) {
            MegaDownloadDialog(
                hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                link = link,
                userAgent = userAgent,
                initialLocationMode = locationMode,
                initialCustomUri = customUri,
                initialUnmeteredOnly = unmeteredOnly,
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
                onFileResolved = { info ->
                    if (link is MegaLink.File) showMegaFileDialog(link, info, userAgent, allowQuick)
                },
                onDownloadFolder = { submission ->
                    overlayContent = null
                    startMegaFolderDownload(submission, userAgent)
                },
                onDismiss = { overlayContent = null }
            )
        }
    }
}

private fun MainActivity.showMegaFileDialog(link: MegaLink.File, info: MegaFileInfo, userAgent: String, allowQuick: Boolean) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    mountDownloadRequestDialog(
        allowQuick = allowQuick,
        url = link.url,
        onCopyLink = { copyDownloadRequestLink(link.url) },
        initialFilename = info.name,
        contentLengthBytes = info.size,
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
        onSubmit = { submission, dismiss, _ ->
            dismiss()
            ClintDownloadManager.enqueueMega(
                this,
                listOf(MegaDownloadEntry(MegaSource(link.url, link.handle, link.key), submission.filename, info.size, info.downloadUrl)),
                userAgent,
                submission.retryEnabled, submission.unmeteredOnly, submission.splitParts, submission.multithreadingParts,
                submission.speedLimitBytesPerSec, submission.locationMode, submission.customLocationUri,
                submission.scheduledStartAtMillis, submission.categorizeEnabled,
                startPaused = submission.startPaused
            )
            if (submission.startPaused) showDownloadAddedSnackbar(submission.filename)
            else showDownloadStartedSnackbar(submission.filename, submission.locationMode, submission.customLocationUri)
        }
    )
}

private fun MainActivity.startMegaFolderDownload(submission: MegaFolderSubmission, userAgent: String) {
    if (submission.entries.isEmpty()) {
        if (submission.skipped > 0) showClintSnackbar(message = getString(R.string.mega_all_skipped))
        return
    }
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    ClintDownloadManager.enqueueMega(
        this, submission.entries, userAgent,
        retryEnabled = prefs.getBoolean(DownloadSettingsKeys.PREF_RETRY_ENABLED, DownloadSettingsKeys.DEFAULT_RETRY_ENABLED),
        unmeteredOnly = submission.unmeteredOnly,
        splitParts = prefs.getInt(DownloadSettingsKeys.PREF_SPLIT_PARTS, DownloadSettingsKeys.DEFAULT_SPLIT_PARTS),
        multithreadingParts = prefs.getInt(DownloadSettingsKeys.PREF_MULTITHREADING_PARTS, DownloadSettingsKeys.DEFAULT_MULTITHREADING_PARTS),
        speedLimitBytesPerSec = resolveSpeedLimitBytesPerSec(
            this,
            prefs.getInt(DownloadSettingsKeys.PREF_SPEED_LIMIT_AMOUNT, DownloadSettingsKeys.DEFAULT_SPEED_LIMIT_AMOUNT),
            prefs.getString(DownloadSettingsKeys.PREF_SPEED_LIMIT_UNIT, DEFAULT_SPEED_LIMIT_UNIT) ?: DEFAULT_SPEED_LIMIT_UNIT
        ),
        locationMode = submission.locationMode,
        customLocationUri = submission.customLocationUri,
        categorizeEnabled = DownloadCategories.isEnabled(this),
        startPaused = submission.startPaused
    )
    if (DownloadFileHelper.isCustomLocationAccessible(this, submission.locationMode, submission.customLocationUri)) {
        val count = submission.entries.size
        showClintSnackbar(
            message = resources.getQuantityString(
                if (submission.startPaused) R.plurals.mega_folder_added_paused else R.plurals.mega_folder_started, count, count
            ),
            actionLabel = getString(R.string.download_started_view_action),
            onAction = { DownloadsActivity.open(this) }
        )
    }
}
