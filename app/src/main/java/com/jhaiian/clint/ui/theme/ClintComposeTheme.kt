package com.jhaiian.clint.ui.theme

import com.jhaiian.clint.customtheme.CUSTOM_THEME_REVISION_PREF
import com.jhaiian.clint.customtheme.isCustomAccent
import com.jhaiian.clint.customtheme.resolveCustomPalette
import com.jhaiian.clint.customtheme.toClintColors
import android.content.Context
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

fun resolveClintColors(context: Context, isLight: Boolean, accent: String, intensity: String): ClintColors {
    val palette = resolveCustomPalette(context, accent, isLight)
    if (palette != null) return palette.toClintColors(isLight)
    val safeAccent = if (isCustomAccent(accent)) "material_you" else accent
    return ClintColors(resolveClintTheme(context, if (isLight) "light" else "dark", safeAccent, intensity))
}

@Composable
fun ClintComposeTheme(theme: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val isLightTheme = rememberIsLightTheme(theme)
    val resolvedTheme = if (isLightTheme) "light" else "dark"
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    val accent = prefs.getString("accent_color", "material_you") ?: "material_you"
    val intensity = prefs.getString("surface_intensity", "soft_tint") ?: "soft_tint"

    val revision = prefs.getLong(CUSTOM_THEME_REVISION_PREF, 0L)

    val clintColors = remember(resolvedTheme, accent, intensity, revision) {
        resolveClintColors(context, isLightTheme, accent, intensity)
    }

    val isLight = clintColors.isLight
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
