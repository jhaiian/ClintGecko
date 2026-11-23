package com.jhaiian.clint.setup

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.RemoveModerator
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.webview.SecureDns
import com.jhaiian.clint.privacy.PrivacyPrefs
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.theme.LocalClintColors

private data class PrivacyChoice(val key: String, val titleRes: Int, val descRes: Int, val icon: ImageVector)

private val TrackingChoices = listOf(
    PrivacyChoice(PrivacyPrefs.ETP_LEVEL_OFF, R.string.etp_level_off, R.string.etp_level_off_desc, Icons.Outlined.RemoveModerator),
    PrivacyChoice(PrivacyPrefs.ETP_LEVEL_STANDARD, R.string.etp_level_standard, R.string.etp_level_standard_desc, Icons.Outlined.Shield),
    PrivacyChoice(PrivacyPrefs.ETP_LEVEL_STRICT, R.string.etp_level_strict, R.string.etp_level_strict_desc, Icons.Outlined.GppGood)
)

private val CookieChoices = listOf(
    PrivacyChoice(PrivacyPrefs.COOKIE_POLICY_ISOLATE, R.string.cookie_policy_isolate, R.string.cookie_policy_isolate_desc, Icons.Outlined.Inventory2),
    PrivacyChoice(PrivacyPrefs.COOKIE_POLICY_BLOCK_THIRD_PARTY, R.string.block_third_party_cookies, R.string.cookie_policy_block_desc, Icons.Outlined.Block),
    PrivacyChoice(PrivacyPrefs.COOKIE_POLICY_ALLOW_ALL, R.string.cookie_policy_allow_all, R.string.cookie_policy_allow_all_desc, Icons.Outlined.Cookie)
)

private val DnsChoices = listOf(
    PrivacyChoice(SecureDns.MODE_OFF, R.string.secure_dns_mode_off, R.string.secure_dns_mode_off_desc, Icons.Outlined.Dns),
    PrivacyChoice(SecureDns.MODE_INCREASED, R.string.secure_dns_mode_increased, R.string.secure_dns_mode_increased_desc, Icons.Outlined.Security),
    PrivacyChoice(SecureDns.MODE_MAX, R.string.secure_dns_mode_max, R.string.secure_dns_mode_max_desc, Icons.Outlined.VerifiedUser)
)

@Composable
internal fun SetupPrivacyPage(state: SetupUiState, metrics: PageMetrics, scrollState: ScrollState) {
    val haptics = rememberSetupHaptics()
    SetupPage(metrics, scrollState, hero = { PrivacyHero(it) }) {
        PageHeader(stringResource(R.string.setup_privacy_title), stringResource(R.string.setup_privacy_body), metrics)
        Spacer(Modifier.height(24.dp))
        SectionTitle(stringResource(R.string.etp_title).uppercase(), metrics, Modifier.rise(2))
        Spacer(Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .rise(3)
        ) {
            TrackingChoices.forEach { choice ->
                val selected = state.etpLevel == choice.key
                ChoiceCard(
                    label = stringResource(choice.titleRes),
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            haptics.tick()
                            state.etpLevel = choice.key
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    IconTile(choice.icon, selected = selected, size = 44.dp)
                }
            }
        }
        Crossfade(targetState = state.etpLevel, label = "etp_caption", modifier = Modifier.rise(3)) { key ->
            TrackingChoices.firstOrNull { it.key == key }?.let { SetupCaptionText(stringResource(it.descRes), metrics) }
        }
        Spacer(Modifier.height(22.dp))
        SectionTitle(stringResource(R.string.cookie_policy_title).uppercase(), metrics, Modifier.rise(4))
        Spacer(Modifier.height(6.dp))
        CookieChoices.forEachIndexed { i, choice ->
            OptionRow(
                icon = choice.icon,
                title = stringResource(choice.titleRes),
                body = stringResource(choice.descRes),
                selected = state.cookiePolicy == choice.key,
                isDefault = choice.key == PrivacyPrefs.DEFAULT_COOKIE_POLICY,
                onClick = {
                    if (state.cookiePolicy != choice.key) {
                        haptics.tick()
                        state.cookiePolicy = choice.key
                    }
                },
                modifier = Modifier.rise(5 + i)
            )
        }
        Spacer(Modifier.height(22.dp))
        SectionTitle(stringResource(R.string.setup_section_anti_tracking).uppercase(), metrics, Modifier.rise(8))
        Spacer(Modifier.height(10.dp))
        SetupToggleCard(
            icon = Icons.Outlined.Fingerprint,
            title = stringResource(R.string.fingerprinting_protection_title),
            body = stringResource(R.string.fingerprinting_protection_summary),
            checked = state.fingerprintingProtection,
            onCheckedChange = {
                haptics.toggle(it)
                state.fingerprintingProtection = it
            },
            modifier = Modifier.rise(9)
        )
        Spacer(Modifier.height(10.dp))
        SetupToggleCard(
            icon = Icons.Outlined.LinkOff,
            title = stringResource(R.string.strip_tracking_params_title),
            body = stringResource(R.string.strip_tracking_params_summary),
            checked = state.stripTrackingParams,
            onCheckedChange = {
                haptics.toggle(it)
                state.stripTrackingParams = it
            },
            modifier = Modifier.rise(10)
        )
        Spacer(Modifier.height(10.dp))
        SetupToggleCard(
            icon = Icons.Outlined.PrivacyTip,
            title = stringResource(R.string.global_privacy_control_title),
            body = stringResource(R.string.global_privacy_control_summary),
            checked = state.globalPrivacyControl,
            onCheckedChange = {
                haptics.toggle(it)
                state.globalPrivacyControl = it
            },
            modifier = Modifier.rise(11)
        )
        Spacer(Modifier.height(10.dp))
        SetupToggleCard(
            icon = Icons.Outlined.VisibilityOff,
            title = stringResource(R.string.do_not_track_title),
            body = stringResource(R.string.do_not_track_summary),
            checked = state.doNotTrack,
            onCheckedChange = {
                haptics.toggle(it)
                state.doNotTrack = it
            },
            modifier = Modifier.rise(12)
        )
    }
}

@Composable
internal fun SetupSecurityPage(state: SetupUiState, metrics: PageMetrics, scrollState: ScrollState) {
    val haptics = rememberSetupHaptics()
    val maxTitle = stringResource(R.string.secure_dns_max_warning_title)
    val maxMessage = stringResource(R.string.secure_dns_max_warning_message)
    val maxPositive = stringResource(R.string.action_enable_anyway)
    val maxNegative = stringResource(R.string.action_cancel)
    SetupPage(metrics, scrollState, hero = { SecurityHero(it) }) {
        PageHeader(stringResource(R.string.setup_security_title), stringResource(R.string.setup_security_body), metrics)
        Spacer(Modifier.height(24.dp))
        SetupToggleCard(
            icon = Icons.Outlined.Lock,
            title = stringResource(R.string.https_only),
            body = stringResource(R.string.https_only_summary),
            checked = state.httpsOnly,
            onCheckedChange = {
                haptics.toggle(it)
                state.httpsOnly = it
            },
            modifier = Modifier.rise(2)
        )
        Spacer(Modifier.height(22.dp))
        SectionTitle(stringResource(R.string.secure_dns_title).uppercase(), metrics, Modifier.rise(3))
        Spacer(Modifier.height(6.dp))
        DnsChoices.forEachIndexed { i, choice ->
            OptionRow(
                icon = choice.icon,
                title = stringResource(choice.titleRes),
                body = stringResource(choice.descRes),
                selected = state.secureDnsMode == choice.key,
                isDefault = choice.key == SecureDns.MODE_OFF,
                onClick = {
                    if (state.secureDnsMode != choice.key) {
                        haptics.tick()
                        if (choice.key == SecureDns.MODE_MAX) {
                            state.confirmDialogConfig = ConfirmDialogConfig(
                                title = maxTitle,
                                message = maxMessage,
                                positiveLabel = maxPositive,
                                onPositive = { state.secureDnsMode = SecureDns.MODE_MAX },
                                negativeLabel = maxNegative
                            )
                        } else {
                            state.secureDnsMode = choice.key
                        }
                    }
                },
                modifier = Modifier.rise(4 + i)
            )
        }
        Spacer(Modifier.height(22.dp))
        SectionTitle(stringResource(R.string.setup_section_more_protection).uppercase(), metrics, Modifier.rise(7))
        Spacer(Modifier.height(10.dp))
        SetupToggleCard(
            icon = Icons.Outlined.Router,
            title = stringResource(R.string.webrtc_protection_title),
            body = stringResource(R.string.webrtc_protection_summary),
            checked = state.webRtcProtection,
            onCheckedChange = {
                haptics.toggle(it)
                state.webRtcProtection = it
            },
            modifier = Modifier.rise(8)
        )
        Spacer(Modifier.height(10.dp))
        SetupToggleCard(
            icon = Icons.Outlined.DeleteSweep,
            title = stringResource(R.string.clear_on_exit_title),
            body = stringResource(R.string.clear_on_exit_summary),
            checked = state.clearOnExit,
            onCheckedChange = {
                haptics.toggle(it)
                state.clearOnExit = it
            },
            modifier = Modifier.rise(9)
        )
        Spacer(Modifier.height(10.dp))
        SetupToggleCard(
            icon = Icons.Outlined.ScreenLockPortrait,
            title = stringResource(R.string.screenshot_blocking_title),
            body = stringResource(R.string.screenshot_blocking_summary),
            checked = state.screenshotBlocking,
            onCheckedChange = {
                haptics.toggle(it)
                state.screenshotBlocking = it
            },
            modifier = Modifier.rise(10)
        )
    }
}

@Composable
internal fun OptionRow(
    icon: ImageVector,
    title: String,
    body: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDefault: Boolean = false
) {
    val colors = LocalClintColors.current
    val container by animateColorAsState(
        if (selected) colors.primary.copy(alpha = 0.14f) else colors.cardBackground,
        label = "option_bg"
    )
    val stroke by animateColorAsState(if (selected) colors.primary else colors.divider, label = "option_stroke")
    val mark by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.5f, stiffness = 320f), label = "option_mark")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(if (selected) 2.dp else 1.dp, stroke, RoundedCornerShape(20.dp))
            .background(container)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 14.dp)
    ) {
        IconTile(icon, selected = selected)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isDefault) {
                    Spacer(Modifier.width(8.dp))
                    DefaultChip(stringResource(R.string.default_label), colors.primary)
                }
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.secondaryText
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) colors.primary else colors.divider, CircleShape)
                .drawBehind { drawCircle(colors.primary, size.minDimension / 2f * mark) }
        ) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer {
                        scaleX = mark
                        scaleY = mark
                        alpha = mark.coerceIn(0f, 1f)
                    }
            )
        }
    }
}
