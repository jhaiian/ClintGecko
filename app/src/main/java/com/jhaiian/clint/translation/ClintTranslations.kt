package com.jhaiian.clint.translation

import android.content.Context
import androidx.preference.PreferenceManager
import com.jhaiian.clint.browser.webview.ClintWebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.mozilla.geckoview.GeckoPreferenceController
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.TranslationsController.RuntimeTranslation
import org.mozilla.geckoview.TranslationsController.SessionTranslation
import java.util.Locale
import kotlin.coroutines.resume

const val PREF_TRANSLATION_OFFER = "translation_offer"
const val PREF_TRANSLATION_SHOW_BUTTON = "translation_show_button"
const val PREF_TRANSLATION_TARGET = "translation_target_language"

enum class TranslationLanguageSetting(val geckoValue: String) {
    OFFER(RuntimeTranslation.OFFER),
    ALWAYS(RuntimeTranslation.ALWAYS),
    NEVER(RuntimeTranslation.NEVER);

    companion object {
        fun fromGecko(value: String?): TranslationLanguageSetting =
            entries.firstOrNull { it.geckoValue.equals(value, ignoreCase = true) } ?: OFFER
    }
}

data class TranslationLanguage(val code: String, val name: String)

data class TranslationLanguageSupport(
    val fromLanguages: List<TranslationLanguage>,
    val toLanguages: List<TranslationLanguage>
)

data class TranslationModel(
    val language: TranslationLanguage,
    val isDownloaded: Boolean,
    val size: Long
)

data class PageTranslationState(
    val docLanguage: String? = null,
    val userLanguage: String? = null,
    val isDocLanguageSupported: Boolean = false,
    val requestedFrom: String? = null,
    val requestedTo: String? = null,
    val isEngineReady: Boolean = false,
    val error: String? = null
) {
    val isRequested: Boolean get() = requestedTo != null
    val isTranslating: Boolean get() = isRequested && error == null && !isEngineReady
    val isTranslated: Boolean get() = isRequested && error == null && isEngineReady
    val canTranslate: Boolean
        get() = isDocLanguageSupported && !docLanguage.isNullOrEmpty() && !userLanguage.isNullOrEmpty() &&
            !ClintTranslations.sameLanguage(docLanguage, userLanguage)
}

val EmptyPageTranslationState: StateFlow<PageTranslationState> = MutableStateFlow(PageTranslationState())

internal suspend fun <T> GeckoResult<T>.awaitResult(): Result<T?> = suspendCancellableCoroutine { cont ->
    accept(
        { value -> if (cont.isActive) cont.resume(Result.success(value)) },
        { error -> if (cont.isActive) cont.resume(Result.failure(error ?: IllegalStateException())) }
    )
}

class PageTranslator(private val session: GeckoSession, private val onOffer: () -> Unit) {

    private val mutableState = MutableStateFlow(PageTranslationState())
    val state: StateFlow<PageTranslationState> = mutableState.asStateFlow()

    val delegate = object : SessionTranslation.Delegate {
        override fun onOfferTranslate(session: GeckoSession) {
            onOffer()
        }

        override fun onTranslationStateChange(session: GeckoSession, translationState: SessionTranslation.TranslationState?) {
            if (translationState == null) {
                mutableState.value = PageTranslationState()
                return
            }
            val detected = translationState.detectedLanguages
            val pair = translationState.requestedTranslationPair
            mutableState.value = PageTranslationState(
                docLanguage = detected?.docLangTag,
                userLanguage = detected?.userLangTag,
                isDocLanguageSupported = detected?.isDocLangTagSupported == true,
                requestedFrom = pair?.fromLanguage,
                requestedTo = pair?.toLanguage,
                isEngineReady = translationState.isEngineReady == true,
                error = translationState.error
            )
        }
    }

    fun reset() {
        mutableState.value = PageTranslationState()
    }

    suspend fun translate(from: String, to: String): Boolean {
        val translation = session.sessionTranslation ?: return false
        val options = SessionTranslation.TranslationOptions.Builder().downloadModel(true).build()
        return runCatching { translation.translate(from, to, options) }.getOrNull()?.awaitResult()?.isSuccess == true
    }

    suspend fun restoreOriginal(): Boolean {
        val translation = session.sessionTranslation ?: return false
        return runCatching { translation.restoreOriginalPage() }.getOrNull()?.awaitResult()?.isSuccess == true
    }

    suspend fun isNeverTranslateSite(): Boolean {
        val translation = session.sessionTranslation ?: return false
        return runCatching { translation.getNeverTranslateSiteSetting() }.getOrNull()?.awaitResult()?.getOrNull() == true
    }

    suspend fun setNeverTranslateSite(never: Boolean): Boolean {
        val translation = session.sessionTranslation ?: return false
        return runCatching { translation.setNeverTranslateSiteSetting(never) }.getOrNull()?.awaitResult()?.isSuccess == true
    }
}

object ClintTranslations {

    @Volatile var offerHandler: ((ClintWebView) -> Unit)? = null

    fun applyPrefs(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        runCatching {
            GeckoPreferenceController.setGeckoPref("browser.translations.enable", true, GeckoPreferenceController.PREF_BRANCH_USER)
        }
        runCatching {
            GeckoPreferenceController.setGeckoPref(
                "browser.translations.automaticallyPopup",
                prefs.getBoolean(PREF_TRANSLATION_OFFER, true),
                GeckoPreferenceController.PREF_BRANCH_USER
            )
        }
    }

    fun sameLanguage(first: String?, second: String?): Boolean {
        if (first.isNullOrEmpty() || second.isNullOrEmpty()) return false
        return Locale.forLanguageTag(first).language.equals(Locale.forLanguageTag(second).language, ignoreCase = true)
    }

    fun displayName(code: String, locale: Locale, fallback: String? = null, capitalize: Boolean = true): String {
        val name = runCatching { Locale.forLanguageTag(code).getDisplayName(locale) }.getOrNull()
        val resolved = if (name.isNullOrBlank() || name.equals(code, ignoreCase = true)) fallback?.takeIf { it.isNotBlank() } ?: code else name
        if (!capitalize) return resolved
        return resolved.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    fun appLocale(context: Context): Locale {
        val locales = context.resources.configuration.locales
        return if (locales.size() > 0) locales.get(0) else Locale.getDefault()
    }

    fun findCompatible(code: String?, languages: List<TranslationLanguage>): TranslationLanguage? {
        if (code.isNullOrEmpty()) return null
        return languages.firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?: languages.firstOrNull { sameLanguage(it.code, code) }
    }

    fun savedTarget(context: Context): String? =
        PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_TRANSLATION_TARGET, null)?.takeIf { it.isNotBlank() }

    fun saveTarget(context: Context, code: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PREF_TRANSLATION_TARGET, code).apply()
    }

    fun defaultTarget(context: Context, state: PageTranslationState): String? {
        val saved = savedTarget(context)
        if (saved != null && !sameLanguage(saved, state.docLanguage)) return saved
        return state.userLanguage?.takeIf { !sameLanguage(it, state.docLanguage) }
    }

    suspend fun isEngineSupported(): Boolean =
        runCatching { RuntimeTranslation.isTranslationsEngineSupported() }.getOrNull()?.awaitResult()?.getOrNull() == true

    suspend fun supportedLanguages(locale: Locale): TranslationLanguageSupport? {
        val support = runCatching { RuntimeTranslation.listSupportedLanguages() }.getOrNull()?.awaitResult()?.getOrNull() ?: return null
        fun map(list: List<org.mozilla.geckoview.TranslationsController.Language>?): List<TranslationLanguage> =
            list.orEmpty()
                .map { TranslationLanguage(it.code, displayName(it.code, locale, it.localizedDisplayName)) }
                .distinctBy { it.code }
                .sortedBy { it.name.lowercase(locale) }
        return TranslationLanguageSupport(map(support.fromLanguages), map(support.toLanguages))
    }

    suspend fun models(locale: Locale): List<TranslationModel>? {
        val models = runCatching { RuntimeTranslation.listModelDownloadStates() }.getOrNull()?.awaitResult()?.getOrNull() ?: return null
        return models
            .mapNotNull { model ->
                val language = model.language ?: return@mapNotNull null
                TranslationModel(
                    language = TranslationLanguage(language.code, displayName(language.code, locale, language.localizedDisplayName)),
                    isDownloaded = model.isDownloaded == true,
                    size = model.size
                )
            }
            .sortedBy { it.language.name.lowercase(locale) }
    }

    private suspend fun manage(operation: String, level: String, code: String?): Boolean {
        val builder = RuntimeTranslation.ModelManagementOptions.Builder()
            .operation(operation)
            .operationLevel(level)
        if (code != null) builder.languageToManage(code)
        return runCatching { RuntimeTranslation.manageLanguageModel(builder.build()) }.getOrNull()?.awaitResult()?.isSuccess == true
    }

    suspend fun downloadLanguage(code: String): Boolean = manage(RuntimeTranslation.DOWNLOAD, RuntimeTranslation.LANGUAGE, code)

    suspend fun deleteLanguage(code: String): Boolean = manage(RuntimeTranslation.DELETE, RuntimeTranslation.LANGUAGE, code)

    suspend fun deleteAllLanguages(): Boolean = manage(RuntimeTranslation.DELETE, RuntimeTranslation.ALL, null)

    suspend fun downloadSize(from: String, to: String): Long? =
        runCatching { RuntimeTranslation.checkPairDownloadSize(from, to) }.getOrNull()?.awaitResult()?.getOrNull()

    suspend fun languageSetting(code: String): TranslationLanguageSetting? {
        val value = runCatching { RuntimeTranslation.getLanguageSetting(code) }.getOrNull()?.awaitResult()?.getOrNull() ?: return null
        return TranslationLanguageSetting.fromGecko(value)
    }

    suspend fun languageSettings(): Map<String, TranslationLanguageSetting>? {
        val settings = runCatching { RuntimeTranslation.getLanguageSettings() }.getOrNull()?.awaitResult()?.getOrNull() ?: return null
        return settings.mapValues { TranslationLanguageSetting.fromGecko(it.value) }
    }

    suspend fun setLanguageSetting(code: String, setting: TranslationLanguageSetting): Boolean =
        runCatching { RuntimeTranslation.setLanguageSettings(code, setting.geckoValue) }.getOrNull()?.awaitResult()?.isSuccess == true

    suspend fun neverTranslateSites(): List<String>? =
        runCatching { RuntimeTranslation.getNeverTranslateSiteList() }.getOrNull()?.awaitResult()?.getOrNull()

    suspend fun removeNeverTranslateSite(origin: String): Boolean =
        runCatching { RuntimeTranslation.setNeverTranslateSpecifiedSite(false, origin) }.getOrNull()?.awaitResult()?.isSuccess == true
}
