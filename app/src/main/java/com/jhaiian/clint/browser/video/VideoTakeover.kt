package com.jhaiian.clint.browser.video

import android.content.Context
import android.webkit.JavascriptInterface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.delegates.onVideoCandidates
import com.jhaiian.clint.browser.delegates.onVideoDetached
import com.jhaiian.clint.browser.delegates.onVideoState
import com.jhaiian.clint.browser.webview.ClintWebView

object VideoPlayerSettings {
    const val PREF_ENABLED = "video_player_enabled"
    const val PREF_AUTO_PIP = "video_player_auto_pip"
    const val PREF_FIT = "video_player_fit"
    const val PREF_SITE_FULLSCREEN = "video_player_site_fullscreen"
    const val PREF_FLOATING_BUTTON = "video_player_floating_button"
    const val PREF_REMEMBER_LEVELS = "video_player_remember_levels"
    const val PREF_GESTURES = "video_player_gestures"
    const val PREF_DOUBLE_TAP = "video_player_double_tap"
    const val PREF_HOLD_BOOST = "video_player_hold_boost"
    const val PREF_SKIP_SECONDS = "video_player_skip_seconds"
    const val PREF_HIDDEN_BUTTONS = "video_player_hidden_buttons"
    const val DEFAULT_SKIP_SECONDS = 10
    const val PREF_SEEK_STYLE = "video_player_seek_style"
    const val PREF_ACCENT_ICONS = "video_player_accent_icons"
    const val PREF_HIDE_DELAY = "video_player_hide_delay"
    const val DEFAULT_HIDE_DELAY = 4
    val HIDE_DELAY_OPTIONS = listOf(2, 4, 6, 10)
    val SKIP_OPTIONS = listOf(5, 10, 30)
    private const val PREF_BRIGHTNESS = "video_player_brightness"
    private const val PREF_VOLUME = "video_player_volume"

    fun isEnabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_ENABLED, true)

    fun isAutoPip(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_AUTO_PIP, true)

    fun isSiteFullscreen(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_SITE_FULLSCREEN, true)

    fun isFloatingButton(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_FLOATING_BUTTON, true)

    fun isRememberLevels(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_REMEMBER_LEVELS, false)

    fun isGestures(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_GESTURES, true)

    fun isDoubleTap(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_DOUBLE_TAP, true)

    fun isHoldBoost(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_HOLD_BOOST, true)

    fun isAccentIcons(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_ACCENT_ICONS, true)

    fun skipSeconds(context: Context): Int =
        PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_SKIP_SECONDS, DEFAULT_SKIP_SECONDS).takeIf { it in SKIP_OPTIONS } ?: DEFAULT_SKIP_SECONDS

    fun hiddenButtons(context: Context): Set<String> =
        PreferenceManager.getDefaultSharedPreferences(context).getStringSet(PREF_HIDDEN_BUTTONS, null)?.toSet().orEmpty()

    internal fun seekStyle(context: Context): VideoSeekStyle {
        val key = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SEEK_STYLE, null)
        return VideoSeekStyle.entries.firstOrNull { it.key == key } ?: VideoSeekStyle.SLIDER
    }

    fun hideDelaySeconds(context: Context): Int =
        PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_HIDE_DELAY, DEFAULT_HIDE_DELAY).takeIf { it in HIDE_DELAY_OPTIONS } ?: DEFAULT_HIDE_DELAY

    internal fun savedBrightness(context: Context): Float? =
        PreferenceManager.getDefaultSharedPreferences(context).getFloat(PREF_BRIGHTNESS, -1f).takeIf { it >= 0f }

    internal fun savedVolume(context: Context): Float? =
        PreferenceManager.getDefaultSharedPreferences(context).getFloat(PREF_VOLUME, -1f).takeIf { it >= 0f }

    internal fun saveBrightness(context: Context, value: Float) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putFloat(PREF_BRIGHTNESS, value).apply()
    }

    internal fun saveVolume(context: Context, fraction: Float) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putFloat(PREF_VOLUME, fraction).apply()
    }

    internal fun fit(context: Context): VideoFit {
        val key = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_FIT, null)
        return VideoFit.entries.firstOrNull { it.key == key } ?: VideoFit.FIT
    }

    internal fun saveFit(context: Context, fit: VideoFit) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PREF_FIT, fit.key).apply()
    }
}

internal enum class VideoButton(val key: String, val label: Int) {
    SPEED("speed", R.string.video_player_speed_title),
    FIT("fit", R.string.video_player_fit),
    PIP("pip", R.string.video_player_popup),
    BACKGROUND("background", R.string.video_player_background),
    ROTATE("rotate", R.string.video_player_rotate),
    LOCK("lock", R.string.video_player_lock)
}

internal enum class VideoSeekStyle(val key: String, val label: Int) {
    SLIDER("slider", R.string.video_player_seek_slider),
    THIN("thin", R.string.video_player_seek_thin),
    BAR("bar", R.string.video_player_seek_bar),
    SQUIGGLE("squiggle", R.string.video_player_seek_squiggle),
    DOTS("dots", R.string.video_player_seek_dots)
}

internal enum class VideoFit(val key: String, val label: Int) {
    FIT("fit", R.string.video_player_fit_fit),
    STRETCH("fill", R.string.video_player_fit_stretch),
    ZOOM("cover", R.string.video_player_fit_zoom),
    ORIGINAL("none", R.string.video_player_fit_original),
    RATIO_16_9("r169", R.string.video_player_fit_16_9),
    RATIO_4_3("r43", R.string.video_player_fit_4_3)
}

internal data class VideoCandidate(
    val tabId: String?,
    val id: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

internal data class VideoTrackOption(val id: String, val label: String, val selected: Boolean)

internal const val VIDEO_TRACK_AUTO = "auto"
internal const val VIDEO_TRACK_OFF = "off"

internal class VideoUiState {
    var candidate by mutableStateOf<VideoCandidate?>(null)
    var dismissedId by mutableStateOf<String?>(null)
    var floatingButton by mutableStateOf(true)
    var active by mutableStateOf(false)
    var paused by mutableStateOf(true)
    var positionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(-1L)
    var bufferedMs by mutableLongStateOf(0L)
    var rate by mutableFloatStateOf(1f)
    var videoWidth by mutableIntStateOf(0)
    var videoHeight by mutableIntStateOf(0)
    var fit by mutableStateOf(VideoFit.FIT)
    var inPip by mutableStateOf(false)
    var qualities by mutableStateOf<List<VideoTrackOption>>(emptyList())
    var audioTracks by mutableStateOf<List<VideoTrackOption>>(emptyList())
    var subtitles by mutableStateOf<List<VideoTrackOption>>(emptyList())
    var pipPending = false
    var pipRequestedAt = 0L
    var resumeAfterPip = false
    var levelsRestored = false

    fun reset() {
        active = false
        paused = true
        positionMs = 0L
        durationMs = -1L
        bufferedMs = 0L
        rate = 1f
        videoWidth = 0
        videoHeight = 0
        qualities = emptyList()
        audioTracks = emptyList()
        subtitles = emptyList()
        pipPending = false
        resumeAfterPip = false
        levelsRestored = false
    }
}

internal class VideoTakeoverBridge(private val activity: MainActivity, private val webView: ClintWebView) {
    @JavascriptInterface
    fun candidates(json: String?) {
        activity.runOnUiThread { activity.onVideoCandidates(webView, json) }
    }

    @JavascriptInterface
    fun state(json: String?) {
        activity.runOnUiThread { activity.onVideoState(webView, json) }
    }

    @JavascriptInterface
    fun detached(frame: String?) {
        activity.runOnUiThread { activity.onVideoDetached(webView) }
    }
}
