package com.jhaiian.clint.parental

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object ParentalPassword {

    const val MIN_LENGTH = 4

    sealed interface VerifyResult {
        object Success : VerifyResult
        object Wrong : VerifyResult
        data class Locked(val remainingMs: Long) : VerifyResult
    }

    private const val PREFS = "parental_password"
    private const val KEY_SALT = "salt"
    private const val KEY_HASH = "hash"
    private const val KEY_ITERATIONS = "iterations"
    private const val KEY_FAILURES = "failures"
    private const val KEY_LOCK_ELAPSED = "lock_until_elapsed"
    private const val KEY_LOCK_WALL = "lock_until_wall"
    private const val KEY_LOCK_BOOT = "lock_boot_count"
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val FAILURES_PER_LOCK = 5
    private const val BASE_LOCK_MS = 30_000L
    private const val MAX_LOCK_MS = 30 * 60_000L
    private const val MAX_LOCK_STEPS = 6

    private val random = SecureRandom()

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun bootCount(context: Context): Int =
        Settings.Global.getInt(context.applicationContext.contentResolver, Settings.Global.BOOT_COUNT, -1)

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(text: String): ByteArray = Base64.decode(text, Base64.NO_WRAP)

    fun isSet(context: Context): Boolean {
        val p = prefs(context)
        return p.contains(KEY_HASH) && p.contains(KEY_SALT)
    }

    @Synchronized
    fun set(context: Context, password: String) {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val hash = derive(password, salt, ITERATIONS)
        prefs(context).edit()
            .clear()
            .putString(KEY_SALT, encode(salt))
            .putString(KEY_HASH, encode(hash))
            .putInt(KEY_ITERATIONS, ITERATIONS)
            .commit()
    }

    @Synchronized
    fun clear(context: Context) {
        prefs(context).edit().clear().commit()
    }

    @Synchronized
    fun remainingLockMs(context: Context): Long {
        val p = prefs(context)
        val untilWall = p.getLong(KEY_LOCK_WALL, 0L)
        if (untilWall == 0L) return 0L
        val sameBoot = p.getInt(KEY_LOCK_BOOT, -1) == bootCount(context) && bootCount(context) != -1
        val remaining = if (sameBoot) {
            p.getLong(KEY_LOCK_ELAPSED, 0L) - SystemClock.elapsedRealtime()
        } else {
            untilWall - System.currentTimeMillis()
        }
        return remaining.coerceIn(0L, MAX_LOCK_MS)
    }

    @Synchronized
    fun verify(context: Context, password: String): VerifyResult {
        val p = prefs(context)
        val saltText = p.getString(KEY_SALT, null)
        val hashText = p.getString(KEY_HASH, null)
        if (saltText == null || hashText == null) return VerifyResult.Success

        val locked = remainingLockMs(context)
        if (locked > 0L) return VerifyResult.Locked(locked)

        val expected = decode(hashText)
        val actual = derive(password, decode(saltText), p.getInt(KEY_ITERATIONS, ITERATIONS))
        if (MessageDigest.isEqual(expected, actual)) {
            p.edit().remove(KEY_FAILURES).remove(KEY_LOCK_ELAPSED).remove(KEY_LOCK_WALL).remove(KEY_LOCK_BOOT).commit()
            return VerifyResult.Success
        }

        val failures = p.getInt(KEY_FAILURES, 0) + 1
        val editor = p.edit().putInt(KEY_FAILURES, failures)
        if (failures % FAILURES_PER_LOCK != 0) {
            editor.commit()
            return VerifyResult.Wrong
        }
        val steps = (failures / FAILURES_PER_LOCK - 1).coerceAtMost(MAX_LOCK_STEPS)
        val lockMs = (BASE_LOCK_MS shl steps).coerceAtMost(MAX_LOCK_MS)
        editor
            .putLong(KEY_LOCK_ELAPSED, SystemClock.elapsedRealtime() + lockMs)
            .putLong(KEY_LOCK_WALL, System.currentTimeMillis() + lockMs)
            .putInt(KEY_LOCK_BOOT, bootCount(context))
            .commit()
        return VerifyResult.Locked(lockMs)
    }
}
