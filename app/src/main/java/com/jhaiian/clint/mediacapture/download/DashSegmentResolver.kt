package com.jhaiian.clint.mediacapture.download

import android.util.Xml
import com.jhaiian.clint.downloads.ClintDownloadManager
import java.net.URI
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser

object DashSegmentResolver {

    private data class SegTemplate(
        var media: String? = null,
        var initialization: String? = null,
        var startNumber: Long = 1L,
        var timescale: Long = 1L,
        var duration: Long? = null
    )

    private enum class Level { MPD, PERIOD, ADAPTATION, REPRESENTATION }

    private fun resolveUrl(base: String, relative: String): String {
        val trimmed = relative.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        return try {
            URI(base).resolve(trimmed).toString()
        } catch (_: Exception) {
            trimmed
        }
    }

    private fun parseIso8601Duration(s: String): Double? {
        val m = Regex("""PT(?:([\d.]+)H)?(?:([\d.]+)M)?(?:([\d.]+)S)?""").matchEntire(s.trim()) ?: return null
        val h = m.groupValues[1].toDoubleOrNull() ?: 0.0
        val mi = m.groupValues[2].toDoubleOrNull() ?: 0.0
        val sec = m.groupValues[3].toDoubleOrNull() ?: 0.0
        val total = h * 3600.0 + mi * 60.0 + sec
        return total.takeIf { it > 0.0 }
    }

    private fun substitute(template: String, repId: String?, bandwidth: Long?, number: Long?, time: Long?): String {
        var out = template
        out = out.replace("\$RepresentationID\$", repId ?: "")
        out = out.replace("\$Bandwidth\$", bandwidth?.toString() ?: "")
        val numberFmt = Regex("""${'$'}Number(%0(\d+)d)?${'$'}""")
        out = numberFmt.replace(out) { m ->
            if (number == null) m.value else {
                val width = m.groupValues[2].toIntOrNull()
                if (width != null) number.toString().padStart(width, '0') else number.toString()
            }
        }
        val timeFmt = Regex("""${'$'}Time(%0(\d+)d)?${'$'}""")
        out = timeFmt.replace(out) { m ->
            if (time == null) m.value else {
                val width = m.groupValues[2].toIntOrNull()
                if (width != null) time.toString().padStart(width, '0') else time.toString()
            }
        }
        out = out.replace("\$\$", "\$")
        return out
    }

    fun fetch(
        mpdUrl: String,
        referer: String,
        cookies: String,
        userAgent: String,
        kind: TrackKind,
        targetWidth: Int?,
        targetHeight: Int?,
        targetBandwidth: Long?,
        extraHeaders: Map<String, String> = emptyMap(),
        targetRepresentationId: String? = null
    ): ResolvedTrack? {
        val builder = Request.Builder().url(mpdUrl)
        StreamRequestHeaders.apply(builder, mpdUrl, referer, cookies, userAgent, extraHeaders)

        val text = try {
            ClintDownloadManager.httpClient.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return null
                resp.body.string()
            }
        } catch (_: Exception) {
            return null
        }

        return try {
            parse(text, mpdUrl, kind, targetWidth, targetHeight, targetBandwidth, targetRepresentationId)
        } catch (_: Exception) {
            null
        }
    }

    private fun parse(
        text: String,
        documentUrl: String,
        kind: TrackKind,
        targetWidth: Int?,
        targetHeight: Int?,
        targetBandwidth: Long?,
        targetRepresentationId: String? = null
    ): ResolvedTrack? {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(java.io.StringReader(text))

        var mpdBase = documentUrl
        var periodBase = documentUrl
        var adaptationBase = documentUrl
        var repBase: String? = null
        var level = Level.MPD
        var mpdDuration: Double? = null
        var mpdIsLive = false
        var mpdUpdatePeriod: Double? = null

        var adaptationTemplate: SegTemplate? = null
        var adaptationTimeline: List<Triple<Long, Long, Int>>? = null
        var repTemplate: SegTemplate? = null
        var repTimeline: List<Triple<Long, Long, Int>>? = null
        var timelineBuilder: MutableList<Triple<Long, Long, Int>>? = null

        var matchedRepId: String? = null
        var matchedBandwidth: Long? = null
        var inMatchedRep = false
        var textBuffer = StringBuilder()
        val collectedTracks = mutableListOf<ResolvedTrack>()

        var segmentListInit: String? = null
        val segmentListUrls = mutableListOf<String>()
        var inSegmentList = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    textBuffer = StringBuilder()
                    when (parser.name) {
                        "MPD" -> {
                            mpdDuration = parser.getAttributeValue(null, "mediaPresentationDuration")
                                ?.let(::parseIso8601Duration)
                            mpdIsLive = parser.getAttributeValue(null, "type") == "dynamic"
                            mpdUpdatePeriod = parser.getAttributeValue(null, "minimumUpdatePeriod")
                                ?.let(::parseIso8601Duration)
                        }
                        "Period" -> {
                            level = Level.PERIOD
                            periodBase = mpdBase
                        }
                        "AdaptationSet" -> {
                            level = Level.ADAPTATION
                            adaptationBase = periodBase
                            adaptationTemplate = null
                            adaptationTimeline = null
                        }
                        "Representation" -> {
                            level = Level.REPRESENTATION
                            val width = parser.getAttributeValue(null, "width")?.toIntOrNull()
                            val height = parser.getAttributeValue(null, "height")?.toIntOrNull()
                            val bandwidth = parser.getAttributeValue(null, "bandwidth")?.toLongOrNull()
                            val id = parser.getAttributeValue(null, "id")
                            repBase = null
                            repTemplate = null
                            repTimeline = null
                            segmentListInit = null
                            segmentListUrls.clear()
                            inMatchedRep = if (targetRepresentationId != null) {
                                id == targetRepresentationId
                            } else {
                                width == targetWidth && height == targetHeight && bandwidth == targetBandwidth
                            }
                            if (inMatchedRep) {
                                matchedRepId = id
                                matchedBandwidth = bandwidth
                            }
                        }
                        "SegmentTemplate" -> {
                            val t = SegTemplate(
                                media = parser.getAttributeValue(null, "media"),
                                initialization = parser.getAttributeValue(null, "initialization"),
                                startNumber = parser.getAttributeValue(null, "startNumber")?.toLongOrNull() ?: 1L,
                                timescale = parser.getAttributeValue(null, "timescale")?.toLongOrNull() ?: 1L,
                                duration = parser.getAttributeValue(null, "duration")?.toLongOrNull()
                            )
                            if (level == Level.REPRESENTATION) repTemplate = t else adaptationTemplate = t
                            timelineBuilder = mutableListOf()
                        }
                        "S" -> {
                            val tAttr = parser.getAttributeValue(null, "t")?.toLongOrNull()
                            val dAttr = parser.getAttributeValue(null, "d")?.toLongOrNull() ?: 0L
                            val rAttr = parser.getAttributeValue(null, "r")?.toIntOrNull() ?: 0
                            val tb = timelineBuilder
                            if (tb != null) {
                                val startT = tAttr ?: (tb.lastOrNull()?.let { it.first + it.second * (it.third + 1) } ?: 0L)
                                tb.add(Triple(startT, dAttr, rAttr))
                            }
                        }
                        "Initialization" -> {
                            if (level == Level.REPRESENTATION && inMatchedRep) {
                                parser.getAttributeValue(null, "sourceURL")?.let { segmentListInit = it }
                            }
                        }
                        "SegmentList" -> {
                            inSegmentList = level == Level.REPRESENTATION && inMatchedRep
                        }
                        "SegmentURL" -> {
                            if (inSegmentList) {
                                parser.getAttributeValue(null, "media")?.let { segmentListUrls.add(it) }
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    textBuffer.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "BaseURL" -> {
                            val value = textBuffer.toString().trim()
                            if (value.isNotEmpty()) {
                                when (level) {
                                    Level.MPD -> mpdBase = resolveUrl(documentUrl, value)
                                    Level.PERIOD -> periodBase = resolveUrl(mpdBase, value)
                                    Level.ADAPTATION -> adaptationBase = resolveUrl(periodBase, value)
                                    Level.REPRESENTATION -> repBase = resolveUrl(adaptationBase, value)
                                }
                            }
                        }
                        "SegmentTemplate" -> {
                            if (level == Level.REPRESENTATION) {
                                repTimeline = timelineBuilder?.toList()
                            } else {
                                adaptationTimeline = timelineBuilder?.toList()
                            }
                            timelineBuilder = null
                        }
                        "SegmentList" -> inSegmentList = false
                        "Representation" -> {
                            if (inMatchedRep) {
                                val baseUrl = repBase ?: adaptationBase
                                val effectiveTemplate = SegTemplate(
                                    media = repTemplate?.media ?: adaptationTemplate?.media,
                                    initialization = repTemplate?.initialization ?: adaptationTemplate?.initialization,
                                    startNumber = repTemplate?.startNumber ?: adaptationTemplate?.startNumber ?: 1L,
                                    timescale = repTemplate?.timescale ?: adaptationTemplate?.timescale ?: 1L,
                                    duration = repTemplate?.duration ?: adaptationTemplate?.duration
                                )
                                val effectiveTimeline = repTimeline ?: adaptationTimeline
                                val result = buildTrack(
                                    kind, baseUrl, matchedRepId, matchedBandwidth, effectiveTemplate,
                                    effectiveTimeline, mpdDuration, segmentListInit, segmentListUrls,
                                    mpdIsLive, mpdUpdatePeriod
                                )
                                if (result != null) collectedTracks += result
                            }
                            inMatchedRep = false
                            level = Level.ADAPTATION
                        }
                        "AdaptationSet" -> level = Level.PERIOD
                        "Period" -> level = Level.MPD
                    }
                }
            }
            eventType = parser.next()
        }
        if (collectedTracks.isEmpty()) return null
        val first = collectedTracks.first()
        if (collectedTracks.size == 1) return first
        val mergedSegments = collectedTracks.flatMap { it.segments }
        val mergedDuration = collectedTracks.mapNotNull { it.durationSeconds }
            .takeIf { it.isNotEmpty() }?.sum() ?: first.durationSeconds
        return first.copy(segments = mergedSegments, durationSeconds = mergedDuration)
    }

    private fun buildTrack(
        kind: TrackKind,
        baseUrl: String,
        repId: String?,
        bandwidth: Long?,
        template: SegTemplate,
        timeline: List<Triple<Long, Long, Int>>?,
        mpdDuration: Double?,
        segmentListInit: String?,
        segmentListUrls: List<String>,
        isLive: Boolean = false,
        updatePeriod: Double? = null
    ): ResolvedTrack? {
        if (segmentListUrls.isNotEmpty()) {
            val init = segmentListInit?.let { SegmentSpec(url = resolveUrl(baseUrl, it)) }
            val segs = segmentListUrls.map { SegmentSpec(url = resolveUrl(baseUrl, it)) }
            return ResolvedTrack(kind, init, segs, mpdDuration, "mp4", isLive, updatePeriod)
        }

        val mediaTemplate = template.media ?: return null
        val init = template.initialization?.let {
            SegmentSpec(url = resolveUrl(baseUrl, substitute(it, repId, bandwidth, null, null)))
        }

        val segments = mutableListOf<SegmentSpec>()
        if (!timeline.isNullOrEmpty()) {
            var number = template.startNumber
            for ((startT, d, r) in timeline) {
                var t = startT
                var repeatCount = r
                while (repeatCount >= 0) {
                    val url = resolveUrl(baseUrl, substitute(mediaTemplate, repId, bandwidth, number, t))
                    segments += SegmentSpec(url = url, sequenceNumber = number)
                    t += d
                    number++
                    repeatCount--
                }
            }
        } else {
            val segDuration = template.duration
            if (segDuration == null || segDuration <= 0L || mpdDuration == null) return null
            val segSeconds = segDuration.toDouble() / template.timescale.toDouble()
            val count = Math.ceil(mpdDuration / segSeconds).toLong().coerceAtLeast(1L)
            for (i in 0 until count) {
                val number = template.startNumber + i
                val url = resolveUrl(baseUrl, substitute(mediaTemplate, repId, bandwidth, number, null))
                segments += SegmentSpec(url = url, sequenceNumber = number)
            }
        }

        if (segments.isEmpty()) return null
        return ResolvedTrack(kind, init, segments, mpdDuration, "mp4", isLive, updatePeriod)
    }
}
