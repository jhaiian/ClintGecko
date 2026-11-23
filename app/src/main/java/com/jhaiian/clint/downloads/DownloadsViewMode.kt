package com.jhaiian.clint.downloads

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintRadioButton
import com.jhaiian.clint.ui.theme.LocalClintColors

enum class LibraryViewMode(val key: String, val label: Int) {
    GRID("grid", R.string.downloads_view_mode_grid),
    GRID_SMALL("grid_small", R.string.downloads_view_mode_grid_small),
    GRID_EXTRA_SMALL("grid_extra_small", R.string.downloads_view_mode_grid_extra_small),
    LIST_BIG("list_big", R.string.downloads_view_mode_list_big),
    LIST_LARGE("list_large", R.string.downloads_view_mode_list_large),
    LIST("list", R.string.downloads_view_mode_list),
    LIST_SMALL("list_small", R.string.downloads_view_mode_list_small),
    LIST_EXTRA_SMALL("list_extra_small", R.string.downloads_view_mode_list_extra_small),
    LIST_COMPACT("list_compact", R.string.downloads_view_mode_list_compact),
    LIST_NO_IMAGE("list_no_image", R.string.downloads_view_mode_list_no_image);

    val isGrid: Boolean get() = this == GRID || this == GRID_SMALL || this == GRID_EXTRA_SMALL

    companion object {
        val Default = GRID_SMALL

        fun fromKey(key: String?): LibraryViewMode = entries.firstOrNull { it.key == key } ?: Default
    }
}

internal object LibraryViewModeStore {
    fun load(prefs: SharedPreferences): LibraryViewMode {
        prefs.getString(DownloadSettingsKeys.PREF_LIBRARY_VIEW_MODE, null)?.let { return LibraryViewMode.fromKey(it) }
        val legacyGrid = prefs.getBoolean(DownloadSettingsKeys.PREF_LIBRARY_GRID, DownloadSettingsKeys.DEFAULT_LIBRARY_GRID)
        return if (legacyGrid) LibraryViewMode.GRID_SMALL else LibraryViewMode.LIST_SMALL
    }

    fun save(prefs: SharedPreferences, mode: LibraryViewMode) {
        prefs.edit().putString(DownloadSettingsKeys.PREF_LIBRARY_VIEW_MODE, mode.key).apply()
    }
}

@Composable
internal fun LibraryViewModeDialog(
    current: LibraryViewMode,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit,
    onSelect: (LibraryViewMode) -> Unit
) {
    val colors = LocalClintColors.current
    ClintDialog(
        title = stringResource(R.string.downloads_view_mode),
        hideStatusBar = hideStatusBar,
        hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss
    ) {
        LibraryViewMode.entries.forEach { mode ->
            val selected = mode == current
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) colors.primary.copy(alpha = 0.10f) else Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClintRadioButton(selected = selected)
                Text(
                    stringResource(mode.label),
                    color = colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}
