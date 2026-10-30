package com.jhaiian.clint.downloads

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.os.StatFs
import android.provider.DocumentsContract
import android.system.Os
import android.system.OsConstants
import androidx.documentfile.provider.DocumentFile
import com.jhaiian.clint.downloads.mega.MegaPaths
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel

internal sealed class DownloadTarget {

    abstract val location: String
    abstract fun exists(context: Context): Boolean
    abstract fun length(context: Context): Long
    abstract fun openChannel(context: Context): FileChannel
    abstract fun setLength(context: Context, length: Long)
    abstract fun delete(context: Context)
    abstract fun availableBytes(context: Context): Long

    class Local(val file: File) : DownloadTarget() {
        override val location: String get() = file.absolutePath

        override fun exists(context: Context): Boolean = file.exists()

        override fun length(context: Context): Long = if (file.isFile) file.length() else -1L

        override fun openChannel(context: Context): FileChannel = RandomAccessFile(file, "rw").channel

        override fun setLength(context: Context, length: Long) {
            RandomAccessFile(file, "rw").use { it.setLength(length) }
        }

        override fun delete(context: Context) {
            file.delete()
        }

        override fun availableBytes(context: Context): Long =
            runCatching { StatFs((file.parentFile ?: file).absolutePath).availableBytes }.getOrDefault(-1L)
    }

    class Document(val uri: Uri) : DownloadTarget() {
        override val location: String get() = uri.toString()

        private fun open(context: Context, mode: String): ParcelFileDescriptor =
            context.contentResolver.openFileDescriptor(uri, mode) ?: throw FileNotFoundException(uri.toString())

        override fun exists(context: Context): Boolean =
            runCatching { open(context, "r").use { it.statSize >= 0L } }.getOrDefault(false)

        override fun length(context: Context): Long =
            runCatching { open(context, "r").use { it.statSize } }.getOrDefault(-1L)

        override fun openChannel(context: Context): FileChannel =
            ParcelFileDescriptor.AutoCloseOutputStream(open(context, "rw")).channel

        override fun setLength(context: Context, length: Long) {
            open(context, "rw").use { Os.ftruncate(it.fileDescriptor, length) }
        }

        override fun delete(context: Context) {
            runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
        }

        override fun availableBytes(context: Context): Long = runCatching {
            open(context, "r").use { pfd ->
                val stat = Os.fstatvfs(pfd.fileDescriptor)
                stat.f_bavail * stat.f_frsize
            }
        }.getOrDefault(-1L)
    }

    companion object {
        fun of(item: DownloadItem): DownloadTarget? {
            val file = item.file
            if (file != null) return Local(file)
            val uri = item.contentUri ?: return null
            if (item.status == DownloadStatus.COMPLETE) return null
            return Document(Uri.parse(uri))
        }
    }
}

internal object SafDirectWriter {

    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    private const val PART_MIME_TYPE = "application/octet-stream"

    class CreatedPart(val uri: Uri, val finalName: String)

    fun createPart(context: Context, item: DownloadItem, finalName: String): CreatedPart? = try {
        val treeUri = DownloadFileHelper.getSafTreeUri(context, item)
        val docDir = if (treeUri != null && treeUri.authority == EXTERNAL_STORAGE_AUTHORITY) {
            DocumentFile.fromTreeUri(context, treeUri)
        } else null
        if (docDir == null || !docDir.canWrite()) {
            null
        } else {
            val targetDir = MegaPaths.safDir(DownloadCategories.resolveSafDir(item.categorizeEnabled, docDir, finalName), item)
            val chosen = freeName(targetDir, finalName)
            val partName = chosen + DownloadFileHelper.PART_SUFFIX
            val doc = targetDir.createFile(PART_MIME_TYPE, partName)
            when {
                doc == null -> null
                doc.name != partName || !isRegularFile(context, doc.uri) -> {
                    runCatching { doc.delete() }
                    null
                }
                else -> CreatedPart(doc.uri, chosen)
            }
        }
    } catch (_: Exception) {
        null
    }

    fun promote(context: Context, partUri: Uri, finalName: String): Uri? = runCatching {
        val current = DocumentFile.fromSingleUri(context, partUri)?.name
        if (current != null && !current.endsWith(DownloadFileHelper.PART_SUFFIX)) {
            partUri
        } else {
            DocumentsContract.renameDocument(context.contentResolver, partUri, DownloadFileHelper.sanitizeFileName(finalName))
        }
    }.getOrNull()

    fun displayName(context: Context, uri: Uri): String? =
        runCatching { DocumentFile.fromSingleUri(context, uri)?.name }.getOrNull()

    fun deleteIfPart(context: Context, uri: Uri) {
        runCatching {
            val doc = DocumentFile.fromSingleUri(context, uri)
            if (doc != null && doc.name?.endsWith(DownloadFileHelper.PART_SUFFIX) == true) doc.delete()
        }
    }

    private fun freeName(dir: DocumentFile, name: String): String {
        val safeName = DownloadFileHelper.sanitizeFileName(name)
        val taken = dir.listFiles().mapNotNullTo(HashSet()) { it.name }
        val dot = safeName.lastIndexOf('.')
        val base = if (dot > 0) safeName.substring(0, dot) else safeName
        val ext = if (dot > 0) safeName.substring(dot) else ""
        var index = 0
        while (true) {
            val candidate = if (index == 0) safeName else "$base($index)$ext"
            if (candidate !in taken && candidate + DownloadFileHelper.PART_SUFFIX !in taken) return candidate
            index++
        }
    }

    private fun isRegularFile(context: Context, uri: Uri): Boolean =
        context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
            OsConstants.S_ISREG(Os.fstat(pfd.fileDescriptor).st_mode)
        } == true
}
