package com.jhaiian.clint.shortcuts

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class ShortcutSavedTab(
    val position: Int,
    val url: String,
    val title: String,
    val isActive: Boolean,
    val tabId: String
)

internal class ShortcutTabDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE (
                $COL_ID          INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SHORTCUT_ID TEXT    NOT NULL,
                $COL_POSITION    INTEGER NOT NULL,
                $COL_URL         TEXT    NOT NULL,
                $COL_TITLE       TEXT    NOT NULL,
                $COL_ACTIVE      INTEGER NOT NULL,
                $COL_TAB_ID      TEXT    NOT NULL
            )"""
        )
        db.execSQL("CREATE INDEX idx_${TABLE}_${COL_SHORTCUT_ID} ON $TABLE($COL_SHORTCUT_ID)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }

    companion object {
        const val DB_NAME = "clint_shortcut_tabs.db"
        const val DB_VERSION = 1
        const val TABLE = "shortcut_tabs"
        const val COL_ID = "id"
        const val COL_SHORTCUT_ID = "shortcut_id"
        const val COL_POSITION = "position"
        const val COL_URL = "url"
        const val COL_TITLE = "title"
        const val COL_ACTIVE = "active"
        const val COL_TAB_ID = "tab_id"
    }
}

object ShortcutTabSessionManager {

    @Volatile private var db: ShortcutTabDatabase? = null

    private fun db(context: Context): ShortcutTabDatabase {
        return db ?: synchronized(this) {
            db ?: ShortcutTabDatabase(context.applicationContext).also { db = it }
        }
    }

    fun save(context: Context, shortcutId: String, tabs: List<ShortcutSavedTab>) {
        val writable = db(context).writableDatabase
        writable.beginTransaction()
        try {
            writable.delete(ShortcutTabDatabase.TABLE, "${ShortcutTabDatabase.COL_SHORTCUT_ID} = ?", arrayOf(shortcutId))
            tabs.forEach { tab ->
                val values = ContentValues().apply {
                    put(ShortcutTabDatabase.COL_SHORTCUT_ID, shortcutId)
                    put(ShortcutTabDatabase.COL_POSITION, tab.position)
                    put(ShortcutTabDatabase.COL_URL, tab.url)
                    put(ShortcutTabDatabase.COL_TITLE, tab.title)
                    put(ShortcutTabDatabase.COL_ACTIVE, if (tab.isActive) 1 else 0)
                    put(ShortcutTabDatabase.COL_TAB_ID, tab.tabId)
                }
                writable.insert(ShortcutTabDatabase.TABLE, null, values)
            }
            writable.setTransactionSuccessful()
        } finally {
            writable.endTransaction()
        }
    }

    fun load(context: Context, shortcutId: String): List<ShortcutSavedTab> {
        val cursor = db(context).readableDatabase.query(
            ShortcutTabDatabase.TABLE,
            arrayOf(
                ShortcutTabDatabase.COL_POSITION,
                ShortcutTabDatabase.COL_URL,
                ShortcutTabDatabase.COL_TITLE,
                ShortcutTabDatabase.COL_ACTIVE,
                ShortcutTabDatabase.COL_TAB_ID
            ),
            "${ShortcutTabDatabase.COL_SHORTCUT_ID} = ?", arrayOf(shortcutId), null, null,
            "${ShortcutTabDatabase.COL_POSITION} ASC"
        )
        val list = mutableListOf<ShortcutSavedTab>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    ShortcutSavedTab(
                        position = it.getInt(0),
                        url = it.getString(1),
                        title = it.getString(2),
                        isActive = it.getInt(3) == 1,
                        tabId = it.getString(4)
                    )
                )
            }
        }
        return list
    }

    fun clear(context: Context, shortcutId: String) {
        db(context).writableDatabase.delete(
            ShortcutTabDatabase.TABLE, "${ShortcutTabDatabase.COL_SHORTCUT_ID} = ?", arrayOf(shortcutId)
        )
    }
}
