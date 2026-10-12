package com.jhaiian.clint.backup

import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

data class CookieBackupItem(val entryId: String, val category: BackupCategory, val file: File, val zipPath: String)

object CookieBackup {

    const val DEFAULT_ENTRY_ID = "cookies_db"
    const val DEFAULT_ZIP_PATH = "cookies/Cookies"
    const val PROFILES_ENTRY_ID = "cookies_profile:db"
    const val PROFILES_ZIP_PATH = "profiles/cookies/Cookies"
    const val CONTAINERS_ENTRY_ID = "cookies_profile:containers"
    const val CONTAINERS_ZIP_PATH = "profiles/cookies/containers.json"

    private const val PENDING_DIR = "clint_pending_cookies"
    private const val COOKIES_DB = "cookies.sqlite"
    private const val CONTAINERS_JSON = "containers.json"
    private const val FULL_COPY = "full.sqlite"
    private const val DEFAULT_PENDING = "default.sqlite"
    private const val PROFILES_PENDING = "profiles.sqlite"
    private const val TABLE = "moz_cookies"
    private const val CONTAINER_MATCH = "originAttributes LIKE '%userContextId=%'"
    private const val OPEN_FLAGS = SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.NO_LOCALIZED_COLLATORS

    fun prepare(context: Context, categories: Set<BackupCategory>, workDir: File): List<CookieBackupItem> {
        val wantDefault = BackupCategory.COOKIES in categories
        val wantProfiles = BackupCategory.PROFILES in categories
        if (!wantDefault && !wantProfiles) return emptyList()
        val profile = ExtensionBackup.profileDir(context) ?: return emptyList()
        val live = File(profile, COOKIES_DB)
        if (!live.isFile) return emptyList()

        workDir.mkdirs()
        val full = File(workDir, FULL_COPY)
        live.copyTo(full, overwrite = true)
        File(profile, "$COOKIES_DB-wal").takeIf { it.isFile }?.copyTo(File(workDir, "$FULL_COPY-wal"), overwrite = true)
        val merged = runCatching {
            SQLiteDatabase.openDatabase(full.path, null, OPEN_FLAGS).use { db ->
                db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
            }
        }.isSuccess
        if (!merged) return emptyList()

        val items = ArrayList<CookieBackupItem>()
        if (wantDefault) {
            val out = File(workDir, DEFAULT_PENDING)
            if (extractSubset(full, out, keepContainers = false)) {
                items.add(CookieBackupItem(DEFAULT_ENTRY_ID, BackupCategory.COOKIES, out, DEFAULT_ZIP_PATH))
            }
        }
        if (wantProfiles) {
            val out = File(workDir, PROFILES_PENDING)
            if (extractSubset(full, out, keepContainers = true)) {
                items.add(CookieBackupItem(PROFILES_ENTRY_ID, BackupCategory.PROFILES, out, PROFILES_ZIP_PATH))
                val containers = File(profile, CONTAINERS_JSON)
                if (containers.isFile && containers.length() > 0) {
                    items.add(CookieBackupItem(CONTAINERS_ENTRY_ID, BackupCategory.PROFILES, containers, CONTAINERS_ZIP_PATH))
                }
            }
        }
        return items
    }

    fun stage(context: Context, zip: ZipFile, category: BackupCategory, entries: List<BackupManifestEntry>, onEach: () -> Unit): Int {
        val root = File(context.filesDir, PENDING_DIR)
        val wanted = when (category) {
            BackupCategory.COOKIES -> mapOf(DEFAULT_ENTRY_ID to DEFAULT_PENDING)
            BackupCategory.PROFILES -> mapOf(PROFILES_ENTRY_ID to PROFILES_PENDING, CONTAINERS_ENTRY_ID to CONTAINERS_JSON)
            else -> return 0
        }
        root.mkdirs()
        wanted.values.forEach { File(root, it).delete() }
        var count = 0
        for (entry in entries) {
            val name = wanted[entry.id] ?: continue
            val zipEntry = zip.getEntry(entry.zipPath) ?: continue
            zip.getInputStream(zipEntry).use { input ->
                FileOutputStream(File(root, name)).use { output -> input.copyTo(output) }
            }
            count++
            onEach()
        }
        val usable = when (category) {
            BackupCategory.COOKIES -> File(root, DEFAULT_PENDING).isFile
            else -> File(root, PROFILES_PENDING).isFile
        }
        if (!usable) {
            wanted.values.forEach { File(root, it).delete() }
            return 0
        }
        return count
    }

    fun applyPending(context: Context) {
        val pending = File(context.filesDir, PENDING_DIR)
        if (!pending.isDirectory) return
        val profile = ExtensionBackup.profileDir(context) ?: return
        runCatching { apply(pending, profile) }
        pending.deleteRecursively()
    }

    private fun apply(pending: File, profile: File) {
        val live = File(profile, COOKIES_DB)
        val defaultFile = File(pending, DEFAULT_PENDING).takeIf { it.isFile }
        val profilesFile = File(pending, PROFILES_PENDING).takeIf { it.isFile }
        if (defaultFile != null) merge(live, defaultFile, containers = false)
        if (profilesFile != null) {
            merge(live, profilesFile, containers = true)
            File(pending, CONTAINERS_JSON).takeIf { it.isFile }?.let { src ->
                val dest = File(profile, CONTAINERS_JSON)
                val temp = File(profile, "$CONTAINERS_JSON.tmp")
                src.copyTo(temp, overwrite = true)
                if (!temp.renameTo(dest)) {
                    temp.copyTo(dest, overwrite = true)
                    temp.delete()
                }
            }
        }
    }

    private fun merge(live: File, backup: File, containers: Boolean) {
        if (!live.isFile) {
            backup.copyTo(live, overwrite = true)
            return
        }
        val matchSql = if (containers) CONTAINER_MATCH else "NOT ($CONTAINER_MATCH)"
        SQLiteDatabase.openDatabase(live.path, null, OPEN_FLAGS).use { db ->
            db.execSQL("ATTACH DATABASE ${DatabaseUtils.sqlEscapeString(backup.path)} AS bk")
            try {
                val mainCols = columns(db, "main")
                val bkCols = columns(db, "bk")
                val shared = mainCols.filter { it != "id" && it in bkCols }
                if (shared.isEmpty()) return
                val list = shared.joinToString(",") { "\"$it\"" }
                db.beginTransaction()
                try {
                    db.execSQL("DELETE FROM main.$TABLE WHERE $matchSql")
                    db.execSQL("INSERT OR REPLACE INTO main.$TABLE ($list) SELECT $list FROM bk.$TABLE WHERE $matchSql")
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            } finally {
                runCatching { db.execSQL("DETACH DATABASE bk") }
            }
        }
        File(live.parentFile, "$COOKIES_DB-wal").delete()
        File(live.parentFile, "$COOKIES_DB-shm").delete()
        File(live.parentFile, "$COOKIES_DB-journal").delete()
    }

    private fun columns(db: SQLiteDatabase, schema: String): Set<String> {
        val result = HashSet<String>()
        db.rawQuery("PRAGMA $schema.table_info($TABLE)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) result.add(cursor.getString(nameIndex))
        }
        return result
    }

    private fun extractSubset(full: File, out: File, keepContainers: Boolean): Boolean {
        full.copyTo(out, overwrite = true)
        val deleteSql = if (keepContainers) "NOT ($CONTAINER_MATCH)" else CONTAINER_MATCH
        val rows = runCatching {
            SQLiteDatabase.openDatabase(out.path, null, OPEN_FLAGS).use { db ->
                db.execSQL("DELETE FROM $TABLE WHERE $deleteSql")
                val count = DatabaseUtils.queryNumEntries(db, TABLE)
                if (count > 0) db.execSQL("VACUUM")
                count
            }
        }.getOrDefault(0L)
        if (rows <= 0L) {
            out.delete()
            return false
        }
        File(out.parentFile, "${out.name}-wal").delete()
        File(out.parentFile, "${out.name}-shm").delete()
        File(out.parentFile, "${out.name}-journal").delete()
        return true
    }
}
