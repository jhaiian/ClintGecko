package com.jhaiian.clint.mediacapture

import android.net.Uri
import java.util.Locale

object MediaLanguageGuess {

    private val isoCodes: Set<String> by lazy { Locale.getISOLanguages().toSet() }

    private val namesToCodes: Map<String, String> by lazy {
        Locale.getISOLanguages()
            .associateBy { code ->
                Locale.forLanguageTag(code).getDisplayLanguage(Locale.ENGLISH).lowercase(Locale.ROOT)
            }
            .filterKeys { it.length >= 4 }
    }

    private val iso3ToCodes: Map<String, String> by lazy {
        Locale.getISOLanguages()
            .mapNotNull { code -> runCatching { Locale.forLanguageTag(code).isO3Language to code }.getOrNull() }
            .toMap()
    }

    private val separator = Regex("[._\\-\\s]+")

    fun fromUrl(url: String): String? {
        val fileName = runCatching { Uri.parse(url).lastPathSegment }.getOrNull() ?: return null
        val base = fileName.substringBeforeLast('.', fileName)
        val tokens = base.split(separator).filter { it.isNotEmpty() }.map { it.lowercase(Locale.ROOT) }
        if (tokens.isEmpty()) return null
        tokens.firstNotNullOfOrNull { namesToCodes[it] }?.let { return it }
        if (tokens.size <= 2) codeOf(tokens[0])?.let { return it }
        if ('.' in base) codeOf(base.substringAfterLast('.').lowercase(Locale.ROOT))?.let { return it }
        return null
    }

    private fun codeOf(token: String): String? = when {
        token.length == 2 && token in isoCodes -> token
        token.length == 3 -> iso3ToCodes[token]
        else -> null
    }
}
