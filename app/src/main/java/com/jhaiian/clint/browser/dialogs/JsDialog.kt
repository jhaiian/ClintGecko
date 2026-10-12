package com.jhaiian.clint.browser.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintDialogActionFooter
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.theme.LocalClintColors

enum class JsDialogType { Alert, Confirm, Prompt, BeforeUnload }

class JsDialogRequest(
    val type: JsDialogType,
    val url: String,
    val message: String,
    val defaultValue: String,
    private val onConfirm: (String) -> Unit,
    private val onCancel: () -> Unit
) {
    private var completed = false

    fun confirm(value: String = "") {
        if (completed) return
        completed = true
        onConfirm(value)
    }

    fun cancel() {
        if (completed) return
        completed = true
        onCancel()
    }
}

@Composable
internal fun JsDialog(request: JsDialogRequest, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    val origin = remember(request) {
        android.net.Uri.parse(request.url).host?.takeIf { it.isNotEmpty() } ?: request.url
    }
    var text by remember(request) { mutableStateOf(request.defaultValue) }
    val focusRequester = remember(request) { FocusRequester() }

    DisposableEffect(request) { onDispose { request.cancel() } }

    if (request.type == JsDialogType.Prompt) {
        LaunchedEffect(request) { runCatching { focusRequester.requestFocus() } }
    }

    ClintDialog(
        title = if (request.type == JsDialogType.BeforeUnload) stringResource(R.string.js_before_unload_title) else stringResource(R.string.js_dialog_title, origin),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = { request.cancel(); onDismiss() },
        footer = {
            if (request.type == JsDialogType.Alert) {
                Row(Modifier.fillMaxWidth().padding(end = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { request.confirm(); onDismiss() }) {
                        Text(stringResource(R.string.action_ok), color = colors.primary, fontWeight = FontWeight.Medium)
                    }
                }
            } else if (request.type == JsDialogType.BeforeUnload) {
                ClintDialogActionFooter(
                    onCancel = { request.cancel(); onDismiss() },
                    cancelLabel = stringResource(R.string.js_before_unload_stay),
                    positiveLabel = stringResource(R.string.js_before_unload_leave),
                    onPositive = { request.confirm(); onDismiss() }
                )
            } else {
                ClintDialogActionFooter(
                    onCancel = { request.cancel(); onDismiss() },
                    positiveLabel = stringResource(R.string.action_ok),
                    onPositive = { request.confirm(text); onDismiss() }
                )
            }
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                if (request.type == JsDialogType.BeforeUnload) stringResource(R.string.js_before_unload_message) else request.message,
                color = colors.onSurface,
                fontSize = 15.sp,
                modifier = Modifier.fillMaxWidth()
            )
            if (request.type == JsDialogType.Prompt) {
                Spacer(Modifier.height(16.dp))
                ClintOutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
