package com.jhaiian.clint.tabs

import android.content.ContentValues
import android.content.Context

data class SavedTab(
    val position: Int,
    val url: String,
    val title: String,
    val isActive: Boolean,
    val tabId: String,
    val shortcutId: String? = null,
    val lastActiveAt: Long = System.currentTimeMillis(),
    val profileId: String = "default",
    val groupId: String? = null
)

object TabSessionManager {

    @Volatile private var db: TabDatabase? = null

    private fun db(context: Context): TabDatabase {
        return db ?: synchronized(this) {
            db ?: TabDatabase(context.applicationContext).also { db = it }
        }
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
                TabDatabase.COL_GROUP_ID
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
                        groupId = it.getString(8)
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
        db(context).writableDatabase.delete(TabDatabase.TABLE, "${TabDatabase.COL_PROFILE_ID} = ?", arrayOf(profileId))
    }

    fun clear(context: Context) {
        val writable = db(context).writableDatabase
        writable.delete(TabDatabase.TABLE, null, null)
        writable.delete(TabDatabase.GROUPS_TABLE, null, null)
    }
}
