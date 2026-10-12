package com.jhaiian.clint.ui.theme

import android.app.Application
import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ThemeMode {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"

    fun isSystemDark(): Boolean =
        (Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    fun isLight(theme: String): Boolean = when (theme) {
        LIGHT -> true
        DARK -> false
        else -> !isSystemDark()
    }
}

object SystemDarkState {
    private var dark by mutableStateOf(ThemeMode.isSystemDark())

    val isDark: Boolean get() = dark

    fun install(application: Application) {
        dark = ThemeMode.isSystemDark()
        application.registerComponentCallbacks(object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) {
                dark = (newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            }

            @Suppress("OVERRIDE_DEPRECATION")
            override fun onLowMemory() {}
        })
    }
}

@Composable
fun rememberIsLightTheme(theme: String): Boolean = when (theme) {
    ThemeMode.LIGHT -> true
    ThemeMode.DARK -> false
    else -> !SystemDarkState.isDark
}
