package com.jhaiian.clint.mediacapture

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object MediaCaptureStore {

    private const val SEGMENT_SPAM_WINDOW_MILLIS = 15_000L
    private const val SEGMENT_SPAM_THRESHOLD = 5
    private const val MAX_SEEN_URLS = 2048
    private const val MAX_TEMPLATES = 512
    private const val MAX_ITEMS = 256

    private data class TemplateWindow(val windowStartMillis: Long, val count: Int)

    private class TabState {
        val items = MutableStateFlow<List<DetectedMedia>>(emptyList())
        val seenUrls = LinkedHashSet<String>()
        val templateCounts = ConcurrentHashMap<String, TemplateWindow>()
        val generation = AtomicInteger(0)
        @Volatile var pageUrl: String? = null
    }

    private val tabs = ConcurrentHashMap<String, TabState>()

    private fun stateFor(tabId: String): TabState = tabs.getOrPut(tabId) { TabState() }

    fun observe(tabId: String): StateFlow<List<DetectedMedia>> = stateFor(tabId).items.asStateFlow()

    fun countForTab(tabId: String?): Int = if (tabId == null) 0 else tabs[tabId]?.items?.value?.size ?: 0

    fun markSeen(tabId: String, url: String): Boolean {
        val seen = stateFor(tabId).seenUrls
        return synchronized(seen) {
            val added = seen.add(url)
            if (added && seen.size > MAX_SEEN_URLS) {
                val oldest = seen.iterator()
                oldest.next()
                oldest.remove()
            }
            added
        }
    }

    fun contains(tabId: String, url: String): Boolean =
        tabs[tabId]?.items?.value?.any { it.url == url || it.groupUrl == url } == true

    fun updatePageUrl(tabId: String, url: String?) {
        stateFor(tabId).pageUrl = url
    }

    fun pageUrlFor(tabId: String): String? = tabs[tabId]?.pageUrl

    fun registerAndCheckSpam(tabId: String, templateKey: String, nowMillis: Long): Boolean {
        val counts = stateFor(tabId).templateCounts
        if (counts.size > MAX_TEMPLATES) {
            counts.entries.removeAll { nowMillis - it.value.windowStartMillis > SEGMENT_SPAM_WINDOW_MILLIS }
        }
        var isSpam = false
        counts.compute(templateKey) { _, existing ->
            val window = if (existing == null || nowMillis - existing.windowStartMillis > SEGMENT_SPAM_WINDOW_MILLIS) {
                TemplateWindow(nowMillis, 1)
            } else {
                TemplateWindow(existing.windowStartMillis, existing.count + 1)
            }
            isSpam = window.count > SEGMENT_SPAM_THRESHOLD
            window
        }
        return isSpam
    }

    private fun dedupeKey(media: DetectedMedia): String =
        if (media.groupUrl != null && media.url == media.groupUrl) {
            "${media.url}#${media.width}x${media.height}#${media.bandwidthBitsPerSec}#${media.mimeType}#${media.representationId}#${media.language}"
        } else {
            media.url
        }

    fun add(tabId: String, media: DetectedMedia): Boolean {
        val state = stateFor(tabId)
        val key = dedupeKey(media)
        var added = false
        state.items.update { current ->
            added = false
            when {
                current.any { dedupeKey(it) == key } -> current
                isDirectHlsPlaylist(media) && current.any { isHlsVariant(it) && it.url == media.url } -> current
                isHlsVariant(media) -> {
                    val direct = current.firstOrNull { isDirectHlsPlaylist(it) && it.url == media.url }
                    if (direct == null) {
                        if (current.size >= MAX_ITEMS) {
                            current
                        } else {
                            added = true
                            current + media
                        }
                    } else {
                        added = true
                        val merged = media.copy(
                            width = media.width ?: direct.width,
                            height = media.height ?: direct.height,
                            durationSeconds = media.durationSeconds ?: direct.durationSeconds,
                            isLive = media.isLive || direct.isLive,
                            estimate = media.estimate ?: direct.estimate
                        )
                        current.map { if (it.id == direct.id) merged else it }
                    }
                }
                current.size >= MAX_ITEMS -> current
                else -> {
                    added = true
                    current + media
                }
            }
        }
        return added
    }

    private fun isDirectHlsPlaylist(media: DetectedMedia): Boolean =
        media.kind == MediaKind.VIDEO &&
            media.format.equals("HLS", ignoreCase = true) &&
            media.groupUrl != null &&
            media.url == media.groupUrl

    private fun isHlsVariant(media: DetectedMedia): Boolean =
        media.kind == MediaKind.VIDEO &&
            media.format.equals("HLS", ignoreCase = true) &&
            media.groupUrl != null &&
            media.url != media.groupUrl

    fun generation(tabId: String): Int = tabs[tabId]?.generation?.get() ?: -1

    fun beginGeneration(tabId: String): Int = stateFor(tabId).generation.get()

    fun clearForTab(tabId: String) {
        val state = tabs[tabId] ?: return
        synchronized(state.seenUrls) { state.seenUrls.clear() }
        state.templateCounts.clear()
        state.generation.incrementAndGet()
        state.items.value = emptyList()
    }

    fun removeTab(tabId: String) {
        tabs.remove(tabId)
    }
}
