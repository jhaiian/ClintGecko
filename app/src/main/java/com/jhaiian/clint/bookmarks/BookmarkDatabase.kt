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
                $COL_FOLDER_CREATED_AT  INTEGER NOT NULL DEFAULT 0
            )"""
        )
        db.execSQL(
            """CREATE TABLE $TABLE (
                $COL_ID              INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_URL             TEXT    NOT NULL UNIQUE,
                $COL_TITLE           TEXT    NOT NULL DEFAULT '',
                $COL_FAVICON_URL     TEXT    NOT NULL DEFAULT '',
                $COL_ADDED_AT        INTEGER NOT NULL DEFAULT 0,
                $COL_LAST_VISIT      INTEGER NOT NULL DEFAULT 0,
                $COL_BOOKMARK_FOLDER_ID INTEGER
            )"""
        )
        db.execSQL("CREATE INDEX idx_bookmarks_folder_id ON $TABLE($COL_BOOKMARK_FOLDER_ID)")
        db.execSQL("CREATE INDEX idx_bookmark_folders_parent_id ON $FOLDER_TABLE($COL_FOLDER_PARENT_ID)")
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
    }

    companion object {
        const val DB_NAME        = "clint_bookmarks.db"
        const val DB_VERSION     = 3

        const val TABLE                  = "bookmarks"
        const val COL_ID                 = "id"
        const val COL_URL                = "url"
        const val COL_TITLE              = "title"
        const val COL_FAVICON_URL        = "favicon_url"
        const val COL_ADDED_AT           = "added_at"
        const val COL_LAST_VISIT         = "last_visit"
        const val COL_BOOKMARK_FOLDER_ID = "folder_id"

        const val FOLDER_TABLE          = "bookmark_folders"
        const val COL_FOLDER_ID         = "id"
        const val COL_FOLDER_NAME       = "name"
        const val COL_FOLDER_PARENT_ID  = "parent_id"
        const val COL_FOLDER_CREATED_AT = "created_at"
    }
}
