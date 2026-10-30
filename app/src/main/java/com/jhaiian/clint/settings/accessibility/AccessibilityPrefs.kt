package com.jhaiian.clint.settings.accessibility

import android.content.SharedPreferences

object AccessibilityPrefs {

    const val PREF_AUTO_FONT_SIZE = "accessibility_auto_font_size"
    const val PREF_FONT_SIZE_PERCENT = "accessibility_font_size_percent"
    const val PREF_FORCE_ZOOM = "accessibility_force_zoom"

    const val DEFAULT_AUTO_FONT_SIZE = true
    const val DEFAULT_FONT_SIZE_PERCENT = 100
    const val DEFAULT_FORCE_ZOOM = false

    const val MIN_FONT_SIZE_PERCENT = 50
    const val MAX_FONT_SIZE_PERCENT = 200
    const val FONT_SIZE_STEP_PERCENT = 5

    fun autoFontSize(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(PREF_AUTO_FONT_SIZE, DEFAULT_AUTO_FONT_SIZE)

    fun forceZoom(prefs: SharedPreferences): Boolean =
        prefs.getBoolean(PREF_FORCE_ZOOM, DEFAULT_FORCE_ZOOM)

    fun fontSizePercent(prefs: SharedPreferences): Int =
        sanitizePercent(prefs.getInt(PREF_FONT_SIZE_PERCENT, DEFAULT_FONT_SIZE_PERCENT))

    fun sanitizePercent(percent: Int): Int {
        val clamped = percent.coerceIn(MIN_FONT_SIZE_PERCENT, MAX_FONT_SIZE_PERCENT)
        val steps = Math.round((clamped - MIN_FONT_SIZE_PERCENT).toFloat() / FONT_SIZE_STEP_PERCENT)
        return MIN_FONT_SIZE_PERCENT + steps * FONT_SIZE_STEP_PERCENT
    }
}
