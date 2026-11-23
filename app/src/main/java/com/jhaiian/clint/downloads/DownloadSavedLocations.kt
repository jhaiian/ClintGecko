package com.jhaiian.clint.downloads

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors
import org.json.JSONArray

object DownloadSavedLocations {

    fun list(context: Context): List<Uri> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val raw = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_SAVED_LOCATIONS, null)
        if (raw == null) {
            val current = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, null)?.takeIf { it.isNotBlank() } ?: return emptyList()
            val seeded = listOf(Uri.parse(current))
            write(context, seeded)
            return seeded
        }
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index -> array.optString(index).takeIf { it.isNotBlank() }?.let { Uri.parse(it) } }.distinct()
        }.getOrDefault(emptyList())
    }

    fun add(context: Context, uri: Uri): List<Uri> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val current = list(context)
        val updated = if (current.contains(uri)) current else current + uri
        val editor = prefs.edit().putString(DownloadSettingsKeys.PREF_DOWNLOAD_SAVED_LOCATIONS, toJson(updated))
        if (prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, null).isNullOrBlank()) {
            editor.putString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, uri.toString())
        }
        editor.apply()
        return updated
    }

    fun setDefault(context: Context, uri: Uri) {
        val updated = list(context).let { if (it.contains(uri)) it else it + uri }
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(DownloadSettingsKeys.PREF_DOWNLOAD_SAVED_LOCATIONS, toJson(updated))
            .putString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, uri.toString())
            .apply()
    }

    fun remove(context: Context, uri: Uri): Uri? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val updated = list(context).filterNot { it == uri }
        val editor = prefs.edit().putString(DownloadSettingsKeys.PREF_DOWNLOAD_SAVED_LOCATIONS, toJson(updated))
        var defaultUri = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, null)?.let { Uri.parse(it) }
        if (defaultUri == null || defaultUri == uri) {
            defaultUri = updated.firstOrNull()
            if (defaultUri != null) {
                editor.putString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, defaultUri.toString())
            } else {
                editor.remove(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI)
                    .putString(DownloadSettingsKeys.PREF_DOWNLOAD_LOCATION_MODE, DownloadSettingsKeys.MODE_DEFAULT)
            }
        }
        editor.apply()
        return defaultUri
    }

    private fun write(context: Context, uris: List<Uri>) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(DownloadSettingsKeys.PREF_DOWNLOAD_SAVED_LOCATIONS, toJson(uris))
            .apply()
    }

    private fun toJson(uris: List<Uri>): String = JSONArray().apply { uris.forEach { put(it.toString()) } }.toString()
}

@Composable
internal fun SavedLocationFolderRow(
    customUri: Uri?,
    compact: Boolean,
    onSelect: (Uri) -> Unit,
    onAddNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val density = LocalDensity.current
    var menuOpen by remember { mutableStateOf(false) }
    var anchorWidthPx by remember { mutableStateOf(0) }
    val saved = remember(menuOpen, customUri) { DownloadSavedLocations.list(context) }
    val iconSize = if (compact) 16.dp else 20.dp

    Box(modifier.fillMaxWidth().onGloballyPositioned { coordinates -> anchorWidthPx = coordinates.size.width }) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { if (saved.isEmpty()) onAddNew() else menuOpen = true }
                .padding(vertical = if (compact) 4.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(iconSize))
            Text(
                customUri?.let { uriToDisplayPath(it) } ?: stringResource(R.string.download_location_tap_to_choose),
                color = colors.onSurface, fontSize = if (compact) 12.sp else 13.sp,
                maxLines = 1, overflow = TextOverflow.MiddleEllipsis,
                modifier = Modifier.weight(1f).padding(start = if (compact) 8.dp else 10.dp)
            )
            if (saved.isNotEmpty()) {
                Icon(
                    Icons.Filled.ArrowDownward, contentDescription = null, tint = colors.iconTint,
                    modifier = Modifier.padding(start = 4.dp).size(iconSize)
                )
            }
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            modifier = Modifier.width(with(density) { anchorWidthPx.toDp() }),
            shape = PopupShape,
            containerColor = colors.popupBackground,
            border = BorderStroke(1.dp, colors.popupStroke)
        ) {
            saved.forEach { uri ->
                val isSelected = uri == customUri
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            if (isSelected) Icons.Filled.Check else Icons.Filled.Folder,
                            contentDescription = null,
                            tint = if (isSelected) colors.primary else colors.iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    text = {
                        Text(
                            uriToDisplayPath(uri),
                            color = if (isSelected) colors.primary else colors.onSurface,
                            fontSize = 13.sp, fontFamily = FontFamily.Monospace,
                            maxLines = 1, overflow = TextOverflow.MiddleEllipsis
                        )
                    },
                    onClick = { menuOpen = false; onSelect(uri) }
                )
            }
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp)) },
                text = { Text(stringResource(R.string.download_location_add_new), color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                onClick = { menuOpen = false; onAddNew() }
            )
        }
    }
}
