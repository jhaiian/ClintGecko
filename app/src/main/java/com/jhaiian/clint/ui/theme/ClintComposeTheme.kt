package com.jhaiian.clint.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceManager

data class ClintColors(
    val background: Color,
    val onSurface: Color,
    val secondaryText: Color,
    val cardBackground: Color,
    val buttonBackground: Color,
    val surfaceVariant: Color,
    val popupBackground: Color,
    val primary: Color,
    val iconTint: Color,
    val divider: Color,
    val popupText: Color,
    val surface: Color,
    val buttonIconTint: Color,
    val popupStroke: Color,
    val popupCheck: Color,
    val onPrimary: Color,
    val colorError: Color,
    val colorErrorContainer: Color,
    val colorOnErrorContainer: Color,
    val buttonTextColor: Color,
    val addressBarColor: Color,
    val isLight: Boolean
)

val LocalClintColors = compositionLocalOf<ClintColors> {
    error("ClintComposeTheme not applied")
}

private fun ClintColors(resolved: ClintResolvedTheme) = ClintColors(
    background = resolved.background,
    onSurface = resolved.onSurface,
    secondaryText = resolved.secondaryText,
    cardBackground = resolved.cardBackground,
    buttonBackground = resolved.buttonBackground,
    surfaceVariant = resolved.surfaceVariant,
    popupBackground = resolved.popupBackground,
    primary = resolved.primary,
    iconTint = resolved.iconTint,
    divider = resolved.divider,
    popupText = resolved.popupText,
    surface = resolved.surface,
    buttonIconTint = resolved.buttonIconTint,
    popupStroke = resolved.popupStroke,
    popupCheck = resolved.popupCheck,
    onPrimary = resolved.onPrimary,
    colorError = resolved.error,
    colorErrorContainer = resolved.errorContainer,
    colorOnErrorContainer = resolved.onErrorContainer,
    buttonTextColor = resolved.buttonTextColor,
    addressBarColor = resolved.addressBar,
    isLight = resolved.isLight
)

@Composable
fun ClintComposeTheme(theme: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val resolvedTheme = if (rememberIsLightTheme(theme)) "light" else "dark"
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val accent = prefs.getString("accent_color", "material_you") ?: "material_you"
    val intensity = prefs.getString("surface_intensity", "soft_tint") ?: "soft_tint"

    val resolved = remember(resolvedTheme, accent, intensity) {
        resolveClintTheme(context, resolvedTheme, accent, intensity)
    }
    val clintColors = remember(resolved) { ClintColors(resolved) }

    val isLight = resolved.isLight
    val base = if (isLight) lightColorScheme() else darkColorScheme()
    val colorScheme = base.copy(
        primary = clintColors.primary,
        onPrimary = clintColors.onPrimary,
        background = clintColors.background,
        onBackground = clintColors.onSurface,
        surface = clintColors.cardBackground,
        onSurface = clintColors.onSurface,
        surfaceVariant = clintColors.surfaceVariant,
        onSurfaceVariant = clintColors.secondaryText,
        error = clintColors.colorError,
        errorContainer = clintColors.colorErrorContainer,
        onErrorContainer = clintColors.colorOnErrorContainer
    )

    CompositionLocalProvider(LocalClintColors provides clintColors) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
