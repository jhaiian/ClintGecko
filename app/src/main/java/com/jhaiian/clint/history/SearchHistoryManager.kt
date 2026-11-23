package com.jhaiian.clint.history

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.jhaiian.clint.profiles.ActiveProfile
import java.net.URL

object SearchHistoryManager {

    private const val RECENT_LIMIT = 200

    private val searchEngineHosts = setOf(
        "duckduckgo.com", "duck.com", "ddg.gg",
        "search.brave.com",
        "google.com",
        "ecosia.org"
    )

    fun isSearchEngineUrl(context: Context, url: String): Boolean {
        val host = runCatching { URL(url).host?.lowercase() }.getOrNull() ?: return false
        if (searchEngineHosts.any { host == it || host.endsWith(".$it") }) return true
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        return com.jhaiian.clint.browser.customSearchEngineHosts(prefs).any { host == it || host.endsWith(".$it") }
    }

    @Volatile private var db: SearchHistoryDatabase? = null

    private fun pid(context: Context): String = ActiveProfile.id(context)

    private const val IN_PROFILE = "${SearchHistoryDatabase.COL_PROFILE_ID} = ?"

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
            put(SearchHistoryDatabase.COL_PROFILE_ID, pid(context))
        }
        writable.insertWithOnConflict(
            SearchHistoryDatabase.TABLE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun restore(context: Context, item: HistoryItem) {
        val writable = db(context).writableDatabase
        val values = ContentValues().apply {
            put(SearchHistoryDatabase.COL_QUERY, item.query)
            put(SearchHistoryDatabase.COL_TITLE, item.title)
            put(SearchHistoryDatabase.COL_TIMESTAMP, item.timestamp.takeIf { it > 0L } ?: System.currentTimeMillis())
            put(SearchHistoryDatabase.COL_PROFILE_ID, pid(context))
        }
        writable.insertWithOnConflict(
            SearchHistoryDatabase.TABLE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getAll(context: Context): List<HistoryItem> = query(context, null)

    fun getRecent(context: Context): List<HistoryItem> = query(context, RECENT_LIMIT)

    private fun query(context: Context, limit: Int?): List<HistoryItem> {
        val cursor = db(context).readableDatabase.query(
            SearchHistoryDatabase.TABLE,
            arrayOf(SearchHistoryDatabase.COL_QUERY, SearchHistoryDatabase.COL_TITLE, SearchHistoryDatabase.COL_TIMESTAMP),
            IN_PROFILE, arrayOf(pid(context)), null, null,
            "${SearchHistoryDatabase.COL_TIMESTAMP} DESC",
            limit?.toString()
        )
        val list = mutableListOf<HistoryItem>()
        cursor.use { while (it.moveToNext()) list.add(HistoryItem(it.getString(0), it.getString(1), it.getLong(2))) }
        return list
    }

    fun search(context: Context, prefix: String): List<HistoryItem> {
        val p = prefix.trim()
        if (p.isBlank()) return getRecent(context)
        val cursor = db(context).readableDatabase.query(
            SearchHistoryDatabase.TABLE,
            arrayOf(SearchHistoryDatabase.COL_QUERY, SearchHistoryDatabase.COL_TITLE, SearchHistoryDatabase.COL_TIMESTAMP),
            "(${SearchHistoryDatabase.COL_QUERY} LIKE ? OR ${SearchHistoryDatabase.COL_TITLE} LIKE ?) AND $IN_PROFILE",
            arrayOf("%$p%", "%$p%", pid(context)),
            null, null,
            "${SearchHistoryDatabase.COL_TIMESTAMP} DESC",
            RECENT_LIMIT.toString()
        )
        val list = mutableListOf<HistoryItem>()
        cursor.use { while (it.moveToNext()) list.add(HistoryItem(it.getString(0), it.getString(1), it.getLong(2))) }
        return list
    }

    fun delete(context: Context, query: String) {
        db(context).writableDatabase.delete(
            SearchHistoryDatabase.TABLE,
            "${SearchHistoryDatabase.COL_QUERY} = ? AND $IN_PROFILE",
            arrayOf(query, pid(context))
        )
    }

    fun getSince(context: Context, since: Long): List<HistoryItem> {
        val cursor = db(context).readableDatabase.query(
            SearchHistoryDatabase.TABLE,
            arrayOf(SearchHistoryDatabase.COL_QUERY, SearchHistoryDatabase.COL_TITLE, SearchHistoryDatabase.COL_TIMESTAMP),
            "${SearchHistoryDatabase.COL_TIMESTAMP} >= ? AND $IN_PROFILE",
            arrayOf(since.toString(), pid(context)),
            null, null,
            "${SearchHistoryDatabase.COL_TIMESTAMP} DESC"
        )
        val list = mutableListOf<HistoryItem>()
        cursor.use { while (it.moveToNext()) list.add(HistoryItem(it.getString(0), it.getString(1), it.getLong(2))) }
        return list
    }

    fun deleteSince(context: Context, since: Long) {
        db(context).writableDatabase.delete(
            SearchHistoryDatabase.TABLE,
            "${SearchHistoryDatabase.COL_TIMESTAMP} >= ? AND $IN_PROFILE",
            arrayOf(since.toString(), pid(context))
        )
    }

    fun clear(context: Context) {
        db(context).writableDatabase.delete(SearchHistoryDatabase.TABLE, IN_PROFILE, arrayOf(pid(context)))
    }

    fun clearAllProfiles(context: Context) {
        db(context).writableDatabase.delete(SearchHistoryDatabase.TABLE, null, null)
    }

    fun removeProfile(context: Context, profileId: String) {
        db(context).writableDatabase.delete(SearchHistoryDatabase.TABLE, IN_PROFILE, arrayOf(profileId))
    }
}
