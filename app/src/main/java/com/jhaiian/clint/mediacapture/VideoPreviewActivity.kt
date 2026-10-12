@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.jhaiian.clint.mediacapture

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import androidx.media3.ui.R as Media3UiR
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.ui.theme.ClintComposeTheme
import com.jhaiian.clint.ui.theme.LocalClintColors

private const val EXTRA_TAB_ID = "tab_id"
private const val EXTRA_MEDIA_ID = "media_id"

private fun orientationFor(width: Int?, height: Int?): Int = when {
    width == null || height == null || width <= 0 || height <= 0 -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    height > width -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
}

class VideoPreviewActivity : ClintActivity() {

    companion object {
        fun start(context: Context, tabId: String, mediaId: String) {
            context.startActivity(
                Intent(context, VideoPreviewActivity::class.java)
                    .putExtra(EXTRA_TAB_ID, tabId)
                    .putExtra(EXTRA_MEDIA_ID, mediaId)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        val tabId = intent.getStringExtra(EXTRA_TAB_ID)
        val mediaId = intent.getStringExtra(EXTRA_MEDIA_ID)
        val media = tabId?.let { id -> MediaCaptureStore.observe(id).value.firstOrNull { it.id == mediaId } }
        if (media == null) {
            finish()
            return
        }

        requestedOrientation = orientationFor(media.width, media.height)

        val theme = PreferenceManager.getDefaultSharedPreferences(this).getString("app_theme", "system") ?: "system"

        setContent {
            ClintComposeTheme(theme = theme) {
                VideoPreviewPlayer(
                    media = media,
                    onVideoSizeDetected = { width, height -> requestedOrientation = orientationFor(width, height) },
                    onKeepScreenOnChanged = { keepOn -> setKeepScreenOn(keepOn) },
                    onClose = { finish() }
                )
            }
        }
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

    private fun setKeepScreenOn(keepOn: Boolean) {
        if (keepOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }
}

private fun PlayerView.applyAccentControls(accent: Int, onAccent: Int) {
    val accentTint = ColorStateList.valueOf(accent)
    (findViewById<View?>(Media3UiR.id.exo_play_pause) as? ImageView)?.apply {
        backgroundTintList = accentTint
        ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(onAccent))
    }
    listOf(
        Media3UiR.id.exo_rew_with_amount,
        Media3UiR.id.exo_ffwd_with_amount,
        Media3UiR.id.exo_prev,
        Media3UiR.id.exo_next,
        Media3UiR.id.exo_settings
    ).forEach { id ->
        (findViewById<View?>(id) as? ImageView)?.let {
            ImageViewCompat.setImageTintList(it, accentTint)
        }
    }
    (findViewById<View?>(Media3UiR.id.exo_progress) as? DefaultTimeBar)?.apply {
        setPlayedColor(accent)
        setScrubberColor(accent)
    }
}

@Composable
private fun VideoPreviewPlayer(
    media: DetectedMedia,
    onVideoSizeDetected: (Int, Int) -> Unit,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val clintColors = LocalClintColors.current
    val accentColor = remember(clintColors.primary) { clintColors.primary.toArgb() }
    val onAccentColor = remember(clintColors.onPrimary) { clintColors.onPrimary.toArgb() }
    var playbackState by remember(media.id) { mutableStateOf(Player.STATE_IDLE) }
    var hasError by remember(media.id) { mutableStateOf(false) }
    var controlsVisible by remember(media.id) { mutableStateOf(false) }

    val player = remember(media.id) {
        val dataSourceFactory = createMediaPreviewDataSourceFactory(context, media)
        ExoPlayer.Builder(context).build().apply {
            setMediaSource(buildMediaPreviewSource(media, dataSourceFactory))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        fun syncKeepScreenOn() {
            val active = player.playWhenReady &&
                (player.playbackState == Player.STATE_READY || player.playbackState == Player.STATE_BUFFERING)
            onKeepScreenOnChanged(active)
        }
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state == Player.STATE_READY) hasError = false
                syncKeepScreenOn()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                syncKeepScreenOn()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                syncKeepScreenOn()
            }

            override fun onPlayerError(error: PlaybackException) {
                hasError = true
                onKeepScreenOnChanged(false)
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    onVideoSizeDetected(videoSize.width, videoSize.height)
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            onKeepScreenOnChanged(false)
            player.release()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> if (!hasError) player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    setUseController(true)
                    setPlayer(player)
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility -> controlsVisible = visibility == View.VISIBLE }
                    )
                    applyAccentControls(accentColor, onAccentColor)
                }
            },
            update = { it.applyAccentControls(accentColor, onAccentColor) },
            modifier = Modifier.fillMaxSize()
        )
        if (playbackState == Player.STATE_BUFFERING && !hasError) {
            CircularProgressIndicator(
                color = clintColors.primary,
                trackColor = clintColors.primary.copy(alpha = 0.25f),
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
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .displayCutoutPadding()
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_close),
                    tint = Color.White
                )
            }
        }
    }
}
