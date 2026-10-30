package com.jhaiian.clint.downloads.mega

import com.jhaiian.clint.downloads.ClintDownloadManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong

data class MegaSource(
    val link: String,
    val handle: String,
    val key: String,
    val folder: String = "",
    val path: String = ""
) {
    fun toJson(): String = JSONObject()
        .put("link", link)
        .put("handle", handle)
        .put("key", key)
        .put("folder", folder)
        .put("path", path)
        .toString()

    companion object {
        fun fromJson(json: String?): MegaSource? {
            if (json.isNullOrBlank()) return null
            val o = runCatching { JSONObject(json) }.getOrNull() ?: return null
            val handle = o.optString("handle")
            val key = o.optString("key")
            if (handle.isBlank() || key.isBlank()) return null
            return MegaSource(o.optString("link"), handle, key, o.optString("folder"), o.optString("path"))
        }
    }
}

class MegaException(val code: Int) : IOException("MEGA error $code") {
    val isPermanent: Boolean
        get() = code == ENOENT || code == EACCESS || code == EBLOCKED || code == EKEY || code == EARGS || code == INVALID_KEY || code == UNSUPPORTED

    companion object {
        const val EARGS = -2
        const val EAGAIN = -3
        const val ERATELIMIT = -4
        const val ENOENT = -9
        const val EACCESS = -11
        const val EKEY = -14
        const val EBLOCKED = -16
        const val EOVERQUOTA = -17
        const val ETEMPUNAVAIL = -18
        const val INVALID_KEY = -1000
        const val UNSUPPORTED = -1001
    }
}

class MegaFileInfo(val name: String, val size: Long, val downloadUrl: String)

class MegaNode(
    val handle: String,
    val parent: String,
    val isFolder: Boolean,
    val name: String,
    val size: Long,
    val key: ByteArray
)

class MegaFolder(val link: MegaLink.Folder, val root: MegaNode, val nodes: List<MegaNode>)

internal object MegaApi {

    private const val ENDPOINT = "https://g.api.mega.co.nz/cs"
    private const val MAX_ATTEMPTS = 6
    private const val FALLBACK_USER_AGENT = "Mozilla/5.0"
    private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    private val sequence = AtomicLong((System.nanoTime() and 0x7FFFFFFFL) * 1000L)

    fun fileInfo(link: MegaLink.File, userAgent: String): MegaFileInfo =
        fileInfo(MegaSource(link.url, link.handle, link.key), userAgent)

    fun fileInfo(source: MegaSource, userAgent: String): MegaFileInfo {
        val nodeKey = MegaCrypto.decodeBase64(source.key)
        val folder = source.folder.ifBlank { null }
        val command = JSONObject()
            .put("a", "g")
            .put("g", 1)
            .put("ssl", 2)
            .put(if (folder == null) "p" else "n", source.handle)
        val result = call(command, folder, userAgent)
        val error = result.optInt("e", 0)
        if (error < 0) throw MegaException(error)
        val url = result.opt("g")
        if (url is JSONArray) throw MegaException(MegaException.UNSUPPORTED)
        val downloadUrl = (url as? String).orEmpty()
        if (downloadUrl.isBlank()) {
            throw MegaException(if (result.optLong("tl", 0L) > 0L) MegaException.EOVERQUOTA else MegaException.ETEMPUNAVAIL)
        }
        val attributes = MegaCrypto.decryptAttributes(result.optString("at"), MegaCrypto.fileAesKey(nodeKey))
            ?: throw MegaException(MegaException.INVALID_KEY)
        return MegaFileInfo(
            name = attributes.optString("n").ifBlank { source.handle },
            size = result.optLong("s", -1L),
            downloadUrl = downloadUrl
        )
    }

    fun folder(link: MegaLink.Folder, userAgent: String): MegaFolder {
        val shareKey = MegaCrypto.decodeBase64(link.key)
        val command = JSONObject().put("a", "f").put("c", 1).put("r", 1).put("ca", 1)
        val result = call(command, link.handle, userAgent)
        val entries = result.optJSONArray("f") ?: throw MegaException(MegaException.ENOENT)
        val nodes = ArrayList<MegaNode>(entries.length())
        var undecryptable = 0
        for (i in 0 until entries.length()) {
            val entry = entries.optJSONObject(i) ?: continue
            val type = entry.optInt("t", -1)
            if (type != 0 && type != 1) continue
            val isFolder = type == 1
            val nodeKey = nodeKeyOf(entry.optString("k"), shareKey, if (isFolder) 16 else 32)
            val attributes = nodeKey?.let {
                MegaCrypto.decryptAttributes(entry.optString("a"), if (isFolder) it else MegaCrypto.fileAesKey(it))
            }
            if (nodeKey == null || attributes == null) {
                undecryptable++
                continue
            }
            nodes.add(
                MegaNode(
                    handle = entry.optString("h"),
                    parent = entry.optString("p"),
                    isFolder = isFolder,
                    name = attributes.optString("n").ifBlank { entry.optString("h") },
                    size = if (isFolder) 0L else entry.optLong("s", 0L),
                    key = nodeKey
                )
            )
        }
        if (nodes.isEmpty()) {
            throw MegaException(if (undecryptable > 0) MegaException.INVALID_KEY else MegaException.ENOENT)
        }
        val handles = nodes.mapTo(HashSet()) { it.handle }
        val root = nodes.firstOrNull { it.isFolder && it.parent !in handles } ?: nodes.first()
        return MegaFolder(link, root, nodes)
    }

    private fun nodeKeyOf(raw: String, shareKey: ByteArray, expectedSize: Int): ByteArray? {
        for (part in raw.split('/')) {
            val encoded = part.substringAfter(':', "")
            if (encoded.isEmpty()) continue
            val encrypted = runCatching { MegaCrypto.decodeBase64(encoded) }.getOrNull() ?: continue
            if (encrypted.size != expectedSize) continue
            return runCatching { MegaCrypto.decryptNodeKey(encrypted, shareKey) }.getOrNull() ?: continue
        }
        return null
    }

    private fun call(command: JSONObject, folderHandle: String?, userAgent: String): JSONObject {
        var attempt = 0
        while (true) {
            val url = buildString {
                append(ENDPOINT).append("?id=").append(sequence.incrementAndGet())
                if (folderHandle != null) append("&n=").append(folderHandle)
            }
            val request = Request.Builder()
                .url(url)
                .post(JSONArray().put(command).toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("User-Agent", userAgent.ifBlank { FALLBACK_USER_AGENT })
                .build()
            val text = ClintDownloadManager.httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("MEGA API HTTP ${response.code}")
                response.body.string()
            }
            val parsed = runCatching { JSONTokener(text.trim()).nextValue() }.getOrNull()
            val first = if (parsed is JSONArray) parsed.opt(0) else parsed
            if (first is JSONObject) return first
            val code = (first as? Number)?.toInt() ?: throw IOException("Unexpected MEGA API response")
            attempt++
            if ((code == MegaException.EAGAIN || code == MegaException.ERATELIMIT) && attempt < MAX_ATTEMPTS) {
                Thread.sleep(500L shl attempt)
                continue
            }
            throw MegaException(code)
        }
    }
}
