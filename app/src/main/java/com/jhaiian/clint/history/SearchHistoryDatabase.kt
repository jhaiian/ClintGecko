package com.jhaiian.clint.history

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class SearchHistoryDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        createTable(db, TABLE)
        db.execSQL("CREATE INDEX idx_search_history_profile_id ON $TABLE($COL_PROFILE_ID)")
    }

    private fun createTable(db: SQLiteDatabase, name: String) {
        db.execSQL(
            """CREATE TABLE $name (
                $COL_ID         INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_QUERY      TEXT    NOT NULL,
                $COL_TITLE      TEXT    NOT NULL DEFAULT '',
                $COL_TIMESTAMP  INTEGER NOT NULL,
                $COL_PROFILE_ID TEXT    NOT NULL DEFAULT '$DEFAULT_PROFILE',
                UNIQUE($COL_QUERY, $COL_PROFILE_ID)
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_TITLE TEXT NOT NULL DEFAULT ''")
        }
        if (oldVersion < 3) {
            createTable(db, REBUILD_TABLE)
            db.execSQL(
                """INSERT INTO $REBUILD_TABLE ($COL_ID, $COL_QUERY, $COL_TITLE, $COL_TIMESTAMP)
                    SELECT $COL_ID, $COL_QUERY, $COL_TITLE, $COL_TIMESTAMP FROM $TABLE"""
            )
            db.execSQL("DROP TABLE $TABLE")
            db.execSQL("ALTER TABLE $REBUILD_TABLE RENAME TO $TABLE")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_search_history_profile_id ON $TABLE($COL_PROFILE_ID)")
        }
    }

    companion object {
        const val DB_NAME       = "clint_search_history.db"
        const val DB_VERSION    = 3
        const val DEFAULT_PROFILE = "default"
        private const val REBUILD_TABLE = "search_history_rebuild"
        const val TABLE         = "search_history"
        const val COL_ID        = "id"
        const val COL_QUERY     = "query"
        const val COL_TITLE     = "title"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_PROFILE_ID = "profile_id"
    }
}
