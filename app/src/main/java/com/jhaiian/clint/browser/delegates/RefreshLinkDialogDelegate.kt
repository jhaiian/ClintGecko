package com.jhaiian.clint.browser.delegates

import android.widget.Toast
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.dialogs.RefreshLinkDialogRequest
import com.jhaiian.clint.downloads.ClintDownloadManager

internal fun MainActivity.showRefreshLinkDownloadDialog(
    url: String,
    filename: String,
    userAgent: String,
    referer: String,
    cookies: String,
    session: MainActivity.RefreshLinkSession
) {
    uiState.refreshLinkDialogRequest = RefreshLinkDialogRequest(
        existingFilename = session.filename,
        onUpdateExisting = {
            ClintDownloadManager.updateDownloadUrl(session.downloadId, url)
            Toast.makeText(this, getString(R.string.refresh_link_updated_toast, session.filename), Toast.LENGTH_SHORT).show()
            refreshLinkSession = null
        },
        onAddNew = { showDownloadDialog(url, filename, userAgent, referer, cookies) }
    )
}
