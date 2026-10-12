package com.jhaiian.clint.media

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.SystemClock
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.webview.ClintWebView
import org.mozilla.geckoview.MediaSession

internal class MediaEntry(val view: ClintWebView, var session: MediaSession) {
    var title: String? = null
    var artist: String? = null
    var album: String? = null
    var artwork: Bitmap? = null
    var features = 0L
    var playing = false
    var positionMs = -1L
    var durationMs = -1L
    var rate = 1.0
    var positionStamp = 0L
    var lastPlayedAt = 0L
    var artworkToken = 0
    var metaVersion = 0
    var resumeOnFocusGain = false

    fun estimatedPositionMs(): Long {
        if (positionMs < 0) return -1L
        if (!playing) return positionMs
        val elapsed = ((SystemClock.elapsedRealtime() - positionStamp) * rate).toLong()
        val position = positionMs + elapsed
        return if (durationMs > 0) position.coerceAtMost(durationMs) else position
    }

    fun host(): String? =
        runCatching { Uri.parse(view.url.orEmpty()).host }.getOrNull()?.removePrefix("www.")?.takeIf { it.isNotBlank() }

    fun displayTitle(context: Context): String =
        title?.takeIf { it.isNotBlank() }
            ?: view.title?.takeIf { it.isNotBlank() }
            ?: host()
            ?: context.getString(R.string.app_name)

    fun displaySubtitle(): String? = artist?.takeIf { it.isNotBlank() } ?: host()
}

object MediaPlaybackController {

    private const val ARTWORK_REQUEST_SIZE = 512

    private lateinit var appContext: Context
    private val entries = LinkedHashMap<ClintWebView, MediaEntry>()
    private var appVisible = false
    private var startedActivities = 0
    private var focusRequest: AudioFocusRequest? = null

    @Volatile var tabIdResolver: ((ClintWebView) -> String?)? = null

    private val muteMethod by lazy {
        runCatching { MediaSession::class.java.getMethod("muteAudio", java.lang.Boolean.TYPE) }.getOrNull()
    }

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                val resumable = entries.values.filter { it.resumeOnFocusGain }
                resumable.forEach {
                    it.resumeOnFocusGain = false
                    runCatching { it.session.play() }
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                entries.values.forEach { it.resumeOnFocusGain = false }
                pauseAll()
                abandonFocus()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                entries.values.filter { it.playing }.forEach {
                    it.resumeOnFocusGain = true
                    runCatching { it.session.pause() }
                }
            }
        }
    }

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
    }

    fun onActivityStarted() {
        startedActivities++
        if (startedActivities == 1) onAppVisibilityChanged(true)
    }

    fun onActivityStopped(changingConfigurations: Boolean) {
        if (startedActivities > 0) startedActivities--
        if (startedActivities == 0 && !changingConfigurations) onAppVisibilityChanged(false)
    }

    private fun onAppVisibilityChanged(visible: Boolean) {
        appVisible = visible
        if (!visible && !MediaPlaybackSettings.isBackgroundPlaybackEnabled(appContext)) pauseAll()
    }

    private fun shouldBlockPlayback(): Boolean =
        !appVisible && !MediaPlaybackSettings.isBackgroundPlaybackEnabled(appContext)

    internal fun current(): MediaEntry? =
        entries.values.filter { it.playing }.maxByOrNull { it.lastPlayedAt }
            ?: entries.values.maxByOrNull { it.lastPlayedAt }

    internal fun tabIdFor(view: ClintWebView): String? = tabIdResolver?.invoke(view)

    fun isPlaying(view: ClintWebView): Boolean = entries[view]?.playing == true

    private fun ensure(view: ClintWebView, mediaSession: MediaSession): MediaEntry {
        val existing = entries[view]
        if (existing != null) {
            existing.session = mediaSession
            return existing
        }
        return MediaEntry(view, mediaSession).also { entries[view] = it }
    }

    fun onActivated(view: ClintWebView, mediaSession: MediaSession) {
        ensure(view, mediaSession)
        view.reapplyAudioMute()
        dispatch()
    }

    fun onDeactivated(view: ClintWebView) {
        val entry = entries.remove(view) ?: return
        entry.playing = false
        view.audioState.audible = false
        view.onMediaIdle()
        releaseFocusIfIdle()
        dispatch()
    }

    fun onMetadata(
        view: ClintWebView,
        mediaSession: MediaSession,
        title: String?,
        artist: String?,
        album: String?,
        artwork: org.mozilla.geckoview.Image?
    ) {
        val entry = ensure(view, mediaSession)
        entry.title = title
        entry.artist = artist
        entry.album = album
        entry.artwork = null
        entry.metaVersion++
        val token = ++entry.artworkToken
        if (artwork != null) {
            runCatching {
                artwork.getBitmap(ARTWORK_REQUEST_SIZE).accept(
                    { bitmap ->
                        if (bitmap != null && entry.artworkToken == token && entries[view] === entry) {
                            entry.artwork = bitmap
                            entry.metaVersion++
                            dispatch()
                        }
                    },
                    { }
                )
            }
        }
        dispatch()
    }

    fun onFeatures(view: ClintWebView, mediaSession: MediaSession, features: Long) {
        val entry = ensure(view, mediaSession)
        entry.features = features
        dispatch()
    }

    fun onPositionState(view: ClintWebView, mediaSession: MediaSession, duration: Double, position: Double, rate: Double) {
        val entry = ensure(view, mediaSession)
        entry.durationMs = if (duration.isFinite() && duration > 0) (duration * 1000).toLong() else -1L
        entry.positionMs = if (position.isFinite() && position >= 0) (position * 1000).toLong() else -1L
        entry.rate = if (rate.isFinite() && rate > 0) rate else 1.0
        entry.positionStamp = SystemClock.elapsedRealtime()
        entry.metaVersion++
        dispatch()
    }

    fun onPlay(view: ClintWebView, mediaSession: MediaSession) {
        val entry = ensure(view, mediaSession)
        if (shouldBlockPlayback()) {
            runCatching { mediaSession.pause() }
            return
        }
        entry.playing = true
        entry.resumeOnFocusGain = false
        entry.lastPlayedAt = SystemClock.elapsedRealtime()
        entry.positionStamp = SystemClock.elapsedRealtime()
        view.audioState.audible = true
        requestFocus()
        dispatch()
    }

    fun onPause(view: ClintWebView, mediaSession: MediaSession) {
        val entry = ensure(view, mediaSession)
        if (entry.playing) {
            entry.positionMs = entry.estimatedPositionMs()
            entry.positionStamp = SystemClock.elapsedRealtime()
        }
        entry.playing = false
        view.audioState.audible = false
        view.onMediaIdle()
        releaseFocusIfIdle()
        dispatch()
    }

    fun onViewReleased(view: ClintWebView) {
        val entry = entries.remove(view)
        view.audioState.audible = false
        if (entry == null) return
        releaseFocusIfIdle()
        dispatch()
    }

    fun applyNativeMute(view: ClintWebView, muted: Boolean) {
        val entry = entries[view] ?: return
        runCatching { muteMethod?.invoke(entry.session, muted) }
    }

    fun play() {
        val entry = current() ?: return
        if (shouldBlockPlayback()) return
        runCatching { entry.session.play() }
    }

    fun pauseCurrent() {
        val entry = current() ?: return
        entry.resumeOnFocusGain = false
        runCatching { entry.session.pause() }
    }

    fun pauseAll() {
        entries.values.filter { it.playing }.forEach { runCatching { it.session.pause() } }
    }

    fun stopCurrent() {
        runCatching { current()?.session?.stop() }
    }

    fun next() {
        runCatching { current()?.session?.nextTrack() }
    }

    fun previous() {
        runCatching { current()?.session?.previousTrack() }
    }

    fun seekForward() {
        runCatching { current()?.session?.seekForward() }
    }

    fun seekBackward() {
        runCatching { current()?.session?.seekBackward() }
    }

    fun seekTo(positionMs: Long) {
        val entry = current() ?: return
        runCatching { entry.session.seekTo(positionMs / 1000.0, false) }
        entry.positionMs = positionMs
        entry.positionStamp = SystemClock.elapsedRealtime()
    }

    private fun requestFocus() {
        if (focusRequest != null) return
        val manager = appContext.getSystemService(AudioManager::class.java) ?: return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
            .build()
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(focusListener)
            .build()
        focusRequest = request
        runCatching { manager.requestAudioFocus(request) }
    }

    private fun abandonFocus() {
        val request = focusRequest ?: return
        focusRequest = null
        val manager = appContext.getSystemService(AudioManager::class.java) ?: return
        runCatching { manager.abandonAudioFocusRequest(request) }
    }

    private fun releaseFocusIfIdle() {
        if (entries.values.none { it.playing || it.resumeOnFocusGain }) abandonFocus()
    }

    private fun dispatch() {
        val entry = current()
        MediaPlaybackService.sync(appContext, entry != null, entry?.playing == true)
    }
}
