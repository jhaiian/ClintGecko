package com.jhaiian.clint.settings.sitepermissions

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.jhaiian.clint.util.registeredDomain

object SitePermissionManager {

    @Volatile private var db: SitePermissionDatabase? = null

    private fun db(context: Context): SitePermissionDatabase {
        return db ?: synchronized(this) {
            db ?: SitePermissionDatabase(context.applicationContext).also { db = it }
        }
    }

    private fun normalizeOrigin(origin: String): String = registeredDomain(origin)

    fun getState(context: Context, origin: String, type: String): String? {
        val key = normalizeOrigin(origin)
        val cursor = db(context).readableDatabase.query(
            SitePermissionDatabase.TABLE,
            arrayOf(SitePermissionDatabase.COL_STATE),
            "${SitePermissionDatabase.COL_ORIGIN} = ? AND ${SitePermissionDatabase.COL_TYPE} = ?",
            arrayOf(key, type),
            null, null, null
        )
        return cursor.use { if (it.moveToFirst()) it.getString(0) else null }
    }

    fun setState(context: Context, origin: String, type: String, state: String) {
        val key = normalizeOrigin(origin)
        val values = ContentValues().apply {
            put(SitePermissionDatabase.COL_ORIGIN, key)
            put(SitePermissionDatabase.COL_TYPE, type)
            put(SitePermissionDatabase.COL_STATE, state)
        }
        db(context).writableDatabase.insertWithOnConflict(
            SitePermissionDatabase.TABLE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getAllByType(context: Context, type: String): List<Triple<String, String, Long>> {
        val cursor = db(context).readableDatabase.query(
            SitePermissionDatabase.TABLE,
            arrayOf(SitePermissionDatabase.COL_ORIGIN, SitePermissionDatabase.COL_STATE, SitePermissionDatabase.COL_ID),
            "${SitePermissionDatabase.COL_TYPE} = ?",
            arrayOf(type),
            null, null,
            "${SitePermissionDatabase.COL_ORIGIN} ASC"
        )
        return cursor.use { c ->
            val result = mutableListOf<Triple<String, String, Long>>()
            while (c.moveToNext()) {
                result.add(Triple(c.getString(0), c.getString(1), c.getLong(2)))
            }
            result
        }
    }

    fun deleteEntry(context: Context, origin: String, type: String) {
        val key = normalizeOrigin(origin)
        db(context).writableDatabase.delete(
            SitePermissionDatabase.TABLE,
            "${SitePermissionDatabase.COL_ORIGIN} IN (?, ?) AND ${SitePermissionDatabase.COL_TYPE} = ?",
            arrayOf(key, origin, type)
        )
    }
}
