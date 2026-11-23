package com.jhaiian.clint.browser.video

import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.MainUiState
import com.jhaiian.clint.browser.delegates.enterVideoTakeover
import com.jhaiian.clint.mediacapture.formatMediaPreviewClock
import com.jhaiian.clint.ui.ClintSlider
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.delay
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.roundToInt

private val SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f)

private val LocalPlayerIconColor = compositionLocalOf { Color.White }

private enum class VideoPanel { SPEED, FIT, QUALITY, AUDIO, SUBTITLES }

private fun formatRate(rate: Float): String =
    NumberFormat.getInstance().apply { maximumFractionDigits = 2 }.format(rate)

@Composable
internal fun VideoFloatingBar(activity: MainActivity, state: MainUiState) {
    val video = state.video
    val candidate = video.candidate
    val colors = LocalClintColors.current
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(IntOffset.Zero) }
    var bounds by remember { mutableStateOf(IntSize.Zero) }
    var barSize by remember { mutableStateOf(IntSize.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                val p = it.positionInWindow()
                origin = IntOffset(p.x.roundToInt(), p.y.roundToInt())
                bounds = it.size
            }
    ) {
        if (candidate == null || candidate.tabId != state.activeTabId || candidate.id == video.dismissedId) return@Box
        val margin = with(density) { 8.dp.roundToPx() }
        val left = candidate.left - origin.x
        val top = candidate.top - origin.y
        val right = candidate.right - origin.x
        val bottom = minOf(candidate.bottom - origin.y, bounds.height - state.contentPaddingBottomPx)
        val y = bottom - barSize.height - margin
        val x = (right - barSize.width - margin).coerceAtLeast(left + margin)
        if (y < maxOf(top, state.contentPaddingTopPx) + margin) return@Box
        Surface(
            color = colors.popupBackground.copy(alpha = 0.92f),
            shape = RoundedCornerShape(50),
            shadowElevation = 4.dp,
            modifier = Modifier
                .offset { IntOffset(x, y) }
                .onSizeChanged { barSize = it }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
                if (activity.canEnterVideoPip()) {
                    IconButton(onClick = { activity.enterVideoTakeover(pip = true) }) {
                        Icon(Icons.Filled.PictureInPictureAlt, stringResource(R.string.video_player_popup), tint = colors.iconTint)
                    }
                }
                IconButton(onClick = { activity.enterVideoTakeover(pip = false) }) {
                    Icon(Icons.Filled.Fullscreen, stringResource(R.string.video_player_fullscreen), tint = colors.iconTint)
                }
                IconButton(onClick = { video.dismissedId = candidate.id }) {
                    Icon(Icons.Filled.Close, stringResource(R.string.action_close), tint = colors.iconTint)
                }
            }
        }
    }
}

@Composable
internal fun VideoPlayerOverlay(video: VideoUiState, controller: VideoPlayerController) {
    val context = LocalContext.current
    val colors = LocalClintColors.current
    val accent = remember { VideoPlayerSettings.isAccentIcons(context) }
    CompositionLocalProvider(LocalPlayerIconColor provides if (accent) colors.primary else Color.White) {
        VideoPlayerOverlayContent(video, controller)
    }
}

@Composable
private fun VideoPlayerOverlayContent(video: VideoUiState, controller: VideoPlayerController) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val iconColor = LocalPlayerIconColor.current
    val activeColor = if (iconColor == Color.White) colors.primary else Color.White
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    val rememberLevels = remember { VideoPlayerSettings.isRememberLevels(context) }
    val gestures = remember { VideoPlayerSettings.isGestures(context) }
    val doubleTap = remember { VideoPlayerSettings.isDoubleTap(context) }
    val holdBoost = remember { VideoPlayerSettings.isHoldBoost(context) }
    val skip = remember { VideoPlayerSettings.skipSeconds(context) }
    val seekStyle = remember { VideoPlayerSettings.seekStyle(context) }
    val hideDelayMs = remember { VideoPlayerSettings.hideDelaySeconds(context) * 1000L }
    val hidden = remember { VideoPlayerSettings.hiddenButtons(context) }
    val skipBackIcon = when (skip) {
        5 -> Icons.Filled.Replay5
        30 -> Icons.Filled.Replay30
        else -> Icons.Filled.Replay10
    }
    val skipForwardIcon = when (skip) {
        5 -> Icons.Filled.Forward5
        30 -> Icons.Filled.Forward30
        else -> Icons.Filled.Forward10
    }
    val canPip = controller.canPip && VideoButton.PIP.key !in hidden
    val canBackground = controller.canBackground && VideoButton.BACKGROUND.key !in hidden
    val canRotate = VideoButton.ROTATE.key !in hidden &&
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA || LocalConfiguration.current.smallestScreenWidthDp < 600)
    var controlsVisible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var panel by remember { mutableStateOf<VideoPanel?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackStamp by remember { mutableIntStateOf(0) }
    var touchStamp by remember { mutableIntStateOf(0) }
    var scrubMs by remember { mutableStateOf<Long?>(null) }
    var boosting by remember { mutableStateOf(false) }
    var boostRestore by remember { mutableFloatStateOf(1f) }
    val title = controller.title

    LaunchedEffect(controlsVisible, video.paused, touchStamp, panel, locked) {
        if (controlsVisible && panel == null && (locked || !video.paused)) {
            delay(if (locked) 2000L else hideDelayMs)
            controlsVisible = false
        }
    }
    LaunchedEffect(Unit) {
        if (!rememberLevels || video.levelsRestored) return@LaunchedEffect
        video.levelsRestored = true
        VideoPlayerSettings.savedBrightness(context)?.let { controller.setBrightness(it) }
        VideoPlayerSettings.savedVolume(context)?.let {
            runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, (it * maxVolume).roundToInt().coerceIn(0, maxVolume), 0) }
        }
    }
    LaunchedEffect(feedbackStamp) {
        if (feedback != null) {
            delay(800)
            feedback = null
        }
    }

    fun showFeedback(text: String) {
        feedback = text
        feedbackStamp++
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(locked) {
                    detectTapGestures(
                        onTap = {
                            controlsVisible = !controlsVisible
                            panel = null
                            touchStamp++
                        },
                        onDoubleTap = if (locked || !doubleTap) null else ({ offset ->
                            val forward = offset.x > size.width / 2f
                            controller.skip(if (forward) skip else -skip)
                            showFeedback(context.getString(if (forward) R.string.video_player_skip_forward else R.string.video_player_skip_back, skip))
                        }),
                        onLongPress = if (locked || !holdBoost) null else ({
                            if (!video.paused && !boosting) {
                                boostRestore = video.rate
                                boosting = true
                                controller.setRate(2f)
                            }
                        }),
                        onPress = {
                            tryAwaitRelease()
                            if (boosting) {
                                boosting = false
                                controller.setRate(boostRestore)
                            }
                        }
                    )
                }
                .pointerInput(locked) {
                    if (locked || !gestures) return@pointerInput
                    var mode = 0
                    var start = Offset.Zero
                    var total = Offset.Zero
                    var startPosition = 0L
                    var startBrightness = 0f
                    var startVolume = 0
                    detectDragGestures(
                        onDragStart = {
                            start = it
                            total = Offset.Zero
                            mode = 0
                        },
                        onDragEnd = {
                            if (mode == 1) scrubMs?.let { controller.seek(it) }
                            if (rememberLevels) {
                                if (mode == 2) VideoPlayerSettings.saveBrightness(context, controller.brightness())
                                if (mode == 3) VideoPlayerSettings.saveVolume(context, audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume)
                            }
                            scrubMs = null
                            mode = 0
                        },
                        onDragCancel = {
                            scrubMs = null
                            mode = 0
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            total += amount
                            if (mode == 0) {
                                mode = when {
                                    abs(total.x) > abs(total.y) -> if (video.durationMs > 0) 1 else -1
                                    start.x < size.width / 2f -> 2
                                    else -> 3
                                }
                                startPosition = video.positionMs
                                startBrightness = controller.brightness()
                                startVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                            }
                            when (mode) {
                                1 -> {
                                    val target = (startPosition + (total.x / size.width * 90_000f).toLong()).coerceIn(0L, video.durationMs.coerceAtLeast(0L))
                                    scrubMs = target
                                    showFeedback(context.getString(R.string.video_player_time, formatMediaPreviewClock(target), formatMediaPreviewClock(video.durationMs)))
                                }
                                2 -> {
                                    val value = (startBrightness - total.y / size.height).coerceIn(0.01f, 1f)
                                    controller.setBrightness(value)
                                    showFeedback(context.getString(R.string.video_player_brightness, (value * 100).roundToInt()))
                                }
                                3 -> {
                                    val value = (startVolume - total.y / size.height * maxVolume * 1.2f).roundToInt().coerceIn(0, maxVolume)
                                    runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0) }
                                    showFeedback(context.getString(R.string.video_player_volume, value * 100 / maxVolume))
                                }
                            }
                        }
                    )
                }
        )

        AnimatedVisibility(
            visible = controlsVisible && !locked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)))
                        .windowInsetsPadding(WindowInsets.displayCutout)
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    PlayerIcon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.video_player_exit)) { controller.exit() }
                    Text(
                        title,
                        color = Color.White,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    val trackButtons = listOf(
                        Triple(VideoPanel.SUBTITLES, Icons.Filled.ClosedCaption, R.string.video_player_subtitles) to video.subtitles,
                        Triple(VideoPanel.AUDIO, Icons.Filled.Audiotrack, R.string.video_player_audio) to video.audioTracks,
                        Triple(VideoPanel.QUALITY, Icons.Filled.HighQuality, R.string.video_player_quality) to video.qualities
                    )
                    trackButtons.forEach { (button, options) ->
                        if (options.size > 1) {
                            PlayerIcon(
                                button.second,
                                stringResource(button.third),
                                tint = if (panel == button.first) activeColor else iconColor
                            ) {
                                touchStamp++
                                panel = if (panel == button.first) null else button.first
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    PlayerIcon(skipBackIcon, stringResource(R.string.video_player_rewind)) {
                        touchStamp++
                        controller.skip(-skip)
                    }
                    IconButton(
                        onClick = {
                            touchStamp++
                            controller.setPaused(!video.paused)
                        },
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(colors.primary)
                    ) {
                        Icon(
                            if (video.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            stringResource(if (video.paused) R.string.media_action_play else R.string.media_action_pause),
                            tint = colors.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    PlayerIcon(skipForwardIcon, stringResource(R.string.video_player_forward)) {
                        touchStamp++
                        controller.skip(skip)
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                        .windowInsetsPadding(WindowInsets.displayCutout)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    when (panel) {
                        VideoPanel.SPEED -> ChoiceRow(
                            options = SPEEDS,
                            selected = { it == video.rate },
                            label = { stringResource(R.string.video_player_speed_value, formatRate(it)) },
                            onSelect = {
                                touchStamp++
                                controller.setRate(it)
                            }
                        )
                        VideoPanel.FIT -> ChoiceRow(
                            options = VideoFit.entries,
                            selected = { it == video.fit },
                            label = { stringResource(it.label) },
                            onSelect = {
                                touchStamp++
                                controller.setFit(it)
                            }
                        )
                        VideoPanel.QUALITY -> TrackChoiceRow(video.qualities) {
                            touchStamp++
                            controller.selectQuality(it)
                        }
                        VideoPanel.AUDIO -> TrackChoiceRow(video.audioTracks) {
                            touchStamp++
                            controller.selectAudio(it)
                        }
                        VideoPanel.SUBTITLES -> TrackChoiceRow(video.subtitles) {
                            touchStamp++
                            controller.selectSubtitle(it)
                        }
                        null -> Unit
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val position = scrubMs ?: video.positionMs
                        if (video.durationMs > 0) {
                            Text(
                                stringResource(R.string.video_player_time, formatMediaPreviewClock(position), formatMediaPreviewClock(video.durationMs)),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.width(12.dp))
                            PlayerSeekBar(
                                style = seekStyle,
                                value = (position.toFloat() / video.durationMs).coerceIn(0f, 1f),
                                onValueChange = {
                                    touchStamp++
                                    scrubMs = (it * video.durationMs).toLong()
                                },
                                onValueChangeFinished = {
                                    scrubMs?.let { controller.seek(it) }
                                    scrubMs = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Text(
                                stringResource(R.string.video_player_live),
                                color = colors.onPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.primary)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (VideoButton.SPEED.key !in hidden) Text(
                            stringResource(R.string.video_player_speed_value, formatRate(video.rate)),
                            color = if (panel == VideoPanel.SPEED) activeColor else iconColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    touchStamp++
                                    panel = if (panel == VideoPanel.SPEED) null else VideoPanel.SPEED
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        )
                        if (VideoButton.FIT.key !in hidden) PlayerIcon(
                            Icons.Filled.AspectRatio,
                            stringResource(R.string.video_player_fit),
                            tint = if (panel == VideoPanel.FIT) activeColor else iconColor
                        ) {
                            touchStamp++
                            panel = if (panel == VideoPanel.FIT) null else VideoPanel.FIT
                        }
                        if (canPip) {
                            PlayerIcon(Icons.Filled.PictureInPictureAlt, stringResource(R.string.video_player_popup)) { controller.enterPip() }
                        }
                        if (canBackground) {
                            PlayerIcon(Icons.Filled.Headphones, stringResource(R.string.video_player_background)) { controller.playInBackground() }
                        }
                        if (canRotate) PlayerIcon(Icons.Filled.ScreenRotation, stringResource(R.string.video_player_rotate)) {
                            touchStamp++
                            controller.toggleRotation()
                        }
                    }
                }
            }
        }

        if (VideoButton.LOCK.key !in hidden) AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .windowInsetsPadding(WindowInsets.displayCutout)
                .padding(end = 16.dp)
        ) {
            IconButton(
                onClick = {
                    locked = !locked
                    panel = null
                    touchStamp++
                },
                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(
                    if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    stringResource(if (locked) R.string.video_player_unlock else R.string.video_player_lock),
                    tint = if (locked) activeColor else iconColor
                )
            }
        }

        if (boosting) {
            OverlayChip(
                text = stringResource(R.string.video_player_speed_value, formatRate(2f)),
                icon = Icons.Filled.FastForward,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 56.dp)
            )
        }
        feedback?.let {
            OverlayChip(text = it, modifier = Modifier.align(Alignment.TopCenter).padding(top = if (boosting) 104.dp else 56.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSeekBar(
    style: VideoSeekStyle,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (style == VideoSeekStyle.SLIDER) {
        ClintSlider(value = value, onValueChange = onValueChange, onValueChangeFinished = onValueChangeFinished, modifier = modifier)
        return
    }
    if (style != VideoSeekStyle.THIN && style != VideoSeekStyle.BAR) {
        CanvasSeekBar(style, value, onValueChange, onValueChangeFinished, modifier)
        return
    }
    val colors = LocalClintColors.current
    val thin = style == VideoSeekStyle.THIN
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        modifier = modifier,
        thumb = {
            if (thin) Box(Modifier.size(12.dp).clip(CircleShape).background(colors.primary))
            else Spacer(Modifier.size(width = 4.dp, height = 14.dp))
        },
        track = { state ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(if (thin) 3.dp else 14.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                Box(Modifier.fillMaxWidth(state.value.coerceIn(0f, 1f)).fillMaxHeight().background(colors.primary))
            }
        }
    )
}

@Composable
private fun PlayerIcon(icon: ImageVector, description: String, tint: Color = LocalPlayerIconColor.current, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, description, tint = tint)
    }
}

@Composable
private fun OverlayChip(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (icon != null) {
            Icon(icon, null, tint = LocalClintColors.current.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TrackChoiceRow(options: List<VideoTrackOption>, onSelect: (String) -> Unit) {
    ChoiceRow(
        options = options,
        selected = { it.selected },
        label = { it.label },
        onSelect = { onSelect(it.id) }
    )
}

@Composable
private fun <T> ChoiceRow(
    options: List<T>,
    selected: (T) -> Boolean,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit
) {
    val colors = LocalClintColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 6.dp)
    ) {
        options.forEach { option ->
            val isSelected = selected(option)
            Text(
                label(option),
                color = if (isSelected) colors.onPrimary else Color.White,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) colors.primary else Color.White.copy(alpha = 0.16f))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}
