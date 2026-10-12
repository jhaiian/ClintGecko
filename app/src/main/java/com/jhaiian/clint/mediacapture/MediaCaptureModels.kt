package com.jhaiian.clint.mediacapture

const val MEDIA_CAPTURE_ENABLED_PREF = "media_capture_enabled"
const val MEDIA_CAPTURE_CONVERT_TS_TO_MP4_PREF = "media_capture_convert_ts_to_mp4"
const val MEDIA_CAPTURE_CONVERT_TS_TO_MP4_DEFAULT = true

enum class MediaKind { VIDEO, AUDIO, SUBTITLE }

enum class ManifestType { HLS, DASH }

data class HlsSegmentEstimate(
    val segmentCount: Int,
    val estimatedBytes: Long?,
    val containerExtension: String?
)

data class DetectedMedia(
    val id: String,
    val url: String,
    val kind: MediaKind,
    val format: String,
    val mimeType: String? = null,
    val label: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val bandwidthBitsPerSec: Long? = null,
    val durationSeconds: Double? = null,
    val sizeBytes: Long? = null,
    val isLive: Boolean = false,
    val hasAudio: Boolean? = null,
    val groupUrl: String? = null,
    val pageUrl: String = "",
    val detectedAtMillis: Long = 0L,
    val requestHeaders: Map<String, String> = emptyMap(),
    val mediaGroupId: String? = null,
    val audioGroupRef: String? = null,
    val language: String? = null,
    val isDefaultTrack: Boolean = false,
    val representationId: String? = null,
    val estimate: HlsSegmentEstimate? = null,
    val cueCount: Int? = null,
    val isHlsPlaylist: Boolean = false
)
