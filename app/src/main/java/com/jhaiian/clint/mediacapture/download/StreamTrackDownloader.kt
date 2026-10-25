package com.jhaiian.clint.mediacapture.download

import com.jhaiian.clint.downloads.AutoConcurrencyController
import com.jhaiian.clint.downloads.BatchedReader
import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.downloads.DownloadAutoTuner
import com.jhaiian.clint.downloads.DownloadDispatchers
import com.jhaiian.clint.downloads.DownloadLog
import com.jhaiian.clint.downloads.HostConnectionLimiter
import com.jhaiian.clint.downloads.SegmentMap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Protocol
import okhttp3.Request

class StreamCancelledException : Exception()

data class DownloadedSegment(val file: File, val key: HlsKeyInfo?)

object StreamTrackDownloader {

    internal const val PLAIN_SUFFIX = ".plain"

    private const val MAX_CONCURRENT_SEGMENTS = 12
    private const val MAX_SEGMENT_ATTEMPTS = 3
    private const val SEGMENT_BUFFER_BYTES = 128 * 1024
    private const val SEGMENT_MAP_INTERVAL_MS = 400L
    private const val UNKNOWN_SIZE_SOFTENING_BYTES = 512L * 1024L
    private const val RAMP_UP_INTERVAL_MS = 3000L
    private const val MONITOR_TICK_MS = 200L
    private const val MAX_RATE_LIMIT_WAIT_SEC = 300L

    private data class FetchResult(
        val size: Long?,
        val reason: String?,
        val resourceTotalLength: Long? = null,
        val httpCode: Int? = null,
        val retryAfterSeconds: Long? = null
    )

    private fun fetchSegment(
        seg: SegmentSpec, target: File, referer: String, cookies: String, userAgent: String, extraHeaders: Map<String, String>,
        activeCalls: MutableSet<Call>, speedLimiter: com.jhaiian.clint.downloads.SpeedLimiter?, tagId: Int,
        multiplexed: AtomicBoolean? = null,
        onBytes: ((written: Long, expected: Long) -> Unit)? = null
    ): FetchResult {
        val builder = Request.Builder().url(seg.url)
        if (tagId != DownloadLog.GLOBAL_ID) {
            builder.tag(ClintDownloadManager.CallTag::class.java, ClintDownloadManager.CallTag(tagId))
        }
        StreamRequestHeaders.apply(builder, seg.url, referer, cookies, userAgent, extraHeaders)
        if (seg.byteRangeOffset != null && seg.byteRangeLength != null) {
            val end = seg.byteRangeOffset + seg.byteRangeLength - 1
            builder.header("Range", "bytes=${seg.byteRangeOffset}-$end")
        }
        val call = ClintDownloadManager.httpClient.newCall(builder.build())
        activeCalls.add(call)
        val requestedRange = seg.byteRangeOffset != null && seg.byteRangeLength != null
        return try {
            call.execute().use { resp ->
                if (resp.protocol == Protocol.HTTP_2) multiplexed?.set(true)
                if (!resp.isSuccessful) {
                    FetchResult(
                        null, "HTTP ${resp.code}", httpCode = resp.code,
                        retryAfterSeconds = com.jhaiian.clint.downloads.parseRetryAfterSeconds(resp.header("Retry-After"))
                    )
                } else if (requestedRange && resp.code != 206 &&
                    !(seg.byteRangeOffset == 0L && resp.body.contentLength() == seg.byteRangeLength)
                ) {
                    FetchResult(null, "server ignored the byte range (HTTP ${resp.code})", httpCode = resp.code)
                } else {
                    val totalLength = resp.header("Content-Range")?.substringAfterLast('/')?.toLongOrNull()
                    val expected = resp.body.contentLength().takeIf { it > 0L } ?: seg.byteRangeLength ?: -1L
                    val htmlType = resp.header("Content-Type")?.substringBefore(';')?.trim()?.equals("text/html", ignoreCase = true) == true
                    var written = 0L
                    var firstByte = -1
                    resp.body.byteStream().use { input ->
                        FileOutputStream(target).use { output ->
                            val reader = BatchedReader(input, ByteArray(SEGMENT_BUFFER_BYTES))
                            while (true) {
                                val batch = minOf(
                                    speedLimiter?.batchBytes(SEGMENT_BUFFER_BYTES) ?: SEGMENT_BUFFER_BYTES,
                                    ClintDownloadManager.globalSpeedLimiter.batchBytes(SEGMENT_BUFFER_BYTES)
                                )
                                val read = reader.fill(batch)
                                if (read == -1) break
                                if (written == 0L) firstByte = reader.buffer[0].toInt() and 0xFF
                                output.write(reader.buffer, 0, read)
                                written += read
                                onBytes?.invoke(written, expected)
                                if (tagId != DownloadLog.GLOBAL_ID) ClintDownloadManager.noteActivity(tagId)
                                speedLimiter?.acquire(read)
                                ClintDownloadManager.globalSpeedLimiter.acquire(read)
                            }
                        }
                    }
                    when {
                        written == 0L -> {
                            target.delete()
                            FetchResult(null, "empty response")
                        }
                        htmlType && firstByte == '<'.code -> {
                            target.delete()
                            FetchResult(null, "server returned a web page instead of media")
                        }
                        else -> FetchResult(written, null, totalLength)
                    }
                }
            }
        } catch (e: Exception) {
            target.delete()
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
        logId: Int = DownloadLog.GLOBAL_ID,
        onSegmentMap: ((List<Float>) -> Unit)? = null,
        onProgress: (completed: Int, total: Int, segmentBytes: Long) -> Unit
    ): Map<Int, DownloadedSegment> = coroutineScope {
        val logging = logId != DownloadLog.GLOBAL_ID
        val host = runCatching { java.net.URI((track.initSegment ?: track.segments.firstOrNull())?.url ?: "").host }.getOrNull().orEmpty()
        workDir.mkdirs()
        val all = listOfNotNull(track.initSegment) + track.segments
        val total = all.size
        val results = ConcurrentHashMap<Int, DownloadedSegment>(alreadyDownloaded)
        var completedCount = results.size
        val progressLock = Any()
        val pending = all.indices.filter { !results.containsKey(it) }
        val autoSegments = DownloadAutoTuner.isAuto(maxConcurrentSegments)
        val tunerContext = ClintDownloadManager.appContext
        val multiplexed = AtomicBoolean(false)
        val workerCount = if (autoSegments) DownloadAutoTuner.segmentCeiling(tunerContext, multiplexed = true) else maxConcurrentSegments.coerceIn(1, 24)
        val ceiling = AtomicInteger(if (autoSegments) DownloadAutoTuner.startingSegments(tunerContext, host, workerCount) else workerCount)
        val maxSafe = AtomicInteger(workerCount)
        val transferred = AtomicLong(0L)
        val autoController = if (autoSegments) {
            AutoConcurrencyController(ceiling, { minOf(maxSafe.get(), DownloadAutoTuner.segmentCeiling(tunerContext, multiplexed.get())) }) {
                listOfNotNull(speedLimiter?.currentLimit(), ClintDownloadManager.globalSpeedLimiter.currentLimit())
                    .filter { it > 0L }.minOrNull() ?: 0L
            }
        } else null
        val rateLimitUntilMs = AtomicLong(0L)
        val bytesDone = AtomicLong(0L)
        val nextPending = AtomicInteger(0)
        val activeCalls = ConcurrentHashMap.newKeySet<Call>()
        val inFlight = ConcurrentHashMap<Int, Float>()
        val knownSizeTotal = AtomicLong(alreadyDownloaded.values.sumOf { runCatching { it.file.length() }.getOrDefault(0L) })
        val knownSizeCount = AtomicInteger(alreadyDownloaded.size)

        fun segmentFraction(written: Long, expected: Long): Float {
            val count = knownSizeCount.get()
            val estimate = when {
                expected > 0L -> expected
                count > 0 -> knownSizeTotal.get() / count
                else -> -1L
            }
            return if (estimate > 0L) (written.toFloat() / estimate).coerceIn(0f, if (expected > 0L) 0.99f else 0.95f)
            else (written.toFloat() / (written + UNKNOWN_SIZE_SOFTENING_BYTES)).coerceIn(0f, 0.9f)
        }

        fun segmentMap(): List<Float> = SegmentMap.build(total) { index ->
            if (results.containsKey(index)) 1f else inFlight[index] ?: 0f
        }
        if (logging) DownloadLog.i(logId, "SEGMENTS") {
            "Segment pass \u00B7 ${pending.size} to fetch of $total \u00B7 ${results.size} already on disk \u00B7 " +
                (if (autoSegments) "auto workers, starting at ${ceiling.get()} of up to $workerCount" else "$workerCount workers") +
                if (track.initSegment != null) " \u00B7 includes init segment" else ""
        }

        val cancelWatcher = launch(Dispatchers.IO) {
            while (isActive) {
                if (isCancelled()) activeCalls.forEach { it.cancel() }
                delay(200L)
            }
        }

        val controller = launch(Dispatchers.Default) {
            var lastRamp = System.currentTimeMillis()
            var lastMapAt = 0L
            while (isActive) {
                delay(MONITOR_TICK_MS)
                val now = System.currentTimeMillis()
                if (onSegmentMap != null && now - lastMapAt >= SEGMENT_MAP_INTERVAL_MS) {
                    lastMapAt = now
                    synchronized(progressLock) { onSegmentMap(segmentMap()) }
                }
                if (autoController != null) {
                    autoController.onTick(now, transferred.get(), canGrow = nextPending.get() < pending.size, blocked = rateLimitUntilMs.get() > now)
                } else if (now - lastRamp >= RAMP_UP_INTERVAL_MS) {
                    lastRamp = now
                    if (ceiling.get() < maxSafe.get() && rateLimitUntilMs.get() <= now) ceiling.incrementAndGet()
                }
            }
        }

        try {
            val jobs = (0 until workerCount).map { workerId ->
                async(DownloadDispatchers.network) {
                    while (true) {
                        ensureActive()
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
                        var size: Long? = null
                        var lastReason: String? = null
                        val segFile = File(workDir, "seg_%05d.part".format(segmentIndex))
                        val tmpFile = File(workDir, "seg_%05d.tmp".format(segmentIndex))
                        var attempt = 0
                        val segmentStartedAt = System.currentTimeMillis()
                        while (attempt < MAX_SEGMENT_ATTEMPTS && size == null) {
                            ensureActive()
                            val waitMs = rateLimitUntilMs.get() - System.currentTimeMillis()
                            if (waitMs > 0L) {
                                delay(waitMs)
                                if (isCancelled()) throw StreamCancelledException()
                            }
                            val fetchStartedAt = System.currentTimeMillis()
                            if (logging) DownloadLog.d(logId, "SEGMENT") {
                                "Segment ${segmentIndex + 1}/$total \u00B7 attempt ${attempt + 1}/$MAX_SEGMENT_ATTEMPTS \u00B7 ${seg.url}" +
                                    if (seg.byteRangeOffset != null && seg.byteRangeLength != null) " \u00B7 bytes=${seg.byteRangeOffset}-${seg.byteRangeOffset + seg.byteRangeLength - 1}" else ""
                            }
                            inFlight[segmentIndex] = 0f
                            var reported = 0L
                            val fetched = HostConnectionLimiter.withPermit(host) {
                                fetchSegment(
                                    seg, tmpFile, referer, cookies, userAgent, extraHeaders, activeCalls, speedLimiter, logId,
                                    multiplexed = multiplexed
                                ) { written, expected ->
                                    inFlight[segmentIndex] = segmentFraction(written, expected)
                                    transferred.addAndGet(written - reported)
                                    reported = written
                                }
                            }
                            val expectedLength = seg.byteRangeLength
                            val fetchedSize = fetched.size
                            size = if (fetchedSize != null) {
                                val reachedEndOfResource = seg.byteRangeOffset != null && fetched.resourceTotalLength != null &&
                                    seg.byteRangeOffset + fetchedSize == fetched.resourceTotalLength
                                if (expectedLength != null && fetchedSize != expectedLength && !reachedEndOfResource) {
                                    lastReason = "range size mismatch (expected $expectedLength, got $fetchedSize)"
                                    null
                                } else fetchedSize
                            } else null
                            if (size == null) {
                                if (isCancelled()) throw StreamCancelledException()
                                if (fetched.reason != null) lastReason = fetched.reason
                                if (logging) DownloadLog.w(logId, "SEGMENT") {
                                    "Segment ${segmentIndex + 1}/$total failed (${lastReason ?: "unknown error"}) after ${System.currentTimeMillis() - fetchStartedAt} ms \u00B7 attempt ${attempt + 1}/$MAX_SEGMENT_ATTEMPTS" +
                                        (if (fetched.httpCode == 429) " \u00B7 rate limited, workers ${ceiling.get()}" else "") + "\n${seg.url}"
                                }
                                if (fetched.httpCode == 429) {
                                    val backoffSeconds = (fetched.retryAfterSeconds ?: (5L shl attempt)).coerceIn(1L, MAX_RATE_LIMIT_WAIT_SEC)
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
                        val finalSize = size
                        if (finalSize == null) {
                            inFlight.remove(segmentIndex)
                            tmpFile.delete()
                            if (logging) DownloadLog.e(logId, "SEGMENT") { "Segment ${segmentIndex + 1}/$total gave up: ${lastReason ?: "unknown error"}\n${seg.url}" }
                            throw IOException("Failed to fetch segment (${lastReason ?: "unknown error"}), part $segmentIndex of $total: ${seg.url}")
                        }
                        if (!tmpFile.renameTo(segFile)) {
                            segFile.delete()
                            if (!tmpFile.renameTo(segFile)) throw IOException("Could not store segment ${segmentIndex + 1} of $total")
                        }
                        bytesDone.addAndGet(finalSize)
                        results[segmentIndex] = DownloadedSegment(segFile, seg.key)
                        inFlight.remove(segmentIndex)
                        knownSizeTotal.addAndGet(finalSize)
                        knownSizeCount.incrementAndGet()
                        if (logging) DownloadLog.d(logId, "SEGMENT") {
                            "Segment ${segmentIndex + 1}/$total done \u00B7 $finalSize B in ${System.currentTimeMillis() - segmentStartedAt} ms"
                        }
                        synchronized(progressLock) {
                            completedCount++
                            onProgress(completedCount, total, finalSize)
                        }
                    }
                }
            }
            for (job in jobs) job.await()
            if (onSegmentMap != null) synchronized(progressLock) { onSegmentMap(segmentMap()) }
            results
        } finally {
            controller.cancel()
            cancelWatcher.cancel()
            if (autoSegments) DownloadAutoTuner.rememberSegments(tunerContext, host, ceiling.get())
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
    ): List<DownloadedSegment> {
        val encryptedTotal = segments.count { it.key != null }
        if (encryptedTotal == 0) return segments
        var completed = 0
        return segments.map { seg ->
            val key = seg.key ?: return@map seg
            if (isCancelled()) throw StreamCancelledException()
            val plain = File(seg.file.path + PLAIN_SUFFIX)
            if (!plain.isFile || plain.length() == 0L) {
                val decrypted = HlsAesDecryptor.decrypt(seg.file.readBytes(), key, referer, cookies, userAgent, extraHeaders)
                    ?: throw IOException("Failed to decrypt segment: ${seg.file.name}")
                val temp = File(seg.file.path + PLAIN_SUFFIX + ".tmp")
                temp.writeBytes(decrypted)
                if (!temp.renameTo(plain)) {
                    plain.delete()
                    if (!temp.renameTo(plain)) throw IOException("Could not store decrypted segment: ${seg.file.name}")
                }
            }
            runCatching { seg.file.delete() }
            completed++
            onProgress(completed, encryptedTotal)
            DownloadedSegment(plain, null)
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
