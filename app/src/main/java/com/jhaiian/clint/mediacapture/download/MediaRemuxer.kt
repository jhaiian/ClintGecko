@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.jhaiian.clint.mediacapture.download

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.media3.common.Format
import androidx.media3.muxer.BufferInfo
import androidx.media3.muxer.Mp4Muxer
import androidx.media3.muxer.SeekableMuxerOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

data class RemuxResult(val success: Boolean, val errorMessage: String?)

object MediaRemuxer {

    private class CancelledRemuxException : Exception("Cancelled")

    private class Track(val extractorIndex: Int, val trackIndex: Int, val muxerTrackId: Int)

    private class PullState(
        val extractor: MediaExtractor,
        val muxerTrackId: Int,
        val offsetUs: Long
    ) {
        var buffer: ByteBuffer = ByteBuffer.allocateDirect(256 * 1024)
        var size = -1
        var ptsUs = 0L
        var flags = 0

        fun pull() {
            val sampleSize = extractor.sampleSize
            if (sampleSize < 0) {
                size = -1
                return
            }
            if (buffer.capacity() < sampleSize) {
                buffer = ByteBuffer.allocateDirect((sampleSize + 65536).toInt())
            }
            buffer.clear()
            size = extractor.readSampleData(buffer, 0)
            ptsUs = (extractor.sampleTime - offsetUs).coerceAtLeast(0)
            flags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                MediaCodec.BUFFER_FLAG_KEY_FRAME
            } else 0
            extractor.advance()
        }
    }

    suspend fun remux(
        @Suppress("UNUSED_PARAMETER") context: Context,
        inputPaths: List<String>,
        outputPath: String,
        onProgress: ((Int) -> Unit)? = null
    ): RemuxResult {
        if (inputPaths.isEmpty()) return RemuxResult(false, null)
        return withContext(Dispatchers.IO) {
            val extractors = mutableListOf<MediaExtractor>()
            try {
                for (path in inputPaths) {
                    val extractor = MediaExtractor()
                    extractor.setDataSource(path)
                    extractors += extractor
                }
                remuxTracks(extractors, inputPaths, outputPath, onProgress) { isActive }
            } catch (e: CancelledRemuxException) {
                RemuxResult(false, e.message)
            } catch (e: Exception) {
                RemuxResult(false, e.message)
            } finally {
                extractors.forEach { runCatching { it.release() } }
            }
        }
    }

    private fun findTrack(extractor: MediaExtractor, mimePrefix: String): Int {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
            if (mime != null && mime.startsWith(mimePrefix)) return i
        }
        return -1
    }

    private val NAL_START_CODE = byteArrayOf(0, 0, 0, 1)

    private fun annexBNalUnits(data: ByteArray): List<ByteArray> {
        val starts = mutableListOf<Int>()
        var i = 0
        while (i + 2 < data.size) {
            if (data[i] == 0.toByte() && data[i + 1] == 0.toByte() && data[i + 2] == 1.toByte()) {
                starts += i
                i += 3
            } else {
                i++
            }
        }
        if (starts.isEmpty()) return emptyList()
        val nals = mutableListOf<ByteArray>()
        for (idx in starts.indices) {
            val contentStart = starts[idx] + 3
            var contentEnd = if (idx + 1 < starts.size) starts[idx + 1] else data.size
            while (contentEnd > contentStart && data[contentEnd - 1] == 0.toByte()) {
                contentEnd--
            }
            if (contentEnd > contentStart) {
                nals += data.copyOfRange(contentStart, contentEnd)
            }
        }
        return nals
    }

    private fun firstSampleNalUnits(extractor: MediaExtractor): List<ByteArray> {
        return try {
            val sampleSize = extractor.sampleSize
            if (sampleSize < 0) return emptyList()
            val buffer = ByteBuffer.allocateDirect((sampleSize + 64).toInt())
            val size = extractor.readSampleData(buffer, 0)
            if (size <= 0) return emptyList()
            val bytes = ByteArray(size)
            buffer.position(0)
            buffer.get(bytes)
            annexBNalUnits(bytes)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun buildAvcInitializationData(
        rawCsd: List<ByteArray>,
        fallbackExtractor: MediaExtractor?
    ): List<ByteArray> {
        val nals = rawCsd.flatMap { annexBNalUnits(it) }
        var sps = nals.firstOrNull { it.isNotEmpty() && (it[0].toInt() and 0x1F) == 7 }
        var pps = nals.firstOrNull { it.isNotEmpty() && (it[0].toInt() and 0x1F) == 8 }
        if ((sps == null || pps == null) && fallbackExtractor != null) {
            val sampleNals = firstSampleNalUnits(fallbackExtractor)
            if (sps == null) sps = sampleNals.firstOrNull { it.isNotEmpty() && (it[0].toInt() and 0x1F) == 7 }
            if (pps == null) pps = sampleNals.firstOrNull { it.isNotEmpty() && (it[0].toInt() and 0x1F) == 8 }
        }
        if (sps == null || pps == null) return rawCsd
        return listOf(NAL_START_CODE + sps, NAL_START_CODE + pps)
    }

    private fun buildHevcInitializationData(
        rawCsd: List<ByteArray>,
        fallbackExtractor: MediaExtractor?
    ): List<ByteArray> {
        val nals = rawCsd.flatMap { annexBNalUnits(it) }
        fun typeOf(n: ByteArray) = (n[0].toInt() shr 1) and 0x3F
        var vps = nals.firstOrNull { it.isNotEmpty() && typeOf(it) == 32 }
        var sps = nals.firstOrNull { it.isNotEmpty() && typeOf(it) == 33 }
        var pps = nals.firstOrNull { it.isNotEmpty() && typeOf(it) == 34 }
        if ((vps == null || sps == null || pps == null) && fallbackExtractor != null) {
            val sampleNals = firstSampleNalUnits(fallbackExtractor)
            if (vps == null) vps = sampleNals.firstOrNull { it.isNotEmpty() && typeOf(it) == 32 }
            if (sps == null) sps = sampleNals.firstOrNull { it.isNotEmpty() && typeOf(it) == 33 }
            if (pps == null) pps = sampleNals.firstOrNull { it.isNotEmpty() && typeOf(it) == 34 }
        }
        if (vps == null || sps == null || pps == null) return rawCsd
        return listOf(NAL_START_CODE + vps, NAL_START_CODE + sps, NAL_START_CODE + pps)
    }

    private fun buildFormat(mediaFormat: MediaFormat, fallbackExtractor: MediaExtractor? = null): Format {
        val builder = Format.Builder()
        val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
        mime?.let { builder.setSampleMimeType(it) }
        if (mediaFormat.containsKey(MediaFormat.KEY_WIDTH)) {
            builder.setWidth(mediaFormat.getInteger(MediaFormat.KEY_WIDTH))
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_HEIGHT)) {
            builder.setHeight(mediaFormat.getInteger(MediaFormat.KEY_HEIGHT))
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_ROTATION)) {
            builder.setRotationDegrees(mediaFormat.getInteger(MediaFormat.KEY_ROTATION))
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            builder.setSampleRate(mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE))
        }
        if (mediaFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            builder.setChannelCount(mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT))
        }
        mediaFormat.getString(MediaFormat.KEY_LANGUAGE)?.let { builder.setLanguage(it) }
        val rawCsd = mutableListOf<ByteArray>()
        var i = 0
        while (true) {
            val key = "csd-$i"
            if (!mediaFormat.containsKey(key)) break
            val csd = mediaFormat.getByteBuffer(key) ?: break
            val bytes = ByteArray(csd.remaining())
            csd.duplicate().get(bytes)
            rawCsd += bytes
            i++
        }
        val initData = when (mime) {
            MediaFormat.MIMETYPE_VIDEO_AVC -> buildAvcInitializationData(rawCsd, fallbackExtractor)
            MediaFormat.MIMETYPE_VIDEO_HEVC -> buildHevcInitializationData(rawCsd, fallbackExtractor)
            else -> rawCsd
        }
        if (initData.isNotEmpty()) builder.setInitializationData(initData)
        return builder.build()
    }

    private fun remuxTracks(
        extractors: List<MediaExtractor>,
        inputPaths: List<String>,
        outputPath: String,
        onProgress: ((Int) -> Unit)?,
        isActive: () -> Boolean
    ): RemuxResult {
        val videoTrackIndices = extractors.map { findTrack(it, "video/") }
        val audioTrackIndices = extractors.map { findTrack(it, "audio/") }

        val videoSourceIdx = when {
            videoTrackIndices[0] >= 0 -> 0
            extractors.size > 1 && videoTrackIndices[1] >= 0 -> 1
            else -> -1
        }
        val audioSourceIdx = when {
            extractors.size > 1 && audioTrackIndices[1] >= 0 -> 1
            audioTrackIndices[0] >= 0 -> 0
            else -> -1
        }
        if (videoSourceIdx < 0 && audioSourceIdx < 0) {
            return RemuxResult(false, "No playable video or audio streams found")
        }

        val outputStream = FileOutputStream(outputPath)
        val muxer = try {
            Mp4Muxer.Builder(SeekableMuxerOutput.of(outputStream))
                .setAttemptStreamableOutputEnabled(false)
                .setSampleBatchingEnabled(true)
                .build()
        } catch (e: Exception) {
            runCatching { outputStream.close() }
            return RemuxResult(false, e.message)
        }

        return try {
            val tracks = mutableListOf<Track>()
            if (videoSourceIdx >= 0) {
                val trackIndex = videoTrackIndices[videoSourceIdx]
                val extractor = extractors[videoSourceIdx]
                extractor.selectTrack(trackIndex)
                val muxerTrackId = muxer.addTrack(buildFormat(extractor.getTrackFormat(trackIndex), extractor))
                tracks += Track(videoSourceIdx, trackIndex, muxerTrackId)
            }
            if (audioSourceIdx >= 0) {
                val trackIndex = audioTrackIndices[audioSourceIdx]
                val extractor = extractors[audioSourceIdx]
                extractor.selectTrack(trackIndex)
                val muxerTrackId = muxer.addTrack(buildFormat(extractor.getTrackFormat(trackIndex)))
                tracks += Track(audioSourceIdx, trackIndex, muxerTrackId)
            }

            val offsetByExtractor = mutableMapOf<Int, Long>()
            for (extractorIndex in tracks.map { it.extractorIndex }.distinct()) {
                val startUs = extractors[extractorIndex].sampleTime
                offsetByExtractor[extractorIndex] = if (startUs < 0) 0L else startUs
            }

            val totalDurationUs = tracks.maxOfOrNull { track ->
                val fmt = extractors[track.extractorIndex].getTrackFormat(track.trackIndex)
                if (fmt.containsKey(MediaFormat.KEY_DURATION)) fmt.getLong(MediaFormat.KEY_DURATION) else 0L
            } ?: 0L
            val totalBytes = inputPaths.sumOf { runCatching { File(it).length() }.getOrDefault(0L) }

            var lastPercent = -1
            var sampleCount = 0L
            var maxPtsUs = 0L
            var bytesProcessed = 0L

            fun reportProgress() {
                val percent = when {
                    totalDurationUs > 0 -> (maxPtsUs * 100 / totalDurationUs).toInt()
                    totalBytes > 0 -> (bytesProcessed * 100 / totalBytes).toInt()
                    else -> -1
                }
                if (percent < 0) return
                val clamped = percent.coerceIn(0, 99)
                if (clamped != lastPercent) {
                    lastPercent = clamped
                    onProgress?.invoke(clamped)
                }
            }

            fun checkCancelled() {
                if ((sampleCount and 0x3F) == 0L && !isActive()) throw CancelledRemuxException()
            }

            val distinctExtractorIndices = tracks.map { it.extractorIndex }.distinct()
            if (distinctExtractorIndices.size <= 1) {
                val extractorIndex = tracks.first().extractorIndex
                val extractor = extractors[extractorIndex]
                val offset = offsetByExtractor.getValue(extractorIndex)
                val trackIdToMuxerId = tracks.associate { it.trackIndex to it.muxerTrackId }
                var buffer = ByteBuffer.allocateDirect(256 * 1024)
                while (true) {
                    checkCancelled()
                    val sampleSize = extractor.sampleSize
                    if (sampleSize < 0) break
                    if (buffer.capacity() < sampleSize) {
                        buffer = ByteBuffer.allocateDirect((sampleSize + 65536).toInt())
                    }
                    buffer.clear()
                    val size = extractor.readSampleData(buffer, 0)
                    val muxerTrackId = trackIdToMuxerId[extractor.getSampleTrackIndex()]
                    if (muxerTrackId == null) {
                        extractor.advance()
                        continue
                    }
                    val ptsUs = (extractor.sampleTime - offset).coerceAtLeast(0)
                    val flags = if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                        MediaCodec.BUFFER_FLAG_KEY_FRAME
                    } else 0
                    buffer.position(0)
                    buffer.limit(size)
                    muxer.writeSampleData(muxerTrackId, buffer, BufferInfo(ptsUs, size, flags))
                    maxPtsUs = maxOf(maxPtsUs, ptsUs)
                    bytesProcessed += size
                    sampleCount++
                    reportProgress()
                    extractor.advance()
                }
            } else {
                val states = tracks.map { track ->
                    PullState(extractors[track.extractorIndex], track.muxerTrackId, offsetByExtractor.getValue(track.extractorIndex))
                }
                states.forEach { it.pull() }
                while (true) {
                    checkCancelled()
                    val next = states.filter { it.size >= 0 }.minByOrNull { it.ptsUs } ?: break
                    next.buffer.position(0)
                    next.buffer.limit(next.size)
                    muxer.writeSampleData(next.muxerTrackId, next.buffer, BufferInfo(next.ptsUs, next.size, next.flags))
                    maxPtsUs = maxOf(maxPtsUs, next.ptsUs)
                    bytesProcessed += next.size
                    sampleCount++
                    reportProgress()
                    next.pull()
                }
            }

            muxer.close()
            onProgress?.invoke(100)
            RemuxResult(true, null)
        } catch (e: CancelledRemuxException) {
            runCatching { muxer.close() }
            RemuxResult(false, "Cancelled")
        } catch (e: Exception) {
            runCatching { muxer.close() }
            RemuxResult(false, e.message)
        }
    }
}
