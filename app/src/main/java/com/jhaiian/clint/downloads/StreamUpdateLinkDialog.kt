package com.jhaiian.clint.downloads

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.R
import com.jhaiian.clint.mediacapture.download.StreamDownloadJob
import com.jhaiian.clint.mediacapture.download.StreamRequestHeaders
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.settings.common.dialogSectionBackground
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintDialogActionFooter
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private data class LinkCheck(val checking: Boolean = false, val error: String? = null, val verified: Boolean = false)

private fun verifyStreamLink(context: Context, item: DownloadItem, url: String, companion: Boolean): LinkCheck {
    val fetchFailed = LinkCheck(error = context.getString(R.string.download_update_link_dialog_fetch_failed))
    val body = try {
        val builder = okhttp3.Request.Builder().url(url).get()
        StreamRequestHeaders.apply(builder, url, item.streamPageUrl, item.cookies, item.userAgent, item.streamHeaders)
        ClintDownloadManager.httpClient.newCall(builder.build()).execute().use { response ->
            if (response.isSuccessful) response.body.string() else null
        }
    } catch (e: Throwable) {
        null
    } ?: return fetchFailed
    val validManifest = if (item.streamFormat.equals("DASH", true)) body.contains("<MPD", ignoreCase = true)
    else body.trimStart().startsWith("#EXTM3U")
    if (!validManifest) return LinkCheck(error = context.getString(R.string.download_update_link_dialog_not_a_manifest))
    val savedTotal = StreamDownloadJob.savedSegmentTotal(context, item.id, if (companion) "audio" else "video")
    if (savedTotal != null) {
        val track = StreamDownloadJob.probeTrack(item, url, companion) ?: return fetchFailed
        val total = track.segments.size + if (track.initSegment != null) 1 else 0
        if (total != savedTotal) {
            return LinkCheck(error = context.getString(R.string.download_update_link_dialog_segments_mismatch, total, savedTotal))
        }
    }
    return LinkCheck(verified = true)
}

@Composable
private fun rememberLinkCheck(text: String, original: String, item: DownloadItem, companion: Boolean): LinkCheck {
    val context = LocalContext.current
    var check by remember { mutableStateOf(LinkCheck(verified = true)) }
    LaunchedEffect(text) {
        val typed = text.trim()
        if (typed.isEmpty() || typed == original) {
            check = LinkCheck(verified = typed == original)
            return@LaunchedEffect
        }
        check = LinkCheck(checking = true)
        delay(600)
        check = withContext(Dispatchers.IO) { verifyStreamLink(context, item, typed, companion) }
    }
    return check
}

@Composable
fun StreamUpdateLinkDialog(item: DownloadItem, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val originalPrimary = item.streamVideoUrl.ifBlank { item.url }
    val originalAudio = item.streamAudioUrl.takeIf { !item.streamPrimaryIsAudio }
    val hasAudio = originalAudio != null

    var primaryText by remember(item.id) { mutableStateOf(originalPrimary) }
    var audioText by remember(item.id) { mutableStateOf(originalAudio.orEmpty()) }
    val primaryCheck = rememberLinkCheck(primaryText, originalPrimary, item, false)
    val audioCheck = if (hasAudio) rememberLinkCheck(audioText, originalAudio.orEmpty(), item, true) else LinkCheck(verified = true)

    val primaryChanged = primaryText.trim() != originalPrimary
    val audioChanged = hasAudio && audioText.trim() != originalAudio
    val canUpdate = (primaryChanged || audioChanged) &&
        !primaryCheck.checking && !audioCheck.checking &&
        primaryCheck.verified && audioCheck.verified

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ClintDialog(
        title = stringResource(R.string.download_update_link_dialog_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            ClintDialogActionFooter(
                onCancel = onDismiss,
                positiveLabel = stringResource(R.string.download_update_link_dialog_positive),
                positiveEnabled = canUpdate,
                onPositive = {
                    ClintDownloadManager.updateStreamUrls(
                        item.id,
                        primaryUrl = primaryText.trim().takeIf { primaryChanged },
                        audioUrl = audioText.trim().takeIf { audioChanged }
                    )
                    onDismiss()
                }
            )
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            SettingsSection(colors.dialogSectionBackground) {
                Column(Modifier.padding(16.dp)) {
                    ClintOutlinedTextField(
                        value = primaryText,
                        onValueChange = { primaryText = it },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        singleLine = true,
                        label = if (hasAudio) { { Text(stringResource(R.string.download_update_link_dialog_video_label)) } } else null,
                        isError = primaryCheck.error != null,
                        trailingIcon = if (primaryCheck.checking) {
                            { CircularProgressIndicator(modifier = Modifier.size(20.dp), color = colors.primary, strokeWidth = 2.dp) }
                        } else null,
                        supportingText = primaryCheck.error?.let { message -> { Text(message, color = colors.colorError) } }
                    )
                    if (hasAudio) {
                        Spacer(Modifier.height(12.dp))
                        ClintOutlinedTextField(
                            value = audioText,
                            onValueChange = { audioText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(stringResource(R.string.download_update_link_dialog_audio_label)) },
                            isError = audioCheck.error != null,
                            trailingIcon = if (audioCheck.checking) {
                                { CircularProgressIndicator(modifier = Modifier.size(20.dp), color = colors.primary, strokeWidth = 2.dp) }
                            } else null,
                            supportingText = audioCheck.error?.let { message -> { Text(message, color = colors.colorError) } }
                        )
                    }
                }
            }
        }
    }
}
