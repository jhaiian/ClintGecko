package com.jhaiian.clint.settings.toolbarcustomization

import android.content.Intent
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
import com.jhaiian.clint.browser.toolbar.ToolbarCustomizationStore
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.listscreen.ConfirmDialogHost
import com.jhaiian.clint.ui.rememberMaxContentWidth
import com.jhaiian.clint.ui.theme.ClintComposeTheme

class ToolbarCustomizationActivity : ClintActivity() {

    private lateinit var uiState: ToolbarCustomizationUiState
    private var restartBaseline = Triple(false, false, false)

    private fun restartSettings() = Triple(uiState.compact, uiState.showLabels, uiState.showInBottomPosition)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("app_theme", "system") ?: "system"
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)
        uiState = ToolbarCustomizationUiState(ToolbarCustomizationStore.read(prefs))
        restartBaseline = restartSettings()

        setContent {
            ClintComposeTheme(theme = theme) {
                val maxContentWidth = rememberMaxContentWidth(this)
                var confirm by remember { mutableStateOf<ConfirmDialogConfig?>(null) }

                Box {
                    ToolbarCustomizationScreen(
                        state = uiState,
                        prefs = prefs,
                        maxContentWidth = maxContentWidth,
                        onExit = { finish() },
                        onChanged = { ToolbarCustomizationStore.write(prefs, uiState.toLayout()) },
                        onRestartRequired = { revert ->
                            confirm = ConfirmDialogConfig(
                                title = getString(R.string.restart_required_title),
                                message = getString(R.string.restart_required_message),
                                cancelable = false,
                                positiveLabel = getString(R.string.action_later),
                                onPositive = { ToolbarCustomizationStore.write(prefs, uiState.toLayout()) },
                                negativeLabel = getString(R.string.action_cancel),
                                onNegative = revert,
                                neutralLabel = getString(R.string.restart_required_confirm),
                                onNeutral = {
                                    ToolbarCustomizationStore.write(prefs, uiState.toLayout())
                                    restartApp()
                                }
                            )
                        },
                        onResetClick = {
                            confirm = ConfirmDialogConfig(
                                title = getString(R.string.toolbar_customization_reset_confirm_title),
                                message = getString(R.string.toolbar_customization_reset_confirm_message),
                                negativeLabel = getString(R.string.action_cancel),
                                positiveLabel = getString(R.string.menu_customization_reset_action),
                                onPositive = {
                                    ToolbarCustomizationStore.reset(prefs)
                                    uiState.load(ToolbarCustomizationStore.read(prefs))
                                }
                            )
                        }
                    )

                    ConfirmDialogHost(confirm, hideStatusBar, hideSystemNavigation) { confirm = null }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (restartSettings() != restartBaseline) restartApp()
    }

    private fun restartApp() {
        restartBaseline = restartSettings()
        val restartIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(restartIntent)
    }
}
