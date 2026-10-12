package com.jhaiian.clint.downloads

import com.jhaiian.clint.R

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.settings.common.dialogSectionBackground
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun DownloadPropertiesDialog(
    item: DownloadItem,
    tick: Long,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onShare: (DownloadItem) -> Unit,
    onOpen: (DownloadItem) -> Unit
) {
    val context = LocalContext.current
    val colors = LocalClintColors.current
    val scope = rememberCoroutineScope()

    val dash = stringResource(R.string.download_props_dash)
    val resolvedPath = remember(item.id, item.filename, item.contentUri, item.locationMode, item.customLocationUri, item.file) { resolvePropertiesPath(context, item, dash) }
    val totalBytesStr = if (item.totalBytes > 0) stringResource(R.string.download_props_size_format, formatFileSize(item.totalBytes), item.totalBytes) else dash
    val downloadedStr = if (item.bytesDownloaded > 0) stringResource(R.string.download_props_size_format, formatFileSize(item.bytesDownloaded), item.bytesDownloaded) else dash
    val activeElapsedSec = remember(item, tick) {
        val inProgress = if (item.activeStartedAt > 0L) System.currentTimeMillis() - item.activeStartedAt else 0L
        (item.activeElapsedMs + inProgress) / 1000L
    }
    val activeTimeStr = if (activeElapsedSec > 0) formatElapsed(activeElapsedSec) else dash
    val avgSpeedBytes = remember(item, tick) { item.averageSpeedBytesPerSec() }
    val avgSpeedStr = if (activeElapsedSec > 0 && item.bytesDownloaded > 0) stringResource(R.string.download_speed_only, formatFileSize(avgSpeedBytes)) else dash
    val dateAddedStr = if (item.startedAt > 0L) formatPropTimestamp(item.startedAt) else dash
    val completedAtResolved = remember(item.id, item.completedAt, item.status) {
        when {
            item.completedAt > 0L -> item.completedAt
            item.status == DownloadStatus.COMPLETE -> fallbackCompletedAt(context, item)
            else -> 0L
        }
    }
    val dateCompletedStr = if (completedAtResolved > 0L) formatPropTimestamp(completedAtResolved) else dash
    val yes = stringResource(R.string.download_props_yes)
    val no = stringResource(R.string.download_props_no)
    val canComputeHash = remember(item.file, item.status, tick) { item.file != null && item.file.exists() }
    val isComplete = item.status == DownloadStatus.COMPLETE

    fun copy(value: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("", value))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, context.getString(R.string.download_props_copied), Toast.LENGTH_SHORT).show()
        }
    }

    var md5 by remember(item.id, item.status, item.filename, item.file) { mutableStateOf<String?>(null) }
    var sha256 by remember(item.id, item.status, item.filename, item.file) { mutableStateOf<String?>(null) }
    var md5Computing by remember(item.id, item.status, item.filename, item.file) { mutableStateOf(false) }
    var sha256Computing by remember(item.id, item.status, item.filename, item.file) { mutableStateOf(false) }
    val checksumNa = stringResource(R.string.download_props_checksum_na)

    fun computeHash(algorithm: String, onDone: (String?) -> Unit) {
        val file = item.file ?: return
        scope.launch {
            withContext(Dispatchers.Default) {
                val hash = runCatching { computeFileHash(file, algorithm) }.getOrNull()
                withContext(Dispatchers.Main) { onDone(hash) }
            }
        }
    }

    ClintDialog(
        title = stringResource(R.string.download_props_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                if (isComplete) {
                    TextButton(onClick = { onDismiss(); onOpen(item) }) {
                        Text(stringResource(R.string.action_open), color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                }
                TextButton(onClick = { onDismiss(); onShare(item) }) {
                    Text(stringResource(R.string.download_menu_share), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            PropSection(stringResource(R.string.download_props_section_file))
            SettingsSection(colors.dialogSectionBackground) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    PropRow(stringResource(R.string.download_props_label_filename), item.filename) { copy(item.filename) }
                    PropRow(stringResource(R.string.download_props_label_path), resolvedPath) { copy(resolvedPath) }
                    PropRow(stringResource(R.string.download_props_label_size), totalBytesStr) { copy(totalBytesStr) }
                    PropRow(stringResource(R.string.download_props_label_downloaded), downloadedStr) { copy(downloadedStr) }
                }
            }

            PropSection(stringResource(R.string.download_props_section_source))
            SettingsSection(colors.dialogSectionBackground) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    PropRow(stringResource(R.string.download_props_label_url), item.url) { copy(item.url) }
                    if (item.referer.isNotEmpty()) {
                        PropRow(stringResource(R.string.download_props_label_page), item.referer) { copy(item.referer) }
                    }
                }
            }

            PropSection(stringResource(R.string.download_props_section_transfer))
            SettingsSection(colors.dialogSectionBackground) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    PropRow(stringResource(R.string.download_props_label_avg_speed), avgSpeedStr) { copy(avgSpeedStr) }
                    PropRow(stringResource(R.string.download_props_label_active_time), activeTimeStr) { copy(activeTimeStr) }
                    PropRow(stringResource(R.string.download_props_label_date_added), dateAddedStr) { copy(dateAddedStr) }
                    PropRow(stringResource(R.string.download_props_label_date_completed), dateCompletedStr) { copy(dateCompletedStr) }
                }
            }

            PropSection(stringResource(R.string.download_props_section_settings))
            SettingsSection(colors.dialogSectionBackground) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    PropRow(stringResource(R.string.download_props_label_unmetered), if (item.unmeteredOnly) yes else no, null)
                    PropRow(stringResource(R.string.download_props_label_resumable), if (item.resumable) yes else no, null)
                }
            }

            PropSection(stringResource(R.string.download_props_section_checksums))
            SettingsSection(colors.dialogSectionBackground) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    PropHashRow(
                        label = stringResource(R.string.download_props_label_md5),
                        value = md5,
                        computing = md5Computing,
                        canCompute = canComputeHash,
                        onCompute = { md5Computing = true; computeHash("MD5") { md5Computing = false; md5 = it ?: checksumNa } },
                        onCopy = { md5?.let { copy(it) } }
                    )
                    PropHashRow(
                        label = stringResource(R.string.download_props_label_sha256),
                        value = sha256,
                        computing = sha256Computing,
                        canCompute = canComputeHash,
                        onCompute = { sha256Computing = true; computeHash("SHA-256") { sha256Computing = false; sha256 = it ?: checksumNa } },
                        onCopy = { sha256?.let { copy(it) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun PropSection(title: String) {
    val colors = LocalClintColors.current
    Text(
        title, color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
    )
}

@Composable
private fun PropRow(label: String, value: String, onClick: (() -> Unit)?) {
    val colors = LocalClintColors.current
    Column(
        Modifier.fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 6.dp)
    ) {
        Text(label, color = colors.secondaryText, fontSize = 11.sp)
        Text(value, color = colors.onSurface, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun PropHashRow(label: String, value: String?, computing: Boolean, canCompute: Boolean, onCompute: () -> Unit, onCopy: () -> Unit) {
    val colors = LocalClintColors.current
    val noFile = stringResource(R.string.download_props_checksum_na_no_file)
    val computingStr = stringResource(R.string.download_props_computing)
    val displayValue = when {
        !canCompute -> noFile
        computing -> computingStr
        value != null -> value
        else -> stringResource(R.string.download_props_checksum_na)
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier.weight(1f).let { if (value != null) it.clickable(onClick = onCopy) else it }
        ) {
            Text(label, color = colors.secondaryText, fontSize = 11.sp)
            Text(displayValue, color = colors.onSurface, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }
        if (canCompute && value == null) {
            Button(onClick = onCompute, enabled = !computing) {
                Text(if (computing) computingStr else stringResource(R.string.download_props_compute))
            }
        }
    }
}

private fun computeFileHash(file: File, algorithm: String): String {
    val digest = MessageDigest.getInstance(algorithm)
    FileInputStream(file).use { fis ->
        val buffer = ByteArray(8192)
        var read: Int
        while (fis.read(buffer).also { read = it } != -1) digest.update(buffer, 0, read)
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

private fun formatPropTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault())
    return sdf.format(java.util.Date(millis))
}

private fun resolvePropertiesPath(context: Context, item: DownloadItem, dash: String): String = when {
    item.contentUri != null -> {
        val uri = Uri.parse(item.contentUri)
        val seg = uri.lastPathSegment ?: item.contentUri
        when {
            seg.startsWith("primary:") -> "/storage/emulated/0/${seg.removePrefix("primary:")}"
            seg.contains(":") -> { val p = seg.split(":", limit = 2); "/storage/${p[0]}/${p[1]}" }
            else -> item.contentUri
        }
    }
    item.locationMode == com.jhaiian.clint.settings.downloads.DownloadSettingsKeys.MODE_CUSTOM -> {
        val treeUri = DownloadFileHelper.getSafTreeUri(context, item)
        if (treeUri != null) {
            val docId = try { DocumentsContract.getTreeDocumentId(treeUri) } catch (_: Throwable) { null }
            when {
                docId != null && docId.startsWith("primary:") -> "/storage/emulated/0/${docId.removePrefix("primary:")}/${item.filename}"
                docId != null && docId.contains(":") -> { val p = docId.split(":", limit = 2); "/storage/${p[0]}/${p[1]}/${item.filename}" }
                else -> treeUri.toString()
            }
        } else item.file?.absolutePath ?: dash
    }
    item.file != null -> item.file.absolutePath
    else -> dash
}

private fun fallbackCompletedAt(context: Context, item: DownloadItem): Long {
    val fileTime = item.file?.takeIf { it.exists() }?.lastModified() ?: 0L
    if (fileTime > 0L) return fileTime
    val uriStr = item.contentUri ?: return 0L
    return runCatching { DocumentFile.fromSingleUri(context, Uri.parse(uriStr))?.lastModified() ?: 0L }.getOrDefault(0L)
}
