package com.jhaiian.clint.mediacapture.download

import java.io.File
import java.io.FileInputStream
import java.io.InputStream

object TsToMp4Converter {

    fun convert(input: File, output: File, isActive: () -> Boolean, onProgress: (Int) -> Unit): Boolean =
        try {
            TsConversion(input, output, isActive, onProgress).run()
        } catch (_: Exception) {
            false
        } catch (_: OutOfMemoryError) {
            false
        }
}

private class TsPacketReader(private val input: InputStream) {
    private val buffer = ByteArray(1 shl 20)
    private var position = 0
    private var limit = 0
    private var endOfInput = false
    var consumed = 0L
        private set

    private fun fill(minimum: Int): Boolean {
        if (limit - position >= minimum) return true
        if (position > 0) {
            System.arraycopy(buffer, position, buffer, 0, limit - position)
            limit -= position
            position = 0
        }
        while (limit < minimum && !endOfInput) {
            val read = input.read(buffer, limit, buffer.size - limit)
            if (read < 0) endOfInput = true else limit += read
        }
        return limit - position >= minimum
    }

    private fun syncAt(offset: Int): Boolean {
        for (k in 0 until 4) {
            val index = position + offset + k * PACKET_SIZE
            if (index >= limit) return k >= 2
            if (buffer[index].toInt() != SYNC_BYTE) return false
        }
        return true
    }

    private fun seekSync(): Boolean {
        while (true) {
            if (!fill(PACKET_SIZE * 4)) {
                if (limit - position < PACKET_SIZE) return false
            }
            val scanLimit = limit - position - PACKET_SIZE
            var offset = 0
            while (offset <= scanLimit) {
                if (buffer[position + offset].toInt() == SYNC_BYTE && syncAt(offset)) {
                    position += offset
                    return true
                }
                offset++
            }
            if (endOfInput) return false
            position += maxOf(scanLimit + 1, 1)
        }
    }

    fun locate(): Boolean = seekSync()

    fun next(destination: ByteArray): Boolean {
        if (!fill(PACKET_SIZE)) return false
        if (buffer[position].toInt() != SYNC_BYTE) {
            if (!seekSync()) return false
        }
        System.arraycopy(buffer, position, destination, 0, PACKET_SIZE)
        position += PACKET_SIZE
        consumed += PACKET_SIZE
        return true
    }

    companion object {
        const val PACKET_SIZE = 188
        const val SYNC_BYTE = 0x47
    }
}

private class TsConversion(
    private val input: File,
    private val output: File,
    private val isActive: () -> Boolean,
    private val onProgress: (Int) -> Unit
) {
    private val sections = HashMap<Int, GrowableBytes>()
    private val pesBuffers = HashMap<Int, GrowableBytes>()
    private var pmtPid = -1
    private var pmtParsed = false
    private var videoPid = -1
    private var audioPid = -1

    private lateinit var sink: Mp4Sink
    private val videoChunks = ChunkedTrack()
    private val audioChunks = ChunkedTrack()
    private var lastWrittenTrack = -1

    private val videoStream = TsVideoStream(this::onVideoAccessUnit)
    private var audioStream: TsAdtsStream? = null

    private val scratch = ByteBuf(1 shl 18)
    private val spsList = ArrayList<ByteArray>()
    private val ppsList = ArrayList<ByteArray>()
    private var spsInfo: SpsInfo? = null

    private val videoDecodeTimes = LongList()
    private val videoPresentationTimes = LongList()
    private val videoSyncSamples = IntList()
    private var videoStarted = false
    private var lastDtsUnwrapped = 0L
    private var videoShift = 0L
    private var lastDtsFinal = 0L
    private var lastPtsFinal = 0L
    private var lastVideoDelta = 0L

    private var adtsParams: AdtsParams? = null
    private val audioTimes = LongList()
    private var audioNextUnits = 0L
    private var audioShiftUnits = 0L
    private var audioLastMarker = 0L
    private var audioHasMarker = false

    fun run(): Boolean {
        val totalBytes = input.length().coerceAtLeast(1L)
        sink = Mp4Sink(output)
        var success = false
        try {
            FileInputStream(input).use { stream ->
                val reader = TsPacketReader(stream)
                if (!reader.locate()) throw TsUnsupportedException("No transport stream")
                sink.write(Mp4Boxes.ftyp())
                val mdatHeaderAt = sink.position
                val header = ByteBuf(16)
                header.u32(1L)
                header.fourcc("mdat")
                header.u64(16L)
                sink.write(header.toByteArray())

                val packet = ByteArray(TsPacketReader.PACKET_SIZE)
                var packetCount = 0L
                var lastPercent = -1
                while (reader.next(packet)) {
                    handlePacket(packet)
                    packetCount++
                    if ((packetCount and 0x1FFFL) == 0L) {
                        if (!isActive()) return false
                        val percent = (reader.consumed * 100 / totalBytes).toInt().coerceIn(0, 99)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            onProgress(percent)
                        }
                    }
                }
                for (pid in listOf(videoPid, audioPid)) if (pid >= 0) flushPes(pid)
                videoStream.finish()
                audioStream?.finish()

                val videoInfo = spsInfo
                if (videoInfo == null || videoDecodeTimes.size == 0) throw TsUnsupportedException("No decodable video")
                if (audioPid >= 0 && audioTimes.size == 0) throw TsUnsupportedException("Audio not decodable")

                val mdatSize = ByteBuf(8)
                mdatSize.u64(sink.position - mdatHeaderAt)
                sink.patch(mdatHeaderAt + 8, mdatSize.toByteArray())

                val videoData = VideoTrackData(
                    chunks = videoChunks,
                    decodeTimes = videoDecodeTimes,
                    presentationTimes = videoPresentationTimes,
                    syncSamples = videoSyncSamples,
                    width = videoInfo.width,
                    height = videoInfo.height,
                    sarWidth = videoInfo.sarWidth,
                    sarHeight = videoInfo.sarHeight,
                    avcC = buildAvcC(videoInfo)
                )
                val params = adtsParams
                val audioData = if (audioPid >= 0 && params != null) {
                    AudioTrackData(
                        chunks = audioChunks,
                        times = audioTimes,
                        sampleRate = SAMPLE_RATES[params.samplingIndex],
                        channels = if (params.channelConfig == 7) 8 else params.channelConfig,
                        audioSpecificConfig = buildAudioSpecificConfig(params)
                    )
                } else null
                sink.write(Mp4Boxes.moov(videoData, audioData))
            }
            success = true
        } finally {
            sink.close()
            if (!success) runCatching { output.delete() }
        }
        onProgress(100)
        return true
    }

    private fun handlePacket(packet: ByteArray) {
        if ((packet[1].toInt() and 0x80) != 0) return
        val payloadStart = (packet[1].toInt() and 0x40) != 0
        val pid = ((packet[1].toInt() and 0x1F) shl 8) or (packet[2].toInt() and 0xFF)
        val control = packet[3].toInt() and 0xFF
        val adaptation = (control shr 4) and 3
        if (adaptation == 0 || adaptation == 2) return
        var index = 4
        if (adaptation == 3) index += 1 + (packet[4].toInt() and 0xFF)
        if (index >= TsPacketReader.PACKET_SIZE) return

        when {
            pid == 0 && pmtPid < 0 -> handleSection(pid, packet, index, payloadStart)
            pid == pmtPid && !pmtParsed -> handleSection(pid, packet, index, payloadStart)
            pid == videoPid || pid == audioPid -> {
                if (((control shr 6) and 3) != 0) throw TsUnsupportedException("Scrambled stream")
                val buffer = pesBuffers.getOrPut(pid) { GrowableBytes() }
                if (payloadStart) flushPes(pid)
                buffer.add(packet, index, TsPacketReader.PACKET_SIZE - index)
            }
        }
    }

    private fun handleSection(pid: Int, packet: ByteArray, index: Int, payloadStart: Boolean) {
        val buffer = sections.getOrPut(pid) { GrowableBytes(1024) }
        if (payloadStart) {
            buffer.clear()
            val start = index + 1 + (packet[index].toInt() and 0xFF)
            if (start < TsPacketReader.PACKET_SIZE) buffer.add(packet, start, TsPacketReader.PACKET_SIZE - start)
        } else {
            if (buffer.size == 0) return
            buffer.add(packet, index, TsPacketReader.PACKET_SIZE - index)
        }
        if (buffer.size < 3) return
        val data = buffer.data
        val sectionLength = (((data[1].toInt() and 0x0F) shl 8) or (data[2].toInt() and 0xFF)) + 3
        if (buffer.size < sectionLength) return
        if (pid == 0) parsePat(data, sectionLength) else parsePmt(data, sectionLength)
        buffer.clear()
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
        var videoFound = false
        var unsupportedVideo = false
        var unsupportedAudio = false
        while (i + 5 <= length - 4) {
            val type = data[i].toInt() and 0xFF
            val pid = ((data[i + 1].toInt() and 0x1F) shl 8) or (data[i + 2].toInt() and 0xFF)
            val infoLength = ((data[i + 3].toInt() and 0x0F) shl 8) or (data[i + 4].toInt() and 0xFF)
            when (type) {
                0x1B -> if (!videoFound) {
                    videoPid = pid
                    videoFound = true
                }
                0x0F -> if (audioPid < 0) audioPid = pid
                0x02, 0x10, 0x24, 0x42, 0xEA -> unsupportedVideo = true
                0x03, 0x04, 0x11, 0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x8A -> unsupportedAudio = true
                0x06 -> if (hasAudioDescriptor(data, i + 5, infoLength)) unsupportedAudio = true
            }
            i += 5 + infoLength
        }
        if (!videoFound || (unsupportedVideo && videoPid < 0)) throw TsUnsupportedException("Unsupported video codec")
        if (audioPid < 0 && unsupportedAudio) throw TsUnsupportedException("Unsupported audio codec")
        if (audioPid >= 0) {
            audioStream = TsAdtsStream(this::onAdtsParams, this::onAudioFrame)
        }
        pmtParsed = true
    }

    private fun hasAudioDescriptor(data: ByteArray, start: Int, length: Int): Boolean {
        var i = start
        val end = start + length
        while (i + 2 <= end) {
            val tag = data[i].toInt() and 0xFF
            if (tag == 0x6A || tag == 0x7A || tag == 0x7B || tag == 0x7C) return true
            i += 2 + (data[i + 1].toInt() and 0xFF)
        }
        return false
    }

    private fun readTimestamp(data: ByteArray, at: Int): Long =
        (((data[at].toLong() shr 1) and 7L) shl 30) or
            ((data[at + 1].toLong() and 0xFFL) shl 22) or
            (((data[at + 2].toLong() and 0xFFL) shr 1) shl 15) or
            ((data[at + 3].toLong() and 0xFFL) shl 7) or
            ((data[at + 4].toLong() and 0xFFL) shr 1)

    private fun flushPes(pid: Int) {
        val buffer = pesBuffers[pid] ?: return
        val data = buffer.data
        val size = buffer.size
        if (size >= 9 && data[0].toInt() == 0 && data[1].toInt() == 0 && data[2].toInt() == 1) {
            val flags = (data[7].toInt() shr 6) and 3
            val headerLength = data[8].toInt() and 0xFF
            var pts = -1L
            var dts = -1L
            if (flags >= 2 && size >= 14) pts = readTimestamp(data, 9)
            dts = if (flags == 3 && size >= 19) readTimestamp(data, 14) else pts
            val start = 9 + headerLength
            var end = size
            val declared = ((data[4].toInt() and 0xFF) shl 8) or (data[5].toInt() and 0xFF)
            if (declared > 0) end = minOf(end, 6 + declared)
            if (start < end) {
                if (pid == videoPid) {
                    videoStream.append(data, start, end - start, pts, dts)
                } else {
                    audioStream?.append(data, start, end - start, pts, dts)
                }
            }
        }
        buffer.clear()
    }

    private fun unwrapNear(value: Long, reference: Long): Long {
        val period = 1L shl 33
        val cycles = Math.floorDiv(reference - value + period / 2, period)
        return value + cycles * period
    }

    private fun writeSample(track: Int, data: ByteArray, offset: Int, length: Int) {
        val chunks = if (track == 0) videoChunks else audioChunks
        if (lastWrittenTrack != track || chunks.chunkSampleCounts.size == 0 ||
            chunks.chunkSampleCounts.last() >= MAX_SAMPLES_PER_CHUNK
        ) {
            chunks.chunkOffsets.add(sink.position)
            chunks.chunkSampleCounts.add(0)
        }
        chunks.chunkSampleCounts.setLast(chunks.chunkSampleCounts.last() + 1)
        chunks.sizes.add(length)
        lastWrittenTrack = track
        sink.write(data, offset, length)
    }

    private fun convertAccessUnit(data: ByteArray, from: Int, to: Int): Boolean {
        scratch.clear()
        var hasIdr = false
        var nalStart = -1
        var i = from
        while (i + 2 < to) {
            if (data[i].toInt() == 0 && data[i + 1].toInt() == 0 && data[i + 2].toInt() == 1) {
                if (nalStart >= 0) hasIdr = appendNal(data, nalStart, i) || hasIdr
                nalStart = i + 3
                i += 3
            } else {
                i++
            }
        }
        if (nalStart >= 0) hasIdr = appendNal(data, nalStart, to) || hasIdr
        return hasIdr
    }

    private fun appendNal(data: ByteArray, start: Int, endExclusive: Int): Boolean {
        var end = endExclusive
        while (end > start && data[end - 1].toInt() == 0) end--
        if (end <= start) return false
        val type = data[start].toInt() and 0x1F
        if (type == 9) return false
        if (type == 7) collectParameterSet(spsList, data, start, end, 16)
        if (type == 8) collectParameterSet(ppsList, data, start, end, 32)
        scratch.u32((end - start).toLong())
        scratch.bytes(data, start, end - start)
        return type == 5
    }

    private fun collectParameterSet(list: ArrayList<ByteArray>, data: ByteArray, start: Int, end: Int, limit: Int) {
        if (list.size >= limit) return
        val candidate = data.copyOfRange(start, end)
        if (list.none { it.contentEquals(candidate) }) list.add(candidate)
    }

    private fun onVideoAccessUnit(data: ByteArray, offset: Int, length: Int, pts: Long, dts: Long) {
        val hasIdr = convertAccessUnit(data, offset, offset + length)
        if (scratch.size == 0) return
        if (!videoStarted && !hasIdr) return
        if (spsInfo == null && spsList.isNotEmpty()) {
            spsInfo = H264Sps.parse(spsList[0])
        }

        val rawDts = if (dts >= 0L) dts else pts
        val first = !videoStarted
        var decode: Long
        var presentation: Long
        if (rawDts < 0L) {
            decode = if (first) 0L else lastDtsFinal + (if (lastVideoDelta > 0L) lastVideoDelta else DEFAULT_VIDEO_DELTA)
            presentation = decode + (lastPtsFinal - lastDtsFinal)
        } else {
            val dtsUnwrapped = if (first) rawDts else unwrapNear(rawDts, lastDtsUnwrapped)
            val ptsUnwrapped = if (pts >= 0L) unwrapNear(pts, dtsUnwrapped) else dtsUnwrapped
            if (!first && (dtsUnwrapped < lastDtsUnwrapped || dtsUnwrapped - lastDtsUnwrapped > DISCONTINUITY_TICKS)) {
                val step = if (lastVideoDelta > 0L) lastVideoDelta else DEFAULT_VIDEO_DELTA
                videoShift = lastDtsFinal + step - dtsUnwrapped
            }
            lastDtsUnwrapped = dtsUnwrapped
            decode = dtsUnwrapped + videoShift
            presentation = ptsUnwrapped + videoShift
        }
        if (!first) {
            if (decode < lastDtsFinal) decode = lastDtsFinal
            if (decode > lastDtsFinal) lastVideoDelta = decode - lastDtsFinal
        }
        if (presentation < decode) presentation = decode
        lastDtsFinal = decode
        lastPtsFinal = presentation
        videoStarted = true

        if (hasIdr) videoSyncSamples.add(videoChunks.sizes.size + 1)
        videoDecodeTimes.add(decode)
        videoPresentationTimes.add(presentation)
        writeSample(0, scratch.data, 0, scratch.size)
    }

    private fun onAdtsParams(params: AdtsParams) {
        adtsParams = params
    }

    private fun onAudioFrame(data: ByteArray, offset: Int, length: Int, pts: Long) {
        val params = adtsParams ?: return
        val sampleRate = SAMPLE_RATES[params.samplingIndex].toLong()
        val first = audioTimes.size == 0
        var time: Long
        if (pts >= 0L) {
            val unwrapped = if (!audioHasMarker) pts else unwrapNear(pts, audioLastMarker)
            audioLastMarker = unwrapped
            audioHasMarker = true
            val candidate = unwrapped * sampleRate / 90000L + audioShiftUnits
            if (first) {
                time = candidate
            } else {
                val difference = candidate - audioNextUnits
                time = when {
                    Math.abs(difference) > 2L * sampleRate -> {
                        audioShiftUnits -= difference
                        audioNextUnits
                    }
                    difference > 2L -> candidate
                    else -> audioNextUnits
                }
            }
        } else {
            time = if (first) 0L else audioNextUnits
        }
        audioNextUnits = time + 1024L
        audioTimes.add(time)
        writeSample(1, data, offset, length)
    }

    private fun buildAvcC(info: SpsInfo): ByteArray {
        val first = spsList[0]
        val out = ByteBuf(256)
        out.u8(1)
        out.u8(first[1].toInt())
        out.u8(first[2].toInt())
        out.u8(first[3].toInt())
        out.u8(0xFF)
        out.u8(0xE0 or spsList.size)
        for (sps in spsList) {
            out.u16(sps.size)
            out.bytes(sps)
        }
        out.u8(ppsList.size)
        for (pps in ppsList) {
            out.u16(pps.size)
            out.bytes(pps)
        }
        if (info.profileIdc != 66 && info.profileIdc != 77 && info.profileIdc != 88) {
            out.u8(0xFC or info.chromaFormat)
            out.u8(0xF8 or (info.bitDepthLuma - 8))
            out.u8(0xF8 or (info.bitDepthChroma - 8))
            out.u8(0)
        }
        return out.toByteArray()
    }

    private fun buildAudioSpecificConfig(params: AdtsParams): ByteArray {
        val objectType = params.profile + 1
        val value = (objectType shl 11) or (params.samplingIndex shl 7) or (params.channelConfig shl 3)
        return byteArrayOf((value shr 8).toByte(), value.toByte())
    }

    companion object {
        private const val DISCONTINUITY_TICKS = 180000L
        private const val DEFAULT_VIDEO_DELTA = 3000L
        private const val MAX_SAMPLES_PER_CHUNK = 512
        private val SAMPLE_RATES = intArrayOf(
            96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350
        )
    }
}
