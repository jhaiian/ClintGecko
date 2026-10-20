package com.jhaiian.clint.tabs

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class TabDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE (
                $COL_ID       INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_POSITION INTEGER NOT NULL,
                $COL_URL      TEXT    NOT NULL,
                $COL_TITLE    TEXT    NOT NULL DEFAULT '',
                $COL_ACTIVE   INTEGER NOT NULL DEFAULT 0,
                $COL_TAB_ID   TEXT,
                $COL_SHORTCUT_ID TEXT,
                $COL_LAST_ACTIVE INTEGER NOT NULL DEFAULT 0,
                $COL_PROFILE_ID TEXT NOT NULL DEFAULT 'default',
                $COL_GROUP_ID TEXT
            )"""
        )
        createGroupsTable(db)
    }

    private fun createGroupsTable(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS $GROUPS_TABLE (
                $COL_GROUP_ID        TEXT    PRIMARY KEY,
                $COL_GROUP_POSITION  INTEGER NOT NULL,
                $COL_GROUP_NAME      TEXT    NOT NULL DEFAULT '',
                $COL_GROUP_COLOR     INTEGER NOT NULL,
                $COL_GROUP_COLLAPSED INTEGER NOT NULL DEFAULT 0
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

        if (oldVersion < 3) {
            addColumnIfMissing(db, COL_TAB_ID, "TEXT")
        }
        if (oldVersion < 4) {
            addColumnIfMissing(db, COL_SHORTCUT_ID, "TEXT")
        }
        if (oldVersion < 5) {
            addColumnIfMissing(db, COL_LAST_ACTIVE, "INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
        }
        if (oldVersion < 6) {
            addColumnIfMissing(db, COL_PROFILE_ID, "TEXT NOT NULL DEFAULT 'default'")
        }
        if (oldVersion < 7) {
            addColumnIfMissing(db, COL_GROUP_ID, "TEXT")
            createGroupsTable(db)
        }
    }

    private fun addColumnIfMissing(db: SQLiteDatabase, column: String, definition: String) {
        val exists = db.rawQuery("PRAGMA table_info($TABLE)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            var found = false
            while (!found && cursor.moveToNext()) found = cursor.getString(nameIndex) == column
            found
        }
        if (!exists) db.execSQL("ALTER TABLE $TABLE ADD COLUMN $column $definition")
    }

    companion object {
        const val DB_NAME    = "clint_tabs.db"
        const val DB_VERSION = 7
        const val TABLE      = "tabs"
        const val COL_ID       = "id"
        const val COL_POSITION = "position"
        const val COL_URL      = "url"
        const val COL_TITLE    = "title"
        const val COL_ACTIVE   = "active"
        const val COL_TAB_ID   = "tab_id"
        const val COL_SHORTCUT_ID = "shortcut_id"
        const val COL_LAST_ACTIVE = "last_active"
        const val COL_PROFILE_ID = "profile_id"
        const val COL_GROUP_ID = "group_id"
        const val GROUPS_TABLE = "tab_groups"
        const val COL_GROUP_POSITION = "position"
        const val COL_GROUP_NAME = "name"
        const val COL_GROUP_COLOR = "color"
        const val COL_GROUP_COLLAPSED = "collapsed"
    }
}
