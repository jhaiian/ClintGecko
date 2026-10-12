package com.jhaiian.clint.history

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class SearchHistoryDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE (
                $COL_ID        INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_QUERY     TEXT    NOT NULL UNIQUE,
                $COL_TITLE     TEXT    NOT NULL DEFAULT '',
                $COL_TIMESTAMP INTEGER NOT NULL
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_TITLE TEXT NOT NULL DEFAULT ''")
        }
    }

    companion object {
        const val DB_NAME       = "clint_search_history.db"
        const val DB_VERSION    = 2
        const val TABLE         = "search_history"
        const val COL_ID        = "id"
        const val COL_QUERY     = "query"
        const val COL_TITLE     = "title"
        const val COL_TIMESTAMP = "timestamp"
    }
}
