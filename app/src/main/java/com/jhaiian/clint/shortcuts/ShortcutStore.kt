package com.jhaiian.clint.shortcuts

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.jhaiian.clint.profiles.WebProfiles

data class WebAppShortcut(
    val id: String,
    val url: String,
    val name: String,
    val iconPath: String?,
    val tabId: String?,
    val frameless: Boolean = true,
    val createdAt: Long = 0L,
    val profileId: String = WebProfiles.DEFAULT_ID
)

object ShortcutStore {

    @Volatile private var db: ShortcutDatabase? = null

    private val urlChangedIds: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    fun markUrlChanged(id: String) {
        urlChangedIds.add(id)
    }

    fun consumeUrlChanged(id: String): Boolean = urlChangedIds.remove(id)

    private val columns = arrayOf(
        ShortcutDatabase.COL_ID,
        ShortcutDatabase.COL_URL,
        ShortcutDatabase.COL_NAME,
        ShortcutDatabase.COL_ICON_PATH,
        ShortcutDatabase.COL_TAB_ID,
        ShortcutDatabase.COL_FRAMELESS,
        ShortcutDatabase.COL_CREATED_AT,
        ShortcutDatabase.COL_PROFILE_ID
    )

    private fun db(context: Context): ShortcutDatabase {
        return db ?: synchronized(this) {
            db ?: ShortcutDatabase(context.applicationContext).also { db = it }
        }
    }

    private fun Cursor.toShortcut() = WebAppShortcut(
        id = getString(0),
        url = getString(1),
        name = getString(2),
        iconPath = getString(3),
        tabId = getString(4),
        frameless = getInt(5) == 1,
        createdAt = getLong(6),
        profileId = getString(7) ?: WebProfiles.DEFAULT_ID
    )

    fun save(context: Context, shortcut: WebAppShortcut) {
        val values = ContentValues().apply {
            put(ShortcutDatabase.COL_ID, shortcut.id)
            put(ShortcutDatabase.COL_URL, shortcut.url)
            put(ShortcutDatabase.COL_NAME, shortcut.name)
            put(ShortcutDatabase.COL_ICON_PATH, shortcut.iconPath)
            put(ShortcutDatabase.COL_TAB_ID, shortcut.tabId)
            put(ShortcutDatabase.COL_CREATED_AT, if (shortcut.createdAt > 0L) shortcut.createdAt else System.currentTimeMillis())
            put(ShortcutDatabase.COL_FRAMELESS, if (shortcut.frameless) 1 else 0)
            put(ShortcutDatabase.COL_PROFILE_ID, shortcut.profileId)
        }
        db(context).writableDatabase.insertWithOnConflict(
            ShortcutDatabase.TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun get(context: Context, id: String): WebAppShortcut? {
        val cursor = db(context).readableDatabase.query(
            ShortcutDatabase.TABLE, columns,
            "${ShortcutDatabase.COL_ID} = ?", arrayOf(id), null, null, null
        )
        return cursor.use { if (it.moveToFirst()) it.toShortcut() else null }
    }

    fun getAll(context: Context): List<WebAppShortcut> {
        val cursor = db(context).readableDatabase.query(
            ShortcutDatabase.TABLE, columns, null, null, null, null,
            "${ShortcutDatabase.COL_CREATED_AT} DESC"
        )
        val list = mutableListOf<WebAppShortcut>()
        cursor.use { while (it.moveToNext()) list.add(it.toShortcut()) }
        return list
    }

    fun framelessIds(context: Context): Set<String> {
        val cursor = db(context).readableDatabase.query(
            ShortcutDatabase.TABLE, arrayOf(ShortcutDatabase.COL_ID),
            "${ShortcutDatabase.COL_FRAMELESS} = 1", null, null, null, null
        )
        val ids = mutableSetOf<String>()
        cursor.use { while (it.moveToNext()) ids.add(it.getString(0)) }
        return ids
    }

    fun update(context: Context, shortcut: WebAppShortcut) {
        val values = ContentValues().apply {
            put(ShortcutDatabase.COL_URL, shortcut.url)
            put(ShortcutDatabase.COL_NAME, shortcut.name)
            put(ShortcutDatabase.COL_ICON_PATH, shortcut.iconPath)
            put(ShortcutDatabase.COL_FRAMELESS, if (shortcut.frameless) 1 else 0)
            put(ShortcutDatabase.COL_PROFILE_ID, shortcut.profileId)
        }
        db(context).writableDatabase.update(
            ShortcutDatabase.TABLE, values, "${ShortcutDatabase.COL_ID} = ?", arrayOf(shortcut.id)
        )
    }

    fun clearProfile(context: Context, profileId: String) {
        val values = ContentValues().apply { put(ShortcutDatabase.COL_PROFILE_ID, WebProfiles.DEFAULT_ID) }
        db(context).writableDatabase.update(
            ShortcutDatabase.TABLE, values, "${ShortcutDatabase.COL_PROFILE_ID} = ?", arrayOf(profileId)
        )
    }

    fun updateTabId(context: Context, id: String, tabId: String) {
        val values = ContentValues().apply { put(ShortcutDatabase.COL_TAB_ID, tabId) }
        db(context).writableDatabase.update(
            ShortcutDatabase.TABLE, values, "${ShortcutDatabase.COL_ID} = ?", arrayOf(id)
        )
    }

    fun delete(context: Context, id: String) {
        db(context).writableDatabase.delete(
            ShortcutDatabase.TABLE, "${ShortcutDatabase.COL_ID} = ?", arrayOf(id)
        )
    }
}
