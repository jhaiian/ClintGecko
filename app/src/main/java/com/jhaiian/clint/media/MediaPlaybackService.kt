package com.jhaiian.clint.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity

class MediaPlaybackService : Service() {

    private lateinit var notificationManager: NotificationManager
    private lateinit var mediaSession: MediaSession
    private val handler = Handler(Looper.getMainLooper())
    private var foreground = false
    private var mustPromote = false
    private var noisyRegistered = false
    private var renderedEntry: MediaEntry? = null
    private var renderedMetaVersion = -1
    private var renderedIncognito = false

    @Volatile var stopping = false
        private set

    private val idleStop = Runnable { shutdown() }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            MediaPlaybackController.pauseAll()
        }
    }

    private val sessionCallback = object : MediaSession.Callback() {
        override fun onPlay() = MediaPlaybackController.play()
        override fun onPause() = MediaPlaybackController.pauseCurrent()
        override fun onStop() = MediaPlaybackController.stopCurrent()
        override fun onSkipToNext() = MediaPlaybackController.next()
        override fun onSkipToPrevious() = MediaPlaybackController.previous()
        override fun onFastForward() = MediaPlaybackController.seekForward()
        override fun onRewind() = MediaPlaybackController.seekBackward()
        override fun onSeekTo(pos: Long) = MediaPlaybackController.seekTo(pos)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.media_notification_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = getString(R.string.media_notification_channel_desc)
                setShowBadge(false)
                setSound(null, null)
            }
        )
        mediaSession = MediaSession(this, "ClintGeckoMedia").apply {
            setCallback(sessionCallback, handler)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra(EXTRA_FOREGROUND, false) == true) mustPromote = true
        when (intent?.action) {
            ACTION_PLAY -> MediaPlaybackController.play()
            ACTION_PAUSE -> MediaPlaybackController.pauseCurrent()
            ACTION_NEXT -> MediaPlaybackController.next()
            ACTION_PREVIOUS -> MediaPlaybackController.previous()
            ACTION_DISMISS -> {
                shutdown()
                return START_NOT_STICKY
            }
        }
        render()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (instance === this) instance = null
        stopping = true
        handler.removeCallbacksAndMessages(null)
        unregisterNoisy()
        runCatching { mediaSession.isActive = false }
        runCatching { mediaSession.release() }
        runCatching { notificationManager.cancel(NOTIFICATION_ID) }
        super.onDestroy()
    }

    internal fun render() {
        if (stopping) return
        val entry = MediaPlaybackController.current()
        if (entry == null) {
            shutdown()
            return
        }
        updateSession(entry)
        val notification = buildNotification(entry)
        if (entry.playing) {
            handler.removeCallbacks(idleStop)
            promote(notification)
            registerNoisy()
        } else {
            unregisterNoisy()
            if (mustPromote && !foreground) promote(notification)
            if (foreground) {
                stopForegroundCompat(false)
                foreground = false
            }
            notificationManager.notify(NOTIFICATION_ID, notification)
            handler.removeCallbacks(idleStop)
            handler.postDelayed(idleStop, IDLE_TIMEOUT_MS)
        }
        mustPromote = false
    }

    private fun promote(notification: Notification) {
        if (foreground) {
            notificationManager.notify(NOTIFICATION_ID, notification)
            return
        }
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            foreground = true
        } catch (_: Exception) {
            foreground = false
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat(remove: Boolean) {
        runCatching {
            stopForeground(if (remove) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
        }
    }

    private fun shutdown() {
        if (stopping) return
        stopping = true
        handler.removeCallbacksAndMessages(null)
        unregisterNoisy()
        if (mustPromote && !foreground) {
            promote(
                Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_24)
                    .setContentTitle(getString(R.string.app_name))
                    .build()
            )
        }
        mustPromote = false
        runCatching { mediaSession.isActive = false }
        stopForegroundCompat(true)
        foreground = false
        notificationManager.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    private fun registerNoisy() {
        if (noisyRegistered) return
        ContextCompat.registerReceiver(
            this, noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        noisyRegistered = true
    }

    private fun unregisterNoisy() {
        if (!noisyRegistered) return
        noisyRegistered = false
        runCatching { unregisterReceiver(noisyReceiver) }
    }

    private fun supportedActions(entry: MediaEntry): Long {
        var actions = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE
        val f = entry.features
        if (f and org.mozilla.geckoview.MediaSession.Feature.STOP != 0L) actions = actions or PlaybackState.ACTION_STOP
        if (f and org.mozilla.geckoview.MediaSession.Feature.NEXT_TRACK != 0L) actions = actions or PlaybackState.ACTION_SKIP_TO_NEXT
        if (f and org.mozilla.geckoview.MediaSession.Feature.PREVIOUS_TRACK != 0L) actions = actions or PlaybackState.ACTION_SKIP_TO_PREVIOUS
        if (f and org.mozilla.geckoview.MediaSession.Feature.SEEK_FORWARD != 0L) actions = actions or PlaybackState.ACTION_FAST_FORWARD
        if (f and org.mozilla.geckoview.MediaSession.Feature.SEEK_BACKWARD != 0L) actions = actions or PlaybackState.ACTION_REWIND
        if (f and org.mozilla.geckoview.MediaSession.Feature.SEEK_TO != 0L && entry.durationMs > 0) actions = actions or PlaybackState.ACTION_SEEK_TO
        return actions
    }

    private fun updateSession(entry: MediaEntry) {
        val incognito = entry.view.isPrivate
        if (renderedEntry !== entry || renderedMetaVersion != entry.metaVersion || renderedIncognito != incognito) {
            renderedEntry = entry
            renderedMetaVersion = entry.metaVersion
            renderedIncognito = incognito
            val metadata = MediaMetadata.Builder()
            if (incognito) {
                metadata.putString(MediaMetadata.METADATA_KEY_TITLE, getString(R.string.media_notification_incognito_title))
            } else {
                metadata.putString(MediaMetadata.METADATA_KEY_TITLE, entry.displayTitle(this))
                entry.displaySubtitle()?.let { metadata.putString(MediaMetadata.METADATA_KEY_ARTIST, it) }
                entry.album?.takeIf { it.isNotBlank() }?.let { metadata.putString(MediaMetadata.METADATA_KEY_ALBUM, it) }
                entry.artwork?.let { metadata.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it) }
            }
            if (entry.durationMs > 0) metadata.putLong(MediaMetadata.METADATA_KEY_DURATION, entry.durationMs)
            mediaSession.setMetadata(metadata.build())
        }
        mediaSession.setSessionActivity(contentIntent(entry))
        val position = entry.estimatedPositionMs().takeIf { it >= 0 } ?: PlaybackState.PLAYBACK_POSITION_UNKNOWN
        val state = if (entry.playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        mediaSession.setPlaybackState(
            PlaybackState.Builder()
                .setActions(supportedActions(entry))
                .setState(state, position, if (entry.playing) entry.rate.toFloat() else 0f, SystemClock.elapsedRealtime())
                .build()
        )
        if (!mediaSession.isActive) mediaSession.isActive = true
    }

    private fun contentIntent(entry: MediaEntry): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            MediaPlaybackController.tabIdFor(entry.view)?.let { putExtra(EXTRA_TAB_ID, it) }
        }
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, MediaPlaybackService::class.java).setAction(action)
        return PendingIntent.getService(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun action(iconRes: Int, label: Int, action: String, requestCode: Int): Notification.Action =
        Notification.Action.Builder(Icon.createWithResource(this, iconRes), getString(label), actionIntent(action, requestCode)).build()

    private fun buildNotification(entry: MediaEntry): Notification {
        val incognito = entry.view.isPrivate
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_24)
            .setContentTitle(if (incognito) getString(R.string.media_notification_incognito_title) else entry.displayTitle(this))
            .setContentIntent(contentIntent(entry))
            .setDeleteIntent(actionIntent(ACTION_DISMISS, 5))
            .setVisibility(if (incognito) Notification.VISIBILITY_PRIVATE else Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(entry.playing)
        if (!incognito) {
            builder.setContentText(entry.displaySubtitle())
            entry.host()?.let { builder.setSubText(it) }
            val art: Bitmap? = entry.artwork
            if (art != null) builder.setLargeIcon(art)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        val compact = ArrayList<Int>()
        var index = 0
        if (entry.features and org.mozilla.geckoview.MediaSession.Feature.PREVIOUS_TRACK != 0L) {
            builder.addAction(action(android.R.drawable.ic_media_previous, R.string.media_action_previous, ACTION_PREVIOUS, 1))
            compact.add(index++)
        }
        if (entry.playing) {
            builder.addAction(action(android.R.drawable.ic_media_pause, R.string.media_action_pause, ACTION_PAUSE, 2))
        } else {
            builder.addAction(action(android.R.drawable.ic_media_play, R.string.media_action_play, ACTION_PLAY, 3))
        }
        compact.add(index++)
        if (entry.features and org.mozilla.geckoview.MediaSession.Feature.NEXT_TRACK != 0L) {
            builder.addAction(action(android.R.drawable.ic_media_next, R.string.media_action_next, ACTION_NEXT, 4))
            compact.add(index)
        }
        builder.setStyle(
            Notification.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(*compact.toIntArray())
        )
        return builder.build()
    }

    companion object {
        const val EXTRA_TAB_ID = "com.jhaiian.clint.media.EXTRA_TAB_ID"
        private const val EXTRA_FOREGROUND = "com.jhaiian.clint.media.EXTRA_FOREGROUND"
        private const val ACTION_PLAY = "com.jhaiian.clint.media.PLAY"
        private const val ACTION_PAUSE = "com.jhaiian.clint.media.PAUSE"
        private const val ACTION_NEXT = "com.jhaiian.clint.media.NEXT"
        private const val ACTION_PREVIOUS = "com.jhaiian.clint.media.PREVIOUS"
        private const val ACTION_DISMISS = "com.jhaiian.clint.media.DISMISS"
        private const val CHANNEL_ID = "clint_media_playback"
        private const val NOTIFICATION_ID = 7421
        private const val IDLE_TIMEOUT_MS = 10L * 60L * 1000L

        @Volatile private var instance: MediaPlaybackService? = null

        internal fun sync(context: Context, hasMedia: Boolean, playing: Boolean) {
            val running = instance
            if (running != null && !running.stopping) {
                running.render()
                return
            }
            if (!hasMedia || !playing) return
            val intent = Intent(context, MediaPlaybackService::class.java).putExtra(EXTRA_FOREGROUND, true)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
            }
        }
    }
}
