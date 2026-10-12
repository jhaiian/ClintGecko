package com.jhaiian.clint.mediacapture

import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.mediacapture.download.HlsSubtitleResolver
import com.jhaiian.clint.mediacapture.download.StreamRequestHeaders
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import okhttp3.Request

object SubtitleFileProbe {

    data class Info(val cueCount: Int, val durationSeconds: Double?, val isHlsPlaylist: Boolean = false)

    private const val MAX_BYTES = 1024 * 1024
    private const val PREVIEW_MAX_BYTES = 512 * 1024
    private const val READ_CHUNK_BYTES = 16 * 1024
    private const val PROBE_MAX_SEGMENTS = 60

    private val timingRegex = Regex(
        """(?:(\d+):)?(\d{1,2}):(\d{2})[.,](\d{1,3})\s*-->\s*(?:(\d+):)?(\d{1,2}):(\d{2})[.,](\d{1,3})"""
    )
    private val dialogueRegex = Regex(
        """^Dialogue:[^,]*,[^,]*,(\d+):(\d{2}):(\d{2})[.](\d{1,3})""",
        RegexOption.MULTILINE
    )

    private val httpClient by lazy {
        ClintDownloadManager.httpClient.newBuilder().callTimeout(10, TimeUnit.SECONDS).build()
    }

    fun probe(media: DetectedMedia, pageUrl: String): Info? {
        val knownSize = media.sizeBytes
        if (knownSize != null && knownSize > MAX_BYTES) return null
        val builder = Request.Builder().url(media.url)
        StreamRequestHeaders.apply(builder, media.url, pageUrl, "", "", media.requestHeaders)
        val bytes = try {
            httpClient.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val out = ByteArrayOutputStream()
                val chunk = ByteArray(READ_CHUNK_BYTES)
                val stream = resp.body.byteStream()
                while (out.size() <= MAX_BYTES) {
                    val read = stream.read(chunk)
                    if (read <= 0) break
                    out.write(chunk, 0, read)
                }
                if (out.size() > MAX_BYTES) return null
                out.toByteArray()
            }
        } catch (_: Exception) {
            return null
        }
        val text = String(bytes, Charsets.UTF_8)
        if (HlsSubtitleResolver.looksLikePlaylist(text)) return probePlaylist(media, pageUrl)
        return parse(text)
    }

    private fun probePlaylist(media: DetectedMedia, pageUrl: String): Info {
        val track = HlsSubtitleResolver.resolveTrack(media.url, pageUrl, "", media.requestHeaders)
            ?: return Info(0, null, true)
        val duration = track.durationSeconds
        if (track.segments.isEmpty() || track.segments.size > PROBE_MAX_SEGMENTS) return Info(0, duration, true)
        val merged = HlsSubtitleResolver.downloadAndMerge(track, pageUrl, "", media.requestHeaders, { false }, httpClient)
            ?: return Info(0, duration, true)
        val parsed = parse(merged)
        return Info(parsed?.cueCount ?: 0, duration ?: parsed?.durationSeconds, true)
    }

    fun fetchText(media: DetectedMedia): String? {
        val builder = Request.Builder().url(media.url)
        StreamRequestHeaders.apply(builder, media.url, media.pageUrl, "", "", media.requestHeaders)
        val text = try {
            httpClient.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val out = ByteArrayOutputStream()
                val chunk = ByteArray(READ_CHUNK_BYTES)
                val stream = resp.body.byteStream()
                while (out.size() < PREVIEW_MAX_BYTES) {
                    val read = stream.read(chunk)
                    if (read <= 0) break
                    out.write(chunk, 0, read)
                }
                String(out.toByteArray(), Charsets.UTF_8).removePrefix("\uFEFF")
            }
        } catch (_: Exception) {
            return null
        }
        if (!HlsSubtitleResolver.looksLikePlaylist(text)) return text
        val track = HlsSubtitleResolver.resolveTrack(media.url, media.pageUrl, "", media.requestHeaders) ?: return null
        val merged = HlsSubtitleResolver.downloadAndMerge(track, media.pageUrl, "", media.requestHeaders, { false }, httpClient)
            ?: return null
        return merged.take(PREVIEW_MAX_BYTES)
    }

    private fun parse(text: String): Info? {
        var count = 0
        var maxEnd = 0.0
        for (match in timingRegex.findAll(text)) {
            count++
            val g = match.groupValues
            maxEnd = maxOf(maxEnd, toSeconds(g[5], g[6], g[7], g[8]))
        }
        if (count == 0) {
            for (match in dialogueRegex.findAll(text)) {
                count++
                val g = match.groupValues
                maxEnd = maxOf(maxEnd, toSeconds(g[1], g[2], g[3], g[4]))
            }
        }
        if (count == 0) return null
        return Info(count, maxEnd.takeIf { it > 0.0 })
    }

    private fun toSeconds(hours: String, minutes: String, seconds: String, fraction: String): Double {
        val h = hours.toIntOrNull() ?: 0
        val m = minutes.toIntOrNull() ?: 0
        val s = seconds.toIntOrNull() ?: 0
        val f = "0.$fraction".toDoubleOrNull() ?: 0.0
        return h * 3600.0 + m * 60.0 + s + f
    }
}
