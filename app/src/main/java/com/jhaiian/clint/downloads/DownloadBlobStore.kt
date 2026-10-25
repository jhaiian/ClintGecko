package com.jhaiian.clint.downloads

import android.content.Context
import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

internal object DownloadBlobStore {

    private const val DIR = "clint_blobs"
    private const val DECODE_CHUNK_CHARS = 64 * 1024
    private const val COPY_BUFFER_BYTES = 256 * 1024

    private fun newFile(context: Context): File {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        return File.createTempFile("blob_", ".bin", dir)
    }

    private inline fun write(context: Context, block: (FileOutputStream) -> Unit): File? {
        val file = runCatching { newFile(context) }.getOrNull() ?: return null
        return try {
            FileOutputStream(file).use(block)
            file
        } catch (_: Throwable) {
            file.delete()
            null
        }
    }

    fun fromBase64(context: Context, base64: String): File? = write(context) { out ->
        val chunk = StringBuilder(DECODE_CHUNK_CHARS)
        for (ch in base64) {
            if (ch.isWhitespace()) continue
            chunk.append(ch)
            if (chunk.length == DECODE_CHUNK_CHARS) {
                out.write(Base64.decode(chunk.toString(), Base64.DEFAULT))
                chunk.setLength(0)
            }
        }
        if (chunk.isNotEmpty()) out.write(Base64.decode(chunk.toString(), Base64.DEFAULT))
    }

    fun fromBytes(context: Context, bytes: ByteArray): File? = write(context) { it.write(bytes) }

    fun fromStream(context: Context, input: InputStream): File? = write(context) { out ->
        input.copyTo(out, COPY_BUFFER_BYTES)
    }

    fun moveTo(source: File, target: File) {
        if (source.renameTo(target)) return
        source.inputStream().use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output, COPY_BUFFER_BYTES) }
        }
        source.delete()
    }

    fun sweep(context: Context, olderThan: Long) {
        File(context.cacheDir, DIR).listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < olderThan) runCatching { file.delete() }
        }
    }
}
