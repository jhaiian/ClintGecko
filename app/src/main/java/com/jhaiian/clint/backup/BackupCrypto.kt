package com.jhaiian.clint.backup

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

private val CONTAINER_MAGIC = "CLINTBK1".toByteArray(StandardCharsets.US_ASCII)
private const val SALT_SIZE = 16
private const val IV_SIZE = 12
private const val GCM_TAG_BITS = 128
private const val AES_KEY_SIZE = 32
private const val STREAM_BUFFER_SIZE = 1 shl 16

const val ARGON2_MEMORY_KB = 19456
const val ARGON2_ITERATIONS = 2
const val ARGON2_PARALLELISM = 1

data class BackupContainerHeader(
    val encrypted: Boolean,
    val salt: ByteArray = ByteArray(0),
    val iv: ByteArray = ByteArray(0),
    val memoryKB: Int = ARGON2_MEMORY_KB,
    val iterations: Int = ARGON2_ITERATIONS,
    val parallelism: Int = ARGON2_PARALLELISM
)

object BackupCrypto {

    fun writeUnencryptedHeader(output: OutputStream) {
        output.write(CONTAINER_MAGIC)
        output.write(0)
    }

    fun writeEncryptedHeader(output: OutputStream, salt: ByteArray, iv: ByteArray, memoryKB: Int, iterations: Int, parallelism: Int) {
        output.write(CONTAINER_MAGIC)
        output.write(1)
        writeInt(output, memoryKB)
        writeInt(output, iterations)
        writeInt(output, parallelism)
        writeShort(output, salt.size)
        output.write(salt)
        writeShort(output, iv.size)
        output.write(iv)
    }

    fun readHeader(input: InputStream): BackupContainerHeader? {
        val magic = ByteArray(CONTAINER_MAGIC.size)
        if (!readFully(input, magic)) return null
        if (!magic.contentEquals(CONTAINER_MAGIC)) return null
        val flag = input.read()
        if (flag < 0) return null
        if (flag == 0) return BackupContainerHeader(encrypted = false)
        val memoryKB = readInt(input) ?: return null
        val iterations = readInt(input) ?: return null
        val parallelism = readInt(input) ?: return null
        val saltLen = readShort(input) ?: return null
        val salt = ByteArray(saltLen)
        if (!readFully(input, salt)) return null
        val ivLen = readShort(input) ?: return null
        val iv = ByteArray(ivLen)
        if (!readFully(input, iv)) return null
        return BackupContainerHeader(true, salt, iv, memoryKB, iterations, parallelism)
    }

    fun randomSalt(): ByteArray = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }

    fun randomIv(): ByteArray = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }

    fun deriveKey(password: CharArray, salt: ByteArray, memoryKB: Int, iterations: Int, parallelism: Int): ByteArray {
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(iterations)
            .withMemoryAsKB(memoryKB)
            .withParallelism(parallelism)
            .withSalt(salt)
            .build()
        val generator = Argon2BytesGenerator()
        generator.init(params)
        val key = ByteArray(AES_KEY_SIZE)
        val passwordBytes = StandardCharsets.UTF_8.encode(java.nio.CharBuffer.wrap(password)).let { buffer ->
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            bytes
        }
        try {
            generator.generateBytes(passwordBytes, key)
        } finally {
            passwordBytes.fill(0)
        }
        return key
    }

    fun encryptStream(input: InputStream, output: OutputStream, key: ByteArray, iv: ByteArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        pumpCipher(input, output, cipher)
    }

    fun decryptStream(input: InputStream, output: OutputStream, key: ByteArray, iv: ByteArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        pumpCipher(input, output, cipher)
    }

    private fun pumpCipher(input: InputStream, output: OutputStream, cipher: Cipher) {
        val buffer = ByteArray(STREAM_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            val processed = cipher.update(buffer, 0, read)
            if (processed != null && processed.isNotEmpty()) output.write(processed)
        }
        val finalBlock = cipher.doFinal()
        if (finalBlock.isNotEmpty()) output.write(finalBlock)
    }

    private fun writeInt(output: OutputStream, value: Int) {
        output.write((value ushr 24) and 0xFF)
        output.write((value ushr 16) and 0xFF)
        output.write((value ushr 8) and 0xFF)
        output.write(value and 0xFF)
    }

    private fun writeShort(output: OutputStream, value: Int) {
        output.write((value ushr 8) and 0xFF)
        output.write(value and 0xFF)
    }

    private fun readInt(input: InputStream): Int? {
        val b = ByteArray(4)
        if (!readFully(input, b)) return null
        return ((b[0].toInt() and 0xFF) shl 24) or ((b[1].toInt() and 0xFF) shl 16) or ((b[2].toInt() and 0xFF) shl 8) or (b[3].toInt() and 0xFF)
    }

    private fun readShort(input: InputStream): Int? {
        val b = ByteArray(2)
        if (!readFully(input, b)) return null
        return ((b[0].toInt() and 0xFF) shl 8) or (b[1].toInt() and 0xFF)
    }

    private fun readFully(input: InputStream, buffer: ByteArray): Boolean {
        var offset = 0
        while (offset < buffer.size) {
            val read = input.read(buffer, offset, buffer.size - offset)
            if (read < 0) return false
            offset += read
        }
        return true
    }
}

class WrongBackupPasswordException : Exception()
class InvalidBackupFileException : Exception()
class UnsupportedBackupVersionException : Exception()
