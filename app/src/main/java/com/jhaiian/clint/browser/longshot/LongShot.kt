package com.jhaiian.clint.browser.longshot

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.Base64
import android.view.View
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.webview.ClintErrorPage
import com.jhaiian.clint.browser.webview.ClintWebView
import com.jhaiian.clint.util.LocaleHelper
import org.json.JSONObject

internal object LongShot {

    const val URL = "clint://longshot"
    private const val SHORT_URL = "clint:longshot"
    private const val SAVE_URL = "$URL/save"
    private const val PREF_BEST = "long_shot_best"
    private const val PREF_SOUND = "long_shot_sound"
    private const val MAX_SCORE = 1_000_000
    private const val PAGE_HEAD = "<!doctype html><html data-clint-page=\"longshot\" class=\"ls-full\""

    private val pagePrefix: String by lazy {
        val head = PAGE_HEAD.substring(0, PAGE_HEAD.length / 3 * 3)
        ClintWebView.ERROR_PAGE_PREFIX + Base64.encodeToString(head.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    @Volatile private var cachedScript: String? = null
    @Volatile private var cachedStyle: String? = null

    fun isLongShotUrl(url: String?): Boolean {
        val value = url?.trim()?.lowercase()?.trimEnd('/') ?: return false
        return value == URL || value == SHORT_URL
    }

    fun isLongShotAddress(url: String?): Boolean {
        val value = url?.trim()?.lowercase() ?: return false
        return value.startsWith(URL) || value.startsWith(SHORT_URL)
    }

    fun matchesSuggestion(query: String): Boolean {
        val value = query.trim().lowercase()
        if (value.length < SHORT_URL.indexOf(':') + 1) return false
        return URL.startsWith(value) || SHORT_URL.startsWith(value)
    }

    fun isSaveRequest(url: String?): Boolean =
        url?.trim()?.lowercase()?.startsWith(SAVE_URL) == true

    fun isPageUri(raw: String?): Boolean = raw?.startsWith(pagePrefix) == true

    fun save(context: Context, url: String) {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = prefs.edit()
        uri.getQueryParameter("best")?.toIntOrNull()?.takeIf { it in 0..MAX_SCORE }?.let {
            if (it > prefs.getInt(PREF_BEST, 0)) editor.putInt(PREF_BEST, it)
        }
        when (uri.getQueryParameter("sound")) {
            "1" -> editor.putBoolean(PREF_SOUND, true)
            "0" -> editor.putBoolean(PREF_SOUND, false)
        }
        editor.apply()
    }

    fun style(context: Context): String =
        cachedStyle ?: readAsset(context, "longshot/long_shot.css").also { cachedStyle = it }

    fun markup(context: Context): String {
        val s = { id: Int -> TextUtils.htmlEncode(context.getString(id)) }
        return "<div class=\"ls-game\" id=\"ls\" hidden>" +
            "<canvas id=\"ls-canvas\" tabindex=\"0\" aria-label=\"${s(R.string.long_shot_canvas_label)}\"></canvas>" +
            "<div class=\"ls-overlay\" id=\"ls-start\">" +
            "<h2>${s(R.string.long_shot_title)}</h2>" +
            "<p>${s(R.string.long_shot_intro)}</p>" +
            "<p class=\"ls-keys\">${s(R.string.long_shot_keys)}</p>" +
            "<button class=\"btn primary\" id=\"ls-play\" type=\"button\">${s(R.string.long_shot_play)}</button>" +
            "</div>" +
            "<div class=\"ls-overlay\" id=\"ls-over\" hidden>" +
            "<h2 id=\"ls-over-title\">${s(R.string.long_shot_game_over)}</h2>" +
            "<div class=\"ls-stats\">" +
            "<span><b id=\"ls-final\">0</b><small>${s(R.string.long_shot_score)}</small></span>" +
            "<span><b id=\"ls-best\">0</b><small>${s(R.string.long_shot_best)}</small></span>" +
            "</div>" +
            "<button class=\"btn primary\" id=\"ls-again\" type=\"button\">${s(R.string.long_shot_play_again)}</button>" +
            "</div>" +
            "<button class=\"ls-sound\" id=\"ls-sound\" type=\"button\" aria-label=\"${s(R.string.long_shot_sound)}\" aria-pressed=\"true\">" +
            "<svg class=\"ls-on\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"$SOUND_ON\"/></svg>" +
            "<svg class=\"ls-off\" viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"$SOUND_OFF\"/></svg>" +
            "</button>" +
            "<div class=\"ls-live\" id=\"ls-live\" aria-live=\"polite\"></div>" +
            "</div>"
    }

    fun script(context: Context, full: Boolean): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val res = context.resources
        val strings = JSONObject()
            .put("best", context.getString(R.string.long_shot_best))
            .put("wind", context.getString(R.string.long_shot_wind))
            .put("noWind", context.getString(R.string.long_shot_no_wind))
            .put("miss", context.getString(R.string.long_shot_miss))
            .put("bullseye", context.getString(R.string.long_shot_bullseye))
            .put("bullseyeMultiplier", context.getString(R.string.long_shot_bullseye_multiplier))
            .put("gameOver", context.getString(R.string.long_shot_game_over))
            .put("newBest", context.getString(R.string.long_shot_new_best))
            .put("mute", context.getString(R.string.long_shot_mute))
            .put("unmute", context.getString(R.string.long_shot_unmute))
            .put("started", context.getString(R.string.long_shot_live_started))
            .put("liveHit", context.getString(R.string.long_shot_live_hit))
            .put("liveBullseye", context.getString(R.string.long_shot_live_bullseye))
            .put("liveMissOne", res.getQuantityString(R.plurals.long_shot_live_miss, 1))
            .put("liveMissMany", res.getQuantityString(R.plurals.long_shot_live_miss, 2))
            .put("liveOver", context.getString(R.string.long_shot_live_over))
        val config = JSONObject()
            .put("strings", strings)
            .put("best", prefs.getInt(PREF_BEST, 0))
            .put("sound", prefs.getBoolean(PREF_SOUND, true))
            .put("full", full)
            .put("lang", LocaleHelper.resolveEffectiveLocale(context).toLanguageTag())
            .put("save", SAVE_URL)
        val js = cachedScript ?: readAsset(context, "longshot/long_shot.js").also { cachedScript = it }
        return "window.LONG_SHOT=" + config.toString().replace("<", "\\u003c") + ";" + js
    }

    fun page(context: Context): String {
        val locale = LocaleHelper.resolveEffectiveLocale(context)
        val dir = if (TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL) "rtl" else "ltr"
        val theme = ClintErrorPage.themeStyle(context)
        val html = PAGE_HEAD + " lang=\"${TextUtils.htmlEncode(locale.toLanguageTag())}\" dir=\"$dir\"><head><meta charset=\"utf-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,viewport-fit=cover\">" +
            "<meta name=\"color-scheme\" content=\"${theme.colorScheme}\">" +
            "<title>${TextUtils.htmlEncode(context.getString(R.string.long_shot_title))}</title>" +
            "<style>${theme.palette}${ClintErrorPage.STYLE}${style(context)}</style></head><body><main>" +
            markup(context) +
            "</main><script>${script(context, true)}</script></body></html>"
        return ClintWebView.ERROR_PAGE_PREFIX + Base64.encodeToString(html.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private fun readAsset(context: Context, path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    private const val SOUND_ON = "M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"
    private const val SOUND_OFF = "M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z"
}
