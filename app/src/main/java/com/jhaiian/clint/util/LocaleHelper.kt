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
    const val LANGUAGE_FILIPINO = "fil"
    const val LANGUAGE_RUSSIAN = "ru"
    const val LANGUAGE_SPANISH = "es"
    const val BASE_LANGUAGE_TAG = LANGUAGE_ENGLISH

    private val SUPPORTED_LANGUAGE_TAGS = listOf(LANGUAGE_ENGLISH, LANGUAGE_FILIPINO, LANGUAGE_RUSSIAN, LANGUAGE_SPANISH)

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
            in SUPPORTED_LANGUAGE_TAGS -> system
            else -> Locale.forLanguageTag(BASE_LANGUAGE_TAG)
        }
    }
}
