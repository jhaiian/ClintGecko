package com.jhaiian.clint.base

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.preference.PreferenceManager
import com.jhaiian.clint.app.ClintApplication
import com.jhaiian.clint.ui.ThemeRevealHolder
import com.jhaiian.clint.ui.ThemeRevealOverlay
import com.jhaiian.clint.ui.theme.ThemeMode
import com.jhaiian.clint.ui.theme.resolveClintTheme
import com.jhaiian.clint.util.LocaleHelper
import kotlin.math.hypot
import kotlin.math.max

abstract class ClintActivity : AppCompatActivity() {

    private var appliedTheme: String? = null
    private var appliedAccent: String? = null
    private var appliedIntensity: String? = null
    private var appliedLanguage: String? = null
    private var appliedSystemDark = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        appliedTheme = prefs.getString("app_theme", "system") ?: "system"
        appliedAccent = prefs.getString("accent_color", "material_you") ?: "material_you"
        appliedIntensity = prefs.getString("surface_intensity", "soft_tint") ?: "soft_tint"
        appliedLanguage = prefs.getString(LocaleHelper.PREF_APP_LANGUAGE, LocaleHelper.LANGUAGE_SYSTEM) ?: LocaleHelper.LANGUAGE_SYSTEM
        appliedSystemDark = ThemeMode.isSystemDark()
        applyWindowChrome()
        super.onCreate(savedInstanceState)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        syncSystemTheme()
    }

    private fun syncSystemTheme() {
        val systemDark = ThemeMode.isSystemDark()
        if (systemDark == appliedSystemDark) return
        appliedSystemDark = systemDark
        if (appliedTheme == ThemeMode.SYSTEM) {
            applyWindowChrome()
            onSystemThemeChanged()
        }
    }

    protected open fun onSystemThemeChanged() {}

    override fun onResume() {
        super.onResume()
        openDialogCount = 0
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val currentTheme = prefs.getString("app_theme", "system") ?: "system"
        val currentAccent = prefs.getString("accent_color", "material_you") ?: "material_you"
        val currentIntensity = prefs.getString("surface_intensity", "soft_tint") ?: "soft_tint"
        val currentLanguage = prefs.getString(LocaleHelper.PREF_APP_LANGUAGE, LocaleHelper.LANGUAGE_SYSTEM) ?: LocaleHelper.LANGUAGE_SYSTEM
        if (currentTheme != appliedTheme || currentAccent != appliedAccent || currentIntensity != appliedIntensity || currentLanguage != appliedLanguage) {
            window.setWindowAnimations(0)
            recreate()
            return
        }
        syncSystemTheme()
        applyStatusBarVisibility()
        applyWindowChrome()
        window.decorView.post {
            applyStatusBarVisibility()
            startRevealIfNeeded()
        }
    }

    @Suppress("DEPRECATION")
    private fun applyWindowChrome() {
        val theme = appliedTheme ?: "system"
        val accent = appliedAccent ?: "material_you"
        val intensity = appliedIntensity ?: "soft_tint"
        val resolved = resolveClintTheme(this, theme, accent, intensity)
        window.setBackgroundDrawable(ColorDrawable(resolved.background.toArgb()))
        window.statusBarColor = resolved.statusBar.toArgb()
        window.navigationBarColor = resolved.navigationBar.toArgb()
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = resolved.isLight
        controller.isAppearanceLightNavigationBars = resolved.isLight
    }

    fun captureAndRecreate(newTheme: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val current = prefs.getString("app_theme", "system") ?: "system"
        if (current == newTheme) return
        captureScreenBitmap()
        prefs.edit().putString("app_theme", newTheme).commit()
        (application as? ClintApplication)?.applyNightMode()
        window.setWindowAnimations(0)
        recreate()
    }

    fun captureAndApplyAccentColor(newAccent: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val current = prefs.getString("accent_color", "material_you") ?: "material_you"
        if (current == newAccent) return
        val currentIntensity = prefs.getString("surface_intensity", "soft_tint") ?: "soft_tint"
        if (currentIntensity == "strong_tint" && newAccent != "purple" && newAccent != "deep_purple" && newAccent != "royal_purple" && newAccent != "amethyst" && newAccent != "lavender" && newAccent != "teal" && newAccent != "pink" && newAccent != "indigo" && newAccent != "cyan" && newAccent != "amber" && newAccent != "mint" && newAccent != "crimson" && newAccent != "slate" && newAccent != "graphite" && newAccent != "obsidian" && newAccent != "onyx" && newAccent != "coral" && newAccent != "midnight" && newAccent != "sepia" && newAccent != "forest" && newAccent != "plum" && newAccent != "sand" && newAccent != "ruby" && newAccent != "sky" && newAccent != "charcoal" && newAccent != "peach" && newAccent != "emerald" && newAccent != "blue" && newAccent != "yellow" && newAccent != "lemon" && newAccent != "gold" && newAccent != "red" && newAccent != "green" && newAccent != "orange" && newAccent != "deep_orange" && newAccent != "tangerine" && newAccent != "apricot" && newAccent != "copper" && newAccent != "scarlet" && newAccent != "lime" && newAccent != "olive" && newAccent != "default" && newAccent != "material_you" && newAccent != "violet" && newAccent != "titanium" && newAccent != "azure" && newAccent != "mustard" && newAccent != "burgundy" && newAccent != "terracotta" && newAccent != "sage") {
            prefs.edit().putString("surface_intensity", "soft_tint").apply()
        }
        captureScreenBitmap()
        prefs.edit().putString("accent_color", newAccent).commit()
        window.setWindowAnimations(0)
        recreate()
    }

    fun captureAndApplySurfaceIntensity(newIntensity: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val current = prefs.getString("surface_intensity", "soft_tint") ?: "soft_tint"
        if (current == newIntensity) return
        captureScreenBitmap()
        prefs.edit().putString("surface_intensity", newIntensity).commit()
        window.setWindowAnimations(0)
        recreate()
    }

    fun captureAndApplyLanguage(newLanguage: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val current = prefs.getString(LocaleHelper.PREF_APP_LANGUAGE, LocaleHelper.LANGUAGE_SYSTEM) ?: LocaleHelper.LANGUAGE_SYSTEM
        if (current == newLanguage) return
        captureScreenBitmap()
        prefs.edit().putString(LocaleHelper.PREF_APP_LANGUAGE, newLanguage).commit()
        window.setWindowAnimations(0)
        recreate()
    }

    private fun captureScreenBitmap() {
        val decor = window.decorView
        try {
            val bmp = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
            decor.draw(Canvas(bmp))
            ThemeRevealHolder.bitmap = bmp
            ThemeRevealHolder.cx = decor.width / 2
            ThemeRevealHolder.cy = decor.height / 2
        } catch (_: Exception) {
        }
    }

    private fun startRevealIfNeeded() {
        val (bmp, cx, cy) = ThemeRevealHolder.consume() ?: return
        if (isFinishing || isDestroyed) {
            bmp.recycle()
            return
        }

        val decor = window.decorView as? android.view.ViewGroup ?: run {
            bmp.recycle()
            return
        }

        val maxRadius = hypot(
            max(cx, decor.width - cx).toDouble(),
            max(cy, decor.height - cy).toDouble()
        ).toFloat()

        val overlay = ThemeRevealOverlay(this, bmp, cx, cy).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        decor.addView(overlay)

        ValueAnimator.ofFloat(0f, maxRadius).apply {
            duration = 450
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                overlay.revealRadius = it.animatedValue as Float
                overlay.invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    decor.removeView(overlay)
                    bmp.recycle()
                }
            })
            start()
        }
    }

    private var openDialogCount = 0

    fun trackDialogShown() { openDialogCount++ }
    fun trackDialogDismissed() { if (openDialogCount > 0) openDialogCount-- }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && openDialogCount == 0) applyStatusBarVisibility()
    }

    fun applyStatusBarFlagToDialog(dialog: android.app.Dialog) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)
        if (hideStatusBar || hideSystemNavigation) {
            dialog.window?.let { dialogWindow ->
                val dialogController = WindowInsetsControllerCompat(dialogWindow, dialogWindow.decorView)
                dialogController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (hideStatusBar) dialogController.hide(WindowInsetsCompat.Type.statusBars())
                if (hideSystemNavigation) dialogController.hide(WindowInsetsCompat.Type.navigationBars())
            }
        }
        trackDialogShown()
        dialog.setOnDismissListener { trackDialogDismissed() }
    }

    private fun applyStatusBarVisibility() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hideStatusBar) {
            controller.hide(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.show(WindowInsetsCompat.Type.statusBars())
        }
        if (hideSystemNavigation) {
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            controller.show(WindowInsetsCompat.Type.navigationBars())
        }
        window.decorView.requestApplyInsets()
    }
}
