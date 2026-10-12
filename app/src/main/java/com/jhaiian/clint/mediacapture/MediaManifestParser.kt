package com.jhaiian.clint.mediacapture

import android.util.Xml
import java.io.StringReader
import java.net.URI
import java.util.UUID
import org.xmlpull.v1.XmlPullParser

object MediaManifestParser {

    private val ATTR_REGEX = Regex("""([A-Za-z0-9\-]+)=("([^"]*)"|[^,]*)""")
    private val VIDEO_CODEC_PREFIXES = listOf("avc1", "avc3", "hev1", "hvc1", "vp8", "vp9", "vp09", "av01", "mp4v", "theora")
    private val AUDIO_CODEC_PREFIXES = listOf("mp4a", "ac-3", "ec-3", "opus", "vorbis", "flac", "alac", "mp3")

    private fun kindFromCodecs(codecs: String?): MediaKind? {
        if (codecs.isNullOrBlank()) return null
        val parts = codecs.split(",").map { it.trim().lowercase() }
        val hasVideo = parts.any { part -> VIDEO_CODEC_PREFIXES.any { part.startsWith(it) } }
        val hasAudio = parts.any { part -> AUDIO_CODEC_PREFIXES.any { part.startsWith(it) } }
        return when {
            hasVideo -> MediaKind.VIDEO
            hasAudio -> MediaKind.AUDIO
            else -> null
        }
    }

    /** Whether a CODECS attribute value (e.g. "avc1.640028,mp4a.40.2") lists an audio codec. */
    private fun codecsHaveAudio(codecs: String?): Boolean {
        if (codecs.isNullOrBlank()) return false
        val parts = codecs.split(",").map { it.trim().lowercase() }
        return parts.any { part -> AUDIO_CODEC_PREFIXES.any { part.startsWith(it) } }
    }

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

    private fun resolveUrl(base: String, relative: String): String {
        val trimmed = relative.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        return try {
            URI(base).resolve(trimmed).toString()
        } catch (_: Exception) {
            trimmed
        }
    }

    fun parseHls(text: String, manifestUrl: String): List<DetectedMedia> {
        val lines = text.lines()
        val isMaster = lines.any { it.trim().startsWith("#EXT-X-STREAM-INF") }

        if (!isMaster) {
            var durationTotal = 0.0
            var isLive = true
            for (raw in lines) {
                val line = raw.trim()
                if (line.startsWith("#EXTINF:")) {
                    val d = line.removePrefix("#EXTINF:").substringBefore(',').toDoubleOrNull() ?: 0.0
                    durationTotal += d
                } else if (line.startsWith("#EXT-X-ENDLIST")) {
                    isLive = false
                }
            }
            return listOf(
                DetectedMedia(
                    id = UUID.randomUUID().toString(),
                    url = manifestUrl,
                    kind = MediaKind.VIDEO,
                    format = "HLS",
                    mimeType = "application/vnd.apple.mpegurl",
                    durationSeconds = durationTotal.takeIf { it > 0.0 },
                    isLive = isLive,
                    groupUrl = manifestUrl
                )
            )
        }

        val results = mutableListOf<DetectedMedia>()
        var pendingVariantAttrs: Map<String, String>? = null

        for (raw in lines) {
            val line = raw.trim()
            when {
                line.startsWith("#EXT-X-STREAM-INF:") -> {
                    pendingVariantAttrs = parseAttributeList(line.substringAfter(":"))
                }
                line.startsWith("#EXT-X-MEDIA:") -> {
                    val attrs = parseAttributeList(line.substringAfter(":"))
                    val uri = attrs["URI"] ?: continue
                    val kind = when (attrs["TYPE"]) {
                        "AUDIO" -> MediaKind.AUDIO
                        "SUBTITLES" -> MediaKind.SUBTITLE
                        else -> null
                    } ?: continue
                    val resolvedUri = resolveUrl(manifestUrl, uri)
                    val uriExt = resolvedUri.substringAfterLast('/').substringAfterLast('.', "").lowercase()
                    val mime = when {
                        kind == MediaKind.SUBTITLE && uriExt == "srt" -> "application/x-subrip"
                        kind == MediaKind.SUBTITLE -> "text/vtt"
                        uriExt == "mp3" -> "audio/mpeg"
                        uriExt == "aac" -> "audio/aac"
                        uriExt == "ac3" -> "audio/ac3"
                        uriExt == "eac3" -> "audio/eac3"
                        else -> "audio/mp4"
                    }
                    results += DetectedMedia(
                        id = UUID.randomUUID().toString(),
                        url = resolvedUri,
                        kind = kind,
                        format = "HLS",
                        mimeType = mime,
                        label = attrs["NAME"],
                        groupUrl = manifestUrl,
                        mediaGroupId = attrs["GROUP-ID"],
                        language = attrs["LANGUAGE"],
                        isDefaultTrack = attrs["DEFAULT"]?.equals("YES", ignoreCase = true) == true
                    )
                }
                line.isNotEmpty() && !line.startsWith("#") -> {
                    val attrs = pendingVariantAttrs
                    pendingVariantAttrs = null
                    val resolution = attrs?.get("RESOLUTION")
                    val parts = resolution?.split("x")
                    val width = parts?.getOrNull(0)?.toIntOrNull()
                    val height = parts?.getOrNull(1)?.toIntOrNull()
                    val bandwidth = (attrs?.get("AVERAGE-BANDWIDTH") ?: attrs?.get("BANDWIDTH"))?.toLongOrNull()
                    val codecs = attrs?.get("CODECS")
                    val kind = when {
                        width != null || height != null -> MediaKind.VIDEO
                        else -> kindFromCodecs(codecs) ?: MediaKind.VIDEO
                    }
                    // If the variant declares an AUDIO group, its own segments are video-only and
                    // audio is delivered by a separate #EXT-X-MEDIA rendition (common on sites that
                    // serve independent audio/video tracks). Otherwise fall back to CODECS, when present.
                    val hasAudio = when {
                        kind != MediaKind.VIDEO -> null
                        attrs?.get("AUDIO") != null -> false
                        codecs != null -> codecsHaveAudio(codecs)
                        else -> null
                    }
                    results += DetectedMedia(
                        id = UUID.randomUUID().toString(),
                        url = resolveUrl(manifestUrl, line),
                        kind = kind,
                        format = "HLS",
                        mimeType = "application/vnd.apple.mpegurl",
                        width = width,
                        height = height,
                        bandwidthBitsPerSec = bandwidth,
                        hasAudio = hasAudio,
                        groupUrl = manifestUrl,
                        audioGroupRef = attrs?.get("AUDIO")
                    )
                }
            }
        }
        return results
    }

    private fun parseIso8601Duration(s: String): Double? {
        val m = Regex("""PT(?:([\d.]+)H)?(?:([\d.]+)M)?(?:([\d.]+)S)?""").matchEntire(s.trim()) ?: return null
        val h = m.groupValues[1].toDoubleOrNull() ?: 0.0
        val mi = m.groupValues[2].toDoubleOrNull() ?: 0.0
        val sec = m.groupValues[3].toDoubleOrNull() ?: 0.0
        val total = h * 3600.0 + mi * 60.0 + sec
        return total.takeIf { it > 0.0 }
    }

    fun parseDash(text: String, manifestUrl: String): List<DetectedMedia> {
        val results = mutableListOf<DetectedMedia>()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(text))

            var eventType = parser.eventType
            var mpdDuration: Double? = null
            var mpdIsLive = false
            var currentAdaptationMime: String? = null
            var currentAdaptationContentType: String? = null
            var currentAdaptationCodecs: String? = null
            var currentAdaptationLang: String? = null
            var currentAdaptationIsMain: Boolean = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "MPD" -> {
                            mpdDuration = parser.getAttributeValue(null, "mediaPresentationDuration")
                                ?.let(::parseIso8601Duration)
                            mpdIsLive = parser.getAttributeValue(null, "type") == "dynamic"
                        }
                        "AdaptationSet" -> {
                            currentAdaptationMime = parser.getAttributeValue(null, "mimeType")
                            currentAdaptationContentType = parser.getAttributeValue(null, "contentType")
                            currentAdaptationCodecs = parser.getAttributeValue(null, "codecs")
                            currentAdaptationLang = parser.getAttributeValue(null, "lang")
                            currentAdaptationIsMain = false
                        }
                        "Role" -> {
                            if (parser.getAttributeValue(null, "value") == "main") currentAdaptationIsMain = true
                        }
                        "Representation" -> {
                            val width = parser.getAttributeValue(null, "width")?.toIntOrNull()
                            val height = parser.getAttributeValue(null, "height")?.toIntOrNull()
                            val bandwidth = parser.getAttributeValue(null, "bandwidth")?.toLongOrNull()
                            val repId = parser.getAttributeValue(null, "id")
                            val mime = parser.getAttributeValue(null, "mimeType") ?: currentAdaptationMime
                            val codecs = parser.getAttributeValue(null, "codecs") ?: currentAdaptationCodecs
                            val kind = when {
                                mime?.startsWith("video/") == true || currentAdaptationContentType == "video" -> MediaKind.VIDEO
                                mime?.startsWith("audio/") == true || currentAdaptationContentType == "audio" -> MediaKind.AUDIO
                                width != null || height != null -> MediaKind.VIDEO
                                else -> MediaKind.AUDIO
                            }
                            // DASH splits video and audio into separate AdaptationSets almost universally
                            // (that's how adaptive bitrate switching works), so a video Representation has
                            // no embedded audio unless its own CODECS explicitly also lists an audio codec
                            // (the rare "muxed" fallback representation).
                            val hasAudio = if (kind == MediaKind.VIDEO) codecsHaveAudio(codecs) else null
                            results += DetectedMedia(
                                id = UUID.randomUUID().toString(),
                                url = manifestUrl,
                                kind = kind,
                                format = "DASH",
                                mimeType = mime,
                                width = width,
                                height = height,
                                bandwidthBitsPerSec = bandwidth,
                                durationSeconds = mpdDuration,
                                isLive = mpdIsLive,
                                hasAudio = hasAudio,
                                groupUrl = manifestUrl,
                                language = currentAdaptationLang,
                                isDefaultTrack = currentAdaptationIsMain,
                                representationId = repId
                            )
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {
        }
        return results
    }
}
