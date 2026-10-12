package com.jhaiian.clint.blocker

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class WebsiteBlockerCategoryDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                id TEXT PRIMARY KEY,
                downloadUrl TEXT NOT NULL,
                isEnabled INTEGER NOT NULL DEFAULT 0,
                isDownloaded INTEGER NOT NULL DEFAULT 0,
                downloadedAt INTEGER NOT NULL DEFAULT 0,
                domainCount INTEGER NOT NULL DEFAULT 0,
                fileSizeBytes INTEGER NOT NULL DEFAULT 0,
                etag TEXT,
                lastModified TEXT
            )
            """.trimIndent()
        )
        WebsiteBlockerDefaults.CATEGORIES.forEach { category ->
            db.insert(TABLE, null, category.toContentValues())
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    fun getAll(): List<WebsiteBlockerCategory> {
        val known = WebsiteBlockerDefaults.CATEGORIES.associateBy { it.id }
        val result = mutableListOf<WebsiteBlockerCategory>()
        readableDatabase.rawQuery("SELECT * FROM $TABLE", null).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(cursor.toCategory())
            }
        }
        return known.keys.map { id -> result.find { it.id == id } ?: known.getValue(id) }
    }

    fun updateEnabled(id: String, isEnabled: Boolean) {
        writableDatabase.update(
            TABLE,
            ContentValues().apply { put("isEnabled", if (isEnabled) 1 else 0) },
            "id = ?",
            arrayOf(id)
        )
    }

    fun updateDownloadState(
        id: String,
        isDownloaded: Boolean,
        downloadedAt: Long,
        domainCount: Long,
        fileSizeBytes: Long,
        etag: String?,
        lastModified: String?
    ) {
        writableDatabase.update(
            TABLE,
            ContentValues().apply {
                put("isDownloaded", if (isDownloaded) 1 else 0)
                put("downloadedAt", downloadedAt)
                put("domainCount", domainCount)
                put("fileSizeBytes", fileSizeBytes)
                put("etag", etag)
                put("lastModified", lastModified)
            },
            "id = ?",
            arrayOf(id)
        )
    }

    fun clearDownloadState(id: String) {
        writableDatabase.update(
            TABLE,
            ContentValues().apply {
                put("isEnabled", 0)
                put("isDownloaded", 0)
                put("downloadedAt", 0L)
                put("domainCount", 0L)
                put("fileSizeBytes", 0L)
                putNull("etag")
                putNull("lastModified")
            },
            "id = ?",
            arrayOf(id)
        )
    }

    private fun WebsiteBlockerCategory.toContentValues() = ContentValues().apply {
        put("id", id)
        put("downloadUrl", downloadUrl)
        put("isEnabled", if (isEnabled) 1 else 0)
        put("isDownloaded", if (isDownloaded) 1 else 0)
        put("downloadedAt", downloadedAt)
        put("domainCount", domainCount)
        put("fileSizeBytes", fileSizeBytes)
        put("etag", etag)
        put("lastModified", lastModified)
    }

    private fun android.database.Cursor.toCategory(): WebsiteBlockerCategory {
        val etagIndex = getColumnIndexOrThrow("etag")
        val lastModifiedIndex = getColumnIndexOrThrow("lastModified")
        return WebsiteBlockerCategory(
            id = getString(getColumnIndexOrThrow("id")),
            downloadUrl = getString(getColumnIndexOrThrow("downloadUrl")),
            isEnabled = getInt(getColumnIndexOrThrow("isEnabled")) != 0,
            isDownloaded = getInt(getColumnIndexOrThrow("isDownloaded")) != 0,
            downloadedAt = getLong(getColumnIndexOrThrow("downloadedAt")),
            domainCount = getLong(getColumnIndexOrThrow("domainCount")),
            fileSizeBytes = getLong(getColumnIndexOrThrow("fileSizeBytes")),
            etag = if (isNull(etagIndex)) null else getString(etagIndex),
            lastModified = if (isNull(lastModifiedIndex)) null else getString(lastModifiedIndex)
        )
    }

    companion object {
        const val DB_NAME = "website_blocker_categories.db"
        private const val DB_VERSION = 1
        private const val TABLE = "categories"
    }
}
