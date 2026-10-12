package com.jhaiian.clint.browser

import android.content.SharedPreferences
import android.net.Uri

const val CustomSearchSuggestionsApiNameKey = "custom_search_suggestions_api_name"
const val CustomSearchSuggestionsApiUrlKey = "custom_search_suggestions_api_url"
const val CustomSearchSuggestionsApiQueryPlaceholder = "{query}"

fun customSearchSuggestionsApiName(prefs: SharedPreferences): String =
    prefs.getString(CustomSearchSuggestionsApiNameKey, "") ?: ""

fun customSearchSuggestionsApiUrlTemplate(prefs: SharedPreferences): String =
    prefs.getString(CustomSearchSuggestionsApiUrlKey, "") ?: ""

fun isValidCustomSearchSuggestionsApiUrl(url: String): Boolean {
    if (!url.contains(CustomSearchSuggestionsApiQueryPlaceholder)) return false
    val uri = Uri.parse(url)
    val scheme = uri.scheme?.lowercase()
    return (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
}

/** Builds the request URL for the user's custom suggestions API, or null if none is configured/valid. */
fun customSearchSuggestionsApiQueryUrl(prefs: SharedPreferences, encodedQuery: String): String? {
    val template = customSearchSuggestionsApiUrlTemplate(prefs)
    if (!isValidCustomSearchSuggestionsApiUrl(template)) return null
    return template.replace(CustomSearchSuggestionsApiQueryPlaceholder, encodedQuery)
}
