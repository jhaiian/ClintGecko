package com.jhaiian.clint.settings.downloads
import androidx.compose.material.icons.filled.ArrowDownward

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import com.jhaiian.clint.ui.ClintOutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.setup.CheckSlot
import com.jhaiian.clint.setup.SelectableCard
import com.jhaiian.clint.settings.common.dialogSectionBackground
import com.jhaiian.clint.settings.common.SettingsPickerOptionBottomSpacing
import com.jhaiian.clint.settings.common.SettingsPickerOptionContentPadding
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.downloads.SPEED_LIMIT_UNIT_KB
import com.jhaiian.clint.downloads.SPEED_LIMIT_UNIT_MB
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.theme.LocalClintColors

private val OptionContentPadding = SettingsPickerOptionContentPadding
private val OptionBottomSpacing = SettingsPickerOptionBottomSpacing

@Composable
fun DownloadDialogUiDialog(
    current: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    ClintDialog(title = stringResource(R.string.download_dialog_ui_title), hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = onDismiss) {
        data class Option(val key: String, val titleRes: Int, val descRes: Int)
        listOf(
            Option(DownloadSettingsKeys.DIALOG_UI_STANDARD, R.string.download_dialog_ui_standard, R.string.download_dialog_ui_standard_desc),
            Option(DownloadSettingsKeys.DIALOG_UI_COMPACT, R.string.download_dialog_ui_compact, R.string.download_dialog_ui_compact_desc)
        ).forEach { option ->
            SelectableCard(
                selected = current == option.key, onClick = { onSelect(option.key) },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing
            ) {
                Column(Modifier.weight(1f).padding(start = 4.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                CheckSlot(current == option.key, colors.primary)
            }
        }
    }
}

@Composable
fun MeasurementSystemDialog(
    current: Boolean,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onSelect: (decimal: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    ClintDialog(title = stringResource(R.string.measurement_system_dialog_title), hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = onDismiss) {
        data class Option(val decimal: Boolean, val titleRes: Int, val descRes: Int)
        listOf(
            Option(false, R.string.measurement_system_binary, R.string.measurement_system_binary_desc),
            Option(true, R.string.measurement_system_decimal, R.string.measurement_system_decimal_desc)
        ).forEach { option ->
            SelectableCard(
                selected = current == option.decimal, onClick = { onSelect(option.decimal) },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = OptionContentPadding, bottomSpacing = OptionBottomSpacing
            ) {
                Column(Modifier.weight(1f).padding(start = 4.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                CheckSlot(current == option.decimal, colors.primary)
            }
        }
    }
}

@Composable
fun DownloadManagerDialog(
    current: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onSelect: (appId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    data class Option(val appId: String, val titleRes: Int)
    val options = listOf(
        Option(com.jhaiian.clint.downloads.DownloadManagerAppIds.CLINT, R.string.download_manager_option_clint),
        Option(com.jhaiian.clint.downloads.DownloadManagerAppIds.ONEDM, R.string.download_manager_option_1dm),
        Option(com.jhaiian.clint.downloads.DownloadManagerAppIds.ONEDM_PLUS, R.string.download_manager_option_1dm_plus),
        Option(com.jhaiian.clint.downloads.DownloadManagerAppIds.ONEDM_LITE, R.string.download_manager_option_1dm_lite),
        Option(com.jhaiian.clint.downloads.DownloadManagerAppIds.ADM, R.string.download_manager_option_adm)
    )
    ClintDialog(title = stringResource(R.string.download_manager_dialog_title), hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation, onDismiss = onDismiss) {
        options.forEach { option ->
            val installed = remember(option.appId) { com.jhaiian.clint.downloads.isDownloadManagerAppInstalled(context, option.appId) }
            val selected = current == option.appId
            Card(
                onClick = { if (installed) onSelect(option.appId) },
                enabled = installed,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = OptionBottomSpacing)
                    .alpha(if (!installed) 0.35f else if (selected) 1.0f else 0.6f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant, disabledContainerColor = colors.surfaceVariant),
                border = if (selected) BorderStroke(3.dp, colors.primary) else null
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(OptionContentPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f).padding(start = 4.dp, end = 8.dp)) {
                        Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        if (!installed) {
                            Text(stringResource(R.string.download_manager_not_installed), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    CheckSlot(selected, colors.primary)
                }
            }
        }
    }
}

@Composable
private fun NumberEntryDialog(
    title: String,
    message: String,
    hint: String,
    initialValue: Int,
    minValue: Int,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var text by remember { mutableStateOf(initialValue.toString()) }

    ClintDialog(
        title = title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onConfirm(text.toIntOrNull()?.coerceAtLeast(minValue) ?: initialValue) }) {
                    Text(stringResource(R.string.action_ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text(message, color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            SettingsSection(colors.dialogSectionBackground) {
                Column(Modifier.padding(16.dp)) {
                    ClintOutlinedTextField(
                        value = text,
                        onValueChange = { new -> if (new.all { it.isDigit() }) text = new },
                        label = { Text(hint) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
fun RetryCountDialog(current: Int, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    NumberEntryDialog(
        title = stringResource(R.string.download_retry_count_dialog_title),
        message = stringResource(R.string.download_retry_count_dialog_message),
        hint = stringResource(R.string.download_retry_count_dialog_hint),
        initialValue = current,
        minValue = 0,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun RetryIntervalDialog(current: Int, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    NumberEntryDialog(
        title = stringResource(R.string.download_retry_interval_dialog_title),
        message = stringResource(R.string.download_retry_interval_dialog_message),
        hint = stringResource(R.string.download_retry_interval_dialog_hint),
        initialValue = current,
        minValue = 1,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun SpeedLimitDialog(
    currentAmount: Int,
    currentUnit: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: (amount: Int, unit: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var amountText by remember { mutableStateOf(if (currentAmount > 0) currentAmount.toString() else "") }
    var unit by remember { mutableStateOf(currentUnit) }
    var unitMenuExpanded by remember { mutableStateOf(false) }

    ClintDialog(
        title = stringResource(R.string.download_speed_limit_dialog_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onConfirm(amountText.toIntOrNull()?.coerceAtLeast(0) ?: 0, unit) }) {
                    Text(stringResource(R.string.action_ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text(
                stringResource(R.string.download_speed_limit_dialog_message),
                color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp)
            )
            SettingsSection(colors.dialogSectionBackground) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ClintOutlinedTextField(
                        value = amountText,
                        onValueChange = { new -> if (new.all { it.isDigit() }) amountText = new },
                        label = { Text(stringResource(R.string.download_speed_limit_dialog_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    Box(Modifier.padding(start = 8.dp)) {
                        val unitLabel = stringResource(if (unit == SPEED_LIMIT_UNIT_MB) R.string.speed_limit_unit_mb else R.string.speed_limit_unit_kb)
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { unitMenuExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(unitLabel, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Icon(
                                androidx.compose.material.icons.Icons.Filled.ArrowDownward, contentDescription = null,
                                tint = colors.iconTint, modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = unitMenuExpanded,
                            onDismissRequest = { unitMenuExpanded = false },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = colors.popupBackground
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.speed_limit_unit_kb)) },
                                onClick = { unit = SPEED_LIMIT_UNIT_KB; unitMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.speed_limit_unit_mb)) },
                                onClick = { unit = SPEED_LIMIT_UNIT_MB; unitMenuExpanded = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
