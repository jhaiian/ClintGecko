package com.jhaiian.clint.blocker.additional

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.listscreen.ConfirmDialogHost
import com.jhaiian.clint.ui.rememberMaxContentWidth
import com.jhaiian.clint.ui.theme.ClintComposeTheme

class AdditionalWebsitesActivity : ClintActivity() {

    private lateinit var db: AdditionalWebsitesDatabase
    private lateinit var uiState: AdditionalWebsitesUiState
    private var deleteConfirm by mutableStateOf<ConfirmDialogConfig?>(null)

    private fun reload() {
        uiState.rules = db.getAll()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        onBackPressedDispatcher.addCallback(this) {
            when {
                uiState.isSearchMode -> { uiState.isSearchMode = false; uiState.searchQuery = "" }
                uiState.isInSelectionMode -> uiState.exitSelectionMode()
                else -> { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
            }
        }

        db = AdditionalWebsitesDatabase(this)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("app_theme", "system") ?: "system"
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)

        uiState = AdditionalWebsitesUiState()
        uiState.isEnabled = AdditionalWebsitesState.isEnabled(this)
        reload()

        setContent {
            ClintComposeTheme(theme = theme) {
                val maxContentWidth = rememberMaxContentWidth(this)
                Box {
                    AdditionalWebsitesScreen(
                        state = uiState,
                        maxContentWidth = maxContentWidth,
                        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                        onExit = { finish() },
                        onToggleEnabled = { enabled ->
                            uiState.isEnabled = enabled
                            AdditionalWebsitesState.setEnabled(this@AdditionalWebsitesActivity, enabled)
                        },
                        onAddHosts = { hosts ->
                            db.addAll(hosts)
                            reload()
                        },
                        onDeleteSelected = { showDeleteSelectedConfirm() }
                    )

                    ConfirmDialogHost(deleteConfirm, hideStatusBar, hideSystemNavigation) { deleteConfirm = null }
                }
            }
        }
    }

    private fun showDeleteSelectedConfirm() {
        val ids = uiState.selectedIds
        if (ids.isEmpty()) return
        deleteConfirm = ConfirmDialogConfig(
            title = getString(R.string.additional_websites_delete_confirm_title),
            message = getString(R.string.additional_websites_delete_confirm_message, ids.size),
            negativeLabel = getString(R.string.action_cancel),
            positiveLabel = getString(R.string.history_delete_selected),
            onPositive = {
                db.removeAll(ids)
                uiState.exitSelectionMode()
                reload()
            }
        )
    }
}
