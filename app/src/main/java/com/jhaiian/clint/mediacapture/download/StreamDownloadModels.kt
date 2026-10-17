package com.jhaiian.clint.mediacapture.download

import okhttp3.Request

enum class TrackKind { VIDEO, AUDIO }

enum class StreamContainerFormat { HLS, DASH }

const val STREAM_FORMAT_HLS_SUBTITLE = "HLS_SUBTITLE"

data class HlsKeyInfo(
    val keyUri: String,
    val ivHex: String?,
    val method: String
)

data class SegmentSpec(
    val url: String,
    val byteRangeOffset: Long? = null,
    val byteRangeLength: Long? = null,
    val sequenceNumber: Long = 0L,
    val key: HlsKeyInfo? = null
)

data class ResolvedTrack(
    val kind: TrackKind,
    val initSegment: SegmentSpec?,
    val segments: List<SegmentSpec>,
    val durationSeconds: Double? = null,
    val containerHintExtension: String = "ts",
    val isLive: Boolean = false,
    val targetDurationSeconds: Double? = null
)

data class StreamDownloadRequest(
    val format: StreamContainerFormat,
    val videoUrl: String?,
    val audioUrl: String?,
    val subtitleUrl: String?,
    val pageUrl: String,
    val referer: String,
    val cookies: String,
    val userAgent: String,
    val filename: String,
    val headers: Map<String, String> = emptyMap(),
    val estimatedTotalBytes: Long = 0L,
    val speedLimitBytesPerSec: Long = 0L,
    val concurrentSegments: Int = 12,
    val noAudio: Boolean = false,
    val isLive: Boolean = false,
    val convertTsToMp4: Boolean = false,
    val subtitleOnly: Boolean = false,
    val audioLanguage: String? = null,
    val audioLabel: String? = null
)

object StreamBackoff {
    suspend fun wait(attempt: Int, isCancelled: () -> Boolean) {
        if (isCancelled()) return
        val base = (250L shl attempt.coerceIn(0, 5)).coerceAtMost(8000L)
        val jitter = (0..(base / 4).coerceAtLeast(1)).random()
        kotlinx.coroutines.delay(base + jitter)
    }
}

object StreamRequestHeaders {
    fun apply(
        builder: Request.Builder,
        url: String,
        referer: String,
        cookies: String,
        userAgent: String,
        extraHeaders: Map<String, String>
    ) {
        buildMap(url, referer, cookies, userAgent, extraHeaders).forEach { (key, value) ->
            runCatching { builder.header(key, value) }
        }
    }

    fun buildMap(
        url: String,
        referer: String,
        cookies: String,
        userAgent: String,
        extraHeaders: Map<String, String>
    ): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        if (userAgent.isNotBlank()) result["User-Agent"] = userAgent
        if (referer.isNotBlank()) result["Referer"] = referer
        val cookieHeader = cookies.ifBlank {
            com.jhaiian.clint.profiles.WebProfiles.cookieForHeaders(extraHeaders, url)
        }
        if (cookieHeader.isNotBlank()) result["Cookie"] = cookieHeader
        extraHeaders.forEach { (key, value) ->
            if (!key.equals(com.jhaiian.clint.profiles.WebProfiles.PROFILE_HEADER, ignoreCase = true)) result[key] = value
        }
        return result
    }
}
