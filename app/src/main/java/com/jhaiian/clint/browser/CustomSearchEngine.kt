package com.jhaiian.clint.browser

import android.content.SharedPreferences
import android.net.Uri

const val CustomSearchEngineNameKey = "custom_search_engine_name"
const val CustomSearchEngineUrlKey = "custom_search_engine_url"
const val CustomSearchEngineQueryPlaceholder = "{query}"

fun customSearchEngineName(prefs: SharedPreferences): String =
    prefs.getString(CustomSearchEngineNameKey, "") ?: ""

fun customSearchEngineUrlTemplate(prefs: SharedPreferences): String =
    prefs.getString(CustomSearchEngineUrlKey, "") ?: ""

fun isValidCustomSearchEngineUrl(url: String): Boolean {
    if (!url.contains(CustomSearchEngineQueryPlaceholder)) return false
    val uri = Uri.parse(url)
    val scheme = uri.scheme?.lowercase()
    return (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
}

fun customSearchEngineQueryUrl(prefs: SharedPreferences, encodedQuery: String): String {
    val template = customSearchEngineUrlTemplate(prefs)
    if (!isValidCustomSearchEngineUrl(template)) return "https://duckduckgo.com/?q=$encodedQuery"
    return template.replace(CustomSearchEngineQueryPlaceholder, encodedQuery)
}

fun customSearchEngineHost(prefs: SharedPreferences): String? {
    val template = customSearchEngineUrlTemplate(prefs)
    if (!isValidCustomSearchEngineUrl(template)) return null
    return Uri.parse(template).host?.lowercase()
}

fun customSearchEngineHomeUrl(prefs: SharedPreferences): String {
    val template = customSearchEngineUrlTemplate(prefs)
    if (!isValidCustomSearchEngineUrl(template)) return "https://duckduckgo.com"
    val uri = Uri.parse(template)
    return "${uri.scheme}://${uri.host}"
}
