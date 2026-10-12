package com.jhaiian.clint.mediacapture.download

import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

object TsToAacConverter {

    fun convert(input: File, output: File, isActive: () -> Boolean, onProgress: (Int) -> Unit): Boolean {
        var success = false
        try {
            success = TsAacExtraction(input, output, isActive, onProgress).run()
        } catch (_: Exception) {
        } catch (_: OutOfMemoryError) {
        } finally {
            if (!success) runCatching { output.delete() }
        }
        return success
    }
}

private class TsAacPacketSource(private val input: InputStream) {
    private val buffer = ByteArray(PACKET_SIZE * 2048)
    private var position = 0
    private var limit = 0
    private var ended = false
    var consumed = 0L
        private set

    private fun available(minimum: Int): Int {
        if (limit - position >= minimum || ended) return limit - position
        if (position > 0) {
            System.arraycopy(buffer, position, buffer, 0, limit - position)
            limit -= position
            position = 0
        }
        while (limit < minimum && !ended) {
            val read = input.read(buffer, limit, buffer.size - limit)
            if (read < 0) ended = true else limit += read
        }
        return limit - position
    }

    fun next(destination: ByteArray): Boolean {
        while (true) {
            val have = available(PACKET_SIZE * 2)
            if (have < PACKET_SIZE) return false
            val synced = buffer[position].toInt() == SYNC_BYTE &&
                (have < PACKET_SIZE * 2 || buffer[position + PACKET_SIZE].toInt() == SYNC_BYTE)
            if (synced) {
                System.arraycopy(buffer, position, destination, 0, PACKET_SIZE)
                position += PACKET_SIZE
                consumed += PACKET_SIZE
                return true
            }
            position++
            consumed++
        }
    }

    companion object {
        const val PACKET_SIZE = 188
        const val SYNC_BYTE = 0x47
    }
}

private class TsAacExtraction(
    private val input: File,
    private val output: File,
    private val isActive: () -> Boolean,
    private val onProgress: (Int) -> Unit
) {
    private val sections = HashMap<Int, ByteArrayOutputStream>()
    private val pes = ByteArrayOutputStream()
    private var pmtPid = -1
    private var pmtParsed = false
    private var audioPid = -1
    private var wroteAny = false
    private lateinit var sink: BufferedOutputStream

    fun run(): Boolean {
        val totalBytes = input.length().coerceAtLeast(1L)
        FileInputStream(input).use { stream ->
            BufferedOutputStream(FileOutputStream(output), 1 shl 16).use { out ->
                sink = out
                val source = TsAacPacketSource(stream)
                val packet = ByteArray(TsAacPacketSource.PACKET_SIZE)
                var packetCount = 0L
                var lastPercent = -1
                while (source.next(packet)) {
                    if (!handlePacket(packet)) return false
                    packetCount++
                    if ((packetCount and 0x1FFFL) == 0L) {
                        if (!isActive()) return false
                        val percent = (source.consumed * 100 / totalBytes).toInt().coerceIn(0, 99)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(percent)
                        }
                    }
                }
                flushPes()
            }
        }
        if (!wroteAny) return false
        onProgress(100)
        return true
    }

    private fun handlePacket(packet: ByteArray): Boolean {
        if ((packet[1].toInt() and 0x80) != 0) return true
        val payloadStart = (packet[1].toInt() and 0x40) != 0
        val pid = ((packet[1].toInt() and 0x1F) shl 8) or (packet[2].toInt() and 0xFF)
        val control = packet[3].toInt() and 0xFF
        val adaptation = (control shr 4) and 3
        if (adaptation == 0 || adaptation == 2) return true
        var index = 4
        if (adaptation == 3) index += 1 + (packet[4].toInt() and 0xFF)
        if (index >= TsAacPacketSource.PACKET_SIZE) return true

        when {
            pid == 0 && pmtPid < 0 -> handleSection(pid, packet, index, payloadStart)
            pid == pmtPid && !pmtParsed -> handleSection(pid, packet, index, payloadStart)
            pid == audioPid && audioPid >= 0 -> {
                if (((control shr 6) and 3) != 0) return false
                if (payloadStart) flushPes()
                pes.write(packet, index, TsAacPacketSource.PACKET_SIZE - index)
            }
        }
        return true
    }

    private fun handleSection(pid: Int, packet: ByteArray, index: Int, payloadStart: Boolean) {
        val buffer = sections.getOrPut(pid) { ByteArrayOutputStream(1024) }
        if (payloadStart) {
            buffer.reset()
            val start = index + 1 + (packet[index].toInt() and 0xFF)
            if (start < TsAacPacketSource.PACKET_SIZE) buffer.write(packet, start, TsAacPacketSource.PACKET_SIZE - start)
        } else {
            if (buffer.size() == 0) return
            buffer.write(packet, index, TsAacPacketSource.PACKET_SIZE - index)
        }
        if (buffer.size() < 3) return
        val data = buffer.toByteArray()
        val sectionLength = (((data[1].toInt() and 0x0F) shl 8) or (data[2].toInt() and 0xFF)) + 3
        if (data.size < sectionLength) return
        if (pid == 0) parsePat(data, sectionLength) else parsePmt(data, sectionLength)
        buffer.reset()
    }

    private fun parsePat(data: ByteArray, length: Int) {
        if ((data[0].toInt() and 0xFF) != 0) return
        var i = 8
        while (i + 4 <= length - 4) {
            val program = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            val pid = ((data[i + 2].toInt() and 0x1F) shl 8) or (data[i + 3].toInt() and 0xFF)
            if (program != 0) {
                pmtPid = pid
                return
            }
            i += 4
        }
    }

    private fun parsePmt(data: ByteArray, length: Int) {
        if ((data[0].toInt() and 0xFF) != 2 || length < 16) return
        val programInfoLength = ((data[10].toInt() and 0x0F) shl 8) or (data[11].toInt() and 0xFF)
        var i = 12 + programInfoLength
        while (i + 5 <= length - 4) {
            val type = data[i].toInt() and 0xFF
            val pid = ((data[i + 1].toInt() and 0x1F) shl 8) or (data[i + 2].toInt() and 0xFF)
            val infoLength = ((data[i + 3].toInt() and 0x0F) shl 8) or (data[i + 4].toInt() and 0xFF)
            if (type == 0x0F && audioPid < 0) audioPid = pid
            i += 5 + infoLength
        }
        pmtParsed = true
    }

    private fun flushPes() {
        val size = pes.size()
        if (size == 0) return
        val data = pes.toByteArray()
        pes.reset()
        if (size < 9 || data[0].toInt() != 0 || data[1].toInt() != 0 || data[2].toInt() != 1) return
        var start = 9 + (data[8].toInt() and 0xFF)
        var end = size
        val declared = ((data[4].toInt() and 0xFF) shl 8) or (data[5].toInt() and 0xFF)
        if (declared > 0) end = minOf(end, 6 + declared)
        if (start >= end) return
        if (!wroteAny) {
            while (start + 1 < end && !((data[start].toInt() and 0xFF) == 0xFF && (data[start + 1].toInt() and 0xF0) == 0xF0)) start++
            if (start + 1 >= end) return
        }
        sink.write(data, start, end - start)
        wroteAny = true
    }
}
