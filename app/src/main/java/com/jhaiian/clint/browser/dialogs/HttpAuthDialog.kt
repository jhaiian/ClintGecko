package com.jhaiian.clint.browser.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintDialogActionFooter
import com.jhaiian.clint.ui.ClintOutlinedTextField
import com.jhaiian.clint.ui.theme.LocalClintColors

class HttpAuthRequest(
    val host: String,
    val realm: String,
    val isSecure: Boolean,
    private val onSignIn: (String, String) -> Unit,
    private val onCancel: () -> Unit
) {
    private var completed = false

    fun signIn(username: String, password: String) {
        if (completed) return
        completed = true
        onSignIn(username, password)
    }

    fun cancel() {
        if (completed) return
        completed = true
        onCancel()
    }
}

@Composable
internal fun HttpAuthDialog(request: HttpAuthRequest, hideStatusBar: Boolean, hideSystemNavigation: Boolean, onDismiss: () -> Unit) {
    val colors = LocalClintColors.current
    var username by remember(request) { mutableStateOf("") }
    var password by remember(request) { mutableStateOf("") }
    var passwordVisible by remember(request) { mutableStateOf(false) }
    val focusRequester = remember(request) { FocusRequester() }

    DisposableEffect(request) { onDispose { request.cancel() } }
    LaunchedEffect(request) { runCatching { focusRequester.requestFocus() } }

    ClintDialog(
        title = stringResource(R.string.http_auth_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = { request.cancel(); onDismiss() },
        footer = {
            ClintDialogActionFooter(
                onCancel = { request.cancel(); onDismiss() },
                positiveLabel = stringResource(R.string.http_auth_sign_in),
                onPositive = { request.signIn(username, password); onDismiss() }
            )
        }
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                stringResource(R.string.http_auth_message, request.host),
                color = colors.onSurface,
                fontSize = 15.sp,
                modifier = Modifier.fillMaxWidth()
            )
            if (request.realm.isNotBlank()) {
                Text(
                    request.realm,
                    color = colors.secondaryText,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
            if (!request.isSecure) {
                Surface(
                    color = colors.colorError.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Text(
                        stringResource(R.string.http_auth_insecure),
                        color = colors.colorError,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            ClintOutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(stringResource(R.string.http_auth_username)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
            )
            Spacer(Modifier.height(12.dp))
            ClintOutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.http_auth_password)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(if (passwordVisible) R.string.http_auth_hide_password else R.string.http_auth_show_password),
                            tint = colors.iconTint
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}
