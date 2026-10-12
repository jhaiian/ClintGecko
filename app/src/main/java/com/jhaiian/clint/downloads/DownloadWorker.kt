package com.jhaiian.clint.downloads

import android.app.NotificationManager
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.io.SequenceInputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal object DownloadWorker {

    private const val MIN_PART_BYTES = 512 * 1024L
    private const val MAX_PART_RETRIES = 3
    private const val RAMP_UP_INTERVAL_MS = 3000L
    private const val DYNAMIC_ADJUST_INTERVAL_MS = 2000L
    private const val MONITOR_TICK_MS = 200L
    private const val PROGRESS_CHECKPOINT_INTERVAL_MS = 1500L

    private fun finalizeElapsed(item: DownloadItem): DownloadItem =
        if (item.activeStartedAt > 0L) {
            item.copy(
                activeElapsedMs = item.activeElapsedMs + (System.currentTimeMillis() - item.activeStartedAt),
                activeStartedAt = 0L
            )
        } else item

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

        val resumeEffectiveParts = if (
            item.resumable && !item.parallelRateLimited && item.totalBytes > 0L &&
            item.file?.exists() == true && item.bytesDownloaded < item.totalBytes
        ) {
            maxOf(1, minOf(item.splitParts, (item.totalBytes / MIN_PART_BYTES).toInt()))
        } else 1

        val isResumingFile = item.bytesDownloaded > 0L && item.file?.exists() == true

        try {
            if (resumeEffectiveParts > 1) {
                item = item.copy(status = DownloadStatus.DOWNLOADING, activeStartedAt = System.currentTimeMillis())
                ClintDownloadManager.persistDownload(item)
                ClintDownloadManager.publish(item)
                runParallelDownload(
                    context, item, item.file!!, resumeEffectiveParts, item.multithreadingParts, safMode,
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
                .apply {
                    if (item.referer.isNotEmpty()) header("Referer", item.referer)
                    if (item.cookies.isNotEmpty()) header("Cookie", item.cookies)
                    if (isResumingFile) header("Range", "bytes=${item.bytesDownloaded}-")
                }
                .build()

            if (ClintDownloadManager.isRemoved(item.id)) throw CancellationException("removed")

            val response = withContext(Dispatchers.IO) {
                runInterruptible { ClintDownloadManager.httpClient.newCall(request).execute() }
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
                response.close()
                fail(context, item, "Server error ${response.code}")
                return
            }

            val serverAcceptedRange = response.code == 206

            if (isResumingFile && !serverAcceptedRange) {
                val contentType = response.header("Content-Type")?.substringBefore(";")?.trim()?.lowercase()
                val looksLikeErrorPage = contentType == "text/html" || contentType == "text/plain" || contentType == "application/json"
                if (looksLikeErrorPage) {
                    response.close()
                    fail(context, item, context.getString(R.string.download_error_link_expired))
                    return
                }
                item = item.copy(bytesDownloaded = 0L, resumable = false, file = null)
                withContext(Dispatchers.IO) { initialItem.file.delete() }
            }

            val effectiveResume = isResumingFile && serverAcceptedRange
            val body = response.body
            val rawStream = body.byteStream()
            var peekedBytes: ByteArray? = null

            var writeStartPos = item.bytesDownloaded
            var outputFile: File

            if (effectiveResume) {
                outputFile = item.file!!
            } else {
                item = item.copy(
                    resumable = response.header("Accept-Ranges") == "bytes",
                    totalBytes = response.header("Content-Length")?.toLongOrNull() ?: -1L,
                    bytesDownloaded = 0L
                )
                writeStartPos = 0L
                ClintDownloadManager.publish(item)

                val peeked = withContext(Dispatchers.IO) {
                    runInterruptible {
                        val buf = ByteArray(512)
                        val read = rawStream.read(buf)
                        if (read > 0) buf.copyOf(read) else ByteArray(0)
                    }
                }
                peekedBytes = peeked

                var finalFilename = item.filename
                val looksGeneric = finalFilename.endsWith(".bin") || !finalFilename.substringAfterLast('/').contains(".")
                if (looksGeneric) {
                    val contentType = response.header("Content-Type")?.substringBefore(";")?.trim()
                    val extFromMime = contentType?.let {
                        android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
                    }
                    val extFromMagic = DownloadFileHelper.detectExtFromMagicBytes(peeked)
                    val ext = extFromMagic ?: extFromMime
                    if (ext != null) {
                        val base = finalFilename.substringBeforeLast(".", finalFilename).ifEmpty { "download" }
                        finalFilename = "$base.$ext"
                    }
                }
                item = item.copy(filename = finalFilename)

                outputFile = withContext(Dispatchers.IO) {
                    val existing = item.file
                    if (existing != null && existing.exists() && item.totalBytes > 0 && existing.length() == item.totalBytes) {
                        existing
                    } else {
                        val rawDestDir = directCustomDir
                            ?: if (safMode) DownloadFileHelper.tempDownloadDir(context) else DownloadFileHelper.resolveDownloadDir()
                        val destDir = if (safMode) rawDestDir else DownloadCategories.resolveDir(item.categorizeEnabled, rawDestDir, finalFilename)
                        destDir.mkdirs()
                        DownloadFileHelper.uniqueFile(destDir, finalFilename)
                    }
                }
                item = item.copy(filename = outputFile.name, file = outputFile)

                if (item.totalBytes > 0) {
                    val allocated = preAllocateFile(context, item, outputFile)
                    if (allocated == null) return
                    item = allocated

                    if (ClintDownloadManager.pauseRequested.remove(item.id)) {
                        handlePauseTransition(context, item)
                        return
                    }

                    val effectiveParts = maxOf(1, minOf(item.splitParts, (item.totalBytes / MIN_PART_BYTES).toInt()))
                    if (item.resumable && effectiveParts > 1 && !item.parallelRateLimited) {
                        response.close()
                        item = item.copy(status = DownloadStatus.DOWNLOADING, activeStartedAt = System.currentTimeMillis())
                        ClintDownloadManager.persistDownload(item)
                        ClintDownloadManager.publish(item)
                        runParallelDownload(context, item, outputFile, effectiveParts, item.multithreadingParts, safMode, speedLimiter = speedLimiter)
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

            val copyResult = copyToFile(context, item, inputStream, outputFile, writeStartPos, speedLimiter)
            item = copyResult.item

            if (!copyResult.completed) {
                handlePauseTransition(context, item)
                return
            }

            if (safMode) {
                moveTempToSaf(context, item)
            } else {
                item = finalizeElapsed(item)
                item = item.copy(completedAt = System.currentTimeMillis(), status = DownloadStatus.COMPLETE)
                ClintDownloadManager.persistDownload(item)
                ClintDownloadManager.publish(item)
                DownloadNotificationHelper.showCompleteNotification(context, item)
                ClintDownloadManager.tryDequeueNext(context)
                scanCompletedFile(context, item)
            }
        } catch (e: Throwable) {
            handleAttemptFailure(context, item, e)
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
        suspend fun cleanup() {
            if (item.status != DownloadStatus.PAUSED) {
                if (e is CancellationException) {
                    item.file?.delete()
                    ClintDownloadManager.persistDownload(item)
                    ClintDownloadManager.publish(item)
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
        outputFile: File,
        writeStartPos: Long,
        speedLimiter: SpeedLimiter
    ): CopyResult {
        var current = item
        var reachedEof = false
        var lastNotifyBytes = current.bytesDownloaded
        var lastSpeedBytes = current.bytesDownloaded
        var lastSpeedTime = System.currentTimeMillis()
        var lastCheckpointTime = System.currentTimeMillis()

        coroutineScope {
            val pauseWatcher = launch(Dispatchers.IO) {
                while (isActive) {
                    if (ClintDownloadManager.pauseRequested.contains(current.id)) {
                        runCatching { inputStream.close() }
                        break
                    }
                    delay(MONITOR_TICK_MS)
                }
            }
            try {
                withContext(Dispatchers.IO) {
                    runInterruptible {
                        RandomAccessFile(outputFile, "rw").use { raf ->
                            raf.seek(writeStartPos)
                            inputStream.use { input ->
                                val buffer = ByteArray(65536)
                                while (true) {
                                    if (ClintDownloadManager.isRemoved(current.id)) throw CancellationException("removed")
                                    if (ClintDownloadManager.pauseRequested.contains(current.id)) break
                                    val read = try {
                                        input.read(buffer)
                                    } catch (e: IOException) {
                                        if (ClintDownloadManager.isRemoved(current.id)) throw CancellationException("removed")
                                        if (ClintDownloadManager.pauseRequested.contains(current.id)) break else throw e
                                    }
                                    if (read == -1) {
                                        reachedEof = true
                                        break
                                    }
                                    raf.write(buffer, 0, read)
                                    speedLimiter.acquire(read)
                                    current = current.copy(bytesDownloaded = current.bytesDownloaded + read)
                                    if (current.bytesDownloaded - lastNotifyBytes >= 65536) {
                                        lastNotifyBytes = current.bytesDownloaded
                                        val now = System.currentTimeMillis()
                                        val elapsed = now - lastSpeedTime
                                        if (elapsed >= 400) {
                                            val delta = current.bytesDownloaded - lastSpeedBytes
                                            current = current.copy(speedBytesPerSec = if (elapsed > 0) delta * 1000L / elapsed else 0L)
                                            lastSpeedBytes = current.bytesDownloaded
                                            lastSpeedTime = now
                                        }
                                        DownloadNotificationHelper.showProgressNotification(context, current)
                                        ClintDownloadManager.publish(ClintDownloadManager.withLiveSettings(current))
                                        if (now - lastCheckpointTime >= PROGRESS_CHECKPOINT_INTERVAL_MS) {
                                            lastCheckpointTime = now
                                            ClintDownloadManager.checkpointProgress(current.id, current.bytesDownloaded, 0L, "")
                                        }
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
        return CopyResult(current, reachedEof)
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

        try {
            var lastNotifiedProgress = 0
            item = withContext(Dispatchers.IO) {
                runInterruptible {
                    val docDir = DocumentFile.fromTreeUri(context, treeUri)
                        ?: throw IOException(context.getString(R.string.download_error_saf_no_access))
                    if (!docDir.canWrite()) throw IOException(context.getString(R.string.download_error_saf_not_writable))

                    val targetDir = DownloadCategories.resolveSafDir(item.categorizeEnabled, docDir, item.filename)
                    val finalName = DownloadFileHelper.uniqueSafName(targetDir, item.filename)
                    val docFile = targetDir.createFile("application/octet-stream", finalName)
                        ?: throw IOException(context.getString(R.string.download_error_saf_create_failed))

                    var working = item.copy(filename = docFile.name ?: item.filename)

                    val totalBytes = tempFile.length()
                    var bytesCopied = 0L
                    val buffer = ByteArray(65536)

                    val output = context.contentResolver.openOutputStream(docFile.uri)
                        ?: throw IOException(context.getString(R.string.download_error_saf_no_stream))
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
                                        DownloadNotificationHelper.showCopyingTempNotification(context, working)
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
            withContext(Dispatchers.IO) { tempFile.delete() }
            if (e is CancellationException) throw e
            if (item.status != DownloadStatus.PAUSED) {
                fail(context, item, e.message ?: context.getString(R.string.download_error_unknown))
            }
        }
    }

    private suspend fun preAllocateFile(context: Context, initialItem: DownloadItem, file: File): DownloadItem? {
        var item = initialItem.copy(status = DownloadStatus.ALLOCATING, allocationProgress = 0)
        ClintDownloadManager.persistDownload(item)
        ClintDownloadManager.publish(item)
        DownloadNotificationHelper.showAllocationNotification(context, item)

        if (item.id in ClintDownloadManager.removedIds) {
            withContext(Dispatchers.IO) { file.delete() }
            ClintDownloadManager.persistDownload(item)
            ClintDownloadManager.publish(item)
            return null
        }

        return try {
            withContext(Dispatchers.IO) {
                runInterruptible {
                    RandomAccessFile(file, "rw").use { it.setLength(item.totalBytes) }
                }
            }
            item = item.copy(allocationProgress = 100)
            ClintDownloadManager.publish(item)
            DownloadNotificationHelper.showAllocationNotification(context, item)
            item
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            withContext(Dispatchers.IO) { file.delete() }
            fail(context, item, e.message ?: context.getString(R.string.download_error_unknown), scheduleRetry = false)
            null
        }
    }

    private fun partsToMask(partCompleted: Array<AtomicBoolean>): Long {
        var mask = 0L
        for (i in partCompleted.indices) if (partCompleted[i].get()) mask = mask or (1L shl i)
        return mask
    }

    private fun encodePartOffsets(partBytesDownloaded: Array<AtomicLong>, partCompleted: Array<AtomicBoolean>): String =
        partBytesDownloaded.indices
            .filter { !partCompleted[it].get() && partBytesDownloaded[it].get() > 0L }
            .joinToString(",") { "$it:${partBytesDownloaded[it].get()}" }

    private fun decodePartOffsets(encoded: String): Map<Int, Long> {
        if (encoded.isBlank()) return emptyMap()
        return encoded.split(",").mapNotNull { pair ->
            val idx = pair.substringBefore(":").toIntOrNull()
            val bytes = pair.substringAfter(":").toLongOrNull()
            if (idx != null && bytes != null) idx to bytes else null
        }.toMap()
    }

    private suspend fun runParallelDownload(
        context: Context,
        initialItem: DownloadItem,
        outputFile: File,
        totalParts: Int,
        simultaneousParts: Int,
        safMode: Boolean,
        speedLimiter: SpeedLimiter,
        resumeFromMask: Long = 0L
    ) {
        var item = initialItem
        val partSize = item.totalBytes / totalParts

        val partBytesDownloaded = Array(totalParts) { AtomicLong(0L) }
        val resumeOffsets = decodePartOffsets(initialItem.partOffsets)
        val partCompleted = Array(totalParts) { idx ->
            val alreadyDone = (resumeFromMask shr idx) and 1L == 1L
            if (alreadyDone) {
                val start = idx.toLong() * partSize
                val end = if (idx == totalParts - 1) item.totalBytes - 1 else (start + partSize - 1)
                partBytesDownloaded[idx].set(end - start + 1)
            } else {
                resumeOffsets[idx]?.let { partBytesDownloaded[idx].set(it) }
            }
            AtomicBoolean(alreadyDone)
        }
        val firstError = AtomicReference<String?>(null)
        val rateLimitDetected = AtomicBoolean(false)
        val rateLimitUntilMs = AtomicLong(0L)
        val currentCeiling = AtomicInteger(simultaneousParts)
        val maxSafeConcurrency = AtomicInteger(simultaneousParts)
        val nextPartIndex = AtomicInteger(0)
        val activeWorkers = AtomicInteger(0)

        fun claimNextPart(): Int? {
            while (true) {
                val idx = nextPartIndex.get()
                if (idx >= totalParts) return null
                if (!nextPartIndex.compareAndSet(idx, idx + 1)) continue
                if (partCompleted[idx].get()) continue
                return idx
            }
        }

        fun stopRequested(): Boolean =
            firstError.get() != null || rateLimitDetected.get() || ClintDownloadManager.pauseRequested.contains(item.id)

        coroutineScope {
            val partJobs = mutableListOf<Job>()

            fun spawnWorker() {
                activeWorkers.incrementAndGet()
                val job = launch(Dispatchers.IO) {
                    try {
                        while (isActive && !stopRequested()) {
                            val partIndex = claimNextPart() ?: break
                            val start = partIndex * partSize
                            val end = if (partIndex == totalParts - 1) item.totalBytes - 1 else (start + partSize - 1)
                            downloadPart(
                                item, partIndex, start, end, outputFile,
                                partBytesDownloaded, partCompleted, firstError,
                                rateLimitDetected, rateLimitUntilMs, currentCeiling, maxSafeConcurrency,
                                speedLimiter
                            )
                        }
                    } finally {
                        activeWorkers.decrementAndGet()
                    }
                }
                partJobs += job
            }

            repeat(currentCeiling.get()) { spawnWorker() }

            var lastNotifyBytes = item.bytesDownloaded
            var lastSpeedBytes = item.bytesDownloaded
            var lastSpeedTime = System.currentTimeMillis()
            var lastRampUp = System.currentTimeMillis()
            var lastDynamicCheck = System.currentTimeMillis()
            var lastDynamicSpeed = 0L
            var lastCheckpointTime = System.currentTimeMillis()

            while (true) {
                delay(MONITOR_TICK_MS)

                if (ClintDownloadManager.isRemoved(item.id)) {
                    partJobs.forEach { it.cancel() }
                    throw CancellationException("removed")
                }

                val total = partBytesDownloaded.sumOf { it.get() }
                item = item.copy(bytesDownloaded = total)
                if (total - lastNotifyBytes >= 65536) {
                    lastNotifyBytes = total
                    val now = System.currentTimeMillis()
                    val elapsed = now - lastSpeedTime
                    if (elapsed >= 400) {
                        val delta = total - lastSpeedBytes
                        item = item.copy(speedBytesPerSec = if (elapsed > 0) delta * 1000L / elapsed else 0L)
                        lastSpeedBytes = total
                        lastSpeedTime = now
                    }
                    DownloadNotificationHelper.showProgressNotification(context, item)
                    ClintDownloadManager.publish(ClintDownloadManager.withLiveSettings(item))
                }

                val now = System.currentTimeMillis()
                if (now - lastCheckpointTime >= PROGRESS_CHECKPOINT_INTERVAL_MS) {
                    lastCheckpointTime = now
                    ClintDownloadManager.checkpointProgress(
                        item.id, total, partsToMask(partCompleted), encodePartOffsets(partBytesDownloaded, partCompleted)
                    )
                }
                if (now - lastRampUp >= RAMP_UP_INTERVAL_MS) {
                    lastRampUp = now
                    if (currentCeiling.get() < maxSafeConcurrency.get() && rateLimitUntilMs.get() <= now) {
                        currentCeiling.incrementAndGet()
                    }
                }
                if (now - lastDynamicCheck >= DYNAMIC_ADJUST_INTERVAL_MS) {
                    val currentSpeed = (total - lastDynamicSpeed).let { delta ->
                        if (lastDynamicCheck > 0) delta * 1000L / (now - lastDynamicCheck).coerceAtLeast(1) else 0L
                    }
                    if (lastDynamicSpeed > 0 && currentSpeed > 0) {
                        if (currentSpeed > lastDynamicSpeed * 1.1 && currentCeiling.get() < maxSafeConcurrency.get()) {
                            currentCeiling.incrementAndGet()
                        } else if (currentSpeed < lastDynamicSpeed * 0.9 && currentCeiling.get() > 1) {
                            currentCeiling.decrementAndGet()
                        }
                    }
                    lastDynamicSpeed = total
                    lastDynamicCheck = now
                }

                val neededWorkers = currentCeiling.get() - activeWorkers.get()
                if (neededWorkers > 0 && nextPartIndex.get() < totalParts) {
                    repeat(neededWorkers) { spawnWorker() }
                }

                if (stopRequested() || !isActive) {
                    partJobs.forEach { it.cancel() }
                    break
                }
                if (activeWorkers.get() == 0 && nextPartIndex.get() >= totalParts) {
                    break
                }
            }
        }

        resolveParallelOutcome(
            context, item, outputFile, totalParts, partSize,
            partBytesDownloaded, partCompleted, firstError, rateLimitDetected, rateLimitUntilMs, safMode
        )
    }

    private suspend fun downloadPart(
        item: DownloadItem,
        partIndex: Int,
        start: Long,
        end: Long,
        outputFile: File,
        partBytesDownloaded: Array<AtomicLong>,
        partCompleted: Array<AtomicBoolean>,
        firstError: AtomicReference<String?>,
        rateLimitDetected: AtomicBoolean,
        rateLimitUntilMs: AtomicLong,
        currentCeiling: AtomicInteger,
        maxSafeConcurrency: AtomicInteger,
        speedLimiter: SpeedLimiter
    ) {
        var attempt = 0
        while (attempt < MAX_PART_RETRIES) {
            if (ClintDownloadManager.isRemoved(item.id)) return
            if (firstError.get() != null || ClintDownloadManager.pauseRequested.contains(item.id) || rateLimitDetected.get()) return

            val waitMs = rateLimitUntilMs.get() - System.currentTimeMillis()
            if (waitMs > 0) {
                delay(waitMs)
                if (firstError.get() != null || ClintDownloadManager.pauseRequested.contains(item.id) || rateLimitDetected.get()) return
            }

            val resumeStart = start + partBytesDownloaded[partIndex].get()
            if (resumeStart > end) {
                partCompleted[partIndex].set(true)
                return
            }

            try {
                val request = Request.Builder()
                    .url(item.url)
                    .tag(ClintDownloadManager.CallTag::class.java, ClintDownloadManager.CallTag(item.id))
                    .header("User-Agent", item.userAgent)
                    .header("Accept", "*/*")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Range", "bytes=$resumeStart-$end")
                    .apply {
                        if (item.referer.isNotEmpty()) header("Referer", item.referer)
                        if (item.cookies.isNotEmpty()) header("Cookie", item.cookies)
                    }
                    .build()

                val call = ClintDownloadManager.httpClient.newCall(request)
                val callWatcher = ClintDownloadManager.applicationScope.launch(Dispatchers.IO) {
                    while (isActive) {
                        if (ClintDownloadManager.isRemoved(item.id) || firstError.get() != null || ClintDownloadManager.pauseRequested.contains(item.id) || rateLimitDetected.get()) {
                            call.cancel()
                            break
                        }
                        delay(MONITOR_TICK_MS)
                    }
                }

                try {
                    val response = withContext(Dispatchers.IO) {
                        runInterruptible { call.execute() }
                    }

                    if (response.code == 429) {
                        val retryAfter = response.header("Retry-After")?.toLongOrNull()
                        val backoffSeconds = retryAfter ?: (5L shl attempt).coerceIn(1L, 300L)
                        rateLimitUntilMs.updateAndGet { current -> maxOf(current, System.currentTimeMillis() + backoffSeconds * 1000L) }
                        response.close()
                        if (currentCeiling.get() > 1) {
                            val shrunk = currentCeiling.updateAndGet { (it - 1).coerceAtLeast(1) }
                            maxSafeConcurrency.updateAndGet { minOf(it, shrunk) }
                            attempt++
                            continue
                        } else {
                            rateLimitDetected.set(true)
                            return
                        }
                    }

                    if (!response.isSuccessful || response.code != 206) {
                        response.close()
                        attempt++
                        if (attempt >= MAX_PART_RETRIES) {
                            firstError.compareAndSet(null, "Server error ${response.code}")
                        } else {
                            delay(attempt * 500L)
                        }
                        continue
                    }

                    val body = response.body

                    try {
                        withContext(Dispatchers.IO) {
                            runInterruptible {
                                body.byteStream().use { input ->
                                    RandomAccessFile(outputFile, "rw").use { raf ->
                                        raf.seek(resumeStart)
                                        val buffer = ByteArray(32768)
                                        while (true) {
                                            if (ClintDownloadManager.isRemoved(item.id)
                                                || firstError.get() != null
                                                || Thread.currentThread().isInterrupted
                                                || ClintDownloadManager.pauseRequested.contains(item.id)
                                                || rateLimitDetected.get()
                                            ) break
                                            val read = input.read(buffer)
                                            if (read == -1) {
                                                partCompleted[partIndex].set(true)
                                                break
                                            }
                                            raf.write(buffer, 0, read)
                                            speedLimiter.acquire(read)
                                            partBytesDownloaded[partIndex].addAndGet(read.toLong())
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
                        response.close()
                    }
                    return
                } finally {
                    callWatcher.cancel()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                attempt++
                if (attempt >= MAX_PART_RETRIES) {
                    firstError.compareAndSet(null, e.message ?: "Download error")
                } else {
                    delay(attempt * 500L)
                }
            }
        }
    }

    private suspend fun resolveParallelOutcome(
        context: Context,
        initialItem: DownloadItem,
        outputFile: File,
        totalParts: Int,
        partSize: Long,
        partBytesDownloaded: Array<AtomicLong>,
        partCompleted: Array<AtomicBoolean>,
        firstError: AtomicReference<String?>,
        rateLimitDetected: AtomicBoolean,
        rateLimitUntilMs: AtomicLong,
        safMode: Boolean
    ) {
        var item = initialItem

        if (ClintDownloadManager.isRemoved(item.id)) throw CancellationException("removed")

        fun interruptedProgress(): DownloadItem {
            val total = partBytesDownloaded.sumOf { it.get() }
            return item.copy(
                bytesDownloaded = total,
                completedPartsMask = partsToMask(partCompleted),
                partOffsets = encodePartOffsets(partBytesDownloaded, partCompleted)
            )
        }

        if (ClintDownloadManager.pauseRequested.remove(item.id)) {
            item = interruptedProgress()
            handlePauseTransition(context, item)
            return
        }

        if (rateLimitDetected.get()) {
            item = interruptedProgress().copy(parallelRateLimited = true, speedBytesPerSec = 0L)
            item = finalizeElapsed(item)
            ClintDownloadManager.persistDownload(item)
            ClintDownloadManager.publish(item)
            val waitMs = rateLimitUntilMs.get() - System.currentTimeMillis()
            if (waitMs > 0) delay(waitMs)
            run(context, item)
            return
        }

        val error = firstError.get()
        if (error != null) {
            item = interruptedProgress()
            fail(context, item, error)
            return
        }

        item = item.copy(bytesDownloaded = item.totalBytes, completedPartsMask = 0L, partOffsets = "")
        if (safMode) {
            moveTempToSaf(context, item)
        } else {
            item = finalizeElapsed(item)
            item = item.copy(completedAt = System.currentTimeMillis(), status = DownloadStatus.COMPLETE)
            ClintDownloadManager.persistDownload(item)
            ClintDownloadManager.publish(item)
            DownloadNotificationHelper.showCompleteNotification(context, item)
            ClintDownloadManager.tryDequeueNext(context)
            scanCompletedFile(context, item)
        }
    }

    private fun isServerError(msg: String): Boolean {
        val match = Regex("Server error (\\d+)").find(msg) ?: return false
        val code = match.groupValues[1].toIntOrNull() ?: return false
        return code in 400..499 && code != 429
    }

    private fun isDiskFull(msg: String): Boolean = msg.contains("No space left on device", ignoreCase = true)

    suspend fun fail(context: Context, initialItem: DownloadItem, msg: String, scheduleRetry: Boolean = true) {
        val ctx = ClintDownloadManager.appContext ?: context
        var item = initialItem

        if (!isServerError(msg) &&
            item.id !in ClintDownloadManager.removedIds &&
            !DownloadNetworkMonitor.isNetworkAvailable(ctx)
        ) {
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

        val serverError = isServerError(msg)
        val diskFull = isDiskFull(msg)
        val displayMsg = if (diskFull) context.getString(R.string.download_error_disk_full) else msg
        val canRetry = scheduleRetry &&
            retryEnabled &&
            (retryUnrecoverable || (!serverError && !diskFull)) &&
            item.id !in ClintDownloadManager.removedIds &&
            (retryCount == 0 || item.retryAttempt < retryCount)

        if (canRetry) {
            item = item.copy(
                retryAttempt = item.retryAttempt + 1,
                status = DownloadStatus.RETRYING,
                retryDelaySec = retryInterval.toInt(),
                lastErrorWasServerError = serverError,
                errorMessage = displayMsg,
                speedBytesPerSec = 0L
            )
            ClintDownloadManager.persistDownload(item)
            ClintDownloadManager.publish(item)
            if (item.retryAttempt == 1) {
                DownloadNotificationHelper.showRetryingNotification(ctx, item)
            }
            delay(retryInterval * 1000L)
            if (item.id in ClintDownloadManager.removedIds) {

            } else if (ClintDownloadManager.pauseRequested.remove(item.id)) {
                item = item.copy(retryDelaySec = 0)
                handlePauseTransition(context, item)
            } else {
                item = item.copy(retryDelaySec = 0)
                ClintDownloadManager.publish(item)
                if (item.isStream) {
                    com.jhaiian.clint.mediacapture.download.StreamDownloadJob.run(context, item)
                } else {
                    run(context, item)
                }
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
