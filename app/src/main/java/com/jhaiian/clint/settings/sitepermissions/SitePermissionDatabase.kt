package com.jhaiian.clint.settings.sitepermissions

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class SitePermissionDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE (
                $COL_ID     INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ORIGIN TEXT    NOT NULL,
                $COL_TYPE   TEXT    NOT NULL,
                $COL_STATE  TEXT    NOT NULL,
                $COL_UPDATED INTEGER NOT NULL DEFAULT 0,
                UNIQUE($COL_ORIGIN, $COL_TYPE)
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_UPDATED INTEGER NOT NULL DEFAULT 0")
        }
    }

    companion object {
        const val DB_NAME           = "clint_site_permissions.db"
        const val DB_VERSION        = 2
        const val TABLE             = "site_permissions"
        const val COL_ID            = "id"
        const val COL_ORIGIN        = "origin"
        const val COL_TYPE          = "type"
        const val COL_STATE         = "state"
        const val COL_UPDATED       = "updated_at"
        const val TYPE_CAMERA        = "camera"
        const val TYPE_MIC           = "mic"
        const val TYPE_LOCATION      = "location"
        const val TYPE_NOTIFICATION  = "notification"
        const val TYPE_CLIPBOARD     = "clipboard"
        const val TYPE_DESKTOP_MODE            = "desktop_mode"
        const val TYPE_OPEN_IN_APP             = "open_in_app"
        const val TYPE_DATA_SAVER_EXCEPTION    = "data_saver_exception"
        const val STATE_ALLOW        = "allow"
        const val STATE_DENY         = "deny"
        const val STATE_ASK          = "ask"
        const val STATE_STAY         = "stay"
        const val STATE_OPEN         = "open"
    }
}
