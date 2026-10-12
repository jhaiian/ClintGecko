package com.jhaiian.clint.downloads

import java.io.File

enum class DownloadStatus {
    QUEUED, CONNECTING, ALLOCATING, DOWNLOADING, RETRYING, DECRYPTING, MUXING, CONVERTING, COPYING_TEMP, DELETING_TEMP, PAUSED, FAILED, COMPLETE;

    companion object {

        val ACTIVELY_WORKING: Set<DownloadStatus> =
            setOf(CONNECTING, DOWNLOADING, ALLOCATING, DECRYPTING, MUXING, CONVERTING, COPYING_TEMP, DELETING_TEMP, RETRYING)

        val NOT_FINISHED: Set<DownloadStatus> =
            setOf(QUEUED, CONNECTING, ALLOCATING, DOWNLOADING, RETRYING, DECRYPTING, MUXING, CONVERTING, COPYING_TEMP, DELETING_TEMP, PAUSED)

        val RUNNING_OR_QUEUED: Set<DownloadStatus> =
            setOf(QUEUED, CONNECTING, ALLOCATING, DOWNLOADING, DECRYPTING, MUXING, CONVERTING, COPYING_TEMP, DELETING_TEMP)
    }
}

data class DownloadItem(
    val id: Int,
    val url: String,
    val filename: String,
    val userAgent: String,
    val referer: String = "",
    val cookies: String = "",
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = -1L,
    val status: DownloadStatus = DownloadStatus.DOWNLOADING,
    val file: File? = null,
    val errorMessage: String? = null,
    val startedAt: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val resumable: Boolean = false,
    val copyProgress: Int = 0,
    val contentUri: String? = null,
    val retryAttempt: Int = 0,
    val retryDelaySec: Int = 0,
    val allocationProgress: Int = 0,
    val waitingForUnmetered: Boolean = false,
    val waitingForNetwork: Boolean = false,
    val waitingForSchedule: Boolean = false,

    val scheduledStartAtMillis: Long = 0L,

    val waitingForCustomSchedule: Boolean = false,
    val activeElapsedMs: Long = 0L,
    @Transient val activeStartedAt: Long = 0L,
    @Transient val parallelRateLimited: Boolean = false,
    val completedAt: Long = 0L,
    val retryEnabled: Boolean = true,
    val lastErrorWasServerError: Boolean = false,
    val unmeteredOnly: Boolean = false,
    val splitParts: Int = 32,
    val multithreadingParts: Int = 4,

    val speedLimitBytesPerSec: Long = 0L,
    val locationMode: String = "default",
    val customLocationUri: String? = null,
    val categorizeEnabled: Boolean = false,

    val completedPartsMask: Long = 0L,

    val partOffsets: String = "",

    val isStream: Boolean = false,
    val streamVideoUrl: String = "",
    val streamAudioUrl: String? = null,
    val streamSubtitleUrl: String? = null,
    val streamFormat: String = "",
    val streamPageUrl: String = "",
    val streamVideoWidth: Int? = null,
    val streamVideoHeight: Int? = null,
    val streamVideoBandwidth: Long? = null,
    val streamAudioBandwidth: Long? = null,
    val streamPrimaryIsAudio: Boolean = false,
    val streamNoAudio: Boolean = false,
    val streamHeaders: Map<String, String> = emptyMap(),
    val streamConcurrentSegments: Int = 6,
    val streamIsLive: Boolean = false,
    val streamConvertTsToMp4: Boolean = false,
    val streamVideoRepresentationId: String? = null,
    val streamAudioRepresentationId: String? = null,
    val segmentsCompleted: Int = 0,
    val segmentsTotal: Int = 0,
    val muxProgress: Int = 0
) {
    val progressPercent: Int
        get() = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else -1

    fun averageSpeedBytesPerSec(): Long {
        val activeMs = activeElapsedMs + (if (activeStartedAt > 0L) System.currentTimeMillis() - activeStartedAt else 0L)
        return if (activeMs > 0L && bytesDownloaded > 0L) bytesDownloaded * 1000L / activeMs else 0L
    }
}
