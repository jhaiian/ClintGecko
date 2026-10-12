package com.jhaiian.clint.browser.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.theme.LocalClintColors

class HttpsOnlyRequest(
    val host: String,
    private val onProceed: () -> Unit,
    private val onCancel: () -> Unit
) {
    private var completed = false

    fun proceed() {
        if (completed) return
        completed = true
        onProceed()
    }

    fun cancel() {
        if (completed) return
        completed = true
        onCancel()
    }
}

@Composable
internal fun HttpsOnlyDialog(request: HttpsOnlyRequest, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current

    DisposableEffect(request) { onDispose { request.cancel() } }

    ClintDialog(
        title = stringResource(R.string.https_only_blocked_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = { request.cancel(); onDismiss() },
        footer = {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = { request.proceed(); onDismiss() },
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        stringResource(R.string.https_only_blocked_proceed),
                        color = colors.colorError,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.End
                    )
                }
                TextButton(onClick = { request.cancel(); onDismiss() }) {
                    Text(stringResource(R.string.action_cancel), color = colors.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                stringResource(R.string.https_only_blocked_message, request.host),
                color = colors.onSurface,
                fontSize = 15.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
