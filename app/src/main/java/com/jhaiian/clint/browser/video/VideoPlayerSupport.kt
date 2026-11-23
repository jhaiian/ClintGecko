package com.jhaiian.clint.browser.video

import android.app.Activity
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.provider.Settings
import android.util.Rational
import androidx.core.content.ContextCompat
import com.jhaiian.clint.R

internal interface VideoPlayerController {
    val title: String
    val canPip: Boolean
    val canBackground: Boolean
    fun exit()
    fun setPaused(paused: Boolean)
    fun seek(positionMs: Long)
    fun skip(seconds: Int)
    fun setRate(rate: Float)
    fun setFit(fit: VideoFit)
    fun enterPip()
    fun playInBackground()
    fun toggleRotation()
    fun brightness(): Float
    fun setBrightness(value: Float)
    fun selectQuality(id: String)
    fun selectAudio(id: String)
    fun selectSubtitle(id: String)
}

internal enum class VideoPipAction(val action: String) {
    PLAY("com.jhaiian.clint.VIDEO_PLAY"),
    PAUSE("com.jhaiian.clint.VIDEO_PAUSE"),
    REWIND("com.jhaiian.clint.VIDEO_REWIND"),
    FORWARD("com.jhaiian.clint.VIDEO_FORWARD")
}

internal fun Context.canEnterVideoPip(): Boolean =
    packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

internal fun Activity.videoPipParams(video: VideoUiState): PictureInPictureParams {
    val ratio = if (video.videoWidth > 0 && video.videoHeight > 0) {
        val value = video.videoWidth.toFloat() / video.videoHeight
        when {
            value > 2.39f -> Rational(239, 100)
            value < 100f / 239f -> Rational(100, 239)
            else -> Rational(video.videoWidth, video.videoHeight)
        }
    } else {
        Rational(16, 9)
    }
    val actions = listOf(
        videoPipAction(android.R.drawable.ic_media_rew, R.string.video_player_rewind, VideoPipAction.REWIND, 11),
        if (video.paused) videoPipAction(android.R.drawable.ic_media_play, R.string.media_action_play, VideoPipAction.PLAY, 12)
        else videoPipAction(android.R.drawable.ic_media_pause, R.string.media_action_pause, VideoPipAction.PAUSE, 13),
        videoPipAction(android.R.drawable.ic_media_ff, R.string.video_player_forward, VideoPipAction.FORWARD, 14)
    )
    return PictureInPictureParams.Builder().setAspectRatio(ratio).setActions(actions).build()
}

private fun Activity.videoPipAction(icon: Int, label: Int, action: VideoPipAction, requestCode: Int): RemoteAction {
    val intent = PendingIntent.getBroadcast(
        this,
        requestCode,
        Intent(action.action).setPackage(packageName),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    val title = getString(label)
    return RemoteAction(Icon.createWithResource(this, icon), title, title, intent)
}

internal fun Activity.registerVideoPipReceiver(onAction: (VideoPipAction) -> Unit): BroadcastReceiver {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            VideoPipAction.entries.firstOrNull { it.action == intent?.action }?.let(onAction)
        }
    }
    val filter = IntentFilter().apply { VideoPipAction.entries.forEach { addAction(it.action) } }
    ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    return receiver
}

internal fun Activity.toggleVideoRotation() {
    val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    requestedOrientation = if (landscape) {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    } else {
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
}

internal fun Activity.videoBrightness(): Float {
    val current = window.attributes.screenBrightness
    if (current >= 0f) return current
    val system = runCatching { Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) }.getOrDefault(128)
    return (system / 255f).coerceIn(0.01f, 1f)
}

internal fun Activity.setVideoBrightness(value: Float) {
    window.attributes = window.attributes.apply { screenBrightness = value.coerceIn(0.01f, 1f) }
}
