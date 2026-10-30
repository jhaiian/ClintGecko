package com.jhaiian.clint.settings.main
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WebAsset

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.BuildConfig
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.RowDivider
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.settings.common.SettingsSectionLabel
import com.jhaiian.clint.settings.common.settingsSearchHighlight
import com.jhaiian.clint.ui.theme.LocalClintColors

enum class MainSettingsEntry {
    BROWSER, LOOK_AND_FEEL, ACCESSIBILITY, PRIVACY, SITE_SETTINGS, DOWNLOADS, DATA_SAVER, BACKUP_RESTORE, UPDATES, MISC, DEBUG, ABOUT
}

private class MainSettingsItem(
    val entry: MainSettingsEntry,
    val icon: ImageVector,
    val title: String,
    val summary: String,
    val onClick: () -> Unit
)

@Composable
fun MainSettingsScreen(
    versionName: String,
    selectedEntry: MainSettingsEntry? = null,
    onSearchClick: () -> Unit,
    onLookAndFeelClick: () -> Unit,
    onAccessibilityClick: () -> Unit,
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
    val updatesItem = if (BuildConfig.IS_FOSS) {
        MainSettingsItem(
            MainSettingsEntry.UPDATES, androidx.compose.material.icons.Icons.Filled.History,
            stringResource(R.string.view_changelog_title), stringResource(R.string.view_changelog_summary), onUpdatesClick
        )
    } else {
        MainSettingsItem(
            MainSettingsEntry.UPDATES, androidx.compose.material.icons.Icons.Filled.Update,
            stringResource(R.string.pref_updates_title), stringResource(R.string.pref_updates_summary), onUpdatesClick
        )
    }

    val groups = listOf(
        stringResource(R.string.settings_group_general) to listOf(
            MainSettingsItem(
                MainSettingsEntry.BROWSER, androidx.compose.material.icons.Icons.Filled.Explore,
                stringResource(R.string.browser_settings), stringResource(R.string.browser_settings_summary), onBrowserClick
            ),
            MainSettingsItem(
                MainSettingsEntry.LOOK_AND_FEEL, androidx.compose.material.icons.Icons.Filled.Palette,
                stringResource(R.string.look_and_feel), stringResource(R.string.look_and_feel_summary), onLookAndFeelClick
            ),
            MainSettingsItem(
                MainSettingsEntry.ACCESSIBILITY, androidx.compose.material.icons.Icons.Filled.Accessibility,
                stringResource(R.string.accessibility_title), stringResource(R.string.accessibility_summary), onAccessibilityClick
            )
        ),
        stringResource(R.string.settings_group_privacy) to listOf(
            MainSettingsItem(
                MainSettingsEntry.PRIVACY, androidx.compose.material.icons.Icons.Filled.Lock,
                stringResource(R.string.privacy_settings), stringResource(R.string.privacy_settings_summary), onPrivacyClick
            ),
            MainSettingsItem(
                MainSettingsEntry.SITE_SETTINGS, androidx.compose.material.icons.Icons.Filled.WebAsset,
                stringResource(R.string.site_settings), stringResource(R.string.site_settings_summary), onSiteSettingsClick
            )
        ),
        stringResource(R.string.settings_group_data) to listOf(
            MainSettingsItem(
                MainSettingsEntry.DOWNLOADS, androidx.compose.material.icons.Icons.Filled.Download,
                stringResource(R.string.download_settings_title), stringResource(R.string.download_settings_summary), onDownloadsClick
            ),
            MainSettingsItem(
                MainSettingsEntry.DATA_SAVER, androidx.compose.material.icons.Icons.Filled.DataSaverOn,
                stringResource(R.string.data_saver_title), stringResource(R.string.data_saver_settings_summary), onDataSaverClick
            ),
            MainSettingsItem(
                MainSettingsEntry.BACKUP_RESTORE, androidx.compose.material.icons.Icons.Filled.SettingsBackupRestore,
                stringResource(R.string.backup_restore_title), stringResource(R.string.backup_restore_summary), onBackupRestoreClick
            )
        ),
        stringResource(R.string.settings_group_advanced) to listOf(
            updatesItem,
            MainSettingsItem(
                MainSettingsEntry.MISC, androidx.compose.material.icons.Icons.Filled.Tune,
                stringResource(R.string.pref_misc_title), stringResource(R.string.pref_misc_summary), onMiscClick
            ),
            MainSettingsItem(
                MainSettingsEntry.DEBUG, androidx.compose.material.icons.Icons.Filled.BugReport,
                stringResource(R.string.debug_title), stringResource(R.string.debug_summary), onDebugClick
            )
        ),
        null to listOf(
            MainSettingsItem(
                MainSettingsEntry.ABOUT, androidx.compose.material.icons.Icons.Filled.Info,
                stringResource(R.string.about), stringResource(R.string.about_summary, versionName), onAboutClick
            )
        )
    )

    SettingsScreenScaffold {
        SettingsSearchPill(onClick = onSearchClick)
        SupportClintCard(onClick = onSupportClintClick)
        groups.forEach { (label, items) ->
            if (label != null) SettingsSectionLabel(label)
            MainSettingsGroup(items, selectedEntry)
        }
    }
}

@Composable
private fun MainSettingsGroup(items: List<MainSettingsItem>, selectedEntry: MainSettingsEntry?) {
    val colors = LocalClintColors.current
    SettingsSection(colors.cardBackground) {
        items.forEachIndexed { index, item ->
            if (index > 0) RowDivider(colors.divider)
            SettingsRow(
                icon = item.icon,
                title = item.title,
                summary = item.summary,
                colors = colors,
                selected = item.entry == selectedEntry,
                onClick = item.onClick
            )
        }
    }
}

@Composable
private fun SupportClintCard(onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(lerp(colors.cardBackground, colors.primary, 0.16f))
            .settingsSearchHighlight(stringResource(R.string.support_clint_title), colors.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(colors.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                androidx.compose.material.icons.Icons.Filled.VolunteerActivism,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(Modifier.weight(1f).padding(start = 16.dp, end = 8.dp)) {
            Text(
                stringResource(R.string.support_clint_title),
                color = colors.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(R.string.support_clint_summary),
                color = colors.secondaryText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            androidx.compose.material.icons.Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun SettingsSearchPill(onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(colors.cardBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp)
    ) {
        Icon(
            androidx.compose.material.icons.Icons.Filled.Search,
            contentDescription = null,
            tint = colors.iconTint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            stringResource(R.string.settings_search),
            color = colors.secondaryText,
            fontSize = 16.sp,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}
