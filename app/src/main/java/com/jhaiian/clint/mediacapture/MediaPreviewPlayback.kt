@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.jhaiian.clint.mediacapture

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.mediacapture.download.StreamRequestHeaders
import java.util.Locale

internal fun resolveMediaPreviewMimeType(media: DetectedMedia): String? = when (media.format.uppercase(Locale.ROOT)) {
    "HLS" -> MimeTypes.APPLICATION_M3U8
    "DASH" -> MimeTypes.APPLICATION_MPD
    else -> media.mimeType
}

internal fun buildMediaPreviewSource(media: DetectedMedia, dataSourceFactory: OkHttpDataSource.Factory): MediaSource {
    val mimeType = resolveMediaPreviewMimeType(media)
    val mediaItem = MediaItem.Builder().setUri(media.url).apply {
        if (!mimeType.isNullOrBlank()) setMimeType(mimeType)
    }.build()
    return when (media.format.uppercase(Locale.ROOT)) {
        "HLS" -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        "DASH" -> DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        else -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
    }
}

internal fun resolveMediaPreviewUserAgent(media: DetectedMedia, fallback: String): String =
    media.requestHeaders.entries
        .firstOrNull { it.key.equals("User-Agent", ignoreCase = true) }
        ?.value
        ?.takeIf { it.isNotBlank() }
        ?: fallback

internal fun createMediaPreviewDataSourceFactory(context: Context, media: DetectedMedia): OkHttpDataSource.Factory {
    val fallbackUserAgent = runCatching { com.jhaiian.clint.browser.webview.ClintGecko.defaultUserAgent() }.getOrNull().orEmpty()
    val userAgent = resolveMediaPreviewUserAgent(media, fallbackUserAgent)
    val headers = StreamRequestHeaders
        .buildMap(media.url, media.pageUrl, "", "", media.requestHeaders)
        .filterKeys { !it.equals("User-Agent", ignoreCase = true) }
    return OkHttpDataSource.Factory(ClintDownloadManager.httpClient)
        .setUserAgent(userAgent)
        .setDefaultRequestProperties(headers)
}

internal fun formatMediaPreviewClock(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
