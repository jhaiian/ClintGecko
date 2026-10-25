package com.jhaiian.clint.browser.siteinfo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RemoveModerator
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.sheets.LongPressContentMaxWidth
import com.jhaiian.clint.settings.common.dialogSectionBackground
import com.jhaiian.clint.settings.sitepermissions.SitePermissionDatabase
import com.jhaiian.clint.ui.ClintDialogStatusBarEffect
import com.jhaiian.clint.ui.ClintSwitch
import com.jhaiian.clint.ui.listscreen.ListMenuItem
import com.jhaiian.clint.ui.listscreen.PopupShape
import com.jhaiian.clint.ui.rememberClintFavicon
import com.jhaiian.clint.ui.theme.LocalClintColors
import java.text.DateFormat
import java.util.Date

internal data class SiteInfoCertificate(
    val issuedTo: String?,
    val issuedBy: String?,
    val validFrom: Long,
    val validUntil: Long
)

internal data class SiteInfoPermission(val type: String, val state: String)

internal enum class SiteInfoTrackerCategory { SOCIAL, COOKIES, TRACKING_CONTENT, FINGERPRINTERS, CRYPTOMINERS }

internal data class SiteInfoTrackerGroup(val category: SiteInfoTrackerCategory, val origins: List<String>)

internal data class SiteInfoTrackers(val total: Int, val groups: List<SiteInfoTrackerGroup>)

internal data class SiteInfoRequest(
    val url: String,
    val host: String,
    val isSecure: Boolean,
    val isCertificateException: Boolean,
    val certificate: SiteInfoCertificate?,
    val permissions: List<SiteInfoPermission>,
    val canManageCookies: Boolean,
    val trackingProtection: Boolean?,
    val trackers: SiteInfoTrackers? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SiteInfoSheet(
    request: SiteInfoRequest,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onPermissionChange: (type: String, state: String?) -> Unit,
    onResetPermissions: () -> Unit,
    onTrackingProtectionChange: (Boolean) -> Unit,
    onCookiesClick: () -> Unit,
    onSiteSettingsClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val configuration = LocalConfiguration.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxSheetHeight = (configuration.screenHeightDp.dp * 0.8f).coerceAtLeast(320.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.popupBackground,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.divider) }
    ) {
        ClintDialogStatusBarEffect(hideStatusBar, hideSystemNavigation)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = LongPressContentMaxWidth)
                    .heightIn(max = maxSheetHeight)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                SiteInfoHeader(request)
                Spacer(Modifier.height(16.dp))
                ConnectionCard(request)
                Spacer(Modifier.height(12.dp))
                request.trackingProtection?.let { enabled ->
                    TrackingProtectionCard(enabled, request.trackers, onTrackingProtectionChange)
                    Spacer(Modifier.height(12.dp))
                }
                PermissionsCard(request, onPermissionChange, onResetPermissions)
                Spacer(Modifier.height(12.dp))
                SiteInfoCard {
                    if (request.canManageCookies) {
                        SiteInfoActionRow(
                            icon = Icons.Filled.Cookie,
                            title = stringResource(R.string.cookies_control_title),
                            summary = stringResource(R.string.site_info_cookies_desc),
                            onClick = onCookiesClick
                        )
                        SiteInfoDivider()
                    }
                    SiteInfoActionRow(
                        icon = Icons.Filled.Tune,
                        title = stringResource(R.string.site_settings),
                        summary = stringResource(R.string.site_info_site_settings_desc),
                        onClick = onSiteSettingsClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SiteInfoHeader(request: SiteInfoRequest) {
    val colors = LocalClintColors.current
    val favicon = rememberClintFavicon(request.url)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(colors.dialogSectionBackground),
            contentAlignment = Alignment.Center
        ) {
            if (favicon != null) {
                Image(
                    bitmap = favicon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp))
                )
            } else {
                Icon(Icons.Filled.Public, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(24.dp))
            }
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                request.host.removePrefix("www."),
                color = colors.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                request.url,
                color = colors.secondaryText,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun ConnectionCard(request: SiteInfoRequest) {
    val colors = LocalClintColors.current
    var expanded by remember { mutableStateOf(false) }
    val certificate = request.certificate
    val accent = if (request.isSecure) colors.primary else colors.colorError
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "siteInfoCertificateChevron")

    SiteInfoCard {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            SiteInfoIconTile(if (request.isSecure) Icons.Filled.Lock else Icons.Filled.LockOpen, accent)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(
                    stringResource(if (request.isSecure) R.string.site_info_connection_secure else R.string.site_info_connection_not_secure),
                    color = if (request.isSecure) colors.onSurface else colors.colorError,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(if (request.isSecure) R.string.site_info_connection_secure_desc else R.string.site_info_connection_not_secure_desc),
                    color = colors.secondaryText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        if (request.isCertificateException) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.colorErrorContainer)
                    .padding(12.dp)
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = colors.colorOnErrorContainer, modifier = Modifier.size(20.dp))
                Text(
                    stringResource(R.string.site_info_certificate_exception),
                    color = colors.colorOnErrorContainer,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
        if (certificate != null) {
            SiteInfoDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                SiteInfoIconTile(Icons.Filled.Verified, colors.primary)
                Column(Modifier.weight(1f).padding(start = 14.dp, end = 8.dp)) {
                    Text(
                        stringResource(R.string.site_info_certificate),
                        color = colors.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    certificate.issuedBy?.let {
                        Text(
                            stringResource(R.string.site_info_verified_by, it),
                            color = colors.secondaryText,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = colors.iconTint,
                    modifier = Modifier.size(22.dp).rotate(chevronRotation)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(start = 70.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    certificate.issuedTo?.let { CertificateField(stringResource(R.string.site_info_issued_to), it) }
                    certificate.issuedBy?.let { CertificateField(stringResource(R.string.site_info_issued_by), it) }
                    if (certificate.validFrom > 0L) CertificateField(stringResource(R.string.site_info_valid_from), dateFormat.format(Date(certificate.validFrom)))
                    if (certificate.validUntil > 0L) CertificateField(stringResource(R.string.site_info_valid_until), dateFormat.format(Date(certificate.validUntil)))
                }
            }
        }
    }
}

@Composable
private fun TrackingProtectionCard(enabled: Boolean, trackers: SiteInfoTrackers?, onChange: (Boolean) -> Unit) {
    val colors = LocalClintColors.current
    var expanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "siteInfoTrackersChevron")
    SiteInfoCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChange(!enabled) }
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            SiteInfoIconTile(if (enabled) Icons.Filled.Shield else Icons.Filled.RemoveModerator, if (enabled) colors.primary else colors.colorError)
            Column(Modifier.weight(1f).padding(start = 14.dp, end = 8.dp)) {
                Text(stringResource(R.string.etp_title), color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(
                    stringResource(if (enabled) R.string.site_info_etp_on_desc else R.string.site_info_etp_off_desc),
                    color = colors.secondaryText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            ClintSwitch(checked = enabled)
        }
        if (enabled && trackers != null) {
            SiteInfoDivider()
            val hasTrackers = trackers.total > 0
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (hasTrackers) Modifier.clickable { expanded = !expanded } else Modifier)
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                SiteInfoIconTile(Icons.Filled.Block, colors.primary)
                Text(
                    if (hasTrackers) {
                        pluralStringResource(R.plurals.site_info_trackers_blocked, trackers.total, trackers.total)
                    } else {
                        stringResource(R.string.site_info_no_trackers_blocked)
                    },
                    color = colors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f).padding(start = 14.dp, end = 8.dp)
                )
                if (hasTrackers) {
                    Icon(
                        Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = colors.iconTint,
                        modifier = Modifier.size(22.dp).rotate(chevronRotation)
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded && hasTrackers,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(start = 70.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    trackers.groups.forEach { group ->
                        Column {
                            Text(
                                stringResource(trackerCategoryTitleRes(group.category), group.origins.size),
                                color = colors.onSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            group.origins.forEach { origin ->
                                Text(
                                    origin,
                                    color = colors.secondaryText,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun trackerCategoryTitleRes(category: SiteInfoTrackerCategory): Int = when (category) {
    SiteInfoTrackerCategory.SOCIAL -> R.string.site_info_trackers_social
    SiteInfoTrackerCategory.COOKIES -> R.string.site_info_trackers_cookies
    SiteInfoTrackerCategory.TRACKING_CONTENT -> R.string.site_info_trackers_content
    SiteInfoTrackerCategory.FINGERPRINTERS -> R.string.site_info_trackers_fingerprinters
    SiteInfoTrackerCategory.CRYPTOMINERS -> R.string.site_info_trackers_cryptominers
}

@Composable
private fun CertificateField(label: String, value: String) {
    val colors = LocalClintColors.current
    Column {
        Text(label, color = colors.secondaryText, fontSize = 12.sp)
        Text(value, color = colors.onSurface, fontSize = 14.sp, modifier = Modifier.padding(top = 1.dp))
    }
}

@Composable
private fun PermissionsCard(
    request: SiteInfoRequest,
    onPermissionChange: (type: String, state: String?) -> Unit,
    onResetPermissions: () -> Unit
) {
    val colors = LocalClintColors.current
    SiteInfoCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp)
        ) {
            Text(
                stringResource(R.string.site_info_permissions),
                color = colors.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp)
            )
            if (request.permissions.isNotEmpty()) {
                TextButton(onClick = onResetPermissions) {
                    Text(stringResource(R.string.site_info_reset_permissions), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
        if (request.permissions.isEmpty()) {
            Text(
                stringResource(R.string.site_info_no_permissions),
                color = colors.secondaryText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
        } else {
            request.permissions.forEachIndexed { index, permission ->
                if (index > 0) SiteInfoDivider()
                PermissionRow(permission, onPermissionChange)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun PermissionRow(permission: SiteInfoPermission, onPermissionChange: (type: String, state: String?) -> Unit) {
    val colors = LocalClintColors.current
    var menuOpen by remember { mutableStateOf(false) }
    val allowed = permission.state == SitePermissionDatabase.STATE_ALLOW
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { menuOpen = true }
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SiteInfoIconTile(permissionIcon(permission.type), colors.primary)
            Text(
                stringResource(permissionTitleRes(permission.type)),
                color = colors.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f).padding(start = 14.dp, end = 8.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background((if (allowed) colors.primary else colors.colorError).copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    if (allowed) Icons.Filled.Check else Icons.Filled.Block,
                    contentDescription = null,
                    tint = if (allowed) colors.primary else colors.colorError,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    stringResource(if (allowed) R.string.site_permission_state_allowed else R.string.site_permission_state_denied),
                    color = if (allowed) colors.primary else colors.colorError,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            shape = PopupShape,
            containerColor = colors.popupBackground,
            border = BorderStroke(1.dp, colors.popupStroke)
        ) {
            ListMenuItem(Icons.Filled.Check, stringResource(R.string.site_permission_always_allow), checked = allowed) {
                menuOpen = false
                onPermissionChange(permission.type, SitePermissionDatabase.STATE_ALLOW)
            }
            ListMenuItem(Icons.Filled.Block, stringResource(R.string.site_permission_always_deny), checked = !allowed) {
                menuOpen = false
                onPermissionChange(permission.type, SitePermissionDatabase.STATE_DENY)
            }
            HorizontalDivider(color = colors.divider)
            ListMenuItem(Icons.AutoMirrored.Filled.HelpOutline, stringResource(R.string.site_permission_ask_first), checked = false) {
                menuOpen = false
                onPermissionChange(permission.type, null)
            }
        }
    }
}

@Composable
private fun SiteInfoActionRow(icon: ImageVector, title: String, summary: String, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        SiteInfoIconTile(icon, colors.primary)
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 8.dp)) {
            Text(title, color = colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(summary, color = colors.secondaryText, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.iconTint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SiteInfoCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalClintColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.dialogSectionBackground),
        content = content
    )
}

@Composable
private fun SiteInfoIconTile(icon: ImageVector, tint: Color) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun SiteInfoDivider() {
    HorizontalDivider(
        color = LocalClintColors.current.divider.copy(alpha = 0.6f),
        modifier = Modifier.padding(start = 70.dp, end = 16.dp)
    )
}

private fun permissionIcon(type: String): ImageVector = when (type) {
    SitePermissionDatabase.TYPE_CAMERA -> Icons.Filled.Videocam
    SitePermissionDatabase.TYPE_MIC -> Icons.Filled.Mic
    SitePermissionDatabase.TYPE_LOCATION -> Icons.Filled.LocationOn
    SitePermissionDatabase.TYPE_NOTIFICATION -> Icons.Filled.Notifications
    else -> Icons.Filled.ContentPaste
}

private fun permissionTitleRes(type: String): Int = when (type) {
    SitePermissionDatabase.TYPE_CAMERA -> R.string.site_settings_camera
    SitePermissionDatabase.TYPE_MIC -> R.string.site_settings_mic
    SitePermissionDatabase.TYPE_LOCATION -> R.string.site_settings_location
    SitePermissionDatabase.TYPE_NOTIFICATION -> R.string.site_settings_notifications
    else -> R.string.site_settings_clipboard
}
