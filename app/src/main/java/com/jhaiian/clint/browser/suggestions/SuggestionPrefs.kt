package com.jhaiian.clint.browser.suggestions

import android.content.SharedPreferences

const val PREF_SUGGESTIONS_SHOW_HISTORY = "suggestions_show_history"
const val PREF_SUGGESTIONS_SHOW_BOOKMARKS = "suggestions_show_bookmarks"
const val PREF_SUGGESTIONS_STYLE = "suggestions_style"
const val PREF_SUGGESTIONS_SHOW_URLS = "suggestions_show_urls"
const val PREF_SUGGESTIONS_URL_MODE = "suggestions_url_mode"
const val PREF_SUGGESTIONS_HISTORY_LIMIT = "suggestions_history_limit"
const val PREF_SUGGESTIONS_BOOKMARK_LIMIT = "suggestions_bookmark_limit"
const val PREF_SUGGESTIONS_ORDER = "suggestions_order"
const val PREF_SUGGESTIONS_TEXT_SIZE = "suggestions_text_size"
const val PREF_SUGGESTIONS_SHOW_HEADERS = "suggestions_show_headers"
const val PREF_SUGGESTIONS_SHOW_FILL = "suggestions_show_fill"
const val PREF_SUGGESTIONS_SHOW_TABS = "suggestions_show_tabs"
const val PREF_SUGGESTIONS_SHOW_FAVORITES = "suggestions_show_favorites"
const val PREF_SUGGESTIONS_SHOW_PASTE = "suggestions_show_paste"
const val PREF_SUGGESTIONS_SHOW_ENGINES = "suggestions_show_engines"

const val SUGGESTIONS_STYLE_COMPACT = "compact"
const val SUGGESTIONS_STYLE_CARDS = "cards"

const val SUGGESTIONS_URL_DOMAIN = "domain"
const val SUGGESTIONS_URL_FULL = "full"
const val SUGGESTIONS_URL_OFF = "off"

const val SUGGESTIONS_ORDER_BOOKMARKS = "bookmarks"
const val SUGGESTIONS_ORDER_HISTORY = "history"
const val SUGGESTIONS_ORDER_WEB = "web"
const val SUGGESTIONS_ORDER_MIXED = "mixed"

const val SUGGESTIONS_TEXT_SMALL = "small"
const val SUGGESTIONS_TEXT_DEFAULT = "default"
const val SUGGESTIONS_TEXT_LARGE = "large"

const val SUGGESTIONS_HISTORY_LIMIT_DEFAULT = 5
const val SUGGESTIONS_BOOKMARK_LIMIT_DEFAULT = 5
val SUGGESTIONS_LIMIT_OPTIONS = listOf(3, 5, 10, 20)

internal data class SuggestionDisplay(
    val compact: Boolean,
    val urlMode: String,
    val order: String,
    val textScale: Float,
    val showHeaders: Boolean,
    val showFill: Boolean,
    val showFavorites: Boolean,
    val showPaste: Boolean,
    val showEngines: Boolean
)

internal fun readSuggestionUrlMode(prefs: SharedPreferences): String =
    prefs.getString(PREF_SUGGESTIONS_URL_MODE, null)
        ?: if (prefs.getBoolean(PREF_SUGGESTIONS_SHOW_URLS, true)) SUGGESTIONS_URL_DOMAIN else SUGGESTIONS_URL_OFF

internal fun readSuggestionDisplay(prefs: SharedPreferences) = SuggestionDisplay(
    compact = prefs.getString(PREF_SUGGESTIONS_STYLE, SUGGESTIONS_STYLE_COMPACT) != SUGGESTIONS_STYLE_CARDS,
    urlMode = readSuggestionUrlMode(prefs),
    order = prefs.getString(PREF_SUGGESTIONS_ORDER, SUGGESTIONS_ORDER_BOOKMARKS) ?: SUGGESTIONS_ORDER_BOOKMARKS,
    textScale = when (prefs.getString(PREF_SUGGESTIONS_TEXT_SIZE, SUGGESTIONS_TEXT_DEFAULT)) {
        SUGGESTIONS_TEXT_SMALL -> 0.9f
        SUGGESTIONS_TEXT_LARGE -> 1.15f
        else -> 1f
    },
    showHeaders = prefs.getBoolean(PREF_SUGGESTIONS_SHOW_HEADERS, true),
    showFill = prefs.getBoolean(PREF_SUGGESTIONS_SHOW_FILL, true),
    showFavorites = prefs.getBoolean(PREF_SUGGESTIONS_SHOW_FAVORITES, true),
    showPaste = prefs.getBoolean(PREF_SUGGESTIONS_SHOW_PASTE, true),
    showEngines = prefs.getBoolean(PREF_SUGGESTIONS_SHOW_ENGINES, true)
)
