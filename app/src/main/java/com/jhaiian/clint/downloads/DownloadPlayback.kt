package com.jhaiian.clint.downloads

import java.util.Locale

private val PLAYABLE_VIDEO_EXTENSIONS = setOf(
    "mp4", "m4v", "mkv", "webm", "mov", "3gp", "3g2", "avi", "flv", "wmv", "mpg", "mpeg"
)

internal fun isPlayableVideoExtension(extension: String): Boolean =
    extension.trim().lowercase(Locale.ROOT) in PLAYABLE_VIDEO_EXTENSIONS

internal fun isStreamableWebUrl(url: String): Boolean =
    url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)
