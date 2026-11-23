package com.jhaiian.clint.browser.webview

import android.content.Context
import androidx.preference.PreferenceManager
import org.mozilla.geckoview.GeckoPreferenceController

object PerformancePrefs {

    const val REDUCE_BLUR = "perf_reduce_blur"
    const val DISABLE_WEBGL = "perf_disable_webgl"
    const val LIMIT_FRAME_RATE = "perf_limit_frame_rate"
    const val REDUCE_MOTION = "perf_reduce_motion"
    const val STOP_ANIMATED_IMAGES = "perf_stop_animated_images"

    const val DEFAULT_ENABLED = false

    private const val FRAME_RATE_LIMIT = 30

    private class Rule(val key: String, val pref: String, val value: Any)

    private val rules = listOf(
        Rule(REDUCE_BLUR, "layout.css.backdrop-filter.enabled", false),
        Rule(DISABLE_WEBGL, "webgl.disabled", true),
        Rule(LIMIT_FRAME_RATE, "layout.frame_rate", FRAME_RATE_LIMIT),
        Rule(REDUCE_MOTION, "ui.prefersReducedMotion", 1),
        Rule(STOP_ANIMATED_IMAGES, "image.animation_mode", "none")
    )

    fun isEnabled(context: Context, key: String): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, DEFAULT_ENABLED)

    fun applyGeckoPrefs(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        rules.forEach { rule ->
            if (prefs.getBoolean(rule.key, DEFAULT_ENABLED)) write(rule)
        }
    }

    fun setEnabled(context: Context, key: String, enabled: Boolean) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        if (enabled) editor.putBoolean(key, true) else editor.remove(key)
        editor.apply()
        val rule = rules.firstOrNull { it.key == key } ?: return
        if (enabled) write(rule) else clear(rule)
    }

    private fun write(rule: Rule) {
        val value = rule.value
        runCatching {
            when (value) {
                is Boolean -> GeckoPreferenceController.setGeckoPref(rule.pref, value, GeckoPreferenceController.PREF_BRANCH_USER)
                is Int -> GeckoPreferenceController.setGeckoPref(rule.pref, value, GeckoPreferenceController.PREF_BRANCH_USER)
                is String -> GeckoPreferenceController.setGeckoPref(rule.pref, value, GeckoPreferenceController.PREF_BRANCH_USER)
                else -> Unit
            }
        }
    }

    private fun clear(rule: Rule) {
        runCatching { GeckoPreferenceController.clearGeckoUserPref(rule.pref) }
    }
}
