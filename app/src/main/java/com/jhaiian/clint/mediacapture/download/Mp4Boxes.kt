package com.jhaiian.clint.mediacapture.download

import java.io.File
import java.io.RandomAccessFile

internal class Mp4Sink(file: File) : AutoCloseable {
    private val output = RandomAccessFile(file, "rw")
    private val buffer = ByteArray(1 shl 20)
    private var buffered = 0
    var position = 0L
        private set

    init {
        output.setLength(0L)
    }

    fun write(src: ByteArray, offset: Int, length: Int) {
        if (length >= buffer.size) {
            flush()
            output.write(src, offset, length)
        } else {
            if (buffered + length > buffer.size) flush()
            System.arraycopy(src, offset, buffer, buffered, length)
            buffered += length
        }
        position += length
    }

    fun write(src: ByteArray) {
        write(src, 0, src.size)
    }

    private fun flush() {
        if (buffered > 0) {
            output.write(buffer, 0, buffered)
            buffered = 0
        }
    }

    fun patch(at: Long, src: ByteArray) {
        flush()
        output.seek(at)
        output.write(src)
        output.seek(position)
    }

    override fun close() {
        flush()
        output.close()
    }
}

internal class ChunkedTrack {
    val sizes = IntList()
    val chunkOffsets = LongList()
    val chunkSampleCounts = IntList()
}

internal class VideoTrackData(
    val chunks: ChunkedTrack,
    val decodeTimes: LongList,
    val presentationTimes: LongList,
    val syncSamples: IntList,
    val width: Int,
    val height: Int,
    val sarWidth: Int,
    val sarHeight: Int,
    val avcC: ByteArray
)

internal class AudioTrackData(
    val chunks: ChunkedTrack,
    val times: LongList,
    val sampleRate: Int,
    val channels: Int,
    val audioSpecificConfig: ByteArray
)

internal object Mp4Boxes {

    private const val MOVIE_TIMESCALE = 1000L
    private const val VIDEO_TIMESCALE = 90000L
    private const val SAMPLES_PER_AAC_FRAME = 1024L
    private const val DEFAULT_VIDEO_DELTA = 3000L

    fun ftyp(): ByteArray {
        val out = ByteBuf(32)
        out.box("ftyp") {
            fourcc("isom")
            u32(512L)
            fourcc("isom")
            fourcc("iso2")
            fourcc("avc1")
            fourcc("mp41")
        }
        return out.toByteArray()
    }

    private fun videoMs(ticks: Long): Long = ticks * MOVIE_TIMESCALE / VIDEO_TIMESCALE

    fun moov(video: VideoTrackData, audio: AudioTrackData?): ByteArray {
        val count = video.chunks.sizes.size
        val videoDeltas = LongList()
        var mediaDuration = 0L
        for (i in 0 until count - 1) {
            val delta = video.decodeTimes[i + 1] - video.decodeTimes[i]
            videoDeltas.add(delta)
            mediaDuration += delta
        }
        val lastDelta = if (videoDeltas.size > 0) videoDeltas[videoDeltas.size - 1] else DEFAULT_VIDEO_DELTA
        videoDeltas.add(lastDelta)
        mediaDuration += lastDelta

        var minPts = Long.MAX_VALUE
        var maxPts = Long.MIN_VALUE
        for (i in 0 until count) {
            minPts = minOf(minPts, video.presentationTimes[i])
            maxPts = maxOf(maxPts, video.presentationTimes[i])
        }
        val firstDts = video.decodeTimes[0]
        val firstDisplayOffset = minPts - firstDts

        var audioStart90k = minPts
        var audioDurationUnits = 0L
        if (audio != null) {
            val audioCount = audio.chunks.sizes.size
            audioStart90k = audio.times[0] * VIDEO_TIMESCALE / audio.sampleRate
            audioDurationUnits = audio.times[audioCount - 1] - audio.times[0] + SAMPLES_PER_AAC_FRAME
        }
        val origin = minOf(minPts, audioStart90k)

        val videoEmptyMs = videoMs(minPts - origin)
        val videoPresentationMs = videoMs(maxPts + lastDelta - minPts)
        val audioEmptyMs = videoMs(audioStart90k - origin)
        val audioPresentationMs = audio?.let { audioDurationUnits * MOVIE_TIMESCALE / it.sampleRate } ?: 0L
        val movieDuration = maxOf(
            videoEmptyMs + videoPresentationMs,
            if (audio != null) audioEmptyMs + audioPresentationMs else 0L
        )

        val out = ByteBuf(1 shl 16)
        out.box("moov") {
            fullBox("mvhd", 0, 0) {
                u32(0L)
                u32(0L)
                u32(MOVIE_TIMESCALE)
                u32(movieDuration)
                u32(0x00010000L)
                u16(0x0100)
                zeros(10)
                matrix()
                zeros(24)
                u32(if (audio != null) 3L else 2L)
            }
            videoTrak(video, videoDeltas, mediaDuration, firstDisplayOffset, videoEmptyMs, videoPresentationMs)
            if (audio != null) {
                audioTrak(audio, audioDurationUnits, audioEmptyMs, audioPresentationMs)
            }
        }
        return out.toByteArray()
    }

    private fun ByteBuf.matrix() {
        u32(0x00010000L)
        u32(0L)
        u32(0L)
        u32(0L)
        u32(0x00010000L)
        u32(0L)
        u32(0L)
        u32(0L)
        u32(0x40000000L)
    }

    private fun ByteBuf.tkhd(trackId: Long, durationMs: Long, isAudio: Boolean, width: Int, height: Int) {
        fullBox("tkhd", 0, 3) {
            u32(0L)
            u32(0L)
            u32(trackId)
            u32(0L)
            u32(durationMs)
            zeros(8)
            u16(0)
            u16(0)
            u16(if (isAudio) 0x0100 else 0)
            u16(0)
            matrix()
            u32(width.toLong() shl 16)
            u32(height.toLong() shl 16)
        }
    }

    private fun ByteBuf.elst(emptyMs: Long, presentationMs: Long, mediaTime: Long) {
        box("edts") {
            fullBox("elst", 0, 0) {
                u32(if (emptyMs > 0L) 2L else 1L)
                if (emptyMs > 0L) {
                    u32(emptyMs)
                    u32(0xFFFFFFFFL)
                    u16(1)
                    u16(0)
                }
                u32(presentationMs)
                u32(mediaTime)
                u16(1)
                u16(0)
            }
        }
    }

    private fun ByteBuf.mdhd(timescale: Long, duration: Long) {
        fullBox("mdhd", 0, 0) {
            u32(0L)
            u32(0L)
            u32(timescale)
            u32(duration.coerceAtMost(0xFFFFFFFFL))
            u16(0x55C4)
            u16(0)
        }
    }

    private fun ByteBuf.hdlr(type: String, name: String) {
        fullBox("hdlr", 0, 0) {
            u32(0L)
            fourcc(type)
            zeros(12)
            fourcc(name)
            u8(0)
        }
    }

    private fun ByteBuf.dinf() {
        box("dinf") {
            fullBox("dref", 0, 0) {
                u32(1L)
                fullBox("url ", 0, 1) {}
            }
        }
    }

    private fun ByteBuf.chunkTables(chunks: ChunkedTrack) {
        fullBox("stsc", 0, 0) {
            val entryCountAt = size
            u32(0L)
            var entries = 0L
            var lastCount = -1
            for (i in 0 until chunks.chunkSampleCounts.size) {
                val perChunk = chunks.chunkSampleCounts[i]
                if (perChunk != lastCount) {
                    u32((i + 1).toLong())
                    u32(perChunk.toLong())
                    u32(1L)
                    entries++
                    lastCount = perChunk
                }
            }
            patchU32(entryCountAt, entries)
        }
        fullBox("stsz", 0, 0) {
            u32(0L)
            u32(chunks.sizes.size.toLong())
            for (i in 0 until chunks.sizes.size) u32(chunks.sizes[i].toLong())
        }
        fullBox("co64", 0, 0) {
            u32(chunks.chunkOffsets.size.toLong())
            for (i in 0 until chunks.chunkOffsets.size) u64(chunks.chunkOffsets[i])
        }
    }

    private fun ByteBuf.sttsFrom(deltas: LongList) {
        fullBox("stts", 0, 0) {
            val entryCountAt = size
            u32(0L)
            var entries = 0L
            var i = 0
            while (i < deltas.size) {
                var run = 1
                while (i + run < deltas.size && deltas[i + run] == deltas[i]) run++
                u32(run.toLong())
                u32(deltas[i])
                entries++
                i += run
            }
            patchU32(entryCountAt, entries)
        }
    }

    private fun ByteBuf.videoTrak(
        video: VideoTrackData,
        deltas: LongList,
        mediaDuration: Long,
        firstDisplayOffset: Long,
        emptyMs: Long,
        presentationMs: Long
    ) {
        val count = video.chunks.sizes.size
        box("trak") {
            tkhd(1L, emptyMs + presentationMs, false, video.width, video.height)
            elst(emptyMs, presentationMs, firstDisplayOffset)
            box("mdia") {
                mdhd(VIDEO_TIMESCALE, mediaDuration)
                hdlr("vide", "VideoHandler")
                box("minf") {
                    fullBox("vmhd", 0, 1) {
                        zeros(8)
                    }
                    dinf()
                    box("stbl") {
                        fullBox("stsd", 0, 0) {
                            u32(1L)
                            box("avc1") {
                                zeros(6)
                                u16(1)
                                zeros(16)
                                u16(video.width)
                                u16(video.height)
                                u32(0x00480000L)
                                u32(0x00480000L)
                                u32(0L)
                                u16(1)
                                zeros(32)
                                u16(0x0018)
                                u16(0xFFFF)
                                box("avcC") { bytes(video.avcC) }
                                if (video.sarWidth > 0 && video.sarHeight > 0 && video.sarWidth != video.sarHeight) {
                                    box("pasp") {
                                        u32(video.sarWidth.toLong())
                                        u32(video.sarHeight.toLong())
                                    }
                                }
                            }
                        }
                        sttsFrom(deltas)
                        var hasOffsets = false
                        for (i in 0 until count) {
                            if (video.presentationTimes[i] != video.decodeTimes[i]) {
                                hasOffsets = true
                                break
                            }
                        }
                        if (hasOffsets) {
                            fullBox("ctts", 0, 0) {
                                val entryCountAt = size
                                u32(0L)
                                var entries = 0L
                                var i = 0
                                while (i < count) {
                                    val offset = video.presentationTimes[i] - video.decodeTimes[i]
                                    var run = 1
                                    while (i + run < count &&
                                        video.presentationTimes[i + run] - video.decodeTimes[i + run] == offset
                                    ) run++
                                    u32(run.toLong())
                                    u32(offset)
                                    entries++
                                    i += run
                                }
                                patchU32(entryCountAt, entries)
                            }
                        }
                        fullBox("stss", 0, 0) {
                            u32(video.syncSamples.size.toLong())
                            for (i in 0 until video.syncSamples.size) u32(video.syncSamples[i].toLong())
                        }
                        chunkTables(video.chunks)
                    }
                }
            }
        }
    }

    private fun ByteBuf.audioTrak(audio: AudioTrackData, durationUnits: Long, emptyMs: Long, presentationMs: Long) {
        val count = audio.chunks.sizes.size
        val deltas = LongList()
        for (i in 0 until count - 1) deltas.add(audio.times[i + 1] - audio.times[i])
        deltas.add(SAMPLES_PER_AAC_FRAME)
        box("trak") {
            tkhd(2L, emptyMs + presentationMs, true, 0, 0)
            elst(emptyMs, presentationMs, 0L)
            box("mdia") {
                mdhd(audio.sampleRate.toLong(), durationUnits)
                hdlr("soun", "SoundHandler")
                box("minf") {
                    fullBox("smhd", 0, 0) {
                        u32(0L)
                    }
                    dinf()
                    box("stbl") {
                        fullBox("stsd", 0, 0) {
                            u32(1L)
                            box("mp4a") {
                                zeros(6)
                                u16(1)
                                zeros(8)
                                u16(audio.channels)
                                u16(16)
                                u16(0)
                                u16(0)
                                u32(audio.sampleRate.toLong() shl 16)
                                fullBox("esds", 0, 0) {
                                    u8(0x03)
                                    u8(23 + audio.audioSpecificConfig.size)
                                    u16(0)
                                    u8(0)
                                    u8(0x04)
                                    u8(15 + audio.audioSpecificConfig.size)
                                    u8(0x40)
                                    u8(0x15)
                                    zeros(3)
                                    u32(0L)
                                    u32(0L)
                                    u8(0x05)
                                    u8(audio.audioSpecificConfig.size)
                                    bytes(audio.audioSpecificConfig)
                                    u8(0x06)
                                    u8(1)
                                    u8(2)
                                }
                            }
                        }
                        sttsFrom(deltas)
                        chunkTables(audio.chunks)
                    }
                }
            }
        }
    }
}
