package com.jhaiian.clint.mediacapture.download

import android.content.Context
import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.downloads.DownloadCategories
import com.jhaiian.clint.downloads.DownloadFileHelper
import com.jhaiian.clint.downloads.DownloadItem
import com.jhaiian.clint.downloads.DownloadNotificationHelper
import com.jhaiian.clint.downloads.DownloadStatus
import com.jhaiian.clint.downloads.DownloadWorker
import com.jhaiian.clint.downloads.StreamStage
import com.jhaiian.clint.R
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    private fun readStage(workDir: File, name: String): Pair<File, String>? {
        val marker = File(workDir, "$name.done")
        if (!marker.isFile) return null
        val info = runCatching { JSONObject(marker.readText()) }.getOrNull() ?: return null
        val extension = info.optString("ext", "").takeIf { it.isNotBlank() } ?: return null
        val size = info.optLong("size", -1L)
        val file = File(workDir, "${name}_track.$extension")
        return if (file.isFile && size > 0L && file.length() == size) file to extension else null
    }

    private fun writeStage(workDir: File, name: String, file: File, extension: String) {
        val marker = File(workDir, "$name.done")
        val temp = File(workDir, "$name.done.tmp")
        temp.writeText(JSONObject().put("ext", extension).put("size", file.length()).toString())
        if (!temp.renameTo(marker)) {
            marker.writeText(temp.readText())
            temp.delete()
        }
    }

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
            val companionAudioUrl = current.streamAudioUrl
            val isCombination = companionAudioUrl != null && !current.streamPrimaryIsAudio

            val locationMode = current.locationMode
            val customLocationUri = current.customLocationUri
            val directCustomDir = DownloadFileHelper.resolveDirectCustomDir(context, current)
            val safMode = DownloadFileHelper.isSafCustomMode(context, current) && directCustomDir == null
            val rawDestDir = directCustomDir
                ?: if (safMode) DownloadFileHelper.tempDownloadDir(context) else DownloadFileHelper.resolveDownloadDir()
            val directMoveAllowed = !safMode &&
                DownloadFileHelper.isCustomLocationAccessible(context, locationMode, customLocationUri) &&
                DownloadFileHelper.canWriteSharedStorageDirectly(context)
            val assembleDirectly = directMoveAllowed && companionAudioUrl == null
            var directAssembledFile: File? = null

            var lastTransformNotifyAt = 0L
            fun onTransformProgress(pct: Int) {
                publishProgress { it.copy(muxProgress = pct.coerceIn(0, 99)) }
                val now = System.currentTimeMillis()
                if (now - lastTransformNotifyAt > 800) {
                    lastTransformNotifyAt = now
                    DownloadNotificationHelper.showProgressNotification(context, current)
                }
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
                val dir = DownloadCategories.resolveDir(current.categorizeEnabled, rawDestDir, targetName)
                return runCatching {
                    dir.mkdirs()
                    if (dir.isDirectory) DownloadFileHelper.uniqueFile(dir, targetName).takeIf { it.isFile } else null
                }.getOrNull()
            }

            suspend fun placeTrack(source: File, target: File, onProgress: (Int) -> Unit) {
                if (directMoveAllowed && DownloadFileHelper.moveFileDirect(source, target)) return
                copyFileWithProgress(source, target, onProgress)
            }

            suspend fun finalizeTrack(name: String, segments: List<DownloadedSegment>, extension: String): File {
                val encryptedTotal = segments.count { it.key != null }
                if (encryptedTotal > 0) {
                    publishProgress { it.copy(status = DownloadStatus.DECRYPTING, segmentsCompleted = 0, segmentsTotal = encryptedTotal) }
                    DownloadNotificationHelper.showProgressNotification(context, current)
                    var decryptCompleted = 0
                    StreamTrackDownloader.decryptSegments(
                        segments, current.streamPageUrl, "", current.userAgent, ::isCancelled, current.streamHeaders
                    ) { _, _ ->
                        decryptCompleted++
                        publishProgress { it.copy(segmentsCompleted = decryptCompleted) }
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
                            return assembled
                        }
                    }
                }

                publishProgress { it.copy(status = DownloadStatus.COPYING_TEMP, copyProgress = 0) }
                DownloadNotificationHelper.showCopyingTempNotification(context, current)

                val partFile = File(workDir, "${name}_track.part")
                val trackFile = File(workDir, "${name}_track.$extension")
                var lastCopyNotifyAt = 0L
                withContext(Dispatchers.IO) {
                    StreamTrackDownloader.concatenate(segments, partFile) { pct ->
                        publishProgress { it.copy(copyProgress = pct) }
                        val now = System.currentTimeMillis()
                        if (now - lastCopyNotifyAt > 800) {
                            lastCopyNotifyAt = now
                            DownloadNotificationHelper.showCopyingTempNotification(context, current)
                        }
                    }
                    trackFile.delete()
                    if (!partFile.renameTo(trackFile)) throw java.io.IOException("Failed to finalize $name track")
                }
                StreamTrackDownloader.cleanup(segments)
                writeStage(workDir, name, trackFile, extension)
                return trackFile
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
            ): Pair<File, String> {
                readStage(workDir, name)?.let { finished ->
                    val savedTotal = runCatching { File(workDir, "$name.total").readText().trim().toInt() }.getOrDefault(0)
                    baseSegDone += savedTotal
                    baseSegTotal += savedTotal
                    baseSegBytes += finished.first.length()
                    return finished
                }

                publishProgress {
                    it.withClockStarted().copy(
                        status = DownloadStatus.DOWNLOADING, streamStage = stage, segmentsCompleted = 0, segmentsTotal = 0,
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
                val totalSegments = track.segments.size + if (track.initSegment != null) 1 else 0
                runCatching {
                    workDir.mkdirs()
                    File(workDir, "$name.total").writeText(totalSegments.toString())
                }
                val existing = loadExistingSegments(segmentsDir, track)
                val bytesAtStart = existing.values.sumOf { it.file.length() }

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
                var lastNotifyAt = 0L
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
                    publishProgress {
                        it.copy(
                            segmentsCompleted = completed, bytesDownloaded = bytesTotal, speedBytesPerSec = speed, totalBytes = projectedTotal,
                            streamSegDone = baseSegDone + completed, streamSegBytes = baseSegBytes + bytesTotal
                        )
                    }
                    if (now - lastNotifyAt > 800) {
                        lastNotifyAt = now
                        DownloadNotificationHelper.showProgressNotification(context, current)
                    }
                }

                val segments = downloadTrackResilient(
                    track, segmentsDir, current.streamPageUrl, current.userAgent, current.streamHeaders, ::isCancelled,
                    { resolveNow() },
                    initialDownloaded = existing,
                    speedLimiter = ClintDownloadManager.activeSpeedLimiters[current.id],
                    concurrentSegments = current.streamConcurrentSegments
                ) { _, _, segBytes -> onSegmentComplete(segBytes) }

                baseSegDone += totalSegments
                baseSegTotal += totalSegments
                baseSegBytes += bytesTotal

                val extension = track.containerHintExtension
                return finalizeTrack(name, segments, extension) to extension
            }

            val videoTrackFile: File
            val videoExtension: String
            val audioTrackFile: File?

            if (isLiveCapture) {
                publishProgress {
                    it.withClockStarted().copy(
                        status = DownloadStatus.DOWNLOADING, segmentsCompleted = 0, segmentsTotal = 0, bytesDownloaded = 0L,
                        activeElapsedMs = 0L, streamSegDone = 0, streamSegTotal = 0, streamSegBytes = 0L
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
                    publishProgress {
                        it.copy(
                            segmentsCompleted = liveCompleted, bytesDownloaded = liveBytes, speedBytesPerSec = speed,
                            streamSegDone = liveCompleted, streamSegBytes = liveBytes
                        )
                    }
                    if (now - lastNotifyAt > 800) {
                        lastNotifyAt = now
                        DownloadNotificationHelper.showProgressNotification(context, current)
                    }
                }

                videoWorkDir.deleteRecursively()
                audioWorkDir.deleteRecursively()
                File(workDir, "video.done").delete()
                File(workDir, "audio.done").delete()

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
                    val audioDeferred = if (companionAudioUrl != null) {
                        async {
                            recordLiveTrack(
                                format, companionAudioUrl, current.streamPageUrl, current.userAgent, current.streamHeaders,
                                audioWorkDir, TrackKind.AUDIO, null, null,
                                current.streamAudioBandwidth, current.streamAudioRepresentationId,
                                ::isCancelled, ClintDownloadManager.activeSpeedLimiters[current.id],
                                current.streamConcurrentSegments, ::onLiveSegment
                            )
                        }
                    } else null
                    videoDeferred.await() to audioDeferred?.await()
                }

                publishProgress { it.withClockStopped().copy(streamSegTotal = liveCompleted) }
                videoExtension = results.first.second.containerHintExtension
                videoTrackFile = finalizeTrack("video", results.first.first, videoExtension)
                audioTrackFile = results.second?.let { finalizeTrack("audio", it.first, it.second.containerHintExtension) }
            } else {
                val videoStage = runTrackStage(
                    "video", if (isCombination) StreamStage.VIDEO else StreamStage.NONE, videoWorkDir,
                    current.streamVideoUrl, primaryKind, current.streamVideoWidth, current.streamVideoHeight,
                    current.streamVideoBandwidth, current.streamVideoRepresentationId
                )
                videoTrackFile = videoStage.first
                videoExtension = videoStage.second
                audioTrackFile = if (companionAudioUrl != null) {
                    runTrackStage(
                        "audio", StreamStage.AUDIO, audioWorkDir, companionAudioUrl, TrackKind.AUDIO, null, null,
                        current.streamAudioBandwidth, current.streamAudioRepresentationId
                    ).first
                } else null
                publishProgress { it.withClockStopped() }
            }

            val needsMux = audioTrackFile != null
            val outputPlan = planOutput(videoExtension)
            val outExtension = outputPlan.extension
            val convertTsToMp4 = outputPlan.convertTsToMp4
            val convertTsToAac = outputPlan.convertTsToAac

            val mergeInputBytes = videoTrackFile.length() + (audioTrackFile?.length() ?: 0L)
            publishProgress {
                it.copy(
                    status = if (convertTsToMp4 || convertTsToAac) DownloadStatus.CONVERTING else DownloadStatus.MUXING,
                    streamStage = if (needsMux) StreamStage.MERGING else StreamStage.NONE,
                    muxProgress = 0, segmentsCompleted = 0, segmentsTotal = 0, speedBytesPerSec = 0L,
                    bytesDownloaded = mergeInputBytes, totalBytes = mergeInputBytes
                )
            }
            DownloadNotificationHelper.showProgressNotification(context, current)

            val inputFiles = listOfNotNull(videoTrackFile, audioTrackFile)

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
            val finalFilename = assembledFile?.name ?: DownloadFileHelper.uniqueFile(destDir, guessedFinalName).name
            var finalFile = assembledFile ?: File(destDir, finalFilename)

            if (needsMux) {
                val result = try {
                    MediaRemuxer.remux(
                        context,
                        inputFiles.map { it.path },
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
                    TsToAacConverter.convert(videoTrackFile, conversionTarget, { isActive }) { pct -> onTransformProgress(pct) }
                }
                if (!converted) {
                    runCatching { finalFile.delete() }
                    finalFile = DownloadFileHelper.uniqueFile(destDir, "${baseName(finalFilename)}.ts")
                    placeTrack(videoTrackFile, finalFile) { }
                }
            } else if (convertTsToMp4) {
                val conversionTarget = finalFile
                val converted = withContext(Dispatchers.IO) {
                    TsToMp4Converter.convert(videoTrackFile, conversionTarget, { isActive }) { pct -> onTransformProgress(pct) }
                }
                if (!converted) {
                    runCatching { finalFile.delete() }
                    finalFile = DownloadFileHelper.uniqueFile(destDir, "${baseName(finalFilename)}.ts")
                    placeTrack(videoTrackFile, finalFile) { }
                }
            } else if (assembledFile == null) {
                try {
                    placeTrack(videoTrackFile, finalFile) { pct -> onTransformProgress(pct) }
                } catch (e: CancellationException) {
                    runCatching { finalFile.delete() }
                    throw e
                }
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

    private class StreamOutputPlan(val extension: String, val convertTsToMp4: Boolean, val convertTsToAac: Boolean)

    private suspend fun copyFileWithProgress(source: File, target: File, onProgress: (Int) -> Unit) {
        withContext(Dispatchers.IO) {
            val total = source.length().coerceAtLeast(1L)
            var copied = 0L
            source.inputStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        onProgress((copied * 100 / total).toInt())
                    }
                }
            }
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
        concurrentSegments: Int = 12,
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
                    status = DownloadStatus.DOWNLOADING, segmentsCompleted = 0, segmentsTotal = 0, bytesDownloaded = 0L,
                    streamSegDone = 0, streamSegTotal = 0, streamSegBytes = 0L
                )
            }
            DownloadNotificationHelper.showProgressNotification(context, current)

            var lastNotifyAt = 0L
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
