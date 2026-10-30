package com.jhaiian.clint.downloads.mega

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.jhaiian.clint.R
import com.jhaiian.clint.downloads.DownloadFileHelper
import com.jhaiian.clint.downloads.DownloadItem
import java.io.File
import java.io.IOException

internal object MegaErrors {

    fun message(context: Context, error: Throwable): String = when (error) {
        is MegaException -> when (error.code) {
            MegaException.ENOENT -> context.getString(R.string.mega_error_not_found)
            MegaException.EACCESS, MegaException.EBLOCKED -> context.getString(R.string.mega_error_blocked)
            MegaException.EOVERQUOTA -> context.getString(R.string.mega_error_quota)
            MegaException.EKEY, MegaException.INVALID_KEY -> context.getString(R.string.mega_error_invalid_key)
            MegaException.UNSUPPORTED -> context.getString(R.string.mega_error_unsupported)
            MegaException.EAGAIN, MegaException.ERATELIMIT, MegaException.ETEMPUNAVAIL -> context.getString(R.string.mega_error_busy)
            else -> context.getString(R.string.mega_error_generic, error.code)
        }
        is IOException -> context.getString(R.string.mega_error_network)
        else -> error.message ?: context.getString(R.string.download_error_unknown)
    }
}

internal object MegaRanges {

    fun url(base: String, start: Long, endInclusive: Long): String = "${base.trimEnd('/')}/$start-$endInclusive"
}

internal object MegaPaths {

    private val safLock = Any()

    fun clean(path: String): String = path.split('/')
        .map { it.trim() }
        .filter { it.isNotEmpty() && it != "." && it != ".." }
        .joinToString("/") { DownloadFileHelper.sanitizeFileName(it) }

    fun localDir(base: File, item: DownloadItem): File {
        val path = item.mega?.path.orEmpty()
        return if (path.isEmpty()) base else File(base, path)
    }

    fun safDir(base: DocumentFile, item: DownloadItem): DocumentFile {
        val path = item.mega?.path.orEmpty()
        if (path.isEmpty()) return base
        synchronized(safLock) {
            var dir = base
            for (segment in path.split('/')) {
                if (segment.isEmpty()) continue
                dir = dir.findFile(segment)?.takeIf { it.isDirectory } ?: dir.createDirectory(segment) ?: return dir
            }
            return dir
        }
    }
}
