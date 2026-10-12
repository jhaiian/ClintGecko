package com.jhaiian.clint.downloads

import com.jhaiian.clint.R

import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.settings.common.dialogSectionBackground
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintDialogActionFooter
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val SIZE_TOLERANCE_MIN_BYTES = 64L * 1024
private const val SIZE_TOLERANCE_MAX_BYTES = 10L * 1024 * 1024
private const val SIZE_TOLERANCE_FRACTION = 0.001

private fun sizesWithinTolerance(a: Long, b: Long): Boolean {
    val diff = kotlin.math.abs(a - b)
    val tolerance = (maxOf(a, b) * SIZE_TOLERANCE_FRACTION).toLong()
        .coerceIn(SIZE_TOLERANCE_MIN_BYTES, SIZE_TOLERANCE_MAX_BYTES)
    return diff <= tolerance
}

@Composable
fun DownloadUpdateLinkDialog(item: DownloadItem, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalClintColors.current

    var text by remember(item.id) { mutableStateOf(item.url) }
    var checking by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var helperText by remember { mutableStateOf<String?>(null) }
    var verifiedUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(text) {
        verifiedUrl = null
        val typed = text.trim()
        if (typed.isEmpty()) {
            checking = false
            errorText = null
            helperText = null
            return@LaunchedEffect
        }
        errorText = null
        helperText = null
        checking = true
        delay(600)
        if (item.isStream) {
            val isValidManifest = withContext(Dispatchers.IO) {
                try {
                    val request = okhttp3.Request.Builder().url(typed).get().build()
                    val response = ClintDownloadManager.httpClient.newCall(request).execute()
                    val body = if (response.isSuccessful) response.body.string() else null
                    response.close()
                    when {
                        body == null -> null
                        item.streamFormat.equals("DASH", true) -> body.contains("<MPD", ignoreCase = true)
                        else -> body.trimStart().startsWith("#EXTM3U")
                    }
                } catch (e: Throwable) {
                    null
                }
            }
            checking = false
            when (isValidManifest) {
                null -> {
                    helperText = null
                    errorText = context.getString(R.string.download_update_link_dialog_fetch_failed)
                }
                false -> {
                    helperText = null
                    errorText = context.getString(R.string.download_update_link_dialog_not_a_manifest)
                }
                true -> {
                    errorText = null
                    helperText = null
                    verifiedUrl = typed
                }
            }
            return@LaunchedEffect
        }
        val remoteSize = withContext(Dispatchers.IO) {
            try {
                var size = -1L
                val headRequest = okhttp3.Request.Builder().url(typed).head().build()
                val headResponse = ClintDownloadManager.httpClient.newCall(headRequest).execute()
                size = headResponse.header("Content-Length")?.toLongOrNull() ?: -1L
                headResponse.close()
                if (size < 0) {
                    val rangeRequest = okhttp3.Request.Builder().url(typed).get()
                        .header("Range", "bytes=0-0").build()
                    val rangeResponse = ClintDownloadManager.httpClient.newCall(rangeRequest).execute()
                    val contentRange = rangeResponse.header("Content-Range")
                    if (contentRange != null) {
                        size = contentRange.substringAfterLast("/").trim().toLongOrNull() ?: -1L
                    }
                    if (size < 0) {
                        size = rangeResponse.header("Content-Length")?.toLongOrNull() ?: -1L
                    }
                    rangeResponse.body.close()
                    rangeResponse.close()
                }
                size
            } catch (e: Throwable) {
                null
            }
        }
        checking = false
        when {
            remoteSize == null -> {
                helperText = null
                errorText = context.getString(R.string.download_update_link_dialog_fetch_failed)
            }
            remoteSize < 0 -> {
                errorText = null
                helperText = context.getString(R.string.download_update_link_dialog_size_unverifiable)
                verifiedUrl = typed
            }
            item.totalBytes <= 0 || sizesWithinTolerance(remoteSize, item.totalBytes) -> {
                errorText = null
                helperText = null
                verifiedUrl = typed
            }
            else -> {
                helperText = null
                errorText = context.getString(R.string.download_update_link_dialog_size_mismatch, remoteSize, item.totalBytes)
            }
        }
    }

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
                positiveEnabled = verifiedUrl != null,
                onPositive = {
                    val url = verifiedUrl ?: return@ClintDialogActionFooter
                    ClintDownloadManager.updateDownloadUrl(item.id, url)
                    onDismiss()
                }
            )
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            SettingsSection(colors.dialogSectionBackground) {
                Column(Modifier.padding(16.dp)) {
                    ClintOutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        singleLine = true,
                        isError = errorText != null,
                        trailingIcon = if (checking) {
                            { CircularProgressIndicator(modifier = Modifier.size(20.dp), color = colors.primary, strokeWidth = 2.dp) }
                        } else null,
                        supportingText = when {
                            errorText != null -> { { Text(errorText!!, color = colors.colorError) } }
                            helperText != null -> { { Text(helperText!!, color = colors.secondaryText) } }
                            else -> null
                        }
                    )
                }
            }
        }
    }
}
