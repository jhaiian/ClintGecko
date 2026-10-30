package com.jhaiian.clint.browser

import android.content.SharedPreferences
import android.net.Uri

const val SearchEngineKey = "search_engine"
const val DefaultSearchEngine = "duckduckgo"
const val CustomSearchEngineNameKey = "custom_search_engine_name"
const val CustomSearchEngineUrlKey = "custom_search_engine_url"
const val IncognitoSearchEngineKey = "incognito_search_engine"
const val IncognitoCustomSearchEngineNameKey = "incognito_custom_search_engine_name"
const val IncognitoCustomSearchEngineUrlKey = "incognito_custom_search_engine_url"
const val SameAsNormalMode = "same"
const val CustomSearchEngineQueryPlaceholder = "{query}"

fun incognitoSearchEngineSetting(prefs: SharedPreferences): String =
    prefs.getString(IncognitoSearchEngineKey, SameAsNormalMode) ?: SameAsNormalMode

private fun usesIncognitoSearchEngine(prefs: SharedPreferences, incognito: Boolean): Boolean =
    incognito && incognitoSearchEngineSetting(prefs) != SameAsNormalMode

fun searchEngineFor(prefs: SharedPreferences, incognito: Boolean = false): String =
    if (usesIncognitoSearchEngine(prefs, incognito)) incognitoSearchEngineSetting(prefs)
    else prefs.getString(SearchEngineKey, DefaultSearchEngine) ?: DefaultSearchEngine

fun customSearchEngineName(prefs: SharedPreferences, incognito: Boolean = false): String =
    prefs.getString(if (usesIncognitoSearchEngine(prefs, incognito)) IncognitoCustomSearchEngineNameKey else CustomSearchEngineNameKey, "") ?: ""

fun customSearchEngineUrlTemplate(prefs: SharedPreferences, incognito: Boolean = false): String =
    prefs.getString(if (usesIncognitoSearchEngine(prefs, incognito)) IncognitoCustomSearchEngineUrlKey else CustomSearchEngineUrlKey, "") ?: ""

fun incognitoCustomSearchEngineName(prefs: SharedPreferences): String =
    prefs.getString(IncognitoCustomSearchEngineNameKey, "") ?: ""

fun incognitoCustomSearchEngineUrlTemplate(prefs: SharedPreferences): String =
    prefs.getString(IncognitoCustomSearchEngineUrlKey, "") ?: ""

fun isValidCustomSearchEngineUrl(url: String): Boolean {
    if (!url.contains(CustomSearchEngineQueryPlaceholder)) return false
    val uri = Uri.parse(url)
    val scheme = uri.scheme?.lowercase()
    return (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
}

fun customSearchEngineQueryUrl(prefs: SharedPreferences, encodedQuery: String, incognito: Boolean = false): String {
    val template = customSearchEngineUrlTemplate(prefs, incognito)
    if (!isValidCustomSearchEngineUrl(template)) return "https://duckduckgo.com/?q=$encodedQuery"
    return template.replace(CustomSearchEngineQueryPlaceholder, encodedQuery)
}

fun customSearchEngineHost(prefs: SharedPreferences, incognito: Boolean = false): String? {
    val template = customSearchEngineUrlTemplate(prefs, incognito)
    if (!isValidCustomSearchEngineUrl(template)) return null
    return Uri.parse(template).host?.lowercase()
}

fun customSearchEngineHomeUrl(prefs: SharedPreferences, incognito: Boolean = false): String {
    val template = customSearchEngineUrlTemplate(prefs, incognito)
    if (!isValidCustomSearchEngineUrl(template)) return "https://duckduckgo.com"
    val uri = Uri.parse(template)
    return "${uri.scheme}://${uri.host}"
}
