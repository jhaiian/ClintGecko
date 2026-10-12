package com.jhaiian.clint.mediacapture.download

import com.jhaiian.clint.downloads.ClintDownloadManager
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import okhttp3.Request

object HlsAesDecryptor {

    private val keyCache = ConcurrentHashMap<String, ByteArray>()

    fun isSupported(method: String): Boolean = method == "AES-128"

    private fun fetchKey(keyUri: String, referer: String, cookies: String, userAgent: String, extraHeaders: Map<String, String>): ByteArray? {
        keyCache[keyUri]?.let { return it }
        val builder = Request.Builder().url(keyUri)
        StreamRequestHeaders.apply(builder, keyUri, referer, cookies, userAgent, extraHeaders)

        return try {
            ClintDownloadManager.httpClient.newCall(builder.build()).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val bytes = resp.body.bytes()
                if (bytes.size != 16) return null
                keyCache[keyUri] = bytes
                bytes
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun hexToBytes(hex: String): ByteArray? {
        val clean = hex.trim()
        if (clean.length != 32) return null
        return try {
            ByteArray(16) { i ->
                clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun decrypt(data: ByteArray, key: HlsKeyInfo, referer: String, cookies: String, userAgent: String, extraHeaders: Map<String, String> = emptyMap()): ByteArray? {
        if (!isSupported(key.method)) return null
        val keyBytes = fetchKey(key.keyUri, referer, cookies, userAgent, extraHeaders) ?: return null
        val ivBytes = key.ivHex?.let(::hexToBytes) ?: return null
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(ivBytes))
            cipher.doFinal(data)
        } catch (_: Exception) {
            null
        }
    }

    fun decryptPrefix(data: ByteArray, key: HlsKeyInfo, referer: String, cookies: String, userAgent: String, extraHeaders: Map<String, String> = emptyMap()): ByteArray? {
        if (!isSupported(key.method)) return null
        val alignedLength = data.size - data.size % 16
        if (alignedLength <= 0) return null
        val keyBytes = fetchKey(key.keyUri, referer, cookies, userAgent, extraHeaders) ?: return null
        val ivBytes = key.ivHex?.let(::hexToBytes) ?: return null
        return try {
            val cipher = Cipher.getInstance("AES/CBC/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(ivBytes))
            cipher.doFinal(data, 0, alignedLength)
        } catch (_: Exception) {
            null
        }
    }
}
