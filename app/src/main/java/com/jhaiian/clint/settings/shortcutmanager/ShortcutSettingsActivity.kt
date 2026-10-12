package com.jhaiian.clint.settings.shortcutmanager

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.shortcuts.ShortcutIconStore
import com.jhaiian.clint.shortcuts.ShortcutStore
import com.jhaiian.clint.shortcuts.ShortcutTabSessionManager
import com.jhaiian.clint.shortcuts.WebAppShortcut
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.listscreen.ConfirmDialogHost
import com.jhaiian.clint.ui.rememberMaxContentWidth
import com.jhaiian.clint.ui.theme.ClintComposeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShortcutSettingsActivity : ClintActivity() {

    private var uiState: ShortcutSettingsUiState? = null

    private val iconPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) { decodeIcon(uri) }
            if (bitmap != null) uiState?.customIcon = bitmap
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val shortcutId = intent.getStringExtra(EXTRA_SHORTCUT_ID)
        val shortcut = shortcutId?.let { ShortcutStore.get(this, it) }
        if (shortcut == null) {
            finish()
            return
        }
        val state = ShortcutSettingsUiState(shortcut)
        uiState = state

        onBackPressedDispatcher.addCallback(this) { requestExit(state) }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("app_theme", "system") ?: "system"
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)

        setContent {
            ClintComposeTheme(theme = theme) {
                val maxContentWidth = rememberMaxContentWidth(this@ShortcutSettingsActivity)
                ShortcutSettingsScreen(
                    state = state,
                    maxContentWidth = maxContentWidth,
                    hideStatusBar = hideStatusBar,
                    hideSystemNavigation = hideSystemNavigation,
                    onExit = { requestExit(state) },
                    onPickIcon = {
                        iconPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onSave = { save(state) }
                )
                ConfirmDialogHost(state.confirmDialog, hideStatusBar, hideSystemNavigation) { state.confirmDialog = null }
            }
        }
    }

    private fun requestExit(state: ShortcutSettingsUiState) {
        if (!state.isDirty) {
            finish()
            return
        }
        state.confirmDialog = ConfirmDialogConfig(
            title = getString(R.string.shortcut_manager_unsaved_title),
            message = getString(R.string.shortcut_manager_unsaved_message),
            neutralLabel = getString(R.string.action_cancel),
            negativeLabel = getString(R.string.shortcut_manager_unsaved_discard),
            onNegative = { finish() },
            positiveLabel = getString(R.string.action_save),
            onPositive = { save(state) }
        )
    }

    private fun save(state: ShortcutSettingsUiState) {
        if (!state.canSave) return
        val shortcut = state.shortcut
        val newUrl = if (state.url.trim() == shortcut.url) shortcut.url else state.normalizedUrl ?: return
        val newIcon = state.customIcon
        val iconPath = if (newIcon != null) ShortcutIconStore.save(this, shortcut.id, newIcon) else shortcut.iconPath
        val updated = shortcut.copy(name = state.name.trim(), url = newUrl, iconPath = iconPath, frameless = state.frameless, profileId = state.profileId)
        ShortcutStore.update(this, updated)
        if (newUrl != shortcut.url) {
            ShortcutTabSessionManager.clear(this, shortcut.id)
            ShortcutStore.markUrlChanged(shortcut.id)
        }
        updatePinnedShortcut(updated, newIcon)
        finish()
    }

    private fun updatePinnedShortcut(shortcut: WebAppShortcut, icon: Bitmap?) {
        runCatching {
            val shortcutIntent = Intent(Intent.ACTION_VIEW, Uri.parse(shortcut.url)).apply {
                setClass(this@ShortcutSettingsActivity, MainActivity::class.java)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(MainActivity.EXTRA_SHORTCUT_ID, shortcut.id)
            }
            val builder = ShortcutInfoCompat.Builder(this, shortcut.id)
                .setShortLabel(shortcut.name)
                .setLongLabel(shortcut.name)
                .setIntent(shortcutIntent)
            if (icon != null) builder.setIcon(IconCompat.createWithBitmap(icon))
            ShortcutManagerCompat.updateShortcuts(this, listOf(builder.build()))
        }
    }

    private fun decodeIcon(uri: Uri): Bitmap? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= ICON_SIZE && bounds.outHeight / (sample * 2) >= ICON_SIZE) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val decoded = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return@runCatching null
            val side = minOf(decoded.width, decoded.height)
            val cropped = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side)
            Bitmap.createScaledBitmap(cropped, ICON_SIZE, ICON_SIZE, true)
        }.getOrNull()
    }

    companion object {
        const val EXTRA_SHORTCUT_ID = "extra_shortcut_id"
        private const val ICON_SIZE = 192
    }
}
