package com.jhaiian.clint.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class BackupResult(
    val includedCategories: Set<BackupCategory>,
    val skippedCategories: Set<BackupCategory>,
    val encrypted: Boolean,
    val totalBytes: Long
)

object BackupManager {

    suspend fun createBackup(
        context: Context,
        categories: Set<BackupCategory>,
        password: CharArray?,
        output: OutputStream,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> },
        onEncryptStart: () -> Unit = {}
    ): BackupResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val liveExtensionUuids = if (BackupCategory.EXTENSIONS in categories) {
            withContext(Dispatchers.Main) { com.jhaiian.clint.extensions.ClintExtensions.liveUuids() }
        } else emptyMap()
        if (BackupCategory.COOKIES in categories || BackupCategory.PROFILES in categories) {
            withContext(Dispatchers.Main) {
                com.jhaiian.clint.profiles.WebProfiles.flushAll()
            }
        }
        val tempZip = File.createTempFile("clint_backup_", ".zip", appContext.cacheDir)
        val extensionWorkDir = File(appContext.cacheDir, "clint_ext_backup_${System.nanoTime()}")
        val cookieWorkDir = File(appContext.cacheDir, "clint_cookie_backup_${System.nanoTime()}")
        try {
            data class PlannedFile(val entryId: String, val category: BackupCategory, val file: File, val zipPath: String)

            val planned = mutableListOf<PlannedFile>()
            for (category in categories) {
                for (target in BackupTargets.forCategory(category)) {
                    when (target.type) {
                        BackupEntryType.DATABASE, BackupEntryType.PREFS -> {
                            val file = target.file(appContext)
                            if (target.type == BackupEntryType.DATABASE && file.parentFile == appContext.getDatabasePath(file.name).parentFile) {
                                checkpointDatabase(file)
                            }
                            if (file.exists() && file.length() > 0) {
                                planned.add(PlannedFile(target.id, category, file, target.zipPath))
                            }
                        }
                        BackupEntryType.DIRECTORY -> {
                            val dir = target.file(appContext)
                            if (dir.exists() && dir.isDirectory) {
                                dir.listFiles()?.filter { it.isFile }?.forEach { child ->
                                    planned.add(PlannedFile("${target.id}:${child.name}", category, child, "${target.zipPath}/${child.name}"))
                                }
                            }
                        }
                    }
                }
            }

            for (item in CookieBackup.prepare(appContext, categories, cookieWorkDir)) {
                planned.add(PlannedFile(item.entryId, item.category, item.file, item.zipPath))
            }

            if (BackupCategory.EXTENSIONS in categories) {
                for (item in ExtensionBackup.prepare(appContext, liveExtensionUuids, extensionWorkDir)) {
                    planned.add(PlannedFile("${ExtensionBackup.ID_PREFIX}${item.relPath}", BackupCategory.EXTENSIONS, item.file, "${ExtensionBackup.ZIP_ROOT}${item.relPath}"))
                }
            }

            val total = planned.size
            var completed = 0
            onProgress(completed, total)

            val manifestEntries = mutableListOf<BackupManifestEntry>()
            val includedCategories = mutableSetOf<BackupCategory>()

            ZipOutputStream(FileOutputStream(tempZip)).use { zip ->
                for (planFile in planned) {
                    addFileToZip(zip, planFile.file, planFile.zipPath)
                    manifestEntries.add(BackupManifestEntry(planFile.entryId, planFile.category.id, planFile.zipPath, planFile.file.length()))
                    includedCategories.add(planFile.category)
                    completed++
                    onProgress(completed, total)
                }

                val packageInfo = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
                val manifest = BackupManifest(
                    format = BACKUP_FORMAT_MAGIC,
                    formatVersion = BACKUP_FORMAT_VERSION,
                    appVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
                    appVersionName = packageInfo.versionName ?: "",
                    createdAt = System.currentTimeMillis(),
                    categories = includedCategories.map { it.id },
                    entries = manifestEntries
                )
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toJson().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            val skippedCategories = categories - includedCategories

            var encrypted = false
            if (password != null && password.isNotEmpty()) {
                val salt = BackupCrypto.randomSalt()
                val iv = BackupCrypto.randomIv()
                onEncryptStart()
                val key = BackupCrypto.deriveKey(password, salt, ARGON2_MEMORY_KB, ARGON2_ITERATIONS, ARGON2_PARALLELISM)
                BackupCrypto.writeEncryptedHeader(output, salt, iv, ARGON2_MEMORY_KB, ARGON2_ITERATIONS, ARGON2_PARALLELISM)
                FileInputStream(tempZip).use { input ->
                    BackupCrypto.encryptStream(input, output, key, iv)
                }
                key.fill(0)
                encrypted = true
            } else {
                BackupCrypto.writeUnencryptedHeader(output)
                FileInputStream(tempZip).use { input ->
                    input.copyTo(output)
                }
            }
            output.flush()

            BackupResult(includedCategories, skippedCategories, encrypted, tempZip.length())
        } finally {
            tempZip.delete()
            extensionWorkDir.deleteRecursively()
            cookieWorkDir.deleteRecursively()
            password?.fill('\u0000')
        }
    }

    private fun checkpointDatabase(file: File) {
        if (!file.exists()) return
        runCatching {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() }
            }
        }
    }

    private fun addFileToZip(zip: ZipOutputStream, file: File, zipPath: String) {
        zip.putNextEntry(ZipEntry(zipPath))
        FileInputStream(file).use { it.copyTo(zip) }
        zip.closeEntry()
    }
}
