package com.jhaiian.clint.browser.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.theme.LocalClintColors

data class CookiesControlRequest(
    val host: String,
    val cookieCount: Int,
    val onDelete: () -> Unit,
    val onExport: () -> Unit,
    val onImport: () -> Unit
)

@Composable
internal fun CookiesControlDialog(request: CookiesControlRequest, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val hasCookies = request.cookieCount > 0

    ClintDialog(
        title = stringResource(R.string.cookies_control_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_close), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Column {
            Text(
                request.host,
                color = colors.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Text(
                stringResource(R.string.cookies_control_cookie_count, request.cookieCount),
                color = colors.secondaryText,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp)
            )
            CookiesControlActionRow(
                icon = Icons.Filled.Delete,
                title = stringResource(R.string.cookies_control_delete_cookies),
                summary = stringResource(R.string.cookies_control_delete_cookies_summary),
                enabled = hasCookies,
                onClick = { onDismiss(); request.onDelete() }
            )
            CookiesControlActionRow(
                icon = Icons.Filled.FileUpload,
                title = stringResource(R.string.cookies_control_export_cookies),
                summary = stringResource(R.string.cookies_control_export_cookies_summary),
                enabled = hasCookies,
                onClick = { onDismiss(); request.onExport() }
            )
            CookiesControlActionRow(
                icon = Icons.Filled.FileDownload,
                title = stringResource(R.string.cookies_control_import_cookies),
                summary = stringResource(R.string.cookies_control_import_cookies_summary),
                enabled = true,
                onClick = { onDismiss(); request.onImport() }
            )
        }
    }
}

@Composable
private fun CookiesControlActionRow(icon: ImageVector, title: String, summary: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .alpha(if (enabled) 1f else 0.38f)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = colors.iconTint, modifier = Modifier.size(24.dp))
        Column(Modifier.padding(start = 16.dp)) {
            Text(title, color = colors.onSurface, fontSize = 15.sp)
            Text(summary, color = colors.secondaryText, fontSize = 13.sp, lineHeight = 17.sp)
        }
    }
}

data class CookiesControlExportRequest(
    val host: String,
    val cookies: String,
    val onCopied: () -> Unit
)

data class CookiesControlImportRequest(
    val host: String,
    val onImport: (String) -> Unit
)

@Composable
internal fun CookiesControlExportDialog(request: CookiesControlExportRequest, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val context = LocalContext.current

    ClintDialog(
        title = stringResource(R.string.cookies_control_export_cookies),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_close), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(request.host, request.cookies)
                    clip.description.extras = PersistableBundle().apply {
                        putBoolean("android.content.extra.IS_SENSITIVE", true)
                    }
                    clipboard.setPrimaryClip(clip)
                    request.onCopied()
                }) {
                    Text(stringResource(R.string.cookies_control_copy), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                stringResource(R.string.cookies_control_export_hint, request.host),
                color = colors.secondaryText,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            ClintOutlinedTextField(
                value = request.cookies,
                onValueChange = {},
                readOnly = true,
                minLines = 4,
                maxLines = 10,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun CookiesControlImportDialog(request: CookiesControlImportRequest, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    var text by remember(request) { mutableStateOf("") }

    ClintDialog(
        title = stringResource(R.string.cookies_control_import_cookies),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(
                    enabled = text.isNotBlank(),
                    onClick = {
                        val value = text
                        onDismiss()
                        request.onImport(value)
                    }
                ) {
                    Text(
                        stringResource(R.string.cookies_control_import_action),
                        color = if (text.isNotBlank()) colors.primary else colors.secondaryText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                stringResource(R.string.cookies_control_import_hint, request.host),
                color = colors.secondaryText,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            ClintOutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.cookies_control_import_field)) },
                minLines = 4,
                maxLines = 10,
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val pasted = clipboard.primaryClip?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)?.coerceToText(context)?.toString()
                    if (!pasted.isNullOrEmpty()) text = pasted
                }) {
                    Text(stringResource(R.string.cookies_control_paste), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
