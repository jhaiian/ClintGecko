package com.jhaiian.clint.shortcuts

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.preference.PreferenceManager
import com.jhaiian.clint.profiles.WebProfiles

internal class ShortcutDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    private val appContext: Context = context.applicationContext

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE (
                $COL_ID         TEXT    PRIMARY KEY,
                $COL_URL        TEXT    NOT NULL,
                $COL_NAME       TEXT    NOT NULL,
                $COL_ICON_PATH  TEXT,
                $COL_TAB_ID     TEXT,
                $COL_CREATED_AT INTEGER NOT NULL,
                $COL_FRAMELESS  INTEGER NOT NULL DEFAULT 1,
                $COL_PROFILE_ID TEXT    NOT NULL DEFAULT '${WebProfiles.DEFAULT_ID}'
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_FRAMELESS INTEGER NOT NULL DEFAULT 1")
            val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
            if (!prefs.getBoolean(LEGACY_FRAMELESS_PREF, true)) {
                db.execSQL("UPDATE $TABLE SET $COL_FRAMELESS = 0")
            }
            prefs.edit().remove(LEGACY_FRAMELESS_PREF).apply()
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_PROFILE_ID TEXT NOT NULL DEFAULT '${WebProfiles.DEFAULT_ID}'")
        }
    }

    companion object {
        const val DB_NAME = "clint_shortcuts.db"
        const val DB_VERSION = 3
        const val TABLE = "shortcuts"
        const val COL_ID = "id"
        const val COL_URL = "url"
        const val COL_NAME = "name"
        const val COL_ICON_PATH = "icon_path"
        const val COL_TAB_ID = "tab_id"
        const val COL_CREATED_AT = "created_at"
        const val COL_FRAMELESS = "frameless"
        const val COL_PROFILE_ID = "profile_id"
        private const val LEGACY_FRAMELESS_PREF = "shortcut_frameless_enabled"
    }
}
