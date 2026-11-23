package com.jhaiian.clint.settings.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class BrowserSettingsUiState(
    initialDeleteInactiveTabs: String,
    initialSearchEngine: String,
    initialSearchSuggestionsApi: String,
    initialIncognitoSearchEngine: String,
    initialIncognitoSearchSuggestionsApi: String,
    initialJavascriptEnabled: Boolean,
    initialHideStatusBar: Boolean,
    initialHideSystemNavigation: Boolean,
    initialIncognitoSearchHistory: Boolean,
    initialRememberBrowsingMode: Boolean,
    initialBackgroundPlayback: Boolean,
    initialAboutConfigEnabled: Boolean
) {
    var deleteInactiveTabs by mutableStateOf(initialDeleteInactiveTabs)
    var searchEngine by mutableStateOf(initialSearchEngine)
    var searchSuggestionsApi by mutableStateOf(initialSearchSuggestionsApi)
    var incognitoSearchEngine by mutableStateOf(initialIncognitoSearchEngine)
    var incognitoSearchSuggestionsApi by mutableStateOf(initialIncognitoSearchSuggestionsApi)
    var javascriptEnabled by mutableStateOf(initialJavascriptEnabled)
    var hideStatusBar by mutableStateOf(initialHideStatusBar)
    var hideSystemNavigation by mutableStateOf(initialHideSystemNavigation)
    var incognitoSearchHistory by mutableStateOf(initialIncognitoSearchHistory)
    var rememberBrowsingMode by mutableStateOf(initialRememberBrowsingMode)
    var backgroundPlayback by mutableStateOf(initialBackgroundPlayback)
    var aboutConfigEnabled by mutableStateOf(initialAboutConfigEnabled)
    var deleteInactiveTabsDialogOpen by mutableStateOf(false)
    var searchEngineDialogOpen by mutableStateOf(false)
    var searchSuggestionsApiDialogOpen by mutableStateOf(false)
    var incognitoSearchEngineDialogOpen by mutableStateOf(false)
    var incognitoSearchSuggestionsApiDialogOpen by mutableStateOf(false)
}
