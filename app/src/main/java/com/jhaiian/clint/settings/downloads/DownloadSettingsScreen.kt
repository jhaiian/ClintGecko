package com.jhaiian.clint.settings.downloads
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.ClintSlider
import com.jhaiian.clint.ui.ClintSwitch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.downloads.DEFAULT_DOWNLOAD_PATH
import com.jhaiian.clint.downloads.SPEED_LIMIT_UNIT_MB
import com.jhaiian.clint.downloads.resolveStorageInfoText
import com.jhaiian.clint.downloads.uriToDisplayPath
import com.jhaiian.clint.settings.common.RowDivider
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.settings.common.settingsSearchHighlight
import com.jhaiian.clint.setup.SectionLabel
import com.jhaiian.clint.ui.theme.ClintColors
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun DownloadSettingsScreen(
    state: DownloadSettingsUiState,
    onDownloadManagerSelected: (String) -> Unit,
    onLocationModeSelected: (String) -> Unit,
    onFolderRowClick: () -> Unit,
    onCategorizeDownloadsClick: () -> Unit,
    onMeasurementSystemSelected: (Boolean) -> Unit,
    onUnmeteredOnlyClick: () -> Unit,
    onScheduleEnabledClick: () -> Unit,
    onScheduleStartClick: () -> Unit,
    onScheduleEndClick: () -> Unit,
    onConcurrentDownloadsChange: (Int) -> Unit,
    onSplitPartsChange: (Int) -> Unit,
    onMultithreadingPartsChange: (Int) -> Unit,
    onConcurrentSegmentsChange: (Int) -> Unit,
    onSpeedLimitConfirm: (amount: Int, unit: String) -> Unit,
    onRetryEnabledClick: () -> Unit,
    onRetryUnrecoverableClick: () -> Unit,
    onRetryCountConfirm: (Int) -> Unit,
    onRetryIntervalConfirm: (Int) -> Unit,
    onIgnoreBatteryOptClick: () -> Unit,
    onGrantAllFilesAccessClick: () -> Unit,
    onPushNotificationsClick: () -> Unit,
    onKeepScreenOnClick: () -> Unit,
    onQuickDownloadClick: () -> Unit,
    onQuickDownloadImagesClick: () -> Unit,
    onDialogUiSelected: (String) -> Unit
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current

    val tapToChooseText = stringResource(R.string.download_location_tap_to_choose)
    val folderPathText = remember(state.locationMode, state.customUri, tapToChooseText) {
        when {
            state.locationMode != DownloadSettingsKeys.MODE_CUSTOM -> DEFAULT_DOWNLOAD_PATH
            state.customUri != null -> uriToDisplayPath(state.customUri!!)
            else -> tapToChooseText
        }
    }
    val storageInfoText = remember(state.locationMode, state.customUri, state.measurementSystemDecimal) {
        resolveStorageInfoText(context, state.locationMode, state.customUri)
    }
    val scheduleStartText = remember(state.scheduleStartMinutes) { formatMinutesOfDay(context, state.scheduleStartMinutes) }
    val scheduleEndText = remember(state.scheduleEndMinutes) { formatMinutesOfDay(context, state.scheduleEndMinutes) }

    SettingsScreenScaffold(
        overlay = {
            when (state.openDialog) {
                DownloadSettingsDialog.MEASUREMENT_SYSTEM -> MeasurementSystemDialog(
                    current = state.measurementSystemDecimal, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onSelect = onMeasurementSystemSelected, onDismiss = { state.openDialog = null }
                )
                DownloadSettingsDialog.RETRY_COUNT -> RetryCountDialog(
                    current = state.retryCount, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onConfirm = onRetryCountConfirm, onDismiss = { state.openDialog = null }
                )
                DownloadSettingsDialog.RETRY_INTERVAL -> RetryIntervalDialog(
                    current = state.retryInterval, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onConfirm = onRetryIntervalConfirm, onDismiss = { state.openDialog = null }
                )
                DownloadSettingsDialog.SPEED_LIMIT -> SpeedLimitDialog(
                    currentAmount = state.speedLimitAmount, currentUnit = state.speedLimitUnit, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onConfirm = onSpeedLimitConfirm, onDismiss = { state.openDialog = null }
                )
                DownloadSettingsDialog.DOWNLOAD_MANAGER -> DownloadManagerDialog(
                    current = state.downloadManagerApp, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onSelect = onDownloadManagerSelected, onDismiss = { state.openDialog = null }
                )
                DownloadSettingsDialog.DIALOG_UI -> DownloadDialogUiDialog(
                    current = state.dialogUi, hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onSelect = onDialogUiSelected, onDismiss = { state.openDialog = null }
                )
                null -> {}
            }
        }
    ) {
        SectionLabel(stringResource(R.string.download_section_manager), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Apps,
                title = stringResource(R.string.download_manager_title),
                summary = stringResource(downloadManagerOptionLabelRes(state.downloadManagerApp)),
                colors = colors,
                onClick = { state.openDialog = DownloadSettingsDialog.DOWNLOAD_MANAGER }
            )
        }

        SectionLabel(stringResource(R.string.download_section_location), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            Column(Modifier.padding(16.dp)) {
                DownloadLocationDropdown(mode = state.locationMode, colors = colors, onModeSelected = onLocationModeSelected)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .alpha(if (state.locationMode == DownloadSettingsKeys.MODE_CUSTOM) 1f else 0.4f)
                        .clickable(enabled = state.locationMode == DownloadSettingsKeys.MODE_CUSTOM, onClick = onFolderRowClick)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Folder, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(20.dp))
                    Text(
                        folderPathText, color = colors.onSurface, fontSize = 14.sp, fontFamily = FontFamily.Monospace,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 12.dp)
                    )
                }
                Text(storageInfoText, color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
            }
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Category,
                title = stringResource(R.string.download_categorize_title),
                summary = stringResource(R.string.download_categorize_summary),
                colors = colors,
                onClick = onCategorizeDownloadsClick,
                trailing = { ClintSwitch(checked = state.categorizeDownloads) }
            )
        }

        SectionLabel(stringResource(R.string.download_section_measurement), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.FormatSize,
                title = stringResource(R.string.measurement_system_title),
                summary = stringResource(if (state.measurementSystemDecimal) R.string.measurement_system_summary_decimal else R.string.measurement_system_summary_binary),
                colors = colors,
                onClick = { state.openDialog = DownloadSettingsDialog.MEASUREMENT_SYSTEM }
            )
        }

        SectionLabel(stringResource(R.string.download_section_network), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Wifi,
                title = stringResource(R.string.download_unmetered_only_title),
                summary = stringResource(R.string.download_unmetered_only_summary),
                colors = colors,
                onClick = onUnmeteredOnlyClick,
                trailing = { ClintSwitch(checked = state.unmeteredOnly) }
            )
        }

        SectionLabel(stringResource(R.string.download_section_scheduling), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.DateRange,
                title = stringResource(R.string.download_schedule_enabled_title),
                summary = stringResource(R.string.download_schedule_enabled_summary, scheduleStartText, scheduleEndText),
                colors = colors,
                onClick = onScheduleEnabledClick,
                trailing = { ClintSwitch(checked = state.scheduleEnabled) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.HourglassEmpty,
                title = stringResource(R.string.download_schedule_start_title),
                summary = scheduleStartText,
                colors = colors,
                enabled = state.scheduleEnabled,
                onClick = { if (state.scheduleEnabled) onScheduleStartClick() }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.HourglassEmpty,
                title = stringResource(R.string.download_schedule_end_title),
                summary = scheduleEndText,
                colors = colors,
                enabled = state.scheduleEnabled,
                onClick = { if (state.scheduleEnabled) onScheduleEndClick() }
            )
        }

        SectionLabel(stringResource(R.string.download_section_concurrent), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SliderSettingsCard(
                icon = androidx.compose.material.icons.Icons.Filled.HourglassBottom,
                title = stringResource(R.string.download_concurrent_title),
                description = stringResource(R.string.download_concurrent_desc),
                value = state.concurrentDownloads,
                valueRange = 1..24,
                summary = if (state.concurrentDownloads == 24) {
                    stringResource(R.string.download_concurrent_value_easter_egg)
                } else {
                    pluralStringResource(R.plurals.download_concurrent_value, state.concurrentDownloads, state.concurrentDownloads)
                },
                colors = colors,
                onValueChange = onConcurrentDownloadsChange
            )
        }

        SectionLabel(stringResource(R.string.download_section_split_parts), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SliderSettingsCard(
                icon = androidx.compose.material.icons.Icons.Filled.Splitscreen,
                title = stringResource(R.string.download_split_parts_title),
                description = stringResource(R.string.download_split_parts_desc),
                value = state.splitParts,
                valueRange = 1..32,
                summary = pluralStringResource(R.plurals.download_split_parts_value, state.splitParts, state.splitParts),
                colors = colors,
                onValueChange = onSplitPartsChange
            )
        }

        SectionLabel(stringResource(R.string.download_section_multithreading), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SliderSettingsCard(
                icon = androidx.compose.material.icons.Icons.Filled.KeyboardDoubleArrowDown,
                title = stringResource(R.string.download_multithreading_title),
                description = stringResource(R.string.download_multithreading_desc),
                value = state.multithreadingParts,
                valueRange = 1..8,
                summary = pluralStringResource(R.plurals.download_multithreading_value, state.multithreadingParts, state.multithreadingParts),
                colors = colors,
                onValueChange = onMultithreadingPartsChange
            )
        }

        SectionLabel(stringResource(R.string.download_section_stream_segments), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SliderSettingsCard(
                icon = androidx.compose.material.icons.Icons.Filled.VideoSettings,
                title = stringResource(R.string.download_concurrent_segments_title),
                description = stringResource(R.string.download_concurrent_segments_desc),
                value = state.concurrentSegments,
                valueRange = 1..8,
                summary = pluralStringResource(R.plurals.download_concurrent_segments_value, state.concurrentSegments, state.concurrentSegments),
                colors = colors,
                onValueChange = onConcurrentSegmentsChange
            )
        }

        SectionLabel(stringResource(R.string.download_section_speed_limit), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            val unitLabel = stringResource(if (state.speedLimitUnit == SPEED_LIMIT_UNIT_MB) R.string.speed_limit_unit_mb else R.string.speed_limit_unit_kb)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Speed,
                title = stringResource(R.string.download_speed_limit_title),
                summary = if (state.speedLimitAmount <= 0) {
                    stringResource(R.string.download_speed_limit_unlimited)
                } else {
                    stringResource(R.string.download_speed_limit_value, state.speedLimitAmount, unitLabel)
                },
                colors = colors,
                onClick = { state.openDialog = DownloadSettingsDialog.SPEED_LIMIT }
            )
        }

        SectionLabel(stringResource(R.string.download_section_retry), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.RestartAlt,
                title = stringResource(R.string.download_retry_enabled_title),
                summary = stringResource(R.string.download_retry_enabled_summary),
                colors = colors,
                onClick = onRetryEnabledClick,
                trailing = { ClintSwitch(checked = state.retryEnabled) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Storage,
                title = stringResource(R.string.download_retry_unrecoverable_title),
                summary = stringResource(R.string.download_retry_unrecoverable_summary),
                colors = colors,
                enabled = state.retryEnabled,
                onClick = { if (state.retryEnabled) onRetryUnrecoverableClick() },
                trailing = { ClintSwitch(checked = state.retryUnrecoverable) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Numbers,
                title = stringResource(R.string.download_retry_count_title),
                summary = if (state.retryCount == 0) {
                    stringResource(R.string.download_retry_count_unlimited)
                } else {
                    stringResource(R.string.download_retry_count_value, state.retryCount)
                },
                colors = colors,
                enabled = state.retryEnabled,
                onClick = { if (state.retryEnabled) state.openDialog = DownloadSettingsDialog.RETRY_COUNT }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.HourglassEmpty,
                title = stringResource(R.string.download_retry_interval_title),
                summary = stringResource(R.string.download_retry_interval_value, state.retryInterval),
                colors = colors,
                enabled = state.retryEnabled,
                onClick = { if (state.retryEnabled) state.openDialog = DownloadSettingsDialog.RETRY_INTERVAL }
            )
        }

        SectionLabel(stringResource(R.string.download_section_general), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.FlashOn,
                title = stringResource(R.string.download_quick_download_title),
                summary = stringResource(R.string.download_quick_download_summary),
                colors = colors,
                onClick = onQuickDownloadClick,
                trailing = { ClintSwitch(checked = state.quickDownload) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Image,
                title = stringResource(R.string.download_quick_download_images_title),
                summary = stringResource(R.string.download_quick_download_images_summary),
                colors = colors,
                enabled = !state.quickDownload,
                onClick = { if (!state.quickDownload) onQuickDownloadImagesClick() },
                trailing = { ClintSwitch(checked = state.quickDownloadImages && !state.quickDownload) }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Visibility,
                title = stringResource(R.string.download_keep_screen_on_title),
                summary = stringResource(R.string.download_keep_screen_on_summary),
                colors = colors,
                onClick = onKeepScreenOnClick,
                trailing = { ClintSwitch(checked = state.keepScreenOn) }
            )
        }

        SectionLabel(stringResource(R.string.download_section_dialog_ui), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.ViewCompact,
                title = stringResource(R.string.download_dialog_ui_title),
                summary = stringResource(if (state.dialogUi == DownloadSettingsKeys.DIALOG_UI_COMPACT) R.string.download_dialog_ui_compact else R.string.download_dialog_ui_standard),
                colors = colors,
                onClick = { state.openDialog = DownloadSettingsDialog.DIALOG_UI }
            )
        }

        SectionLabel(stringResource(R.string.download_section_notifications), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Notifications,
                title = stringResource(R.string.download_push_notifications_title),
                summary = stringResource(R.string.download_push_notifications_summary),
                colors = colors,
                onClick = onPushNotificationsClick,
                trailing = { ClintSwitch(checked = state.pushNotifications) }
            )
        }

        SectionLabel(stringResource(R.string.download_section_permissions), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.BatterySaver,
                title = stringResource(R.string.download_ignore_battery_opt_title),
                summary = stringResource(if (state.ignoringBatteryOptimizations) R.string.download_ignore_battery_opt_granted else R.string.download_ignore_battery_opt_summary),
                colors = colors,
                enabled = !state.ignoringBatteryOptimizations,
                onClick = { if (!state.ignoringBatteryOptimizations) onIgnoreBatteryOptClick() }
            )
            if (state.showGrantAllFilesAccessRow) {
                RowDivider(colors.divider)
                SettingsRow(
                    icon = androidx.compose.material.icons.Icons.Filled.Folder,
                    title = stringResource(R.string.download_grant_all_files_access_title),
                    summary = stringResource(if (state.allFilesAccessGranted) R.string.download_grant_all_files_access_granted else R.string.download_grant_all_files_access_summary),
                    colors = colors,
                    enabled = !state.allFilesAccessGranted,
                    onClick = { if (!state.allFilesAccessGranted) onGrantAllFilesAccessClick() }
                )
            }
        }
    }
}

@Composable
private fun DownloadLocationDropdown(mode: String, colors: ClintColors, onModeSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var anchorWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val options = listOf(
        DownloadSettingsKeys.MODE_DEFAULT to stringResource(R.string.download_location_option_default),
        DownloadSettingsKeys.MODE_CUSTOM to stringResource(R.string.download_location_option_custom)
    )
    val selectedLabel = options.firstOrNull { it.first == mode }?.second ?: options[0].second

    Box(
        Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates -> anchorWidthPx = coordinates.size.width }
    ) {
        ClintOutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(stringResource(R.string.download_location_label)) },
            trailingIcon = { Icon(androidx.compose.material.icons.Icons.Filled.ArrowDownward, contentDescription = null, tint = colors.iconTint) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(4.dp))
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(with(density) { anchorWidthPx.toDp() }),
            shape = RoundedCornerShape(16.dp),
            containerColor = colors.popupBackground
        ) {
            options.forEach { (key, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { expanded = false; onModeSelected(key) })
            }
        }
    }
}

@Composable
private fun SliderSettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    value: Int,
    valueRange: IntRange,
    summary: String,
    colors: ClintColors,
    onValueChange: (Int) -> Unit
) {
    Column(Modifier.fillMaxWidth().settingsSearchHighlight(title, colors.primary).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(title, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(description, color = colors.secondaryText, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        ClintSlider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
            steps = (valueRange.last - valueRange.first - 1).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
        Text(summary, color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
    }
}

private fun downloadManagerOptionLabelRes(appId: String): Int = when (appId) {
    com.jhaiian.clint.downloads.DownloadManagerAppIds.ONEDM -> R.string.download_manager_option_1dm
    com.jhaiian.clint.downloads.DownloadManagerAppIds.ONEDM_PLUS -> R.string.download_manager_option_1dm_plus
    com.jhaiian.clint.downloads.DownloadManagerAppIds.ONEDM_LITE -> R.string.download_manager_option_1dm_lite
    com.jhaiian.clint.downloads.DownloadManagerAppIds.ADM -> R.string.download_manager_option_adm
    else -> R.string.download_manager_option_clint
}

private fun formatMinutesOfDay(context: android.content.Context, minutes: Int): String {
    val cal = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, minutes / 60)
        set(java.util.Calendar.MINUTE, minutes % 60)
    }
    return android.text.format.DateFormat.getTimeFormat(context).format(cal.time)
}
