package com.jhaiian.clint.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import androidx.compose.ui.graphics.toArgb
import com.jhaiian.clint.ui.theme.resolveClintTheme

internal object ThemeSwatchUtils {

    data class SwatchColors(val bg: Int, val surface: Int, val accent: Int)

    fun buildSwatchDrawable(context: Context, bgColor: Int, surfaceColor: Int, accentColor: Int): LayerDrawable {
        val dp = context.resources.displayMetrics.density
        val r10 = 10f * dp
        val r3 = 3f * dp
        val bgShape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(bgColor)
            cornerRadius = r10
        }
        val surfaceShape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(surfaceColor)
            cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, r10, r10, r10, r10)
        }
        val accentShape = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(accentColor)
            cornerRadius = r3
        }
        val top22 = (22f * dp).toInt()
        val top6 = (6f * dp).toInt()
        val left6 = (6f * dp).toInt()
        val right14 = (14f * dp).toInt()
        val bottom24 = (24f * dp).toInt()
        return LayerDrawable(arrayOf(bgShape, surfaceShape, accentShape)).also {
            it.setLayerInset(1, 0, top22, 0, 0)
            it.setLayerInset(2, left6, top6, right14, bottom24)
        }
    }

    fun resolveSwatchColors(context: Context, theme: String, accent: String): SwatchColors {
        val resolved = resolveClintTheme(context, theme, accent, "strong_tint")
        return SwatchColors(resolved.background.toArgb(), resolved.surface.toArgb(), resolved.primary.toArgb())
    }

    fun resolveSoftTintSwatchBgSurface(context: Context, theme: String, accent: String): Pair<Int, Int> {
        val resolved = resolveClintTheme(context, theme, accent, "soft_tint")
        return resolved.background.toArgb() to resolved.surface.toArgb()
    }

    fun resolveNoTintSwatchBgSurface(context: Context, theme: String, accent: String): Pair<Int, Int> {
        val resolved = resolveClintTheme(context, theme, accent, "no_tint")
        return resolved.background.toArgb() to resolved.surface.toArgb()
    }

    fun isSurfaceIntensityEnabled(theme: String, accent: String): Boolean =
        accent == "material_you" || accent == "purple" || accent == "deep_purple" || accent == "royal_purple" || accent == "amethyst" || accent == "lavender" || accent == "teal" || accent == "pink" || accent == "indigo" || accent == "cyan" || accent == "amber" || accent == "mint" || accent == "crimson" || accent == "slate" || accent == "graphite" || accent == "obsidian" || accent == "onyx" || accent == "coral" || accent == "midnight" || accent == "sepia" || accent == "forest" || accent == "plum" || accent == "sand" || accent == "ruby" || accent == "sky" || accent == "charcoal" || accent == "peach" || accent == "emerald" || accent == "blue" || accent == "yellow" || accent == "lemon" || accent == "gold" || accent == "red" || accent == "green" || accent == "orange" || accent == "deep_orange" || accent == "tangerine" || accent == "apricot" || accent == "copper" || accent == "scarlet" || accent == "lime" || accent == "olive" || accent == "default" || accent == "violet" || accent == "titanium" || accent == "azure" || accent == "mustard" || accent == "burgundy" || accent == "terracotta" || accent == "sage"
}
