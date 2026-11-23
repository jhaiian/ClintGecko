package com.jhaiian.clint.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.preference.PreferenceManager
import java.util.Locale

object LocaleHelper {
    const val PREF_APP_LANGUAGE = "app_language"
    const val LANGUAGE_SYSTEM = "system"
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_ARABIC = "ar"
    const val LANGUAGE_CHINESE_SIMPLIFIED = "zh-CN"
    const val LANGUAGE_FRENCH = "fr"
    const val LANGUAGE_GERMAN = "de"
    const val LANGUAGE_FILIPINO = "fil"
    const val LANGUAGE_HINDI = "hi"
    const val LANGUAGE_INDONESIAN = "in"
    const val LANGUAGE_PORTUGUESE_BRAZIL = "pt-BR"
    const val LANGUAGE_RUSSIAN = "ru"
    const val LANGUAGE_SPANISH = "es"
    const val LANGUAGE_VIETNAMESE = "vi"
    const val BASE_LANGUAGE_TAG = LANGUAGE_ENGLISH

    private val SUPPORTED_LANGUAGE_TAGS = listOf(
        LANGUAGE_ENGLISH,
        LANGUAGE_ARABIC,
        LANGUAGE_CHINESE_SIMPLIFIED,
        LANGUAGE_FILIPINO,
        LANGUAGE_FRENCH,
        LANGUAGE_GERMAN,
        LANGUAGE_HINDI,
        LANGUAGE_INDONESIAN,
        LANGUAGE_PORTUGUESE_BRAZIL,
        LANGUAGE_RUSSIAN,
        LANGUAGE_SPANISH,
        LANGUAGE_VIETNAMESE
    )

    fun wrapContext(context: Context): Context {
        val locale = resolveEffectiveLocale(context)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun resolveEffectiveLocale(context: Context): Locale {
        val stored = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(PREF_APP_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM
        if (stored in SUPPORTED_LANGUAGE_TAGS) return Locale.forLanguageTag(stored)
        return supportedSystemLocale()
    }

    private fun supportedSystemLocale(): Locale {
        val system = Resources.getSystem().configuration.locales[0]
        return when (system.language) {
            "tl" -> Locale.forLanguageTag(LANGUAGE_FILIPINO)
            "id" -> Locale.forLanguageTag(LANGUAGE_INDONESIAN)
            "pt" -> Locale.forLanguageTag(LANGUAGE_PORTUGUESE_BRAZIL)
            "zh" -> Locale.forLanguageTag(if (isTraditionalChinese(system)) BASE_LANGUAGE_TAG else LANGUAGE_CHINESE_SIMPLIFIED)
            in SUPPORTED_LANGUAGE_TAGS -> system
            else -> Locale.forLanguageTag(BASE_LANGUAGE_TAG)
        }
    }

    private fun isTraditionalChinese(locale: Locale): Boolean {
        if (locale.script.isNotEmpty()) return locale.script == "Hant"
        return locale.country in setOf("TW", "HK", "MO")
    }
}
