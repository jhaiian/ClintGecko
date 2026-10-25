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
import com.jhaiian.clint.ui.rememberMaxContentWidth
import com.jhaiian.clint.ui.theme.ClintComposeTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class DeleteBrowsingDataActivity : ClintActivity() {

    private var range by mutableStateOf(BrowsingDataRange.LAST_HOUR)
    private var selection by mutableStateOf<Set<BrowsingDataType>>(emptySet())
    private var summary by mutableStateOf<BrowsingDataSummary?>(null)
    private var isDeleting by mutableStateOf(false)
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
        isDeleting = true
        lifecycleScope.launch {
            BrowsingDataCleaner.delete(this@DeleteBrowsingDataActivity, range, toDelete)
            Toast.makeText(this@DeleteBrowsingDataActivity, getString(R.string.delete_data_done), Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
