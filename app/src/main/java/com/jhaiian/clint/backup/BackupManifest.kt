package com.jhaiian.clint.backup

import org.json.JSONArray
import org.json.JSONObject

const val BACKUP_FORMAT_MAGIC = "clint_backup"
const val BACKUP_FORMAT_VERSION = 1

data class BackupManifestEntry(
    val id: String,
    val category: String,
    val zipPath: String,
    val sizeBytes: Long
)

data class BackupManifest(
    val format: String,
    val formatVersion: Int,
    val appVersionCode: Long,
    val appVersionName: String,
    val createdAt: Long,
    val categories: List<String>,
    val entries: List<BackupManifestEntry>
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("format", format)
        root.put("formatVersion", formatVersion)
        root.put("appVersionCode", appVersionCode)
        root.put("appVersionName", appVersionName)
        root.put("createdAt", createdAt)
        root.put("categories", JSONArray(categories))
        val entriesArr = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject()
            obj.put("id", entry.id)
            obj.put("category", entry.category)
            obj.put("zipPath", entry.zipPath)
            obj.put("sizeBytes", entry.sizeBytes)
            entriesArr.put(obj)
        }
        root.put("entries", entriesArr)
        return root.toString()
    }

    companion object {
        fun fromJson(json: String): BackupManifest? {
            return runCatching {
                val root = JSONObject(json)
                val categoriesArr = root.optJSONArray("categories") ?: JSONArray()
                val categories = (0 until categoriesArr.length()).map { categoriesArr.getString(it) }
                val entriesArr = root.optJSONArray("entries") ?: JSONArray()
                val entries = (0 until entriesArr.length()).map { i ->
                    val obj = entriesArr.getJSONObject(i)
                    BackupManifestEntry(
                        id = obj.getString("id"),
                        category = obj.getString("category"),
                        zipPath = obj.getString("zipPath"),
                        sizeBytes = obj.optLong("sizeBytes", 0L)
                    )
                }
                BackupManifest(
                    format = root.getString("format"),
                    formatVersion = root.getInt("formatVersion"),
                    appVersionCode = root.optLong("appVersionCode", 0L),
                    appVersionName = root.optString("appVersionName", ""),
                    createdAt = root.optLong("createdAt", 0L),
                    categories = categories,
                    entries = entries
                )
            }.getOrNull()
        }
    }
}
