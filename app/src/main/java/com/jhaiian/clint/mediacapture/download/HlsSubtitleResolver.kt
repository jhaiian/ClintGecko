package com.jhaiian.clint.mediacapture.download

import com.jhaiian.clint.downloads.ClintDownloadManager
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.math.abs
import okhttp3.OkHttpClient
import okhttp3.Request

object HlsSubtitleResolver {

    class Fetched(val bytes: ByteArray, val extension: String, val fromPlaylist: Boolean)

    private const val SNIFF_BYTES = 64
    private const val MAX_PLAIN_BYTES = 8 * 1024 * 1024
    private const val MAX_SEGMENT_BYTES = 8 * 1024 * 1024
    private const val SEGMENT_ATTEMPTS = 3
    private const val SEGMENT_THREADS = 6
    private const val READ_CHUNK_BYTES = 16 * 1024
    private const val MPEGTS_CLOCK = 90000.0

    private val timingRegex = Regex(
        """^((?:\d+:)?\d{1,2}:\d{2}[.,]\d{1,3})\s*-->\s*((?:\d+:)?\d{1,2}:\d{2}[.,]\d{1,3})(.*)$"""
    )
    private val mpegtsRegex = Regex("""MPEGTS:(\d+)""")
    private val localRegex = Regex("""LOCAL:([0-9:.,]+)""")

    fun looksLikePlaylist(text: String): Boolean =
        text.trimStart('\uFEFF', ' ', '\t', '\r', '\n').startsWith("#EXTM3U")

    fun sniffPlaylist(
        url: String,
        pageUrl: String,
        userAgent: String,
        headers: Map<String, String>,
        client: OkHttpClient = ClintDownloadManager.httpClient
    ): Boolean {
        val builder = Request.Builder().url(url)
        StreamRequestHeaders.apply(builder, url, pageUrl, "", userAgent, headers)
        return try {
            client.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return false
                val prefix = readPrefix(resp.body.byteStream(), SNIFF_BYTES)
                looksLikePlaylist(String(prefix, Charsets.UTF_8))
            }
        } catch (_: Exception) {
            false
        }
    }

    fun resolveTrack(
        url: String,
        pageUrl: String,
        userAgent: String,
        headers: Map<String, String>
    ): ResolvedTrack? = HlsPlaylistFetcher.fetch(url, pageUrl, "", userAgent, TrackKind.VIDEO, headers)

    fun fetch(
        url: String,
        pageUrl: String,
        userAgent: String,
        headers: Map<String, String>,
        isCancelled: () -> Boolean,
        client: OkHttpClient = ClintDownloadManager.httpClient,
        onProgress: ((completed: Int, total: Int, bytes: Long) -> Unit)? = null
    ): Fetched? {
        val first = download(url, pageUrl, userAgent, headers, null, null, client, MAX_PLAIN_BYTES) ?: return null
        val text = String(first, Charsets.UTF_8)
        if (!looksLikePlaylist(text)) {
            return Fetched(first, plainExtension(url, text), false)
        }
        val track = resolveTrack(url, pageUrl, userAgent, headers) ?: return null
        val merged = downloadAndMerge(track, pageUrl, userAgent, headers, isCancelled, client, onProgress) ?: return null
        return Fetched(merged.toByteArray(Charsets.UTF_8), "vtt", true)
    }

    fun downloadAndMerge(
        track: ResolvedTrack,
        pageUrl: String,
        userAgent: String,
        headers: Map<String, String>,
        isCancelled: () -> Boolean,
        client: OkHttpClient = ClintDownloadManager.httpClient,
        onProgress: ((completed: Int, total: Int, bytes: Long) -> Unit)? = null
    ): String? {
        val specs = track.segments
        if (specs.isEmpty()) return null
        val pool = Executors.newFixedThreadPool(minOf(SEGMENT_THREADS, specs.size))
        try {
            val futures = specs.map { spec ->
                pool.submit(Callable { fetchSegment(spec, pageUrl, userAgent, headers, client) })
            }
            val texts = ArrayList<String>(specs.size)
            var totalBytes = 0L
            for ((index, future) in futures.withIndex()) {
                if (isCancelled()) throw StreamCancelledException()
                val data = try {
                    future.get()
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return null
                } catch (_: Exception) {
                    return null
                } ?: return null
                totalBytes += data.size
                texts += String(data, Charsets.UTF_8)
                onProgress?.invoke(index + 1, specs.size, totalBytes)
            }
            return merge(texts)
        } finally {
            pool.shutdownNow()
        }
    }

    private fun plainExtension(url: String, text: String): String {
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        return when {
            text.trimStart('\uFEFF', ' ', '\t', '\r', '\n').startsWith("WEBVTT") -> "vtt"
            cleanUrl.endsWith(".srt", ignoreCase = true) -> "srt"
            else -> "vtt"
        }
    }

    private fun fetchSegment(
        spec: SegmentSpec,
        pageUrl: String,
        userAgent: String,
        headers: Map<String, String>,
        client: OkHttpClient
    ): ByteArray? {
        var attempt = 0
        while (attempt < SEGMENT_ATTEMPTS) {
            val data = download(
                spec.url, pageUrl, userAgent, headers,
                spec.byteRangeOffset, spec.byteRangeLength, client, MAX_SEGMENT_BYTES
            )
            if (data != null) {
                val key = spec.key ?: return data
                return HlsAesDecryptor.decrypt(data, key, pageUrl, "", userAgent, headers)
            }
            attempt++
            if (attempt < SEGMENT_ATTEMPTS) Thread.sleep(300L * attempt)
        }
        return null
    }

    private fun download(
        url: String,
        pageUrl: String,
        userAgent: String,
        headers: Map<String, String>,
        rangeOffset: Long?,
        rangeLength: Long?,
        client: OkHttpClient,
        maxBytes: Int
    ): ByteArray? {
        val builder = Request.Builder().url(url)
        StreamRequestHeaders.apply(builder, url, pageUrl, "", userAgent, headers)
        val ranged = rangeOffset != null && rangeLength != null && rangeLength > 0L
        if (ranged) {
            builder.header("Range", "bytes=$rangeOffset-${rangeOffset + rangeLength - 1}")
        }
        return try {
            client.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val data = readLimited(resp.body.byteStream(), maxBytes) ?: return null
                if (ranged && resp.code == 200) {
                    val start = rangeOffset.toInt()
                    val end = start + rangeLength.toInt()
                    if (end <= data.size) data.copyOfRange(start, end) else null
                } else {
                    data
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun readPrefix(stream: InputStream, limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val chunk = ByteArray(limit)
        while (out.size() < limit) {
            val read = stream.read(chunk, 0, limit - out.size())
            if (read <= 0) break
            out.write(chunk, 0, read)
        }
        return out.toByteArray()
    }

    private fun readLimited(stream: InputStream, limit: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val chunk = ByteArray(READ_CHUNK_BYTES)
        while (true) {
            val read = stream.read(chunk)
            if (read < 0) break
            out.write(chunk, 0, read)
            if (out.size() > limit) return null
        }
        return out.toByteArray()
    }

    private fun splitBlocks(text: String): List<List<String>> {
        val blocks = ArrayList<List<String>>()
        var current = ArrayList<String>()
        for (line in text.replace("\r\n", "\n").replace('\r', '\n').split('\n')) {
            if (line.isBlank()) {
                if (current.isNotEmpty()) {
                    blocks += current
                    current = ArrayList()
                }
            } else {
                current += line
            }
        }
        if (current.isNotEmpty()) blocks += current
        return blocks
    }

    private fun merge(texts: List<String>): String? {
        val headerExtras = ArrayList<String>()
        val preludeBlocks = LinkedHashSet<String>()
        val cueSeen = HashSet<String>()
        val cues = StringBuilder()
        var baseOffset: Double? = null

        for ((index, raw) in texts.withIndex()) {
            val blocks = splitBlocks(raw.removePrefix("\uFEFF"))
            if (blocks.isEmpty() || !blocks[0][0].trimStart().startsWith("WEBVTT")) return null

            val header = blocks[0]
            val headerLines = ArrayList<String>()
            var offset: Double? = null
            var inlineCueStart = -1
            for (i in 1 until header.size) {
                val line = header[i]
                if (line.contains("-->")) {
                    inlineCueStart = i
                    break
                }
                if (line.trimStart().startsWith("X-TIMESTAMP-MAP")) {
                    val mpegts = mpegtsRegex.find(line)?.groupValues?.get(1)?.toLongOrNull()
                    val local = localRegex.find(line)?.groupValues?.get(1)
                    if (mpegts != null && local != null) {
                        offset = mpegts / MPEGTS_CLOCK - parseSeconds(local)
                    }
                } else {
                    headerLines += line
                }
            }
            if (index == 0) headerExtras += headerLines
            if (baseOffset == null && offset != null) baseOffset = offset

            val shift = if (offset != null && baseOffset != null) offset - baseOffset else 0.0

            val bodyBlocks = ArrayList<List<String>>()
            if (inlineCueStart >= 0) bodyBlocks += header.subList(inlineCueStart, header.size)
            bodyBlocks += blocks.drop(1)

            for (block in bodyBlocks) {
                val timingIndex = block.indexOfFirst { it.contains("-->") }
                if (timingIndex < 0) {
                    val first = block[0].trim()
                    if (first.startsWith("STYLE") || first.startsWith("REGION")) {
                        preludeBlocks += block.joinToString("\n")
                    }
                    continue
                }
                val match = timingRegex.matchEntire(block[timingIndex].trim()) ?: continue
                val timingLine = if (abs(shift) < 0.0005) {
                    block[timingIndex].trim()
                } else {
                    val start = parseSeconds(match.groupValues[1]) + shift
                    val end = parseSeconds(match.groupValues[2]) + shift
                    "${formatTime(start)} --> ${formatTime(end)}${match.groupValues[3]}"
                }
                val payload = block.subList(timingIndex + 1, block.size)
                if (!cueSeen.add(timingLine + "\n" + payload.joinToString("\n"))) continue
                for (i in 0 until timingIndex) cues.append(block[i]).append('\n')
                cues.append(timingLine).append('\n')
                for (line in payload) cues.append(line).append('\n')
                cues.append('\n')
            }
        }

        val out = StringBuilder("WEBVTT\n")
        for (line in headerExtras) out.append(line).append('\n')
        out.append('\n')
        for (block in preludeBlocks) out.append(block).append("\n\n")
        out.append(cues)
        return out.toString()
    }

    private fun parseSeconds(value: String): Double {
        var total = 0.0
        for (part in value.replace(',', '.').split(':')) {
            total = total * 60.0 + (part.toDoubleOrNull() ?: 0.0)
        }
        return total
    }

    private fun formatTime(seconds: Double): String {
        val totalMs = Math.round(seconds.coerceAtLeast(0.0) * 1000.0)
        val hours = totalMs / 3_600_000L
        val minutes = (totalMs / 60_000L) % 60L
        val secs = (totalMs / 1000L) % 60L
        val millis = totalMs % 1000L
        return String.format(Locale.US, "%02d:%02d:%02d.%03d", hours, minutes, secs, millis)
    }
}
