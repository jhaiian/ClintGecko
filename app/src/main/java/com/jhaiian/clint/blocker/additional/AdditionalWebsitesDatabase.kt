package com.jhaiian.clint.blocker.additional

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AdditionalWebsitesDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                host TEXT UNIQUE NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    fun getAll(): List<AdditionalWebsiteRule> {
        val result = mutableListOf<AdditionalWebsiteRule>()
        readableDatabase.rawQuery("SELECT * FROM $TABLE ORDER BY createdAt DESC", null).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(
                    AdditionalWebsiteRule(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        host = cursor.getString(cursor.getColumnIndexOrThrow("host")),
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"))
                    )
                )
            }
        }
        return result
    }

    fun add(host: String): Boolean {
        val values = ContentValues().apply {
            put("host", host)
            put("createdAt", System.currentTimeMillis())
        }
        return try {
            writableDatabase.insertOrThrow(TABLE, null, values) != -1L
        } catch (_: SQLiteConstraintException) {
            false
        }
    }

    fun addAll(hosts: List<String>): Int {
        if (hosts.isEmpty()) return 0
        val db = writableDatabase
        var inserted = 0
        db.beginTransaction()
        try {
            val seenThisBatch = mutableSetOf<String>()
            for (host in hosts) {
                if (!seenThisBatch.add(host)) continue
                val values = ContentValues().apply {
                    put("host", host)
                    put("createdAt", System.currentTimeMillis())
                }
                try {
                    if (db.insertOrThrow(TABLE, null, values) != -1L) inserted++
                } catch (_: SQLiteConstraintException) {
                    // duplicate host already stored, skip
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return inserted
    }

    fun remove(id: Long) {
        writableDatabase.delete(TABLE, "id = ?", arrayOf(id.toString()))
    }

    fun removeAll(ids: Set<Long>) {
        if (ids.isEmpty()) return
        val placeholders = ids.joinToString(",") { "?" }
        writableDatabase.delete(TABLE, "id IN ($placeholders)", ids.map { it.toString() }.toTypedArray())
    }

    companion object {
        const val DB_NAME = "additional_websites.db"
        private const val DB_VERSION = 1
        private const val TABLE = "additional_websites"
    }
}
