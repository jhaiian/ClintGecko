package com.jhaiian.clint.browser.find

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jhaiian.clint.browser.webview.ClintWebView

internal const val PREF_FIND_MATCH_CASE = "find_match_case"
internal const val PREF_FIND_WHOLE_WORD = "find_whole_word"
internal const val PREF_FIND_LINKS_ONLY = "find_links_only"
internal const val PREF_FIND_REGEX = "find_regex"
internal const val PREF_FIND_BAR_TOP = "find_bar_top"
internal const val PREF_FIND_DIM_PAGE = "find_dim_page"
internal const val PREF_FIND_REMEMBER_OPTIONS = "find_remember_options"
internal const val PREF_FIND_SAVE_RECENT = "find_save_recent"
internal const val PREF_FIND_ADDRESS_BAR_SUGGESTION = "find_address_bar_suggestion"
internal const val PREF_FIND_SCROLL_MARKERS = "find_scroll_markers"
internal const val PREF_FIND_RECENT = "find_recent"

internal const val FIND_MATCH_RGB = 0xFFD54F
internal const val FIND_CURRENT_RGB = 0x38D878

internal data class FindHit(val before: String, val match: String, val after: String)

internal class FindUiState {
    var open by mutableStateOf(false)
    var query by mutableStateOf("")
    var regex by mutableStateOf(false)
    var matchCase by mutableStateOf(false)
    var wholeWord by mutableStateOf(false)
    var linksOnly by mutableStateOf(false)
    var current by mutableIntStateOf(0)
    var total by mutableIntStateOf(0)
    var invalidPattern by mutableStateOf(false)
    var searching by mutableStateOf(false)
    var listOpen by mutableStateOf(false)
    var hits by mutableStateOf<List<FindHit>>(emptyList())
    var hitTotal by mutableIntStateOf(0)
    var ticks by mutableStateOf<List<Float>>(emptyList())
    var recent by mutableStateOf<List<String>>(emptyList())
    var barAtTop by mutableStateOf(false)
    var showScrollMarkers by mutableStateOf(true)
    var optionsOpen by mutableStateOf(false)
    var barHeightPx by mutableIntStateOf(0)

    var voicePending = false
    var target: ClintWebView? = null
    var scanToken = 0
    var jumpToken = 0
    val handler = Handler(Looper.getMainLooper())
    var searchRunnable: Runnable? = null
    var scanRunnable: Runnable? = null
}
