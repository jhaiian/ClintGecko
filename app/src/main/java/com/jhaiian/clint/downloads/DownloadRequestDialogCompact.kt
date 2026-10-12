package com.jhaiian.clint.downloads

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.RowDivider
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import com.jhaiian.clint.ui.ClintCompactTextField
import com.jhaiian.clint.ui.ClintScheduleDateTimeRows
import com.jhaiian.clint.ui.ClintSlider
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
internal fun CompactDownloadRequestContent(
    url: String,
    onCopyLink: () -> Unit,
    filename: String,
    onFilenameChange: (String) -> Unit,
    extension: String,
    onExtensionChange: (String) -> Unit,
    filenameFocusRequester: FocusRequester,
    fileSizeText: String,
    locationMode: String,
    onLocationModeSelected: (String) -> Unit,
    customUri: Uri?,
    onPickFolder: () -> Unit,
    showStorageInfo: Boolean,
    storageInfoText: String,
    categorizeEnabled: Boolean,
    onCategorizeToggle: () -> Unit,
    destinationPreviewText: String?,
    showOptions: Boolean,
    retryEnabled: Boolean,
    onRetryToggle: () -> Unit,
    unmeteredOnly: Boolean,
    onUnmeteredToggle: () -> Unit,
    showSplitAndMultithreading: Boolean,
    splitParts: Int,
    onSplitPartsChange: (Int) -> Unit,
    multithreadingParts: Int,
    onMultithreadingPartsChange: (Int) -> Unit,
    showConcurrentSegments: Boolean,
    concurrentSegments: Int,
    onConcurrentSegmentsChange: (Int) -> Unit,
    speedLimitText: String,
    onSpeedLimitTextChange: (String) -> Unit,
    speedUnitLabel: String,
    kbLabel: String,
    mbLabel: String,
    onSpeedUnitChange: (String) -> Unit,
    showSchedule: Boolean,
    scheduleEnabled: Boolean,
    onScheduleToggle: () -> Unit,
    scheduledMillis: Long,
    onScheduledMillisChange: (Long) -> Unit,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean
) {
    val colors = LocalClintColors.current

    Column(Modifier.padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Link,
                contentDescription = stringResource(R.string.download_dialog_link_clip_label),
                tint = colors.iconTint,
                modifier = Modifier.size(24.dp).clickable { onCopyLink() }.padding(4.dp)
            )
            Text(
                url, color = colors.secondaryText, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                maxLines = 1, overflow = TextOverflow.MiddleEllipsis,
                modifier = Modifier.weight(1f).padding(start = 6.dp)
            )
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ClintCompactTextField(
                value = filename, onValueChange = onFilenameChange,
                modifier = Modifier.weight(1f).focusRequester(filenameFocusRequester),
                placeholder = stringResource(R.string.download_dialog_filename_hint)
            )
            ClintCompactTextField(
                value = extension, onValueChange = onExtensionChange,
                modifier = Modifier.width(88.dp).padding(start = 8.dp),
                placeholder = stringResource(R.string.download_dialog_extension_hint)
            )
        }
        Text(fileSizeText, color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.padding(start = 2.dp, top = 3.dp))

        CompactDivider()

        CompactLocationRow(mode = locationMode, onModeSelected = onLocationModeSelected)
        if (locationMode == DownloadSettingsKeys.MODE_CUSTOM) {
            Row(
                Modifier.fillMaxWidth().clickable { onPickFolder() }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Folder, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(16.dp))
                Text(
                    customUri?.let { uriToDisplayPath(it) } ?: stringResource(R.string.download_location_tap_to_choose),
                    color = colors.onSurface, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.MiddleEllipsis,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }
        }
        if (showStorageInfo && storageInfoText.isNotEmpty()) {
            Text(storageInfoText, color = colors.secondaryText, fontSize = 10.sp, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
        }
        CompactToggleRow(stringResource(R.string.download_categorize_title), categorizeEnabled, onCategorizeToggle)
        destinationPreviewText?.let {
            Text(
                it, color = colors.secondaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                maxLines = 1, overflow = TextOverflow.MiddleEllipsis, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
            )
        }

        if (showOptions) {
            CompactDivider()

            CompactToggleRow(stringResource(R.string.download_retry_enabled_title), retryEnabled, onRetryToggle)
            CompactToggleRow(stringResource(R.string.download_unmetered_only_title), unmeteredOnly, onUnmeteredToggle)

            if (showSplitAndMultithreading) {
                CompactSliderRow(
                    title = stringResource(R.string.download_split_parts_title),
                    valueText = pluralStringResource(R.plurals.download_split_parts_value, splitParts, splitParts),
                    value = splitParts, range = 1..32, onChange = onSplitPartsChange
                )
                CompactSliderRow(
                    title = stringResource(R.string.download_multithreading_title),
                    valueText = pluralStringResource(R.plurals.download_multithreading_value, multithreadingParts, multithreadingParts),
                    value = multithreadingParts, range = 1..8, onChange = onMultithreadingPartsChange
                )
            }

            if (showConcurrentSegments) {
                CompactSliderRow(
                    title = stringResource(R.string.download_concurrent_segments_title),
                    valueText = pluralStringResource(R.plurals.download_concurrent_segments_value, concurrentSegments, concurrentSegments),
                    value = concurrentSegments, range = 1..8, onChange = onConcurrentSegmentsChange
                )
            }

            CompactSpeedLimitRow(
                text = speedLimitText, onTextChange = onSpeedLimitTextChange,
                unitLabel = speedUnitLabel, kbLabel = kbLabel, mbLabel = mbLabel, onUnitChange = onSpeedUnitChange
            )

            if (showSchedule) {
                CompactToggleRow(stringResource(R.string.download_schedule_this_title), scheduleEnabled, onScheduleToggle)
                if (scheduleEnabled) {
                    ClintScheduleDateTimeRows(
                        scheduledMillis = scheduledMillis,
                        onScheduledMillisChange = onScheduledMillisChange,
                        hideStatusBar = hideStatusBar,
                        hideSystemNavigation = hideSystemNavigation,
                        compact = true
                    )
                }
            }
        }
    }
}

@Composable
internal fun CompactDivider() {
    val colors = LocalClintColors.current
    Box(Modifier.padding(vertical = 6.dp)) { RowDivider(colors.divider) }
}

@Composable
internal fun CompactToggleRow(title: String, checked: Boolean, onToggle: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 32.dp).clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title, color = colors.onSurface, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Box(Modifier.size(width = 40.dp, height = 24.dp), contentAlignment = Alignment.Center) {
            ClintSwitch(checked = checked, modifier = Modifier.scale(0.75f))
        }
    }
}

@Composable
internal fun CompactSliderRow(title: String, valueText: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    val colors = LocalClintColors.current
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = colors.onSurface, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(valueText, color = colors.secondaryText, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
    }
    ClintSlider(
        value = value.toFloat(),
        onValueChange = { onChange(it.toInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        steps = (range.last - range.first - 1).coerceAtLeast(0),
        modifier = Modifier.fillMaxWidth().height(28.dp)
    )
}

@Composable
internal fun CompactLocationRow(mode: String, onModeSelected: (String) -> Unit) {
    val colors = LocalClintColors.current
    var menuOpen by remember { mutableStateOf(false) }
    val modeLabel = stringResource(
        if (mode == DownloadSettingsKeys.MODE_CUSTOM) R.string.download_location_option_custom else R.string.download_location_option_default
    )
    Box {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 32.dp).clickable { menuOpen = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.download_location_label), color = colors.onSurface, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            Text(modeLabel, color = colors.primary, fontSize = 13.sp, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
            Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = colors.iconTint, modifier = Modifier.padding(start = 4.dp).size(16.dp))
        }
        DropdownMenu(
            expanded = menuOpen, onDismissRequest = { menuOpen = false },
            shape = PopupShape, containerColor = colors.popupBackground, border = BorderStroke(1.dp, colors.popupStroke)
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.download_location_option_default), color = colors.onSurface, fontSize = 14.sp) },
                onClick = { menuOpen = false; onModeSelected(DownloadSettingsKeys.MODE_DEFAULT) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.download_location_option_custom), color = colors.onSurface, fontSize = 14.sp) },
                onClick = { menuOpen = false; onModeSelected(DownloadSettingsKeys.MODE_CUSTOM) }
            )
        }
    }
}

@Composable
internal fun CompactSpeedLimitRow(
    text: String,
    onTextChange: (String) -> Unit,
    unitLabel: String,
    kbLabel: String,
    mbLabel: String,
    onUnitChange: (String) -> Unit
) {
    val colors = LocalClintColors.current
    var menuOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.download_dialog_speed_limit_title), color = colors.onSurface, fontSize = 13.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        ClintCompactTextField(
            value = text,
            onValueChange = { onTextChange(it.filter { c -> c.isDigit() }) },
            modifier = Modifier.width(80.dp),
            placeholder = "0",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Box {
            Row(
                Modifier.clickable { menuOpen = true }.padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(unitLabel, color = colors.onSurface, fontSize = 13.sp, maxLines = 1)
                Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = colors.iconTint, modifier = Modifier.padding(start = 2.dp).size(16.dp))
            }
            DropdownMenu(
                expanded = menuOpen, onDismissRequest = { menuOpen = false },
                shape = PopupShape, containerColor = colors.popupBackground, border = BorderStroke(1.dp, colors.popupStroke)
            ) {
                DropdownMenuItem(text = { Text(kbLabel, color = colors.onSurface, fontSize = 14.sp) }, onClick = { onUnitChange(kbLabel); menuOpen = false })
                DropdownMenuItem(text = { Text(mbLabel, color = colors.onSurface, fontSize = 14.sp) }, onClick = { onUnitChange(mbLabel); menuOpen = false })
            }
        }
    }
}
