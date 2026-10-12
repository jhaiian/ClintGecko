package com.jhaiian.clint.browser.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.theme.LocalClintColors

data class PopupAlertRequest(
    val sourceHost: String,
    val newUrl: String,
    val onAllow: () -> Unit,
    val onDeny: () -> Unit = {}
)

@Composable
internal fun PopupAlertDialog(request: PopupAlertRequest, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val context = LocalContext.current

    ClintDialog(
        title = stringResource(R.string.popup_alert_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onDismiss(); request.onDeny() }) {
                    Text(stringResource(R.string.action_no), color = colors.primary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = { onDismiss(); request.onAllow() }) {
                    Text(stringResource(R.string.action_yes), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {

            AndroidView(
                factory = { ctx ->
                    android.widget.TextView(ctx).apply {
                        setTextColor(colors.onSurface.toArgb())
                        textSize = 14f
                    }
                },
                update = { tv ->
                    tv.setTextColor(colors.onSurface.toArgb())
                    tv.text = android.text.Html.fromHtml(
                        context.getString(R.string.popup_alert_message, request.sourceHost),
                        android.text.Html.FROM_HTML_MODE_COMPACT
                    )
                }
            )
            Text(
                stringResource(R.string.popup_alert_opens_label),
                color = colors.primary,
                fontSize = 11.sp,
                letterSpacing = 0.1.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp)
            )
            Surface(color = colors.cardBackground, shape = RoundedCornerShape(14.dp)) {
                Text(
                    request.newUrl,
                    color = colors.onSurface,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}
