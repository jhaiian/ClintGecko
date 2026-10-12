package com.jhaiian.clint.mediacapture.download

import com.jhaiian.clint.downloads.ClintDownloadManager
import java.net.URI
import okhttp3.Request

object HlsPlaylistFetcher {

    private fun resolveUrl(base: String, relative: String): String {
        val trimmed = relative.trim().trim('"')
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        return try {
            URI(base).resolve(trimmed).toString()
        } catch (_: Exception) {
            trimmed
        }
    }

    private val ATTR_REGEX = Regex("""([A-Za-z0-9\-]+)=("([^"]*)"|[^,]*)""")

    private fun parseAttributeList(s: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        for (m in ATTR_REGEX.findAll(s)) {
            val key = m.groupValues[1].uppercase()
            val quoted = m.groupValues[3]
            val value = if (quoted.isNotEmpty()) quoted else m.groupValues[2].trim().trim('"')
            result[key] = value
        }
        return result
    }

    private fun parseByteRange(value: String, previousEnd: Long): Pair<Long, Long> {
        val parts = value.split("@")
        val length = parts[0].trim().toLongOrNull() ?: 0L
        val offset = parts.getOrNull(1)?.trim()?.toLongOrNull() ?: previousEnd
        return offset to length
    }

    private fun sequenceIv(sequenceNumber: Long): String {
        val bytes = ByteArray(16)
        var value = sequenceNumber
        for (i in 15 downTo 0) {
            bytes[i] = (value and 0xFF).toByte()
            value = value ushr 8
        }
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun fetch(
        playlistUrl: String,
        referer: String,
        cookies: String,
        userAgent: String,
        kind: TrackKind,
        extraHeaders: Map<String, String> = emptyMap()
    ): ResolvedTrack? {
        val builder = Request.Builder().url(playlistUrl)
        StreamRequestHeaders.apply(builder, playlistUrl, referer, cookies, userAgent, extraHeaders)

        val text = try {
            ClintDownloadManager.httpClient.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return null
                resp.body.string()
            }
        } catch (_: Exception) {
            return null
        }

        if (!text.trimStart('\uFEFF', ' ', '\t', '\r', '\n').startsWith("#EXTM3U")) return null

        val lines = text.lines()
        var sequenceNumber = 0L
        var lastByteRangeEnd = 0L
        var pendingByteRange: Pair<Long, Long>? = null
        var activeKey: HlsKeyInfo? = null
        var initSegment: SegmentSpec? = null
        var containerHint = "ts"
        val segments = mutableListOf<SegmentSpec>()
        var durationTotal = 0.0
        var hasEndlist = false
        var targetDuration: Double? = null

        for (raw in lines) {
            val line = raw.trim()
            when {
                line.startsWith("#EXT-X-MEDIA-SEQUENCE:") -> {
                    sequenceNumber = line.substringAfter(":").trim().toLongOrNull() ?: 0L
                }
                line.startsWith("#EXT-X-TARGETDURATION:") -> {
                    targetDuration = line.substringAfter(":").trim().toDoubleOrNull()
                }
                line.startsWith("#EXT-X-ENDLIST") -> {
                    hasEndlist = true
                }
                line.startsWith("#EXT-X-KEY:") -> {
                    val attrs = parseAttributeList(line.substringAfter(":"))
                    val method = attrs["METHOD"] ?: "NONE"
                    activeKey = if (method == "NONE") {
                        null
                    } else {
                        val uri = attrs["URI"]
                        if (uri != null) {
                            val ivRaw = attrs["IV"]?.removePrefix("0x")?.removePrefix("0X")
                            HlsKeyInfo(resolveUrl(playlistUrl, uri), ivRaw, method)
                        } else null
                    }
                }
                line.startsWith("#EXT-X-MAP:") -> {
                    val attrs = parseAttributeList(line.substringAfter(":"))
                    val uri = attrs["URI"]
                    if (uri != null) {
                        val br = attrs["BYTERANGE"]?.let { parseByteRange(it, 0L) }
                        containerHint = "mp4"
                        initSegment = SegmentSpec(
                            url = resolveUrl(playlistUrl, uri),
                            byteRangeOffset = br?.first,
                            byteRangeLength = br?.second,
                            key = activeKey
                        )
                    }
                }
                line.startsWith("#EXT-X-BYTERANGE:") -> {
                    val value = line.substringAfter(":").trim()
                    val (offset, length) = parseByteRange(value, lastByteRangeEnd)
                    pendingByteRange = offset to length
                    lastByteRangeEnd = offset + length
                }
                line.startsWith("#EXTINF:") -> {
                    val d = line.removePrefix("#EXTINF:").substringBefore(',').trim().toDoubleOrNull() ?: 0.0
                    durationTotal += d
                }
                line.isNotEmpty() && !line.startsWith("#") -> {
                    val br = pendingByteRange
                    pendingByteRange = null
                    if (containerHint == "ts" && (line.substringBefore('?').endsWith(".m4s") ||
                            line.substringBefore('?').endsWith(".mp4"))
                    ) {
                        containerHint = "mp4"
                    }
                    segments += SegmentSpec(
                        url = resolveUrl(playlistUrl, line),
                        byteRangeOffset = br?.first,
                        byteRangeLength = br?.second,
                        sequenceNumber = sequenceNumber,
                        key = activeKey?.let {
                            if (it.ivHex == null) it.copy(ivHex = sequenceIv(sequenceNumber)) else it
                        }
                    )
                    sequenceNumber++
                }
            }
        }

        if (segments.isEmpty() && initSegment == null) return null

        return ResolvedTrack(
            kind = kind,
            initSegment = initSegment,
            segments = segments,
            durationSeconds = durationTotal.takeIf { it > 0.0 },
            containerHintExtension = containerHint,
            isLive = !hasEndlist,
            targetDurationSeconds = targetDuration
        )
    }
}
