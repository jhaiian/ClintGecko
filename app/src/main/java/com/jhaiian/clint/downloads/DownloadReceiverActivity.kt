package com.jhaiian.clint.downloads

import android.content.Intent
import android.os.Bundle
import android.util.Patterns

data class IncomingDownloadSession(
    val userAgent: String = "",
    val referer: String = "",
    val cookies: String = "",
    val filename: String = ""
) {
    val isEmpty: Boolean get() = userAgent.isEmpty() && referer.isEmpty() && cookies.isEmpty() && filename.isEmpty()
}

data class IncomingDownload(val url: String, val session: IncomingDownloadSession?)

private val FILENAME_KEYS = arrayOf("extra_filename", "com.android.extra.filename", "filename", Intent.EXTRA_TITLE)
private val COOKIE_KEYS = arrayOf("extra_cookies", "Cookie", "cookie", "cookies")
private val USER_AGENT_KEYS = arrayOf("extra_useragent", "User-Agent", "user-agent", "user_agent", "useragent")
private val REFERER_KEYS = arrayOf("extra_referer", "Referer", "referer", "referrer")

internal fun parseIncomingDownload(intent: Intent?): IncomingDownload? {
    if (intent == null) return null
    val url = when (intent.action) {
        Intent.ACTION_VIEW -> intent.dataString?.trim()
        Intent.ACTION_SEND -> runCatching { intent.getStringExtra(Intent.EXTRA_TEXT) }.getOrNull()?.trim()?.let { text ->
            val matcher = Patterns.WEB_URL.matcher(text)
            if (matcher.find()) matcher.group().takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) } ?: text else text
        }
        else -> null
    }?.takeIf { it.isNotEmpty() } ?: return null
    val extras = runCatching { intent.extras }.getOrNull()
    val headers = extras?.let { runCatching { it.getBundle("headers") }.getOrNull() }
    fun read(keys: Array<String>, headerName: String? = null): String {
        val direct = keys.firstNotNullOfOrNull { key -> extras?.let { runCatching { it.getString(key) }.getOrNull() }?.trim()?.takeIf { it.isNotEmpty() } }
        if (direct != null) return direct
        if (headerName == null || headers == null) return ""
        return headers.keySet().firstOrNull { it.equals(headerName, ignoreCase = true) }
            ?.let { runCatching { headers.getString(it) }.getOrNull() }?.trim().orEmpty()
    }
    val session = IncomingDownloadSession(
        userAgent = read(USER_AGENT_KEYS, "User-Agent"),
        referer = read(REFERER_KEYS, "Referer"),
        cookies = read(COOKIE_KEYS, "Cookie"),
        filename = read(FILENAME_KEYS).substringAfterLast('/').substringAfterLast('\\')
    )
    return IncomingDownload(url, session.takeUnless { it.isEmpty })
}

class DownloadReceiverActivity : DownloadsActivity() {

    override val translucentWindow: Boolean get() = true

    override val isDownloadReceiver: Boolean get() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!uiState.manualDownloadDialogOpen) finish()
    }
}
