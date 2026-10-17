package com.jhaiian.clint.mediacapture

import java.util.Locale

data class MediaCaptureOption(val video: DetectedMedia, val audio: DetectedMedia?, val audioOrdinal: Int = 0) {
    val key: String get() = "${video.id}|${audio?.id.orEmpty()}"
}

fun compatibleAudioTracks(video: DetectedMedia, all: List<DetectedMedia>): List<DetectedMedia> {
    if (video.kind != MediaKind.VIDEO || video.hasAudio != false) return emptyList()
    val group = video.groupUrl ?: return emptyList()
    val sameGroup = all.filter { it.kind == MediaKind.AUDIO && it.groupUrl == group }
    if (sameGroup.isEmpty()) return emptyList()
    val ref = video.audioGroupRef
    val scoped = if (ref != null) sameGroup.filter { it.mediaGroupId == ref } else sameGroup
    return scoped.ifEmpty { sameGroup }.sortedByDescending { it.isDefaultTrack }
}

fun buildVideoOptions(videos: List<DetectedMedia>, all: List<DetectedMedia>): List<MediaCaptureOption> =
    videos.flatMap { video ->
        val audioTracks = compatibleAudioTracks(video, all)
        if (audioTracks.isEmpty()) listOf(MediaCaptureOption(video, null))
        else audioTracks.mapIndexed { index, audio -> MediaCaptureOption(video, audio, index + 1) }
    }

fun mediaEstimatedBytes(media: DetectedMedia): Long? {
    media.sizeBytes?.takeIf { it > 0L }?.let { return it }
    media.estimate?.estimatedBytes?.takeIf { it > 0L }?.let { return it }
    val duration = media.durationSeconds
    val bandwidth = media.bandwidthBitsPerSec
    if (duration != null && duration > 0.0 && bandwidth != null && bandwidth > 0L && !media.isLive) {
        return ((duration * bandwidth) / 8.0).toLong()
    }
    return null
}

fun optionEstimatedBytes(option: MediaCaptureOption): Long? {
    val video = mediaEstimatedBytes(option.video) ?: return null
    val audio = option.audio?.let { mediaEstimatedBytes(it) } ?: 0L
    return video + audio
}

fun audioTrackName(audio: DetectedMedia): String? {
    val label = audio.label?.takeIf { it.isNotBlank() }
    val tag = audio.language?.takeIf { it.isNotBlank() }
    val locale = tag?.let { Locale.forLanguageTag(it.replace('_', '-')) }
    val languageName = locale?.getDisplayName(Locale.getDefault())
        ?.takeIf { name -> name.isNotBlank() && !name.equals(tag, ignoreCase = true) }
        ?.replaceFirstChar { c -> c.titlecase(Locale.getDefault()) }
    val englishName = locale?.getDisplayName(Locale.ENGLISH)?.takeIf { it.isNotBlank() }
    return when {
        languageName == null && label == null -> tag?.uppercase(Locale.ROOT)
        languageName == null -> label
        label == null -> languageName
        label.contains(languageName, ignoreCase = true) ||
            (englishName != null && label.contains(englishName, ignoreCase = true)) -> label
        else -> "$languageName ($label)"
    }
}
