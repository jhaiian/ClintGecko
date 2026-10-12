package com.jhaiian.clint.settings.main
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WebAsset

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jhaiian.clint.BuildConfig
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.RowDivider
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun MainSettingsScreen(
    versionName: String,
    onLookAndFeelClick: () -> Unit,
    onBrowserClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onSiteSettingsClick: () -> Unit,
    onDataSaverClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onBackupRestoreClick: () -> Unit,
    onUpdatesClick: () -> Unit,
    onMiscClick: () -> Unit,
    onDebugClick: () -> Unit,
    onAboutClick: () -> Unit,
    onSupportClintClick: () -> Unit
) {
    val colors = LocalClintColors.current

    SettingsScreenScaffold {
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Palette,
                title = stringResource(R.string.look_and_feel),
                summary = stringResource(R.string.look_and_feel_summary),
                colors = colors,
                onClick = onLookAndFeelClick
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Explore,
                title = stringResource(R.string.browser_settings),
                summary = stringResource(R.string.browser_settings_summary),
                colors = colors,
                onClick = onBrowserClick
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Lock,
                title = stringResource(R.string.privacy_settings),
                summary = stringResource(R.string.privacy_settings_summary),
                colors = colors,
                onClick = onPrivacyClick
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.WebAsset,
                title = stringResource(R.string.site_settings),
                summary = stringResource(R.string.site_settings_summary),
                colors = colors,
                onClick = onSiteSettingsClick
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.DataSaverOn,
                title = stringResource(R.string.data_saver_title),
                summary = stringResource(R.string.data_saver_settings_summary),
                colors = colors,
                onClick = onDataSaverClick
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Download,
                title = stringResource(R.string.download_settings_title),
                summary = stringResource(R.string.download_settings_summary),
                colors = colors,
                onClick = onDownloadsClick
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.SettingsBackupRestore,
                title = stringResource(R.string.backup_restore_title),
                summary = stringResource(R.string.backup_restore_summary),
                colors = colors,
                onClick = onBackupRestoreClick
            )
            RowDivider(colors.divider)
            if (BuildConfig.IS_FOSS) {
                SettingsRow(
                    icon = androidx.compose.material.icons.Icons.Filled.History,
                    title = stringResource(R.string.view_changelog_title),
                    summary = stringResource(R.string.view_changelog_summary),
                    colors = colors,
                    onClick = onUpdatesClick
                )
            } else {
                SettingsRow(
                    icon = androidx.compose.material.icons.Icons.Filled.Update,
                    title = stringResource(R.string.pref_updates_title),
                    summary = stringResource(R.string.pref_updates_summary),
                    colors = colors,
                    onClick = onUpdatesClick
                )
            }
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Tune,
                title = stringResource(R.string.pref_misc_title),
                summary = stringResource(R.string.pref_misc_summary),
                colors = colors,
                onClick = onMiscClick
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.BugReport,
                title = stringResource(R.string.debug_title),
                summary = stringResource(R.string.debug_summary),
                colors = colors,
                onClick = onDebugClick
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.VolunteerActivism,
                title = stringResource(R.string.support_clint_title),
                summary = stringResource(R.string.support_clint_summary),
                colors = colors,
                onClick = onSupportClintClick
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Info,
                title = stringResource(R.string.about),
                summary = stringResource(R.string.about_summary, versionName),
                colors = colors,
                onClick = onAboutClick
            )
        }
    }
}
