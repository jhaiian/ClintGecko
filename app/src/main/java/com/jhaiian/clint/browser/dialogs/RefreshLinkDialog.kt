package com.jhaiian.clint.browser.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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

data class RefreshLinkDialogRequest(
    val existingFilename: String,
    val onUpdateExisting: () -> Unit,
    val onAddNew: () -> Unit
)

@Composable
internal fun RefreshLinkDialog(request: RefreshLinkDialogRequest, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current

    var updateExisting by remember(request) { mutableStateOf(true) }

    ClintDialog(
        title = stringResource(R.string.refresh_link_dialog_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_close), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = {
                    onDismiss()
                    if (updateExisting) request.onUpdateExisting() else request.onAddNew()
                }) {
                    Text(stringResource(R.string.action_ok), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Column {
            Text(
                stringResource(R.string.refresh_link_dialog_message, request.existingFilename),
                color = colors.onSurface.copy(alpha = 0.75f),
                fontSize = 14.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Column(Modifier.padding(top = 16.dp)) {
                RefreshLinkRadioRow(
                    selected = updateExisting,
                    text = stringResource(R.string.refresh_link_option_update_existing),
                    onClick = { updateExisting = true }
                )
                RefreshLinkRadioRow(
                    selected = !updateExisting,
                    text = stringResource(R.string.refresh_link_option_add_new),
                    onClick = { updateExisting = false }
                )
            }
        }
    }
}

@Composable
private fun RefreshLinkRadioRow(selected: Boolean, text: String, onClick: () -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier.fillMaxWidth().height(48.dp).clickable(onClick = onClick).padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = colors.primary))
        Text(text, color = colors.onSurface, fontSize = 15.sp, modifier = Modifier.padding(start = 4.dp))
    }
}
