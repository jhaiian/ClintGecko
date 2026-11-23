package com.jhaiian.clint.settings.sitepermissions

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class SitePermissionDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        createTable(db, TABLE)
        db.execSQL("CREATE INDEX idx_site_permissions_profile_id ON $TABLE($COL_PROFILE_ID)")
    }

    private fun createTable(db: SQLiteDatabase, name: String) {
        db.execSQL(
            """CREATE TABLE $name (
                $COL_ID     INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ORIGIN TEXT    NOT NULL,
                $COL_TYPE   TEXT    NOT NULL,
                $COL_STATE  TEXT    NOT NULL,
                $COL_UPDATED INTEGER NOT NULL DEFAULT 0,
                $COL_PROFILE_ID TEXT NOT NULL DEFAULT '$DEFAULT_PROFILE',
                UNIQUE($COL_ORIGIN, $COL_TYPE, $COL_PROFILE_ID)
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_UPDATED INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 3) {
            createTable(db, REBUILD_TABLE)
            db.execSQL(
                """INSERT INTO $REBUILD_TABLE ($COL_ID, $COL_ORIGIN, $COL_TYPE, $COL_STATE, $COL_UPDATED)
                    SELECT $COL_ID, $COL_ORIGIN, $COL_TYPE, $COL_STATE, $COL_UPDATED FROM $TABLE"""
            )
            db.execSQL("DROP TABLE $TABLE")
            db.execSQL("ALTER TABLE $REBUILD_TABLE RENAME TO $TABLE")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_site_permissions_profile_id ON $TABLE($COL_PROFILE_ID)")
        }
    }

    companion object {
        const val DB_NAME           = "clint_site_permissions.db"
        const val DB_VERSION        = 3
        const val DEFAULT_PROFILE   = "default"
        private const val REBUILD_TABLE = "site_permissions_rebuild"
        const val TABLE             = "site_permissions"
        const val COL_ID            = "id"
        const val COL_ORIGIN        = "origin"
        const val COL_TYPE          = "type"
        const val COL_STATE         = "state"
        const val COL_UPDATED       = "updated_at"
        const val COL_PROFILE_ID    = "profile_id"
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
