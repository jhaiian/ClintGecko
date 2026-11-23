package com.jhaiian.clint.settings.browser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.preference.PreferenceManager
import com.jhaiian.clint.settings.SettingsActivity
import com.jhaiian.clint.settings.common.SettingsScreenScaffold

@Composable
fun SuggestionsSettingsPane(activity: SettingsActivity) {
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(activity) }
    SettingsScreenScaffold {
        SuggestionSettingsSection(
            hideStatusBar = prefs.getBoolean("hide_status_bar", false),
            hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)
        )
    }
}
