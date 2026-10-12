package com.jhaiian.clint.ui.listscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.theme.LocalClintColors

data class ConfirmDialogConfig(
    val title: String,
    val message: String,
    val positiveLabel: String,
    val onPositive: () -> Unit = {},
    val negativeLabel: String? = null,
    val onNegative: () -> Unit = {},
    val neutralLabel: String? = null,
    val onNeutral: () -> Unit = {},
    val cancelable: Boolean = true
)

@Composable
fun ConfirmDialogHost(config: ConfirmDialogConfig?, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    if (config == null) return
    val colors = LocalClintColors.current
    ClintDialog(
        title = config.title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        cancelable = config.cancelable,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                config.neutralLabel?.let { label ->
                    TextButton(onClick = { onDismiss(); config.onNeutral() }) {
                        Text(label, color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                }
                config.negativeLabel?.let { label ->
                    TextButton(onClick = { onDismiss(); config.onNegative() }) {
                        Text(label, color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                }
                TextButton(onClick = { onDismiss(); config.onPositive() }) {
                    Text(config.positiveLabel, color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Text(config.message, color = colors.onSurface, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}
