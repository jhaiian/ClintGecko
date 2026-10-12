package com.jhaiian.clint.mediacapture

import android.media.MediaDataSource
import android.media.MediaExtractor
import android.media.MediaFormat
import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.mediacapture.download.HlsAesDecryptor
import com.jhaiian.clint.mediacapture.download.ResolvedTrack
import com.jhaiian.clint.mediacapture.download.SegmentSpec
import com.jhaiian.clint.mediacapture.download.StreamRequestHeaders
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import okhttp3.Request

object HlsResolutionProbe {

    private const val MAX_SEGMENT_BYTES = 1024 * 1024
    private const val MAX_INIT_BYTES = 256 * 1024
    private const val READ_CHUNK_BYTES = 16 * 1024

    private val probeHttpClient by lazy {
        ClintDownloadManager.httpClient.newBuilder().callTimeout(20, TimeUnit.SECONDS).build()
    }

    private class BytesDataSource(private val data: ByteArray) : MediaDataSource() {
        override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
            if (position < 0L || position >= data.size) return -1
            val count = minOf(size.toLong(), data.size - position).toInt()
            System.arraycopy(data, position.toInt(), buffer, offset, count)
            return count
        }

        override fun getSize(): Long = data.size.toLong()

        override fun close() = Unit
    }

    fun probe(track: ResolvedTrack, pageUrl: String, headers: Map<String, String>): Pair<Int, Int>? {
        val initBytes = track.initSegment?.let { fetchPrefix(it, pageUrl, headers, MAX_INIT_BYTES) }
        val candidates = listOfNotNull(track.segments.firstOrNull(), track.segments.lastOrNull())
            .distinctBy { it.url to it.byteRangeOffset }
        for (segment in candidates) {
            val body = fetchPrefix(segment, pageUrl, headers, MAX_SEGMENT_BYTES) ?: continue
            val data = if (initBytes != null) initBytes + body else body
            readResolution(data)?.let { return it }
        }
        return if (candidates.isEmpty() && initBytes != null) readResolution(initBytes) else null
    }

    private fun fetchPrefix(
        spec: SegmentSpec,
        pageUrl: String,
        headers: Map<String, String>,
        limit: Int
    ): ByteArray? {
        val offset = spec.byteRangeOffset
        val length = spec.byteRangeLength
        val want = if (length != null) minOf(length, limit.toLong()).toInt() else limit
        if (want <= 0) return null
        val builder = Request.Builder().url(spec.url)
        StreamRequestHeaders.apply(builder, spec.url, pageUrl, "", "", headers)
        if (offset != null && length != null) {
            builder.header("Range", "bytes=$offset-${offset + want - 1}")
        }
        val raw = try {
            probeHttpClient.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return null
                if (offset != null && offset > 0L && resp.code != 206) return null
                val out = ByteArrayOutputStream()
                val chunk = ByteArray(READ_CHUNK_BYTES)
                val stream = resp.body.byteStream()
                while (out.size() < want) {
                    val read = stream.read(chunk, 0, minOf(chunk.size, want - out.size()))
                    if (read <= 0) break
                    out.write(chunk, 0, read)
                }
                out.toByteArray()
            }
        } catch (_: Exception) {
            return null
        }
        if (raw.isEmpty()) return null
        val key = spec.key ?: return raw
        return HlsAesDecryptor.decryptPrefix(raw, key, pageUrl, "", "", headers)
    }

    private fun readResolution(data: ByteArray): Pair<Int, Int>? {
        val extractor = MediaExtractor()
        var result: Pair<Int, Int>? = null
        try {
            extractor.setDataSource(BytesDataSource(data))
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (!mime.startsWith("video/")) continue
                if (!format.containsKey(MediaFormat.KEY_WIDTH) || !format.containsKey(MediaFormat.KEY_HEIGHT)) continue
                val width = format.getInteger(MediaFormat.KEY_WIDTH)
                val height = format.getInteger(MediaFormat.KEY_HEIGHT)
                if (width > 0 && height > 0) {
                    result = width to height
                    break
                }
            }
        } catch (_: Exception) {
        } finally {
            runCatching { extractor.release() }
        }
        return result
    }
}
