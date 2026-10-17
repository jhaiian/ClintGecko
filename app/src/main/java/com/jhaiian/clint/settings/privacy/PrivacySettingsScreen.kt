package com.jhaiian.clint.settings.privacy
import androidx.compose.material.icons.filled.ContentPasteOff
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.VisibilityOff

import androidx.compose.foundation.layout.padding
import com.jhaiian.clint.ui.ClintSwitch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.webview.SecureDns
import com.jhaiian.clint.settings.common.RowDivider
import com.jhaiian.clint.settings.common.SettingsRow
import com.jhaiian.clint.settings.common.SettingsScreenScaffold
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.setup.SectionLabel
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun PrivacySettingsScreen(
    state: PrivacySettingsUiState,
    onBlockThirdPartyCookiesClick: () -> Unit,
    onCustomUserAgentClick: () -> Unit,
    onHttpsOnlyClick: () -> Unit,
    onDoNotTrackClick: () -> Unit,
    onStrictCertificatesClick: () -> Unit,
    onStripTrackingParamsClick: () -> Unit,
    onGlobalPrivacyControlClick: () -> Unit,
    onFingerprintingProtectionClick: () -> Unit,
    onReferrerTrimmingClick: () -> Unit,
    onWebRtcProtectionClick: () -> Unit,
    onClearOnExitClick: () -> Unit,
    onClearOnExitConfirmed: (history: Boolean, cookies: Boolean, cache: Boolean) -> Unit,
    onAppLockClick: () -> Unit,
    onAppLockTimeoutConfirmed: (Int) -> Unit,
    onScreenshotBlockingClick: () -> Unit,
    onIncognitoKeyboardClick: () -> Unit,
    onClipboardClearConfirmed: (Int) -> Unit,
    onSecureDnsModeConfirmed: (String) -> Unit,
    onSecureDnsProviderConfirmed: (provider: String, customUrl: String) -> Unit,
    onAutofillClick: () -> Unit,
    onAutofillServiceClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val colors = LocalClintColors.current
    val historyLabel = stringResource(R.string.clear_on_exit_history)
    val cookiesLabel = stringResource(R.string.clear_on_exit_cookies)
    val cacheLabel = stringResource(R.string.clear_on_exit_cache)
    val nothingLabel = stringResource(R.string.clear_on_exit_none)

    SettingsScreenScaffold {

        SectionLabel(stringResource(R.string.privacy_section_privacy), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Cookie,
                title = stringResource(R.string.block_third_party_cookies),
                summary = stringResource(R.string.block_third_party_cookies_summary),
                colors = colors,
                onClick = onBlockThirdPartyCookiesClick,
                trailing = {
                    ClintSwitch(checked = state.blockThirdPartyCookies)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Language,
                title = stringResource(R.string.custom_user_agent),
                summary = stringResource(R.string.custom_user_agent_summary),
                colors = colors,
                onClick = onCustomUserAgentClick,
                trailing = {
                    ClintSwitch(checked = state.customUserAgent)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Lock,
                title = stringResource(R.string.https_only),
                summary = stringResource(R.string.https_only_summary),
                colors = colors,
                onClick = onHttpsOnlyClick,
                trailing = {
                    ClintSwitch(checked = state.httpsOnly)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.VerifiedUser,
                title = stringResource(R.string.strict_certificates_title),
                summary = stringResource(R.string.strict_certificates_summary),
                colors = colors,
                onClick = onStrictCertificatesClick,
                trailing = {
                    ClintSwitch(checked = state.strictCertificates)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.VisibilityOff,
                title = stringResource(R.string.do_not_track_title),
                summary = stringResource(R.string.do_not_track_summary),
                colors = colors,
                onClick = onDoNotTrackClick,
                trailing = {
                    ClintSwitch(checked = state.doNotTrack)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Dns,
                title = stringResource(R.string.secure_dns_title),
                summary = stringResource(
                    when (state.secureDnsMode) {
                        SecureDns.MODE_INCREASED -> R.string.secure_dns_mode_increased
                        SecureDns.MODE_MAX -> R.string.secure_dns_mode_max
                        else -> R.string.secure_dns_mode_off
                    }
                ),
                colors = colors,
                onClick = { state.secureDnsModeDialogOpen = true }
            )
            if (state.secureDnsMode != SecureDns.MODE_OFF) {
                RowDivider(colors.divider)
                SettingsRow(
                    icon = androidx.compose.material.icons.Icons.Filled.Dns,
                    title = stringResource(R.string.secure_dns_provider_title),
                    summary = when (state.secureDnsProvider) {
                        SecureDns.PROVIDER_GOOGLE -> stringResource(R.string.secure_dns_provider_google)
                        SecureDns.PROVIDER_QUAD9 -> stringResource(R.string.secure_dns_provider_quad9)
                        SecureDns.PROVIDER_CUSTOM -> state.secureDnsCustomUrl.ifBlank { stringResource(R.string.secure_dns_provider_custom) }
                        else -> stringResource(R.string.secure_dns_provider_cloudflare)
                    },
                    colors = colors,
                    onClick = { state.secureDnsProviderDialogOpen = true }
                )
            }
        }

        SectionLabel(stringResource(R.string.privacy_section_tracking), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.LinkOff,
                title = stringResource(R.string.strip_tracking_params_title),
                summary = stringResource(R.string.strip_tracking_params_summary),
                colors = colors,
                onClick = onStripTrackingParamsClick,
                trailing = {
                    ClintSwitch(checked = state.stripTrackingParams)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Fingerprint,
                title = stringResource(R.string.fingerprinting_protection_title),
                summary = stringResource(R.string.fingerprinting_protection_summary),
                colors = colors,
                onClick = onFingerprintingProtectionClick,
                trailing = {
                    ClintSwitch(checked = state.fingerprintingProtection)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.GppGood,
                title = stringResource(R.string.global_privacy_control_title),
                summary = stringResource(R.string.global_privacy_control_summary),
                colors = colors,
                onClick = onGlobalPrivacyControlClick,
                trailing = {
                    ClintSwitch(checked = state.globalPrivacyControl)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Link,
                title = stringResource(R.string.referrer_trimming_title),
                summary = stringResource(R.string.referrer_trimming_summary),
                colors = colors,
                onClick = onReferrerTrimmingClick,
                trailing = {
                    ClintSwitch(checked = state.referrerTrimming)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Videocam,
                title = stringResource(R.string.webrtc_protection_title),
                summary = stringResource(R.string.webrtc_protection_summary),
                colors = colors,
                onClick = onWebRtcProtectionClick,
                trailing = {
                    ClintSwitch(checked = state.webRtcProtection)
                }
            )
        }

        SectionLabel(stringResource(R.string.privacy_section_device), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.PhonelinkLock,
                title = stringResource(R.string.app_lock_title),
                summary = stringResource(R.string.app_lock_summary),
                colors = colors,
                onClick = onAppLockClick,
                trailing = {
                    ClintSwitch(checked = state.appLock)
                }
            )
            if (state.appLock) {
                RowDivider(colors.divider)
                SettingsRow(
                    icon = androidx.compose.material.icons.Icons.Filled.Timer,
                    title = stringResource(R.string.app_lock_timeout_title),
                    summary = stringResource(appLockTimeoutLabel(state.appLockTimeout)),
                    colors = colors,
                    onClick = { state.appLockTimeoutDialogOpen = true }
                )
            }
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.HideImage,
                title = stringResource(R.string.screenshot_blocking_title),
                summary = stringResource(R.string.screenshot_blocking_summary),
                colors = colors,
                onClick = onScreenshotBlockingClick,
                trailing = {
                    ClintSwitch(checked = state.screenshotBlocking)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.Keyboard,
                title = stringResource(R.string.incognito_keyboard_title),
                summary = stringResource(R.string.incognito_keyboard_summary),
                colors = colors,
                onClick = onIncognitoKeyboardClick,
                trailing = {
                    ClintSwitch(checked = state.incognitoKeyboard)
                }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.ContentPasteOff,
                title = stringResource(R.string.clipboard_clear_title),
                summary = stringResource(clipboardClearLabel(state.clipboardClearSeconds)),
                colors = colors,
                onClick = { state.clipboardDialogOpen = true }
            )
            RowDivider(colors.divider)
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.DeleteSweep,
                title = stringResource(R.string.clear_on_exit_title),
                summary = stringResource(R.string.clear_on_exit_summary),
                colors = colors,
                onClick = onClearOnExitClick,
                trailing = {
                    ClintSwitch(checked = state.clearOnExit)
                }
            )
            if (state.clearOnExit) {
                RowDivider(colors.divider)
                SettingsRow(
                    icon = androidx.compose.material.icons.Icons.Filled.Checklist,
                    title = stringResource(R.string.clear_on_exit_items_title),
                    summary = listOfNotNull(
                        historyLabel.takeIf { state.clearHistory },
                        cookiesLabel.takeIf { state.clearCookies },
                        cacheLabel.takeIf { state.clearCache }
                    ).joinToString(", ").ifEmpty { nothingLabel },
                    colors = colors,
                    onClick = { state.clearOnExitDialogOpen = true }
                )
            }
        }

        if (state.autofillSupported) {
            SectionLabel(stringResource(R.string.privacy_section_autofill), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
            SettingsSection(colors.cardBackground) {
                SettingsRow(
                    icon = androidx.compose.material.icons.Icons.Filled.Password,
                    title = stringResource(R.string.autofill_title),
                    summary = stringResource(R.string.autofill_summary),
                    colors = colors,
                    onClick = onAutofillClick,
                    trailing = {
                        ClintSwitch(checked = state.autofillEnabled)
                    }
                )
                RowDivider(colors.divider)
                SettingsRow(
                    icon = androidx.compose.material.icons.Icons.Filled.ManageAccounts,
                    title = stringResource(R.string.autofill_service_title),
                    summary = stringResource(
                        if (state.autofillServiceActive) R.string.autofill_service_summary_active else R.string.autofill_service_summary_none
                    ),
                    colors = colors,
                    onClick = onAutofillServiceClick
                )
            }
        }

        if (state.appLockTimeoutDialogOpen) {
            PrivacyChoiceDialog(
                title = stringResource(R.string.app_lock_timeout_title),
                values = AppLockTimeoutValues,
                labelFor = ::appLockTimeoutLabel,
                current = state.appLockTimeout,
                hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                onConfirm = onAppLockTimeoutConfirmed,
                onDismiss = { state.appLockTimeoutDialogOpen = false }
            )
        }
        if (state.clipboardDialogOpen) {
            PrivacyChoiceDialog(
                title = stringResource(R.string.clipboard_clear_title),
                values = ClipboardClearValues,
                labelFor = ::clipboardClearLabel,
                current = state.clipboardClearSeconds,
                hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                onConfirm = onClipboardClearConfirmed,
                onDismiss = { state.clipboardDialogOpen = false }
            )
        }
        if (state.clearOnExitDialogOpen) {
            ClearOnExitDialog(
                history = state.clearHistory,
                cookies = state.clearCookies,
                cache = state.clearCache,
                hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                onConfirm = onClearOnExitConfirmed,
                onDismiss = { state.clearOnExitDialogOpen = false }
            )
        }
        if (state.secureDnsModeDialogOpen) {
            SecureDnsModeDialog(
                current = state.secureDnsMode,
                hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                onConfirm = onSecureDnsModeConfirmed,
                onDismiss = { state.secureDnsModeDialogOpen = false }
            )
        }
        if (state.secureDnsProviderDialogOpen) {
            SecureDnsProviderDialog(
                current = state.secureDnsProvider,
                customUrl = state.secureDnsCustomUrl,
                hideStatusBar = state.hideStatusBar, hideSystemNavigation = state.hideSystemNavigation,
                onConfirm = onSecureDnsProviderConfirmed,
                onDismiss = { state.secureDnsProviderDialogOpen = false }
            )
        }

        SectionLabel(stringResource(R.string.privacy_section_history), colors.primary, Modifier.padding(start = 4.dp, bottom = 8.dp))
        SettingsSection(colors.cardBackground) {
            SettingsRow(
                icon = androidx.compose.material.icons.Icons.Filled.History,
                title = stringResource(R.string.history_title),
                summary = stringResource(R.string.history_summary),
                colors = colors,
                onClick = onHistoryClick
            )
        }
    }
}
