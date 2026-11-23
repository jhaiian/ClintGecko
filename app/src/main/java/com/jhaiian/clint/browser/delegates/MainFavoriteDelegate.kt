package com.jhaiian.clint.browser.delegates

import android.graphics.Bitmap
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.home.HomeEditRequest
import com.jhaiian.clint.browser.home.HomeSiteTile
import com.jhaiian.clint.browser.home.HomeTileStore
import com.jhaiian.clint.browser.home.isClintHomeUrl
import com.jhaiian.clint.browser.home.newHomeTileId
import com.jhaiian.clint.browser.home.readHomepageLayout
import com.jhaiian.clint.browser.webview.ClintGecko
import com.jhaiian.clint.profiles.WebProfiles
import com.jhaiian.clint.ui.showClintSnackbar

internal fun MainActivity.openCreateFavorite() {
    val tab = tabManager.activeTab ?: return
    val url = tab.webView.url
    if (url.isNullOrEmpty() || isClintHomeUrl(url)) return
    val title = tab.title.trim().takeIf { it.isNotEmpty() && it != url }.orEmpty()
    uiState.favoriteRequest = HomeEditRequest(null, url, title)
}

internal fun MainActivity.saveFavorite(url: String, title: String, icon: Bitmap?) {
    val profileId = uiState.activeTabId?.let { WebProfiles.profileOfTab(it) } ?: WebProfiles.DEFAULT_ID
    val tileProfile = if (readHomepageLayout(this, prefs, profileId).separateTiles) profileId else null
    val appContext = applicationContext
    val site = HomeSiteTile(newHomeTileId(), url, title, icon != null)
    ClintGecko.io.execute {
        val tiles = HomeTileStore.load(appContext, tileProfile)
        HomeTileStore.saveAsync(appContext, tiles + site, icon?.let { site.id to it }, tileProfile) {
            if (!isFinishing && !isDestroyed) showClintSnackbar(message = getString(R.string.home_favorite_added))
        }
    }
}
