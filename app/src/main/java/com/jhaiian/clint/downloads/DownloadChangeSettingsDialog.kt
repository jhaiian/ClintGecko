package com.jhaiian.clint.downloads
import androidx.compose.material.icons.filled.ArrowDownward

import android.widget.Toast

import com.jhaiian.clint.R

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.settings.common.dialogSectionBackground
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintDialogActionFooter
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun DownloadChangeSettingsDialog(item: DownloadItem, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalClintColors.current

    var retryEnabled by remember(item.id) { mutableStateOf(item.retryEnabled) }
    var unmeteredOnly by remember(item.id) { mutableStateOf(item.unmeteredOnly) }
    val (initAmount, initUnit) = remember(item.id) { speedLimitBytesToAmountAndUnit(context, item.speedLimitBytesPerSec) }
    var speedLimitText by remember(item.id) { mutableStateOf(if (initAmount > 0) initAmount.toString() else "") }
    val kbLabel = stringResource(R.string.speed_limit_unit_kb)
    val mbLabel = stringResource(R.string.speed_limit_unit_mb)
    var unitLabel by remember(item.id) { mutableStateOf(if (initUnit == SPEED_LIMIT_UNIT_MB) mbLabel else kbLabel) }
    var unitMenuOpen by remember { mutableStateOf(false) }

    ClintDialog(
        title = stringResource(R.string.download_change_settings_dialog_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            ClintDialogActionFooter(
                onCancel = onDismiss,
                positiveLabel = stringResource(R.string.action_save),
                onPositive = {
                    val amount = speedLimitText.toIntOrNull()?.coerceAtLeast(0) ?: 0
                    val unit = if (unitLabel == mbLabel) SPEED_LIMIT_UNIT_MB else SPEED_LIMIT_UNIT_KB
                    val bytesPerSec = resolveSpeedLimitBytesPerSec(context, amount, unit)
                    ClintDownloadManager.updateDownloadSettings(
                        context, item.id,
                        retryEnabled = retryEnabled,
                        unmeteredOnly = unmeteredOnly,
                        speedLimitBytesPerSec = bytesPerSec
                    )
                    Toast.makeText(context, context.getString(R.string.download_change_settings_saved), Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            )
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            Text(
                stringResource(R.string.download_change_settings_dialog_info),
                color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)
            )

            SettingsSection(colors.dialogSectionBackground) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth().clickable { retryEnabled = !retryEnabled }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.download_retry_enabled_title), color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.download_retry_enabled_summary), color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        ClintSwitch(checked = retryEnabled)
                    }

                    Row(
                        Modifier.fillMaxWidth().clickable { unmeteredOnly = !unmeteredOnly }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.download_unmetered_only_title), color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.download_dialog_unmetered_summary), color = colors.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        ClintSwitch(checked = unmeteredOnly)
                    }

                    Text(
                        stringResource(R.string.download_dialog_speed_limit_title), color = colors.onSurface,
                        fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        stringResource(R.string.download_dialog_speed_limit_desc), color = colors.secondaryText,
                        fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ClintOutlinedTextField(
                            value = speedLimitText,
                            onValueChange = { speedLimitText = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Box(Modifier.padding(start = 8.dp)) {
                            Row(
                                Modifier
                                    .clickable { unitMenuOpen = true }
                                    .padding(horizontal = 12.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(unitLabel, color = colors.onSurface, fontSize = 14.sp)
                                Icon(androidx.compose.material.icons.Icons.Filled.ArrowDownward, contentDescription = null, tint = colors.iconTint, modifier = Modifier.padding(start = 2.dp))
                            }
                            DropdownMenu(
                                expanded = unitMenuOpen,
                                onDismissRequest = { unitMenuOpen = false },
                                shape = PopupShape,
                                containerColor = colors.popupBackground,
                                border = BorderStroke(1.dp, colors.popupStroke),
                                modifier = Modifier.width(120.dp)
                            ) {
                                DropdownMenuItem(text = { Text(kbLabel, color = colors.onSurface) }, onClick = { unitLabel = kbLabel; unitMenuOpen = false })
                                DropdownMenuItem(text = { Text(mbLabel, color = colors.onSurface) }, onClick = { unitLabel = mbLabel; unitMenuOpen = false })
                            }
                        }
                    }
                }
            }
        }
    }
}
