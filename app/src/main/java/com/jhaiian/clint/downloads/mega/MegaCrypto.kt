package com.jhaiian.clint.downloads.mega

import org.json.JSONObject
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal object MegaCrypto {

    fun decodeBase64(value: String): ByteArray {
        val cleaned = value.trim().replace('-', '+').replace('_', '/').replace(",", "").trimEnd('=')
        val padded = cleaned + "=".repeat((4 - cleaned.length % 4) % 4)
        return Base64.getDecoder().decode(padded)
    }

    fun encodeBase64(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    fun fileAesKey(nodeKey: ByteArray): ByteArray =
        if (nodeKey.size >= 32) ByteArray(16) { (nodeKey[it].toInt() xor nodeKey[it + 16].toInt()).toByte() }
        else nodeKey.copyOf(16)

    fun fileNonce(nodeKey: ByteArray): ByteArray =
        if (nodeKey.size >= 24) nodeKey.copyOfRange(16, 24) else ByteArray(8)

    fun decryptAttributes(encoded: String, aesKey: ByteArray): JSONObject? {
        val data = runCatching { decodeBase64(encoded) }.getOrNull() ?: return null
        if (data.isEmpty() || data.size % 16 != 0) return null
        val plain = runCatching {
            val cipher = Cipher.getInstance("AES/CBC/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), IvParameterSpec(ByteArray(16)))
            String(cipher.doFinal(data), Charsets.UTF_8)
        }.getOrNull() ?: return null
        val end = plain.lastIndexOf('}')
        if (!plain.startsWith("MEGA{") || end < 4) return null
        return runCatching { JSONObject(plain.substring(4, end + 1)) }.getOrNull()
    }

    fun decryptNodeKey(encrypted: ByteArray, shareKey: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(shareKey, "AES"))
        return cipher.doFinal(encrypted)
    }
}

internal class MegaCtrDecryptor(nodeKey: ByteArray) {

    constructor(source: MegaSource) : this(MegaCrypto.decodeBase64(source.key))

    private val cipher: Cipher = Cipher.getInstance("AES/ECB/NoPadding").apply {
        init(Cipher.ENCRYPT_MODE, SecretKeySpec(MegaCrypto.fileAesKey(nodeKey), "AES"))
    }
    private val nonce = MegaCrypto.fileNonce(nodeKey)
    private val counters = ByteArray(MAX_BLOCKS * BLOCK)
    private val keystream = ByteArray(MAX_BLOCKS * BLOCK)

    fun process(position: Long, buffer: ByteArray, offset: Int, length: Int) {
        var done = 0
        while (done < length) {
            val absolute = position + done
            val firstBlock = absolute ushr 4
            val skip = (absolute and 15L).toInt()
            val remaining = length - done
            val blocks = minOf((skip + remaining + BLOCK - 1) / BLOCK, MAX_BLOCKS)
            for (b in 0 until blocks) {
                val base = b * BLOCK
                System.arraycopy(nonce, 0, counters, base, 8)
                val counter = firstBlock + b
                for (i in 0 until 8) counters[base + 8 + i] = (counter ushr (56 - 8 * i)).toByte()
            }
            cipher.doFinal(counters, 0, blocks * BLOCK, keystream, 0)
            val usable = minOf(blocks * BLOCK - skip, remaining)
            val start = offset + done
            for (i in 0 until usable) {
                buffer[start + i] = (buffer[start + i].toInt() xor keystream[skip + i].toInt()).toByte()
            }
            done += usable
        }
    }

    private companion object {
        const val BLOCK = 16
        const val MAX_BLOCKS = 4096
    }
}
