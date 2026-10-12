package com.jhaiian.clint.settings.sitepermissions

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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
import com.jhaiian.clint.setup.DefaultChip
import com.jhaiian.clint.setup.SectionLabel
import com.jhaiian.clint.setup.SelectableCard
import com.jhaiian.clint.ui.rememberMaxContentWidth
import com.jhaiian.clint.settings.site.AddSiteDialog
import com.jhaiian.clint.settings.site.AddSiteModeDialog
import com.jhaiian.clint.settings.site.SiteEntry
import com.jhaiian.clint.settings.site.SiteListDeleteConfirmDialog
import com.jhaiian.clint.settings.site.SiteListScreen
import com.jhaiian.clint.settings.site.SiteListUiState
import com.jhaiian.clint.ui.theme.ClintComposeTheme
import com.jhaiian.clint.ui.theme.LocalClintColors

class SitePermissionActivity : ClintActivity() {

    companion object {
        const val EXTRA_TYPE = "type"
        const val PREF_VALUE_ASK = "ask"
        const val PREF_VALUE_DENY = "deny"
        const val PREF_VALUE_ALLOW = "allow"
        const val PREF_VALUE_STAY = "stay"
        const val PREF_VALUE_OPEN = "open"
    }

    private fun titleForType(type: String): Int = when (type) {
        SitePermissionDatabase.TYPE_CAMERA -> R.string.site_settings_camera
        SitePermissionDatabase.TYPE_MIC -> R.string.site_settings_mic
        SitePermissionDatabase.TYPE_LOCATION -> R.string.site_settings_location
        SitePermissionDatabase.TYPE_NOTIFICATION -> R.string.site_settings_notifications
        SitePermissionDatabase.TYPE_CLIPBOARD -> R.string.site_settings_clipboard
        SitePermissionDatabase.TYPE_OPEN_IN_APP -> R.string.site_settings_open_in_app
        else -> R.string.site_settings_camera
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val type = intent.getStringExtra(EXTRA_TYPE) ?: SitePermissionDatabase.TYPE_CAMERA
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("app_theme", "system") ?: "system"
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)
        val defaultBehaviorKey = "site_perm_default_$type"
        val isOpenInApp = type == SitePermissionDatabase.TYPE_OPEN_IN_APP

        val listState = SiteListUiState()
        fun reload() {
            listState.allItems = SitePermissionManager.getAllByType(this, type).map { SiteEntry(it.first, it.second, it.third) }
        }
        reload()

        setContent {
            ClintComposeTheme(theme = theme) {
                val colors = LocalClintColors.current
                val maxContentWidth = rememberMaxContentWidth(this)
                var defaultBehavior by remember {
                    mutableStateOf(prefs.getString(defaultBehaviorKey, PREF_VALUE_ASK) ?: PREF_VALUE_ASK)
                }

                Box {
                    SiteListScreen(
                        state = listState,
                        maxContentWidth = maxContentWidth,
                        title = stringResource(titleForType(type)),
                        searchHint = stringResource(R.string.site_permission_search_hint),
                        emptyText = stringResource(R.string.site_permission_no_exceptions),
                        stateLabel = { state ->
                            when (state) {
                                SitePermissionDatabase.STATE_DENY -> stringResource(R.string.site_permission_state_denied) to colors.secondaryText
                                SitePermissionDatabase.STATE_STAY -> stringResource(R.string.open_in_app_mode_stay) to colors.secondaryText
                                SitePermissionDatabase.STATE_OPEN -> stringResource(R.string.open_in_app_mode_open) to colors.primary
                                SitePermissionDatabase.STATE_ASK -> stringResource(R.string.open_in_app_mode_ask) to colors.onSurface
                                else -> stringResource(R.string.site_permission_state_allowed) to colors.primary
                            }
                        },
                        onExit = { finish() },
                        onAddClick = { listState.addDialogOpen = true },
                        onDeleteClick = { listState.deleteConfirmOpen = true },
                        header = {
                            SectionLabel(
                                stringResource(R.string.site_permission_default_behavior), colors.primary,
                                Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp)
                            )
                            (if (isOpenInApp) listOf(
                                Triple(PREF_VALUE_ASK, R.string.open_in_app_mode_ask, R.string.open_in_app_mode_ask_desc),
                                Triple(PREF_VALUE_STAY, R.string.open_in_app_mode_stay, R.string.open_in_app_mode_stay_desc),
                                Triple(PREF_VALUE_OPEN, R.string.open_in_app_mode_open, R.string.open_in_app_mode_open_desc)
                            ) else listOf(
                                Triple(PREF_VALUE_ASK, R.string.site_permission_ask_first, R.string.site_permission_ask_first_desc),
                                Triple(PREF_VALUE_DENY, R.string.site_permission_always_deny, R.string.site_permission_always_deny_desc),
                                Triple(PREF_VALUE_ALLOW, R.string.site_permission_always_allow, R.string.site_permission_always_allow_desc)
                            )).forEach { (value, titleRes, descRes) ->
                                val selected = defaultBehavior == value
                                SelectableCard(
                                    selected = selected,
                                    onClick = {
                                        prefs.edit().putString(defaultBehaviorKey, value).apply()
                                        defaultBehavior = value
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
                                    if (value == PREF_VALUE_ASK) DefaultChip(stringResource(R.string.default_label), colors.primary)
                                }
                            }
                            HorizontalDivider(color = colors.divider, modifier = Modifier.padding(top = 4.dp))
                            SectionLabel(
                                stringResource(R.string.site_permission_exceptions), colors.primary,
                                Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
                            )
                        }
                    )

                    if (listState.addDialogOpen && isOpenInApp) {
                        AddSiteModeDialog(
                            title = stringResource(R.string.site_permission_add_exception),
                            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                            options = listOf(
                                SitePermissionDatabase.STATE_STAY to R.string.open_in_app_mode_stay,
                                SitePermissionDatabase.STATE_OPEN to R.string.open_in_app_mode_open,
                                SitePermissionDatabase.STATE_ASK to R.string.open_in_app_mode_ask
                            ),
                            onConfirm = { origin, state ->
                                SitePermissionManager.setState(this@SitePermissionActivity, origin, type, state)
                                reload()
                                listState.addDialogOpen = false
                            },
                            onDismiss = { listState.addDialogOpen = false }
                        )
                    } else if (listState.addDialogOpen) {
                        AddSiteDialog(
                            title = stringResource(R.string.site_permission_add_exception),
                            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                            showStateChoice = true,
                            onConfirm = { origin, allowed ->
                                val state = if (allowed) SitePermissionDatabase.STATE_ALLOW else SitePermissionDatabase.STATE_DENY
                                SitePermissionManager.setState(this@SitePermissionActivity, origin, type, state)
                                reload()
                                listState.addDialogOpen = false
                            },
                            onDismiss = { listState.addDialogOpen = false }
                        )
                    }
                    if (listState.deleteConfirmOpen) {
                        SiteListDeleteConfirmDialog(
                            title = stringResource(R.string.site_permission_delete_confirm_title),
                            message = stringResource(R.string.site_permission_delete_confirm_message, listState.selectedOrigins.size),
                            hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                            onConfirm = {
                                listState.selectedOrigins.forEach { origin -> SitePermissionManager.deleteEntry(this@SitePermissionActivity, origin, type) }
                                listState.removeSelectedItems()
                                listState.deleteConfirmOpen = false
                                Toast.makeText(this@SitePermissionActivity, getString(R.string.site_permission_items_deleted), Toast.LENGTH_SHORT).show()
                            },
                            onDismiss = { listState.deleteConfirmOpen = false }
                        )
                    }
                }
            }
        }
    }
}
