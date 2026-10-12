package com.jhaiian.clint.mediacapture.download

import com.jhaiian.clint.downloads.ClintDownloadManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.Call
import okhttp3.Request

class StreamCancelledException : Exception()

data class DownloadedSegment(val file: File, val key: HlsKeyInfo?)

object StreamTrackDownloader {

    private const val MAX_CONCURRENT_SEGMENTS = 6
    private const val MAX_SEGMENT_ATTEMPTS = 3

    private data class FetchResult(val bytes: ByteArray?, val reason: String?, val resourceTotalLength: Long? = null)

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
                    FetchResult(null, "HTTP ${resp.code}")
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
        val semaphore = Semaphore(maxConcurrentSegments.coerceIn(1, 8))
        val activeCalls = ConcurrentHashMap.newKeySet<Call>()

        val cancelWatcher = launch(Dispatchers.IO) {
            while (isActive) {
                if (isCancelled()) activeCalls.forEach { it.cancel() }
                delay(200L)
            }
        }

        try {
            val pending = all.indices.filter { !results.containsKey(it) }
            val jobs = pending.map { index ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        if (isCancelled()) throw StreamCancelledException()
                        val seg = all[index]
                        var bytes: ByteArray? = null
                        var lastReason: String? = null
                        var attempt = 0
                        while (attempt < MAX_SEGMENT_ATTEMPTS && bytes == null) {
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
                                attempt++
                                if (attempt < MAX_SEGMENT_ATTEMPTS) StreamBackoff.wait(attempt, isCancelled)
                            }
                        }
                        if (bytes == null) {
                            throw IOException("Failed to fetch segment (${lastReason ?: "unknown error"}), part $index of $total: ${seg.url}")
                        }
                        val segFile = File(workDir, "seg_%05d.part".format(index))
                        FileOutputStream(segFile).use { it.write(bytes) }
                        speedLimiter?.acquire(bytes.size)
                        results[index] = DownloadedSegment(segFile, seg.key)
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

    fun concatenate(segments: List<DownloadedSegment>, outputFile: File, onProgress: ((pct: Int) -> Unit)? = null) {
        outputFile.parentFile?.mkdirs()
        val totalBytes = segments.sumOf { it.file.length() }
        var written = 0L
        var lastPct = -1
        FileOutputStream(outputFile).use { out ->
            for (seg in segments) {
                seg.file.inputStream().use { it.copyTo(out) }
                written += seg.file.length()
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

    fun cleanup(segments: List<DownloadedSegment>) {
        for (seg in segments) {
            runCatching { seg.file.delete() }
            runCatching { File(seg.file.path + ".dec").delete() }
        }
    }
}
