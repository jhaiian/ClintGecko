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
import com.jhaiian.clint.browser.webview.SecureDns
import com.jhaiian.clint.settings.common.SettingsPickerOptionBottomSpacing
import com.jhaiian.clint.settings.common.SettingsPickerOptionContentPadding
import com.jhaiian.clint.setup.DefaultChip
import com.jhaiian.clint.setup.SelectableCard
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.ClintRadioButton
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun SecureDnsModeDialog(
    current: String,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var selected by remember(current) { mutableStateOf(current) }

    ClintDialog(
        title = stringResource(R.string.secure_dns_title),
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
        data class ModeOption(val key: String, val titleRes: Int, val descRes: Int, val isDefault: Boolean)
        listOf(
            ModeOption(SecureDns.MODE_OFF, R.string.secure_dns_mode_off, R.string.secure_dns_mode_off_desc, true),
            ModeOption(SecureDns.MODE_INCREASED, R.string.secure_dns_mode_increased, R.string.secure_dns_mode_increased_desc, false),
            ModeOption(SecureDns.MODE_MAX, R.string.secure_dns_mode_max, R.string.secure_dns_mode_max_desc, false)
        ).forEach { option ->
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
                if (option.isDefault) DefaultChip(stringResource(R.string.default_label), colors.primary)
            }
        }
    }
}

@Composable
fun SecureDnsProviderDialog(
    current: String,
    customUrl: String,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onConfirm: (provider: String, customUrl: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var selected by remember(current) { mutableStateOf(current) }
    var url by remember(customUrl) { mutableStateOf(customUrl) }
    var urlError by remember { mutableStateOf(false) }

    ClintDialog(
        title = stringResource(R.string.secure_dns_provider_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(
                    onClick = {
                        if (selected == SecureDns.PROVIDER_CUSTOM && !SecureDns.isValidDohUri(url)) {
                            urlError = true
                        } else {
                            onConfirm(selected, url.trim())
                        }
                    }
                ) {
                    Text(stringResource(android.R.string.ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        data class ProviderOption(val key: String, val titleRes: Int, val descRes: Int, val isDefault: Boolean)
        listOf(
            ProviderOption(SecureDns.PROVIDER_CLOUDFLARE, R.string.secure_dns_provider_cloudflare, R.string.secure_dns_provider_cloudflare_desc, true),
            ProviderOption(SecureDns.PROVIDER_GOOGLE, R.string.secure_dns_provider_google, R.string.secure_dns_provider_google_desc, false),
            ProviderOption(SecureDns.PROVIDER_QUAD9, R.string.secure_dns_provider_quad9, R.string.secure_dns_provider_quad9_desc, false),
            ProviderOption(SecureDns.PROVIDER_CUSTOM, R.string.secure_dns_provider_custom, R.string.secure_dns_provider_custom_desc, false)
        ).forEach { option ->
            val sel = selected == option.key
            SelectableCard(
                selected = sel, onClick = { selected = option.key; urlError = false },
                cardBackground = colors.surfaceVariant, primary = colors.primary,
                contentPadding = SettingsPickerOptionContentPadding, bottomSpacing = SettingsPickerOptionBottomSpacing
            ) {
                ClintRadioButton(selected = sel)
                Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                    Text(stringResource(option.titleRes), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(option.descRes), color = colors.secondaryText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
                if (option.isDefault) DefaultChip(stringResource(R.string.default_label), colors.primary)
            }
        }
        if (selected == SecureDns.PROVIDER_CUSTOM) {
            ClintOutlinedTextField(
                value = url,
                onValueChange = { url = it; urlError = false },
                label = { Text(stringResource(R.string.secure_dns_custom_url_hint)) },
                singleLine = true,
                isError = urlError,
                supportingText = {
                    Text(
                        stringResource(if (urlError) R.string.secure_dns_custom_url_error else R.string.secure_dns_custom_url_helper),
                        color = if (urlError) colors.colorError else colors.secondaryText,
                        fontSize = 12.sp
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
