package com.jhaiian.clint.browser.webview

import android.net.Uri

internal fun isSameOriginUrl(sourceUrl: String?, targetUrl: String): Boolean {
    val source = sourceUrl?.let { Uri.parse(it) } ?: return false
    val target = Uri.parse(targetUrl)
    val sourceScheme = source.scheme?.lowercase() ?: return false
    val targetScheme = target.scheme?.lowercase() ?: return false
    if (sourceScheme != targetScheme || (sourceScheme != "http" && sourceScheme != "https")) return false
    val sourceHost = source.host?.lowercase()?.takeIf { it.isNotEmpty() } ?: return false
    val targetHost = target.host?.lowercase()?.takeIf { it.isNotEmpty() } ?: return false
    return sourceHost == targetHost && effectivePort(source, sourceScheme) == effectivePort(target, targetScheme)
}

private fun effectivePort(uri: Uri, scheme: String): Int =
    uri.port.takeIf { it != -1 } ?: if (scheme == "https") 443 else 80
