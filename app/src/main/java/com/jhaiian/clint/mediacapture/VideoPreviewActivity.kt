@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.jhaiian.clint.mediacapture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.browser.video.VideoFit
import com.jhaiian.clint.browser.video.VideoPipAction
import com.jhaiian.clint.browser.video.VideoPlayerController
import com.jhaiian.clint.browser.video.VideoPlayerOverlay
import com.jhaiian.clint.browser.video.VideoPlayerSettings
import com.jhaiian.clint.browser.video.VideoTrackOption
import com.jhaiian.clint.browser.video.VIDEO_TRACK_AUTO
import com.jhaiian.clint.browser.video.VIDEO_TRACK_OFF
import com.jhaiian.clint.browser.video.VideoUiState
import com.jhaiian.clint.browser.video.canEnterVideoPip
import com.jhaiian.clint.browser.video.registerVideoPipReceiver
import com.jhaiian.clint.browser.video.setVideoBrightness
import com.jhaiian.clint.browser.video.toggleVideoRotation
import com.jhaiian.clint.browser.video.videoBrightness
import com.jhaiian.clint.browser.video.videoPipParams
import com.jhaiian.clint.ui.theme.ClintComposeTheme
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.delay

private const val EXTRA_TAB_ID = "tab_id"
private const val EXTRA_MEDIA_ID = "media_id"
private const val EXTRA_AUDIO_MEDIA_ID = "audio_media_id"
private const val EXTRA_START_MS = "start_ms"
private const val EXTRA_AUTO_QUALITY = "auto_quality"
private const val EXTRA_DIRECT_URL = "direct_url"
private const val EXTRA_DIRECT_TITLE = "direct_title"
private const val EXTRA_DIRECT_USER_AGENT = "direct_user_agent"
private const val EXTRA_DIRECT_REFERER = "direct_referer"
private const val EXTRA_DIRECT_COOKIES = "direct_cookies"

private const val DIRECT_MEDIA_ID = "direct"
private const val MEDIA_PREFIX = "m:"

private fun orientationFor(width: Int?, height: Int?): Int = when {
    width == null || height == null || width <= 0 || height <= 0 -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    height > width -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
}

class VideoPreviewActivity : ClintActivity() {

    companion object {
        fun start(
            context: Context,
            tabId: String,
            mediaId: String,
            audioMediaId: String? = null,
            startMs: Long = 0L,
            autoQuality: Boolean = false
        ) {
            context.startActivity(
                Intent(context, VideoPreviewActivity::class.java)
                    .putExtra(EXTRA_TAB_ID, tabId)
                    .putExtra(EXTRA_MEDIA_ID, mediaId)
                    .putExtra(EXTRA_AUDIO_MEDIA_ID, audioMediaId)
                    .putExtra(EXTRA_START_MS, startMs)
                    .putExtra(EXTRA_AUTO_QUALITY, autoQuality)
            )
        }

        fun startDirect(
            context: Context,
            url: String,
            title: String,
            userAgent: String,
            referer: String,
            cookies: String
        ) {
            context.startActivity(
                Intent(context, VideoPreviewActivity::class.java)
                    .putExtra(EXTRA_DIRECT_URL, url)
                    .putExtra(EXTRA_DIRECT_TITLE, title)
                    .putExtra(EXTRA_DIRECT_USER_AGENT, userAgent)
                    .putExtra(EXTRA_DIRECT_REFERER, referer)
                    .putExtra(EXTRA_DIRECT_COOKIES, cookies)
            )
        }
    }

    private fun directMedia(url: String): DetectedMedia {
        val headers = LinkedHashMap<String, String>()
        intent.getStringExtra(EXTRA_DIRECT_USER_AGENT)?.takeIf { it.isNotBlank() }?.let { headers["User-Agent"] = it }
        intent.getStringExtra(EXTRA_DIRECT_COOKIES)?.takeIf { it.isNotBlank() }?.let { headers["Cookie"] = it }
        val title = intent.getStringExtra(EXTRA_DIRECT_TITLE).orEmpty()
        return DetectedMedia(
            id = DIRECT_MEDIA_ID,
            url = url,
            kind = MediaKind.VIDEO,
            format = title.substringAfterLast('.', "").uppercase(java.util.Locale.ROOT).ifBlank { "VIDEO" },
            label = title.ifBlank { null },
            pageUrl = intent.getStringExtra(EXTRA_DIRECT_REFERER).orEmpty(),
            requestHeaders = headers
        )
    }

    private val video = VideoUiState().apply { active = true }
    private var player: ExoPlayer? = null
    private var pipReceiver: BroadcastReceiver? = null
    private var resumeOnStart = false
    private var previewTitle = ""
    private var currentMedia: DetectedMedia? = null
    private var alternates: List<DetectedMedia> = emptyList()
    private var pendingVideo: DetectedMedia? = null
    private var pendingAudio: DetectedMedia? = null
    private var buffering by mutableStateOf(false)
    private var hasError by mutableStateOf(false)

    private val controller = object : VideoPlayerController {
        override val title: String get() = previewTitle
        override val canPip: Boolean get() = canEnterVideoPip()
        override val canBackground: Boolean = false
        override fun exit() = finish()
        override fun setPaused(paused: Boolean) = setPreviewPaused(paused)
        override fun seek(positionMs: Long) {
            video.positionMs = positionMs.coerceAtLeast(0L)
            player?.seekTo(video.positionMs)
        }
        override fun skip(seconds: Int) {
            val p = player ?: return
            val target = (p.currentPosition + seconds * 1000L).coerceAtLeast(0L)
            seek(if (video.durationMs > 0) target.coerceAtMost(video.durationMs) else target)
        }
        override fun setRate(rate: Float) {
            video.rate = rate
            player?.setPlaybackSpeed(rate)
        }
        override fun setFit(fit: VideoFit) {
            video.fit = fit
            VideoPlayerSettings.saveFit(this@VideoPreviewActivity, fit)
        }
        override fun enterPip() = enterPreviewPip()
        override fun playInBackground() = Unit
        override fun toggleRotation() = toggleVideoRotation()
        override fun brightness(): Float = videoBrightness()
        override fun setBrightness(value: Float) = setVideoBrightness(value)
        override fun selectQuality(id: String) {
            if (id.startsWith(MEDIA_PREFIX)) {
                switchMedia(id.removePrefix(MEDIA_PREFIX))
                return
            }
            updateParameters { params ->
                if (id == VIDEO_TRACK_AUTO) params.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                else trackOverride(id)?.let { params.setOverrideForType(it) }
            }
        }
        override fun selectAudio(id: String) {
            updateParameters { params -> trackOverride(id)?.let { params.setOverrideForType(it) } }
        }
        override fun selectSubtitle(id: String) {
            updateParameters { params ->
                if (id == VIDEO_TRACK_OFF) params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                else trackOverride(id)?.let { params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).setOverrideForType(it) }
            }
        }
    }

    private fun updateParameters(block: (androidx.media3.common.TrackSelectionParameters.Builder) -> Unit) {
        val p = player ?: return
        val builder = p.trackSelectionParameters.buildUpon()
        block(builder)
        p.trackSelectionParameters = builder.build()
        updateTrackOptions(p.currentTracks)
    }

    private fun trackOverride(id: String): TrackSelectionOverride? {
        val parts = id.split(":")
        val groupIndex = parts.getOrNull(1)?.toIntOrNull() ?: return null
        val trackIndex = parts.getOrNull(2)?.toIntOrNull() ?: return null
        val group = player?.currentTracks?.groups?.getOrNull(groupIndex) ?: return null
        if (trackIndex !in 0 until group.length) return null
        return TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
    }

    private fun switchMedia(mediaId: String) {
        val p = player ?: return
        val target = alternates.firstOrNull { it.id == mediaId } ?: return
        if (target.id == currentMedia?.id) return
        val position = p.currentPosition
        val playWhenReady = p.playWhenReady
        currentMedia = target
        p.setMediaSource(buildCombinedMediaPreviewSource(this, target, null), position)
        p.prepare()
        p.playWhenReady = playWhenReady
        updateTrackOptions(p.currentTracks)
    }

    private fun qualityLabel(format: Format): String = when {
        format.height > 0 -> getString(R.string.video_player_quality_value, format.height)
        format.bitrate > 0 -> getString(R.string.video_player_bitrate, format.bitrate / 1000)
        else -> format.label ?: format.id ?: getString(R.string.video_player_quality)
    }

    private fun mediaQualityLabel(media: DetectedMedia): String =
        media.height?.takeIf { it > 0 }?.let { getString(R.string.video_player_quality_value, it) }
            ?: media.bandwidthBitsPerSec?.takeIf { it > 0 }?.let { getString(R.string.video_player_bitrate, (it / 1000).toInt()) }
            ?: media.label?.takeIf { it.isNotBlank() }
            ?: media.format

    private fun languageLabel(format: Format, ordinal: Int): String =
        trackLanguageName(format.label, format.language) ?: getString(R.string.video_player_track, ordinal)

    private fun applyPendingSelection(tracks: Tracks) {
        val p = player ?: return
        if (tracks.groups.isEmpty()) return
        val video = pendingVideo
        val audio = pendingAudio
        pendingVideo = null
        pendingAudio = null
        val builder = p.trackSelectionParameters.buildUpon()
        var changed = false
        if (video != null) {
            findTrack(tracks, C.TRACK_TYPE_VIDEO) { format ->
                (video.height == null || format.height == video.height) &&
                    (video.width == null || format.width <= 0 || format.width == video.width) &&
                    (video.bandwidthBitsPerSec == null || format.bitrate <= 0 || kotlin.math.abs(format.bitrate - video.bandwidthBitsPerSec) <= video.bandwidthBitsPerSec / 10)
            }?.let {
                builder.setOverrideForType(it)
                changed = true
            }
        }
        if (audio != null) {
            findTrack(tracks, C.TRACK_TYPE_AUDIO) { format ->
                (audio.language != null && format.language.equals(audio.language, ignoreCase = true)) ||
                    (audio.label != null && format.label == audio.label)
            }?.let {
                builder.setOverrideForType(it)
                changed = true
            }
        }
        if (changed) p.trackSelectionParameters = builder.build()
    }

    private fun findTrack(tracks: Tracks, type: Int, match: (Format) -> Boolean): TrackSelectionOverride? {
        for (group in tracks.groups) {
            if (group.type != type) continue
            for (i in 0 until group.length) {
                if (group.isTrackSupported(i) && match(group.getTrackFormat(i))) return TrackSelectionOverride(group.mediaTrackGroup, i)
            }
        }
        return null
    }

    private fun updateTrackOptions(tracks: Tracks) {
        val p = player ?: return
        val videoOverride = p.trackSelectionParameters.overrides.values.firstOrNull { it.type == C.TRACK_TYPE_VIDEO }
        val textDisabled = C.TRACK_TYPE_TEXT in p.trackSelectionParameters.disabledTrackTypes
        val qualities = mutableListOf<Pair<Format, VideoTrackOption>>()
        val audios = mutableListOf<VideoTrackOption>()
        val texts = mutableListOf<VideoTrackOption>()
        tracks.groups.forEachIndexed { groupIndex, group ->
            for (i in 0 until group.length) {
                if (!group.isTrackSupported(i)) continue
                val format = group.getTrackFormat(i)
                val id = "$groupIndex:$i"
                when (group.type) {
                    C.TRACK_TYPE_VIDEO -> qualities += format to VideoTrackOption(
                        "v:$id",
                        qualityLabel(format),
                        videoOverride != null && videoOverride.mediaTrackGroup == group.mediaTrackGroup && i in videoOverride.trackIndices
                    )
                    C.TRACK_TYPE_AUDIO -> audios += VideoTrackOption("a:$id", languageLabel(format, audios.size + 1), group.isTrackSelected(i))
                    C.TRACK_TYPE_TEXT -> texts += VideoTrackOption("s:$id", languageLabel(format, texts.size + 1), !textDisabled && group.isTrackSelected(i))
                }
            }
        }
        val current = currentMedia
        video.qualities = when {
            qualities.size >= 2 ->
                listOf(VideoTrackOption(VIDEO_TRACK_AUTO, getString(R.string.video_player_auto), videoOverride == null)) +
                    qualities.sortedWith(compareByDescending<Pair<Format, VideoTrackOption>> { it.first.height }.thenByDescending { it.first.bitrate }).map { it.second }
            alternates.size >= 2 && current != null ->
                alternates.map { VideoTrackOption(MEDIA_PREFIX + it.id, mediaQualityLabel(it), it.id == current.id) }
            else -> emptyList()
        }
        video.audioTracks = if (audios.size >= 2) audios else emptyList()
        video.subtitles = if (texts.isEmpty()) emptyList() else {
            listOf(VideoTrackOption(VIDEO_TRACK_OFF, getString(R.string.video_player_off), texts.none { it.selected })) + texts
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        val tabId = intent.getStringExtra(EXTRA_TAB_ID)
        val mediaId = intent.getStringExtra(EXTRA_MEDIA_ID)
        val audioMediaId = intent.getStringExtra(EXTRA_AUDIO_MEDIA_ID)
        val directUrl = intent.getStringExtra(EXTRA_DIRECT_URL)?.takeIf { it.isNotBlank() }
        val detected = if (directUrl != null) emptyList<DetectedMedia>() else tabId?.let { id -> MediaCaptureStore.observe(id).value }.orEmpty()
        val media = if (directUrl != null) directMedia(directUrl) else detected.firstOrNull { it.id == mediaId }
        if (media == null) {
            finish()
            return
        }
        val audio = audioMediaId?.let { id -> detected.firstOrNull { it.id == id && it.kind == MediaKind.AUDIO } }
        val startMs = intent.getLongExtra(EXTRA_START_MS, 0L).coerceAtLeast(0L)
        val autoQuality = intent.getBooleanExtra(EXTRA_AUTO_QUALITY, false)
        val manifestUrl = media.groupUrl?.takeIf { media.format.uppercase(java.util.Locale.ROOT) in setOf("HLS", "DASH") }
        val playMedia = if (manifestUrl != null) media.copy(url = manifestUrl) else media
        val playAudio = if (manifestUrl != null) null else audio
        currentMedia = media
        if (manifestUrl != null) {
            pendingVideo = if (autoQuality) null else media
            pendingAudio = audio
        } else {
            val mediaSeconds = media.durationSeconds
            alternates = detected.filter { item ->
                val seconds = item.durationSeconds
                item.kind == MediaKind.VIDEO && item.groupUrl == null &&
                    (seconds == null || mediaSeconds == null || seconds <= 0.0 || mediaSeconds <= 0.0 || kotlin.math.abs(seconds - mediaSeconds) < 3.0)
            }.sortedWith(compareByDescending<DetectedMedia> { it.height ?: 0 }.thenByDescending { it.bandwidthBitsPerSec ?: 0L })
        }
        previewTitle = media.label?.takeIf { it.isNotBlank() }
            ?: runCatching { Uri.parse(media.url).lastPathSegment }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: media.format

        requestedOrientation = orientationFor(media.width, media.height)

        val exo = ExoPlayer.Builder(this).build().apply {
            setMediaSource(buildCombinedMediaPreviewSource(this@VideoPreviewActivity, playMedia, playAudio), startMs)
            addListener(playerListener)
            prepare()
            playWhenReady = true
        }
        player = exo
        video.paused = false
        video.fit = VideoPlayerSettings.fit(this)

        val theme = PreferenceManager.getDefaultSharedPreferences(this).getString("app_theme", "system") ?: "system"

        setContent {
            ClintComposeTheme(theme = theme) {
                PreviewScreen(exo)
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            buffering = state == Player.STATE_BUFFERING
            if (state == Player.STATE_READY) hasError = false
            if (state == Player.STATE_ENDED) video.paused = true
            syncPlayback()
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            video.paused = !playWhenReady || player?.playbackState == Player.STATE_ENDED
            syncPlayback()
        }

        override fun onPlayerError(error: PlaybackException) {
            hasError = true
            video.paused = true
            syncPlayback()
        }

        override fun onTracksChanged(tracks: Tracks) {
            applyPendingSelection(tracks)
            updateTrackOptions(tracks)
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            if (videoSize.width > 0 && videoSize.height > 0) {
                video.videoWidth = videoSize.width
                video.videoHeight = videoSize.height
                if (!video.inPip) requestedOrientation = orientationFor(videoSize.width, videoSize.height)
            }
        }
    }

    private fun setPreviewPaused(paused: Boolean) {
        val p = player ?: return
        if (!paused && p.playbackState == Player.STATE_ENDED) p.seekTo(0L)
        if (!paused && p.playbackState == Player.STATE_IDLE) p.prepare()
        p.playWhenReady = !paused
        video.paused = paused
        syncPlayback()
    }

    private fun syncPlayback() {
        if (video.paused) window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (video.inPip) runCatching { setPictureInPictureParams(videoPipParams(video)) }
    }

    private fun enterPreviewPip() {
        if (!canEnterVideoPip()) return
        runCatching { enterPictureInPictureMode(videoPipParams(video)) }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!video.paused && VideoPlayerSettings.isAutoPip(this)) enterPreviewPip()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        video.inPip = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            if (pipReceiver == null) {
                pipReceiver = registerVideoPipReceiver { action ->
                    when (action) {
                        VideoPipAction.PLAY -> setPreviewPaused(false)
                        VideoPipAction.PAUSE -> setPreviewPaused(true)
                        VideoPipAction.REWIND -> controller.skip(-VideoPlayerSettings.skipSeconds(this@VideoPreviewActivity))
                        VideoPipAction.FORWARD -> controller.skip(VideoPlayerSettings.skipSeconds(this@VideoPreviewActivity))
                    }
                    syncPlayback()
                }
            }
            return
        }
        releasePipReceiver()
        hideSystemBars()
        schedulePipDismissCheck()
    }

    private fun schedulePipDismissCheck() {
        window.decorView.postDelayed({
            if (!isFinishing && !isInPictureInPictureMode && !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) finish()
        }, 500)
    }

    private fun releasePipReceiver() {
        pipReceiver?.let { runCatching { unregisterReceiver(it) } }
        pipReceiver = null
    }

    override fun onStart() {
        super.onStart()
        if (resumeOnStart) {
            resumeOnStart = false
            setPreviewPaused(false)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isInPictureInPictureMode || video.inPip) {
            schedulePipDismissCheck()
            return
        }
        if (isChangingConfigurations) return
        resumeOnStart = !video.paused
        if (!video.paused) setPreviewPaused(true)
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        window.decorView.post { hideSystemBars() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
            window.decorView.post { hideSystemBars() }
        }
    }

    override fun onDestroy() {
        releasePipReceiver()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        player?.removeListener(playerListener)
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    @Composable
    private fun PreviewScreen(exo: ExoPlayer) {
        val colors = LocalClintColors.current
        LaunchedEffect(exo) {
            while (true) {
                video.positionMs = exo.currentPosition.coerceAtLeast(0L)
                video.bufferedMs = exo.bufferedPosition.coerceAtLeast(0L)
                val duration = exo.duration
                video.durationMs = if (exo.isCurrentMediaItemLive || duration == C.TIME_UNSET || duration <= 0) -1L else duration
                video.rate = exo.playbackParameters.speed
                delay(500)
            }
        }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            PreviewSurface(exo, video.fit, video.videoWidth, video.videoHeight)
            if (buffering && !hasError) {
                CircularProgressIndicator(
                    color = colors.primary,
                    trackColor = colors.primary.copy(alpha = 0.25f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            if (hasError) {
                Text(
                    stringResource(R.string.media_capture_video_preview_error),
                    color = Color.White,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 16.dp)
                )
            }
            if (!video.inPip) VideoPlayerOverlay(video, controller)
        }
    }
}

@Composable
private fun PreviewSurface(exo: ExoPlayer, fit: VideoFit, videoWidth: Int, videoHeight: Int) {
    val density = LocalDensity.current
    val surfaceModifier = when (fit) {
        VideoFit.RATIO_16_9 -> Modifier.aspectRatio(16f / 9f)
        VideoFit.RATIO_4_3 -> Modifier.aspectRatio(4f / 3f)
        VideoFit.ORIGINAL ->
            if (videoWidth > 0 && videoHeight > 0) with(density) { Modifier.requiredSize(videoWidth.toDp(), videoHeight.toDp()) }
            else Modifier.fillMaxSize()
        else -> Modifier.fillMaxSize()
    }
    val resizeMode = when (fit) {
        VideoFit.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        VideoFit.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        else -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    }
    Box(Modifier.fillMaxSize().clipToBounds(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = false
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    player = exo
                }
            },
            update = { it.resizeMode = resizeMode },
            modifier = surfaceModifier
        )
    }
}
