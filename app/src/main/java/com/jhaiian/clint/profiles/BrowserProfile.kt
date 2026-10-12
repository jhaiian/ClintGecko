package com.jhaiian.clint.profiles

import android.content.ContentValues
import android.content.Context
import java.util.UUID

data class BrowserProfile(val id: String, val name: String, val color: Int)

object ProfileRepository {

    val defaultColor: Int = 0xFF757575.toInt()

    val palette: List<Int> = listOf(
        0xFF1E88E5, 0xFFE53935, 0xFF43A047, 0xFFFB8C00,
        0xFF8E24AA, 0xFF00ACC1, 0xFFD81B60, 0xFF6D4C41
    ).map { it.toInt() }

    @Volatile
    private var db: ProfileDatabase? = null

    private fun database(context: Context): ProfileDatabase =
        db ?: synchronized(this) {
            db ?: ProfileDatabase(context.applicationContext).also { db = it }
        }

    fun all(context: Context): List<BrowserProfile> {
        val result = mutableListOf<BrowserProfile>()
        database(context).readableDatabase.query(
            ProfileDatabase.TABLE,
            arrayOf(ProfileDatabase.COL_ID, ProfileDatabase.COL_NAME, ProfileDatabase.COL_COLOR),
            null, null, null, null,
            "${ProfileDatabase.COL_POSITION} ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(BrowserProfile(cursor.getString(0), cursor.getString(1), cursor.getInt(2)))
            }
        }
        return result
    }

    fun get(context: Context, id: String): BrowserProfile? = all(context).firstOrNull { it.id == id }

    fun create(context: Context, name: String, color: Int): BrowserProfile {
        val profile = BrowserProfile("p" + UUID.randomUUID().toString().replace("-", "").take(12), name, color)
        val values = ContentValues().apply {
            put(ProfileDatabase.COL_ID, profile.id)
            put(ProfileDatabase.COL_NAME, profile.name)
            put(ProfileDatabase.COL_COLOR, profile.color)
        }
        database(context).writableDatabase.insert(ProfileDatabase.TABLE, null, values)
        return profile
    }

    fun update(context: Context, id: String, name: String, color: Int) {
        val values = ContentValues().apply {
            put(ProfileDatabase.COL_NAME, name)
            put(ProfileDatabase.COL_COLOR, color)
        }
        database(context).writableDatabase.update(ProfileDatabase.TABLE, values, "${ProfileDatabase.COL_ID} = ?", arrayOf(id))
    }

    fun remove(context: Context, id: String) {
        database(context).writableDatabase.delete(ProfileDatabase.TABLE, "${ProfileDatabase.COL_ID} = ?", arrayOf(id))
    }
}
