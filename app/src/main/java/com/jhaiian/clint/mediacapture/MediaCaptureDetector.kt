package com.jhaiian.clint.mediacapture

import android.net.Uri
import com.jhaiian.clint.browser.webview.ClintRequest
import com.jhaiian.clint.downloads.ClintDownloadManager
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.jhaiian.clint.mediacapture.download.HlsPlaylistFetcher
import com.jhaiian.clint.mediacapture.download.TrackKind
import okhttp3.Request

object MediaCaptureDetector {

    private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mkv", "mov", "m4v", "3gp", "3g2", "avi", "flv", "wmv")
    private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "oga", "opus", "wma", "weba")
    private val SUBTITLE_EXTENSIONS = setOf("vtt", "srt")
    private val MANIFEST_EXTENSIONS = setOf("m3u8", "mpd")
    private val SEGMENT_EXTENSIONS = setOf("ts", "m4s", "cmfv", "cmfa")
    private val NON_MEDIA_DESTS = setOf(
        "document", "iframe", "script", "style", "image", "font",
        "worker", "sharedworker", "manifest", "report", "object", "embed", "xslt"
    )
    private const val ENRICHMENT_TIMEOUT_MILLIS = 12_000L
    private val enrichmentPermits = Semaphore(3)
    private val UNSUPPORTED_METADATA_FORMATS = setOf("AVI", "FLV", "WMV")
    private val TRANSPORT_STREAM_CONTENT_TYPES = setOf("video/mp2t", "video/vnd.dlna.mpeg-tts", "video/iso.segment", "audio/iso.segment")
    private val MEDIA_DESTS = setOf("video", "audio", "track")
    private val CONTENT_TYPE_FORMATS = mapOf(
        "quicktime" to "MOV", "matroska" to "MKV", "msvideo" to "AVI", "ms-wmv" to "WMV", "ms-asf" to "WMV",
        "ms-wma" to "WMA", "3gpp" to "3GP", "3gpp2" to "3G2", "mp4a-latm" to "AAC", "aacp" to "AAC",
        "wave" to "WAV", "vnd.wave" to "WAV", "subrip" to "SRT", "mp2t" to "TS"
    )
    private const val MAX_LABEL_CHARS = 40
    private const val MAX_MANIFEST_BYTES = 3L * 1024 * 1024
    private const val MAX_MANIFEST_CHARS = MAX_MANIFEST_BYTES.toInt()
    private const val MIN_EXTENSIONLESS_MEDIA_BYTES = 20_000L
    private const val MIN_VIDEO_BYTES = 20_000L
    private const val SNIFF_BYTES = 512
    private val EXCLUDED_HEADERS = setOf(
        "range", "if-range", "if-modified-since", "if-none-match",
        "accept-encoding", "connection", "content-length", "host", "cookie"
    )
    private val FILENAME_REGEX = Regex("""filename\*?=(?:UTF-8''|"|')?([^";']+)""", RegexOption.IGNORE_CASE)

    private val probeHttpClient by lazy {
        ClintDownloadManager.httpClient.newBuilder().callTimeout(10, TimeUnit.SECONDS).build()
    }
    private val manifestHttpClient by lazy {
        ClintDownloadManager.httpClient.newBuilder().callTimeout(20, TimeUnit.SECONDS).build()
    }

    private fun sanitizeHeaders(headers: Map<String, String>?): Map<String, String> {
        if (headers.isNullOrEmpty()) return emptyMap()
        return headers.filterKeys { it.lowercase() !in EXCLUDED_HEADERS }
    }

    private fun extensionOf(uri: Uri): String =
        (uri.path ?: "").lowercase().substringAfterLast('/').substringAfterLast('.', "")

    private fun knownMediaExtension(ext: String): Boolean =
        ext in VIDEO_EXTENSIONS || ext in AUDIO_EXTENSIONS || ext in SUBTITLE_EXTENSIONS || ext in MANIFEST_EXTENSIONS

    private fun safeQueryParamNames(uri: Uri): Set<String> =
        runCatching { uri.queryParameterNames }.getOrNull() ?: emptySet()

    private fun extensionFromQuery(uri: Uri): String? {
        for (name in safeQueryParamNames(uri)) {
            val value = runCatching { uri.getQueryParameter(name) }.getOrNull() ?: continue
            val candidate = value.substringBefore('?').substringAfterLast('/').substringAfterLast('.', "").lowercase()
            if (candidate.isNotEmpty() && knownMediaExtension(candidate)) return candidate
        }
        return null
    }

    private fun segmentTemplateKey(uri: Uri): String {
        val normalizedPath = (uri.path ?: "").replace(Regex("\\d+"), "#")
        val queryKeys = safeQueryParamNames(uri).sorted().joinToString(",")
        return "${uri.scheme}://${uri.host}$normalizedPath?$queryKeys"
    }

    fun onRequestObserved(tabId: String, pageUrl: String?, request: ClintRequest) {
        if (!request.method.equals("GET", ignoreCase = true)) return
        val uri = request.url
        val scheme = uri.scheme?.lowercase() ?: return
        if (scheme != "http" && scheme != "https") return

        val urlString = uri.toString()
        var ext = extensionOf(uri)
        if (ext in SEGMENT_EXTENSIONS) return
        val responseType = request.responseContentType?.substringBefore(';')?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        if (responseType != null && responseType in TRANSPORT_STREAM_CONTENT_TYPES) return
        if (!MediaCaptureStore.markSeen(tabId, "request:$urlString")) return
        if (MediaCaptureStore.contains(tabId, urlString)) return

        if (!knownMediaExtension(ext)) {
            extensionFromQuery(uri)?.let { ext = it }
        }
        val headers = com.jhaiian.clint.profiles.WebProfiles.withMarker(request.requestHeaders, com.jhaiian.clint.profiles.WebProfiles.profileOfTab(tabId))
        val dest = headers.entries
            .firstOrNull { it.key.equals("Sec-Fetch-Dest", ignoreCase = true) }
            ?.value?.lowercase() ?: ""
        val accept = headers.entries
            .firstOrNull { it.key.equals("Accept", ignoreCase = true) }
            ?.value?.lowercase() ?: ""
        val looksLikeMediaAccept = accept.contains("video/") || accept.contains("audio/")
        val isMediaDest = dest in MEDIA_DESTS
        val looksLikeNonMediaAccept = accept.contains("application/json") || accept.contains("text/html")
        val safePageUrl = pageUrl ?: ""
        val now = System.currentTimeMillis()

        fun probeUnlessSpam(kind: MediaKind?, format: String?) {
            if (MediaCaptureStore.registerAndCheckSpam(tabId, segmentTemplateKey(uri), now)) return
            probeAndRecord(tabId, safePageUrl, urlString, headers, kind, format, responseType = responseType)
        }

        when {
            ext == "m3u8" -> fetchAndParseManifest(tabId, safePageUrl, urlString, ManifestType.HLS, headers)
            ext == "mpd" -> fetchAndParseManifest(tabId, safePageUrl, urlString, ManifestType.DASH, headers)
            responseType != null && responseType.contains("mpegurl") ->
                fetchAndParseManifest(tabId, safePageUrl, urlString, ManifestType.HLS, headers)
            responseType == "application/dash+xml" ->
                fetchAndParseManifest(tabId, safePageUrl, urlString, ManifestType.DASH, headers)
            ext in VIDEO_EXTENSIONS -> probeUnlessSpam(MediaKind.VIDEO, ext.uppercase())
            ext in AUDIO_EXTENSIONS -> probeUnlessSpam(MediaKind.AUDIO, ext.uppercase())
            ext in SUBTITLE_EXTENSIONS -> {
                MediaCaptureStore.add(
                    tabId,
                    DetectedMedia(
                        id = UUID.randomUUID().toString(),
                        url = urlString,
                        kind = MediaKind.SUBTITLE,
                        format = ext.uppercase(),
                        pageUrl = safePageUrl,
                        detectedAtMillis = now,
                        requestHeaders = sanitizeHeaders(headers)
                    )
                )
            }
            kindFromContentType(responseType) != null -> probeUnlessSpam(null, null)
            looksLikeMediaAccept || (dest !in NON_MEDIA_DESTS && !looksLikeNonMediaAccept) ->
                probeUnlessSpam(null, null)
            isMediaDest || looksLikeMediaAccept -> probeUnlessSpam(null, null)
            else -> Unit
        }
    }

    fun onPageMediaFound(
        tabId: String,
        pageUrl: String?,
        frameUrl: String?,
        userAgent: String?,
        candidates: List<PageMediaCandidate>
    ) {
        val safePageUrl = pageUrl ?: frameUrl ?: ""
        val referer = frameUrl?.takeIf { it.startsWith("http", ignoreCase = true) } ?: safePageUrl
        val baseHeaders = LinkedHashMap<String, String>()
        if (!userAgent.isNullOrBlank()) baseHeaders["User-Agent"] = userAgent
        if (referer.isNotEmpty()) baseHeaders["Referer"] = referer
        val headers = com.jhaiian.clint.profiles.WebProfiles.withMarker(baseHeaders, com.jhaiian.clint.profiles.WebProfiles.profileOfTab(tabId))
        for (candidate in candidates) {
            val uri = runCatching { Uri.parse(candidate.url) }.getOrNull() ?: continue
            val scheme = uri.scheme?.lowercase()
            if (scheme != "http" && scheme != "https") continue
            val urlString = uri.toString()
            if (!MediaCaptureStore.markSeen(tabId, "page:$urlString")) continue
            if (MediaCaptureStore.contains(tabId, urlString)) continue
            var ext = extensionOf(uri)
            if (ext in SEGMENT_EXTENSIONS) continue
            if (!knownMediaExtension(ext)) ext = extensionFromQuery(uri) ?: ""
            val label = candidate.label?.trim()?.take(MAX_LABEL_CHARS)?.takeIf { it.isNotEmpty() }
            when {
                ext == "m3u8" -> fetchAndParseManifest(tabId, safePageUrl, urlString, ManifestType.HLS, headers)
                ext == "mpd" -> fetchAndParseManifest(tabId, safePageUrl, urlString, ManifestType.DASH, headers)
                ext in SUBTITLE_EXTENSIONS -> MediaCaptureStore.add(
                    tabId,
                    DetectedMedia(
                        id = UUID.randomUUID().toString(),
                        url = urlString,
                        kind = MediaKind.SUBTITLE,
                        format = ext.uppercase(),
                        label = label,
                        language = candidate.language,
                        pageUrl = safePageUrl,
                        detectedAtMillis = System.currentTimeMillis(),
                        requestHeaders = sanitizeHeaders(headers)
                    )
                )
                ext in VIDEO_EXTENSIONS -> probeAndRecord(tabId, safePageUrl, urlString, headers, MediaKind.VIDEO, ext.uppercase(), label)
                ext in AUDIO_EXTENSIONS -> probeAndRecord(tabId, safePageUrl, urlString, headers, MediaKind.AUDIO, ext.uppercase(), label)
                else -> probeAndRecord(tabId, safePageUrl, urlString, headers, null, null, label, candidate.language)
            }
        }
    }

    private fun applyHeaders(builder: Request.Builder, original: Map<String, String>?, url: String, refererUrl: String? = null) {
        original?.forEach { (key, value) ->
            if (!key.equals("Range", ignoreCase = true) && !key.equals("Accept-Encoding", ignoreCase = true) && !key.equals(com.jhaiian.clint.profiles.WebProfiles.PROFILE_HEADER, ignoreCase = true)) {
                runCatching { builder.header(key, value) }
            }
        }
        val hasCookie = original?.keys?.any { it.equals("Cookie", ignoreCase = true) } == true
        if (!hasCookie) {
            val cookie = com.jhaiian.clint.profiles.WebProfiles.cookieForHeaders(original, url)
            if (cookie.isNotBlank()) runCatching { builder.header("Cookie", cookie) }
        }
        val hasReferer = original?.keys?.any { it.equals("Referer", ignoreCase = true) } == true
        if (!hasReferer && !refererUrl.isNullOrBlank()) {
            runCatching { builder.header("Referer", refererUrl) }
        }
    }

    private data class HeadResult(val contentType: String?, val contentLength: Long?, val contentDisposition: String?)

    private fun performHead(url: String, headers: Map<String, String>?, refererUrl: String?): HeadResult? = try {
        val builder = Request.Builder().url(url).head()
        applyHeaders(builder, headers, url, refererUrl)
        probeHttpClient.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) null
            else HeadResult(
                resp.header("Content-Type"),
                resp.header("Content-Length")?.toLongOrNull(),
                resp.header("Content-Disposition")
            )
        }
    } catch (_: Exception) {
        null
    }

    private fun performRangedGet(url: String, headers: Map<String, String>?, refererUrl: String?): HeadResult? = try {
        val builder = Request.Builder().url(url).header("Range", "bytes=0-0")
        applyHeaders(builder, headers, url, refererUrl)
        probeHttpClient.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) null
            else {
                val size = if (resp.code == 206) {
                    resp.header("Content-Range")?.substringAfterLast('/')?.toLongOrNull()
                } else {
                    resp.header("Content-Length")?.toLongOrNull()
                }
                HeadResult(resp.header("Content-Type"), size, resp.header("Content-Disposition"))
            }
        }
    } catch (_: Exception) {
        null
    }

    private fun performProbe(url: String, headers: Map<String, String>?, refererUrl: String?): HeadResult? {
        val head = performHead(url, headers, refererUrl)
        if (head != null && (head.contentType != null || head.contentLength != null)) return head
        return performRangedGet(url, headers, refererUrl) ?: head
    }

    private fun kindFromContentType(contentType: String?): MediaKind? {
        val ct = contentType?.substringBefore(';')?.trim()?.lowercase() ?: return null
        return when {
            ct.startsWith("video/") -> MediaKind.VIDEO
            ct.startsWith("audio/") -> MediaKind.AUDIO
            ct.contains("vtt") || ct == "application/x-subrip" -> MediaKind.SUBTITLE
            else -> null
        }
    }

    private fun formatFromContentType(contentType: String?): String? {
        val kind = kindFromContentType(contentType) ?: return null
        val ct = contentType?.substringBefore(';')?.trim()?.lowercase() ?: return null
        val subtype = ct.substringAfter('/', "").removePrefix("x-").takeIf { it.isNotEmpty() } ?: return null
        return when {
            subtype == "mpeg" || subtype == "mp3" -> if (kind == MediaKind.AUDIO) "MP3" else "MPEG"
            subtype == "vtt" -> "VTT"
            else -> CONTENT_TYPE_FORMATS[subtype] ?: subtype.uppercase()
        }
    }

    private fun extensionFromContentDisposition(value: String?): String? {
        if (value.isNullOrBlank()) return null
        val name = FILENAME_REGEX.find(value)?.groupValues?.get(1)?.trim()?.trim('"') ?: return null
        return name.substringAfterLast('.', "").lowercase().takeIf { it.isNotEmpty() }
    }

    private fun kindFromExtension(ext: String?): MediaKind? = when (ext) {
        null -> null
        in VIDEO_EXTENSIONS -> MediaKind.VIDEO
        in AUDIO_EXTENSIONS -> MediaKind.AUDIO
        in SUBTITLE_EXTENSIONS -> MediaKind.SUBTITLE
        else -> null
    }

    private fun sniffLeadingBytes(url: String, headers: Map<String, String>?, refererUrl: String?): ByteArray? = try {
        val builder = Request.Builder().url(url).header("Range", "bytes=0-${SNIFF_BYTES - 1}")
        applyHeaders(builder, headers, url, refererUrl)
        probeHttpClient.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) null
            else {
                val buffer = ByteArray(SNIFF_BYTES)
                var total = 0
                val stream = resp.body.byteStream()
                while (total < buffer.size) {
                    val read = stream.read(buffer, total, buffer.size - total)
                    if (read <= 0) break
                    total += read
                }
                if (total == 0) null else buffer.copyOf(total)
            }
        }
    } catch (_: Exception) {
        null
    }

    private fun asciiAt(bytes: ByteArray, offset: Int, length: Int): String? {
        if (offset < 0 || offset + length > bytes.size) return null
        return String(bytes, offset, length, Charsets.US_ASCII)
    }

    private fun textStart(bytes: ByteArray): Int {
        val hasBom = bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        var index = if (hasBom) 3 else 0
        while (index < bytes.size && bytes[index].toInt().toChar().isWhitespace()) index++
        return index
    }

    private fun isHlsPlaylist(bytes: ByteArray): Boolean =
        asciiAt(bytes, textStart(bytes), 7) == "#EXTM3U"

    private fun isDashManifest(bytes: ByteArray): Boolean {
        val start = textStart(bytes)
        if (start >= bytes.size || bytes[start] != '<'.code.toByte()) return false
        return String(bytes, start, bytes.size - start, Charsets.ISO_8859_1).contains("<MPD")
    }

    private fun kindAndFormatFromMagicBytes(bytes: ByteArray): Pair<MediaKind, String>? {
        if (bytes.size >= 4 &&
            bytes[0] == 0x1A.toByte() && bytes[1] == 0x45.toByte() &&
            bytes[2] == 0xDF.toByte() && bytes[3] == 0xA3.toByte()
        ) return MediaKind.VIDEO to "WEBM"
        if (asciiAt(bytes, 4, 4) == "ftyp") {
            val brand = asciiAt(bytes, 8, 4) ?: ""
            return if (brand.startsWith("M4A") || brand.startsWith("M4B")) MediaKind.AUDIO to "M4A" else MediaKind.VIDEO to "MP4"
        }
        if (asciiAt(bytes, 0, 3) == "ID3") return MediaKind.AUDIO to "MP3"
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && (bytes[1].toInt() and 0xE0) == 0xE0) return MediaKind.AUDIO to "MP3"
        if (asciiAt(bytes, 0, 4) == "OggS") return MediaKind.AUDIO to "OGG"
        if (asciiAt(bytes, 0, 4) == "fLaC") return MediaKind.AUDIO to "FLAC"
        if (asciiAt(bytes, 0, 3) == "FLV") return MediaKind.VIDEO to "FLV"
        if (asciiAt(bytes, 0, 4) == "RIFF") {
            val subtype = asciiAt(bytes, 8, 4) ?: ""
            return when {
                subtype == "WAVE" -> MediaKind.AUDIO to "WAV"
                subtype.startsWith("AVI") -> MediaKind.VIDEO to "AVI"
                else -> null
            }
        }
        return null
    }

    private fun probeAndRecord(
        tabId: String,
        pageUrl: String,
        url: String,
        headers: Map<String, String>?,
        extKind: MediaKind?,
        extFormat: String?,
        label: String? = null,
        language: String? = null,
        responseType: String? = null
    ) {
        val generation = MediaCaptureStore.beginGeneration(tabId)
        ClintDownloadManager.applicationScope.launch {
            val head = performProbe(url, headers, pageUrl)
            if (MediaCaptureStore.generation(tabId) != generation) return@launch
            val contentType = head?.contentType?.substringBefore(';')?.trim()?.lowercase()
            when {
                contentType != null && contentType.contains("mpegurl") ->
                    fetchAndParseManifest(tabId, pageUrl, url, ManifestType.HLS, headers, generation)
                contentType == "application/dash+xml" ->
                    fetchAndParseManifest(tabId, pageUrl, url, ManifestType.DASH, headers, generation)
                else -> {
                    if (extKind == null && contentType != null && contentType in TRANSPORT_STREAM_CONTENT_TYPES) return@launch
                    val cdExt = extensionFromContentDisposition(head?.contentDisposition)
                    var kind = extKind ?: kindFromContentType(contentType) ?: kindFromExtension(cdExt)
                    var format = extFormat ?: cdExt?.takeIf { kindFromExtension(it) != null }?.uppercase() ?: formatFromContentType(contentType)
                    var mimeType = head?.contentType
                    var trustedSize = true
                    if (kind == null) {
                        val leading = sniffLeadingBytes(url, headers, pageUrl)
                        if (leading != null && isHlsPlaylist(leading)) {
                            fetchAndParseManifest(tabId, pageUrl, url, ManifestType.HLS, headers, generation)
                            return@launch
                        }
                        if (leading != null && isDashManifest(leading)) {
                            fetchAndParseManifest(tabId, pageUrl, url, ManifestType.DASH, headers, generation)
                            return@launch
                        }
                        val magic = leading?.let(::kindAndFormatFromMagicBytes)
                        if (magic != null) {
                            kind = magic.first
                            format = format ?: magic.second
                        }
                    }
                    if (kind == null) {
                        kind = kindFromContentType(responseType)
                        if (kind != null) {
                            format = format ?: formatFromContentType(responseType)
                            mimeType = responseType
                            trustedSize = false
                        }
                    }
                    if (kind == null) return@launch
                    val size = head?.contentLength?.takeIf { trustedSize }
                    if (size != null) {
                        if (extKind == null && extFormat == null && size < MIN_EXTENSIONLESS_MEDIA_BYTES) return@launch
                        if (kind == MediaKind.VIDEO && size < MIN_VIDEO_BYTES) return@launch
                    }
                    val detected = DetectedMedia(
                        id = UUID.randomUUID().toString(),
                        url = url,
                        kind = kind,
                        format = format ?: "UNKNOWN",
                        mimeType = mimeType,
                        label = label,
                        language = language,
                        sizeBytes = size,
                        pageUrl = pageUrl,
                        detectedAtMillis = System.currentTimeMillis(),
                        requestHeaders = sanitizeHeaders(headers)
                    )
                    val ready = enrichFileMedia(detected, pageUrl)
                    if (MediaCaptureStore.generation(tabId) != generation) return@launch
                    MediaCaptureStore.add(tabId, ready)
                }
            }
        }
    }

    private fun requestManifestBody(
        manifestUrl: String,
        headers: Map<String, String>?,
        pageUrl: String,
        useRange: Boolean
    ): String? = try {
        val builder = Request.Builder().url(manifestUrl)
        if (useRange) builder.header("Range", "bytes=0-${MAX_MANIFEST_BYTES - 1}")
        applyHeaders(builder, headers, manifestUrl, pageUrl)
        manifestHttpClient.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body.byteStream().bufferedReader().use { reader ->
                val buffer = CharArray(8 * 1024)
                val sb = StringBuilder()
                var total = 0
                while (total < MAX_MANIFEST_CHARS) {
                    val read = reader.read(buffer)
                    if (read == -1) break
                    sb.append(buffer, 0, read)
                    total += read
                }
                sb.toString()
            }
        }
    } catch (_: Exception) {
        null
    }

    private fun fetchAndParseManifest(
        tabId: String,
        pageUrl: String,
        manifestUrl: String,
        type: ManifestType,
        headers: Map<String, String>?,
        generation: Int = MediaCaptureStore.beginGeneration(tabId)
    ) {
        ClintDownloadManager.applicationScope.launch {
            if (MediaCaptureStore.generation(tabId) != generation) return@launch
            val text = requestManifestBody(manifestUrl, headers, pageUrl, useRange = true)
                ?: requestManifestBody(manifestUrl, headers, pageUrl, useRange = false)
                ?: return@launch
            val entries = try {
                when (type) {
                    ManifestType.HLS -> MediaManifestParser.parseHls(text, manifestUrl)
                    ManifestType.DASH -> MediaManifestParser.parseDash(text, manifestUrl)
                }
            } catch (_: Exception) {
                emptyList()
            }
            val now = System.currentTimeMillis()
            val sanitized = sanitizeHeaders(headers)
            val prepared = entries.map {
                it.copy(pageUrl = pageUrl, detectedAtMillis = now, requestHeaders = sanitized)
            }
            val ready = if (type == ManifestType.HLS) enrichHlsEntries(prepared, pageUrl) else prepared
            if (MediaCaptureStore.generation(tabId) != generation) return@launch
            ready.forEach { MediaCaptureStore.add(tabId, it) }
        }
    }

    private suspend fun enrichFileMedia(media: DetectedMedia, pageUrl: String): DetectedMedia {
        if (!needsFileMetadata(media)) return media
        val job = ClintDownloadManager.applicationScope.async(Dispatchers.IO) {
            enrichmentPermits.withPermit {
                runCatching { readFileMetadata(media, pageUrl) }.getOrNull() ?: media
            }
        }
        return withTimeoutOrNull(ENRICHMENT_TIMEOUT_MILLIS) { job.await() } ?: media
    }

    private fun readFileMetadata(media: DetectedMedia, pageUrl: String): DetectedMedia {
        if (media.kind == MediaKind.SUBTITLE) {
            val language = media.language ?: MediaLanguageGuess.fromUrl(media.url)
            val info = SubtitleFileProbe.probe(media, pageUrl)
                ?: return media.copy(language = language)
            return media.copy(
                cueCount = info.cueCount,
                durationSeconds = media.durationSeconds ?: info.durationSeconds,
                language = language,
                isHlsPlaylist = info.isHlsPlaylist
            )
        }
        val info = MediaFileMetadataProbe.probe(media, pageUrl) ?: return media
        val duration = media.durationSeconds ?: info.durationSeconds
        val size = media.sizeBytes
        val bitrate = media.bandwidthBitsPerSec
            ?: info.bitrateBitsPerSec
            ?: if (media.kind == MediaKind.AUDIO && duration != null && duration > 0.0 && size != null && size > 0L) {
                ((size * 8) / duration).toLong()
            } else null
        return media.copy(
            width = info.width ?: media.width,
            height = info.height ?: media.height,
            durationSeconds = duration,
            bandwidthBitsPerSec = bitrate
        )
    }

    private fun needsFileMetadata(media: DetectedMedia): Boolean {
        if (media.groupUrl != null) return false
        return when (media.kind) {
            MediaKind.VIDEO ->
                media.width == null && media.height == null &&
                    media.format.uppercase() !in UNSUPPORTED_METADATA_FORMATS
            MediaKind.AUDIO ->
                media.durationSeconds == null &&
                    media.format.uppercase() !in UNSUPPORTED_METADATA_FORMATS
            MediaKind.SUBTITLE -> media.cueCount == null
        }
    }

    private suspend fun enrichHlsEntries(entries: List<DetectedMedia>, pageUrl: String): List<DetectedMedia> {
        val jobs = entries.map { entry ->
            ClintDownloadManager.applicationScope.async(Dispatchers.IO) {
                enrichmentPermits.withPermit { enrichHlsEntry(entry, pageUrl) }
            }
        }
        val deadlineNanos = System.nanoTime() + ENRICHMENT_TIMEOUT_MILLIS * 1_000_000L
        return entries.mapIndexed { index, entry ->
            val remainingMillis = ((deadlineNanos - System.nanoTime()) / 1_000_000L).coerceAtLeast(1L)
            withTimeoutOrNull(remainingMillis) { jobs[index].await() } ?: entry
        }
    }

    private fun enrichHlsEntry(entry: DetectedMedia, pageUrl: String): DetectedMedia {
        val wantsEstimate = needsSegmentEstimate(entry)
        val wantsResolution = needsResolutionProbe(entry)
        if (!wantsEstimate && !wantsResolution) return entry
        val kind = if (entry.kind == MediaKind.AUDIO) TrackKind.AUDIO else TrackKind.VIDEO
        val track = runCatching {
            HlsPlaylistFetcher.fetch(entry.url, pageUrl, "", "", kind, entry.requestHeaders)
        }.getOrNull() ?: return entry
        var result = entry
        if (entry.durationSeconds == null) {
            track.durationSeconds?.takeIf { it > 0.0 }?.let { result = result.copy(durationSeconds = it) }
        }
        if (wantsEstimate) {
            runCatching { HlsSegmentEstimator.estimate(entry, track, pageUrl) }.getOrNull()?.let { estimate ->
                result = result.copy(estimate = estimate)
            }
        }
        if (wantsResolution) {
            runCatching { HlsResolutionProbe.probe(track, pageUrl, entry.requestHeaders) }.getOrNull()?.let { size ->
                result = result.copy(width = size.first, height = size.second)
            }
        }
        return result
    }

    private fun needsSegmentEstimate(entry: DetectedMedia): Boolean =
        entry.kind != MediaKind.SUBTITLE &&
            !entry.isLive &&
            entry.format.equals("HLS", ignoreCase = true) &&
            entry.groupUrl != null &&
            entry.estimate == null

    private fun needsResolutionProbe(entry: DetectedMedia): Boolean =
        entry.kind == MediaKind.VIDEO &&
            entry.format.equals("HLS", ignoreCase = true) &&
            entry.width == null &&
            entry.height == null &&
            (entry.url != entry.groupUrl || entry.durationSeconds != null)
}
