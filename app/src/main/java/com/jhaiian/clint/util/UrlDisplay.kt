package com.jhaiian.clint.util

const val PREF_STRIP_URL_SCHEME = "strip_url_scheme"

fun stripUrlForDisplay(url: String): String {
    if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) return url
    val withoutScheme = url.substringAfter("://")
    val withoutWww = if (withoutScheme.startsWith("www.", ignoreCase = true)) withoutScheme.substring(4) else withoutScheme
    return withoutWww.removeSuffix("/").ifEmpty { url }
}
