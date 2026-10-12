package com.jhaiian.clint.downloads

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import com.jhaiian.clint.ui.ClintCompactTextField
import com.jhaiian.clint.ui.ClintScheduleDateTimeRows
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
internal fun CompactManualDownloadContent(
    url: String,
    onUrlChange: (String) -> Unit,
    urlError: String?,
    isFetching: Boolean,
    filename: String,
    onFilenameChange: (String) -> Unit,
    extension: String,
    onExtensionChange: (String) -> Unit,
    fileSizeText: String?,
    locationMode: String,
    onLocationModeSelected: (String) -> Unit,
    customUri: Uri?,
    onPickFolder: () -> Unit,
    categorizeEnabled: Boolean,
    onCategorizeToggle: () -> Unit,
    destinationPreviewText: String?,
    retryEnabled: Boolean,
    onRetryToggle: () -> Unit,
    unmeteredOnly: Boolean,
    onUnmeteredToggle: () -> Unit,
    isStream: Boolean,
    splitParts: Int,
    onSplitPartsChange: (Int) -> Unit,
    multithreadingParts: Int,
    onMultithreadingPartsChange: (Int) -> Unit,
    concurrentSegments: Int,
    onConcurrentSegmentsChange: (Int) -> Unit,
    speedLimitText: String,
    onSpeedLimitTextChange: (String) -> Unit,
    speedUnitLabel: String,
    kbLabel: String,
    mbLabel: String,
    onSpeedUnitChange: (String) -> Unit,
    scheduleEnabled: Boolean,
    onScheduleToggle: () -> Unit,
    scheduledMillis: Long,
    onScheduledMillisChange: (Long) -> Unit,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean
) {
    val colors = LocalClintColors.current

    Column(Modifier.padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ClintCompactTextField(
                value = url, onValueChange = onUrlChange,
                modifier = Modifier.weight(1f),
                placeholder = stringResource(R.string.download_manual_url_hint),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                isError = urlError != null
            )
            if (isFetching) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(start = 10.dp).size(18.dp),
                    color = colors.primary, strokeWidth = 2.dp
                )
            }
        }
        urlError?.let {
            Text(it, color = colors.colorError, fontSize = 11.sp, modifier = Modifier.padding(start = 2.dp, top = 3.dp))
        }

        CompactDivider()

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ClintCompactTextField(
                value = filename, onValueChange = onFilenameChange,
                modifier = Modifier.weight(1f),
                placeholder = stringResource(R.string.download_dialog_filename_hint)
            )
            ClintCompactTextField(
                value = extension, onValueChange = onExtensionChange,
                modifier = Modifier.width(88.dp).padding(start = 8.dp),
                placeholder = stringResource(R.string.download_dialog_extension_hint)
            )
        }
        fileSizeText?.let {
            Text(it, color = colors.secondaryText, fontSize = 11.sp, modifier = Modifier.padding(start = 2.dp, top = 3.dp))
        }

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
        CompactToggleRow(stringResource(R.string.download_categorize_title), categorizeEnabled, onCategorizeToggle)
        destinationPreviewText?.let {
            Text(
                it, color = colors.secondaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                maxLines = 1, overflow = TextOverflow.MiddleEllipsis, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
            )
        }

        CompactDivider()

        CompactToggleRow(stringResource(R.string.download_retry_enabled_title), retryEnabled, onRetryToggle)
        CompactToggleRow(stringResource(R.string.download_unmetered_only_title), unmeteredOnly, onUnmeteredToggle)

        if (!isStream) {
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
        } else {
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
