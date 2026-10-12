package com.jhaiian.clint.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.theme.LocalClintColors

data class WebPermissionDialogRequest(
    val title: String,
    val message: String,
    val isIncognito: Boolean,
    val onAllow: (remember: Boolean) -> Unit,
    val onDeny: (remember: Boolean) -> Unit
)

@Composable
internal fun WebPermissionDialog(request: WebPermissionDialogRequest, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current

    var remember by remember(request) { mutableStateOf(true) }

    ClintDialog(
        title = request.title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,

        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onDismiss(); request.onDeny(remember) }) {
                    Text(stringResource(R.string.action_deny), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onDismiss(); request.onAllow(remember) }) {
                    Text(stringResource(R.string.action_allow), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        WebPermissionDialogContent(request, remember) { remember = it }
    }
}

@Composable
private fun ColumnScope.WebPermissionDialogContent(request: WebPermissionDialogRequest, remember: Boolean, onRememberChange: (Boolean) -> Unit) {
    val colors = LocalClintColors.current
    Text(
        request.message,
        color = colors.onSurface,
        fontSize = 14.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
    if (!request.isIncognito) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                .clickable { onRememberChange(!remember) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = remember,
                onCheckedChange = onRememberChange,
                colors = CheckboxDefaults.colors(checkedColor = colors.primary)
            )
            Text(stringResource(R.string.camera_web_request_remember), color = colors.onSurface, fontSize = 14.sp)
        }
    }
}
