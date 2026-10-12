package com.jhaiian.clint.settings.shortcutmanager

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jhaiian.clint.shortcuts.WebAppShortcut
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig

class ShortcutSettingsUiState(val shortcut: WebAppShortcut) {
    var name by mutableStateOf(shortcut.name)
    var url by mutableStateOf(shortcut.url)
    var frameless by mutableStateOf(shortcut.frameless)
    var profileId by mutableStateOf(shortcut.profileId)
    var customIcon by mutableStateOf<Bitmap?>(null)
    var confirmDialog by mutableStateOf<ConfirmDialogConfig?>(null)

    val normalizedUrl: String?
        get() = normalizeShortcutUrl(url)

    val isUrlValid: Boolean
        get() = normalizedUrl != null

    val isDirty: Boolean
        get() = name.trim() != shortcut.name ||
            url.trim() != shortcut.url ||
            frameless != shortcut.frameless ||
            profileId != shortcut.profileId ||
            customIcon != null

    val canSave: Boolean
        get() = isDirty && name.isNotBlank() && isUrlValid
}

fun normalizeShortcutUrl(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return null
    val candidate = if (trimmed.contains("://")) trimmed else "https://$trimmed"
    val uri = runCatching { Uri.parse(candidate) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase()
    if (scheme != "http" && scheme != "https") return null
    val host = uri.host
    if (host.isNullOrBlank()) return null
    return if (host.contains('.') || host.equals("localhost", ignoreCase = true)) candidate else null
}
