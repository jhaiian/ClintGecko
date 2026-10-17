package com.jhaiian.clint.browser.cookiescontrol

import android.net.Uri
import com.jhaiian.clint.browser.webview.ClintGecko
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject

data class CookiesControlImportResult(val imported: Int, val skipped: Int)

data class CookiesControlDeleteResult(val deleted: Int, val remaining: Int)

data class ControlCookie(
    val name: String,
    val value: String,
    val domain: String,
    val hostOnly: Boolean,
    val path: String,
    val secure: Boolean,
    val httpOnly: Boolean,
    val expires: Long
)

object CookiesControlStore {

    private const val HEADER = "# Netscape HTTP Cookie File"
    private const val HTTP_ONLY_PREFIX = "#HttpOnly_"

    private suspend fun call(op: String, url: String, storeId: String?, payload: JSONObject? = null): Any? =
        suspendCancellableCoroutine { continuation ->
            ClintGecko.cookieOpAsync(op, url, storeId, payload) { result ->
                if (continuation.isActive) continuation.resume(result)
            }
        }

    suspend fun list(url: String, storeId: String?): List<ControlCookie>? {
        val array = call("list", url, storeId) as? JSONArray ?: return null
        val result = ArrayList<ControlCookie>(array.length())
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val name = item.optString("name")
            if (name.isEmpty()) continue
            result.add(
                ControlCookie(
                    name = name,
                    value = item.optString("value"),
                    domain = item.optString("domain"),
                    hostOnly = item.optBoolean("hostOnly", true),
                    path = item.optString("path", "/").ifEmpty { "/" },
                    secure = item.optBoolean("secure", false),
                    httpOnly = item.optBoolean("httpOnly", false),
                    expires = item.optDouble("exp", 0.0).toLong().coerceAtLeast(0L)
                )
            )
        }
        return result
    }

    suspend fun count(url: String, storeId: String?): Int = list(url, storeId)?.size ?: 0

    suspend fun deleteAll(url: String, storeId: String?): CookiesControlDeleteResult {
        val before = list(url, storeId)?.size ?: return CookiesControlDeleteResult(0, 0)
        if (before == 0) return CookiesControlDeleteResult(0, 0)
        call("remove", url, storeId)
        val remaining = list(url, storeId)?.size ?: 0
        return CookiesControlDeleteResult((before - remaining).coerceAtLeast(0), remaining)
    }

    suspend fun buildExport(url: String, storeId: String?): String? {
        val cookies = list(url, storeId)
        if (cookies.isNullOrEmpty()) return null
        val host = Uri.parse(url).host.orEmpty()
        val builder = StringBuilder()
        builder.append(HEADER).append('\n')
        builder.append("# https://curl.se/docs/http-cookies.html").append('\n')
        builder.append('\n')
        cookies.forEach { cookie ->
            val domain = cookie.domain.ifEmpty { host }
            val includeSubdomains = !cookie.hostOnly || domain.startsWith(".")
            if (cookie.httpOnly) builder.append(HTTP_ONLY_PREFIX)
            builder.append(domain).append('\t')
                .append(if (includeSubdomains) "TRUE" else "FALSE").append('\t')
                .append(cookie.path).append('\t')
                .append(if (cookie.secure) "TRUE" else "FALSE").append('\t')
                .append(cookie.expires).append('\t')
                .append(cookie.name).append('\t')
                .append(cookie.value).append('\n')
        }
        return builder.toString()
    }

    suspend fun import(url: String, storeId: String?, text: String): CookiesControlImportResult {
        val hasNetscape = text.lineSequence().any { it.trim().removePrefix(HTTP_ONLY_PREFIX).split('\t').size >= 7 }
        return if (hasNetscape) importNetscape(url, storeId, text) else importPairs(url, storeId, text)
    }

    private suspend fun commit(url: String, storeId: String?, cookies: JSONArray): Int {
        if (cookies.length() == 0) return 0
        val result = call("set", url, storeId, JSONObject().put("cookies", cookies)) as? JSONObject
        return result?.optInt("ok", 0) ?: 0
    }

    private suspend fun importPairs(url: String, storeId: String?, text: String): CookiesControlImportResult {
        val uri = Uri.parse(url)
        val secure = uri.scheme.equals("https", ignoreCase = true)
        val cleaned = text.trim().removePrefix("Cookie:").removePrefix("cookie:")
        val cookies = JSONArray()
        cleaned.split(';', '\n', '\r').forEach { part ->
            val trimmed = part.trim()
            val index = trimmed.indexOf('=')
            if (index <= 0) return@forEach
            val name = trimmed.substring(0, index).trim()
            val value = trimmed.substring(index + 1).trim()
            if (name.isEmpty()) return@forEach
            cookies.put(
                JSONObject()
                    .put("url", url)
                    .put("name", name)
                    .put("value", value)
                    .put("path", "/")
                    .put("secure", secure)
                    .put("httpOnly", false)
            )
        }
        val imported = commit(url, storeId, cookies)
        return CookiesControlImportResult(imported, 0)
    }

    private suspend fun importNetscape(url: String, storeId: String?, text: String): CookiesControlImportResult {
        val uri = Uri.parse(url)
        val host = uri.host.orEmpty().lowercase(Locale.ROOT)
        val siteScheme = uri.scheme?.lowercase(Locale.ROOT) ?: "https"
        var skipped = 0
        val now = System.currentTimeMillis()
        val cookies = JSONArray()

        text.lineSequence().forEach { rawLine ->
            var line = rawLine.trim()
            if (line.isEmpty()) return@forEach
            var httpOnly = false
            if (line.startsWith(HTTP_ONLY_PREFIX, ignoreCase = true)) {
                httpOnly = true
                line = line.substring(HTTP_ONLY_PREFIX.length)
            } else if (line.startsWith("#")) {
                return@forEach
            }
            val fields = line.split('\t')
            if (fields.size < 7) return@forEach

            val rawDomain = fields[0].trim().lowercase(Locale.ROOT)
            val domain = rawDomain.removePrefix(".")
            val includeSubdomains = fields[1].trim().equals("TRUE", ignoreCase = true) || rawDomain.startsWith(".")
            val path = fields[2].trim().ifEmpty { "/" }
            val secure = fields[3].trim().equals("TRUE", ignoreCase = true)
            val expirySeconds = fields[4].trim().toLongOrNull() ?: 0L
            val name = fields[5].trim()
            val value = fields.drop(6).joinToString("\t").trim()

            if (name.isEmpty() || domain.isEmpty()) return@forEach
            if (!(host == domain || host.endsWith(".$domain"))) {
                skipped++
                return@forEach
            }
            if (expirySeconds > 0L && expirySeconds * 1000L <= now) return@forEach

            val scheme = if (secure) "https" else siteScheme
            val item = JSONObject()
                .put("url", "$scheme://$domain/")
                .put("name", name)
                .put("value", value)
                .put("path", path)
                .put("secure", secure)
                .put("httpOnly", httpOnly)
            if (includeSubdomains) item.put("domain", ".$domain")
            if (expirySeconds > 0L) item.put("exp", expirySeconds)
            cookies.put(item)
        }

        val imported = commit(url, storeId, cookies)
        skipped += (cookies.length() - imported).coerceAtLeast(0)
        return CookiesControlImportResult(imported, skipped)
    }
}
