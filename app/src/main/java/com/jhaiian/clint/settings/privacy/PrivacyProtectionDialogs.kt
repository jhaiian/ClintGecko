package com.jhaiian.clint.settings.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.privacy.PrivacyPrefs
import com.jhaiian.clint.settings.common.SettingsPickerOptionBottomSpacing
import com.jhaiian.clint.settings.common.SettingsPickerOptionContentPadding
import com.jhaiian.clint.setup.DefaultChip
import com.jhaiian.clint.setup.SelectableCard
import com.jhaiian.clint.ui.ClintCheckbox
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintRadioButton
import com.jhaiian.clint.ui.theme.LocalClintColors

internal fun appLockTimeoutLabel(seconds: Int): Int = when (seconds) {
    0 -> R.string.app_lock_timeout_immediately
    300 -> R.string.app_lock_timeout_5_minutes
    900 -> R.string.app_lock_timeout_15_minutes
    else -> R.string.app_lock_timeout_1_minute
}

internal fun clipboardClearLabel(seconds: Int): Int = when (seconds) {
    30 -> R.string.clipboard_clear_30_seconds
    60 -> R.string.clipboard_clear_1_minute
    300 -> R.string.clipboard_clear_5_minutes
    else -> R.string.clipboard_clear_off
}

internal fun cookiePolicyLabel(policy: String): Int = when (policy) {
    PrivacyPrefs.COOKIE_POLICY_ALLOW_ALL -> R.string.cookie_policy_allow_all
    PrivacyPrefs.COOKIE_POLICY_BLOCK_THIRD_PARTY -> R.string.block_third_party_cookies
    else -> R.string.cookie_policy_isolate
}

internal fun etpLevelLabel(level: String): Int = when (level) {
    PrivacyPrefs.ETP_LEVEL_OFF -> R.string.etp_level_off
    PrivacyPrefs.ETP_LEVEL_STRICT -> R.string.etp_level_strict
    else -> R.string.etp_level_standard
}

private data class DescribedOption(val key: String, val titleRes: Int, val descRes: Int)

@Composable
internal fun CookiePolicyDialog(
    current: String,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    DescribedChoiceDialog(
        title = stringResource(R.string.cookie_policy_title),
        options = listOf(
            DescribedOption(PrivacyPrefs.COOKIE_POLICY_ALLOW_ALL, R.string.cookie_policy_allow_all, R.string.cookie_policy_allow_all_desc),
            DescribedOption(PrivacyPrefs.COOKIE_POLICY_ISOLATE, R.string.cookie_policy_isolate, R.string.cookie_policy_isolate_desc),
            DescribedOption(PrivacyPrefs.COOKIE_POLICY_BLOCK_THIRD_PARTY, R.string.block_third_party_cookies, R.string.cookie_policy_block_desc)
        ),
        defaultKey = PrivacyPrefs.DEFAULT_COOKIE_POLICY,
        current = current,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
internal fun EtpLevelDialog(
    current: String,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    DescribedChoiceDialog(
        title = stringResource(R.string.etp_title),
        options = listOf(
            DescribedOption(PrivacyPrefs.ETP_LEVEL_STANDARD, R.string.etp_level_standard, R.string.etp_level_standard_desc),
            DescribedOption(PrivacyPrefs.ETP_LEVEL_STRICT, R.string.etp_level_strict, R.string.etp_level_strict_desc),
            DescribedOption(PrivacyPrefs.ETP_LEVEL_OFF, R.string.etp_level_off, R.string.etp_level_off_desc)
        ),
        defaultKey = PrivacyPrefs.DEFAULT_ETP_LEVEL,
        current = current,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
private fun DescribedChoiceDialog(
    title: String,
    options: List<DescribedOption>,
    defaultKey: String,
    current: String,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var selected by remember(current) { mutableStateOf(current) }

    ClintDialog(
        title = title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onConfirm(selected) }) {
                    Text(stringResource(android.R.string.ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        options.forEach { option ->
            val sel = selected == option.key
            SelectableCard(
                selected = sel, onClick = { selected = option.key },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = SettingsPickerOptionContentPadding, bottomSpacing = SettingsPickerOptionBottomSpacing
            ) {
                ClintRadioButton(selected = sel)
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                if (option.key == defaultKey) DefaultChip(stringResource(R.string.default_label), colors.primary)
            }
        }
    }
}

@Composable
internal fun PrivacyChoiceDialog(
    title: String,
    values: List<Int>,
    labelFor: (Int) -> Int,
    current: Int,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var selected by remember(current) { mutableStateOf(current) }

    ClintDialog(
        title = title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onConfirm(selected) }) {
                    Text(stringResource(android.R.string.ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        values.forEach { value ->
            val sel = selected == value
            SelectableCard(
                selected = sel, onClick = { selected = value },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = SettingsPickerOptionContentPadding, bottomSpacing = SettingsPickerOptionBottomSpacing
            ) {
                ClintRadioButton(selected = sel)
                Text(
                    stringResource(labelFor(value)),
                    color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}

@Composable
internal fun ClearOnExitDialog(
    history: Boolean,
    cookies: Boolean,
    cache: Boolean,
    tabs: Boolean,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onConfirm: (history: Boolean, cookies: Boolean, cache: Boolean, tabs: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var historyChecked by remember(history) { mutableStateOf(history) }
    var cookiesChecked by remember(cookies) { mutableStateOf(cookies) }
    var cacheChecked by remember(cache) { mutableStateOf(cache) }
    var tabsChecked by remember(tabs) { mutableStateOf(tabs) }

    ClintDialog(
        title = stringResource(R.string.clear_on_exit_items_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onConfirm(historyChecked, cookiesChecked, cacheChecked, tabsChecked) }) {
                    Text(stringResource(android.R.string.ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        CheckOption(R.string.clear_on_exit_history, historyChecked) { historyChecked = it }
        CheckOption(R.string.clear_on_exit_cookies, cookiesChecked) { cookiesChecked = it }
        CheckOption(R.string.clear_on_exit_cache, cacheChecked) { cacheChecked = it }
        CheckOption(R.string.clear_on_exit_tabs, tabsChecked) { tabsChecked = it }
    }
}

@Composable
private fun CheckOption(label: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = LocalClintColors.current
    SelectableCard(
        selected = checked, onClick = { onChange(!checked) },
        cardBackground = colors.surfaceVariant, primary = colors.primary,
        contentPadding = SettingsPickerOptionContentPadding, bottomSpacing = SettingsPickerOptionBottomSpacing
    ) {
        ClintCheckbox(checked = checked, onCheckedChange = onChange)
        Text(
            stringResource(label),
            color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

internal val AppLockTimeoutValues get() = PrivacyPrefs.appLockTimeouts
internal val ClipboardClearValues get() = PrivacyPrefs.clipboardDelays
