package com.jhaiian.clint.settings.shortcutmanager

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.shortcuts.ShortcutIconStore
import com.jhaiian.clint.shortcuts.ShortcutPinner
import com.jhaiian.clint.shortcuts.ShortcutStore
import com.jhaiian.clint.shortcuts.ShortcutTabSessionManager
import com.jhaiian.clint.shortcuts.WebAppShortcut
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.listscreen.ConfirmDialogHost
import com.jhaiian.clint.ui.rememberMaxContentWidth
import com.jhaiian.clint.ui.theme.ClintComposeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShortcutManagerActivity : ClintActivity() {

    private val uiState = ShortcutManagerUiState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        onBackPressedDispatcher.addCallback(this) {
            when {
                uiState.isSearchMode -> uiState.closeSearch()
                uiState.isInSelectionMode -> uiState.exitSelectionMode()
                else -> {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("app_theme", "system") ?: "system"
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)

        uiState.shortcuts = ShortcutStore.getAll(this)

        setContent {
            ClintComposeTheme(theme = theme) {
                val maxContentWidth = rememberMaxContentWidth(this@ShortcutManagerActivity)
                Box {
                    ShortcutManagerScreen(
                        state = uiState,
                        maxContentWidth = maxContentWidth,
                        onExit = { finish() },
                        onOpenSettings = { shortcut -> openSettings(shortcut) },
                        onCreateShortcut = { shortcut -> pinShortcut(shortcut) },
                        onDelete = { shortcut -> confirmDelete(shortcut) },
                        onDeleteSelectedClick = { confirmDeleteSelected() }
                    )
                    ConfirmDialogHost(uiState.confirmDialog, hideStatusBar, hideSystemNavigation) { uiState.confirmDialog = null }
                    ConfirmDialogHost(uiState.deleteConfirm, hideStatusBar, hideSystemNavigation) { uiState.deleteConfirm = null }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        uiState.shortcuts = ShortcutStore.getAll(this)
        uiState.iconRevision++
    }

    private fun openSettings(shortcut: WebAppShortcut) {
        startActivity(
            Intent(this, ShortcutSettingsActivity::class.java)
                .putExtra(ShortcutSettingsActivity.EXTRA_SHORTCUT_ID, shortcut.id)
        )
    }

    private fun pinShortcut(shortcut: WebAppShortcut) {
        if (!ShortcutPinner.isSupported(this)) {
            Toast.makeText(this, getString(R.string.create_shortcut_unsupported), Toast.LENGTH_SHORT).show()
            return
        }
        val alreadyPinned = ShortcutPinner.isPinned(this, shortcut.id)
        val requested = runCatching { ShortcutPinner.requestPin(this, shortcut) }.getOrDefault(false)
        if (!requested) {
            Toast.makeText(this, getString(R.string.create_shortcut_unsupported), Toast.LENGTH_SHORT).show()
        } else if (alreadyPinned) {
            Toast.makeText(this, getString(R.string.shortcut_manager_already_pinned), Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete(shortcut: WebAppShortcut) {
        uiState.confirmDialog = ConfirmDialogConfig(
            title = getString(R.string.shortcut_manager_delete_confirm_title),
            message = getString(R.string.shortcut_manager_delete_confirm_message, shortcut.name),
            positiveLabel = getString(R.string.action_delete),
            negativeLabel = getString(R.string.action_cancel),
            onPositive = { deleteShortcut(shortcut) }
        )
    }

    private fun confirmDeleteSelected() {
        val count = uiState.selectedCount
        if (count == 0) return
        uiState.deleteConfirm = ConfirmDialogConfig(
            title = resources.getQuantityString(R.plurals.shortcut_manager_delete_selected_title, count, count),
            message = resources.getQuantityString(R.plurals.shortcut_manager_delete_selected_message, count, count),
            positiveLabel = getString(R.string.action_delete),
            negativeLabel = getString(R.string.action_cancel),
            onPositive = { deleteSelected() }
        )
    }

    private fun deleteSelected() {
        val ids = uiState.selectedIds
        if (ids.isEmpty()) return
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { removeShortcuts(ids.toList()) }
            uiState.shortcuts = uiState.shortcuts.filterNot { it.id in ids }
            uiState.exitSelectionMode()
            Toast.makeText(
                this@ShortcutManagerActivity,
                resources.getQuantityString(R.plurals.shortcut_manager_deleted_count, ids.size, ids.size),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun deleteShortcut(shortcut: WebAppShortcut) {
        removeShortcuts(listOf(shortcut.id))
        uiState.shortcuts = uiState.shortcuts.filter { it.id != shortcut.id }
        uiState.selectedIds = uiState.selectedIds - shortcut.id
    }

    private fun removeShortcuts(ids: List<String>) {
        ids.forEach { id ->
            ShortcutStore.delete(this, id)
            ShortcutIconStore.delete(this, id)
            ShortcutTabSessionManager.clear(this, id)
        }
        runCatching {
            ShortcutManagerCompat.disableShortcuts(this, ids, getString(R.string.shortcut_manager_disabled_message))
        }
    }
}
