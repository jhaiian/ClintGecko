package com.jhaiian.clint.settings.browser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class BrowserSettingsUiState(
    initialHomepage: String,
    initialHomepageCustomUrl: String,
    initialHomepageDesign: String,
    initialHomepageShowFavorites: Boolean,
    initialHomepageShowQuickActions: Boolean,
    initialHomepageShowRecent: Boolean,
    initialHomepageCenterContent: Boolean,
    initialHomepageImageFullBrightness: Boolean,
    initialDeleteInactiveTabs: String,
    initialSearchEngine: String,
    initialCustomSearchEngineName: String,
    initialCustomSearchEngineUrl: String,
    initialSearchSuggestionsApi: String,
    initialCustomSearchSuggestionsApiName: String,
    initialCustomSearchSuggestionsApiUrl: String,
    initialIncognitoSearchEngine: String,
    initialIncognitoCustomSearchEngineName: String,
    initialIncognitoCustomSearchEngineUrl: String,
    initialIncognitoSearchSuggestionsApi: String,
    initialIncognitoCustomSearchSuggestionsApiName: String,
    initialIncognitoCustomSearchSuggestionsApiUrl: String,
    initialSuggestionsShowHistory: Boolean,
    initialSuggestionsShowBookmarks: Boolean,
    initialJavascriptEnabled: Boolean,
    initialHideStatusBar: Boolean,
    initialHideSystemNavigation: Boolean,
    initialIncognitoSearchHistory: Boolean,
    initialRememberBrowsingMode: Boolean,
    initialBackgroundPlayback: Boolean,
    initialAboutConfigEnabled: Boolean
) {
    var homepage by mutableStateOf(initialHomepage)
    var homepageCustomUrl by mutableStateOf(initialHomepageCustomUrl)
    var homepageDesign by mutableStateOf(initialHomepageDesign)
    var homepageShowFavorites by mutableStateOf(initialHomepageShowFavorites)
    var homepageShowQuickActions by mutableStateOf(initialHomepageShowQuickActions)
    var homepageShowRecent by mutableStateOf(initialHomepageShowRecent)
    var homepageCenterContent by mutableStateOf(initialHomepageCenterContent)
    var homepageImageFullBrightness by mutableStateOf(initialHomepageImageFullBrightness)
    var deleteInactiveTabs by mutableStateOf(initialDeleteInactiveTabs)
    var searchEngine by mutableStateOf(initialSearchEngine)
    var customSearchEngineName by mutableStateOf(initialCustomSearchEngineName)
    var customSearchEngineUrl by mutableStateOf(initialCustomSearchEngineUrl)
    var searchSuggestionsApi by mutableStateOf(initialSearchSuggestionsApi)
    var customSearchSuggestionsApiName by mutableStateOf(initialCustomSearchSuggestionsApiName)
    var customSearchSuggestionsApiUrl by mutableStateOf(initialCustomSearchSuggestionsApiUrl)
    var incognitoSearchEngine by mutableStateOf(initialIncognitoSearchEngine)
    var incognitoCustomSearchEngineName by mutableStateOf(initialIncognitoCustomSearchEngineName)
    var incognitoCustomSearchEngineUrl by mutableStateOf(initialIncognitoCustomSearchEngineUrl)
    var incognitoSearchSuggestionsApi by mutableStateOf(initialIncognitoSearchSuggestionsApi)
    var incognitoCustomSearchSuggestionsApiName by mutableStateOf(initialIncognitoCustomSearchSuggestionsApiName)
    var incognitoCustomSearchSuggestionsApiUrl by mutableStateOf(initialIncognitoCustomSearchSuggestionsApiUrl)
    var suggestionsShowHistory by mutableStateOf(initialSuggestionsShowHistory)
    var suggestionsShowBookmarks by mutableStateOf(initialSuggestionsShowBookmarks)
    var javascriptEnabled by mutableStateOf(initialJavascriptEnabled)
    var hideStatusBar by mutableStateOf(initialHideStatusBar)
    var hideSystemNavigation by mutableStateOf(initialHideSystemNavigation)
    var incognitoSearchHistory by mutableStateOf(initialIncognitoSearchHistory)
    var rememberBrowsingMode by mutableStateOf(initialRememberBrowsingMode)
    var backgroundPlayback by mutableStateOf(initialBackgroundPlayback)
    var aboutConfigEnabled by mutableStateOf(initialAboutConfigEnabled)
    var homepageDialogOpen by mutableStateOf(false)
    var homepageDesignDialogOpen by mutableStateOf(false)
    var deleteInactiveTabsDialogOpen by mutableStateOf(false)
    var searchEngineDialogOpen by mutableStateOf(false)
    var searchSuggestionsApiDialogOpen by mutableStateOf(false)
    var incognitoSearchEngineDialogOpen by mutableStateOf(false)
    var incognitoSearchSuggestionsApiDialogOpen by mutableStateOf(false)
}
