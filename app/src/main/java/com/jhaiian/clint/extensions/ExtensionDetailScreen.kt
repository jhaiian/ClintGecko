package com.jhaiian.clint.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.common.RowDivider
import com.jhaiian.clint.settings.common.SettingsSection
import com.jhaiian.clint.ui.AdaptiveWidthContainer
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintDialogActionFooter
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.theme.LocalClintColors
import java.text.DateFormat
import java.time.Instant
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExtensionDetailScreen(
    info: ExtensionInfo,
    maxContentWidth: Dp?,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onBack: () -> Unit,
    onRemoved: () -> Unit
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    var confirmRemove by remember { mutableStateOf(false) }
    val busy = info.id in ClintExtensions.busyIds
    val permissions = ExtensionPermissions.visible(info.grantedPermissions)
    val hasAccess = permissions.isNotEmpty() || ExtensionPermissions.hostAccess(info.grantedOrigins).let { it.allSites || it.domains.isNotEmpty() }
    val risk = ExtensionPermissions.risk(info.grantedPermissions, info.grantedOrigins)
    val optionalPerms = ExtensionPermissions.visible(info.optionalPermissions)
    val optionalHosts = info.optionalOrigins

    Surface(color = colors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Surface(color = colors.surface, shadowElevation = 4.dp, modifier = Modifier.statusBarsPadding()) {
                Row(
                    Modifier.fillMaxWidth().height(56.dp).padding(start = 4.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.onSurface)
                    }
                    Text(
                        info.name,
                        color = colors.onSurface,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                }
            }
            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Box(Modifier.weight(1f).fillMaxWidth()) {
                AdaptiveWidthContainer(maxContentWidth) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                        DetailHeader(info, busy)
                        Spacer(Modifier.height(14.dp))

                        if (info.description.isNotBlank()) {
                            SettingsSection(colors.cardBackground, bottomSpacing = 14.dp) {
                                Text(
                                    info.description,
                                    color = colors.onSurface,
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        SectionTitle(stringResource(R.string.ext_section_access))
                        SettingsSection(colors.cardBackground, bottomSpacing = 14.dp) {
                            Column(Modifier.padding(16.dp)) {
                                RiskMeter(risk)
                                Text(
                                    riskLabel(risk),
                                    color = riskColor(risk),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 10.dp)
                                )
                                Text(
                                    riskDescription(risk),
                                    color = colors.secondaryText,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            if (hasAccess) {
                                RowDivider(colors.divider)
                                permissions.forEach { permission ->
                                    val spec = ExtensionPermissions.spec(permission)
                                    PermissionLine(spec.icon, permissionText(permission), spec.risk)
                                }
                                HostAccessLine(info.grantedOrigins)
                            } else {
                                RowDivider(colors.divider)
                                Text(
                                    stringResource(R.string.ext_perm_none),
                                    color = colors.secondaryText,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        if (info.dataCollection.isNotEmpty()) {
                            SectionTitle(stringResource(R.string.ext_section_data))
                            SettingsSection(colors.cardBackground, bottomSpacing = 14.dp) {
                                DataCollectionLines(info.dataCollection)
                            }
                        }

                        if (optionalPerms.isNotEmpty() || optionalHosts.isNotEmpty()) {
                            SectionTitle(stringResource(R.string.ext_section_optional))
                            SettingsSection(colors.cardBackground, bottomSpacing = 14.dp) {
                                Text(
                                    stringResource(R.string.ext_section_optional_hint),
                                    color = colors.secondaryText,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp)
                                )
                                optionalPerms.forEach { permission ->
                                    val spec = ExtensionPermissions.spec(permission)
                                    val granted = permission in info.grantedOptionalPermissions
                                    PermissionLine(
                                        icon = spec.icon,
                                        text = permissionText(permission),
                                        risk = spec.risk,
                                        modifier = Modifier.clickable(enabled = !busy) {
                                            ClintExtensions.setOptionalAccess(info, permission, null, !granted)
                                        },
                                        trailing = { ClintSwitch(checked = granted) }
                                    )
                                }
                                optionalHosts.forEach { origin ->
                                    val granted = origin in info.grantedOptionalOrigins
                                    PermissionLine(
                                        icon = Icons.Filled.Language,
                                        text = ExtensionPermissions.hostAccess(listOf(origin)).let { access ->
                                            if (access.allSites) stringResource(R.string.ext_sites_all) else access.domains.firstOrNull() ?: origin
                                        },
                                        risk = if (ExtensionPermissions.hostAccess(listOf(origin)).allSites) ExtensionRisk.HIGH else ExtensionRisk.MEDIUM,
                                        modifier = Modifier.clickable(enabled = !busy) {
                                            ClintExtensions.setOptionalAccess(info, null, origin, !granted)
                                        },
                                        trailing = { ClintSwitch(checked = granted) }
                                    )
                                }
                            }
                        }

                        SectionTitle(stringResource(R.string.ext_section_settings))
                        SettingsSection(colors.cardBackground, bottomSpacing = 14.dp) {
                            ActionRow(
                                icon = Icons.Filled.VisibilityOff,
                                title = stringResource(R.string.ext_detail_private),
                                summary = stringResource(R.string.ext_detail_private_summary),
                                onClick = { ClintExtensions.setAllowedInPrivate(info, !info.allowedInPrivate) },
                                enabled = !busy,
                                trailing = { ClintSwitch(checked = info.allowedInPrivate) }
                            )
                            if (info.optionsUrl != null) {
                                RowDivider(colors.divider)
                                ActionRow(
                                    icon = Icons.Filled.Settings,
                                    title = stringResource(R.string.ext_detail_options),
                                    summary = stringResource(R.string.ext_detail_options_summary),
                                    onClick = { ClintExtensions.showOptions(info) },
                                    enabled = info.status == ExtensionStatus.ON
                                )
                            }
                            if (!info.temporary) {
                                RowDivider(colors.divider)
                                ActionRow(
                                    icon = Icons.Filled.Refresh,
                                    title = stringResource(R.string.ext_detail_update),
                                    summary = stringResource(R.string.ext_detail_update_summary, info.version),
                                    onClick = { ClintExtensions.checkUpdate(info) },
                                    enabled = !busy,
                                    trailing = {
                                        if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.5.dp, color = colors.primary)
                                    }
                                )
                            }
                            info.homepageUrl?.let { link ->
                                RowDivider(colors.divider)
                                ActionRow(
                                    icon = Icons.Filled.Home,
                                    title = stringResource(R.string.ext_detail_homepage),
                                    summary = link.removePrefix("https://").removePrefix("http://").trimEnd('/'),
                                    onClick = { openLink(context, link) },
                                    trailing = { OpenIcon() }
                                )
                            }
                            info.amoUrl?.let { link ->
                                RowDivider(colors.divider)
                                ActionRow(
                                    icon = Icons.Filled.Storefront,
                                    title = stringResource(R.string.ext_detail_store),
                                    summary = stringResource(R.string.ext_detail_store_summary),
                                    onClick = { openLink(context, link) },
                                    trailing = { OpenIcon() }
                                )
                            }
                        }

                        SectionTitle(stringResource(R.string.ext_section_about))
                        SettingsSection(colors.cardBackground, bottomSpacing = 14.dp) {
                            AboutRow(stringResource(R.string.ext_about_version), info.version)
                            RowDivider(colors.divider)
                            AboutRow(stringResource(R.string.ext_about_id), info.id)
                            RowDivider(colors.divider)
                            AboutRow(stringResource(R.string.ext_about_signature), signatureText(info.signedState))
                            formatDate(info.updateDate)?.let {
                                RowDivider(colors.divider)
                                AboutRow(stringResource(R.string.ext_about_updated), it)
                            }
                        }

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.colorError.copy(alpha = 0.12f))
                                .clickable(enabled = !busy) { confirmRemove = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = colors.colorError, modifier = Modifier.size(22.dp))
                            Text(
                                stringResource(R.string.ext_detail_remove),
                                color = colors.colorError,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                    }
                }
            }
        }
    }

    if (confirmRemove) {
        ClintDialog(
            title = stringResource(R.string.ext_remove_title, info.name),
            hideStatusBar = hideStatusBar,
            hideSystemNavigation = hideSystemNavigation,
            onDismiss = { confirmRemove = false },
            footer = {
                ClintDialogActionFooter(
                    onCancel = { confirmRemove = false },
                    positiveLabel = stringResource(R.string.ext_remove_action),
                    onPositive = {
                        confirmRemove = false
                        ClintExtensions.uninstall(info)
                        onRemoved()
                    }
                )
            }
        ) {
            Text(
                stringResource(R.string.ext_remove_message),
                color = colors.secondaryText,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHeader(info: ExtensionInfo, busy: Boolean) {
    val colors = LocalClintColors.current
    val tint = riskColor(info.risk)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    listOf(lerp(colors.cardBackground, tint, 0.20f), colors.cardBackground)
                )
            )
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ExtensionIconTile(info, 76.dp)
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(
                    info.name,
                    color = colors.onSurface,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    buildString {
                        append(stringResource(R.string.ext_version_fmt, info.version))
                        info.author?.let { append(" · ").append(stringResource(R.string.ext_by_fmt, it)) }
                    },
                    color = colors.secondaryText,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatusPill(riskLabel(info.risk), tint)
            statusText(info.status)?.let { text ->
                StatusPill(text, if (info.status == ExtensionStatus.OFF) colors.secondaryText else colors.colorError, dot = false)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.background.copy(alpha = 0.55f))
                .clickable(enabled = info.canToggle && !busy) { ClintExtensions.setEnabled(info, !info.enabled) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.ext_detail_enabled),
                    color = colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    stringResource(if (info.enabled) R.string.ext_detail_enabled_on else R.string.ext_detail_enabled_off),
                    color = colors.secondaryText,
                    fontSize = 12.5.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (busy) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = colors.primary)
            } else {
                ClintSwitch(checked = info.enabled)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val colors = LocalClintColors.current
    Text(
        text,
        color = colors.primary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
    )
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    trailing: @Composable () -> Unit = {}
) {
    val colors = LocalClintColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) colors.iconTint else colors.secondaryText.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp)
        )
        Column(Modifier.weight(1f).padding(start = 16.dp, end = 12.dp)) {
            Text(
                title,
                color = if (enabled) colors.onSurface else colors.secondaryText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                summary,
                color = colors.secondaryText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        trailing()
    }
}

@Composable
private fun OpenIcon() {
    Icon(
        Icons.AutoMirrored.Filled.OpenInNew,
        contentDescription = null,
        tint = LocalClintColors.current.secondaryText,
        modifier = Modifier.size(18.dp)
    )
}

@Composable
private fun AboutRow(label: String, value: String) {
    val colors = LocalClintColors.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.Top) {
        Text(label, color = colors.secondaryText, fontSize = 14.sp, modifier = Modifier.padding(end = 16.dp))
        Text(
            value,
            color = colors.onSurface,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun signatureText(state: Int): String = stringResource(
    when (state) {
        org.mozilla.geckoview.WebExtension.SignedStateFlags.SIGNED,
        org.mozilla.geckoview.WebExtension.SignedStateFlags.PRELIMINARY,
        org.mozilla.geckoview.WebExtension.SignedStateFlags.SYSTEM,
        org.mozilla.geckoview.WebExtension.SignedStateFlags.PRIVILEGED -> R.string.ext_sig_signed
        org.mozilla.geckoview.WebExtension.SignedStateFlags.MISSING -> R.string.ext_sig_unsigned
        else -> R.string.ext_sig_unknown
    }
)

private fun formatDate(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    val millis = runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
        ?: raw.toLongOrNull()
        ?: return null
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
}

private fun openLink(context: Context, url: String) {
    ClintExtensions.openInTab(url)
    (context as? android.app.Activity)?.finish()
}
