package com.jhaiian.clint.settings.desktopmode

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import com.jhaiian.clint.ui.ClintRadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.setup.SectionLabel
import com.jhaiian.clint.setup.SelectableCard
import com.jhaiian.clint.settings.sitepermissions.SitePermissionDatabase
import com.jhaiian.clint.settings.sitepermissions.SitePermissionManager
import com.jhaiian.clint.ui.rememberMaxContentWidth
import com.jhaiian.clint.settings.site.AddSiteDialog
import com.jhaiian.clint.settings.site.SiteEntry
import com.jhaiian.clint.settings.site.SiteListDeleteConfirmDialog
import com.jhaiian.clint.settings.site.SiteListScreen
import com.jhaiian.clint.settings.site.SiteListUiState
import com.jhaiian.clint.ui.theme.ClintComposeTheme
import com.jhaiian.clint.ui.theme.LocalClintColors

class DesktopModeActivity : ClintActivity() {

    companion object {
        const val PREF_DESKTOP_MODE_SAVE_STATE = "desktop_mode_save_state"
        const val VALUE_SAVE_STATE = "save"
        const val VALUE_DO_NOT_SAVE = "do_not_save"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val type = SitePermissionDatabase.TYPE_DESKTOP_MODE
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("app_theme", "system") ?: "system"
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)

        val listState = SiteListUiState()
        fun reload() {
            listState.allItems = SitePermissionManager.getAllByType(this, type).map { SiteEntry(it.first, it.second, it.third) }
        }
        reload()

        setContent {
            ClintComposeTheme(theme = theme) {
                val colors = LocalClintColors.current
                val maxContentWidth = rememberMaxContentWidth(this)
                var saveState by remember {
                    mutableStateOf(prefs.getString(PREF_DESKTOP_MODE_SAVE_STATE, VALUE_SAVE_STATE) ?: VALUE_SAVE_STATE)
                }

                Box {
                    SiteListScreen(
                        state = listState,
                        maxContentWidth = maxContentWidth,
                        title = stringResource(R.string.site_settings_desktop_mode),
                        searchHint = stringResource(R.string.desktop_mode_search_hint),
                        emptyText = stringResource(R.string.desktop_mode_no_saved_sites),
                        stateLabel = { _ -> stringResource(R.string.desktop_mode_state_on) to colors.primary },
                        onExit = { finish() },
                        onAddClick = { listState.addDialogOpen = true },
                        onDeleteClick = { listState.deleteConfirmOpen = true },
                        header = {
                            listOf(
                                Triple(VALUE_SAVE_STATE, R.string.desktop_mode_save_state, R.string.desktop_mode_save_state_desc),
                                Triple(VALUE_DO_NOT_SAVE, R.string.desktop_mode_do_not_save_state, R.string.desktop_mode_do_not_save_state_desc)
                            ).forEach { (value, titleRes, descRes) ->
                                val selected = saveState == value
                                SelectableCard(
                                    selected = selected,
                                    onClick = {
                                        prefs.edit().putString(PREF_DESKTOP_MODE_SAVE_STATE, value).apply()
                                        saveState = value
                                    },
                                    cardBackground = colors.cardBackground, primary = colors.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    contentPadding = 14.dp, bottomSpacing = 8.dp
                                ) {
                                    ClintRadioButton(selected = selected)
                                    Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                                        Text(stringResource(titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        Text(stringResource(descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                                    }
                                }
                            }
                            SectionLabel(
                                stringResource(R.string.desktop_mode_saved_sites), colors.primary,
                                Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
                            )
                        }
                    )

                    if (listState.addDialogOpen) {
                        AddSiteDialog(
                            title = stringResource(R.string.desktop_mode_add_site),
                            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                            showStateChoice = false,
                            onConfirm = { origin, _ ->
                                SitePermissionManager.setState(this@DesktopModeActivity, origin, type, SitePermissionDatabase.STATE_ALLOW)
                                reload()
                                listState.addDialogOpen = false
                            },
                            onDismiss = { listState.addDialogOpen = false }
                        )
                    }
                    if (listState.deleteConfirmOpen) {
                        SiteListDeleteConfirmDialog(
                            title = stringResource(R.string.desktop_mode_delete_confirm_title),
                            message = stringResource(R.string.desktop_mode_delete_confirm_message, listState.selectedOrigins.size),
                            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                            onConfirm = {
                                listState.selectedOrigins.forEach { origin -> SitePermissionManager.deleteEntry(this@DesktopModeActivity, origin, type) }
                                listState.removeSelectedItems()
                                listState.deleteConfirmOpen = false
                                Toast.makeText(this@DesktopModeActivity, getString(R.string.desktop_mode_items_deleted), Toast.LENGTH_SHORT).show()
                            },
                            onDismiss = { listState.deleteConfirmOpen = false }
                        )
                    }
                }
            }
        }
    }
}
