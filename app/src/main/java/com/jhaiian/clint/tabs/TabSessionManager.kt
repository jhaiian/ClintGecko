package com.jhaiian.clint.tabs

import android.content.ContentValues
import android.content.Context
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class SavedTab(
    val position: Int,
    val url: String,
    val title: String,
    val isActive: Boolean,
    val tabId: String,
    val shortcutId: String? = null,
    val lastActiveAt: Long = System.currentTimeMillis(),
    val profileId: String = "default",
    val groupId: String? = null,
    val isPinned: Boolean = false
)

data class ClosedTab(
    val id: Long = 0L,
    val url: String,
    val title: String,
    val profileId: String = "default",
    val groupName: String? = null,
    val groupColor: Int? = null,
    val closedAt: Long
)

object TabSessionManager {

    const val MAX_CLOSED_TABS = 25

    private const val PENDING_WRITE_TIMEOUT_MS = 2000L

    @Volatile private var db: TabDatabase? = null

    private val writer = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "tab-session-io")
    }

    private fun db(context: Context): TabDatabase {
        return db ?: synchronized(this) {
            db ?: TabDatabase(context.applicationContext).also { db = it }
        }
    }

    fun saveAsync(context: Context, tabs: List<SavedTab>, groups: List<TabGroup> = emptyList()) {
        val appContext = context.applicationContext
        writer.execute { runCatching { save(appContext, tabs, groups) } }
    }

    fun clearAsync(context: Context) {
        val appContext = context.applicationContext
        writer.execute { runCatching { clear(appContext) } }
    }

    fun addClosedAsync(context: Context, entries: List<ClosedTab>) {
        if (entries.isEmpty()) return
        val appContext = context.applicationContext
        writer.execute {
            runCatching {
                val writable = db(appContext).writableDatabase
                writable.beginTransaction()
                try {
                    entries.forEach { entry ->
                        val values = ContentValues().apply {
                            put(TabDatabase.COL_URL, entry.url)
                            put(TabDatabase.COL_TITLE, entry.title)
                            put(TabDatabase.COL_PROFILE_ID, entry.profileId)
                            put(TabDatabase.COL_CLOSED_GROUP_NAME, entry.groupName)
                            put(TabDatabase.COL_CLOSED_GROUP_COLOR, entry.groupColor)
                            put(TabDatabase.COL_CLOSED_AT, entry.closedAt)
                        }
                        writable.insert(TabDatabase.CLOSED_TABLE, null, values)
                    }
                    writable.execSQL(
                        "DELETE FROM ${TabDatabase.CLOSED_TABLE} WHERE ${TabDatabase.COL_ID} NOT IN " +
                            "(SELECT ${TabDatabase.COL_ID} FROM ${TabDatabase.CLOSED_TABLE} " +
                            "ORDER BY ${TabDatabase.COL_CLOSED_AT} DESC, ${TabDatabase.COL_ID} DESC LIMIT $MAX_CLOSED_TABS)"
                    )
                    writable.setTransactionSuccessful()
                } finally {
                    writable.endTransaction()
                }
            }
        }
    }

    fun removeClosedBatchAsync(context: Context, closedAt: Long) {
        val appContext = context.applicationContext
        writer.execute {
            runCatching {
                db(appContext).writableDatabase.delete(
                    TabDatabase.CLOSED_TABLE,
                    "${TabDatabase.COL_CLOSED_AT} = ?",
                    arrayOf(closedAt.toString())
                )
            }
        }
    }

    fun removeClosedAsync(context: Context, id: Long) {
        val appContext = context.applicationContext
        writer.execute {
            runCatching {
                db(appContext).writableDatabase.delete(
                    TabDatabase.CLOSED_TABLE,
                    "${TabDatabase.COL_ID} = ?",
                    arrayOf(id.toString())
                )
            }
        }
    }

    fun clearClosedAsync(context: Context) {
        val appContext = context.applicationContext
        writer.execute { runCatching { clearClosed(appContext) } }
    }

    fun loadClosed(context: Context): List<ClosedTab> {
        awaitPendingWrites()
        val cursor = db(context).readableDatabase.query(
            TabDatabase.CLOSED_TABLE,
            arrayOf(
                TabDatabase.COL_ID,
                TabDatabase.COL_URL,
                TabDatabase.COL_TITLE,
                TabDatabase.COL_PROFILE_ID,
                TabDatabase.COL_CLOSED_GROUP_NAME,
                TabDatabase.COL_CLOSED_GROUP_COLOR,
                TabDatabase.COL_CLOSED_AT
            ),
            null, null, null, null,
            "${TabDatabase.COL_CLOSED_AT} DESC, ${TabDatabase.COL_ID} ASC"
        )
        val list = mutableListOf<ClosedTab>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    ClosedTab(
                        id = it.getLong(0),
                        url = it.getString(1),
                        title = it.getString(2) ?: "",
                        profileId = it.getString(3) ?: "default",
                        groupName = if (it.isNull(4)) null else it.getString(4),
                        groupColor = if (it.isNull(5)) null else it.getInt(5),
                        closedAt = it.getLong(6)
                    )
                )
            }
        }
        return list
    }

    fun clearClosed(context: Context) {
        db(context).writableDatabase.delete(TabDatabase.CLOSED_TABLE, null, null)
    }

    fun deleteClosedSince(context: Context, since: Long) {
        db(context).writableDatabase.delete(
            TabDatabase.CLOSED_TABLE,
            "${TabDatabase.COL_CLOSED_AT} >= ?",
            arrayOf(since.toString())
        )
    }

    fun awaitPendingWrites() {
        runCatching { writer.submit(Runnable {}).get(PENDING_WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS) }
    }

    fun save(context: Context, tabs: List<SavedTab>, groups: List<TabGroup> = emptyList()) {
        val writable = db(context).writableDatabase
        writable.beginTransaction()
        try {
            writable.delete(TabDatabase.TABLE, null, null)
            writable.delete(TabDatabase.GROUPS_TABLE, null, null)
            groups.forEachIndexed { index, group ->
                val values = ContentValues().apply {
                    put(TabDatabase.COL_GROUP_ID, group.id)
                    put(TabDatabase.COL_GROUP_POSITION, index)
                    put(TabDatabase.COL_GROUP_NAME, group.name)
                    put(TabDatabase.COL_GROUP_COLOR, group.color)
                    put(TabDatabase.COL_GROUP_COLLAPSED, if (group.collapsed) 1 else 0)
                }
                writable.insert(TabDatabase.GROUPS_TABLE, null, values)
            }
            tabs.forEach { tab ->
                val values = ContentValues().apply {
                    put(TabDatabase.COL_POSITION, tab.position)
                    put(TabDatabase.COL_URL, tab.url)
                    put(TabDatabase.COL_TITLE, tab.title)
                    put(TabDatabase.COL_ACTIVE, if (tab.isActive) 1 else 0)
                    put(TabDatabase.COL_TAB_ID, tab.tabId)
                    put(TabDatabase.COL_SHORTCUT_ID, tab.shortcutId)
                    put(TabDatabase.COL_LAST_ACTIVE, tab.lastActiveAt)
                    put(TabDatabase.COL_PROFILE_ID, tab.profileId)
                    put(TabDatabase.COL_GROUP_ID, tab.groupId)
                    put(TabDatabase.COL_PINNED, if (tab.isPinned) 1 else 0)
                }
                writable.insert(TabDatabase.TABLE, null, values)
            }
            writable.setTransactionSuccessful()
        } finally {
            writable.endTransaction()
        }
    }

    fun load(context: Context): List<SavedTab> {
        val cursor = db(context).readableDatabase.query(
            TabDatabase.TABLE,
            arrayOf(
                TabDatabase.COL_POSITION,
                TabDatabase.COL_URL,
                TabDatabase.COL_TITLE,
                TabDatabase.COL_ACTIVE,
                TabDatabase.COL_TAB_ID,
                TabDatabase.COL_SHORTCUT_ID,
                TabDatabase.COL_LAST_ACTIVE,
                TabDatabase.COL_PROFILE_ID,
                TabDatabase.COL_GROUP_ID,
                TabDatabase.COL_PINNED
            ),
            null, null, null, null,
            "${TabDatabase.COL_POSITION} ASC"
        )
        val list = mutableListOf<SavedTab>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    SavedTab(
                        position = it.getInt(0),
                        url = it.getString(1),
                        title = it.getString(2),
                        isActive = it.getInt(3) == 1,
                        tabId = it.getString(4) ?: java.util.UUID.randomUUID().toString(),
                        shortcutId = it.getString(5),
                        lastActiveAt = it.getLong(6),
                        profileId = it.getString(7) ?: "default",
                        groupId = it.getString(8),
                        isPinned = it.getInt(9) == 1
                    )
                )
            }
        }
        return list
    }

    fun loadGroups(context: Context): List<TabGroup> {
        val cursor = db(context).readableDatabase.query(
            TabDatabase.GROUPS_TABLE,
            arrayOf(
                TabDatabase.COL_GROUP_ID,
                TabDatabase.COL_GROUP_NAME,
                TabDatabase.COL_GROUP_COLOR,
                TabDatabase.COL_GROUP_COLLAPSED
            ),
            null, null, null, null,
            "${TabDatabase.COL_GROUP_POSITION} ASC"
        )
        val list = mutableListOf<TabGroup>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    TabGroup(
                        id = it.getString(0),
                        name = it.getString(1) ?: "",
                        color = it.getInt(2),
                        collapsed = it.getInt(3) == 1
                    )
                )
            }
        }
        return list
    }

    fun isEmpty(context: Context): Boolean {
        val cursor = db(context).readableDatabase.query(
            TabDatabase.TABLE,
            arrayOf(TabDatabase.COL_ID),
            null, null, null, null, null,
            "1"
        )
        return cursor.use { !it.moveToFirst() }
    }

    fun removeProfile(context: Context, profileId: String) {
        val writable = db(context).writableDatabase
        writable.delete(TabDatabase.TABLE, "${TabDatabase.COL_PROFILE_ID} = ?", arrayOf(profileId))
        writable.delete(TabDatabase.CLOSED_TABLE, "${TabDatabase.COL_PROFILE_ID} = ?", arrayOf(profileId))
    }

    fun clear(context: Context) {
        val writable = db(context).writableDatabase
        writable.delete(TabDatabase.TABLE, null, null)
        writable.delete(TabDatabase.GROUPS_TABLE, null, null)
    }
}
