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

private fun usesIncognitoSuggestionsApi(prefs: SharedPreferences, incognito: Boolean): Boolean =
    incognito && incognitoSearchSuggestionsApiSetting(prefs) != SameAsNormalMode

fun searchSuggestionsApiFor(prefs: SharedPreferences, incognito: Boolean = false): String =
    if (usesIncognitoSuggestionsApi(prefs, incognito)) incognitoSearchSuggestionsApiSetting(prefs)
    else prefs.getString(SearchSuggestionsApiKey, DefaultSearchSuggestionsApi) ?: DefaultSearchSuggestionsApi

fun customSearchSuggestionsApiName(prefs: SharedPreferences, incognito: Boolean = false): String =
    prefs.getString(if (usesIncognitoSuggestionsApi(prefs, incognito)) IncognitoCustomSearchSuggestionsApiNameKey else CustomSearchSuggestionsApiNameKey, "") ?: ""

fun customSearchSuggestionsApiUrlTemplate(prefs: SharedPreferences, incognito: Boolean = false): String =
    prefs.getString(if (usesIncognitoSuggestionsApi(prefs, incognito)) IncognitoCustomSearchSuggestionsApiUrlKey else CustomSearchSuggestionsApiUrlKey, "") ?: ""

fun incognitoCustomSearchSuggestionsApiName(prefs: SharedPreferences): String =
    prefs.getString(IncognitoCustomSearchSuggestionsApiNameKey, "") ?: ""

fun incognitoCustomSearchSuggestionsApiUrlTemplate(prefs: SharedPreferences): String =
    prefs.getString(IncognitoCustomSearchSuggestionsApiUrlKey, "") ?: ""

fun isValidCustomSearchSuggestionsApiUrl(url: String): Boolean {
    if (!url.contains(CustomSearchSuggestionsApiQueryPlaceholder)) return false
    val uri = Uri.parse(url)
    val scheme = uri.scheme?.lowercase()
    return (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
}

/** Builds the request URL for the user's custom suggestions API, or null if none is configured/valid. */
fun customSearchSuggestionsApiQueryUrl(prefs: SharedPreferences, encodedQuery: String, incognito: Boolean = false): String? {
    val template = customSearchSuggestionsApiUrlTemplate(prefs, incognito)
    if (!isValidCustomSearchSuggestionsApiUrl(template)) return null
    return template.replace(CustomSearchSuggestionsApiQueryPlaceholder, encodedQuery)
}
