package com.jhaiian.clint.mediacapture

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class MediaCaptureSort { QUALITY, SIZE, NEWEST, OLDEST }

object MediaCaptureSortState {
    var mode by mutableStateOf(MediaCaptureSort.QUALITY)
}

private val qualityComparator = compareByDescending<DetectedMedia> {
    if (it.kind == MediaKind.VIDEO) (it.width ?: 0) * (it.height ?: 0) else 0
}
    .thenByDescending { if (it.hasAudio == false) 0 else 1 }
    .thenByDescending { it.bandwidthBitsPerSec ?: -1L }
    .thenByDescending { it.sizeBytes ?: it.estimate?.estimatedBytes ?: -1L }

fun sortMediaCapture(items: List<DetectedMedia>, mode: MediaCaptureSort): List<DetectedMedia> {
    if (items.size < 2) return items
    return when (mode) {
        MediaCaptureSort.QUALITY ->
            if (items.first().kind == MediaKind.SUBTITLE) {
                items.sortedBy { it.detectedAtMillis }
            } else {
                items.sortedWith(qualityComparator)
            }
        MediaCaptureSort.SIZE -> items.sortedByDescending { it.sizeBytes ?: it.estimate?.estimatedBytes ?: -1L }
        MediaCaptureSort.NEWEST -> items.sortedByDescending { it.detectedAtMillis }
        MediaCaptureSort.OLDEST -> items.sortedBy { it.detectedAtMillis }
    }
}
