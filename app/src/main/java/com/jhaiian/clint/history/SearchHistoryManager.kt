package com.jhaiian.clint.history

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.net.URL

object SearchHistoryManager {

    private const val MAX_ENTRIES = 50

    private val searchEngineHosts = setOf(
        "duckduckgo.com", "duck.com", "ddg.gg",
        "search.brave.com",
        "google.com",
        "ecosia.org"
    )

    fun isSearchEngineUrl(context: Context, url: String): Boolean {
        val host = runCatching { URL(url).host?.lowercase() }.getOrNull() ?: return false
        if (searchEngineHosts.any { host == it || host.endsWith(".$it") }) return true
        val customHost = com.jhaiian.clint.browser.customSearchEngineHost(
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        )
        return customHost != null && (host == customHost || host.endsWith(".$customHost"))
    }

    @Volatile private var db: SearchHistoryDatabase? = null

    private fun db(context: Context): SearchHistoryDatabase {
        return db ?: synchronized(this) {
            db ?: SearchHistoryDatabase(context.applicationContext).also { db = it }
        }
    }

    fun add(context: Context, query: String, title: String = "") {
        val q = query.trim()
        if (q.isBlank()) return
        val writable = db(context).writableDatabase
        val values = ContentValues().apply {
            put(SearchHistoryDatabase.COL_QUERY, q)
            put(SearchHistoryDatabase.COL_TITLE, title.trim())
            put(SearchHistoryDatabase.COL_TIMESTAMP, System.currentTimeMillis())
        }
        writable.insertWithOnConflict(
            SearchHistoryDatabase.TABLE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        trimToMax(writable)
    }

    fun getAll(context: Context): List<HistoryItem> {
        val cursor = db(context).readableDatabase.query(
            SearchHistoryDatabase.TABLE,
            arrayOf(SearchHistoryDatabase.COL_QUERY, SearchHistoryDatabase.COL_TITLE, SearchHistoryDatabase.COL_TIMESTAMP),
            null, null, null, null,
            "${SearchHistoryDatabase.COL_TIMESTAMP} DESC",
            MAX_ENTRIES.toString()
        )
        val list = mutableListOf<HistoryItem>()
        cursor.use { while (it.moveToNext()) list.add(HistoryItem(it.getString(0), it.getString(1), it.getLong(2))) }
        return list
    }

    fun search(context: Context, prefix: String): List<HistoryItem> {
        val p = prefix.trim()
        if (p.isBlank()) return getAll(context)
        val cursor = db(context).readableDatabase.query(
            SearchHistoryDatabase.TABLE,
            arrayOf(SearchHistoryDatabase.COL_QUERY, SearchHistoryDatabase.COL_TITLE, SearchHistoryDatabase.COL_TIMESTAMP),
            "${SearchHistoryDatabase.COL_QUERY} LIKE ? OR ${SearchHistoryDatabase.COL_TITLE} LIKE ?",
            arrayOf("%$p%", "%$p%"),
            null, null,
            "${SearchHistoryDatabase.COL_TIMESTAMP} DESC",
            MAX_ENTRIES.toString()
        )
        val list = mutableListOf<HistoryItem>()
        cursor.use { while (it.moveToNext()) list.add(HistoryItem(it.getString(0), it.getString(1), it.getLong(2))) }
        return list
    }

    fun delete(context: Context, query: String) {
        db(context).writableDatabase.delete(
            SearchHistoryDatabase.TABLE,
            "${SearchHistoryDatabase.COL_QUERY} = ?",
            arrayOf(query)
        )
    }

    fun clear(context: Context) {
        db(context).writableDatabase.delete(SearchHistoryDatabase.TABLE, null, null)
    }

    private fun trimToMax(writable: SQLiteDatabase) {
        writable.execSQL(
            """DELETE FROM ${SearchHistoryDatabase.TABLE}
               WHERE ${SearchHistoryDatabase.COL_ID} NOT IN (
                 SELECT ${SearchHistoryDatabase.COL_ID}
                 FROM   ${SearchHistoryDatabase.TABLE}
                 ORDER  BY ${SearchHistoryDatabase.COL_TIMESTAMP} DESC
                 LIMIT  $MAX_ENTRIES
               )"""
        )
    }
}
