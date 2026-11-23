package com.jhaiian.clint.downloads

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.listscreen.ListSortOrder

enum class DownloadProgressStyle { RING, BAR }

enum class DownloadDensity(val cardPadding: Dp, val gap: Dp, val outer: Dp) {
    COMPACT(10.dp, 6.dp, 3.dp),
    COMFORTABLE(14.dp, 10.dp, 5.dp),
    SPACIOUS(18.dp, 14.dp, 8.dp)
}

enum class DownloadTapAction { OPEN, SHEET, SHARE }

enum class SheetActionId(@StringRes val label: Int, val icon: ImageVector) {
    FOLDER(R.string.downloads_sheet_folder, Icons.Filled.FolderOpen),
    RENAME(R.string.downloads_sheet_rename, Icons.Filled.Edit),
    DOWNLOAD_NEXT(R.string.downloads_sheet_download_next, Icons.Filled.VerticalAlignTop),
    SETTINGS(R.string.downloads_sheet_settings, Icons.Filled.Tune),
    UPDATE_LINK(R.string.downloads_sheet_update_link, Icons.Filled.Link),
    UPDATE_LINK_BROWSER(R.string.downloads_sheet_update_link_browser, Icons.Filled.Language),
    REDOWNLOAD(R.string.downloads_sheet_redownload, Icons.Filled.Refresh),
    REDOWNLOAD_CUSTOM(R.string.downloads_sheet_redownload_custom, Icons.Filled.Download),
    COPY_LINK(R.string.downloads_sheet_copy_link, Icons.Filled.Link),
    COPY_NAME(R.string.downloads_sheet_copy_name, Icons.Filled.ContentCopy),
    COPY_PATH(R.string.downloads_sheet_copy_path, Icons.Filled.ContentCopy),
    PROPERTIES(R.string.download_menu_properties, Icons.Filled.Info),
    LOG(R.string.download_log_view, Icons.Filled.BugReport)
}

object DownloadAppearanceKeys {
    const val PROGRESS_STYLE = "download_appearance_progress_style"
    const val DENSITY = "download_appearance_density"
    const val SHOW_THUMBNAIL = "download_appearance_show_thumbnail"
    const val SHOW_SPEED = "download_appearance_show_speed"
    const val SHOW_ETA = "download_appearance_show_eta"
    const val SHOW_SOURCE = "download_appearance_show_source"
    const val SHOW_PARTS = "download_appearance_show_parts"
    const val SHOW_LIVE_STATS = "download_appearance_show_live_stats"
    const val SHOW_ITEM_GRAPH = "download_appearance_show_item_graph"
    const val SHOW_TOTAL_GRAPH = "download_appearance_show_total_graph"
    const val TAP_ACTION = "download_appearance_tap_action"
    const val SHEET_ACTIONS = "download_appearance_sheet_actions"
    const val SORT_KEY = "download_appearance_sort_key"
    const val SORT_ORDER = "download_appearance_sort_order"
    const val NOTIF_ACTIONS = "download_notification_actions"
    const val NOTIF_SPEED = "download_notification_speed"
    const val NOTIF_PROGRESS = "download_notification_progress"
    const val NOTIF_QUIET_COMPLETE = "download_notification_quiet_complete"
    const val NOTIF_LIVE_UPDATE = "download_notification_live_update"
}

data class DownloadAppearance(
    val progressStyle: DownloadProgressStyle = DownloadProgressStyle.RING,
    val density: DownloadDensity = DownloadDensity.COMFORTABLE,
    val showThumbnail: Boolean = true,
    val showSpeed: Boolean = true,
    val showEta: Boolean = true,
    val showSource: Boolean = false,
    val showParts: Boolean = true,
    val showLiveStats: Boolean = true,
    val showItemGraph: Boolean = true,
    val showTotalGraph: Boolean = true,
    val tapAction: DownloadTapAction = DownloadTapAction.OPEN,
    val sheetActions: List<SheetActionId> = SheetActionId.entries,
    val sortKey: DownloadSortKey = DownloadSortKey.DATE,
    val sortOrder: ListSortOrder = defaultSortOrder(DownloadSortKey.DATE),
    val notifActions: Boolean = true,
    val notifSpeed: Boolean = true,
    val notifProgress: Boolean = true,
    val notifQuietComplete: Boolean = false,
    val notifLiveUpdate: Boolean = true
) {
    companion object {
        fun read(prefs: SharedPreferences): DownloadAppearance {
            val key = enumOf(prefs.getString(DownloadAppearanceKeys.SORT_KEY, null), DownloadSortKey.DATE)
            return DownloadAppearance(
                progressStyle = enumOf(prefs.getString(DownloadAppearanceKeys.PROGRESS_STYLE, null), DownloadProgressStyle.RING),
                density = enumOf(prefs.getString(DownloadAppearanceKeys.DENSITY, null), DownloadDensity.COMFORTABLE),
                showThumbnail = prefs.getBoolean(DownloadAppearanceKeys.SHOW_THUMBNAIL, true),
                showSpeed = prefs.getBoolean(DownloadAppearanceKeys.SHOW_SPEED, true),
                showEta = prefs.getBoolean(DownloadAppearanceKeys.SHOW_ETA, true),
                showSource = prefs.getBoolean(DownloadAppearanceKeys.SHOW_SOURCE, false),
                showParts = prefs.getBoolean(DownloadAppearanceKeys.SHOW_PARTS, true),
                showLiveStats = prefs.getBoolean(DownloadAppearanceKeys.SHOW_LIVE_STATS, true),
                showItemGraph = prefs.getBoolean(DownloadAppearanceKeys.SHOW_ITEM_GRAPH, true),
                showTotalGraph = prefs.getBoolean(DownloadAppearanceKeys.SHOW_TOTAL_GRAPH, true),
                tapAction = enumOf(prefs.getString(DownloadAppearanceKeys.TAP_ACTION, null), DownloadTapAction.OPEN),
                sheetActions = prefs.getString(DownloadAppearanceKeys.SHEET_ACTIONS, null)
                    ?.split(',')
                    ?.mapNotNull { id -> SheetActionId.entries.firstOrNull { it.name == id } }
                    ?: SheetActionId.entries,
                sortKey = key,
                sortOrder = enumOf(prefs.getString(DownloadAppearanceKeys.SORT_ORDER, null), defaultSortOrder(key)),
                notifActions = prefs.getBoolean(DownloadAppearanceKeys.NOTIF_ACTIONS, true),
                notifSpeed = prefs.getBoolean(DownloadAppearanceKeys.NOTIF_SPEED, true),
                notifProgress = prefs.getBoolean(DownloadAppearanceKeys.NOTIF_PROGRESS, true),
                notifQuietComplete = prefs.getBoolean(DownloadAppearanceKeys.NOTIF_QUIET_COMPLETE, false),
                notifLiveUpdate = prefs.getBoolean(DownloadAppearanceKeys.NOTIF_LIVE_UPDATE, true)
            )
        }

        fun saveSort(prefs: SharedPreferences, key: DownloadSortKey, order: ListSortOrder) {
            prefs.edit()
                .putString(DownloadAppearanceKeys.SORT_KEY, key.name)
                .putString(DownloadAppearanceKeys.SORT_ORDER, order.name)
                .apply()
        }
    }
}

fun defaultSortOrder(key: DownloadSortKey): ListSortOrder = when (key) {
    DownloadSortKey.NAME, DownloadSortKey.STATUS -> ListSortOrder.ASCENDING
    DownloadSortKey.DATE, DownloadSortKey.SIZE -> ListSortOrder.DESCENDING
}

private inline fun <reified T : Enum<T>> enumOf(name: String?, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default

val LocalDownloadAppearance = staticCompositionLocalOf { DownloadAppearance() }
