package com.jhaiian.clint.settings.site

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.ClintRadioButton
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
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun AddSiteDialog(
    title: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    showStateChoice: Boolean,
    onConfirm: (origin: String, allowed: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var origin by remember { mutableStateOf("") }
    var allowed by remember { mutableStateOf(true) }

    ClintDialog(
        title = title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { if (origin.isNotBlank()) onConfirm(origin.trim(), allowed) }) {
                    Text(stringResource(R.string.site_permission_add), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        ClintOutlinedTextField(
            value = origin,
            onValueChange = { origin = it },
            label = { Text(stringResource(R.string.site_permission_website_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (showStateChoice) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth().clickable { allowed = true }.padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ClintRadioButton(selected = allowed)
                    Text(
                        stringResource(R.string.site_permission_state_allowed),
                        color = colors.onSurface, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Row(
                    Modifier.fillMaxWidth().clickable { allowed = false }.padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ClintRadioButton(selected = !allowed)
                    Text(
                        stringResource(R.string.site_permission_state_denied),
                        color = colors.onSurface, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddSiteModeDialog(
    title: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    options: List<Pair<String, Int>>,
    onConfirm: (origin: String, state: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    var origin by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(options.first().first) }

    ClintDialog(
        title = title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { if (origin.isNotBlank()) onConfirm(origin.trim(), selected) }) {
                    Text(stringResource(R.string.site_permission_add), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        ClintOutlinedTextField(
            value = origin,
            onValueChange = { origin = it },
            label = { Text(stringResource(R.string.site_permission_website_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            options.forEach { (value, labelRes) ->
                Row(
                    Modifier.fillMaxWidth().clickable { selected = value }.padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ClintRadioButton(selected = selected == value)
                    Text(
                        stringResource(labelRes),
                        color = colors.onSurface, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SiteListDeleteConfirmDialog(
    title: String,
    message: String,
    hideStatusBar: Boolean, hideSystemNavigation: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    ClintDialog(
        title = title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onDismiss(); onConfirm() }) {
                    Text(stringResource(R.string.history_delete_selected), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Text(message, color = colors.onSurface, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}
