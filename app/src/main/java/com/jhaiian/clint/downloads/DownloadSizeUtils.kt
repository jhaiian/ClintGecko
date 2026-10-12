package com.jhaiian.clint.downloads

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import com.jhaiian.clint.util.DEFAULT_MEASUREMENT_SYSTEM
import com.jhaiian.clint.util.MEASUREMENT_SYSTEM_DECIMAL
import com.jhaiian.clint.util.PREF_MEASUREMENT_SYSTEM
import com.jhaiian.clint.util.formatStorageBytes
import java.io.File
import java.util.Locale

internal const val SPEED_LIMIT_UNIT_KB = "KB"
internal const val SPEED_LIMIT_UNIT_MB = "MB"
internal const val DEFAULT_SPEED_LIMIT_UNIT = SPEED_LIMIT_UNIT_KB

internal fun resolveSpeedLimitBytesPerSec(context: Context, amount: Int, unit: String): Long {
    if (amount <= 0) return 0L
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val isDecimal = prefs.getString(PREF_MEASUREMENT_SYSTEM, DEFAULT_MEASUREMENT_SYSTEM) == MEASUREMENT_SYSTEM_DECIMAL
    val kb = if (isDecimal) 1000L else 1024L
    val multiplier = if (unit == SPEED_LIMIT_UNIT_MB) kb * kb else kb
    return amount.toLong() * multiplier
}

internal fun speedLimitBytesToAmountAndUnit(context: Context, bytesPerSec: Long): Pair<Int, String> {
    if (bytesPerSec <= 0L) return 0 to DEFAULT_SPEED_LIMIT_UNIT
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val isDecimal = prefs.getString(PREF_MEASUREMENT_SYSTEM, DEFAULT_MEASUREMENT_SYSTEM) == MEASUREMENT_SYSTEM_DECIMAL
    val kb = if (isDecimal) 1000L else 1024L
    val mb = kb * kb
    return if (bytesPerSec % mb == 0L) {
        (bytesPerSec / mb).toInt() to SPEED_LIMIT_UNIT_MB
    } else {
        maxOf(1L, bytesPerSec / kb).toInt() to SPEED_LIMIT_UNIT_KB
    }
}

internal fun resolveVolumePathFromUri(uri: Uri): String? {
    val segment = uri.lastPathSegment ?: return null
    return when {
        segment.startsWith("primary:") -> Environment.getExternalStorageDirectory().absolutePath
        segment.contains(":") -> "/storage/${segment.substringBefore(":")}"
        else -> null
    }
}

internal const val DEFAULT_DOWNLOAD_PATH = "/storage/emulated/0/Download/"

internal fun uriToDisplayPath(uri: Uri): String {
    val path = uri.lastPathSegment ?: return uri.toString()
    return when {
        path.startsWith("primary:") -> "/storage/emulated/0/${path.removePrefix("primary:")}/"
        path.contains(":") -> {
            val parts = path.split(":", limit = 2)
            "/storage/${parts[0]}/${parts[1]}/"
        }
        else -> uri.toString()
    }
}

internal fun resolveStorageInfoText(context: Context, mode: String, customUri: Uri?): String {
    return try {
        val path = if (mode == DownloadSettingsKeys.MODE_CUSTOM) {
            customUri?.let { resolveVolumePathFromUri(it) }
        } else {
            Environment.getExternalStorageDirectory().absolutePath
        }
            ?: return context.getString(R.string.download_dialog_storage_unavailable)
        val stat = StatFs(path)
        val total = stat.blockCountLong * stat.blockSizeLong
        val free = stat.availableBlocksLong * stat.blockSizeLong
        val used = total - free
        val freePercent = if (total > 0L) free * 100.0 / total else 0.0
        context.getString(
            R.string.download_dialog_storage_value,
            formatStorageBytes(used),
            formatStorageBytes(total),
            formatStorageBytes(free),
            String.format(Locale.US, "%.1f", freePercent)
        )
    } catch (_: Throwable) {
        context.getString(R.string.download_dialog_storage_unavailable)
    }
}

internal fun updateStorageInfo(context: Context, tvStorage: TextView, mode: String, customUri: Uri?) {
    tvStorage.text = resolveStorageInfoText(context, mode, customUri)
}

internal fun checkStorageAvailable(
    context: Context,
    contentLength: Long,
    mode: String,
    customUri: Uri?
): String? {
    if (contentLength <= 0L) return null
    val emulatedFree = try {
        val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
        stat.availableBlocksLong * stat.blockSizeLong
    } catch (_: Throwable) { return null }

    if (mode != DownloadSettingsKeys.MODE_CUSTOM) {
        return if (emulatedFree < contentLength) {
            context.getString(
                R.string.download_error_storage_direct,
                formatStorageBytes(contentLength),
                formatStorageBytes(emulatedFree)
            )
        } else null
    }

    val canWriteDirectly = DownloadFileHelper.canWriteSharedStorageDirectly(context)

    return if (customUri?.lastPathSegment?.startsWith("primary:") == true) {

        if (canWriteDirectly) {
            if (emulatedFree < contentLength) {
                context.getString(
                    R.string.download_error_storage_direct,
                    formatStorageBytes(contentLength),
                    formatStorageBytes(emulatedFree)
                )
            } else null
        } else {
            val required = contentLength * 2
            if (emulatedFree < required) {
                context.getString(
                    R.string.download_error_storage_emulated,
                    formatStorageBytes(required),
                    formatStorageBytes(emulatedFree)
                )
            } else null
        }
    } else {
        val destPath = customUri?.let { resolveVolumePathFromUri(it) } ?: return null
        val destFree = try {
            val stat = StatFs(destPath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Throwable) { return null }
        if (!canWriteDirectly && emulatedFree < contentLength) {
            return context.getString(
                R.string.download_error_storage_temp,
                formatStorageBytes(contentLength),
                formatStorageBytes(emulatedFree)
            )
        }
        if (destFree < contentLength) {
            context.getString(
                R.string.download_error_storage_dest,
                formatStorageBytes(contentLength),
                formatStorageBytes(destFree)
            )
        } else null
    }
}

private const val FAT32_MAX_FILE_SIZE = 4L * 1024 * 1024 * 1024

private val FAT32_FS_TYPES = setOf("vfat", "fat", "fat32", "msdos")

internal fun checkFat32FileSizeLimit(
    context: Context,
    contentLength: Long,
    mode: String,
    customUri: Uri?
): String? {
    if (contentLength < FAT32_MAX_FILE_SIZE) return null
    if (mode != DownloadSettingsKeys.MODE_CUSTOM) return null
    val segment = customUri?.lastPathSegment ?: return null
    if (segment.startsWith("primary:")) return null
    val destPath = resolveVolumePathFromUri(customUri) ?: return null
    if (!isRemovableVolume(context, destPath)) return null
    val fsType = mountedFsType(destPath)?.lowercase(Locale.US) ?: return null
    if (fsType !in FAT32_FS_TYPES) return null
    return context.getString(R.string.download_error_fat32_message, formatStorageBytes(contentLength))
}

private fun isRemovableVolume(context: Context, path: String): Boolean {
    return try {
        val storageManager = context.getSystemService(StorageManager::class.java) ?: return false
        storageManager.getStorageVolume(File(path))?.isRemovable == true
    } catch (_: Throwable) { false }
}

private fun mountedFsType(path: String): String? {
    val canonicalPath = try { File(path).canonicalPath } catch (_: Throwable) { path }
    return try {
        File("/proc/mounts").bufferedReader().useLines { lines ->
            var bestMountPoint = ""
            var bestFsType: String? = null
            for (line in lines) {
                val fields = line.split(" ")
                if (fields.size < 3) continue
                val mountPoint = fields[1]
                val matches = canonicalPath == mountPoint || canonicalPath.startsWith("$mountPoint/")
                if (matches && mountPoint.length > bestMountPoint.length) {
                    bestMountPoint = mountPoint
                    bestFsType = fields[2]
                }
            }
            bestFsType
        }
    } catch (_: Throwable) { null }
}

internal fun estimateBase64DecodedSize(base64: String): Long {
    val trimmed = base64.trimEnd()
    val padding = when {
        trimmed.endsWith("==") -> 2
        trimmed.endsWith("=") -> 1
        else -> 0
    }
    return (trimmed.length.toLong() * 3 / 4) - padding
}
