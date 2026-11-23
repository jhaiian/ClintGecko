package com.jhaiian.clint.privacy

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.parental.ParentalControls
import com.jhaiian.clint.parental.ParentalGateHost
import com.jhaiian.clint.parental.ParentalGateRequest
import com.jhaiian.clint.ui.rememberMaxContentWidth
import com.jhaiian.clint.ui.theme.ClintComposeTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DeleteBrowsingDataActivity : ClintActivity() {

    private var range by mutableStateOf(BrowsingDataRange.LAST_HOUR)
    private var selection by mutableStateOf<Set<BrowsingDataType>>(emptySet())
    private var summary by mutableStateOf<BrowsingDataSummary?>(null)
    private var isDeleting by mutableStateOf(false)
    private var parentalGate by mutableStateOf<ParentalGateRequest?>(null)
    private var summaryJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("app_theme", "system") ?: "system"
        range = BrowsingDataCleaner.savedRange(this)
        selection = BrowsingDataCleaner.savedSelection(this)
        refreshSummary()

        setContent {
            ClintComposeTheme(theme = theme) {
                DeleteBrowsingDataScreen(
                    range = range,
                    selection = selection,
                    summary = summary,
                    isDeleting = isDeleting,
                    maxContentWidth = rememberMaxContentWidth(this),
                    onBack = { finish() },
                    onRangeChange = { newRange ->
                        range = newRange
                        BrowsingDataCleaner.saveRange(this, newRange)
                        refreshSummary()
                    },
                    onToggle = { type ->
                        selection = if (type in selection) selection - type else selection + type
                        BrowsingDataCleaner.saveSelection(this, selection)
                    },
                    onDelete = { deleteSelected() }
                )
                ParentalGateHost(
                    request = parentalGate,
                    hideStatusBar = prefs.getBoolean("hide_status_bar", false),
                    hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)
                ) { parentalGate = null }
            }
        }
    }

    private fun refreshSummary() {
        summaryJob?.cancel()
        summaryJob = lifecycleScope.launch {
            summary = BrowsingDataCleaner.summary(this@DeleteBrowsingDataActivity, range)
        }
    }

    private fun deleteSelected() {
        if (isDeleting) return
        val currentSummary = summary
        val tabsAvailable = (currentSummary?.openTabs ?: 0) > 0
        val toDelete = if (tabsAvailable) selection else selection - BrowsingDataType.TABS
        if (toDelete.isEmpty()) return
        if (BrowsingDataType.HISTORY !in toDelete) {
            performDelete(toDelete)
            return
        }
        ParentalControls.gate(
            context = this,
            area = ParentalControls.AREA_HISTORY,
            title = getString(R.string.parental_history_title),
            message = getString(R.string.parental_history_message),
            positiveLabel = getString(R.string.history_delete_selected),
            show = { parentalGate = it },
            action = { performDelete(toDelete) }
        )
    }

    private fun performDelete(toDelete: Set<BrowsingDataType>) {
        if (isDeleting) return
        isDeleting = true
        lifecycleScope.launch {
            BrowsingDataCleaner.delete(this@DeleteBrowsingDataActivity, range, toDelete)
            Toast.makeText(this@DeleteBrowsingDataActivity, getString(R.string.delete_data_done), Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
