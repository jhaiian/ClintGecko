package com.jhaiian.clint.browser.delegates

import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.extensions.ClintExtensions

internal fun MainActivity.setupExtensions() {
    ClintExtensions.tabOpenerOwner = this
    ClintExtensions.tabOpener = { url, active ->
        runCatching {
            val openerId = tabManager.activeTab?.id
            if (active) {
                openNewTab(false, url, openerTabId = openerId, profileId = com.jhaiian.clint.profiles.WebProfiles.DEFAULT_ID)
                tabManager.activeTab?.webView?.session
            } else {
                openNewTabInBackground(url, openerId)
                null
            }
        }.getOrNull()
    }
}

internal fun MainActivity.teardownExtensions() {
    if (ClintExtensions.tabOpenerOwner === this) {
        ClintExtensions.tabOpener = null
        ClintExtensions.tabOpenerOwner = null
    }
}
