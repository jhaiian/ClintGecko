package com.jhaiian.clint.history

import com.jhaiian.clint.R

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.compose.material3.SnackbarHostState
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.ui.ClintSnackbarHost
import com.jhaiian.clint.ui.SnackbarHostActivity
import com.jhaiian.clint.ui.UNDO_SNACKBAR_TIMEOUT_MS
import com.jhaiian.clint.ui.showClintSnackbar
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.listscreen.ConfirmDialogHost
import com.jhaiian.clint.ui.rememberMaxContentWidth
import com.jhaiian.clint.ui.theme.ClintComposeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryActivity : ClintActivity(), SnackbarHostActivity {

    override val snackbarHostState = SnackbarHostState()

    private lateinit var uiState: HistoryUiState
    private lateinit var prefs: android.content.SharedPreferences

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

        uiState = HistoryUiState()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("app_theme", "system") ?: "system"
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)

        loadHistory()

        setContent {
            ClintComposeTheme(theme = theme) {
                val maxContentWidth = rememberMaxContentWidth(this)

                HistoryScreen(
                    state = uiState,
                    maxContentWidth = maxContentWidth,
                    onExit = { finish() },
                    onOpenItem = { item -> openHistoryItem(item) },
                    onDeleteItem = { item -> deleteItem(item) },
                    onDeleteSelectedClick = { showDeleteConfirm() },
                    onClearAllClick = { showClearAllConfirm() },
                    onDeleteBrowsingDataClick = {
                        startActivity(Intent(this, com.jhaiian.clint.privacy.DeleteBrowsingDataActivity::class.java))
                    }
                )

                ConfirmDialogHost(uiState.deleteConfirm, hideStatusBar, hideSystemNavigation) { uiState.deleteConfirm = null }
                ConfirmDialogHost(uiState.clearAllConfirm, hideStatusBar, hideSystemNavigation) { uiState.clearAllConfirm = null }
                ClintSnackbarHost(hostState = snackbarHostState)
            }
        }
    }

    override fun onRestart() {
        super.onRestart()
        loadHistory()
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) { SearchHistoryManager.getAll(this@HistoryActivity) }
            uiState.items = items
            uiState.isLoading = false
        }
    }

    private fun openHistoryItem(item: HistoryItem) {
        val url = if (item.query.startsWith("http")) {
            item.query
        } else {
            val encoded = Uri.encode(item.query)
            when (prefs.getString("search_engine", "duckduckgo")) {
                "brave" -> "https://search.brave.com/search?q=$encoded"
                "ecosia" -> "https://www.ecosia.org/search?q=$encoded"
                "google" -> "https://www.google.com/search?q=$encoded"
                "custom" -> com.jhaiian.clint.browser.customSearchEngineQueryUrl(prefs, encoded)
                else -> "https://duckduckgo.com/?q=$encoded"
            }
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.setPackage(packageName)
        startActivity(intent)
        finish()
    }

    private fun deleteItem(item: HistoryItem) {
        uiState.items = uiState.items.filterNot { it.query == item.query }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { SearchHistoryManager.delete(this@HistoryActivity, item.query) }
            showClintSnackbar(
                message = getString(R.string.history_items_deleted),
                actionLabel = getString(R.string.action_undo),
                timeoutMs = UNDO_SNACKBAR_TIMEOUT_MS
            ) { restoreItem(item) }
        }
    }

    private fun restoreItem(item: HistoryItem) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { SearchHistoryManager.restore(this@HistoryActivity, item) }
            if (uiState.items.none { it.query == item.query }) {
                uiState.items = (uiState.items + item).sortedByDescending { it.timestamp }
            }
        }
    }

    private fun showDeleteConfirm() {
        val count = uiState.selectedCount
        if (count == 0) return
        uiState.deleteConfirm = ConfirmDialogConfig(
            title = getString(R.string.history_delete_confirm_title),
            message = getString(R.string.history_delete_confirm_message, count),
            negativeLabel = getString(R.string.action_cancel),
            positiveLabel = getString(R.string.history_delete_selected),
            onPositive = { deleteSelected() }
        )
    }

    private fun deleteSelected() {
        val toDelete = uiState.selectedKeys
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                for (query in toDelete) SearchHistoryManager.delete(this@HistoryActivity, query)
            }
            uiState.items = uiState.items.filterNot { it.query in toDelete }
            uiState.exitSelectionMode()
            showClintSnackbar(getString(R.string.history_items_deleted))
        }
    }

    private fun showClearAllConfirm() {
        if (uiState.items.isEmpty()) return
        uiState.clearAllConfirm = ConfirmDialogConfig(
            title = getString(R.string.history_clear_all),
            message = getString(R.string.history_clear_all_confirm_message),
            negativeLabel = getString(R.string.action_cancel),
            positiveLabel = getString(R.string.history_clear_all),
            onPositive = { clearAllHistory() }
        )
    }

    private fun clearAllHistory() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { SearchHistoryManager.clear(this@HistoryActivity) }
            uiState.items = emptyList()
            uiState.exitSelectionMode()
            showClintSnackbar(getString(R.string.history_all_cleared))
        }
    }
}
