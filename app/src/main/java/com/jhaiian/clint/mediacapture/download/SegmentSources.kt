package com.jhaiian.clint.mediacapture.download

import android.media.MediaDataSource
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.RandomAccessFile

internal class SegmentInputStream(private val files: List<File>) : InputStream() {

    private var index = 0
    private var current: InputStream? = null

    private fun stream(): InputStream? {
        while (current == null && index < files.size) current = FileInputStream(files[index++])
        return current
    }

    override fun read(): Int {
        while (true) {
            val stream = stream() ?: return -1
            val value = stream.read()
            if (value >= 0) return value
            stream.close()
            current = null
        }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (len == 0) return 0
        while (true) {
            val stream = stream() ?: return -1
            val count = stream.read(b, off, len)
            if (count > 0) return count
            stream.close()
            current = null
        }
    }

    override fun close() {
        current?.close()
        current = null
        index = files.size
    }
}

internal class SegmentDataSource(private val files: List<File>) : MediaDataSource() {

    private val offsets = LongArray(files.size + 1)
    private val lock = Any()
    private var openIndex = -1
    private var openFile: RandomAccessFile? = null

    init {
        var total = 0L
        files.forEachIndexed { index, file ->
            offsets[index] = total
            total += file.length()
        }
        offsets[files.size] = total
    }

    override fun getSize(): Long = offsets[files.size]

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int = synchronized(lock) {
        if (size <= 0) return@synchronized 0
        if (position < 0L || position >= offsets[files.size]) return@synchronized -1
        var index = indexFor(position)
        var cursor = position
        var written = 0
        while (written < size && index < files.size) {
            val available = offsets[index + 1] - cursor
            if (available <= 0L) {
                index++
                continue
            }
            val file = fileAt(index)
            file.seek(cursor - offsets[index])
            val count = file.read(buffer, offset + written, minOf(available, (size - written).toLong()).toInt())
            if (count <= 0) break
            written += count
            cursor += count
        }
        if (written == 0) -1 else written
    }

    private fun indexFor(position: Long): Int {
        var low = 0
        var high = files.size - 1
        while (low < high) {
            val middle = (low + high + 1) ushr 1
            if (offsets[middle] <= position) low = middle else high = middle - 1
        }
        return low
    }

    private fun fileAt(index: Int): RandomAccessFile {
        val existing = openFile
        if (existing != null && openIndex == index) return existing
        existing?.close()
        val opened = RandomAccessFile(files[index], "r")
        openFile = opened
        openIndex = index
        return opened
    }

    override fun close() {
        synchronized(lock) {
            openFile?.close()
            openFile = null
            openIndex = -1
        }
    }
}
