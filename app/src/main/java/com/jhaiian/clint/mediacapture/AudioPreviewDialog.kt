package com.jhaiian.clint.mediacapture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintSlider
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.delay

@Composable
fun AudioPreviewDialog(
    media: DetectedMedia,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current

    var playbackState by remember(media.id) { mutableStateOf(Player.STATE_IDLE) }
    var isPlaying by remember(media.id) { mutableStateOf(false) }
    var hasError by remember(media.id) { mutableStateOf(false) }
    var positionMs by remember(media.id) { mutableStateOf(0L) }
    var durationMs by remember(media.id) { mutableStateOf(0L) }
    var isDragging by remember(media.id) { mutableStateOf(false) }
    var dragValue by remember(media.id) { mutableStateOf(0f) }

    val player = remember(media.id) {
        val dataSourceFactory = createMediaPreviewDataSourceFactory(context, media)
        ExoPlayer.Builder(context).build().apply {
            setMediaSource(buildMediaPreviewSource(media, dataSourceFactory))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state == Player.STATE_READY) {
                    hasError = false
                    durationMs = player.duration.coerceAtLeast(0L)
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                hasError = true
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            if (!isDragging) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                if (player.duration > 0) durationMs = player.duration
            }
            delay(200)
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

    ClintDialog(
        title = stringResource(R.string.media_capture_audio_preview_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(
                Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.back), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                mediaCaptureTitle(media),
                color = colors.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis
            )
            when {
                hasError -> Text(
                    stringResource(R.string.media_capture_audio_preview_error),
                    color = colors.secondaryText,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                )
                playbackState == Player.STATE_IDLE || (playbackState == Player.STATE_BUFFERING && durationMs <= 0L) -> Box(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.primary, trackColor = colors.surfaceVariant)
                }
                else -> {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { if (isPlaying) player.pause() else player.play() }) {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(if (isPlaying) R.string.action_pause else R.string.action_play),
                                tint = colors.primary
                            )
                        }
                        ClintSlider(
                            value = if (isDragging) dragValue else if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                            onValueChange = { isDragging = true; dragValue = it },
                            onValueChangeFinished = {
                                isDragging = false
                                if (durationMs > 0) player.seekTo((dragValue * durationMs).toLong())
                            },
                            enabled = durationMs > 0,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatMediaPreviewClock(positionMs), color = colors.secondaryText, fontSize = 11.sp)
                        Text(formatMediaPreviewClock(durationMs), color = colors.secondaryText, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
