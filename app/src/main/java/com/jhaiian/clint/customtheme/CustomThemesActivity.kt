package com.jhaiian.clint.customtheme

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.ui.rememberMaxContentWidth
import com.jhaiian.clint.ui.theme.ClintComposeTheme

class CustomThemesActivity : ClintActivity() {

    private class EditorSession(val theme: CustomTheme, val isNew: Boolean)

    private val themes = mutableStateListOf<CustomTheme>()
    private var activeAccent by mutableStateOf("material_you")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val appTheme = prefs.getString("app_theme", "system") ?: "system"
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)
        reload()

        setContent {
            ClintComposeTheme(theme = appTheme) {
                val maxContentWidth = rememberMaxContentWidth(this@CustomThemesActivity)
                var session by remember { mutableStateOf<EditorSession?>(null) }
                val current = session

                if (current == null) {
                    CustomThemesScreen(
                        themes = themes,
                        activeAccent = activeAccent,
                        maxContentWidth = maxContentWidth,
                        hideStatusBar = hideStatusBar,
                        hideSystemNavigation = hideSystemNavigation,
                        onExit = { finish() },
                        onCreate = {
                            var index = themes.size + 1
                            var name = getString(R.string.custom_theme_default_name, index)
                            while (CustomThemeRepository.isNameTaken(this@CustomThemesActivity, name)) {
                                index++
                                name = getString(R.string.custom_theme_default_name, index)
                            }
                            session = EditorSession(
                                CustomThemeRepository.createFromCurrent(this@CustomThemesActivity, name),
                                true
                            )
                        },
                        onApply = { theme -> captureAndApplyAccentColor(customAccentFor(theme.id)) },
                        onEdit = { theme -> session = EditorSession(theme, false) },
                        onDuplicate = { theme -> duplicate(theme) },
                        onDelete = { theme -> remove(theme) }
                    )
                } else {
                    CustomThemeEditorScreen(
                        initial = current.theme,
                        isNew = current.isNew,
                        maxContentWidth = maxContentWidth,
                        hideStatusBar = hideStatusBar,
                        hideSystemNavigation = hideSystemNavigation,
                        takenNames = themes.filter { it.id != current.theme.id }.map { it.name },
                        onSave = { saved ->
                            if (!CustomThemeRepository.save(this@CustomThemesActivity, saved)) return@CustomThemeEditorScreen
                            reload()
                            if (activeAccent == customAccentFor(saved.id)) {
                                captureAndRecreateForCustomTheme()
                            } else {
                                session = null
                            }
                        },
                        onExit = { session = null }
                    )
                }
            }
        }
    }

    private fun reload() {
        themes.clear()
        themes.addAll(CustomThemeRepository.all(this))
        activeAccent = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("accent_color", "material_you") ?: "material_you"
    }

    private fun duplicate(theme: CustomTheme) {
        val name = CustomThemeRepository.uniqueName(this, getString(R.string.custom_theme_copy_name, theme.name))
        CustomThemeRepository.save(this, theme.copy(id = CustomThemeRepository.newId(), name = name))
        reload()
    }

    private fun remove(theme: CustomTheme) {
        val wasActive = activeAccent == customAccentFor(theme.id)
        CustomThemeRepository.delete(this, theme.id)
        reload()
        if (wasActive) captureAndApplyAccentColor("material_you")
    }
}
