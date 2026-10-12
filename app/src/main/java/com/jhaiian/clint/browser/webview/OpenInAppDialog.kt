package com.jhaiian.clint.browser.webview

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.viewinterop.AndroidView
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintCheckbox
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.theme.LocalClintColors

data class OpenInAppMatch(val label: String, val icon: Drawable?, val packageName: String)

data class OpenInAppRequest(
    val host: String,
    val matches: List<OpenInAppMatch>,
    val showRemember: Boolean,
    val onStayHere: (remember: Boolean) -> Unit,
    val onOpenApp: (packageName: String, remember: Boolean) -> Unit
)

@Composable
internal fun OpenInAppDialog(request: OpenInAppRequest, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val single = request.matches.singleOrNull()
    var dontAsk by remember(request) { mutableStateOf(false) }

    ClintDialog(
        title = stringResource(if (single != null) R.string.open_in_app_dialog_title else R.string.open_in_app_chooser_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        cancelable = false,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onDismiss(); request.onStayHere(dontAsk && request.showRemember) }) {
                    Text(stringResource(R.string.open_in_app_dialog_stay_here), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                if (single != null) {
                    TextButton(onClick = { onDismiss(); request.onOpenApp(single.packageName, dontAsk && request.showRemember) }) {
                        Text(stringResource(R.string.open_in_app_dialog_confirm), color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    ) {
        if (single != null) {
            Column {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (single.icon != null) AppIcon(single.icon, sizeDp = 40, modifier = Modifier.padding(end = 12.dp))
                    Text(
                        stringResource(R.string.open_in_app_dialog_message, request.host, single.label),
                        color = colors.onSurface,
                        fontSize = 14.sp
                    )
                }
                if (request.showRemember) DontAskAgainRow(dontAsk) { dontAsk = it }
            }
        } else {
            Column {
                Text(
                    stringResource(R.string.open_in_app_chooser_message, request.host),
                    color = colors.onSurface,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                if (request.showRemember) DontAskAgainRow(dontAsk) { dontAsk = it }
                request.matches.forEach { match ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { onDismiss(); request.onOpenApp(match.packageName, dontAsk && request.showRemember) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (match.icon != null) AppIcon(match.icon, sizeDp = 32, modifier = Modifier.padding(end = 16.dp))
                        Text(match.label, color = colors.onSurface, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DontAskAgainRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalClintColors.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClintCheckbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(stringResource(R.string.open_in_app_dont_ask_again), color = colors.onSurface, fontSize = 14.sp)
    }
}

@Composable
private fun AppIcon(drawable: Drawable, sizeDp: Int, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx -> ImageView(ctx) },
        update = { it.setImageDrawable(drawable) },
        modifier = modifier.size(sizeDp.dp)
    )
}
