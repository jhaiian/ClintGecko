package com.jhaiian.clint.mediacapture.download

import android.content.Context
import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.downloads.DownloadAutoTuner
import com.jhaiian.clint.downloads.DownloadCategories
import com.jhaiian.clint.downloads.DownloadFileHelper
import com.jhaiian.clint.downloads.DownloadItem
import com.jhaiian.clint.downloads.DownloadLog
import com.jhaiian.clint.downloads.DownloadNotificationHelper
import com.jhaiian.clint.downloads.DownloadStatus
import com.jhaiian.clint.downloads.DownloadWorker
import com.jhaiian.clint.downloads.StreamStage
import com.jhaiian.clint.R
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

object StreamDownloadJob {

    private fun DownloadItem.withClockStarted(): DownloadItem =
        if (activeStartedAt > 0L) this else copy(activeStartedAt = System.currentTimeMillis())

    private fun DownloadItem.withClockStopped(): DownloadItem =
        if (activeStartedAt > 0L) {
            copy(activeElapsedMs = activeElapsedMs + (System.currentTimeMillis() - activeStartedAt), activeStartedAt = 0L)
        } else this

    private class TrackSource(val files: List<File>, val extension: String) {
        val length: Long get() = files.sumOf { it.length() }
    }

    private fun readStage(workDir: File, name: String): TrackSource? {
        val marker = File(workDir, "$name.done")
        if (!marker.isFile) return null
        val info = runCatching { JSONObject(marker.readText()) }.getOrNull() ?: return null
        val extension = info.optString("ext", "").takeIf { it.isNotBlank() } ?: return null
        val size = info.optLong("size", -1L)
        val listed = info.optJSONArray("files")
        val files = if (listed != null) {
            (0 until listed.length()).map { File(workDir, listed.getString(it)) }
        } else {
            listOf(File(workDir, "${name}_track.$extension"))
        }
        if (files.isEmpty() || size <= 0L || files.any { !it.isFile }) return null
        return if (files.sumOf { it.length() } == size) TrackSource(files, extension) else null
    }

    private fun writeStage(workDir: File, name: String, source: TrackSource) {
        val marker = File(workDir, "$name.done")
        val temp = File(workDir, "$name.done.tmp")
        val files = JSONArray()
        source.files.forEach { files.put(it.relativeTo(workDir).path) }
        temp.writeText(JSONObject().put("ext", source.extension).put("size", source.length).put("files", files).toString())
        if (!temp.renameTo(marker)) {
            marker.writeText(temp.readText())
            temp.delete()
        }
    }

    suspend fun run(context: Context, initialItem: DownloadItem) {
        if (DownloadWorker.isCompleteInSafTemp(context, initialItem)) {
            DownloadLog.i(initialItem.id, "RESUME") { "The finished file is still in temporary storage; copying it to the chosen folder" }
            DownloadWorker.moveTempToSaf(context, initialItem)
            return
        }
        if (initialItem.streamFormat == STREAM_FORMAT_HLS_SUBTITLE) {
            runSubtitle(context, initialItem)
            return
        }
        var current = initialItem
        DownloadLog.section(current.id, "Starting stream download attempt") {
            listOf(
                "File" to current.filename,
                "Format" to current.streamFormat,
                "Page" to current.streamPageUrl.ifBlank { null },
                "Video URL" to current.streamVideoUrl.ifBlank { null },
                "Audio URL" to current.streamAudioUrl,
                "Live" to current.streamIsLive,
                "Segment workers" to (if (DownloadAutoTuner.isAuto(current.streamConcurrentSegments)) "auto" else current.streamConcurrentSegments),
                "Retry attempt" to current.retryAttempt.takeIf { it > 0 }
            )
        }
        val workDir = File(context.filesDir, "stream_downloads/${current.id}")

        fun isCancelled(): Boolean =
            current.id in ClintDownloadManager.removedIds || current.id in ClintDownloadManager.pauseRequested

        var lastPublishAt = 0L

        fun publishProgress(transform: (DownloadItem) -> DownloadItem) {
            current = transform(current)
            lastPublishAt = System.currentTimeMillis()
            ClintDownloadManager.publish(current)
        }

        fun publishProgressThrottled(transform: (DownloadItem) -> DownloadItem) {
            current = transform(current)
            val now = System.currentTimeMillis()
            if (now - lastPublishAt >= PUBLISH_INTERVAL_MS) {
                lastPublishAt = now
                ClintDownloadManager.publish(current)
            }
        }

        try {
            val format = if (current.streamFormat == "DASH") StreamContainerFormat.DASH else StreamContainerFormat.HLS
            val primaryKind = if (current.streamPrimaryIsAudio) TrackKind.AUDIO else TrackKind.VIDEO

            val isLiveCapture = current.streamIsLive
            val videoWorkDir = File(workDir, "video")
            val audioWorkDir = File(workDir, "audio")
            val companionAudioUrl = current.streamAudioUrl
            val isCombination = companionAudioUrl != null && !current.streamPrimaryIsAudio

            val locationMode = current.locationMode
            val customLocationUri = current.customLocationUri
            val directCustomDir = DownloadFileHelper.resolveDirectCustomDir(context, current)
            val safMode = DownloadFileHelper.isSafCustomMode(context, current) && directCustomDir == null
            val rawDestDir = directCustomDir
                ?: if (safMode) DownloadFileHelper.tempDownloadDir(context) else DownloadFileHelper.resolveDownloadDir()
            val destinationWritable = safMode ||
                DownloadFileHelper.isCustomLocationAccessible(context, locationMode, customLocationUri)
            val assembleDirectly = destinationWritable && companionAudioUrl == null
            var directAssembledFile: File? = null

            fun reserveOutput(dir: File, name: String): File =
                if (safMode) DownloadFileHelper.uniqueFile(dir, name) else DownloadFileHelper.uniquePartFile(dir, name)

            fun onTransformProgress(pct: Int) {
                publishProgressThrottled { it.copy(muxProgress = pct.coerceIn(0, 99)) }
                DownloadNotificationHelper.showProgressNotificationThrottled(context, current)
            }

            fun planOutput(trackExtension: String): StreamOutputPlan {
                val hasAudio = companionAudioUrl != null
                val userExtension = current.filename.substringAfterLast('.', "").trim().takeIf { it.isNotBlank() }
                val isTs = trackExtension.equals("ts", ignoreCase = true)
                val wantsTsConversion = current.streamConvertTsToMp4 && !hasAudio && !current.streamPrimaryIsAudio && isTs
                val wantsAacConversion = current.streamConvertTsToMp4 && !hasAudio && current.streamPrimaryIsAudio && isTs
                val audioFragmented = current.streamPrimaryIsAudio && !hasAudio && trackExtension == "mp4"
                val extension = when {
                    hasAudio -> "mp4"
                    audioFragmented && userExtension.equals("aac", ignoreCase = true) -> "m4a"
                    userExtension != null -> userExtension
                    wantsTsConversion -> "mp4"
                    wantsAacConversion -> "aac"
                    audioFragmented -> "m4a"
                    trackExtension == "mp4" -> "mp4"
                    else -> "ts"
                }
                return StreamOutputPlan(
                    extension,
                    wantsTsConversion && !extension.equals("ts", ignoreCase = true),
                    wantsAacConversion && extension.equals("aac", ignoreCase = true)
                )
            }

            fun reserveDirectTarget(extension: String): File? {
                val targetName = "${baseName(current.filename)}.$extension"
                val dir = if (safMode) rawDestDir else DownloadCategories.resolveDir(current.categorizeEnabled, rawDestDir, targetName)
                return runCatching {
                    dir.mkdirs()
                    if (dir.isDirectory) reserveOutput(dir, targetName).takeIf { it.isFile } else null
                }.getOrNull()
            }

            suspend fun placeTrack(source: TrackSource, target: File, onProgress: (Int) -> Unit) {
                val single = source.files.singleOrNull()
                if (single != null && DownloadFileHelper.moveFileDirect(single, target)) return
                withContext(Dispatchers.IO) {
                    StreamTrackDownloader.concatenateMoving(
                        source.files.map { DownloadedSegment(it, null) }, target, { ensureActive() }, onProgress
                    )
                }
            }

            suspend fun finalizeTrack(name: String, downloadedSegments: List<DownloadedSegment>, extension: String): TrackSource {
                var segments = downloadedSegments
                val encryptedTotal = segments.count { it.key != null }
                if (encryptedTotal > 0) {
                    publishProgress { it.copy(status = DownloadStatus.DECRYPTING, segmentsCompleted = 0, segmentsTotal = encryptedTotal) }
                    DownloadNotificationHelper.showProgressNotification(context, current)
                    segments = withContext(Dispatchers.IO) {
                        StreamTrackDownloader.decryptSegments(
                            segments, current.streamPageUrl, "", current.userAgent, ::isCancelled, current.streamHeaders
                        ) { done, _ ->
                            publishProgressThrottled { it.copy(segmentsCompleted = done) }
                        }
                    }
                }

                if (assembleDirectly && name == "video") {
                    val plan = planOutput(extension)
                    if (!plan.convertTsToMp4 && !plan.convertTsToAac) {
                        val assembled = reserveDirectTarget(plan.extension)
                        if (assembled != null) {
                            publishProgress {
                                it.copy(
                                    status = DownloadStatus.MUXING, streamStage = StreamStage.NONE,
                                    muxProgress = 0, speedBytesPerSec = 0L
                                )
                            }
                            DownloadNotificationHelper.showProgressNotification(context, current)
                            try {
                                withContext(Dispatchers.IO) {
                                    StreamTrackDownloader.concatenateMoving(segments, assembled, { ensureActive() }) { pct ->
                                        onTransformProgress(pct)
                                    }
                                }
                            } catch (e: Throwable) {
                                runCatching { assembled.delete() }
                                throw e
                            }
                            directAssembledFile = assembled
                            return TrackSource(listOf(assembled), extension)
                        }
                    }
                }

                val source = TrackSource(segments.map { it.file }, extension)
                withContext(Dispatchers.IO) { writeStage(workDir, name, source) }
                return source
            }

            var baseSegDone = 0
            var baseSegTotal = 0
            var baseSegBytes = 0L

            suspend fun runTrackStage(
                name: String,
                stage: StreamStage,
                segmentsDir: File,
                url: String,
                kind: TrackKind,
                width: Int?,
                height: Int?,
                bandwidth: Long?,
                representationId: String?
            ): TrackSource {
                readStage(workDir, name)?.let { finished ->
                    val savedTotal = runCatching { File(workDir, "$name.total").readText().trim().toInt() }.getOrDefault(0)
                    baseSegDone += savedTotal
                    baseSegTotal += savedTotal
                    baseSegBytes += finished.length
                    return finished
                }

                publishProgress {
                    it.withClockStarted().copy(
                        status = DownloadStatus.DOWNLOADING, streamStage = stage, segmentsCompleted = 0, segmentsTotal = 0, partProgress = emptyList(),
                        bytesDownloaded = 0L, speedBytesPerSec = 0L, copyProgress = 0,
                        streamSegDone = baseSegDone, streamSegTotal = baseSegTotal, streamSegBytes = baseSegBytes
                    )
                }
                DownloadNotificationHelper.showProgressNotification(context, current)

                fun resolveNow(): ResolvedTrack? = resolveTrack(
                    format, url, current.streamPageUrl, current.userAgent, kind, width, height, bandwidth,
                    current.streamHeaders, representationId
                )

                val track = resolveNow()
                    ?: throw java.io.IOException(context.getString(R.string.download_error_playlist_unavailable))
                val trackSpecs = listOfNotNull(track.initSegment) + track.segments
                unsupportedEncryption(trackSpecs)?.let { method ->
                    throw StreamUnsupportedException(context.getString(R.string.download_error_stream_encryption_unsupported, method))
                }
                prefetchKeys(context, trackSpecs, current.streamPageUrl, current.userAgent, current.streamHeaders, ::isCancelled)
                val totalSegments = track.segments.size + if (track.initSegment != null) 1 else 0
                runCatching {
                    workDir.mkdirs()
                    File(workDir, "$name.total").writeText(totalSegments.toString())
                }
                val existing = loadExistingSegments(segmentsDir, track)
                val bytesAtStart = existing.values.sumOf { it.file.length() }
                DownloadLog.i(current.id, "PLAYLIST") {
                    buildString {
                        append("Resolved $name track \u00B7 ${track.segments.size} segments")
                        if (track.initSegment != null) append(" + init segment")
                        track.durationSeconds?.let { append(" \u00B7 duration ${com.jhaiian.clint.downloads.formatElapsed(it.toLong())}") }
                        track.targetDurationSeconds?.let { append(" \u00B7 target ${it}s per segment") }
                        append(" \u00B7 container ${track.containerHintExtension}")
                        if (track.segments.any { it.key != null }) append(" \u00B7 encrypted")
                        if (existing.isNotEmpty()) append(" \u00B7 resuming with ${existing.size} segments already on disk")
                        append("\nPlaylist: $url")
                        track.segments.firstOrNull()?.let { append("\nFirst segment: ${it.url}") }
                        track.segments.lastOrNull()?.let { append("\nLast segment: ${it.url}") }
                    }
                }

                publishProgress {
                    it.copy(
                        segmentsCompleted = existing.size, segmentsTotal = totalSegments, bytesDownloaded = bytesAtStart,
                        streamSegDone = baseSegDone + existing.size, streamSegTotal = baseSegTotal + totalSegments,
                        streamSegBytes = baseSegBytes + bytesAtStart,
                        totalBytes = if (existing.isNotEmpty()) (bytesAtStart.toDouble() / existing.size * totalSegments).toLong() else -1L
                    )
                }
                DownloadNotificationHelper.showProgressNotification(context, current)

                var completed = existing.size
                var bytesTotal = bytesAtStart
                var lastSpeedBytes = bytesAtStart
                var lastSpeedTime = System.currentTimeMillis()

                fun onSegmentComplete(segmentBytes: Long) {
                    completed++
                    bytesTotal += segmentBytes
                    val now = System.currentTimeMillis()
                    val elapsed = now - lastSpeedTime
                    val speed = if (elapsed >= 500) {
                        val delta = bytesTotal - lastSpeedBytes
                        lastSpeedBytes = bytesTotal
                        lastSpeedTime = now
                        if (elapsed > 0) delta * 1000L / elapsed else 0L
                    } else current.speedBytesPerSec
                    val projectedTotal = (bytesTotal.toDouble() / completed * totalSegments).toLong()
                    publishProgressThrottled {
                        it.copy(
                            segmentsCompleted = completed, bytesDownloaded = bytesTotal, speedBytesPerSec = speed, totalBytes = projectedTotal,
                            streamSegDone = baseSegDone + completed, streamSegBytes = baseSegBytes + bytesTotal
                        )
                    }
                    DownloadNotificationHelper.showProgressNotificationThrottled(context, current)
                }

                val segments = downloadTrackResilient(
                    track, segmentsDir, current.streamPageUrl, current.userAgent, current.streamHeaders, ::isCancelled,
                    { resolveNow() },
                    initialDownloaded = existing,
                    speedLimiter = ClintDownloadManager.activeSpeedLimiters[current.id],
                    concurrentSegments = current.streamConcurrentSegments,
                    logId = current.id,
                    onSegmentMap = { map -> publishProgressThrottled { it.copy(partProgress = map) } }
                ) { _, _, segBytes -> onSegmentComplete(segBytes) }

                baseSegDone += totalSegments
                baseSegTotal += totalSegments
                baseSegBytes += bytesTotal

                return finalizeTrack(name, segments, track.containerHintExtension)
            }

            val videoTrack: TrackSource
            val videoExtension: String
            val audioTrack: TrackSource?

            val finishedLiveVideo = if (isLiveCapture) readStage(workDir, "video") else null

            if (isLiveCapture && finishedLiveVideo != null) {
                videoTrack = finishedLiveVideo
                videoExtension = finishedLiveVideo.extension
                audioTrack = if (companionAudioUrl != null) {
                    readStage(workDir, "audio") ?: run {
                        val savedAudio = loadLiveState(audioWorkDir)
                        finalizeTrack("audio", savedAudio.ordered, savedAudio.containerHint)
                    }
                } else null
            } else if (isLiveCapture) {
                publishProgress {
                    it.withClockStarted().copy(
                        status = DownloadStatus.DOWNLOADING, segmentsCompleted = 0, segmentsTotal = 0, bytesDownloaded = 0L,
                        streamSegDone = 0, streamSegTotal = 0, streamSegBytes = 0L
                    )
                }
                DownloadNotificationHelper.showProgressNotification(context, current)

                var liveCompleted = 0
                var liveBytes = 0L
                var lastSpeedBytes = 0L
                var lastSpeedTime = System.currentTimeMillis()

                fun onLiveRestored(count: Int, bytes: Long) {
                    liveCompleted += count
                    liveBytes += bytes
                    lastSpeedBytes = liveBytes
                    publishProgress {
                        it.copy(
                            segmentsCompleted = liveCompleted, bytesDownloaded = liveBytes,
                            streamSegDone = liveCompleted, streamSegBytes = liveBytes
                        )
                    }
                }

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
                    publishProgressThrottled {
                        it.copy(
                            segmentsCompleted = liveCompleted, bytesDownloaded = liveBytes, speedBytesPerSec = speed,
                            streamSegDone = liveCompleted, streamSegBytes = liveBytes
                        )
                    }
                    DownloadNotificationHelper.showProgressNotificationThrottled(context, current)
                }

                val results = coroutineScope {
                    val videoDeferred = async {
                        recordLiveTrack(
                            context, format, current.streamVideoUrl, current.streamPageUrl, current.userAgent, current.streamHeaders,
                            videoWorkDir, primaryKind, current.streamVideoWidth, current.streamVideoHeight,
                            current.streamVideoBandwidth, current.streamVideoRepresentationId,
                            ::isCancelled, ClintDownloadManager.activeSpeedLimiters[current.id],
                            current.streamConcurrentSegments, ::onLiveRestored, ::onLiveSegment, logId = current.id
                        )
                    }
                    val audioDeferred = if (companionAudioUrl != null) {
                        async {
                            recordLiveTrack(
                                context, format, companionAudioUrl, current.streamPageUrl, current.userAgent, current.streamHeaders,
                                audioWorkDir, TrackKind.AUDIO, null, null,
                                current.streamAudioBandwidth, current.streamAudioRepresentationId,
                                ::isCancelled, ClintDownloadManager.activeSpeedLimiters[current.id],
                                current.streamConcurrentSegments, ::onLiveRestored, ::onLiveSegment, logId = current.id
                            )
                        }
                    } else null
                    videoDeferred.await() to audioDeferred?.await()
                }

                publishProgress { it.withClockStopped().copy(streamSegTotal = liveCompleted) }
                videoExtension = results.first.second.containerHintExtension
                videoTrack = finalizeTrack("video", results.first.first, videoExtension)
                audioTrack = results.second?.let { finalizeTrack("audio", it.first, it.second.containerHintExtension) }
            } else {
                val videoStage = runTrackStage(
                    "video", if (isCombination) StreamStage.VIDEO else StreamStage.NONE, videoWorkDir,
                    current.streamVideoUrl, primaryKind, current.streamVideoWidth, current.streamVideoHeight,
                    current.streamVideoBandwidth, current.streamVideoRepresentationId
                )
                videoTrack = videoStage
                videoExtension = videoStage.extension
                audioTrack = if (companionAudioUrl != null) {
                    runTrackStage(
                        "audio", StreamStage.AUDIO, audioWorkDir, companionAudioUrl, TrackKind.AUDIO, null, null,
                        current.streamAudioBandwidth, current.streamAudioRepresentationId
                    )
                } else null
                publishProgress { it.withClockStopped() }
            }

            val needsMux = audioTrack != null
            val outputPlan = planOutput(videoExtension)
            val outExtension = outputPlan.extension
            val convertTsToMp4 = outputPlan.convertTsToMp4
            val convertTsToAac = outputPlan.convertTsToAac

            val mergeInputBytes = withContext(Dispatchers.IO) { videoTrack.length + (audioTrack?.length ?: 0L) }
            publishProgress {
                it.copy(
                    status = if (convertTsToMp4 || convertTsToAac) DownloadStatus.CONVERTING else DownloadStatus.MUXING,
                    streamStage = if (needsMux) StreamStage.MERGING else StreamStage.NONE,
                    muxProgress = 0, segmentsCompleted = 0, segmentsTotal = 0, speedBytesPerSec = 0L,
                    bytesDownloaded = mergeInputBytes, totalBytes = mergeInputBytes
                )
            }
            DownloadNotificationHelper.showProgressNotification(context, current)

            val inputTracks = listOfNotNull(videoTrack, audioTrack)

            if (!DownloadFileHelper.isCustomLocationAccessible(context, locationMode, customLocationUri)) {
                directAssembledFile?.let { runCatching { it.delete() } }
                DownloadWorker.fail(
                    context, current.withClockStopped().copy(streamStage = StreamStage.NONE),
                    context.getString(R.string.download_location_invalid_message), scheduleRetry = false
                )
                return
            }
            val assembledFile = directAssembledFile
            val guessedFinalName = "${baseName(current.filename)}.$outExtension"
            val destDir = when {
                assembledFile != null -> assembledFile.parentFile ?: rawDestDir
                safMode -> rawDestDir
                else -> DownloadCategories.resolveDir(current.categorizeEnabled, rawDestDir, guessedFinalName)
            }
            destDir.mkdirs()
            var finalFile = assembledFile ?: withContext(Dispatchers.IO) { reserveOutput(destDir, guessedFinalName) }
            val finalFilename = DownloadFileHelper.finalNameOf(finalFile)

            suspend fun placeTsFallback(failedTarget: File): File {
                runCatching { failedTarget.delete() }
                currentCoroutineContext().ensureActive()
                if (isCancelled()) throw StreamCancelledException()
                val fallback = withContext(Dispatchers.IO) { reserveOutput(destDir, "${baseName(finalFilename)}.ts") }
                try {
                    placeTrack(videoTrack, fallback) { }
                } catch (e: Throwable) {
                    runCatching { fallback.delete() }
                    throw e
                }
                return fallback
            }

            if (needsMux) {
                val result = try {
                    MediaRemuxer.remuxSegments(
                        inputTracks.map { it.files },
                        finalFile.path,
                        current.streamAudioLanguage
                    ) { pct -> onTransformProgress(pct) }
                } catch (e: CancellationException) {
                    runCatching { finalFile.delete() }
                    throw e
                }
                if (!result.success) {
                    runCatching { finalFile.delete() }
                    val message = result.errorMessage?.let {
                        context.getString(R.string.download_error_mux_failed_detail, it)
                    } ?: context.getString(R.string.download_error_mux_failed)
                    DownloadWorker.fail(
                        context, current.withClockStopped().copy(streamStage = StreamStage.NONE), message, scheduleRetry = false
                    )
                    return
                }
                val verification = MediaRemuxer.verify(finalFile.path, expectVideo = true, expectAudio = true)
                if (verification != MuxVerification.OK) {
                    runCatching { finalFile.delete() }
                    val messageRes = when (verification) {
                        MuxVerification.MISSING_VIDEO -> R.string.download_error_mux_missing_video
                        MuxVerification.MISSING_AUDIO -> R.string.download_error_mux_missing_audio
                        else -> R.string.download_error_mux_unreadable
                    }
                    DownloadWorker.fail(
                        context, current.withClockStopped().copy(streamStage = StreamStage.NONE),
                        context.getString(messageRes), scheduleRetry = false
                    )
                    return
                }
            } else if (convertTsToAac) {
                val conversionTarget = finalFile
                val converted = withContext(Dispatchers.IO) {
                    TsToAacConverter.convert(videoTrack.files, conversionTarget, { isActive }) { pct -> onTransformProgress(pct) }
                }
                if (!converted) finalFile = placeTsFallback(finalFile)
            } else if (convertTsToMp4) {
                val conversionTarget = finalFile
                val converted = withContext(Dispatchers.IO) {
                    TsToMp4Converter.convert(videoTrack.files, conversionTarget, { isActive }) { pct -> onTransformProgress(pct) }
                }
                if (!converted) finalFile = placeTsFallback(finalFile)
            } else if (assembledFile == null) {
                val placementTarget = finalFile
                try {
                    placeTrack(videoTrack, placementTarget) { pct -> onTransformProgress(pct) }
                } catch (e: Throwable) {
                    runCatching { placementTarget.delete() }
                    throw e
                }
            }

            publishProgress { it.copy(muxProgress = 100) }

            if (!safMode && DownloadFileHelper.isPartFile(finalFile)) {
                val partFile = finalFile
                val promoted = withContext(Dispatchers.IO) { DownloadFileHelper.promotePartFile(partFile) }
                if (promoted != null) {
                    finalFile = promoted
                } else {
                    DownloadLog.w(current.id, "FILE") { "Could not rename ${partFile.name}; keeping the finished file under its temporary name" }
                }
            }

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
                filename = DownloadFileHelper.finalNameOf(finalFile),
                file = finalFile,
                streamStage = StreamStage.NONE,
                totalBytes = finalFile.length(),
                bytesDownloaded = finalFile.length()
            )

            if (safMode) {
                DownloadWorker.moveTempToSaf(context, current)
            } else {
                current = current.copy(status = DownloadStatus.COMPLETE, completedAt = System.currentTimeMillis())
                ClintDownloadManager.persistDownload(current)
                ClintDownloadManager.publish(current)
                ClintDownloadManager.onDownloadCompleted(context, current)
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: StreamUnsupportedException) {
            DownloadLog.e(current.id, "ERROR") { "Stream cannot be saved: ${e.message}" }
            DownloadWorker.fail(
                context, current.withClockStopped(), e.message ?: context.getString(R.string.download_error_unknown), scheduleRetry = false
            )
        } catch (e: Throwable) {
            DownloadLog.error(current.id, "ERROR", "Stream attempt failed: ${e.javaClass.simpleName}: ${e.message}", e)
            val stopped = current.withClockStopped()
            val failed = if (stopped.retryAttempt > 0 && stopped.streamSegDone > initialItem.streamSegDone) stopped.copy(retryAttempt = 0) else stopped
            DownloadWorker.fail(context, failed, e.message ?: context.getString(R.string.download_error_unknown))
        }
    }

    private const val PUBLISH_INTERVAL_MS = 250L

    private class StreamOutputPlan(val extension: String, val convertTsToMp4: Boolean, val convertTsToAac: Boolean)

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
        concurrentSegments: Int = DownloadAutoTuner.AUTO,
        logId: Int = DownloadLog.GLOBAL_ID,
        onSegmentMap: ((List<Float>) -> Unit)? = null,
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
                    track, workDir, pageUrl, "", userAgent, isCancelled, downloaded, extraHeaders, speedLimiter, concurrentSegments,
                    logId = logId, onSegmentMap = onSegmentMap, onProgress = onProgress
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
                DownloadLog.w(logId, "PLAYLIST") {
                    "Segment pass ${attempt + 1}/$MAX_RESOLVE_ATTEMPTS stopped: ${e.message} \u00B7 ${downloaded.size} segments kept \u00B7 refreshing the playlist"
                }
            }
            attempt++
            if (attempt >= MAX_RESOLVE_ATTEMPTS) break
            StreamBackoff.wait(attempt, isCancelled)
            val fresh = resolve()
            if (fresh != null) track = fresh
        }
        throw lastError ?: java.io.IOException("Failed to download segments after $MAX_RESOLVE_ATTEMPTS attempts")
    }

    private fun loadExistingSegments(dir: File, track: ResolvedTrack): Map<Int, DownloadedSegment> =
        loadSavedSegments(dir, listOfNotNull(track.initSegment) + track.segments)

    private fun loadSavedSegments(dir: File, specs: List<SegmentSpec>): Map<Int, DownloadedSegment> {
        if (!dir.isDirectory) return emptyMap()
        val result = mutableMapOf<Int, DownloadedSegment>()
        for (index in specs.indices) {
            savedSegment(File(dir, "seg_%05d.part".format(index)), specs[index].key)?.let { result[index] = it }
        }
        return result
    }

    private fun savedSegment(file: File, key: HlsKeyInfo?): DownloadedSegment? {
        val plain = File(file.path + StreamTrackDownloader.PLAIN_SUFFIX)
        return when {
            plain.isFile && plain.length() > 0L -> DownloadedSegment(plain, null)
            file.isFile && file.length() > 0L -> DownloadedSegment(file, if (File(file.path + ".dec").isFile) null else key)
            else -> null
        }
    }

    private class StreamUnsupportedException(message: String) : Exception(message)

    private const val KEY_FETCH_ATTEMPTS = 3

    private fun unsupportedEncryption(specs: List<SegmentSpec>): String? =
        specs.firstNotNullOfOrNull { spec -> spec.key?.method?.takeIf { !HlsAesDecryptor.isSupported(it) } }

    private suspend fun prefetchKeys(
        context: Context,
        specs: List<SegmentSpec>,
        pageUrl: String,
        userAgent: String,
        extraHeaders: Map<String, String>,
        isCancelled: () -> Boolean
    ) {
        for (key in specs.mapNotNull { it.key }.distinctBy { it.keyUri }) {
            var attempt = 0
            while (!withContext(Dispatchers.IO) { HlsAesDecryptor.prefetch(key, pageUrl, "", userAgent, extraHeaders) }) {
                attempt++
                if (attempt >= KEY_FETCH_ATTEMPTS) {
                    throw java.io.IOException(context.getString(R.string.download_error_stream_key_unavailable))
                }
                StreamBackoff.wait(attempt, isCancelled)
                if (isCancelled()) throw StreamCancelledException()
            }
        }
    }

    private const val LIVE_STATE_FILE = "live.log"
    private val LIVE_BATCH_DIR = Regex("""b(\d{4,})""")

    private class LiveState(
        val seenUrls: MutableSet<String>,
        val ordered: MutableList<DownloadedSegment>,
        val nextBatch: Int,
        val containerHint: String
    )

    private fun loadLiveState(workDir: File): LiveState {
        val seen = mutableSetOf<String>()
        val ordered = mutableListOf<DownloadedSegment>()
        var containerHint = "ts"
        val log = File(workDir, LIVE_STATE_FILE)
        if (log.isFile) {
            runCatching {
                log.forEachLine { line ->
                    val parts = line.split('\t')
                    when {
                        parts.size == 2 && parts[0] == "seen" -> seen += parts[1]
                        parts.size == 2 && parts[0] == "container" -> containerHint = parts[1]
                        parts.size == 5 && parts[0] == "seg" -> {
                            val key = if (parts[2] == "-") null else HlsKeyInfo(parts[2], parts[3].takeIf { it != "-" }, parts[4])
                            savedSegment(File(workDir, parts[1]), key)?.let { ordered += it }
                        }
                    }
                }
            }
        }
        val nextBatch = workDir.listFiles()
            ?.mapNotNull { dir -> LIVE_BATCH_DIR.matchEntire(dir.name)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { dir.isDirectory } }
            ?.maxOrNull()?.plus(1) ?: 0
        return LiveState(seen, ordered, nextBatch, containerHint)
    }

    private fun appendLiveState(workDir: File, containerHint: String, specs: List<SegmentSpec>, saved: List<DownloadedSegment>) {
        runCatching {
            val text = buildString {
                append("container\t").append(containerHint).append('\n')
                for (segment in saved) {
                    val key = segment.key
                    append("seg\t").append(segment.file.relativeTo(workDir).path)
                    append('\t').append(key?.keyUri ?: "-")
                    append('\t').append(key?.ivHex ?: "-")
                    append('\t').append(key?.method ?: "-")
                    append('\n')
                }
                for (spec in specs) append("seen\t").append(spec.url).append('\n')
            }
            File(workDir, LIVE_STATE_FILE).appendText(text)
        }
    }

    private fun baseName(filename: String): String {
        val dot = filename.lastIndexOf('.')
        return if (dot > 0) filename.substring(0, dot) else filename
    }

    internal fun savedSegmentTotal(context: Context, id: Int, name: String): Int? {
        val dir = File(context.filesDir, "stream_downloads/$id")
        if (File(dir, "$name.done").isFile) return null
        return File(dir, "$name.total").takeIf { it.isFile }?.readText()?.trim()?.toIntOrNull()
    }

    internal fun probeTrack(item: DownloadItem, url: String, companion: Boolean): ResolvedTrack? {
        val format = if (item.streamFormat == "DASH") StreamContainerFormat.DASH else StreamContainerFormat.HLS
        val kind = if (companion || item.streamPrimaryIsAudio) TrackKind.AUDIO else TrackKind.VIDEO
        return resolveTrack(
            format, url, item.streamPageUrl, item.userAgent, kind,
            if (companion) null else item.streamVideoWidth,
            if (companion) null else item.streamVideoHeight,
            if (companion) item.streamAudioBandwidth else item.streamVideoBandwidth,
            item.streamHeaders,
            if (companion) item.streamAudioRepresentationId else item.streamVideoRepresentationId
        )
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
    private const val MAX_LIVE_FAILED_BATCHES = 5

    private suspend fun recordLiveTrack(
        context: Context,
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
        onRestored: (count: Int, bytes: Long) -> Unit,
        onSegment: (segmentBytes: Long) -> Unit,
        logId: Int = DownloadLog.GLOBAL_ID
    ): Pair<List<DownloadedSegment>, ResolvedTrack> {
        workDir.mkdirs()
        val state = loadLiveState(workDir)
        val seenUrls = state.seenUrls
        val ordered = state.ordered
        var containerHint = state.containerHint
        var batch = state.nextBatch
        var consecutiveFailures = 0
        var consecutiveEmptyPolls = 0
        var consecutiveFailedBatches = 0
        if (ordered.isNotEmpty()) {
            DownloadLog.i(logId, "LIVE") { "Continuing the recording with ${ordered.size} segments already saved" }
            onRestored(ordered.size, ordered.sumOf { it.file.length() })
        }

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
                val unsupportedMethod = unsupportedEncryption(newSpecs)
                if (unsupportedMethod != null) {
                    if (ordered.isNotEmpty()) {
                        DownloadLog.w(logId, "LIVE") { "The stream switched to $unsupportedMethod encryption; finishing with what was recorded" }
                        break
                    }
                    throw StreamUnsupportedException(context.getString(R.string.download_error_stream_encryption_unsupported, unsupportedMethod))
                }
                prefetchKeys(context, newSpecs, pageUrl, userAgent, extraHeaders, isCancelled)
                val batchDir = File(workDir, "b%04d".format(batch++))
                val syntheticTrack = ResolvedTrack(kind, null, newSpecs, null, containerHint)
                val downloaded = try {
                    StreamTrackDownloader.downloadSegments(
                        syntheticTrack, batchDir, pageUrl, "", userAgent, isCancelled, emptyMap(), extraHeaders,
                        speedLimiter, concurrentSegments, logId = logId
                    ) { _, _, segBytes -> onSegment(segBytes) }
                } catch (e: StreamCancelledException) {
                    throw e
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    DownloadLog.w(logId, "LIVE") { "Part of this batch could not be fetched (${e.message}); keeping the rest and moving on" }
                    loadSavedSegments(batchDir, newSpecs)
                }
                val saved = newSpecs.indices.mapNotNull { downloaded[it] }
                ordered += saved
                for (spec in newSpecs) seenUrls.add(spec.url)
                appendLiveState(workDir, containerHint, newSpecs, saved)
                if (saved.isEmpty()) {
                    consecutiveFailedBatches++
                    if (consecutiveFailedBatches >= MAX_LIVE_FAILED_BATCHES) {
                        if (ordered.isNotEmpty()) break
                        throw java.io.IOException("Failed to fetch live segments: $url")
                    }
                } else {
                    consecutiveFailedBatches = 0
                }
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
                    status = DownloadStatus.DOWNLOADING, segmentsCompleted = 0, segmentsTotal = 0, bytesDownloaded = 0L,
                    streamSegDone = 0, streamSegTotal = 0, streamSegBytes = 0L
                )
            }
            DownloadNotificationHelper.showProgressNotification(context, current)

            val fetched = withContext(Dispatchers.IO) {
                HlsSubtitleResolver.fetch(
                    current.streamVideoUrl, current.streamPageUrl, current.userAgent, current.streamHeaders, ::isCancelled
                ) { completed, total, bytes ->
                    publishProgress {
                        it.copy(
                            segmentsCompleted = completed, segmentsTotal = total, bytesDownloaded = bytes,
                            streamSegDone = completed, streamSegTotal = total, streamSegBytes = bytes
                        )
                    }
                    DownloadNotificationHelper.showProgressNotificationThrottled(context, current)
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
                ClintDownloadManager.onDownloadCompleted(context, current)
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            DownloadLog.error(current.id, "ERROR", "Stream attempt failed: ${e.javaClass.simpleName}: ${e.message}", e)
            DownloadWorker.fail(context, current.withClockStopped(), e.message ?: context.getString(R.string.download_error_unknown))
        }
    }
}
