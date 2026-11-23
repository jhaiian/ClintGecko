package com.jhaiian.clint.bookmarks

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class BookmarkDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $FOLDER_TABLE (
                $COL_FOLDER_ID          INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_FOLDER_NAME        TEXT    NOT NULL,
                $COL_FOLDER_PARENT_ID   INTEGER,
                $COL_FOLDER_CREATED_AT  INTEGER NOT NULL DEFAULT 0,
                $COL_FOLDER_PROFILE_ID  TEXT    NOT NULL DEFAULT '$DEFAULT_PROFILE'
            )"""
        )
        createBookmarksTable(db, TABLE)
        db.execSQL("CREATE INDEX idx_bookmarks_folder_id ON $TABLE($COL_BOOKMARK_FOLDER_ID)")
        db.execSQL("CREATE INDEX idx_bookmarks_profile_id ON $TABLE($COL_PROFILE_ID)")
        db.execSQL("CREATE INDEX idx_bookmark_folders_parent_id ON $FOLDER_TABLE($COL_FOLDER_PARENT_ID)")
        db.execSQL("CREATE INDEX idx_bookmark_folders_profile_id ON $FOLDER_TABLE($COL_FOLDER_PROFILE_ID)")
    }

    private fun createBookmarksTable(db: SQLiteDatabase, name: String) {
        db.execSQL(
            """CREATE TABLE $name (
                $COL_ID              INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_URL             TEXT    NOT NULL,
                $COL_TITLE           TEXT    NOT NULL DEFAULT '',
                $COL_FAVICON_URL     TEXT    NOT NULL DEFAULT '',
                $COL_ADDED_AT        INTEGER NOT NULL DEFAULT 0,
                $COL_LAST_VISIT      INTEGER NOT NULL DEFAULT 0,
                $COL_BOOKMARK_FOLDER_ID INTEGER,
                $COL_PROFILE_ID      TEXT    NOT NULL DEFAULT '$DEFAULT_PROFILE',
                UNIQUE($COL_URL, $COL_PROFILE_ID)
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_LAST_VISIT INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 3) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS $FOLDER_TABLE (
                    $COL_FOLDER_ID          INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_FOLDER_NAME        TEXT    NOT NULL,
                    $COL_FOLDER_PARENT_ID   INTEGER,
                    $COL_FOLDER_CREATED_AT  INTEGER NOT NULL DEFAULT 0
                )"""
            )
            db.execSQL("ALTER TABLE $TABLE ADD COLUMN $COL_BOOKMARK_FOLDER_ID INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookmarks_folder_id ON $TABLE($COL_BOOKMARK_FOLDER_ID)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookmark_folders_parent_id ON $FOLDER_TABLE($COL_FOLDER_PARENT_ID)")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE $FOLDER_TABLE ADD COLUMN $COL_FOLDER_PROFILE_ID TEXT NOT NULL DEFAULT '$DEFAULT_PROFILE'")
            createBookmarksTable(db, REBUILD_TABLE)
            db.execSQL(
                """INSERT INTO $REBUILD_TABLE
                    ($COL_ID, $COL_URL, $COL_TITLE, $COL_FAVICON_URL, $COL_ADDED_AT, $COL_LAST_VISIT, $COL_BOOKMARK_FOLDER_ID)
                    SELECT $COL_ID, $COL_URL, $COL_TITLE, $COL_FAVICON_URL, $COL_ADDED_AT, $COL_LAST_VISIT, $COL_BOOKMARK_FOLDER_ID
                    FROM $TABLE"""
            )
            db.execSQL("DROP TABLE $TABLE")
            db.execSQL("ALTER TABLE $REBUILD_TABLE RENAME TO $TABLE")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookmarks_folder_id ON $TABLE($COL_BOOKMARK_FOLDER_ID)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookmarks_profile_id ON $TABLE($COL_PROFILE_ID)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookmark_folders_profile_id ON $FOLDER_TABLE($COL_FOLDER_PROFILE_ID)")
        }
    }

    companion object {
        const val DB_NAME        = "clint_bookmarks.db"
        const val DB_VERSION     = 4
        const val DEFAULT_PROFILE = "default"
        private const val REBUILD_TABLE = "bookmarks_rebuild"

        const val TABLE                  = "bookmarks"
        const val COL_ID                 = "id"
        const val COL_URL                = "url"
        const val COL_TITLE              = "title"
        const val COL_FAVICON_URL        = "favicon_url"
        const val COL_ADDED_AT           = "added_at"
        const val COL_LAST_VISIT         = "last_visit"
        const val COL_BOOKMARK_FOLDER_ID = "folder_id"
        const val COL_PROFILE_ID         = "profile_id"

        const val FOLDER_TABLE          = "bookmark_folders"
        const val COL_FOLDER_ID         = "id"
        const val COL_FOLDER_NAME       = "name"
        const val COL_FOLDER_PARENT_ID  = "parent_id"
        const val COL_FOLDER_CREATED_AT = "created_at"
        const val COL_FOLDER_PROFILE_ID = "profile_id"
    }
}
