package com.jhaiian.clint.downloads

import android.content.Context
import com.jhaiian.clint.R
import com.jhaiian.clint.downloads.mega.MegaCtrDecryptor
import com.jhaiian.clint.downloads.mega.MegaRanges
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

internal class PartRange(val start: Long, end: Long, done: Long) {
    @Volatile var end: Long = end
    val done = AtomicLong(done)
    @Volatile var active = false
    @Volatile var failures = 0
    @Volatile var retryAt = 0L
    val position: Long get() = start + done.get()
    val remaining: Long get() = (end - position + 1).coerceAtLeast(0L)
    val length: Long get() = (end - start + 1).coerceAtLeast(1L)
    val complete: Boolean get() = remaining == 0L
}

internal class ContentRangeInfo(val start: Long, val end: Long, val total: Long?) {
    companion object {
        private val SATISFIED = Regex("""bytes\s+(\d+)-(\d+)/(\d+|\*)""", RegexOption.IGNORE_CASE)
        private val UNSATISFIED = Regex("""bytes\s+\*/(\d+)""", RegexOption.IGNORE_CASE)

        fun parse(header: String?): ContentRangeInfo? {
            val match = SATISFIED.find(header ?: return null) ?: return null
            val start = match.groupValues[1].toLongOrNull() ?: return null
            val end = match.groupValues[2].toLongOrNull() ?: return null
            return ContentRangeInfo(start, end, match.groupValues[3].toLongOrNull())
        }

        fun parseUnsatisfiedTotal(header: String?): Long? =
            UNSATISFIED.find(header ?: return null)?.groupValues?.get(1)?.toLongOrNull()
    }
}

internal fun parseRetryAfterSeconds(value: String?): Long? {
    val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    raw.toLongOrNull()?.let { return it.coerceAtLeast(0L) }
    return runCatching {
        val at = java.time.ZonedDateTime.parse(raw, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
        ((at - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
    }.getOrNull()
}

internal object PartMap {

    fun encode(ranges: List<PartRange>): String =
        ranges.joinToString(";") { "${it.start}-${it.end}:${it.done.get().coerceAtMost(it.end - it.start + 1)}" }

    fun decode(encoded: String, totalBytes: Long): MutableList<PartRange>? {
        if (encoded.isBlank() || totalBytes <= 0L) return null
        val ranges = encoded.split(";").mapNotNull { entry ->
            val span = entry.substringBefore(":")
            val start = span.substringBefore("-").toLongOrNull()
            val end = span.substringAfter("-", "").toLongOrNull()
            val done = entry.substringAfter(":", "").toLongOrNull()
            if (start == null || end == null || done == null || end < start) null
            else PartRange(start, end, done.coerceIn(0L, end - start + 1))
        }.sortedBy { it.start }
        if (ranges.isEmpty() || ranges.first().start != 0L || ranges.last().end != totalBytes - 1) return null
        for (i in 1 until ranges.size) {
            if (ranges[i].start != ranges[i - 1].end + 1) return null
        }
        return ranges.toMutableList()
    }

    fun equal(totalBytes: Long, parts: Int, completedMask: Long, offsets: String): MutableList<PartRange> {
        val count = parts.coerceAtLeast(1)
        val size = totalBytes / count
        val partial = offsets.split(",").mapNotNull { pair ->
            val index = pair.substringBefore(":").toIntOrNull()
            val bytes = pair.substringAfter(":", "").toLongOrNull()
            if (index != null && bytes != null) index to bytes else null
        }.toMap()
        return MutableList(count) { i ->
            val start = i * size
            val end = if (i == count - 1) totalBytes - 1 else start + size - 1
            val length = end - start + 1
            val done = when {
                i < 64 && (completedMask shr i) and 1L == 1L -> length
                else -> (partial[i] ?: 0L).coerceIn(0L, length)
            }
            PartRange(start, end, done)
        }
    }

    fun fractions(encoded: String, totalBytes: Long): List<Float>? =
        decode(encoded, totalBytes)?.map { (it.done.get().toFloat() / it.length).coerceIn(0f, 1f) }
}

internal sealed interface ParallelOutcome {
    class Completed(val item: DownloadItem) : ParallelOutcome
    class Paused(val item: DownloadItem) : ParallelOutcome
    class FallbackSingle(val item: DownloadItem, val waitMs: Long) : ParallelOutcome
    class Restart(val item: DownloadItem, val reason: String) : ParallelOutcome
    class Failed(val item: DownloadItem, val message: String) : ParallelOutcome
}

internal object ParallelDownloader {

    private const val MIN_SPLIT_BYTES = 512L * 1024L
    private const val BUFFER_BYTES = 256 * 1024
    private const val MONITOR_TICK_MS = 200L
    private const val CHECKPOINT_INTERVAL_MS = 2000L
    private const val RAMP_UP_INTERVAL_MS = 3000L
    private const val NO_PROGRESS_LIMIT_MS = 90_000L
    private const val MAX_BACKOFF_MS = 30_000L
    private const val MAX_RATE_LIMIT_WAIT_SEC = 300L
    private const val GIVE_UP_FAILURES = 8

    private sealed interface RangeResult {
        data object Done : RangeResult
        data object Stopped : RangeResult
        data object NoRangeSupport : RangeResult
        class RateLimited(val retryAfterSeconds: Long?) : RangeResult
        class Restart(val reason: String) : RangeResult
        class Fatal(val message: String) : RangeResult
        class Failed(val message: String) : RangeResult
    }

    private fun validatorFor(item: DownloadItem): String? =
        item.etag?.takeIf { it.isNotBlank() && !it.startsWith("W/") }
            ?: item.lastModified?.takeIf { it.isNotBlank() && item.restarts == 0 }

    suspend fun download(
        context: Context,
        initialItem: DownloadItem,
        target: DownloadTarget,
        plannedParts: Int,
        maxConnections: Int,
        speedLimiter: SpeedLimiter
    ): ParallelOutcome {
        var item = initialItem
        val id = item.id
        val totalBytes = item.totalBytes
        val ranges: MutableList<PartRange> = PartMap.decode(item.partMap, totalBytes)
            ?: PartMap.equal(totalBytes, plannedParts, item.completedPartsMask, item.partOffsets)
        val lock = Any()
        val validator = validatorFor(item)
        val host = item.url.toHttpUrlOrNull()?.host.orEmpty()
        val autoConnections = DownloadAutoTuner.isAuto(maxConnections)
        val autoSplit = DownloadAutoTuner.isAuto(item.splitParts)
        val connectionCap = if (autoConnections) DownloadAutoTuner.connectionCeiling(context) else maxConnections.coerceAtLeast(1)
        val startConnections = if (autoConnections) DownloadAutoTuner.startingConnections(context, host, connectionCap) else connectionCap
        val minSplitBytes = if (autoSplit) DownloadAutoTuner.MIN_AUTO_PART_BYTES else MIN_SPLIT_BYTES
        val ceiling = AtomicInteger(startConnections)
        val maxSafe = AtomicInteger(connectionCap)
        val rateLimitUntil = AtomicLong(0L)
        val fatal = AtomicReference<String?>(null)
        val restart = AtomicReference<String?>(null)
        val fallback = AtomicBoolean(false)
        val lastError = AtomicReference<String?>(null)
        val activeWorkers = AtomicInteger(0)
        val autoController = if (autoConnections) {
            AutoConcurrencyController(ceiling, { maxSafe.get() }) {
                listOf(speedLimiter.currentLimit(), ClintDownloadManager.globalSpeedLimiter.currentLimit())
                    .filter { it > 0L }.minOrNull() ?: 0L
            }
        } else null

        DownloadLog.i(id, "PARTS") {
            val finished = ranges.count { it.complete }
            "Parallel download · ${ranges.size} ranges · " +
                (if (autoConnections) "auto connections, starting at ${ceiling.get()} of up to $connectionCap" else "up to ${ceiling.get()} connections") +
                " over HTTP/1.1" +
                (if (finished > 0) " · $finished ranges already finished" else "") +
                (if (validator != null) " · If-Range: $validator" else " · no validator for safe resume")
        }

        fun stopRequested(): Boolean =
            fatal.get() != null || restart.get() != null || fallback.get() ||
                ClintDownloadManager.pauseRequested.contains(id) || ClintDownloadManager.isRemoved(id)

        fun canSplitMore(): Boolean = !autoSplit || ranges.size < DownloadAutoTuner.MAX_AUTO_PARTS

        fun splitRange(victim: PartRange): PartRange? = synchronized(victim) {
            val position = victim.position
            val remaining = victim.end - position + 1
            if (remaining < minSplitBytes * 2 || !canSplitMore()) null else {
                val middle = position + remaining / 2
                val piece = PartRange(middle, victim.end, 0L).also { it.active = true }
                victim.end = middle - 1
                ranges.add(ranges.indexOf(victim) + 1, piece)
                DownloadLog.d(id, "SPLIT") { "Split range at $middle · ${victim.start}-${victim.end} keeps ${victim.remaining} B, new ${piece.start}-${piece.end}" }
                piece
            }
        }

        fun claim(): PartRange? = synchronized(lock) {
            val now = System.currentTimeMillis()
            val free = ranges.firstOrNull { !it.active && !it.complete && it.retryAt <= now }
            if (free != null) {
                free.active = true
                free
            } else {
                val victim = if (canSplitMore()) {
                    ranges.filter { it.active && it.remaining >= minSplitBytes * 2 }.maxByOrNull { it.remaining }
                } else null
                if (victim == null) null else splitRange(victim)
            }
        }

        fun hasWaitingRanges(): Boolean = synchronized(lock) { ranges.any { !it.active && !it.complete } }
        fun hasWork(): Boolean = synchronized(lock) {
            val splittable = canSplitMore()
            ranges.any { !it.complete && (!it.active || (splittable && it.remaining >= minSplitBytes * 2)) }
        }
        fun allComplete(): Boolean = synchronized(lock) { ranges.all { it.complete } }
        fun snapshot(): List<PartRange> = synchronized(lock) { ranges.toList() }

        fun transfer(range: PartRange, call: Call): RangeResult {
            call.execute().use { response ->
                when {
                    response.code == 429 -> return RangeResult.RateLimited(parseRetryAfterSeconds(response.header("Retry-After")))
                    item.mega != null && response.code == DownloadWorker.MEGA_QUOTA_HTTP_CODE ->
                        return RangeResult.Fatal(context.getString(R.string.mega_error_quota))
                    item.mega != null && response.code == 200 && range.position > 0L &&
                        response.header("Content-Length")?.toLongOrNull() == totalBytes -> return RangeResult.NoRangeSupport
                    item.mega != null && response.code == 200 -> Unit
                    response.code == 200 && validator != null -> return RangeResult.Restart("server sent the whole file for If-Range, so it changed on the server")
                    response.code == 200 -> return RangeResult.NoRangeSupport
                    response.code == 401 || response.code == 403 || response.code == 404 || response.code == 410 ->
                        return RangeResult.Fatal("Server error ${response.code}")
                    response.code != 206 -> return RangeResult.Failed("HTTP ${response.code}")
                }
                val contentRange = ContentRangeInfo.parse(response.header("Content-Range"))
                if (contentRange != null) {
                    val remoteTotal = contentRange.total
                    if (remoteTotal != null && remoteTotal != totalBytes) {
                        return RangeResult.Restart("the file size changed on the server ($remoteTotal instead of $totalBytes bytes)")
                    }
                    if (contentRange.start != range.position) {
                        return RangeResult.Failed("server sent bytes ${contentRange.start}- instead of ${range.position}-")
                    }
                }
                target.openChannel(context).use { channel ->
                    val decryptor = item.mega?.let { MegaCtrDecryptor(it) }
                    val reader = BatchedReader(response.body.byteStream(), ByteArray(BUFFER_BYTES))
                    while (true) {
                        if (stopRequested() || Thread.currentThread().isInterrupted) return RangeResult.Stopped
                        val wanted = minOf(range.remaining, BUFFER_BYTES.toLong()).toInt()
                        if (wanted <= 0) break
                        val batch = minOf(
                            wanted,
                            speedLimiter.batchBytes(BUFFER_BYTES),
                            ClintDownloadManager.globalSpeedLimiter.batchBytes(BUFFER_BYTES)
                        )
                        val read = reader.fill(batch)
                        if (read == -1) break
                        val written = synchronized(range) {
                            val allowed = range.end - range.position + 1
                            if (allowed <= 0L) 0 else {
                                val count = minOf(read.toLong(), allowed).toInt()
                                decryptor?.process(range.position, reader.buffer, 0, count)
                                val chunk = ByteBuffer.wrap(reader.buffer, 0, count)
                                var position = range.position
                                while (chunk.hasRemaining()) position += channel.write(chunk, position)
                                range.done.addAndGet(count.toLong())
                                count
                            }
                        }
                        if (written > 0) {
                            ClintDownloadManager.noteActivity(id)
                            speedLimiter.acquire(written)
                            ClintDownloadManager.globalSpeedLimiter.acquire(written)
                        }
                        if (written < read) break
                    }
                }
            }
            return if (range.complete) RangeResult.Done else RangeResult.Failed("connection closed at byte ${range.position} before ${range.end}")
        }

        suspend fun fetch(range: PartRange): RangeResult {
            val startPosition = range.position
            val endPosition = range.end
            if (startPosition > endPosition) return RangeResult.Done
            val megaRange = item.mega != null
            val request = Request.Builder()
                .url(if (megaRange) MegaRanges.url(item.url, startPosition, endPosition) else item.url)
                .tag(ClintDownloadManager.CallTag::class.java, ClintDownloadManager.CallTag(id))
                .header("User-Agent", item.userAgent)
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Accept-Encoding", "identity")
                .apply {
                    if (!megaRange) header("Range", "bytes=$startPosition-$endPosition")
                    if (item.referer.isNotEmpty()) header("Referer", item.referer)
                    if (item.cookies.isNotEmpty()) header("Cookie", item.cookies)
                    if (validator != null && !megaRange) header("If-Range", validator)
                }
                .build()
            DownloadLog.d(id, "RANGE") { "GET bytes=$startPosition-$endPosition (attempt ${range.failures + 1})" }
            return HostConnectionLimiter.withPermit(host) {
                coroutineScope {
                    val call = ClintDownloadManager.rangedHttpClient.newCall(request)
                    val watcher = launch(Dispatchers.IO) {
                        while (isActive) {
                            if (stopRequested()) {
                                call.cancel()
                                break
                            }
                            delay(MONITOR_TICK_MS)
                        }
                    }
                    try {
                        withContext(DownloadDispatchers.network) { runInterruptible { transfer(range, call) } }
                    } finally {
                        watcher.cancel()
                    }
                }
            }
        }

        fun handle(range: PartRange, result: RangeResult, startedAt: Long, startDone: Long) {
            val now = System.currentTimeMillis()
            when (result) {
                is RangeResult.Done -> {
                    range.failures = 0
                    DownloadLog.i(id, "RANGE") {
                        "Range ${range.start}-${range.end} finished · ${DownloadLog.formatBytes(range.done.get() - startDone)} in ${now - startedAt} ms"
                    }
                }
                is RangeResult.Stopped -> Unit
                is RangeResult.NoRangeSupport -> {
                    DownloadLog.w(id, "RANGE") { "Server answered a range request with the whole file; switching to a single connection" }
                    fallback.set(true)
                }
                is RangeResult.Restart -> restart.compareAndSet(null, result.reason)
                is RangeResult.Fatal -> {
                    DownloadLog.e(id, "RANGE") { "Range ${range.start}-${range.end}: ${result.message}" }
                    fatal.compareAndSet(null, result.message)
                }
                is RangeResult.RateLimited -> {
                    val backoffSeconds = (result.retryAfterSeconds ?: (5L shl range.failures.coerceAtMost(6)))
                        .coerceIn(1L, MAX_RATE_LIMIT_WAIT_SEC)
                    rateLimitUntil.updateAndGet { maxOf(it, now + backoffSeconds * 1000L) }
                    range.failures++
                    range.retryAt = now + backoffSeconds * 1000L
                    lastError.set("Server error 429")
                    if (ceiling.get() > 1) {
                        val shrunk = ceiling.updateAndGet { (it - 1).coerceAtLeast(1) }
                        maxSafe.updateAndGet { minOf(it, shrunk) }
                        DownloadLog.w(id, "RANGE") { "Rate limited (HTTP 429) · waiting ${backoffSeconds}s · connections reduced to $shrunk" }
                    } else {
                        DownloadLog.w(id, "RANGE") { "Rate limited with one connection · waiting ${backoffSeconds}s and keeping the finished ranges" }
                    }
                }
                is RangeResult.Failed -> {
                    range.failures++
                    lastError.set(result.message)
                    if (autoConnections && ceiling.get() > 1) ceiling.updateAndGet { (it - 1).coerceAtLeast(1) }
                    val backoff = minOf(MAX_BACKOFF_MS, 1000L shl (range.failures - 1).coerceIn(0, 5)) + Random.nextLong(0L, 750L)
                    range.retryAt = now + backoff
                    DownloadLog.w(id, "RANGE") {
                        "Range ${range.start}-${range.end} failed: ${result.message} · failure ${range.failures} · back in the pool in ${backoff} ms"
                    }
                }
            }
        }

        val previousTotal = AtomicLong(ranges.sumOf { it.done.get() })
        val lastProgressAt = AtomicLong(System.currentTimeMillis())

        coroutineScope {
            val jobs = mutableListOf<Job>()

            fun spawnWorker() {
                activeWorkers.incrementAndGet()
                jobs += launch(DownloadDispatchers.network) {
                    try {
                        while (isActive && !stopRequested()) {
                            if (activeWorkers.get() > ceiling.get()) break
                            val wait = rateLimitUntil.get() - System.currentTimeMillis()
                            if (wait > 0L) {
                                delay(wait)
                                continue
                            }
                            val range = claim()
                            if (range == null) {
                                if (hasWaitingRanges()) {
                                    delay(250L)
                                    continue
                                }
                                break
                            }
                            val startedAt = System.currentTimeMillis()
                            val startDone = range.done.get()
                            val result = try {
                                fetch(range)
                            } catch (e: CancellationException) {
                                range.active = false
                                throw e
                            } catch (e: Throwable) {
                                if (stopRequested()) RangeResult.Stopped
                                else RangeResult.Failed("${e.javaClass.simpleName}: ${e.message}")
                            }
                            handle(range, result, startedAt, startDone)
                            range.active = false
                        }
                    } finally {
                        activeWorkers.decrementAndGet()
                    }
                }
            }

            repeat(ceiling.get()) { spawnWorker() }

            var lastNotifyBytes = previousTotal.get()
            var lastSpeedBytes = previousTotal.get()
            var lastSpeedTime = System.currentTimeMillis()
            var lastCheckpoint = System.currentTimeMillis()
            var lastRampUp = System.currentTimeMillis()

            while (true) {
                delay(MONITOR_TICK_MS)
                if (ClintDownloadManager.isRemoved(id)) {
                    jobs.forEach { it.cancel() }
                    throw CancellationException("removed")
                }
                val current = snapshot()
                val total = current.sumOf { it.done.get() }
                val now = System.currentTimeMillis()
                if (total != previousTotal.get()) {
                    previousTotal.set(total)
                    lastProgressAt.set(now)
                }
                item = item.copy(
                    bytesDownloaded = total,
                    partProgress = current.map { (it.done.get().toFloat() / it.length).coerceIn(0f, 1f) },
                    activeConnections = activeWorkers.get()
                )
                if (total - lastNotifyBytes >= 65536L) {
                    lastNotifyBytes = total
                    val elapsed = now - lastSpeedTime
                    if (elapsed >= 400L) {
                        item = item.copy(speedBytesPerSec = (total - lastSpeedBytes) * 1000L / elapsed)
                        lastSpeedBytes = total
                        lastSpeedTime = now
                    }
                    DownloadNotificationHelper.showProgressNotificationThrottled(context, item)
                    ClintDownloadManager.publish(item)
                }
                if (now - lastCheckpoint >= CHECKPOINT_INTERVAL_MS) {
                    lastCheckpoint = now
                    val encoded = PartMap.encode(current)
                    runCatching { target.openChannel(context).use { it.force(false) } }
                    ClintDownloadManager.checkpointProgress(id, total, 0L, "", encoded, item.generation)
                }
                if (autoController != null) {
                    autoController.onTick(now, total, canGrow = hasWork(), blocked = rateLimitUntil.get() > now)
                } else if (now - lastRampUp >= RAMP_UP_INTERVAL_MS) {
                    lastRampUp = now
                    if (ceiling.get() < maxSafe.get() && rateLimitUntil.get() <= now) ceiling.incrementAndGet()
                }
                val error = lastError.get()
                if (error != null && now - lastProgressAt.get() >= NO_PROGRESS_LIMIT_MS &&
                    current.any { !it.complete && it.failures >= GIVE_UP_FAILURES }
                ) {
                    DownloadLog.e(id, "PARTS") { "No progress for ${NO_PROGRESS_LIMIT_MS / 1000}s and ranges keep failing; giving up with \"$error\"" }
                    fatal.compareAndSet(null, error)
                }
                if (stopRequested()) {
                    jobs.forEach { it.cancel() }
                    break
                }
                if (allComplete()) break
                val needed = ceiling.get() - activeWorkers.get()
                if (needed > 0 && hasWork()) repeat(needed) { spawnWorker() }
            }
        }

        if (autoConnections) DownloadAutoTuner.rememberConnections(context, host, ceiling.get())
        val finalRanges = snapshot()
        val total = finalRanges.sumOf { it.done.get() }
        item = item.copy(
            bytesDownloaded = total,
            partMap = PartMap.encode(finalRanges),
            completedPartsMask = 0L,
            partOffsets = "",
            partProgress = finalRanges.map { (it.done.get().toFloat() / it.length).coerceIn(0f, 1f) },
            activeConnections = 0
        )
        if (ClintDownloadManager.isRemoved(id)) throw CancellationException("removed")
        if (ClintDownloadManager.pauseRequested.remove(id)) return ParallelOutcome.Paused(item)
        restart.get()?.let { return ParallelOutcome.Restart(item, it) }
        if (fallback.get()) {
            var prefix = 0L
            for (range in finalRanges) {
                prefix += range.done.get()
                if (!range.complete) break
            }
            DownloadLog.i(id, "PARTS") { "Continuing with one connection from byte $prefix (the contiguous downloaded prefix)" }
            return ParallelOutcome.FallbackSingle(
                item.copy(bytesDownloaded = prefix, partMap = "", partProgress = emptyList()),
                (rateLimitUntil.get() - System.currentTimeMillis()).coerceAtLeast(0L)
            )
        }
        fatal.get()?.let { return ParallelOutcome.Failed(item, it) }
        if (total != totalBytes || finalRanges.any { !it.complete }) {
            return ParallelOutcome.Failed(item, context.getString(com.jhaiian.clint.R.string.download_error_size_mismatch, DownloadLog.formatBytes(total), DownloadLog.formatBytes(totalBytes)))
        }
        return ParallelOutcome.Completed(item.copy(partMap = ""))
    }
}
