package com.jhaiian.clint.blocker.engine

import android.content.Context
import com.jhaiian.clint.blocker.WebsiteBlockerCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object WebsiteBlockerCompiler {

    suspend fun compile(
        context: Context,
        categories: List<WebsiteBlockerCategory>,
        additionalWebsiteHosts: List<String>,
        onProgress: suspend (WebsiteBlockerCompileProgress) -> Unit
    ): WebsiteBlockerCompileResult = withContext(Dispatchers.IO) {
        val activeCategories = categories.filter { it.isEnabled && it.isDownloaded }
        val totalItems = activeCategories.size + 1
        var processed = 0

        val builderPtr = WebsiteBlockerNative.nativeCreateBuilder()
        if (builderPtr == 0L) {
            return@withContext WebsiteBlockerCompileResult(false, 0, 0, "Failed to initialize compiler")
        }

        try {
            for (category in activeCategories) {
                onProgress(WebsiteBlockerCompileProgress(category.id, processed, totalItems))
                val file = WebsiteBlockerPaths.categoryFile(context, category.id)
                if (file.exists()) {
                    WebsiteBlockerNative.nativeAddText(builderPtr, file.readText())
                }
                processed++
            }

            onProgress(WebsiteBlockerCompileProgress("additional", processed, totalItems))
            val additionalText = additionalWebsiteHosts.joinToString("\n")
            val additionalFile = WebsiteBlockerPaths.additionalWebsitesFile(context)
            additionalFile.writeText(additionalText)
            if (additionalText.isNotBlank()) {
                WebsiteBlockerNative.nativeAddText(builderPtr, additionalText)
            }
            processed++

            val tempFile = WebsiteBlockerPaths.engineTempFile(context)
            if (tempFile.exists()) tempFile.delete()

            val resultJson = JSONObject(
                WebsiteBlockerNative.nativeFinalizeBuilder(builderPtr, tempFile.absolutePath)
            )
            val success = resultJson.optBoolean("success", false)
            val domainCount = resultJson.optLong("domainCount", 0L)
            val sizeBytes = resultJson.optLong("sizeBytes", 0L)

            if (!success || !tempFile.exists()) {
                return@withContext WebsiteBlockerCompileResult(false, 0, 0, "Failed to write compiled database")
            }

            val engineFile = WebsiteBlockerPaths.engineFile(context)
            if (engineFile.exists()) engineFile.delete()
            if (!tempFile.renameTo(engineFile)) {
                return@withContext WebsiteBlockerCompileResult(false, 0, 0, "Failed to activate compiled database")
            }

            CompiledWebsiteBlockerManifest(
                compiledAt = System.currentTimeMillis(),
                domainCount = domainCount,
                sizeBytes = sizeBytes,
                enabledCategoryIds = activeCategories.map { it.id }.toSet(),
                additionalWebsitesCount = additionalWebsiteHosts.size
            ).write(WebsiteBlockerPaths.manifestFile(context))

            WebsiteBlockerCompileResult(true, domainCount, sizeBytes)
        } finally {
            WebsiteBlockerNative.nativeDestroyBuilder(builderPtr)
        }
    }
}
