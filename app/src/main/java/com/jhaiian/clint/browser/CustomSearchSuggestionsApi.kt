package com.jhaiian.clint.browser

import android.content.SharedPreferences
import android.net.Uri

const val SearchSuggestionsApiKey = "search_suggestions_api"
const val DefaultSearchSuggestionsApi = "duckduckgo"
const val CustomSearchSuggestionsApiNameKey = "custom_search_suggestions_api_name"
const val CustomSearchSuggestionsApiUrlKey = "custom_search_suggestions_api_url"
const val IncognitoSearchSuggestionsApiKey = "incognito_search_suggestions_api"
const val IncognitoCustomSearchSuggestionsApiNameKey = "incognito_custom_search_suggestions_api_name"
const val IncognitoCustomSearchSuggestionsApiUrlKey = "incognito_custom_search_suggestions_api_url"
const val CustomSearchSuggestionsApiQueryPlaceholder = "{query}"

fun incognitoSearchSuggestionsApiSetting(prefs: SharedPreferences): String =
    prefs.getString(IncognitoSearchSuggestionsApiKey, SameAsNormalMode) ?: SameAsNormalMode

private fun usableSuggestionsApi(prefs: SharedPreferences, api: String): Boolean =
    !isCustomEngineKey(api) || customSearchEngineFor(prefs, api)?.hasSuggestions == true

fun searchSuggestionsApiFor(prefs: SharedPreferences, incognito: Boolean = false): String {
    migrateLegacyCustomEngines(prefs)
    val incognitoApi = incognitoSearchSuggestionsApiSetting(prefs)
    if (incognito && incognitoApi != SameAsNormalMode && usableSuggestionsApi(prefs, incognitoApi)) return incognitoApi
    val api = prefs.getString(SearchSuggestionsApiKey, DefaultSearchSuggestionsApi) ?: DefaultSearchSuggestionsApi
    return if (usableSuggestionsApi(prefs, api)) api else DefaultSearchSuggestionsApi
}

fun isValidCustomSearchSuggestionsApiUrl(url: String): Boolean {
    if (!url.contains(CustomSearchSuggestionsApiQueryPlaceholder)) return false
    val uri = Uri.parse(url)
    val scheme = uri.scheme?.lowercase()
    return (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
}

fun suggestionsQueryUrlFor(prefs: SharedPreferences, api: String, encodedQuery: String): String? =
    customSearchEngineFor(prefs, api)?.takeIf { it.hasSuggestions }
        ?.suggestUrl?.replace(CustomSearchSuggestionsApiQueryPlaceholder, encodedQuery)
