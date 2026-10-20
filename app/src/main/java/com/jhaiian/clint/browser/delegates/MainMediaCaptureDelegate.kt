package com.jhaiian.clint.browser.delegates

import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.downloads.DownloadConflictDialogRequest
import com.jhaiian.clint.downloads.DownloadFileHelper
import com.jhaiian.clint.downloads.DownloadItem
import com.jhaiian.clint.downloads.DownloadRequestSubmission
import com.jhaiian.clint.downloads.DownloadStatus
import com.jhaiian.clint.downloads.DownloadsActivity
import com.jhaiian.clint.mediacapture.DetectedMedia
import com.jhaiian.clint.mediacapture.MEDIA_CAPTURE_CONVERT_TS_TO_MP4_DEFAULT
import com.jhaiian.clint.mediacapture.MEDIA_CAPTURE_CONVERT_TS_TO_MP4_PREF
import com.jhaiian.clint.mediacapture.MediaCaptureDialog
import com.jhaiian.clint.mediacapture.MediaKind
import com.jhaiian.clint.mediacapture.download.HlsSubtitleResolver
import com.jhaiian.clint.mediacapture.download.StreamContainerFormat
import com.jhaiian.clint.mediacapture.download.StreamDownloadRequest
import com.jhaiian.clint.mediacapture.isMediaCaptureManifestBased
import com.jhaiian.clint.mediacapture.audioTrackName
import com.jhaiian.clint.mediacapture.compatibleAudioTracks
import com.jhaiian.clint.mediacapture.pickSoleSubtitle
import com.jhaiian.clint.mediacapture.suggestMediaCaptureFilename
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import com.jhaiian.clint.ui.showClintSnackbar
import com.jhaiian.clint.ui.theme.ClintComposeTheme
import com.jhaiian.clint.util.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun MainActivity.mountMediaCaptureDialog() {
    val tabId = tabManager.activeTab?.id ?: return
    val pageUrl = tabManager.activeTab?.webView?.url ?: ""
    val pageTitle = tabManager.activeTab?.title?.takeIf { it.isNotBlank() } ?: pageUrl
    val theme = prefs.getString("app_theme", "system") ?: "system"
    val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
    val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)
    tabManager.activeTab?.webView?.rescanMedia()

    overlayContent = {
        ClintComposeTheme(theme = theme) {
            MediaCaptureDialog(
                tabId = tabId,
                hideStatusBar = hideStatusBar,
                hideSystemNavigation = hideSystemNavigation,
                onDismiss = { overlayContent = null },
                onCopyLink = { text -> copyMediaCaptureLink(text) },
                onPreviewMediaStarted = {
                    tabManager.activeTab?.webView?.evaluateJavascript(com.jhaiian.clint.browser.webview.TabMediaControl.PAUSE_SCRIPT, null)
                },
                onDownload = { media, audio, allItems, estimatedBytes, containerExtension ->
                    showMediaCaptureDownloadDialog(media, audio, pageUrl, pageTitle, allItems, estimatedBytes, containerExtension)
                }
            )
        }
    }
}

private fun MainActivity.copyMediaCaptureLink(text: String) {
    val clipboard = getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.media_capture_link_clip_label), text))
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, getString(R.string.media_capture_link_copied), Toast.LENGTH_SHORT).show()
    }
}

private fun MainActivity.showMediaCaptureDownloadDialog(
    media: DetectedMedia,
    audio: DetectedMedia?,
    pageUrl: String,
    pageTitle: String,
    allItems: List<DetectedMedia>,
    estimatedBytes: Long?,
    containerExtension: String?
) {
    if (media.kind == MediaKind.SUBTITLE && !media.isHlsPlaylist && media.cueCount == null) {
        val userAgent = com.jhaiian.clint.browser.webview.ClintGecko.defaultUserAgent()
        lifecycleScope.launch {
            val isPlaylist = withContext(Dispatchers.IO) {
                HlsSubtitleResolver.sniffPlaylist(media.url, pageUrl, userAgent, media.requestHeaders)
            }
            showMediaCaptureDownloadDialogReady(
                media.copy(isHlsPlaylist = isPlaylist), audio, pageUrl, pageTitle, allItems, estimatedBytes, containerExtension
            )
        }
        return
    }
    showMediaCaptureDownloadDialogReady(media, audio, pageUrl, pageTitle, allItems, estimatedBytes, containerExtension)
}

private fun MainActivity.showMediaCaptureDownloadDialogReady(
    media: DetectedMedia,
    audio: DetectedMedia?,
    pageUrl: String,
    pageTitle: String,
    allItems: List<DetectedMedia>,
    estimatedBytes: Long?,
    containerExtension: String?
) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val userAgent = com.jhaiian.clint.browser.webview.ClintGecko.defaultUserAgent()
    val isManifestBased = isMediaCaptureManifestBased(media)
    val isHlsSubtitle = media.kind == MediaKind.SUBTITLE && media.isHlsPlaylist
    val isStreamJob = isManifestBased || isHlsSubtitle
    val isAudioPrimary = media.kind == MediaKind.AUDIO
    val convertTsToMp4 = prefs.getBoolean(MEDIA_CAPTURE_CONVERT_TS_TO_MP4_PREF, MEDIA_CAPTURE_CONVERT_TS_TO_MP4_DEFAULT)
    val pairedAudio = if (isManifestBased && !isAudioPrimary) audio else null
    val realExtension = if (isManifestBased) {
        when {
            pairedAudio != null -> "mp4"
            isAudioPrimary -> when (containerExtension) {
                "mp4" -> "m4a"
                "ts", null -> if (convertTsToMp4) "aac" else "ts"
                else -> containerExtension
            }
            containerExtension == null || containerExtension == "mp4" || convertTsToMp4 -> "mp4"
            else -> containerExtension
        }
    } else null
    val baseFilename = suggestMediaCaptureFilename(media, pageTitle, realExtension)
    val filename = if (pairedAudio != null && compatibleAudioTracks(media, allItems).size > 1) {
        val audioSuffix = audioTrackName(pairedAudio)?.replace(Regex("[/\\\\:*?\"<>|]"), "_")?.trim()
        val dot = baseFilename.lastIndexOf('.')
        if (audioSuffix.isNullOrBlank()) baseFilename
        else if (dot > 0) "${baseFilename.substring(0, dot)} ($audioSuffix)${baseFilename.substring(dot)}"
        else "$baseFilename ($audioSuffix)"
    } else baseFilename
    val knownLengthBytes = media.sizeBytes?.takeIf { it > 0L }
    val fileSizeDisplayOverride = if (isManifestBased && knownLengthBytes == null) {
        estimatedBytes?.takeIf { it > 0L }?.let { getString(R.string.download_dialog_file_size_estimated, formatFileSize(it)) }
    } else null

    mountDownloadRequestDialog(
        url = media.url,
        onCopyLink = { copyMediaCaptureLink(media.url) },
        initialFilename = filename,
        contentLengthBytes = knownLengthBytes ?: estimatedBytes ?: -1L,
        fileSizeDisplayOverride = fileSizeDisplayOverride,
        fetchUrl = if (!isStreamJob && knownLengthBytes == null) media.url else null,
        fetchUserAgent = userAgent,
        checkStorage = true,
        showOptions = true,
        showStorageInfo = true,
        showSplitAndMultithreading = !isStreamJob,
        showConcurrentSegments = isManifestBased,
        initialConcurrentSegments = prefs.getInt(DownloadSettingsKeys.PREF_STREAM_CONCURRENT_SEGMENTS, DownloadSettingsKeys.DEFAULT_STREAM_CONCURRENT_SEGMENTS),
        initialLocationMode = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_LOCATION_MODE, DownloadSettingsKeys.MODE_DEFAULT) ?: DownloadSettingsKeys.MODE_DEFAULT,
        initialCustomUri = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, null)?.let { Uri.parse(it) },
        initialRetryEnabled = prefs.getBoolean(DownloadSettingsKeys.PREF_RETRY_ENABLED, DownloadSettingsKeys.DEFAULT_RETRY_ENABLED),
        initialUnmeteredOnly = prefs.getBoolean(DownloadSettingsKeys.PREF_UNMETERED_ONLY, DownloadSettingsKeys.DEFAULT_UNMETERED_ONLY),
        initialSplitParts = prefs.getInt(DownloadSettingsKeys.PREF_SPLIT_PARTS, DownloadSettingsKeys.DEFAULT_SPLIT_PARTS),
        initialMultithreadingParts = prefs.getInt(DownloadSettingsKeys.PREF_MULTITHREADING_PARTS, DownloadSettingsKeys.DEFAULT_MULTITHREADING_PARTS),
        initialSpeedLimitAmount = prefs.getInt(DownloadSettingsKeys.PREF_SPEED_LIMIT_AMOUNT, DownloadSettingsKeys.DEFAULT_SPEED_LIMIT_AMOUNT),
        initialSpeedLimitUnit = prefs.getString(DownloadSettingsKeys.PREF_SPEED_LIMIT_UNIT, com.jhaiian.clint.downloads.DEFAULT_SPEED_LIMIT_UNIT) ?: com.jhaiian.clint.downloads.DEFAULT_SPEED_LIMIT_UNIT,
        onSubmit = { submission, dismiss, onRename ->
            fun proceed() {
                if (isStreamJob) {
                    if (DownloadFileHelper.isCustomLocationAccessible(this, submission.locationMode, submission.customLocationUri)) {
                        showClintSnackbar(
                            message = getString(R.string.toast_downloading, submission.filename),
                            actionLabel = getString(R.string.download_started_view_action),
                            onAction = { DownloadsActivity.open(this) }
                        )
                    }
                    dismiss()
                    if (isHlsSubtitle) {
                        enqueueMediaCaptureHlsSubtitle(media, pageUrl, userAgent, submission)
                    } else {
                        enqueueMediaCaptureStream(media, pairedAudio, allItems, pageUrl, userAgent, submission, knownLengthBytes ?: estimatedBytes ?: 0L, convertTsToMp4)
                    }
                } else {
                    initiateDownload(
                        media.url, submission.filename,
                        mediaCaptureRequestHeader(media, "User-Agent") ?: userAgent,
                        mediaCaptureRequestHeader(media, "Referer") ?: pageUrl,
                        mediaCaptureCookies(media),
                        submission.retryEnabled, submission.unmeteredOnly, submission.splitParts, submission.multithreadingParts, submission.speedLimitBytesPerSec,
                        submission.locationMode, submission.customLocationUri, submission.categorizeEnabled, submission.scheduledStartAtMillis,
                        onDismiss = dismiss,
                        onRename = onRename,
                        onStarted = { showDownloadStartedSnackbar(submission.filename, submission.locationMode, submission.customLocationUri) }
                    )
                }
            }

            val existing = findExistingMediaCaptureDownload(media, pairedAudio)
            if (existing != null) {
                val isPending = existing.status in DownloadStatus.NOT_FINISHED
                uiState.conflictDialogRequest = DownloadConflictDialogRequest(
                    onAddDuplicate = { proceed() },
                    onOverride = {
                        ClintDownloadManager.remove(this, existing.id, deleteFile = true)
                        proceed()
                    },
                    onRename = onRename,
                    onUpdateLink = if (isPending) {
                        {
                            if (existing.isStream) ClintDownloadManager.updateStreamUrls(existing.id, media.url, pairedAudio?.url)
                            else ClintDownloadManager.updateDownloadUrl(existing.id, media.url)
                            dismiss()
                            showClintSnackbar(message = getString(R.string.media_capture_duplicate_link_updated))
                        }
                    } else null
                )
            } else {
                proceed()
            }
        }
    )
}

private fun MainActivity.findExistingMediaCaptureDownload(media: DetectedMedia, audio: DetectedMedia?): DownloadItem? =
    ClintDownloadManager.downloadsFlow.value.firstOrNull { item ->
        if (item.status == DownloadStatus.FAILED) return@firstOrNull false
        if (!item.isStream) return@firstOrNull item.url == media.url
        val primaryRepresentation = if (media.kind == MediaKind.AUDIO) item.streamVideoRepresentationId ?: item.streamAudioRepresentationId else item.streamVideoRepresentationId
        item.streamVideoUrl == media.url &&
            primaryRepresentation == media.representationId &&
            item.streamAudioUrl == audio?.url &&
            (audio == null || item.streamAudioRepresentationId == audio.representationId)
    }

private fun MainActivity.enqueueMediaCaptureStream(
    media: DetectedMedia,
    pairedAudio: DetectedMedia?,
    allItems: List<DetectedMedia>,
    pageUrl: String,
    userAgent: String,
    submission: DownloadRequestSubmission,
    estimatedTotalBytes: Long,
    convertTsToMp4: Boolean
) {
    val isDash = media.format.equals("DASH", true)
    val isAudioPrimary = media.kind == MediaKind.AUDIO
    val audio = if (!isAudioPrimary) pairedAudio else null
    val subtitle = pickSoleSubtitle(media, allItems)
    val headers = media.requestHeaders.ifEmpty { audio?.requestHeaders ?: emptyMap() }

    val request = StreamDownloadRequest(
        format = if (isDash) StreamContainerFormat.DASH else StreamContainerFormat.HLS,
        videoUrl = if (!isAudioPrimary) media.url else null,
        audioUrl = audio?.url ?: (if (isAudioPrimary) media.url else null),
        subtitleUrl = subtitle?.url,
        pageUrl = pageUrl,
        referer = pageUrl,
        cookies = "",
        userAgent = userAgent,
        filename = submission.filename,
        headers = headers,
        estimatedTotalBytes = estimatedTotalBytes,
        speedLimitBytesPerSec = submission.speedLimitBytesPerSec,
        concurrentSegments = submission.concurrentSegments,
        noAudio = !isAudioPrimary && media.hasAudio == false && audio == null,
        isLive = media.isLive,
        convertTsToMp4 = convertTsToMp4
    )
    ClintDownloadManager.enqueueStream(
        context = this,
        request = request,
        videoWidth = if (!isAudioPrimary) media.width else null,
        videoHeight = if (!isAudioPrimary) media.height else null,
        videoBandwidth = media.bandwidthBitsPerSec,
        audioBandwidth = audio?.bandwidthBitsPerSec ?: (if (isAudioPrimary) media.bandwidthBitsPerSec else null),
        primaryIsAudio = isAudioPrimary,
        locationMode = submission.locationMode,
        customLocationUri = submission.customLocationUri,
        categorizeEnabled = submission.categorizeEnabled,
        videoRepresentationId = media.representationId,
        audioRepresentationId = audio?.representationId ?: (if (isAudioPrimary) media.representationId else null),
        audioLanguage = (audio ?: media.takeIf { isAudioPrimary })?.language,
        audioLabel = audio?.let { audioTrackName(it) }
    )
}

private fun MainActivity.enqueueMediaCaptureHlsSubtitle(
    media: DetectedMedia,
    pageUrl: String,
    userAgent: String,
    submission: DownloadRequestSubmission
) {
    val request = StreamDownloadRequest(
        format = StreamContainerFormat.HLS,
        videoUrl = media.url,
        audioUrl = null,
        subtitleUrl = null,
        pageUrl = pageUrl,
        referer = pageUrl,
        cookies = "",
        userAgent = userAgent,
        filename = submission.filename,
        headers = media.requestHeaders,
        speedLimitBytesPerSec = submission.speedLimitBytesPerSec,
        concurrentSegments = submission.concurrentSegments,
        subtitleOnly = true
    )
    ClintDownloadManager.enqueueStream(
        context = this,
        request = request,
        videoWidth = null,
        videoHeight = null,
        videoBandwidth = null,
        audioBandwidth = null,
        locationMode = submission.locationMode,
        customLocationUri = submission.customLocationUri,
        categorizeEnabled = submission.categorizeEnabled
    )
}

private fun mediaCaptureRequestHeader(media: DetectedMedia, name: String): String? =
    media.requestHeaders.entries
        .firstOrNull { it.key.equals(name, ignoreCase = true) }
        ?.value
        ?.takeIf { it.isNotBlank() }

private fun mediaCaptureCookies(media: DetectedMedia): String =
    com.jhaiian.clint.profiles.WebProfiles.cookieForHeaders(media.requestHeaders, media.url)
