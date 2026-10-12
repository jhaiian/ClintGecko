package com.jhaiian.clint.settings.menucustomization

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.browser.menu.BrowserMenuCustomizationStore
import com.jhaiian.clint.browser.menu.CustomizableMenuItem
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.listscreen.ConfirmDialogHost
import com.jhaiian.clint.ui.rememberMaxContentWidth
import com.jhaiian.clint.ui.theme.ClintComposeTheme

class MenuCustomizationActivity : ClintActivity() {

    private lateinit var uiState: MenuCustomizationUiState

    private fun buildEntries(prefs: android.content.SharedPreferences): List<MenuCustomizationEntry> {
        val order = BrowserMenuCustomizationStore.readOrder(prefs)
        val hidden = BrowserMenuCustomizationStore.readHidden(prefs)
        return order.map { MenuCustomizationEntry(it, it !in hidden) }
    }

    private fun persistHidden(prefs: android.content.SharedPreferences) {
        val hidden = uiState.entries.filter { !it.visible }.map { it.item }.toSet()
        BrowserMenuCustomizationStore.writeHidden(prefs, hidden)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("app_theme", "system") ?: "system"
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)

        uiState = MenuCustomizationUiState(buildEntries(prefs))

        setContent {
            ClintComposeTheme(theme = theme) {
                val maxContentWidth = rememberMaxContentWidth(this)
                var resetConfirm by remember { mutableStateOf<ConfirmDialogConfig?>(null) }

                Box {
                    MenuCustomizationScreen(
                        state = uiState,
                        maxContentWidth = maxContentWidth,
                        onExit = { finish() },
                        onToggleVisible = { id ->
                            val entry = uiState.entries.find { it.item.id == id }
                            entry?.let { it.visible = !it.visible }
                            persistHidden(prefs)
                        },
                        onCommitReorder = { orderedIds ->
                            val items = orderedIds.mapNotNull { CustomizableMenuItem.fromId(it) }
                            BrowserMenuCustomizationStore.writeOrder(prefs, items)
                        },
                        onResetClick = {
                            resetConfirm = ConfirmDialogConfig(
                                title = getString(R.string.menu_customization_reset_confirm_title),
                                message = getString(R.string.menu_customization_reset_confirm_message),
                                negativeLabel = getString(R.string.action_cancel),
                                positiveLabel = getString(R.string.menu_customization_reset_action),
                                onPositive = {
                                    BrowserMenuCustomizationStore.resetToDefault(prefs)
                                    uiState.replaceAll(buildEntries(prefs))
                                }
                            )
                        }
                    )

                    ConfirmDialogHost(resetConfirm, hideStatusBar, hideSystemNavigation) { resetConfirm = null }
                }
            }
        }
    }
}
