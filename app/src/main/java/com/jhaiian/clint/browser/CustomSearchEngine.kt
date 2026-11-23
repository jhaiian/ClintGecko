package com.jhaiian.clint.browser

import android.content.SharedPreferences
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

const val SearchEngineKey = "search_engine"
const val DefaultSearchEngine = "duckduckgo"
const val CustomSearchEngineNameKey = "custom_search_engine_name"
const val CustomSearchEngineUrlKey = "custom_search_engine_url"
const val IncognitoSearchEngineKey = "incognito_search_engine"
const val IncognitoCustomSearchEngineNameKey = "incognito_custom_search_engine_name"
const val IncognitoCustomSearchEngineUrlKey = "incognito_custom_search_engine_url"
const val SameAsNormalMode = "same"
const val CustomSearchEngineQueryPlaceholder = "{query}"
const val CustomSearchEnginesKey = "custom_search_engines"
const val CustomEnginePrefix = "custom:"

data class CustomSearchEngine(
    val id: String,
    val name: String,
    val searchUrl: String,
    val suggestUrl: String,
    val description: String = ""
) {
    val key: String get() = CustomEnginePrefix + id
    val hasSearch: Boolean get() = isValidCustomSearchEngineUrl(searchUrl)
    val hasSuggestions: Boolean get() = isValidCustomSearchSuggestionsApiUrl(suggestUrl)
}

fun isCustomEngineKey(key: String): Boolean = key.startsWith(CustomEnginePrefix)

fun newCustomSearchEngineId(): String = UUID.randomUUID().toString().replace("-", "").take(12)

private val customEngineLock = Any()

private fun parseCustomSearchEngines(json: String?): List<CustomSearchEngine> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { i ->
            val obj = array.optJSONObject(i) ?: return@mapNotNull null
            val id = obj.optString("id")
            if (id.isBlank()) null else CustomSearchEngine(
                id = id,
                name = obj.optString("name"),
                searchUrl = obj.optString("search"),
                suggestUrl = obj.optString("suggest"),
                description = obj.optString("desc")
            )
        }
    }.getOrDefault(emptyList())
}

private fun serializeCustomSearchEngines(engines: List<CustomSearchEngine>): String =
    JSONArray(engines.map {
        JSONObject().put("id", it.id).put("name", it.name).put("search", it.searchUrl).put("suggest", it.suggestUrl).put("desc", it.description)
    }).toString()

private val legacyCustomKeys = listOf(
    CustomSearchEngineNameKey, CustomSearchEngineUrlKey,
    IncognitoCustomSearchEngineNameKey, IncognitoCustomSearchEngineUrlKey,
    CustomSearchSuggestionsApiNameKey, CustomSearchSuggestionsApiUrlKey,
    IncognitoCustomSearchSuggestionsApiNameKey, IncognitoCustomSearchSuggestionsApiUrlKey
)

fun migrateLegacyCustomEngines(prefs: SharedPreferences) {
    if (legacyCustomKeys.none { prefs.contains(it) }) return
    synchronized(customEngineLock) {
        if (legacyCustomKeys.none { prefs.contains(it) }) return
        val engines = parseCustomSearchEngines(prefs.getString(CustomSearchEnginesKey, null)).toMutableList()
        val editor = prefs.edit()
        fun import(nameKey: String, urlKey: String, suggestions: Boolean, selectionKey: String) {
            val name = prefs.getString(nameKey, "").orEmpty().trim()
            val url = prefs.getString(urlKey, "").orEmpty().trim()
            val valid = if (suggestions) isValidCustomSearchSuggestionsApiUrl(url) else isValidCustomSearchEngineUrl(url)
            val selectedLegacy = prefs.getString(selectionKey, null) == "custom"
            if (valid) {
                val label = name.ifBlank { Uri.parse(url).host.orEmpty() }
                val index = engines.indexOfFirst { (if (suggestions) it.suggestUrl else it.searchUrl) == url }
                    .takeIf { it >= 0 }
                    ?: engines.indexOfFirst { it.name.equals(label, ignoreCase = true) && (if (suggestions) it.suggestUrl else it.searchUrl).isBlank() }
                val entry = if (index >= 0) {
                    val current = engines[index]
                    (if (suggestions) current.copy(suggestUrl = url) else current.copy(searchUrl = url)).also { engines[index] = it }
                } else {
                    CustomSearchEngine(newCustomSearchEngineId(), label, if (suggestions) "" else url, if (suggestions) url else "").also { engines.add(it) }
                }
                if (selectedLegacy) editor.putString(selectionKey, entry.key)
            } else if (selectedLegacy) {
                editor.remove(selectionKey)
            }
            editor.remove(nameKey).remove(urlKey)
        }
        import(CustomSearchEngineNameKey, CustomSearchEngineUrlKey, false, SearchEngineKey)
        import(IncognitoCustomSearchEngineNameKey, IncognitoCustomSearchEngineUrlKey, false, IncognitoSearchEngineKey)
        import(CustomSearchSuggestionsApiNameKey, CustomSearchSuggestionsApiUrlKey, true, SearchSuggestionsApiKey)
        import(IncognitoCustomSearchSuggestionsApiNameKey, IncognitoCustomSearchSuggestionsApiUrlKey, true, IncognitoSearchSuggestionsApiKey)
        editor.putString(CustomSearchEnginesKey, serializeCustomSearchEngines(engines)).commit()
    }
}

fun customSearchEngines(prefs: SharedPreferences): List<CustomSearchEngine> {
    migrateLegacyCustomEngines(prefs)
    return parseCustomSearchEngines(prefs.getString(CustomSearchEnginesKey, null))
}

fun customSearchEngineFor(prefs: SharedPreferences, key: String): CustomSearchEngine? =
    if (!isCustomEngineKey(key)) null else customSearchEngines(prefs).firstOrNull { it.key == key }

fun saveCustomSearchEngine(prefs: SharedPreferences, engine: CustomSearchEngine) {
    synchronized(customEngineLock) {
        val engines = customSearchEngines(prefs).toMutableList()
        val index = engines.indexOfFirst { it.id == engine.id }
        if (index >= 0) engines[index] = engine else engines.add(engine)
        prefs.edit().putString(CustomSearchEnginesKey, serializeCustomSearchEngines(engines)).apply()
    }
}

fun removeCustomSearchEngineRole(prefs: SharedPreferences, engine: CustomSearchEngine, suggestions: Boolean) {
    synchronized(customEngineLock) {
        val remaining = if (suggestions) engine.copy(suggestUrl = "") else engine.copy(searchUrl = "")
        val engines = customSearchEngines(prefs).mapNotNull {
            when {
                it.id != engine.id -> it
                remaining.searchUrl.isBlank() && remaining.suggestUrl.isBlank() -> null
                else -> remaining
            }
        }
        val editor = prefs.edit().putString(CustomSearchEnginesKey, serializeCustomSearchEngines(engines))
        val selections = if (suggestions) {
            listOf(SearchSuggestionsApiKey to DefaultSearchSuggestionsApi, IncognitoSearchSuggestionsApiKey to SameAsNormalMode)
        } else {
            listOf(SearchEngineKey to DefaultSearchEngine, IncognitoSearchEngineKey to SameAsNormalMode)
        }
        selections.forEach { (key, fallback) -> if (prefs.getString(key, null) == engine.key) editor.putString(key, fallback) }
        editor.apply()
    }
}

fun incognitoSearchEngineSetting(prefs: SharedPreferences): String =
    prefs.getString(IncognitoSearchEngineKey, SameAsNormalMode) ?: SameAsNormalMode

private fun usableSearchEngine(prefs: SharedPreferences, engine: String): Boolean =
    !isCustomEngineKey(engine) || customSearchEngineFor(prefs, engine)?.hasSearch == true

fun searchEngineFor(prefs: SharedPreferences, incognito: Boolean = false): String {
    migrateLegacyCustomEngines(prefs)
    val incognitoEngine = incognitoSearchEngineSetting(prefs)
    if (incognito && incognitoEngine != SameAsNormalMode && usableSearchEngine(prefs, incognitoEngine)) return incognitoEngine
    val engine = prefs.getString(SearchEngineKey, DefaultSearchEngine) ?: DefaultSearchEngine
    return if (usableSearchEngine(prefs, engine)) engine else DefaultSearchEngine
}

fun customSearchEngineName(prefs: SharedPreferences, incognito: Boolean = false): String =
    customSearchEngineFor(prefs, searchEngineFor(prefs, incognito))?.name.orEmpty()

fun isValidCustomSearchEngineUrl(url: String): Boolean {
    if (!url.contains(CustomSearchEngineQueryPlaceholder)) return false
    val uri = Uri.parse(url)
    val scheme = uri.scheme?.lowercase()
    return (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
}

fun searchQueryUrlFor(prefs: SharedPreferences, engine: String, encodedQuery: String): String = when (engine) {
    "brave" -> "https://search.brave.com/search?q=$encodedQuery"
    "ecosia" -> "https://www.ecosia.org/search?q=$encodedQuery"
    "google" -> "https://www.google.com/search?q=$encodedQuery"
    else -> customSearchEngineFor(prefs, engine)?.takeIf { it.hasSearch }
        ?.searchUrl?.replace(CustomSearchEngineQueryPlaceholder, encodedQuery)
        ?: "https://duckduckgo.com/?q=$encodedQuery"
}

fun searchHomeUrlFor(prefs: SharedPreferences, engine: String): String = when (engine) {
    "brave" -> "https://search.brave.com"
    "ecosia" -> "https://www.ecosia.org"
    "google" -> "https://www.google.com"
    else -> customSearchEngineFor(prefs, engine)?.let { custom ->
        val uri = Uri.parse(custom.searchUrl.ifBlank { custom.suggestUrl })
        if (uri.scheme.isNullOrBlank() || uri.host.isNullOrBlank()) null else "${uri.scheme}://${uri.host}"
    } ?: "https://duckduckgo.com"
}

fun customSearchEngineHosts(prefs: SharedPreferences): List<String> =
    customSearchEngines(prefs).filter { it.hasSearch }.mapNotNull { Uri.parse(it.searchUrl).host?.lowercase() }
