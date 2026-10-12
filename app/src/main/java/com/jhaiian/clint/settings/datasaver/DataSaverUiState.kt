package com.jhaiian.clint.settings.datasaver

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class DataSaverUiState(
    initialEnabled: Boolean,
    initialMeteredOnly: Boolean,
    initialDisableImages: Boolean,
    initialDisableAutoplay: Boolean,
    initialSendHeader: Boolean,
    initialBlockFonts: Boolean,
    initialBlockFrames: Boolean,
    initialBlockPreload: Boolean,
    initialCacheFirst: Boolean,
    initialBlockVideo: Boolean,
    initialDisableSuggestions: Boolean,
    initialDisableFavicons: Boolean,
    initialBlockScripts: Boolean,
    initialBlockCss: Boolean
) {
    var enabled by mutableStateOf(initialEnabled)
    var meteredOnly by mutableStateOf(initialMeteredOnly)
    var disableImages by mutableStateOf(initialDisableImages)
    var disableAutoplay by mutableStateOf(initialDisableAutoplay)
    var sendHeader by mutableStateOf(initialSendHeader)
    var blockFonts by mutableStateOf(initialBlockFonts)
    var blockFrames by mutableStateOf(initialBlockFrames)
    var blockPreload by mutableStateOf(initialBlockPreload)
    var cacheFirst by mutableStateOf(initialCacheFirst)
    var blockVideo by mutableStateOf(initialBlockVideo)
    var disableSuggestions by mutableStateOf(initialDisableSuggestions)
    var disableFavicons by mutableStateOf(initialDisableFavicons)
    var blockScripts by mutableStateOf(initialBlockScripts)
    var blockCss by mutableStateOf(initialBlockCss)
}
