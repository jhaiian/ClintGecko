package com.jhaiian.clint.mediacapture.download

import android.content.Context
import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.downloads.DownloadCategories
import com.jhaiian.clint.downloads.DownloadFileHelper
import com.jhaiian.clint.downloads.DownloadItem
import com.jhaiian.clint.downloads.DownloadNotificationHelper
import com.jhaiian.clint.downloads.DownloadStatus
import com.jhaiian.clint.downloads.DownloadWorker
import com.jhaiian.clint.R
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

object StreamDownloadJob {

    private fun DownloadItem.withClockStarted(): DownloadItem =
        if (activeStartedAt > 0L) this else copy(activeStartedAt = System.currentTimeMillis())

    private fun DownloadItem.withClockStopped(): DownloadItem =
        if (activeStartedAt > 0L) {
            copy(activeElapsedMs = activeElapsedMs + (System.currentTimeMillis() - activeStartedAt), activeStartedAt = 0L)
        } else this

    suspend fun run(context: Context, initialItem: DownloadItem) {
        if (initialItem.streamFormat == STREAM_FORMAT_HLS_SUBTITLE) {
            runSubtitle(context, initialItem)
            return
        }
        var current = initialItem
        val workDir = File(context.filesDir, "stream_downloads/${current.id}")

        fun isCancelled(): Boolean =
            current.id in ClintDownloadManager.removedIds || current.id in ClintDownloadManager.pauseRequested

        fun publishProgress(transform: (DownloadItem) -> DownloadItem) {
            current = transform(current)
            ClintDownloadManager.publish(current)
        }

        try {
            val format = if (current.streamFormat == "DASH") StreamContainerFormat.DASH else StreamContainerFormat.HLS
            val primaryKind = if (current.streamPrimaryIsAudio) TrackKind.AUDIO else TrackKind.VIDEO

            val isLiveCapture = current.streamIsLive
            val videoWorkDir = File(workDir, "video")
            val audioWorkDir = File(workDir, "audio")

            val videoTrack: ResolvedTrack
            val audioTrack: ResolvedTrack?
            val videoSegments: List<DownloadedSegment>
            val audioSegments: List<DownloadedSegment>

            if (isLiveCapture) {
                publishProgress {
                    it.withClockStarted().copy(
                        status = DownloadStatus.DOWNLOADING, segmentsCompleted = 0, segmentsTotal = 0, bytesDownloaded = 0L,
                        activeElapsedMs = 0L
                    )
                }
                DownloadNotificationHelper.showProgressNotification(context, current)

                var liveCompleted = 0
                var liveBytes = 0L
                var lastNotifyAt = 0L
                var lastSpeedBytes = 0L
                var lastSpeedTime = System.currentTimeMillis()

                fun onLiveSegment(segmentBytes: Long) {
                    liveCompleted++
                    liveBytes += segmentBytes
                    val now = System.currentTimeMillis()
                    val elapsed = now - lastSpeedTime
                    val speed = if (elapsed >= 500) {
                        val delta = liveBytes - lastSpeedBytes
                        lastSpeedBytes = liveBytes
                        lastSpeedTime = now
                        if (elapsed > 0) delta * 1000L / elapsed else 0L
                    } else current.speedBytesPerSec
                    publishProgress { it.copy(segmentsCompleted = liveCompleted, bytesDownloaded = liveBytes, speedBytesPerSec = speed) }
                    if (now - lastNotifyAt > 800) {
                        lastNotifyAt = now
                        DownloadNotificationHelper.showProgressNotification(context, current)
                    }
                }

                videoWorkDir.deleteRecursively()
                audioWorkDir.deleteRecursively()

                val liveAudioUrl = if (!current.streamNoAudio) current.streamAudioUrl else null

                val results = coroutineScope {
                    val videoDeferred = async {
                        recordLiveTrack(
                            format, current.streamVideoUrl, current.streamPageUrl, current.userAgent, current.streamHeaders,
                            videoWorkDir, primaryKind, current.streamVideoWidth, current.streamVideoHeight,
                            current.streamVideoBandwidth, current.streamVideoRepresentationId,
                            ::isCancelled, ClintDownloadManager.activeSpeedLimiters[current.id],
                            current.streamConcurrentSegments, ::onLiveSegment
                        )
                    }
                    val audioDeferred = if (liveAudioUrl != null) {
                        async {
                            recordLiveTrack(
                                format, liveAudioUrl, current.streamPageUrl, current.userAgent, current.streamHeaders,
                                audioWorkDir, TrackKind.AUDIO, null, null,
                                current.streamAudioBandwidth, current.streamAudioRepresentationId,
                                ::isCancelled, ClintDownloadManager.activeSpeedLimiters[current.id],
                                current.streamConcurrentSegments, ::onLiveSegment
                            )
                        }
                    } else null
                    videoDeferred.await() to audioDeferred?.await()
                }

                videoTrack = results.first.second
                audioTrack = results.second?.second
                videoSegments = results.first.first
                audioSegments = results.second?.first ?: emptyList()
            } else {
                val resolvedVideoTrack = resolveTrack(
                    format, current.streamVideoUrl, current.streamPageUrl, current.userAgent, primaryKind,
                    current.streamVideoWidth, current.streamVideoHeight, current.streamVideoBandwidth, current.streamHeaders,
                    current.streamVideoRepresentationId
                ) ?: run {
                    DownloadWorker.fail(context, current.withClockStopped(), context.getString(R.string.download_error_playlist_unavailable))
                    return
                }
                videoTrack = resolvedVideoTrack

                val audioTrackResolved = if (current.streamNoAudio) null else current.streamAudioUrl?.let { url ->
                    resolveTrack(
                        format, url, current.streamPageUrl, current.userAgent, TrackKind.AUDIO, null, null,
                        current.streamAudioBandwidth, current.streamHeaders, current.streamAudioRepresentationId
                    )
                }
                audioTrack = audioTrackResolved

                val videoCount = videoTrack.segments.size + if (videoTrack.initSegment != null) 1 else 0
                val audioCount = audioTrack?.let { it.segments.size + if (it.initSegment != null) 1 else 0 } ?: 0
                val totalSegments = videoCount + audioCount

                val existingVideoSegments = loadExistingSegments(videoWorkDir, videoTrack)
                val existingAudioSegments = audioTrack?.let { loadExistingSegments(audioWorkDir, it) } ?: emptyMap()
                val alreadyCompleted = existingVideoSegments.size + existingAudioSegments.size
                val bytesAtStart = existingVideoSegments.values.sumOf { it.file.length() } +
                    existingAudioSegments.values.sumOf { it.file.length() }

                publishProgress {
                    it.withClockStarted().copy(
                        status = DownloadStatus.DOWNLOADING, segmentsCompleted = alreadyCompleted, segmentsTotal = totalSegments,
                        bytesDownloaded = bytesAtStart,
                        totalBytes = if (alreadyCompleted > 0) (bytesAtStart.toDouble() / alreadyCompleted * totalSegments).toLong() else it.totalBytes
                    )
                }
                DownloadNotificationHelper.showProgressNotification(context, current)

                var overallCompleted = alreadyCompleted
                var bytesTotal = bytesAtStart
                var lastNotifyAt = 0L
                var lastSpeedBytes = bytesAtStart
                var lastSpeedTime = System.currentTimeMillis()

                fun onSegmentComplete(segmentBytes: Long) {
                    overallCompleted++
                    bytesTotal += segmentBytes
                    val now = System.currentTimeMillis()
                    val elapsed = now - lastSpeedTime
                    val speed = if (elapsed >= 500) {
                        val delta = bytesTotal - lastSpeedBytes
                        lastSpeedBytes = bytesTotal
                        lastSpeedTime = now
                        if (elapsed > 0) delta * 1000L / elapsed else 0L
                    } else current.speedBytesPerSec
                    val projectedTotal = if (overallCompleted > 0) (bytesTotal.toDouble() / overallCompleted * totalSegments).toLong() else bytesTotal
                    publishProgress {
                        it.copy(
                            segmentsCompleted = overallCompleted, bytesDownloaded = bytesTotal, speedBytesPerSec = speed,
                            totalBytes = projectedTotal
                        )
                    }
                    if (now - lastNotifyAt > 800) {
                        lastNotifyAt = now
                        DownloadNotificationHelper.showProgressNotification(context, current)
                    }
                }

                videoSegments = downloadTrackResilient(
                    videoTrack, videoWorkDir, current.streamPageUrl, current.userAgent, current.streamHeaders, ::isCancelled,
                    {
                        resolveTrack(
                            format, current.streamVideoUrl, current.streamPageUrl, current.userAgent, primaryKind,
                            current.streamVideoWidth, current.streamVideoHeight, current.streamVideoBandwidth, current.streamHeaders,
                            current.streamVideoRepresentationId
                        )
                    },
                    initialDownloaded = existingVideoSegments,
                    speedLimiter = ClintDownloadManager.activeSpeedLimiters[current.id],
                    concurrentSegments = current.streamConcurrentSegments
                ) { _, _, segBytes -> onSegmentComplete(segBytes) }

                audioSegments = audioTrack?.let { initialAudioTrack ->
                    downloadTrackResilient(
                        initialAudioTrack, audioWorkDir, current.streamPageUrl, current.userAgent, current.streamHeaders, ::isCancelled,
                        {
                            current.streamAudioUrl?.let { url ->
                                resolveTrack(
                                    format, url, current.streamPageUrl, current.userAgent, TrackKind.AUDIO, null, null,
                                    current.streamAudioBandwidth, current.streamHeaders, current.streamAudioRepresentationId
                                )
                            }
                        },
                        initialDownloaded = existingAudioSegments,
                        speedLimiter = ClintDownloadManager.activeSpeedLimiters[current.id],
                        concurrentSegments = current.streamConcurrentSegments
                    ) { _, _, segBytes -> onSegmentComplete(segBytes) }
                } ?: emptyList()
            }

            publishProgress { it.withClockStopped() }

            val anyEncrypted = (videoSegments + audioSegments).any { it.key != null }
            if (anyEncrypted) {
                val encryptedTotal = (videoSegments + audioSegments).count { it.key != null }
                publishProgress { it.copy(status = DownloadStatus.DECRYPTING, segmentsCompleted = 0, segmentsTotal = encryptedTotal) }
                DownloadNotificationHelper.showProgressNotification(context, current)

                var decryptCompleted = 0
                fun onDecryptProgress() {
                    decryptCompleted++
                    publishProgress { it.copy(segmentsCompleted = decryptCompleted) }
                }
                StreamTrackDownloader.decryptSegments(
                    videoSegments, current.streamPageUrl, "", current.userAgent, ::isCancelled, current.streamHeaders
                ) { _, _ -> onDecryptProgress() }
                StreamTrackDownloader.decryptSegments(
                    audioSegments, current.streamPageUrl, "", current.userAgent, ::isCancelled, current.streamHeaders
                ) { _, _ -> onDecryptProgress() }
            }

            publishProgress { it.copy(status = DownloadStatus.COPYING_TEMP, copyProgress = 0) }
            DownloadNotificationHelper.showCopyingTempNotification(context, current)

            var lastCopyNotifyAt = 0L
            fun onCombineProgress(pct: Int) {
                publishProgress { it.copy(copyProgress = pct) }
                val now = System.currentTimeMillis()
                if (now - lastCopyNotifyAt > 800) {
                    lastCopyNotifyAt = now
                    DownloadNotificationHelper.showCopyingTempNotification(context, current)
                }
            }

            val videoTrackFile = File(workDir, "video_track.${videoTrack.containerHintExtension}")
            StreamTrackDownloader.concatenate(videoSegments, videoTrackFile, ::onCombineProgress)
            val audioTrackFile = audioTrack?.let {
                val f = File(workDir, "audio_track.${it.containerHintExtension}")
                StreamTrackDownloader.concatenate(audioSegments, f, ::onCombineProgress)
                f
            }
            publishProgress { it.copy(copyProgress = 100) }

            StreamTrackDownloader.cleanup(videoSegments)
            StreamTrackDownloader.cleanup(audioSegments)

            val needsMux = audioTrackFile != null
            val userExtension = current.filename.substringAfterLast('.', "").trim().takeIf { it.isNotBlank() }
            val wantsTsConversion = current.streamConvertTsToMp4 && !needsMux && !current.streamPrimaryIsAudio &&
                videoTrack.containerHintExtension.equals("ts", ignoreCase = true)
            val wantsAacConversion = current.streamConvertTsToMp4 && !needsMux && current.streamPrimaryIsAudio &&
                videoTrack.containerHintExtension.equals("ts", ignoreCase = true)
            val audioFragmented = current.streamPrimaryIsAudio && !needsMux && videoTrack.containerHintExtension == "mp4"
            val outExtension = when {
                audioFragmented && userExtension.equals("aac", ignoreCase = true) -> "m4a"
                userExtension != null -> userExtension
                needsMux || wantsTsConversion -> "mp4"
                wantsAacConversion -> "aac"
                audioFragmented -> "m4a"
                videoTrack.containerHintExtension == "mp4" -> "mp4"
                else -> "ts"
            }
            val convertTsToMp4 = wantsTsConversion && !outExtension.equals("ts", ignoreCase = true)
            val convertTsToAac = wantsAacConversion && outExtension.equals("aac", ignoreCase = true)

            publishProgress {
                it.copy(status = if (convertTsToMp4 || convertTsToAac) DownloadStatus.CONVERTING else DownloadStatus.MUXING, muxProgress = 0)
            }
            DownloadNotificationHelper.showProgressNotification(context, current)

            var lastTransformNotifyAt = 0L
            fun onTransformProgress(pct: Int) {
                publishProgress { it.copy(muxProgress = pct.coerceIn(0, 99)) }
                val now = System.currentTimeMillis()
                if (now - lastTransformNotifyAt > 800) {
                    lastTransformNotifyAt = now
                    DownloadNotificationHelper.showProgressNotification(context, current)
                }
            }

            val inputFiles = listOfNotNull(videoTrackFile, audioTrackFile)

            val locationMode = current.locationMode
            val customLocationUri = current.customLocationUri
            if (!DownloadFileHelper.isCustomLocationAccessible(context, locationMode, customLocationUri)) {
                DownloadWorker.fail(context, current.withClockStopped(), context.getString(R.string.download_location_invalid_message))
                workDir.deleteRecursively()
                return
            }
            val directCustomDir = DownloadFileHelper.resolveDirectCustomDir(context, current)
            val safMode = DownloadFileHelper.isSafCustomMode(context, current) && directCustomDir == null
            val rawDestDir = directCustomDir
                ?: if (safMode) DownloadFileHelper.tempDownloadDir(context) else DownloadFileHelper.resolveDownloadDir()

            val guessedFinalName = "${baseName(current.filename)}.$outExtension"
            val destDir = if (safMode) rawDestDir else DownloadCategories.resolveDir(current.categorizeEnabled, rawDestDir, guessedFinalName)
            destDir.mkdirs()
            val finalFilename = DownloadFileHelper.uniqueFile(destDir, guessedFinalName).name
            var finalFile = File(destDir, finalFilename)

            if (needsMux) {
                val result = MediaRemuxer.remux(
                    context,
                    inputFiles.map { it.path },
                    finalFile.path
                ) { pct -> onTransformProgress(pct) }
                if (!result.success) {
                    val message = result.errorMessage?.let {
                        context.getString(R.string.download_error_mux_failed_detail, it)
                    } ?: context.getString(R.string.download_error_mux_failed)
                    DownloadWorker.fail(context, current.withClockStopped(), message)
                    workDir.deleteRecursively()
                    return
                }
            } else if (convertTsToAac) {
                val conversionTarget = finalFile
                val converted = withContext(Dispatchers.IO) {
                    TsToAacConverter.convert(videoTrackFile, conversionTarget, { isActive }) { pct -> onTransformProgress(pct) }
                }
                if (!converted) {
                    runCatching { finalFile.delete() }
                    finalFile = DownloadFileHelper.uniqueFile(destDir, "${baseName(finalFilename)}.ts")
                    videoTrackFile.copyTo(finalFile, overwrite = true)
                }
            } else if (convertTsToMp4) {
                val conversionTarget = finalFile
                val converted = withContext(Dispatchers.IO) {
                    TsToMp4Converter.convert(videoTrackFile, conversionTarget, { isActive }) { pct -> onTransformProgress(pct) }
                }
                if (!converted) {
                    runCatching { finalFile.delete() }
                    finalFile = DownloadFileHelper.uniqueFile(destDir, "${baseName(finalFilename)}.ts")
                    videoTrackFile.copyTo(finalFile, overwrite = true)
                }
            } else {
                videoTrackFile.copyTo(finalFile, overwrite = true)
            }

            publishProgress { it.copy(muxProgress = 100) }

            if (!safMode) {
                current.streamSubtitleUrl?.let { subUrl ->
                    runCatching {
                        downloadSubtitleSidecar(subUrl, current.streamPageUrl, current.userAgent, current.streamHeaders, destDir, finalFile)
                    }
                }
            }

            publishProgress { it.copy(status = DownloadStatus.DELETING_TEMP) }
            DownloadNotificationHelper.showDeletingTempNotification(context, current)
            workDir.deleteRecursively()

            current = current.withClockStopped().copy(
                filename = finalFile.name,
                file = finalFile,
                totalBytes = finalFile.length(),
                bytesDownloaded = finalFile.length()
            )

            if (safMode) {
                DownloadWorker.moveTempToSaf(context, current)
            } else {
                current = current.copy(status = DownloadStatus.COMPLETE, completedAt = System.currentTimeMillis())
                ClintDownloadManager.persistDownload(current)
                ClintDownloadManager.publish(current)
                DownloadNotificationHelper.showCompleteNotification(context, current)
                ClintDownloadManager.tryDequeueNext(context)
            }
        } catch (_: StreamCancelledException) {
            if (current.id in ClintDownloadManager.removedIds) {
                workDir.deleteRecursively()
                return
            }
            if (current.id in ClintDownloadManager.pauseRequested) {
                ClintDownloadManager.pauseRequested.remove(current.id)
                val updated = current.withClockStopped().copy(status = DownloadStatus.PAUSED)
                ClintDownloadManager.publish(updated)
                ClintDownloadManager.persistDownload(updated)
                ClintDownloadManager.tryDequeueNext(context)
            }
        } catch (e: Throwable) {
            DownloadWorker.fail(context, current.withClockStopped(), e.message ?: context.getString(R.string.download_error_unknown))
        }
    }

    private const val MAX_RESOLVE_ATTEMPTS = 5

    private suspend fun downloadTrackResilient(
        initialTrack: ResolvedTrack,
        workDir: File,
        pageUrl: String,
        userAgent: String,
        extraHeaders: Map<String, String>,
        isCancelled: () -> Boolean,
        resolve: () -> ResolvedTrack?,
        initialDownloaded: Map<Int, DownloadedSegment> = emptyMap(),
        speedLimiter: com.jhaiian.clint.downloads.SpeedLimiter? = null,
        concurrentSegments: Int = 6,
        onProgress: (completed: Int, total: Int, segmentBytes: Long) -> Unit
    ): List<DownloadedSegment> {
        var track = initialTrack
        var downloaded: Map<Int, DownloadedSegment> = initialDownloaded
        var lastError: Throwable? = null
        var attempt = 0
        while (attempt < MAX_RESOLVE_ATTEMPTS) {
            if (isCancelled()) throw StreamCancelledException()
            try {
                downloaded = StreamTrackDownloader.downloadSegments(
                    track, workDir, pageUrl, "", userAgent, isCancelled, downloaded, extraHeaders, speedLimiter, concurrentSegments, onProgress
                )
                val total = (listOfNotNull(track.initSegment) + track.segments).size
                if (downloaded.size >= total) {
                    return (0 until total).map { downloaded.getValue(it) }
                }
            } catch (e: StreamCancelledException) {
                throw e
            } catch (e: Throwable) {
                lastError = e
                downloaded = downloaded + loadExistingSegments(workDir, track)
            }
            attempt++
            if (attempt >= MAX_RESOLVE_ATTEMPTS) break
            StreamBackoff.wait(attempt, isCancelled)
            val fresh = resolve()
            if (fresh != null) track = fresh
        }
        throw lastError ?: java.io.IOException("Failed to download segments after $MAX_RESOLVE_ATTEMPTS attempts")
    }

    private fun loadExistingSegments(dir: File, track: ResolvedTrack): Map<Int, DownloadedSegment> {
        if (!dir.isDirectory) return emptyMap()
        val all = listOfNotNull(track.initSegment) + track.segments
        val result = mutableMapOf<Int, DownloadedSegment>()
        for (index in all.indices) {
            val file = File(dir, "seg_%05d.part".format(index))
            if (file.isFile && file.length() > 0L) {
                val alreadyDecrypted = File(file.path + ".dec").isFile
                result[index] = DownloadedSegment(file, if (alreadyDecrypted) null else all[index].key)
            }
        }
        return result
    }

    private fun baseName(filename: String): String {
        val dot = filename.lastIndexOf('.')
        return if (dot > 0) filename.substring(0, dot) else filename
    }

    private fun resolveTrack(
        format: StreamContainerFormat,
        url: String,
        pageUrl: String,
        userAgent: String,
        kind: TrackKind,
        width: Int?,
        height: Int?,
        bandwidth: Long?,
        extraHeaders: Map<String, String>,
        representationId: String? = null
    ): ResolvedTrack? = when (format) {
        StreamContainerFormat.HLS -> HlsPlaylistFetcher.fetch(url, pageUrl, "", userAgent, kind, extraHeaders)
        StreamContainerFormat.DASH -> DashSegmentResolver.fetch(url, pageUrl, "", userAgent, kind, width, height, bandwidth, extraHeaders, representationId)
    }

    private const val MAX_LIVE_POLL_FAILURES = 5
    private const val MAX_LIVE_EMPTY_POLLS = 3

    private suspend fun recordLiveTrack(
        format: StreamContainerFormat,
        url: String,
        pageUrl: String,
        userAgent: String,
        extraHeaders: Map<String, String>,
        workDir: File,
        kind: TrackKind,
        width: Int?,
        height: Int?,
        bandwidth: Long?,
        representationId: String?,
        isCancelled: () -> Boolean,
        speedLimiter: com.jhaiian.clint.downloads.SpeedLimiter?,
        concurrentSegments: Int,
        onSegment: (segmentBytes: Long) -> Unit
    ): Pair<List<DownloadedSegment>, ResolvedTrack> {
        workDir.mkdirs()
        val seenUrls = mutableSetOf<String>()
        val ordered = mutableListOf<DownloadedSegment>()
        var containerHint = "ts"
        var batch = 0
        var consecutiveFailures = 0
        var consecutiveEmptyPolls = 0

        while (true) {
            if (isCancelled()) throw StreamCancelledException()
            val track = resolveTrack(format, url, pageUrl, userAgent, kind, width, height, bandwidth, extraHeaders, representationId)
            if (track == null) {
                consecutiveFailures++
                if (consecutiveFailures >= MAX_LIVE_POLL_FAILURES) {
                    if (ordered.isNotEmpty()) break
                    throw java.io.IOException("Failed to fetch live playlist: $url")
                }
                StreamBackoff.wait(consecutiveFailures, isCancelled)
                continue
            }
            consecutiveFailures = 0
            containerHint = track.containerHintExtension

            val allSpecs = listOfNotNull(track.initSegment) + track.segments
            val newSpecs = allSpecs.filter { it.url !in seenUrls }
            if (newSpecs.isNotEmpty()) {
                consecutiveEmptyPolls = 0
                val batchDir = File(workDir, "b%04d".format(batch++))
                val syntheticTrack = ResolvedTrack(kind, null, newSpecs, null, containerHint)
                val downloaded = StreamTrackDownloader.downloadSegments(
                    syntheticTrack, batchDir, pageUrl, "", userAgent, isCancelled, emptyMap(), extraHeaders,
                    speedLimiter, concurrentSegments
                ) { _, _, segBytes -> onSegment(segBytes) }
                for (i in newSpecs.indices) {
                    downloaded[i]?.let { ordered += it }
                }
                for (spec in newSpecs) seenUrls.add(spec.url)
            } else {
                consecutiveEmptyPolls++
            }

            if (!track.isLive) break
            if (consecutiveEmptyPolls >= MAX_LIVE_EMPTY_POLLS && ordered.isNotEmpty()) break
            if (isCancelled()) throw StreamCancelledException()
            val pollDelayMs = ((track.targetDurationSeconds ?: 4.0) * 1000L).toLong().coerceIn(1500L, 12000L)
            kotlinx.coroutines.delay(pollDelayMs)
        }

        return ordered.toList() to ResolvedTrack(kind, null, emptyList(), null, containerHint)
    }

    private fun downloadSubtitleSidecar(
        url: String,
        pageUrl: String,
        userAgent: String,
        extraHeaders: Map<String, String>,
        destDir: File,
        finalFile: File
    ) {
        val fetched = HlsSubtitleResolver.fetch(url, pageUrl, userAgent, extraHeaders, { false }) ?: return
        val subFile = DownloadFileHelper.uniqueFile(destDir, "${baseName(finalFile.name)}.${fetched.extension}")
        subFile.writeBytes(fetched.bytes)
    }

    private suspend fun runSubtitle(context: Context, initialItem: DownloadItem) {
        var current = initialItem

        fun isCancelled(): Boolean =
            current.id in ClintDownloadManager.removedIds || current.id in ClintDownloadManager.pauseRequested

        fun publishProgress(transform: (DownloadItem) -> DownloadItem) {
            current = transform(current)
            ClintDownloadManager.publish(current)
        }

        try {
            publishProgress {
                it.withClockStarted().copy(
                    status = DownloadStatus.DOWNLOADING, segmentsCompleted = 0, segmentsTotal = 0, bytesDownloaded = 0L
                )
            }
            DownloadNotificationHelper.showProgressNotification(context, current)

            var lastNotifyAt = 0L
            val fetched = withContext(Dispatchers.IO) {
                HlsSubtitleResolver.fetch(
                    current.streamVideoUrl, current.streamPageUrl, current.userAgent, current.streamHeaders, ::isCancelled
                ) { completed, total, bytes ->
                    publishProgress { it.copy(segmentsCompleted = completed, segmentsTotal = total, bytesDownloaded = bytes) }
                    val now = System.currentTimeMillis()
                    if (now - lastNotifyAt > 800) {
                        lastNotifyAt = now
                        DownloadNotificationHelper.showProgressNotification(context, current)
                    }
                }
            }
            if (isCancelled()) throw StreamCancelledException()
            if (fetched == null) {
                DownloadWorker.fail(context, current.withClockStopped(), context.getString(R.string.download_error_playlist_unavailable))
                return
            }
            publishProgress { it.withClockStopped() }

            if (!DownloadFileHelper.isCustomLocationAccessible(context, current.locationMode, current.customLocationUri)) {
                DownloadWorker.fail(context, current.withClockStopped(), context.getString(R.string.download_location_invalid_message))
                return
            }
            val directCustomDir = DownloadFileHelper.resolveDirectCustomDir(context, current)
            val safMode = DownloadFileHelper.isSafCustomMode(context, current) && directCustomDir == null
            val rawDestDir = directCustomDir
                ?: if (safMode) DownloadFileHelper.tempDownloadDir(context) else DownloadFileHelper.resolveDownloadDir()

            val userExtension = current.filename.substringAfterLast('.', "").trim().takeIf { it.isNotBlank() }
            val guessedFinalName = "${baseName(current.filename)}.${userExtension ?: fetched.extension}"
            val destDir = if (safMode) rawDestDir else DownloadCategories.resolveDir(current.categorizeEnabled, rawDestDir, guessedFinalName)
            destDir.mkdirs()
            val finalFile = DownloadFileHelper.uniqueFile(destDir, guessedFinalName)
            finalFile.writeBytes(fetched.bytes)

            current = current.withClockStopped().copy(
                filename = finalFile.name,
                file = finalFile,
                totalBytes = finalFile.length(),
                bytesDownloaded = finalFile.length()
            )

            if (safMode) {
                DownloadWorker.moveTempToSaf(context, current)
            } else {
                current = current.copy(status = DownloadStatus.COMPLETE, completedAt = System.currentTimeMillis())
                ClintDownloadManager.persistDownload(current)
                ClintDownloadManager.publish(current)
                DownloadNotificationHelper.showCompleteNotification(context, current)
                ClintDownloadManager.tryDequeueNext(context)
            }
        } catch (_: StreamCancelledException) {
            if (current.id in ClintDownloadManager.removedIds) return
            if (current.id in ClintDownloadManager.pauseRequested) {
                ClintDownloadManager.pauseRequested.remove(current.id)
                val updated = current.withClockStopped().copy(status = DownloadStatus.PAUSED)
                ClintDownloadManager.publish(updated)
                ClintDownloadManager.persistDownload(updated)
                ClintDownloadManager.tryDequeueNext(context)
            }
        } catch (e: Throwable) {
            DownloadWorker.fail(context, current.withClockStopped(), e.message ?: context.getString(R.string.download_error_unknown))
        }
    }
}
