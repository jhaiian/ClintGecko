package com.jhaiian.clint.downloads

import android.app.NotificationManager
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.util.formatFileSize
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.SequenceInputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal object DownloadWorker {

    private const val MIN_PART_BYTES = 512 * 1024L
    private const val SPACE_MARGIN_BYTES = 16L * 1024L * 1024L
    private const val MONITOR_TICK_MS = 200L
    private const val PROGRESS_CHECKPOINT_INTERVAL_MS = 2000L
    private const val PUBLISH_INTERVAL_MS = 250L
    private const val SPEED_WINDOW_MS = 400L
    private const val COPY_BUFFER_BYTES = 256 * 1024
    private const val SAF_COPY_BUFFER_BYTES = 256 * 1024
    private const val MAX_RETRY_DELAY_SEC = 600L
    private const val MAX_PARALLEL_RESTARTS = 2

    internal enum class FailureKind { TRANSIENT, SERVER_REJECTED, DISK_FULL, NEEDS_USER_ACTION }

    private class SafAccessException(message: String) : IOException(message)

    private fun finalizeElapsed(item: DownloadItem): DownloadItem =
        if (item.activeStartedAt > 0L) {
            item.copy(
                activeElapsedMs = item.activeElapsedMs + (System.currentTimeMillis() - item.activeStartedAt),
                activeStartedAt = 0L
            )
        } else item

    internal fun DownloadItem.resetRetriesIfProgressed(startBytes: Long): DownloadItem =
        if (retryAttempt > 0 && bytesDownloaded > startBytes) copy(retryAttempt = 0) else this

    internal fun isCompleteOnDisk(context: Context, item: DownloadItem): Boolean {
        if (item.totalBytes <= 0L || item.bytesDownloaded < item.totalBytes) return false
        val target = DownloadTarget.of(item) ?: return false
        return target.length(context) == item.totalBytes
    }

    internal fun isCompleteInSafTemp(context: Context, item: DownloadItem): Boolean =
        DownloadFileHelper.isSafCustomMode(context, item) && DownloadFileHelper.isInTempDir(context, item.file) && isCompleteOnDisk(context, item)

    private suspend fun finishDownloaded(context: Context, downloaded: DownloadItem, safMode: Boolean) {
        if (DownloadTarget.of(downloaded) is DownloadTarget.Document) {
            finishDocument(context, downloaded)
            return
        }
        if (safMode) {
            moveTempToSaf(context, downloaded)
            return
        }
        var item = finalizeElapsed(downloaded)
        val partFile = item.file?.takeIf { DownloadFileHelper.isPartFile(it) }
        if (partFile != null) {
            val promoted = withContext(Dispatchers.IO) { DownloadFileHelper.promotePartFile(partFile) }
            if (promoted != null) {
                item = item.copy(file = promoted, filename = promoted.name)
            } else {
                DownloadLog.w(item.id, "FILE") { "Could not rename ${partFile.name}; keeping the finished file under its temporary name" }
            }
        }
        item = item.copy(completedAt = System.currentTimeMillis(), status = DownloadStatus.COMPLETE)
        ClintDownloadManager.persistDownload(item)
        ClintDownloadManager.publish(item)
        DownloadNotificationHelper.showCompleteNotification(context, item)
        ClintDownloadManager.tryDequeueNext(context)
        scanCompletedFile(context, item)
    }

    private suspend fun finishDocument(context: Context, downloaded: DownloadItem) {
        var item = finalizeElapsed(downloaded)
        val partUri = item.contentUri?.let(Uri::parse)
        if (partUri != null) {
            val desiredName = item.filename
            val promoted = withContext(Dispatchers.IO) { SafDirectWriter.promote(context, partUri, desiredName) }
            if (promoted != null) {
                val name = withContext(Dispatchers.IO) { SafDirectWriter.displayName(context, promoted) }
                item = item.copy(contentUri = promoted.toString(), filename = name ?: desiredName)
            } else {
                DownloadLog.w(item.id, "FILE") { "Could not rename the finished file in the chosen folder; it keeps its temporary name" }
            }
        }
        item = item.copy(completedAt = System.currentTimeMillis(), status = DownloadStatus.COMPLETE, copyProgress = 100)
        ClintDownloadManager.persistDownload(item)
        ClintDownloadManager.publish(item)
        DownloadNotificationHelper.showCompleteNotification(context, item)
        ClintDownloadManager.tryDequeueNext(context)
        scanCompletedFile(context, item)
    }

    private suspend fun restartFromScratch(context: Context, item: DownloadItem) {
        withContext(Dispatchers.IO) { DownloadTarget.of(item)?.delete(context) }
        ClintDownloadManager.requestNextAttempt(
            finalizeElapsed(item).copy(
                bytesDownloaded = 0L, file = null, contentUri = null, resumable = false, partMap = "", completedPartsMask = 0L,
                partOffsets = "", partProgress = emptyList(), etag = null, lastModified = null, speedBytesPerSec = 0L
            )
        )
    }

    private fun contentRangeMatches(header: String?, start: Long, totalBytes: Long): Boolean {
        val range = ContentRangeInfo.parse(header) ?: return true
        if (range.start != start) return false
        val total = range.total ?: return true
        return totalBytes <= 0L || total == totalBytes
    }

    internal fun httpFailureKind(code: Int): FailureKind = when (code) {
        408, 425, 429 -> FailureKind.TRANSIENT
        in 400..499 -> FailureKind.SERVER_REJECTED
        else -> FailureKind.TRANSIENT
    }

    private fun safFailureKind(e: Throwable): FailureKind = when {
        e is SafAccessException || e is SecurityException -> FailureKind.NEEDS_USER_ACTION
        isDiskFull(e.message.orEmpty()) -> FailureKind.DISK_FULL
        else -> FailureKind.TRANSIENT
    }

    private fun backoffSeconds(baseSeconds: Long, attempt: Int): Long {
        val base = baseSeconds.coerceAtLeast(1L)
        val exponential = base shl (attempt - 1).coerceIn(0, 16)
        val capped = maxOf(base, minOf(MAX_RETRY_DELAY_SEC, exponential))
        return capped + kotlin.random.Random.nextLong(0L, capped / 5L + 1L)
    }

    suspend fun run(context: Context, initialItem: DownloadItem) {
        var item = initialItem

        if (item.unmeteredOnly && !DownloadNetworkMonitor.isNetworkUnmetered(context)) {
            DownloadNetworkMonitor.unmeteredPausedIds.add(item.id)
            item = item.copy(status = DownloadStatus.PAUSED, waitingForUnmetered = true, speedBytesPerSec = 0L)
            ClintDownloadManager.persistDownload(item)
            ClintDownloadManager.publish(item)
            context.getSystemService(NotificationManager::class.java).cancel(item.id)
            DownloadNotificationHelper.showWaitingUnmeteredNotification(context, item)
            return
        }

        val directCustomDir = DownloadFileHelper.resolveDirectCustomDir(context, item)

        val safMode = DownloadFileHelper.isSafCustomMode(context, item) &&
            (directCustomDir == null || DownloadFileHelper.isInTempDir(context, item.file))
        val speedLimiter = ClintDownloadManager.activeSpeedLimiters.getOrPut(item.id) {
            SpeedLimiter(item.speedLimitBytesPerSec)
        }

        if (withContext(Dispatchers.IO) { isCompleteOnDisk(context, item) }) {
            DownloadLog.i(item.id, "RESUME") { "All ${DownloadLog.formatBytes(item.totalBytes)} are already on disk; finishing without downloading again" }
            finishDownloaded(context, item, safMode)
            return
        }

        val existingTarget = DownloadTarget.of(item)
        val targetExists = existingTarget != null && withContext(Dispatchers.IO) { existingTarget.exists(context) }

        val resumeEffectiveParts = if (
            item.resumable && !item.parallelRateLimited && item.totalBytes > 0L &&
            targetExists && item.bytesDownloaded < item.totalBytes
        ) {
            DownloadAutoTuner.plannedParts(context, item, item.url.toHttpUrlOrNull()?.host.orEmpty(), MIN_PART_BYTES)
        } else 1

        val isResumingFile = item.bytesDownloaded > 0L && targetExists

        DownloadLog.section(item.id, if (isResumingFile) "Resuming download attempt" else "Starting download attempt") {
            listOf(
                "File" to item.filename,
                "URL" to item.url,
                "Mode" to if (resumeEffectiveParts > 1) "parallel resume, $resumeEffectiveParts parts" else "probe then decide",
                "Storage" to when {
                    existingTarget is DownloadTarget.Document -> "written directly to the chosen folder"
                    safMode -> "chosen folder, written directly when possible"
                    else -> "written in place"
                },
                "Resume from" to if (isResumingFile) DownloadLog.formatBytes(item.bytesDownloaded) else null,
                "Finished parts" to if (item.completedPartsMask != 0L) java.lang.Long.bitCount(item.completedPartsMask) else null,
                "Retry attempt" to item.retryAttempt.takeIf { it > 0 },
                "Rate limited before" to item.parallelRateLimited.takeIf { it }
            )
        }

        var transferredBytes = -1L

        try {
            if (resumeEffectiveParts > 1) {
                item = item.copy(status = DownloadStatus.DOWNLOADING, activeStartedAt = System.currentTimeMillis())
                ClintDownloadManager.persistDownload(item)
                ClintDownloadManager.publish(item)
                runParallelDownload(
                    context, item, checkNotNull(existingTarget), resumeEffectiveParts, item.multithreadingParts, safMode,
                    speedLimiter = speedLimiter,
                    resumeFromMask = item.completedPartsMask
                )
                return
            }

            item = item.copy(status = DownloadStatus.CONNECTING, speedBytesPerSec = 0L)
            ClintDownloadManager.persistDownload(item)
            ClintDownloadManager.publish(item)
            DownloadNotificationHelper.showProgressNotification(context, item)

            val request = Request.Builder()
                .url(item.url)
                .tag(ClintDownloadManager.CallTag::class.java, ClintDownloadManager.CallTag(item.id))
                .header("User-Agent", item.userAgent)
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Accept-Encoding", "identity")
                .apply {
                    if (item.referer.isNotEmpty()) header("Referer", item.referer)
                    if (item.cookies.isNotEmpty()) header("Cookie", item.cookies)
                    if (isResumingFile) {
                        header("Range", "bytes=${item.bytesDownloaded}-")
                        (item.etag?.takeIf { it.isNotBlank() && !it.startsWith("W/") }
                            ?: item.lastModified?.takeIf { it.isNotBlank() && item.restarts == 0 })?.let { header("If-Range", it) }
                    }
                }
                .build()

            if (ClintDownloadManager.isRemoved(item.id)) throw CancellationException("removed")

            DownloadLog.i(item.id, "HTTP") { "GET ${item.url}" + if (isResumingFile) " \u00B7 Range bytes=${item.bytesDownloaded}-" else "" }
            val requestStartedAt = System.currentTimeMillis()
            val response = withContext(DownloadDispatchers.network) {
                runInterruptible { ClintDownloadManager.httpClient.newCall(request).execute() }
            }
            DownloadLog.i(item.id, "HTTP") {
                buildString {
                    append("Response ${response.code} ${response.message} \u00B7 ${response.protocol} \u00B7 ${System.currentTimeMillis() - requestStartedAt} ms")
                    if (response.request.url.toString() != item.url) append("\nFinal URL: ${response.request.url}")
                    listOf("Content-Length", "Content-Range", "Content-Type", "Accept-Ranges", "Content-Disposition", "ETag", "Last-Modified", "Server")
                        .forEach { name -> response.header(name)?.let { append("\n$name: $it") } }
                }
            }

            if (ClintDownloadManager.isRemoved(item.id)) {
                response.close()
                throw CancellationException("removed")
            }

            if (ClintDownloadManager.pauseRequested.remove(item.id)) {
                response.close()
                handlePauseTransition(context, item)
                return
            }

            if (!response.isSuccessful) {
                if (response.code == 416 && isResumingFile) {
                    val remoteTotal = ContentRangeInfo.parseUnsatisfiedTotal(response.header("Content-Range"))
                    response.close()
                    val localLength = withContext(Dispatchers.IO) { existingTarget.length(context) }
                    if (remoteTotal != null && remoteTotal > 0L && localLength == remoteTotal &&
                        (item.totalBytes <= 0L || item.totalBytes == remoteTotal) && item.bytesDownloaded >= remoteTotal
                    ) {
                        DownloadLog.i(item.id, "RESUME") { "Server reports the file is already complete (${DownloadLog.formatBytes(remoteTotal)})" }
                        finishDownloaded(context, item.copy(totalBytes = remoteTotal, bytesDownloaded = remoteTotal), safMode)
                    } else {
                        DownloadLog.w(item.id, "RESUME") { "Server rejected the resume position (HTTP 416); restarting from the beginning" }
                        restartFromScratch(context, item)
                    }
                    return
                }
                DownloadLog.e(item.id, "HTTP") { "Server refused the request with ${response.code} ${response.message}" }
                response.close()
                fail(context, item, "Server error ${response.code}", kind = httpFailureKind(response.code))
                return
            }

            val serverAcceptedRange = response.code == 206

            if (isResumingFile && serverAcceptedRange &&
                !contentRangeMatches(response.header("Content-Range"), item.bytesDownloaded, item.totalBytes)
            ) {
                DownloadLog.w(item.id, "RESUME") {
                    "Server answered with a different range or file size (${response.header("Content-Range")}); restarting from the beginning"
                }
                response.close()
                restartFromScratch(context, item)
                return
            }

            if (isResumingFile && !serverAcceptedRange) {
                val contentType = response.header("Content-Type")?.substringBefore(";")?.trim()?.lowercase()
                val looksLikeErrorPage = contentType == "text/html" || contentType == "text/plain" || contentType == "application/json"
                if (looksLikeErrorPage) {
                    DownloadLog.e(item.id, "RESUME") { "Server ignored the range request and returned $contentType; the link has probably expired" }
                    response.close()
                    fail(context, item, context.getString(R.string.download_error_link_expired))
                    return
                }
                DownloadLog.w(item.id, "RESUME") { "Server ignored the range request (HTTP ${response.code}); restarting from the beginning" }
                item = item.copy(bytesDownloaded = 0L, resumable = false, file = null, contentUri = null)
                withContext(Dispatchers.IO) { existingTarget.delete(context) }
            }

            val effectiveResume = isResumingFile && serverAcceptedRange
            val body = response.body
            val rawStream = body.byteStream()
            var peekedBytes: ByteArray? = null

            var writeStartPos = item.bytesDownloaded
            var outputTarget: DownloadTarget

            if (effectiveResume) {
                outputTarget = checkNotNull(existingTarget)
            } else {
                item = item.copy(
                    resumable = response.header("Accept-Ranges") == "bytes",
                    totalBytes = response.header("Content-Length")?.toLongOrNull() ?: -1L,
                    bytesDownloaded = 0L,
                    etag = response.header("ETag"),
                    lastModified = response.header("Last-Modified"),
                    partMap = "",
                    completedPartsMask = 0L,
                    partOffsets = ""
                )
                writeStartPos = 0L
                ClintDownloadManager.publish(item)

                val peeked = withContext(DownloadDispatchers.network) {
                    runInterruptible { DownloadFilenameResolver.readHead(rawStream) }
                }
                peekedBytes = peeked

                val finalFilename = DownloadFilenameResolver.refine(
                    item.filename,
                    response.header("Content-Type"),
                    peeked
                )
                item = item.copy(filename = finalFilename)

                val planned = item
                val (createdTarget, createdName) = withContext(Dispatchers.IO) {
                    val existing = DownloadTarget.of(planned)
                    if (existing != null && planned.totalBytes > 0 && existing.length(context) == planned.totalBytes) {
                        existing to when (existing) {
                            is DownloadTarget.Local -> DownloadFileHelper.finalNameOf(existing.file)
                            is DownloadTarget.Document -> initialItem.filename
                        }
                    } else {
                        if (existing != null) runCatching { existing.delete(context) }
                        val document = if (safMode) SafDirectWriter.createPart(context, planned, finalFilename) else null
                        if (document != null) {
                            DownloadTarget.Document(document.uri) to document.finalName
                        } else {
                            val rawDestDir = directCustomDir
                                ?: if (safMode) DownloadFileHelper.tempDownloadDir(context) else DownloadFileHelper.resolveDownloadDir()
                            val destDir = if (safMode) rawDestDir else DownloadCategories.resolveDir(planned.categorizeEnabled, rawDestDir, finalFilename)
                            destDir.mkdirs()
                            val file = if (safMode) DownloadFileHelper.uniqueFile(destDir, finalFilename)
                            else DownloadFileHelper.uniquePartFile(destDir, finalFilename)
                            DownloadTarget.Local(file) to DownloadFileHelper.finalNameOf(file)
                        }
                    }
                }
                outputTarget = createdTarget
                item = when (createdTarget) {
                    is DownloadTarget.Local -> item.copy(filename = createdName, file = createdTarget.file, contentUri = null)
                    is DownloadTarget.Document -> item.copy(filename = createdName, file = null, contentUri = createdTarget.uri.toString())
                }
                DownloadLog.i(item.id, "FILE") {
                    "Saving to ${createdTarget.location} \u00B7 size ${DownloadLog.formatBytes(item.totalBytes)} \u00B7 resumable ${item.resumable}"
                }

                if (item.totalBytes > 0) {
                    val free = withContext(Dispatchers.IO) { createdTarget.availableBytes(context) }
                    if (free >= 0L && free < item.totalBytes + SPACE_MARGIN_BYTES) {
                        DownloadLog.e(item.id, "STORAGE") { "Not enough space: needs ${DownloadLog.formatBytes(item.totalBytes)}, ${DownloadLog.formatBytes(free)} free" }
                        response.close()
                        fail(
                            context, item,
                            context.getString(R.string.download_error_not_enough_space, formatFileSize(item.totalBytes), formatFileSize(free)),
                            scheduleRetry = false
                        )
                        return
                    }
                    val allocated = preAllocateFile(context, item, createdTarget)
                    if (allocated == null) return
                    item = allocated

                    if (ClintDownloadManager.pauseRequested.remove(item.id)) {
                        handlePauseTransition(context, item)
                        return
                    }

                    val effectiveParts = DownloadAutoTuner.plannedParts(context, item, item.url.toHttpUrlOrNull()?.host.orEmpty(), MIN_PART_BYTES)
                    if (item.resumable && effectiveParts > 1 && !item.parallelRateLimited) {
                        DownloadLog.i(item.id, "PARTS") { "Server supports ranges; switching to $effectiveParts parts with ${if (DownloadAutoTuner.isAuto(item.multithreadingParts)) "auto" else item.multithreadingParts} connections" }
                        response.close()
                        item = item.copy(status = DownloadStatus.DOWNLOADING, activeStartedAt = System.currentTimeMillis())
                        ClintDownloadManager.persistDownload(item)
                        ClintDownloadManager.publish(item)
                        runParallelDownload(context, item, createdTarget, effectiveParts, item.multithreadingParts, safMode, speedLimiter = speedLimiter)
                        return
                    }
                }
            }

            val inputStream = if (effectiveResume) {
                rawStream
            } else {
                SequenceInputStream(java.io.ByteArrayInputStream(peekedBytes ?: ByteArray(0)), rawStream)
            }

            item = item.copy(status = DownloadStatus.DOWNLOADING, activeStartedAt = System.currentTimeMillis())
            ClintDownloadManager.persistDownload(item)
            ClintDownloadManager.publish(item)
            DownloadLog.i(item.id, "TRANSFER") {
                "Single connection transfer from byte $writeStartPos" + when {
                    !item.resumable -> " \u00B7 server does not support ranges"
                    item.parallelRateLimited -> " \u00B7 parallel mode disabled after rate limiting"
                    item.totalBytes <= 0L -> " \u00B7 size unknown"
                    else -> ""
                }
            }

            val copyResult = copyToFile(context, item, inputStream, outputTarget, writeStartPos, speedLimiter) { transferredBytes = it }
            item = copyResult.item

            if (!copyResult.completed) {
                handlePauseTransition(context, item)
                return
            }

            if (item.totalBytes > 0L && item.bytesDownloaded != item.totalBytes) {
                DownloadLog.e(item.id, "VERIFY") { "Size check failed: received ${item.bytesDownloaded} of ${item.totalBytes} bytes" }
                fail(
                    context, item.resetRetriesIfProgressed(initialItem.bytesDownloaded),
                    context.getString(R.string.download_error_size_mismatch, formatFileSize(item.bytesDownloaded), formatFileSize(item.totalBytes))
                )
                return
            }
            DownloadLog.i(item.id, "VERIFY") { "Size check passed: ${DownloadLog.formatBytes(item.bytesDownloaded)}" }
            if (item.totalBytes <= 0L) item = item.copy(totalBytes = item.bytesDownloaded)

            finishDownloaded(context, item, safMode)
        } catch (e: Throwable) {
            val failedItem = if (transferredBytes > item.bytesDownloaded) {
                item.copy(bytesDownloaded = transferredBytes, speedBytesPerSec = 0L)
            } else item
            handleAttemptFailure(context, failedItem.resetRetriesIfProgressed(initialItem.bytesDownloaded), e)
            if (e is CancellationException) throw e
        }
    }

    private suspend fun handlePauseTransition(context: Context, item: DownloadItem) {
        var paused = finalizeElapsed(item)
        val waitingForSchedule = paused.id in DownloadScheduleMonitor.scheduleWaitingIds
        val waitingForUnmetered = !waitingForSchedule && paused.id in DownloadNetworkMonitor.unmeteredPausedIds
        paused = paused.copy(
            status = DownloadStatus.PAUSED, speedBytesPerSec = 0L,
            waitingForSchedule = waitingForSchedule, waitingForUnmetered = waitingForUnmetered
        )
        ClintDownloadManager.persistDownload(paused)
        ClintDownloadManager.publish(paused)
        context.getSystemService(NotificationManager::class.java).cancel(paused.id)
        if (waitingForSchedule) {
            DownloadNotificationHelper.showWaitingScheduleNotification(context, paused)
        } else if (waitingForUnmetered) {
            DownloadNotificationHelper.showWaitingUnmeteredNotification(context, paused)
        } else {
            DownloadNotificationHelper.showPausedNotification(context, paused)
        }
        ClintDownloadManager.tryDequeueNext(context)
    }

    private suspend fun handleAttemptFailure(context: Context, item: DownloadItem, e: Throwable) {
        if (e is CancellationException) {
            DownloadLog.i(item.id, "CANCEL") { "Attempt cancelled (${e.message ?: "no reason"})" }
        } else {
            DownloadLog.error(item.id, "ERROR", "Attempt failed: ${e.javaClass.simpleName}: ${e.message}", e)
        }
        suspend fun cleanup() {
            if (item.status != DownloadStatus.PAUSED) {
                if (e is CancellationException) {
                    if (ClintDownloadManager.isRemoved(item.id)) {
                        withContext(Dispatchers.IO) { DownloadTarget.of(item)?.delete(context) }
                    }
                } else {
                    fail(context, item, e.message ?: context.getString(R.string.download_error_unknown))
                }
            }
        }
        if (e is CancellationException) {
            withContext(NonCancellable) { cleanup() }
        } else {
            cleanup()
        }
    }

    private class CopyResult(val item: DownloadItem, val completed: Boolean)

    private suspend fun copyToFile(
        context: Context,
        item: DownloadItem,
        inputStream: java.io.InputStream,
        target: DownloadTarget,
        writeStartPos: Long,
        speedLimiter: SpeedLimiter,
        onProgress: (Long) -> Unit
    ): CopyResult {
        val id = item.id
        var downloaded = item.bytesDownloaded
        var speed = item.speedBytesPerSec
        var reachedEof = false
        var lastPublishAt = 0L
        var lastSpeedBytes = downloaded
        var lastSpeedTime = System.currentTimeMillis()
        var lastCheckpointTime = System.currentTimeMillis()

        fun snapshot(): DownloadItem = item.copy(bytesDownloaded = downloaded, speedBytesPerSec = speed)

        coroutineScope {
            val pauseWatcher = launch(Dispatchers.IO) {
                while (isActive) {
                    if (ClintDownloadManager.pauseRequested.contains(id)) {
                        runCatching { inputStream.close() }
                        break
                    }
                    delay(MONITOR_TICK_MS)
                }
            }
            try {
                withContext(DownloadDispatchers.network) {
                    runInterruptible {
                        target.openChannel(context).use { channel ->
                            var position = writeStartPos
                            inputStream.use { input ->
                                val reader = BatchedReader(input, ByteArray(COPY_BUFFER_BYTES))
                                while (true) {
                                    if (ClintDownloadManager.isRemoved(id)) throw CancellationException("removed")
                                    if (ClintDownloadManager.pauseRequested.contains(id)) break
                                    val batch = minOf(
                                        speedLimiter.batchBytes(COPY_BUFFER_BYTES),
                                        ClintDownloadManager.globalSpeedLimiter.batchBytes(COPY_BUFFER_BYTES)
                                    )
                                    val read = try {
                                        reader.fill(batch)
                                    } catch (e: IOException) {
                                        if (ClintDownloadManager.isRemoved(id)) throw CancellationException("removed")
                                        if (ClintDownloadManager.pauseRequested.contains(id)) break else throw e
                                    }
                                    if (read == -1) {
                                        if (ClintDownloadManager.pauseRequested.contains(id)) break
                                        reachedEof = true
                                        break
                                    }
                                    val chunk = ByteBuffer.wrap(reader.buffer, 0, read)
                                    while (chunk.hasRemaining()) position += channel.write(chunk, position)
                                    downloaded += read
                                    onProgress(downloaded)
                                    ClintDownloadManager.noteActivity(id)
                                    speedLimiter.acquire(read)
                                    ClintDownloadManager.globalSpeedLimiter.acquire(read)
                                    val now = System.currentTimeMillis()
                                    val elapsed = now - lastSpeedTime
                                    if (elapsed >= SPEED_WINDOW_MS) {
                                        speed = (downloaded - lastSpeedBytes) * 1000L / elapsed
                                        lastSpeedBytes = downloaded
                                        lastSpeedTime = now
                                    }
                                    if (now - lastPublishAt >= PUBLISH_INTERVAL_MS) {
                                        lastPublishAt = now
                                        val current = snapshot()
                                        DownloadNotificationHelper.showProgressNotificationThrottled(context, current)
                                        ClintDownloadManager.publish(current)
                                    }
                                    if (now - lastCheckpointTime >= PROGRESS_CHECKPOINT_INTERVAL_MS) {
                                        lastCheckpointTime = now
                                        runCatching { channel.force(false) }
                                        ClintDownloadManager.checkpointProgress(
                                            id, downloaded, 0L, "", generation = item.generation
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } finally {
                pauseWatcher.cancel()
            }
        }
        return CopyResult(snapshot(), reachedEof)
    }

    internal suspend fun moveTempToSaf(context: Context, initialItem: DownloadItem) {
        var item = initialItem
        val treeUri = DownloadFileHelper.getSafTreeUri(context, item)
        val tempFile = item.file

        if (treeUri == null || tempFile == null) {
            item = finalizeElapsed(item)
            item = item.copy(completedAt = System.currentTimeMillis(), status = DownloadStatus.COMPLETE)
            ClintDownloadManager.persistDownload(item)
            ClintDownloadManager.publish(item)
            DownloadNotificationHelper.showCompleteNotification(context, item)
            ClintDownloadManager.tryDequeueNext(context)
            return
        }

        item = item.copy(status = DownloadStatus.COPYING_TEMP, copyProgress = 0)
        ClintDownloadManager.persistDownload(item)
        ClintDownloadManager.publish(item)
        DownloadNotificationHelper.showCopyingTempNotification(context, item)

        var createdDoc: DocumentFile? = null
        var copied = false
        try {
            var lastNotifiedProgress = 0
            item = withContext(Dispatchers.IO) {
                runInterruptible {
                    val docDir = DocumentFile.fromTreeUri(context, treeUri)
                        ?: throw SafAccessException(context.getString(R.string.download_error_saf_no_access))
                    if (!docDir.canWrite()) throw SafAccessException(context.getString(R.string.download_error_saf_not_writable))

                    val targetDir = DownloadCategories.resolveSafDir(item.categorizeEnabled, docDir, item.filename)
                    val finalName = DownloadFileHelper.uniqueSafName(targetDir, item.filename)
                    val docFile = targetDir.createFile("application/octet-stream", finalName)
                        ?: throw SafAccessException(context.getString(R.string.download_error_saf_create_failed))
                    createdDoc = docFile

                    var working = item.copy(filename = docFile.name ?: item.filename)

                    val totalBytes = tempFile.length()
                    var bytesCopied = 0L
                    val buffer = ByteArray(SAF_COPY_BUFFER_BYTES)

                    val output = context.contentResolver.openOutputStream(docFile.uri)
                        ?: throw SafAccessException(context.getString(R.string.download_error_saf_no_stream))
                    output.use { out ->
                        tempFile.inputStream().use { input ->
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                out.write(buffer, 0, read)
                                bytesCopied += read
                                if (totalBytes > 0) {
                                    val newProgress = ((bytesCopied * 100) / totalBytes).toInt()
                                    if (newProgress != working.copyProgress) {
                                        working = working.copy(copyProgress = newProgress)
                                        DownloadNotificationHelper.showCopyingTempNotificationThrottled(context, working)
                                        ClintDownloadManager.publish(working)
                                        lastNotifiedProgress = newProgress
                                    }
                                }
                            }
                        }
                    }

                    working.copy(file = null, contentUri = docFile.uri.toString(), copyProgress = 100)
                }
            }
            copied = true

            item = item.copy(status = DownloadStatus.DELETING_TEMP)
            ClintDownloadManager.persistDownload(item)
            ClintDownloadManager.publish(item)
            DownloadNotificationHelper.showDeletingTempNotification(context, item)
            withContext(Dispatchers.IO) { tempFile.delete() }

            item = finalizeElapsed(item)
            item = item.copy(completedAt = System.currentTimeMillis(), status = DownloadStatus.COMPLETE)
            ClintDownloadManager.persistDownload(item)
            ClintDownloadManager.publish(item)
            DownloadNotificationHelper.showCompleteNotification(context, item)
            ClintDownloadManager.tryDequeueNext(context)
            scanCompletedFile(context, item)
        } catch (e: Throwable) {
            if (copied) {
                runCatching { tempFile.delete() }
            } else {
                createdDoc?.let { doc -> runCatching { doc.delete() } }
            }
            if (e is CancellationException) {
                if (ClintDownloadManager.isRemoved(initialItem.id)) runCatching { tempFile.delete() }
                throw e
            }
            if (item.status != DownloadStatus.PAUSED) {
                DownloadLog.e(item.id, "SAF") {
                    "Copying to the chosen folder failed (${e.javaClass.simpleName}: ${e.message})" +
                        if (copied) "" else "; the downloaded file is kept so only the copy is retried"
                }
                fail(
                    context, item, e.message ?: context.getString(R.string.download_error_unknown),
                    kind = if (copied) null else safFailureKind(e)
                )
            }
        }
    }

    private suspend fun preAllocateFile(context: Context, initialItem: DownloadItem, target: DownloadTarget): DownloadItem? {
        var item = initialItem.copy(status = DownloadStatus.ALLOCATING, allocationProgress = 0)
        ClintDownloadManager.persistDownload(item)
        ClintDownloadManager.publish(item)
        DownloadNotificationHelper.showAllocationNotification(context, item)

        if (item.id in ClintDownloadManager.removedIds) {
            withContext(Dispatchers.IO) { target.delete(context) }
            ClintDownloadManager.persistDownload(item)
            ClintDownloadManager.publish(item)
            return null
        }

        return try {
            val length = item.totalBytes
            withContext(Dispatchers.IO) {
                runInterruptible { target.setLength(context, length) }
            }
            item = item.copy(allocationProgress = 100)
            ClintDownloadManager.publish(item)
            DownloadNotificationHelper.showAllocationNotification(context, item)
            item
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            withContext(Dispatchers.IO) { target.delete(context) }
            fail(context, item, e.message ?: context.getString(R.string.download_error_unknown), scheduleRetry = false)
            null
        }
    }

    private suspend fun runParallelDownload(
        context: Context,
        initialItem: DownloadItem,
        target: DownloadTarget,
        totalParts: Int,
        simultaneousParts: Int,
        safMode: Boolean,
        speedLimiter: SpeedLimiter,
        resumeFromMask: Long = 0L
    ) {
        val seeded = if (resumeFromMask != 0L && initialItem.completedPartsMask == 0L) initialItem.copy(completedPartsMask = resumeFromMask) else initialItem
        when (val outcome = ParallelDownloader.download(context, seeded, target, totalParts, simultaneousParts, speedLimiter)) {
            is ParallelOutcome.Completed -> finishDownloaded(context, outcome.item, safMode)
            is ParallelOutcome.Paused -> handlePauseTransition(context, outcome.item)
            is ParallelOutcome.FallbackSingle -> {
                var item = outcome.item.copy(parallelRateLimited = true, speedBytesPerSec = 0L)
                item = finalizeElapsed(item)
                ClintDownloadManager.persistDownload(item)
                ClintDownloadManager.publish(item)
                if (outcome.waitMs > 0L) delay(outcome.waitMs)
                ClintDownloadManager.requestNextAttempt(item)
            }
            is ParallelOutcome.Restart -> {
                val restarts = outcome.item.restarts + 1
                val singleConnection = restarts >= MAX_PARALLEL_RESTARTS
                DownloadLog.w(outcome.item.id, "RESUME") {
                    "Restarting from the beginning: ${outcome.reason}" + when {
                        singleConnection -> " · repeated restarts, continuing with one connection"
                        else -> " · date validators are ignored from now on"
                    }
                }
                withContext(Dispatchers.IO) { target.delete(context) }
                val item = finalizeElapsed(outcome.item).copy(
                    bytesDownloaded = 0L, partMap = "", completedPartsMask = 0L, partOffsets = "",
                    partProgress = emptyList(), etag = null, lastModified = null, speedBytesPerSec = 0L,
                    restarts = restarts, parallelRateLimited = outcome.item.parallelRateLimited || singleConnection
                )
                ClintDownloadManager.persistDownload(item)
                ClintDownloadManager.publish(item)
                ClintDownloadManager.requestNextAttempt(item)
            }
            is ParallelOutcome.Failed -> fail(
                context,
                finalizeElapsed(outcome.item).resetRetriesIfProgressed(initialItem.bytesDownloaded),
                outcome.message
            )
        }
    }

    private fun isServerError(msg: String): Boolean {
        val match = Regex("Server error (\\d+)").find(msg) ?: return false
        val code = match.groupValues[1].toIntOrNull() ?: return false
        return code in 400..499 && code != 429
    }

    private fun isDiskFull(msg: String): Boolean = msg.contains("No space left on device", ignoreCase = true)

    private fun classify(msg: String): FailureKind = when {
        isServerError(msg) -> FailureKind.SERVER_REJECTED
        isDiskFull(msg) -> FailureKind.DISK_FULL
        else -> FailureKind.TRANSIENT
    }

    suspend fun fail(
        context: Context,
        initialItem: DownloadItem,
        msg: String,
        scheduleRetry: Boolean = true,
        kind: FailureKind? = null
    ) {
        currentCoroutineContext().ensureActive()
        if (ClintDownloadManager.isStale(initialItem)) return
        val ctx = ClintDownloadManager.appContext ?: context
        var item = initialItem
        val failureKind = kind ?: classify(msg)

        if (scheduleRetry &&
            failureKind == FailureKind.TRANSIENT &&
            item.generation != 0L &&
            item.id !in ClintDownloadManager.removedIds &&
            !DownloadNetworkMonitor.isNetworkAvailable(ctx)
        ) {
            DownloadLog.w(item.id, "NETWORK") { "No network after error \"$msg\"; waiting for a connection" }
            item = finalizeElapsed(item)
            item = item.copy(
                status = DownloadStatus.PAUSED, waitingForNetwork = true, speedBytesPerSec = 0L,
                retryAttempt = 0, retryDelaySec = 0, errorMessage = null
            )
            DownloadNetworkMonitor.networkWaitingIds.add(item.id)
            context.getSystemService(NotificationManager::class.java).cancel(item.id)
            DownloadNotificationHelper.showWaitingNetworkNotification(ctx, item)
            ClintDownloadManager.persistDownload(item)
            ClintDownloadManager.publish(item)
            ClintDownloadManager.tryDequeueNext(ctx)
            if (DownloadNetworkMonitor.isNetworkAvailable(ctx) && DownloadNetworkMonitor.networkWaitingIds.remove(item.id)) {
                ClintDownloadManager.resume(ctx, item.id)
            }
            return
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        val retryEnabled = ClintDownloadManager.withLiveSettings(item).retryEnabled
        val retryUnrecoverable = prefs.getBoolean(
            DownloadSettingsKeys.PREF_RETRY_UNRECOVERABLE, DownloadSettingsKeys.DEFAULT_RETRY_UNRECOVERABLE
        )
        val retryCount = prefs.getInt(
            DownloadSettingsKeys.PREF_RETRY_COUNT, DownloadSettingsKeys.DEFAULT_RETRY_COUNT
        )
        val retryInterval = prefs.getInt(
            DownloadSettingsKeys.PREF_RETRY_INTERVAL, DownloadSettingsKeys.DEFAULT_RETRY_INTERVAL
        ).toLong()

        val serverError = failureKind == FailureKind.SERVER_REJECTED
        val diskFull = failureKind == FailureKind.DISK_FULL
        val needsUserAction = failureKind == FailureKind.NEEDS_USER_ACTION
        val displayMsg = if (diskFull) context.getString(R.string.download_error_disk_full) else msg
        val canRetry = scheduleRetry &&
            !needsUserAction &&
            item.generation != 0L &&
            retryEnabled &&
            (retryUnrecoverable || (!serverError && !diskFull)) &&
            item.id !in ClintDownloadManager.removedIds &&
            (retryCount == 0 || item.retryAttempt < retryCount)
        val delaySeconds = if (canRetry) backoffSeconds(retryInterval, item.retryAttempt + 1) else 0L

        DownloadLog.i(item.id, "RETRY") {
            "Error \"$msg\" \u00B7 ${failureKind.name.lowercase()} \u00B7 retry enabled $retryEnabled \u00B7 " +
                "attempt ${item.retryAttempt + 1}/${if (retryCount == 0) "\u221E" else retryCount} \u00B7 " +
                if (canRetry) "retrying in ${delaySeconds}s" else "giving up"
        }
        if (canRetry) {
            item = item.copy(
                retryAttempt = item.retryAttempt + 1,
                status = DownloadStatus.RETRYING,
                retryDelaySec = delaySeconds.toInt(),
                lastErrorWasServerError = serverError,
                errorMessage = displayMsg,
                speedBytesPerSec = 0L
            )
            ClintDownloadManager.persistDownload(item)
            ClintDownloadManager.publish(item)
            if (item.retryAttempt == 1) {
                DownloadNotificationHelper.showRetryingNotification(ctx, item)
            }
            delay(delaySeconds * 1000L)
            if (item.id in ClintDownloadManager.removedIds || ClintDownloadManager.isStale(item)) {

            } else if (ClintDownloadManager.pauseRequested.remove(item.id)) {
                item = item.copy(retryDelaySec = 0)
                handlePauseTransition(context, item)
            } else {
                item = ClintDownloadManager.withLiveSettings(item.copy(retryDelaySec = 0))
                ClintDownloadManager.publish(item)
                ClintDownloadManager.requestNextAttempt(item)
            }
        } else if (item.id !in ClintDownloadManager.removedIds) {
            item = item.copy(
                retryAttempt = 0, status = DownloadStatus.FAILED,
                lastErrorWasServerError = serverError, errorMessage = displayMsg
            )
            ClintDownloadManager.persistDownload(item)
            ClintDownloadManager.publish(item)
            DownloadNotificationHelper.showFailedNotification(context, item)
            ClintDownloadManager.tryDequeueNext(context)
        }
    }

    private fun scanCompletedFile(context: Context, item: DownloadItem) {
        val path = item.file?.absolutePath ?: run {
            val uriStr = item.contentUri ?: return
            val uri = android.net.Uri.parse(uriStr)
            if (!DocumentsContract.isDocumentUri(context, uri)) return
            val docId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull() ?: return
            val parts = docId.split(":")
            if (parts.size != 2 || parts[0] != "primary") return
            File(Environment.getExternalStorageDirectory(), parts[1]).absolutePath
        }
        MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
    }
}
