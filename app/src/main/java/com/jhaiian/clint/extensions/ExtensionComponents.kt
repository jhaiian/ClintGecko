package com.jhaiian.clint.extensions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.theme.LocalClintColors

private val tilePalette = listOf(
    Color(0xFF5C6BC0), Color(0xFF26A69A), Color(0xFFEF6C00), Color(0xFFAB47BC),
    Color(0xFF42A5F5), Color(0xFFEC407A), Color(0xFF7CB342), Color(0xFF8D6E63)
)

@Composable
fun riskColor(risk: ExtensionRisk): Color = when (risk) {
    ExtensionRisk.LOW -> Color(0xFF2FA866)
    ExtensionRisk.MEDIUM -> Color(0xFFE5A100)
    ExtensionRisk.HIGH -> LocalClintColors.current.colorError
}

@Composable
fun riskLabel(risk: ExtensionRisk): String = stringResource(
    when (risk) {
        ExtensionRisk.LOW -> R.string.ext_risk_low
        ExtensionRisk.MEDIUM -> R.string.ext_risk_medium
        ExtensionRisk.HIGH -> R.string.ext_risk_high
    }
)

@Composable
fun riskDescription(risk: ExtensionRisk): String = stringResource(
    when (risk) {
        ExtensionRisk.LOW -> R.string.ext_risk_low_desc
        ExtensionRisk.MEDIUM -> R.string.ext_risk_medium_desc
        ExtensionRisk.HIGH -> R.string.ext_risk_high_desc
    }
)

@Composable
fun ExtensionIconTile(bitmap: ImageBitmap?, seed: String, name: String, size: Dp, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val shape = RoundedCornerShape(size * 0.3f)
    if (bitmap != null) {
        Box(
            modifier.size(size).clip(shape).background(colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size * 0.72f)
            )
        }
    } else {
        val tint = tilePalette[(seed.hashCode() and 0x7fffffff) % tilePalette.size]
        Box(modifier.size(size).clip(shape).background(tint), contentAlignment = Alignment.Center) {
            Text(
                name.trim().firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ExtensionIconTile(info: ExtensionInfo, size: Dp, modifier: Modifier = Modifier) {
    ExtensionIconTile(ClintExtensions.icons[info.id], info.id, info.name, size, modifier)
}

@Composable
fun RiskMeter(risk: ExtensionRisk, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val level = when (risk) {
        ExtensionRisk.LOW -> 1
        ExtensionRisk.MEDIUM -> 2
        ExtensionRisk.HIGH -> 3
    }
    val active = riskColor(risk)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        for (i in 1..3) {
            Box(
                Modifier
                    .weight(1f)
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(if (i <= level) active else colors.divider)
            )
        }
    }
}

@Composable
fun StatusPill(text: String, tint: Color, modifier: Modifier = Modifier, dot: Boolean = true) {
    Row(
        modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dot) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(tint))
        }
        Text(
            text,
            color = tint,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = if (dot) 6.dp else 0.dp)
        )
    }
}

@Composable
fun statusText(status: ExtensionStatus): String? = when (status) {
    ExtensionStatus.ON -> null
    ExtensionStatus.OFF -> stringResource(R.string.ext_status_off)
    ExtensionStatus.BLOCKED -> stringResource(R.string.ext_status_blocked)
    ExtensionStatus.SOFT_BLOCKED -> stringResource(R.string.ext_status_soft_blocked)
    ExtensionStatus.UNSIGNED -> stringResource(R.string.ext_status_unsigned)
    ExtensionStatus.INCOMPATIBLE -> stringResource(R.string.ext_status_incompatible)
}

@Composable
fun permissionText(permission: String): String {
    val res = ExtensionPermissions.spec(permission).labelRes
    return if (res != null) stringResource(res) else stringResource(R.string.ext_perm_unknown, permission)
}

@Composable
fun PermissionLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    risk: ExtensionRisk,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    val colors = LocalClintColors.current
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(riskColor(risk).copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = riskColor(risk), modifier = Modifier.size(18.dp))
        }
        Text(
            text,
            color = colors.onSurface,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            modifier = Modifier.weight(1f).padding(start = 14.dp)
        )
        trailing()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HostAccessLine(origins: List<String>, modifier: Modifier = Modifier) {
    val colors = LocalClintColors.current
    val access = ExtensionPermissions.hostAccess(origins)
    if (!access.allSites && access.domains.isEmpty()) return
    val risk = if (access.allSites) ExtensionRisk.HIGH else ExtensionRisk.MEDIUM
    Column(modifier.fillMaxWidth()) {
        PermissionLine(
            icon = Icons.Filled.Public,
            text = if (access.allSites) stringResource(R.string.ext_sites_all) else stringResource(R.string.ext_sites_some),
            risk = risk
        )
        if (!access.allSites) {
            FlowRow(
                Modifier.fillMaxWidth().padding(start = 64.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                access.domains.take(8).forEach { domain ->
                    Text(
                        domain,
                        color = colors.onSurface,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceVariant)
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
                if (access.domains.size > 8) {
                    Text(
                        stringResource(R.string.ext_sites_more, access.domains.size - 8),
                        color = colors.secondaryText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DataCollectionLines(keys: List<String>) {
    keys.forEach { key ->
        val res = ExtensionPermissions.dataLabelRes(key)
        PermissionLine(
            icon = androidx.compose.material.icons.Icons.Filled.Public,
            text = if (res != null) stringResource(res) else stringResource(R.string.ext_perm_unknown, key),
            risk = ExtensionRisk.MEDIUM
        )
    }
}

@Composable
fun ClickableRowModifier(onClick: (() -> Unit)?): Modifier =
    if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
