package com.jhaiian.clint.profiles

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class ProfileDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE (
                $COL_POSITION INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ID       TEXT    NOT NULL UNIQUE,
                $COL_NAME     TEXT    NOT NULL,
                $COL_COLOR    INTEGER NOT NULL
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    companion object {
        const val DB_NAME      = "clint_profiles.db"
        const val DB_VERSION   = 1
        const val TABLE        = "profiles"
        const val COL_POSITION = "position"
        const val COL_ID       = "id"
        const val COL_NAME     = "name"
        const val COL_COLOR    = "color"
    }
}
