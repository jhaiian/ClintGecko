package com.jhaiian.clint.settings.misc
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.setup.SectionLabel
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
private fun RerunSetupConfirmDialog(hideStatusBar: Boolean, hideSystemNavigation: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    ClintDialog(
        title = stringResource(R.string.rerun_setup_confirm_title),
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
                    Text(stringResource(R.string.action_rerun), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Text(
            stringResource(R.string.rerun_setup_confirm_message),
            color = colors.onSurface,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun MiscScreen(
    state: MiscUiState,
    onDefaultBrowserClick: () -> Unit,
    onRerunSetupClick: () -> Unit,
    onRerunSetupConfirm: () -> Unit
) {
    val colors = LocalClintColors.current

    SettingsScreenScaffold(
        overlay = {
            if (state.rerunSetupConfirmDialogOpen) {
                RerunSetupConfirmDialog(
                    hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                    onConfirm = onRerunSetupConfirm,
                    onDismiss = { state.rerunSetupConfirmDialogOpen = false }
                )
            }
        }
    ) {
        SectionLabel(stringResource(R.string.misc_section_app), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Language,
                title = stringResource(R.string.default_browser_title),
                summary = state.defaultBrowserSummary,
                colors = colors,
                onClick = onDefaultBrowserClick
            )
        }

        SectionLabel(stringResource(R.string.misc_section_setup), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Refresh,
                title = stringResource(R.string.pref_rerun_setup_title),
                summary = stringResource(R.string.pref_rerun_setup_summary),
                colors = colors,
                onClick = onRerunSetupClick
            )
        }
    }
}
