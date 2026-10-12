package com.jhaiian.clint.settings.update
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.jhaiian.clint.ui.ClintSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.RowDivider
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.setup.SectionLabel
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
private fun BetaEnrolConfirmDialog(hideStatusBar: Boolean, hideSystemNavigation: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    ClintDialog(
        title = stringResource(R.string.beta_enrol_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(
                Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.secondaryText, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.beta_enrol_confirm), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Text(
            stringResource(R.string.beta_enrol_message),
            color = colors.onSurface,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun UpdateSettingsScreen(
    state: UpdateSettingsUiState,
    onCheckOnLaunchClick: () -> Unit,
    onSkipOnMeteredClick: () -> Unit,
    onCheckForUpdatesClick: () -> Unit,
    onViewChangelogClick: () -> Unit,
    onBetaChannelClick: () -> Unit,
    onBetaConfirm: () -> Unit
) {
    val colors = LocalClintColors.current

    SettingsScreenScaffold(
        overlay = {
            if (state.betaConfirmDialogOpen) {
                BetaEnrolConfirmDialog(
                    hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onConfirm = onBetaConfirm,
                    onDismiss = { state.betaConfirmDialogOpen = false }
                )
            }
        }
    ) {
        SectionLabel(stringResource(R.string.update_section_updates), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Refresh,
                title = stringResource(R.string.check_update_on_launch_title),
                summary = stringResource(R.string.check_update_on_launch_summary),
                colors = colors,
                onClick = onCheckOnLaunchClick,
                trailing = {
                    ClintSwitch(checked = state.checkOnLaunch)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Wifi,
                title = stringResource(R.string.skip_update_on_metered_title),
                summary = stringResource(R.string.skip_update_on_metered_summary),
                colors = colors,
                onClick = onSkipOnMeteredClick,
                enabled = state.checkOnLaunch,
                trailing = {
                    ClintSwitch(checked = state.skipOnMetered)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Update,
                title = stringResource(R.string.check_for_updates_title),
                summary = stringResource(R.string.check_for_updates_summary),
                colors = colors,
                onClick = onCheckForUpdatesClick
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.History,
                title = stringResource(R.string.view_changelog_title),
                summary = stringResource(R.string.view_changelog_summary),
                colors = colors,
                onClick = onViewChangelogClick
            )
        }

        SectionLabel(stringResource(R.string.update_section_channel), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Warning,
                title = stringResource(R.string.beta_channel_title),
                summary = stringResource(R.string.beta_channel_summary),
                colors = colors,
                onClick = onBetaChannelClick,
                trailing = {
                    ClintSwitch(checked = state.betaChannel)
                }
            )
        }
    }
}
