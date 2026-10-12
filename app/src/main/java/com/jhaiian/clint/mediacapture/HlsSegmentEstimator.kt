package com.jhaiian.clint.mediacapture

import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.mediacapture.download.ResolvedTrack
import com.jhaiian.clint.mediacapture.download.SegmentSpec
import com.jhaiian.clint.mediacapture.download.StreamRequestHeaders
import okhttp3.Request

object HlsSegmentEstimator {

    fun estimate(media: DetectedMedia, track: ResolvedTrack, pageUrl: String): HlsSegmentEstimate {
        val headers = media.requestHeaders
        val segmentCount = track.segments.size + if (track.initSegment != null) 1 else 0
        val duration = track.durationSeconds ?: media.durationSeconds
        val bandwidth = media.bandwidthBitsPerSec
        val byteRangeTotal = sumByteRangeLengths(track.segments)?.let { total ->
            total + (track.initSegment?.byteRangeLength ?: 0L)
        }
        val distinctUrls = track.segments.map { it.url }.distinct()
        val singleFileTotal = if (distinctUrls.size == 1 && byteRangeTotal != null) {
            fetchSegmentContentLength(distinctUrls[0], pageUrl, headers)
        } else null
        val sampledTotal = if (singleFileTotal == null && byteRangeTotal == null) {
            if (distinctUrls.size < track.segments.size) {
                sampleAverageContentLengthPerUrl(distinctUrls, pageUrl, headers)
                    ?.let { it * distinctUrls.size }
            } else {
                sampleAverageContentLengthPerUrl(track.segments.map { it.url }, pageUrl, headers)
                    ?.let { it * segmentCount }
            }
        } else null
        val estimatedBytes = singleFileTotal ?: byteRangeTotal ?: sampledTotal
            ?: if (duration != null && duration > 0.0 && bandwidth != null && bandwidth > 0L) {
                ((duration * bandwidth) / 8.0).toLong()
            } else null
        return HlsSegmentEstimate(segmentCount, estimatedBytes, track.containerHintExtension)
    }

    private fun fetchSegmentContentLength(
        url: String,
        pageUrl: String,
        extraHeaders: Map<String, String>
    ): Long? = runCatching {
        val builder = Request.Builder().url(url).head()
        StreamRequestHeaders.apply(builder, url, pageUrl, "", "", extraHeaders)
        ClintDownloadManager.httpClient.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) return@use null
            resp.header("Content-Length")?.toLongOrNull()?.takeIf { it > 0L }
        }
    }.getOrNull()

    private fun sumByteRangeLengths(segments: List<SegmentSpec>): Long? {
        if (segments.isEmpty()) return null
        var total = 0L
        for (seg in segments) {
            val length = seg.byteRangeLength ?: return null
            total += length
        }
        return total
    }

    private fun sampleAverageContentLengthPerUrl(
        urls: List<String>,
        pageUrl: String,
        extraHeaders: Map<String, String>
    ): Long? {
        if (urls.isEmpty()) return null
        val sampleCount = minOf(3, urls.size)
        val step = maxOf(1, urls.size / sampleCount)
        val indices = (0 until urls.size step step).take(sampleCount)
        var totalBytes = 0L
        var counted = 0
        for (idx in indices) {
            val size = fetchSegmentContentLength(urls[idx], pageUrl, extraHeaders) ?: continue
            totalBytes += size
            counted++
        }
        return if (counted > 0) totalBytes / counted else null
    }
}
