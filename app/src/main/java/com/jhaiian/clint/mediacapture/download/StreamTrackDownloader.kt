package com.jhaiian.clint.mediacapture.download

import com.jhaiian.clint.downloads.ClintDownloadManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Request

class StreamCancelledException : Exception()

data class DownloadedSegment(val file: File, val key: HlsKeyInfo?)

object StreamTrackDownloader {

    private const val MAX_CONCURRENT_SEGMENTS = 12
    private const val MAX_SEGMENT_ATTEMPTS = 3
    private const val RAMP_UP_INTERVAL_MS = 3000L
    private const val DYNAMIC_ADJUST_INTERVAL_MS = 2000L
    private const val MONITOR_TICK_MS = 200L

    private data class FetchResult(
        val bytes: ByteArray?,
        val reason: String?,
        val resourceTotalLength: Long? = null,
        val httpCode: Int? = null,
        val retryAfterSeconds: Long? = null
    )

    private fun fetchSegment(
        seg: SegmentSpec, referer: String, cookies: String, userAgent: String, extraHeaders: Map<String, String>,
        activeCalls: MutableSet<Call>
    ): FetchResult {
        val builder = Request.Builder().url(seg.url)
        StreamRequestHeaders.apply(builder, seg.url, referer, cookies, userAgent, extraHeaders)
        if (seg.byteRangeOffset != null && seg.byteRangeLength != null) {
            val end = seg.byteRangeOffset + seg.byteRangeLength - 1
            builder.header("Range", "bytes=${seg.byteRangeOffset}-$end")
        }
        val call = ClintDownloadManager.httpClient.newCall(builder.build())
        activeCalls.add(call)
        return try {
            call.execute().use { resp ->
                if (!resp.isSuccessful) {
                    FetchResult(null, "HTTP ${resp.code}", httpCode = resp.code, retryAfterSeconds = resp.header("Retry-After")?.toLongOrNull())
                } else {
                    val totalLength = resp.header("Content-Range")?.substringAfterLast('/')?.toLongOrNull()
                    FetchResult(resp.body.bytes(), null, totalLength)
                }
            }
        } catch (e: Exception) {
            FetchResult(null, e.javaClass.simpleName + (e.message?.let { ": $it" } ?: ""))
        } finally {
            activeCalls.remove(call)
        }
    }

    suspend fun downloadSegments(
        track: ResolvedTrack,
        workDir: File,
        referer: String,
        cookies: String,
        userAgent: String,
        isCancelled: () -> Boolean,
        alreadyDownloaded: Map<Int, DownloadedSegment> = emptyMap(),
        extraHeaders: Map<String, String> = emptyMap(),
        speedLimiter: com.jhaiian.clint.downloads.SpeedLimiter? = null,
        maxConcurrentSegments: Int = MAX_CONCURRENT_SEGMENTS,
        onProgress: (completed: Int, total: Int, segmentBytes: Long) -> Unit
    ): Map<Int, DownloadedSegment> = coroutineScope {
        workDir.mkdirs()
        val all = listOfNotNull(track.initSegment) + track.segments
        val total = all.size
        val results = ConcurrentHashMap<Int, DownloadedSegment>(alreadyDownloaded)
        var completedCount = results.size
        val progressLock = Any()
        val pending = all.indices.filter { !results.containsKey(it) }
        val workerCount = maxConcurrentSegments.coerceIn(1, 24)
        val ceiling = AtomicInteger(workerCount)
        val maxSafe = AtomicInteger(workerCount)
        val rateLimitUntilMs = AtomicLong(0L)
        val bytesDone = AtomicLong(0L)
        val nextPending = AtomicInteger(0)
        val activeCalls = ConcurrentHashMap.newKeySet<Call>()

        val cancelWatcher = launch(Dispatchers.IO) {
            while (isActive) {
                if (isCancelled()) activeCalls.forEach { it.cancel() }
                delay(200L)
            }
        }

        val controller = launch(Dispatchers.Default) {
            var lastRamp = System.currentTimeMillis()
            var lastCheck = lastRamp
            var lastBytes = 0L
            var lastSpeed = 0L
            while (isActive) {
                delay(MONITOR_TICK_MS)
                val now = System.currentTimeMillis()
                if (now - lastRamp >= RAMP_UP_INTERVAL_MS) {
                    lastRamp = now
                    if (ceiling.get() < maxSafe.get() && rateLimitUntilMs.get() <= now) ceiling.incrementAndGet()
                }
                if (now - lastCheck >= DYNAMIC_ADJUST_INTERVAL_MS) {
                    val doneBytes = bytesDone.get()
                    val speed = (doneBytes - lastBytes) * 1000L / (now - lastCheck).coerceAtLeast(1L)
                    val enoughWork = pending.size - nextPending.get() >= ceiling.get()
                    if (enoughWork && lastSpeed > 0L && speed > 0L) {
                        if (speed > lastSpeed * 1.1 && ceiling.get() < maxSafe.get()) {
                            ceiling.incrementAndGet()
                        } else if (speed < lastSpeed * 0.9 && ceiling.get() > 1) {
                            ceiling.decrementAndGet()
                        }
                    }
                    lastSpeed = speed
                    lastBytes = doneBytes
                    lastCheck = now
                }
            }
        }

        try {
            val jobs = (0 until workerCount).map { workerId ->
                async(Dispatchers.IO) {
                    while (true) {
                        if (isCancelled()) throw StreamCancelledException()
                        if (nextPending.get() >= pending.size) break
                        if (workerId >= ceiling.get()) {
                            delay(100L)
                            continue
                        }
                        val index = nextPending.getAndIncrement()
                        if (index >= pending.size) break
                        val segmentIndex = pending[index]
                        val seg = all[segmentIndex]
                        var bytes: ByteArray? = null
                        var lastReason: String? = null
                        var attempt = 0
                        while (attempt < MAX_SEGMENT_ATTEMPTS && bytes == null) {
                            val waitMs = rateLimitUntilMs.get() - System.currentTimeMillis()
                            if (waitMs > 0L) {
                                delay(waitMs)
                                if (isCancelled()) throw StreamCancelledException()
                            }
                            val fetched = fetchSegment(seg, referer, cookies, userAgent, extraHeaders, activeCalls)
                            val expectedLength = seg.byteRangeLength
                            bytes = if (fetched.bytes != null) {
                                val actualSize = fetched.bytes.size.toLong()
                                val reachedEndOfResource = seg.byteRangeOffset != null && fetched.resourceTotalLength != null &&
                                    seg.byteRangeOffset + actualSize == fetched.resourceTotalLength
                                if (expectedLength != null && actualSize != expectedLength && !reachedEndOfResource) {
                                    lastReason = "range size mismatch (expected $expectedLength, got $actualSize)"
                                    null
                                } else fetched.bytes
                            } else null
                            if (bytes == null) {
                                if (isCancelled()) throw StreamCancelledException()
                                if (fetched.reason != null) lastReason = fetched.reason
                                if (fetched.httpCode == 429) {
                                    val backoffSeconds = fetched.retryAfterSeconds ?: (5L shl attempt).coerceIn(1L, 300L)
                                    rateLimitUntilMs.updateAndGet { current ->
                                        maxOf(current, System.currentTimeMillis() + backoffSeconds * 1000L)
                                    }
                                    if (ceiling.get() > 1) {
                                        val shrunk = ceiling.updateAndGet { (it - 1).coerceAtLeast(1) }
                                        maxSafe.updateAndGet { minOf(it, shrunk) }
                                    }
                                }
                                attempt++
                                if (attempt < MAX_SEGMENT_ATTEMPTS) StreamBackoff.wait(attempt, isCancelled)
                            }
                        }
                        if (bytes == null) {
                            throw IOException("Failed to fetch segment (${lastReason ?: "unknown error"}), part $segmentIndex of $total: ${seg.url}")
                        }
                        val segFile = File(workDir, "seg_%05d.part".format(segmentIndex))
                        FileOutputStream(segFile).use { it.write(bytes) }
                        speedLimiter?.acquire(bytes.size)
                        bytesDone.addAndGet(bytes.size.toLong())
                        results[segmentIndex] = DownloadedSegment(segFile, seg.key)
                        synchronized(progressLock) {
                            completedCount++
                            onProgress(completedCount, total, bytes.size.toLong())
                        }
                    }
                }
            }
            for (job in jobs) job.await()
            results
        } finally {
            controller.cancel()
            cancelWatcher.cancel()
        }
    }

    fun decryptSegments(
        segments: List<DownloadedSegment>,
        referer: String,
        cookies: String,
        userAgent: String,
        isCancelled: () -> Boolean,
        extraHeaders: Map<String, String> = emptyMap(),
        onProgress: (completed: Int, total: Int) -> Unit
    ) {
        val encrypted = segments.filter { it.key != null }
        if (encrypted.isEmpty()) return
        var completed = 0
        for (seg in encrypted) {
            if (isCancelled()) throw StreamCancelledException()
            val data = seg.file.readBytes()
            val decrypted = HlsAesDecryptor.decrypt(data, seg.key!!, referer, cookies, userAgent, extraHeaders)
                ?: throw IOException("Failed to decrypt segment: ${seg.file.name}")
            seg.file.writeBytes(decrypted)
            File(seg.file.path + ".dec").createNewFile()
            completed++
            onProgress(completed, encrypted.size)
        }
    }

    private fun transferAll(source: File, target: java.nio.channels.FileChannel): Long {
        java.io.FileInputStream(source).channel.use { input ->
            val size = input.size()
            var position = 0L
            while (position < size) {
                val moved = input.transferTo(position, size - position, target)
                if (moved <= 0L) break
                position += moved
            }
            if (position < size) {
                input.position(position)
                java.nio.channels.Channels.newInputStream(input).copyTo(java.nio.channels.Channels.newOutputStream(target))
            }
            return size
        }
    }

    fun concatenate(segments: List<DownloadedSegment>, outputFile: File, onProgress: ((pct: Int) -> Unit)? = null) {
        outputFile.parentFile?.mkdirs()
        val totalBytes = segments.sumOf { it.file.length() }
        var written = 0L
        var lastPct = -1
        FileOutputStream(outputFile).channel.use { out ->
            for (seg in segments) {
                written += transferAll(seg.file, out)
                if (onProgress != null && totalBytes > 0L) {
                    val pct = ((written * 100L) / totalBytes).toInt().coerceIn(0, 100)
                    if (pct != lastPct) {
                        lastPct = pct
                        onProgress(pct)
                    }
                }
            }
        }
    }

    fun concatenateMoving(
        segments: List<DownloadedSegment>,
        outputFile: File,
        checkActive: () -> Unit,
        onProgress: (pct: Int) -> Unit
    ) {
        outputFile.parentFile?.mkdirs()
        val totalBytes = segments.sumOf { it.file.length() }
        var written = 0L
        var lastPct = -1
        FileOutputStream(outputFile).channel.use { out ->
            for (seg in segments) {
                checkActive()
                val size = transferAll(seg.file, out)
                runCatching { seg.file.delete() }
                runCatching { File(seg.file.path + ".dec").delete() }
                written += size
                if (totalBytes > 0L) {
                    val pct = ((written * 100L) / totalBytes).toInt().coerceIn(0, 100)
                    if (pct != lastPct) {
                        lastPct = pct
                        onProgress(pct)
                    }
                }
            }
        }
    }

    fun cleanup(segments: List<DownloadedSegment>) {
        for (seg in segments) {
            runCatching { seg.file.delete() }
            runCatching { File(seg.file.path + ".dec").delete() }
        }
    }
}
