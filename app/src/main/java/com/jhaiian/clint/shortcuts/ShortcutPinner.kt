package com.jhaiian.clint.shortcuts

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity

object ShortcutPinner {

    fun isSupported(context: Context): Boolean =
        ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    fun isPinned(context: Context, shortcutId: String): Boolean = runCatching {
        context.getSystemService(ShortcutManager::class.java)
            ?.pinnedShortcuts
            ?.any { it.id == shortcutId && it.isEnabled } == true
    }.getOrDefault(false)

    fun launchIntent(context: Context, shortcut: WebAppShortcut): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(shortcut.url)).apply {
            setClass(context, ShortcutLaunchActivity::class.java)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(MainActivity.EXTRA_SHORTCUT_ID, shortcut.id)
        }

    fun requestPin(context: Context, shortcut: WebAppShortcut): Boolean {
        val icon = ShortcutIconStore.load(shortcut.iconPath)?.let { IconCompat.createWithBitmap(it) }
            ?: fallbackLauncherIcon(context)
        val info = ShortcutInfoCompat.Builder(context, shortcut.id)
            .setShortLabel(shortcut.name)
            .setLongLabel(shortcut.name)
            .setIcon(icon)
            .setIntent(launchIntent(context, shortcut))
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, info, null)
    }

    fun migrateLaunchIntents(context: Context) {
        val appContext = context.applicationContext
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(appContext)
        if (prefs.getBoolean(PREF_LAUNCH_INTENTS_MIGRATED, false)) return
        Thread {
            val infos = runCatching { ShortcutStore.getAll(appContext) }.getOrDefault(emptyList()).mapNotNull { shortcut ->
                runCatching {
                    ShortcutInfoCompat.Builder(appContext, shortcut.id)
                        .setShortLabel(shortcut.name)
                        .setLongLabel(shortcut.name)
                        .setIntent(launchIntent(appContext, shortcut))
                        .build()
                }.getOrNull()
            }
            val updated = infos.isEmpty() || runCatching { ShortcutManagerCompat.updateShortcuts(appContext, infos) }.getOrDefault(false)
            if (updated) prefs.edit().putBoolean(PREF_LAUNCH_INTENTS_MIGRATED, true).apply()
        }.start()
    }

    private const val PREF_LAUNCH_INTENTS_MIGRATED = "shortcut_launch_intents_migrated"

    private fun fallbackLauncherIcon(context: Context): IconCompat {
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
        val bitmap = drawable?.let {
            val width = it.intrinsicWidth.coerceAtLeast(1)
            val height = it.intrinsicHeight.coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            it.setBounds(0, 0, canvas.width, canvas.height)
            it.draw(canvas)
            bmp
        }
        return bitmap?.let { IconCompat.createWithBitmap(it) }
            ?: IconCompat.createWithResource(context, R.mipmap.ic_launcher)
    }
}
