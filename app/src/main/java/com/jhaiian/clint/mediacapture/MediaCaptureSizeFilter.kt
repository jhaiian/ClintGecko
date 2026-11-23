package com.jhaiian.clint.mediacapture

import android.content.SharedPreferences
import com.jhaiian.clint.util.DEFAULT_MEASUREMENT_SYSTEM
import com.jhaiian.clint.util.MEASUREMENT_SYSTEM_DECIMAL
import com.jhaiian.clint.util.PREF_MEASUREMENT_SYSTEM

data class MediaCaptureSizeFilter(
    val enabled: Boolean = false,
    val amount: Int = MEDIA_CAPTURE_MIN_SIZE_DEFAULT_AMOUNT,
    val unit: String = MEDIA_CAPTURE_MIN_SIZE_UNIT_KB
)

fun readMediaCaptureSizeFilter(prefs: SharedPreferences): MediaCaptureSizeFilter {
    val unit = prefs.getString(MEDIA_CAPTURE_MIN_SIZE_UNIT_PREF, MEDIA_CAPTURE_MIN_SIZE_UNIT_KB)
    return MediaCaptureSizeFilter(
        enabled = prefs.getBoolean(MEDIA_CAPTURE_MIN_SIZE_ENABLED_PREF, false),
        amount = prefs.getInt(MEDIA_CAPTURE_MIN_SIZE_AMOUNT_PREF, MEDIA_CAPTURE_MIN_SIZE_DEFAULT_AMOUNT)
            .coerceIn(1, MEDIA_CAPTURE_MIN_SIZE_MAX_AMOUNT),
        unit = if (unit == MEDIA_CAPTURE_MIN_SIZE_UNIT_MB) MEDIA_CAPTURE_MIN_SIZE_UNIT_MB else MEDIA_CAPTURE_MIN_SIZE_UNIT_KB
    )
}

fun writeMediaCaptureSizeFilter(prefs: SharedPreferences, filter: MediaCaptureSizeFilter) {
    prefs.edit()
        .putBoolean(MEDIA_CAPTURE_MIN_SIZE_ENABLED_PREF, filter.enabled)
        .putInt(MEDIA_CAPTURE_MIN_SIZE_AMOUNT_PREF, filter.amount)
        .putString(MEDIA_CAPTURE_MIN_SIZE_UNIT_PREF, filter.unit)
        .apply()
}

fun mediaCaptureMinBytes(filter: MediaCaptureSizeFilter, prefs: SharedPreferences): Long {
    if (!filter.enabled) return 0L
    val decimal = prefs.getString(PREF_MEASUREMENT_SYSTEM, DEFAULT_MEASUREMENT_SYSTEM) == MEASUREMENT_SYSTEM_DECIMAL
    val kb = if (decimal) 1000L else 1024L
    val multiplier = if (filter.unit == MEDIA_CAPTURE_MIN_SIZE_UNIT_MB) kb * kb else kb
    return filter.amount.toLong() * multiplier
}

fun optionPassesMinSize(option: MediaCaptureOption, minBytes: Long): Boolean {
    if (minBytes <= 0L) return true
    val bytes = optionEstimatedBytes(option) ?: return true
    return bytes >= minBytes
}

fun mediaPassesMinSize(media: DetectedMedia, minBytes: Long): Boolean {
    if (minBytes <= 0L || media.kind == MediaKind.SUBTITLE) return true
    val bytes = mediaEstimatedBytes(media) ?: return true
    return bytes >= minBytes
}
