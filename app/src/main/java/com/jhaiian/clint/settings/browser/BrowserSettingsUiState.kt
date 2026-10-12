package com.jhaiian.clint.settings.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class BrowserSettingsUiState(
    initialHomepage: String,
    initialHomepageDesign: String,
    initialHomepageShowFavorites: Boolean,
    initialHomepageShowRecent: Boolean,
    initialHomepageCenterContent: Boolean,
    initialDeleteInactiveTabs: String,
    initialSearchEngine: String,
    initialCustomSearchEngineName: String,
    initialCustomSearchEngineUrl: String,
    initialSearchSuggestionsApi: String,
    initialCustomSearchSuggestionsApiName: String,
    initialCustomSearchSuggestionsApiUrl: String,
    initialJavascriptEnabled: Boolean,
    initialHideStatusBar: Boolean,
    initialHideSystemNavigation: Boolean,
    initialIncognitoSearchHistory: Boolean,
    initialBackgroundPlayback: Boolean
) {
    var homepage by mutableStateOf(initialHomepage)
    var homepageDesign by mutableStateOf(initialHomepageDesign)
    var homepageShowFavorites by mutableStateOf(initialHomepageShowFavorites)
    var homepageShowRecent by mutableStateOf(initialHomepageShowRecent)
    var homepageCenterContent by mutableStateOf(initialHomepageCenterContent)
    var deleteInactiveTabs by mutableStateOf(initialDeleteInactiveTabs)
    var searchEngine by mutableStateOf(initialSearchEngine)
    var customSearchEngineName by mutableStateOf(initialCustomSearchEngineName)
    var customSearchEngineUrl by mutableStateOf(initialCustomSearchEngineUrl)
    var searchSuggestionsApi by mutableStateOf(initialSearchSuggestionsApi)
    var customSearchSuggestionsApiName by mutableStateOf(initialCustomSearchSuggestionsApiName)
    var customSearchSuggestionsApiUrl by mutableStateOf(initialCustomSearchSuggestionsApiUrl)
    var javascriptEnabled by mutableStateOf(initialJavascriptEnabled)
    var hideStatusBar by mutableStateOf(initialHideStatusBar)
    var hideSystemNavigation by mutableStateOf(initialHideSystemNavigation)
    var incognitoSearchHistory by mutableStateOf(initialIncognitoSearchHistory)
    var backgroundPlayback by mutableStateOf(initialBackgroundPlayback)
    var homepageDialogOpen by mutableStateOf(false)
    var homepageDesignDialogOpen by mutableStateOf(false)
    var deleteInactiveTabsDialogOpen by mutableStateOf(false)
    var searchEngineDialogOpen by mutableStateOf(false)
    var searchSuggestionsApiDialogOpen by mutableStateOf(false)
}
