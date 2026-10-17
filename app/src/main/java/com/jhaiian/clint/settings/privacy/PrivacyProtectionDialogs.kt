package com.jhaiian.clint.settings.privacy

import androidx.compose.foundation.layout.Arrangement
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
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onConfirm: (history: Boolean, cookies: Boolean, cache: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var historyChecked by remember(history) { mutableStateOf(history) }
    var cookiesChecked by remember(cookies) { mutableStateOf(cookies) }
    var cacheChecked by remember(cache) { mutableStateOf(cache) }

    ClintDialog(
        title = stringResource(R.string.clear_on_exit_items_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onConfirm(historyChecked, cookiesChecked, cacheChecked) }) {
                    Text(stringResource(android.R.string.ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        CheckOption(R.string.clear_on_exit_history, historyChecked) { historyChecked = it }
        CheckOption(R.string.clear_on_exit_cookies, cookiesChecked) { cookiesChecked = it }
        CheckOption(R.string.clear_on_exit_cache, cacheChecked) { cacheChecked = it }
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
