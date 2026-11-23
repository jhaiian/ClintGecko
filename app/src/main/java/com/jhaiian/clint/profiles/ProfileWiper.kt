package com.jhaiian.clint.profiles

import android.content.Context
import com.jhaiian.clint.bookmarks.BookmarkManager
import com.jhaiian.clint.downloads.ClintDownloadManager
import com.jhaiian.clint.extensions.ProfileExtensions
import com.jhaiian.clint.history.SearchHistoryManager
import com.jhaiian.clint.settings.sitepermissions.SitePermissionManager
import com.jhaiian.clint.shortcuts.ShortcutStore
import com.jhaiian.clint.tabs.TabSessionManager

object ProfileWiper {

    fun eraseData(context: Context, profileId: String, keepDownloads: Boolean, synchronous: Boolean = false) {
        if (profileId == WebProfiles.DEFAULT_ID) return
        val app = context.applicationContext
        WebProfiles.clearData(profileId)
        if (synchronous) TabSessionManager.removeProfile(app, profileId)
        val work = {
            if (!synchronous) TabSessionManager.removeProfile(app, profileId)
            ShortcutStore.clearProfile(app, profileId)
            BookmarkManager.removeProfile(app, profileId)
            SearchHistoryManager.removeProfile(app, profileId)
            SitePermissionManager.removeProfile(app, profileId)
            ProfileExtensions.forget(app, profileId)
            if (keepDownloads) ClintDownloadManager.reassignProfile(app, profileId)
            else ClintDownloadManager.removeProfileDownloads(app, profileId)
        }
        if (synchronous) work() else Thread { work() }.start()
    }

    fun purgeTemporary(context: Context, synchronous: Boolean) {
        ProfileRepository.all(context).filter { it.temporary }.forEach {
            eraseData(context, it.id, keepDownloads = false, synchronous = synchronous)
        }
    }
}
