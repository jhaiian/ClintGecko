package com.jhaiian.clint.downloads

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class DownloadDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE (
                $COL_ID                INTEGER PRIMARY KEY,
                $COL_URL               TEXT    NOT NULL,
                $COL_FILENAME          TEXT    NOT NULL,
                $COL_USER_AGENT        TEXT    NOT NULL DEFAULT '',
                $COL_REFERER           TEXT    NOT NULL DEFAULT '',
                $COL_COOKIES           TEXT    NOT NULL DEFAULT '',
                $COL_BYTES_DOWNLOADED  INTEGER NOT NULL DEFAULT 0,
                $COL_TOTAL_BYTES       INTEGER NOT NULL DEFAULT -1,
                $COL_STATUS            TEXT    NOT NULL,
                $COL_FILE_PATH         TEXT,
                $COL_ERROR_MESSAGE     TEXT,
                $COL_STARTED_AT        INTEGER NOT NULL DEFAULT 0,
                $COL_RESUMABLE         INTEGER NOT NULL DEFAULT 0,
                $COL_CONTENT_URI       TEXT,
                $COL_ACTIVE_ELAPSED_MS INTEGER NOT NULL DEFAULT 0,
                $COL_COMPLETED_AT      INTEGER NOT NULL DEFAULT 0,
                $COL_RETRY_ENABLED     INTEGER NOT NULL DEFAULT 1,
                $COL_UNMETERED_ONLY    INTEGER NOT NULL DEFAULT 0,
                $COL_SPLIT_PARTS       INTEGER NOT NULL DEFAULT 32,
                $COL_MULTITHREADING    INTEGER NOT NULL DEFAULT 4,
                $COL_LOCATION_MODE     TEXT    NOT NULL DEFAULT 'default',
                $COL_CUSTOM_LOC_URI    TEXT,
                $COL_CATEGORIZE_ENABLED INTEGER NOT NULL DEFAULT 0,
                $COL_COMPLETED_PARTS_MASK INTEGER NOT NULL DEFAULT 0,
                $COL_PART_OFFSETS      TEXT    NOT NULL DEFAULT '',
                $COL_SCHEDULED_START_AT INTEGER NOT NULL DEFAULT 0,
                $COL_WAITING_CUSTOM_SCHEDULE INTEGER NOT NULL DEFAULT 0,
                $COL_SPEED_LIMIT_BPS   INTEGER NOT NULL DEFAULT 0,
                $COL_STREAM_JSON       TEXT
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_COMPLETED_PARTS_MASK INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_PART_OFFSETS TEXT NOT NULL DEFAULT ''")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_SCHEDULED_START_AT INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_WAITING_CUSTOM_SCHEDULE INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_SPEED_LIMIT_BPS INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_STREAM_JSON TEXT")
        }
        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_CATEGORIZE_ENABLED INTEGER NOT NULL DEFAULT 0")
        }
    }

    companion object {
        const val DB_NAME                = "clint_downloads.db"
        const val DB_VERSION             = 7
        const val TABLE                  = "downloads"
        const val COL_ID                 = "id"
        const val COL_URL                = "url"
        const val COL_FILENAME           = "filename"
        const val COL_USER_AGENT         = "user_agent"
        const val COL_REFERER            = "referer"
        const val COL_COOKIES            = "cookies"
        const val COL_BYTES_DOWNLOADED   = "bytes_downloaded"
        const val COL_TOTAL_BYTES        = "total_bytes"
        const val COL_STATUS             = "status"
        const val COL_FILE_PATH          = "file_path"
        const val COL_ERROR_MESSAGE      = "error_message"
        const val COL_STARTED_AT         = "started_at"
        const val COL_RESUMABLE          = "resumable"
        const val COL_CONTENT_URI        = "content_uri"
        const val COL_ACTIVE_ELAPSED_MS  = "active_elapsed_ms"
        const val COL_COMPLETED_AT       = "completed_at"
        const val COL_RETRY_ENABLED      = "retry_enabled"
        const val COL_UNMETERED_ONLY     = "unmetered_only"
        const val COL_SPLIT_PARTS        = "split_parts"
        const val COL_MULTITHREADING     = "multithreading_parts"
        const val COL_LOCATION_MODE      = "location_mode"
        const val COL_CUSTOM_LOC_URI     = "custom_location_uri"
        const val COL_CATEGORIZE_ENABLED = "categorize_enabled"
        const val COL_COMPLETED_PARTS_MASK = "completed_parts_mask"
        const val COL_PART_OFFSETS       = "part_offsets"
        const val COL_SCHEDULED_START_AT = "scheduled_start_at"
        const val COL_WAITING_CUSTOM_SCHEDULE = "waiting_custom_schedule"
        const val COL_SPEED_LIMIT_BPS    = "speed_limit_bps"
        const val COL_STREAM_JSON        = "stream_json"
    }
}
