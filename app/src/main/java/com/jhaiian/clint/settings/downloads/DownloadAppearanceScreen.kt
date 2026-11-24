package com.jhaiian.clint.settings.downloads

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.downloads.DownloadAppearance
import com.jhaiian.clint.downloads.DownloadAppearanceKeys
import com.jhaiian.clint.downloads.DownloadCard
import com.jhaiian.clint.downloads.DownloadDensity
import com.jhaiian.clint.downloads.DownloadItem
import com.jhaiian.clint.downloads.DownloadNotificationHelper
import com.jhaiian.clint.downloads.DownloadProgressStyle
import com.jhaiian.clint.downloads.DownloadSortKey
import com.jhaiian.clint.downloads.DownloadTapAction
import com.jhaiian.clint.downloads.FilterChipPill
import com.jhaiian.clint.downloads.LocalDownloadAppearance
import com.jhaiian.clint.downloads.SheetActionId
import com.jhaiian.clint.downloads.defaultSortOrder
import com.jhaiian.clint.settings.SettingsActivity
import com.jhaiian.clint.settings.browser.HomepageChoiceDialog
import com.jhaiian.clint.settings.browser.HomepageOrderDialog
import com.jhaiian.clint.settings.common.RowDivider
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.settings.common.SettingsSectionLabel
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.theme.LocalClintColors
import com.jhaiian.clint.util.formatFileSize

private const val DIALOG_PROGRESS = "progress"
private const val DIALOG_DENSITY = "density"
private const val DIALOG_SORT = "sort"
private const val DIALOG_TAP = "tap"
private const val DIALOG_SHEET = "sheet"

private const val PREVIEW_TOTAL_BYTES = 512L * 1024 * 1024
private const val PREVIEW_DONE_BYTES = 187L * 1024 * 1024
private const val PREVIEW_SPEED_BYTES = 4_400_000L

@Composable
private fun previewItem(): DownloadItem = DownloadItem(
    id = -1,
    url = "https://example.com/video.mp4",
    filename = stringResource(R.string.download_appearance_sample_name),
    userAgent = "",
    bytesDownloaded = PREVIEW_DONE_BYTES,
    totalBytes = PREVIEW_TOTAL_BYTES,
    speedBytesPerSec = PREVIEW_SPEED_BYTES,
    resumable = true,
    activeElapsedMs = 48_000L,
    multithreadingParts = 8,
    partProgress = listOf(1f, 1f, 0.8f, 0.55f, 0.3f, 0.1f, 0f, 0f)
)

@Composable
private fun NotificationPreview(appearance: DownloadAppearance) {
    val colors = LocalClintColors.current
    val speedText = "${formatFileSize(PREVIEW_SPEED_BYTES)}/s"
    val statusText = stringResource(
        R.string.download_status_progress, 36, formatFileSize(PREVIEW_DONE_BYTES), formatFileSize(PREVIEW_TOTAL_BYTES)
    )
    val contentText = if (appearance.notifSpeed) "$statusText  •  $speedText" else statusText
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(colors.primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Download, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(12.dp))
            }
            Text(
                stringResource(R.string.app_name),
                color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp)
            )
        }
        Text(
            stringResource(R.string.download_appearance_sample_name),
            color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(contentText, color = colors.secondaryText, fontSize = 13.sp, lineHeight = 18.sp)
        if (appearance.notifProgress) {
            LinearProgressIndicator(
                progress = { 0.36f },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(4.dp).clip(CircleShape),
                color = colors.primary,
                trackColor = colors.primary.copy(alpha = 0.18f)
            )
        }
        if (appearance.notifActions) {
            Text(
                stringResource(R.string.action_pause),
                color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@Composable
private fun DownloadPreviewCard(appearance: DownloadAppearance) {
    val colors = LocalClintColors.current
    var showNotification by rememberSaveable { mutableStateOf(false) }
    val tallScreen = LocalConfiguration.current.screenHeightDp >= 700
    var manualVisible by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val visible = manualVisible ?: tallScreen
    Surface(color = colors.cardBackground, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.download_appearance_preview_title),
                        color = colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(R.string.download_appearance_preview_hint),
                        color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp)
                    )
                }
                IconButton(onClick = { manualVisible = !visible }) {
                    Icon(
                        if (visible) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(R.string.download_appearance_preview_toggle),
                        tint = colors.primary
                    )
                }
            }
            if (visible) {
                Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChipPill(stringResource(R.string.download_appearance_preview_card), null, Icons.Filled.ViewAgenda, !showNotification) { showNotification = false }
                    FilterChipPill(stringResource(R.string.download_appearance_preview_notification), null, Icons.Filled.Notifications, showNotification) { showNotification = true }
                }
                CompositionLocalProvider(LocalDownloadAppearance provides appearance) {
                    if (showNotification) {
                        NotificationPreview(appearance)
                    } else {
                        DownloadCard(
                            item = previewItem(),
                            tick = 0L,
                            expanded = false,
                            isSelected = false,
                            isInSelectionMode = true,
                            history = emptyList(),
                            onClick = {},
                            onLongClick = {},
                            onToggleExpand = {},
                            onPause = {},
                            onResume = {},
                            onRetry = {},
                            onMore = {},
                            onChangeSettings = {},
                            onRemove = {}
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadAppearancePane(activity: SettingsActivity) {
    val colors = LocalClintColors.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(activity) }
    val hideStatusBar = remember { prefs.getBoolean("hide_status_bar", false) }
    val hideSystemNavigation = remember { prefs.getBoolean("hide_system_navigation", false) }
    var version by remember { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<String?>(null) }
    val appearance = remember(version) { DownloadAppearance.read(prefs) }
    val liveAllowed = remember(version) { DownloadNotificationHelper.canPostLiveUpdates(activity) }
    val pushEnabled = remember(version) {
        prefs.getBoolean(DownloadSettingsKeys.PREF_PUSH_NOTIFICATIONS, DownloadSettingsKeys.DEFAULT_PUSH_NOTIFICATIONS)
    }

    if (DownloadNotificationHelper.supportsLiveUpdates()) {
        DisposableEffect(activity) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) version++
            }
            activity.lifecycle.addObserver(observer)
            onDispose { activity.lifecycle.removeObserver(observer) }
        }
    }

    fun save(block: SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
        version++
        dialog = null
    }

    fun flip(key: String, current: Boolean) = save { putBoolean(key, !current) }

    val progressOptions = listOf(
        DownloadProgressStyle.RING.name to stringResource(R.string.download_appearance_progress_ring),
        DownloadProgressStyle.BAR.name to stringResource(R.string.download_appearance_progress_bar)
    )
    val densityOptions = listOf(
        DownloadDensity.COMPACT.name to stringResource(R.string.download_appearance_density_compact),
        DownloadDensity.COMFORTABLE.name to stringResource(R.string.download_appearance_density_comfortable),
        DownloadDensity.SPACIOUS.name to stringResource(R.string.download_appearance_density_spacious)
    )
    val sortOptions = listOf(
        DownloadSortKey.DATE.name to stringResource(R.string.downloads_sort_by_date),
        DownloadSortKey.NAME.name to stringResource(R.string.downloads_sort_by_name),
        DownloadSortKey.SIZE.name to stringResource(R.string.downloads_sort_by_size),
        DownloadSortKey.STATUS.name to stringResource(R.string.downloads_sort_by_status)
    )
    val tapOptions = listOf(
        DownloadTapAction.OPEN.name to stringResource(R.string.download_appearance_tap_open),
        DownloadTapAction.SHEET.name to stringResource(R.string.download_appearance_tap_details),
        DownloadTapAction.SHARE.name to stringResource(R.string.download_appearance_tap_share)
    )

    fun label(options: List<Pair<String, String>>, key: String) = options.firstOrNull { it.first == key }?.second.orEmpty()

    SettingsScreenScaffold(
        header = { DownloadPreviewCard(appearance) },
        overlay = {
            when (dialog) {
                DIALOG_PROGRESS -> HomepageChoiceDialog(
                    title = stringResource(R.string.download_appearance_progress_title), options = progressOptions, current = appearance.progressStyle.name,
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { picked -> save { putString(DownloadAppearanceKeys.PROGRESS_STYLE, picked) } },
                    onDismiss = { dialog = null }
                )
                DIALOG_DENSITY -> HomepageChoiceDialog(
                    title = stringResource(R.string.download_appearance_density_title), options = densityOptions, current = appearance.density.name,
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { picked -> save { putString(DownloadAppearanceKeys.DENSITY, picked) } },
                    onDismiss = { dialog = null }
                )
                DIALOG_SORT -> HomepageChoiceDialog(
                    title = stringResource(R.string.download_appearance_sort_title), options = sortOptions, current = appearance.sortKey.name,
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { picked ->
                        save {
                            putString(DownloadAppearanceKeys.SORT_KEY, picked)
                            putString(DownloadAppearanceKeys.SORT_ORDER, defaultSortOrder(DownloadSortKey.valueOf(picked)).name)
                        }
                    },
                    onDismiss = { dialog = null }
                )
                DIALOG_TAP -> HomepageChoiceDialog(
                    title = stringResource(R.string.download_appearance_tap_title), options = tapOptions, current = appearance.tapAction.name,
                    hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                    onConfirm = { picked -> save { putString(DownloadAppearanceKeys.TAP_ACTION, picked) } },
                    onDismiss = { dialog = null }
                )
                DIALOG_SHEET -> {
                    val shown = appearance.sheetActions.map { it.name }
                    HomepageOrderDialog(
                        title = stringResource(R.string.download_appearance_sheet_title),
                        keys = shown + SheetActionId.entries.map { it.name }.filter { it !in shown },
                        enabled = shown.toSet(),
                        label = { stringResource(SheetActionId.valueOf(it).label) },
                        icon = { SheetActionId.valueOf(it).icon },
                        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                        onConfirm = { order, checked ->
                            save { putString(DownloadAppearanceKeys.SHEET_ACTIONS, order.filter { it in checked }.joinToString(",")) }
                        },
                        onDismiss = { dialog = null }
                    )
                }
            }
        }
    ) {
        SettingsSectionLabel(stringResource(R.string.download_appearance_section_layout))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = Icons.Filled.DonutLarge,
                title = stringResource(R.string.download_appearance_progress_title),
                summary = label(progressOptions, appearance.progressStyle.name),
                colors = colors,
                onClick = { dialog = DIALOG_PROGRESS }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.LinearScale,
                title = stringResource(R.string.download_appearance_density_title),
                summary = label(densityOptions, appearance.density.name),
                colors = colors,
                onClick = { dialog = DIALOG_DENSITY }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.Image,
                title = stringResource(R.string.download_appearance_thumbnail_title),
                summary = stringResource(R.string.download_appearance_thumbnail_summary),
                colors = colors,
                onClick = { flip(DownloadAppearanceKeys.SHOW_THUMBNAIL, appearance.showThumbnail) },
                trailing = { ClintSwitch(checked = appearance.showThumbnail) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.Bolt,
                title = stringResource(R.string.download_appearance_speed_title),
                summary = stringResource(R.string.download_appearance_speed_summary),
                colors = colors,
                onClick = { flip(DownloadAppearanceKeys.SHOW_SPEED, appearance.showSpeed) },
                trailing = { ClintSwitch(checked = appearance.showSpeed) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.Schedule,
                title = stringResource(R.string.download_appearance_eta_title),
                summary = stringResource(R.string.download_appearance_eta_summary),
                colors = colors,
                onClick = { flip(DownloadAppearanceKeys.SHOW_ETA, appearance.showEta) },
                trailing = { ClintSwitch(checked = appearance.showEta) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.Language,
                title = stringResource(R.string.download_appearance_source_title),
                summary = stringResource(R.string.download_appearance_source_summary),
                colors = colors,
                onClick = { flip(DownloadAppearanceKeys.SHOW_SOURCE, appearance.showSource) },
                trailing = { ClintSwitch(checked = appearance.showSource) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.Splitscreen,
                title = stringResource(R.string.download_appearance_parts_title),
                summary = stringResource(R.string.download_appearance_parts_summary),
                colors = colors,
                onClick = { flip(DownloadAppearanceKeys.SHOW_PARTS, appearance.showParts) },
                trailing = { ClintSwitch(checked = appearance.showParts) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.Speed,
                title = stringResource(R.string.download_appearance_live_stats_title),
                summary = stringResource(R.string.download_appearance_live_stats_summary),
                colors = colors,
                onClick = { flip(DownloadAppearanceKeys.SHOW_LIVE_STATS, appearance.showLiveStats) },
                trailing = { ClintSwitch(checked = appearance.showLiveStats) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.ShowChart,
                title = stringResource(R.string.download_appearance_item_graph_title),
                summary = stringResource(R.string.download_appearance_item_graph_summary),
                colors = colors,
                onClick = { flip(DownloadAppearanceKeys.SHOW_ITEM_GRAPH, appearance.showItemGraph) },
                trailing = { ClintSwitch(checked = appearance.showItemGraph) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.ShowChart,
                title = stringResource(R.string.download_appearance_total_graph_title),
                summary = stringResource(R.string.download_appearance_total_graph_summary),
                colors = colors,
                onClick = { flip(DownloadAppearanceKeys.SHOW_TOTAL_GRAPH, appearance.showTotalGraph) },
                trailing = { ClintSwitch(checked = appearance.showTotalGraph) }
            )
        }

        SettingsSectionLabel(stringResource(R.string.download_appearance_section_library))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.Sort,
                title = stringResource(R.string.download_appearance_sort_title),
                summary = label(sortOptions, appearance.sortKey.name),
                colors = colors,
                onClick = { dialog = DIALOG_SORT }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.TouchApp,
                title = stringResource(R.string.download_appearance_tap_title),
                summary = label(tapOptions, appearance.tapAction.name),
                colors = colors,
                onClick = { dialog = DIALOG_TAP }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.Tune,
                title = stringResource(R.string.download_appearance_sheet_title),
                summary = stringResource(R.string.download_appearance_sheet_summary, appearance.sheetActions.size, SheetActionId.entries.size),
                colors = colors,
                onClick = { dialog = DIALOG_SHEET }
            )
        }

        SettingsSectionLabel(stringResource(R.string.download_section_notifications))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = Icons.Filled.Notifications,
                title = stringResource(R.string.download_push_notifications_title),
                summary = stringResource(R.string.download_push_notifications_summary),
                colors = colors,
                onClick = { flip(DownloadSettingsKeys.PREF_PUSH_NOTIFICATIONS, pushEnabled) },
                trailing = { ClintSwitch(checked = pushEnabled) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.SmartButton,
                title = stringResource(R.string.download_appearance_notif_actions_title),
                summary = stringResource(R.string.download_appearance_notif_actions_summary),
                colors = colors,
                onClick = { flip(DownloadAppearanceKeys.NOTIF_ACTIONS, appearance.notifActions) },
                trailing = { ClintSwitch(checked = appearance.notifActions) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.ShowChart,
                title = stringResource(R.string.download_appearance_notif_speed_title),
                summary = stringResource(R.string.download_appearance_notif_speed_summary),
                colors = colors,
                onClick = { flip(DownloadAppearanceKeys.NOTIF_SPEED, appearance.notifSpeed) },
                trailing = { ClintSwitch(checked = appearance.notifSpeed) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.LinearScale,
                title = stringResource(R.string.download_appearance_notif_progress_title),
                summary = stringResource(R.string.download_appearance_notif_progress_summary),
                colors = colors,
                onClick = { flip(DownloadAppearanceKeys.NOTIF_PROGRESS, appearance.notifProgress) },
                trailing = { ClintSwitch(checked = appearance.notifProgress) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = Icons.Filled.NotificationsOff,
                title = stringResource(R.string.download_appearance_notif_quiet_title),
                summary = stringResource(R.string.download_appearance_notif_quiet_summary),
                colors = colors,
                onClick = { flip(DownloadAppearanceKeys.NOTIF_QUIET_COMPLETE, appearance.notifQuietComplete) },
                trailing = { ClintSwitch(checked = appearance.notifQuietComplete) }
            )
            if (DownloadNotificationHelper.supportsLiveUpdates()) {
                RowDivider(colors.divider)
                SettingsRow(
                    icon = Icons.Filled.Bolt,
                    title = stringResource(R.string.download_appearance_notif_live_title),
                    summary = stringResource(R.string.download_appearance_notif_live_summary),
                    colors = colors,
                    onClick = { flip(DownloadAppearanceKeys.NOTIF_LIVE_UPDATE, appearance.notifLiveUpdate) },
                    trailing = { ClintSwitch(checked = appearance.notifLiveUpdate) }
                )
                if (appearance.notifLiveUpdate && !liveAllowed) {
                    RowDivider(colors.divider)
                    SettingsRow(
                        icon = Icons.Filled.Notifications,
                        title = stringResource(R.string.download_appearance_notif_live_blocked_title),
                        summary = stringResource(R.string.download_appearance_notif_live_blocked_summary),
                        colors = colors,
                        onClick = { openLiveUpdateSettings(activity) }
                    )
                }
            }
        }
    }
}

private fun openLiveUpdateSettings(context: Context) {
    val promoted = Intent("android.settings.MANAGE_APP_PROMOTED_NOTIFICATIONS")
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    val general = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    try {
        context.startActivity(promoted)
    } catch (_: Exception) {
        try {
            context.startActivity(general)
        } catch (_: Exception) {
        }
    }
}
