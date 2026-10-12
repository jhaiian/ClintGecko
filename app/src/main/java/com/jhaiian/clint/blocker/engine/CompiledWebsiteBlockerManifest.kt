package com.jhaiian.clint.blocker.engine

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class CompiledWebsiteBlockerManifest(
    val compiledAt: Long,
    val domainCount: Long,
    val sizeBytes: Long,
    val enabledCategoryIds: Set<String>,
    val additionalWebsitesCount: Int
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("compiledAt", compiledAt)
        put("domainCount", domainCount)
        put("sizeBytes", sizeBytes)
        put("enabledCategoryIds", JSONArray(enabledCategoryIds.toList()))
        put("additionalWebsitesCount", additionalWebsitesCount)
    }

    fun write(file: File) {
        file.writeText(toJson().toString())
    }

    companion object {
        fun read(file: File): CompiledWebsiteBlockerManifest? {
            if (!file.exists()) return null
            return runCatching {
                val json = JSONObject(file.readText())
                val ids = mutableSetOf<String>()
                val array = json.getJSONArray("enabledCategoryIds")
                for (i in 0 until array.length()) ids.add(array.getString(i))
                CompiledWebsiteBlockerManifest(
                    compiledAt = json.getLong("compiledAt"),
                    domainCount = json.getLong("domainCount"),
                    sizeBytes = json.getLong("sizeBytes"),
                    enabledCategoryIds = ids,
                    additionalWebsitesCount = json.optInt("additionalWebsitesCount", 0)
                )
            }.getOrNull()
        }
    }
}
