package com.jhaiian.clint.browser.home

import android.net.Uri

const val HOMEPAGE_CUSTOM = "custom"
const val PREF_HOMEPAGE_CUSTOM_URL = "homepage_custom_url"

private val SCHEME_PREFIX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")

fun normalizeCustomHomepageUrl(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return null
    val withScheme = if (SCHEME_PREFIX.containsMatchIn(trimmed)) trimmed else "https://$trimmed"
    val uri = Uri.parse(withScheme)
    val scheme = uri.scheme?.lowercase()
    val host = uri.host
    if ((scheme != "http" && scheme != "https") || host.isNullOrBlank()) return null
    if (!host.contains('.') && !host.contains(':') && !host.equals("localhost", ignoreCase = true)) return null
    return withScheme
}
