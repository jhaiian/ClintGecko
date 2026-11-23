package com.jhaiian.clint.settings.parental

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.parental.ParentalControls
import com.jhaiian.clint.parental.ParentalGateHost
import com.jhaiian.clint.parental.ParentalGateRequest
import com.jhaiian.clint.parental.ParentalPassword
import com.jhaiian.clint.parental.ParentalPasswordSetDialog
import com.jhaiian.clint.parental.ParentalPasswordSetRequest
import com.jhaiian.clint.settings.SettingsActivity
import com.jhaiian.clint.settings.common.RowDivider
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.settings.common.SettingsSectionLabel
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.listscreen.ConfirmDialogHost
import com.jhaiian.clint.ui.theme.LocalClintColors

private class ProtectedArea(
    val key: String,
    val icon: ImageVector,
    val titleRes: Int,
    val summaryRes: Int
)

private val protectedAreas = listOf(
    ProtectedArea(ParentalControls.AREA_BLOCKER, Icons.Filled.Shield, R.string.website_blocker_title, R.string.parental_controls_protects_blocker),
    ProtectedArea(ParentalControls.AREA_EXTENSIONS, Icons.Filled.Extension, R.string.ext_title, R.string.parental_controls_protects_extensions),
    ProtectedArea(ParentalControls.AREA_HISTORY, Icons.Filled.History, R.string.history_title, R.string.parental_controls_protects_history),
    ProtectedArea(ParentalControls.AREA_RESTORE, Icons.Filled.SettingsBackupRestore, R.string.backup_restore_title, R.string.parental_controls_protects_restore)
)

@Composable
fun ParentalControlsPane(activity: SettingsActivity) {
    val colors = LocalClintColors.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(activity) }
    val hideStatusBar = remember { prefs.getBoolean("hide_status_bar", false) }
    val hideSystemNavigation = remember { prefs.getBoolean("hide_system_navigation", false) }
    var passwordSet by remember { mutableStateOf(ParentalPassword.isSet(activity)) }
    var optionsDialog by remember { mutableStateOf<ConfirmDialogConfig?>(null) }
    var gate by remember { mutableStateOf<ParentalGateRequest?>(null) }
    var setRequest by remember { mutableStateOf<ParentalPasswordSetRequest?>(null) }
    val areaEnabled = remember {
        mutableStateMapOf<String, Boolean>().apply {
            protectedAreas.forEach { put(it.key, ParentalControls.isEnabled(activity, it.key)) }
        }
    }

    fun openSet(changing: Boolean) {
        setRequest = ParentalPasswordSetRequest(changing = changing, onSaved = { passwordSet = true })
    }

    fun confirmChange() {
        gate = ParentalGateRequest(
            title = activity.getString(R.string.parental_password_change_title),
            message = activity.getString(R.string.parental_password_change_message),
            positiveLabel = activity.getString(R.string.restore_action_continue),
            onConfirm = { openSet(changing = true) }
        )
    }

    fun confirmRemoval() {
        gate = ParentalGateRequest(
            title = activity.getString(R.string.parental_password_remove_title),
            message = activity.getString(R.string.parental_password_remove_message),
            positiveLabel = activity.getString(R.string.parental_password_remove_action),
            onConfirm = {
                ParentalPassword.clear(activity)
                passwordSet = false
            }
        )
    }

    fun onPasswordClick() {
        if (!passwordSet) {
            openSet(changing = false)
            return
        }
        optionsDialog = ConfirmDialogConfig(
            title = activity.getString(R.string.parental_password_title),
            message = activity.getString(R.string.parental_password_options_message),
            neutralLabel = activity.getString(R.string.action_cancel),
            negativeLabel = activity.getString(R.string.parental_password_remove_action),
            onNegative = { confirmRemoval() },
            positiveLabel = activity.getString(R.string.parental_password_change_action),
            onPositive = { confirmChange() }
        )
    }

    fun onAreaClick(area: ProtectedArea) {
        if (!passwordSet) return
        if (areaEnabled[area.key] != true) {
            ParentalControls.setEnabled(activity, area.key, true)
            areaEnabled[area.key] = true
            return
        }
        val name = activity.getString(area.titleRes)
        gate = ParentalGateRequest(
            title = activity.getString(R.string.parental_controls_switch_off_title, name),
            message = activity.getString(R.string.parental_controls_switch_off_message, name),
            positiveLabel = activity.getString(R.string.website_blocker_guard_turn_off_action),
            onConfirm = {
                ParentalControls.setEnabled(activity, area.key, false)
                areaEnabled[area.key] = false
            }
        )
    }

    SettingsScreenScaffold(
        overlay = {
            ConfirmDialogHost(optionsDialog, hideStatusBar, hideSystemNavigation) { optionsDialog = null }
            ParentalGateHost(gate, hideStatusBar, hideSystemNavigation) { gate = null }
            ParentalPasswordSetDialog(setRequest, hideStatusBar, hideSystemNavigation) { setRequest = null }
        }
    ) {
        SettingsSectionLabel(stringResource(R.string.parental_controls_section_password))
        SettingsSection(colors.cardBackground, bottomSpacing = 8.dp) {
            SettingsRow(
                icon = Icons.Filled.Lock,
                title = stringResource(R.string.parental_password_title),
                summary = stringResource(if (passwordSet) R.string.parental_password_summary_on else R.string.parental_password_summary_off),
                colors = colors,
                onClick = ::onPasswordClick
            )
        }
        Text(
            stringResource(R.string.parental_controls_description),
            color = colors.secondaryText,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
        )

        SettingsSectionLabel(stringResource(R.string.parental_controls_section_protects))
        SettingsSection(colors.cardBackground, bottomSpacing = 8.dp) {
            protectedAreas.forEachIndexed { index, area ->
                if (index > 0) RowDivider(colors.divider)
                SettingsRow(
                    icon = area.icon,
                    title = stringResource(area.titleRes),
                    summary = stringResource(area.summaryRes),
                    colors = colors,
                    enabled = passwordSet,
                    onClick = { onAreaClick(area) },
                    trailing = { ClintSwitch(checked = passwordSet && areaEnabled[area.key] == true) }
                )
            }
        }
        Text(
            stringResource(if (passwordSet) R.string.parental_controls_areas_hint else R.string.parental_controls_areas_hint_no_password),
            color = colors.secondaryText,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
        )
    }
}
