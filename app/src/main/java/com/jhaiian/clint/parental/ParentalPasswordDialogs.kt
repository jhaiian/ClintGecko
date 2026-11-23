package com.jhaiian.clint.parental

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.backuprestore.PasswordVisibilityToggle
import com.jhaiian.clint.ui.ClintDialog
import com.jhaiian.clint.ui.ClintDialogActionFooter
import com.jhaiian.clint.ui.ClintOutlinedPasswordField
import com.jhaiian.clint.ui.status.formatElapsedSeconds
import com.jhaiian.clint.ui.theme.LocalClintColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ParentalPasswordSetRequest(
    val changing: Boolean,
    val onSaved: () -> Unit
)

@Composable
internal fun ParentalPasswordVerifyDialog(
    request: ParentalGateRequest,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit
) {
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var password by remember(request) { mutableStateOf("") }
    var visible by remember(request) { mutableStateOf(false) }
    var checking by remember(request) { mutableStateOf(false) }
    var error by remember(request) { mutableStateOf<String?>(null) }
    val wrongText = stringResource(R.string.parental_password_wrong)

    fun lockedText(remainingMs: Long): String =
        context.getString(R.string.parental_password_locked, formatElapsedSeconds((remainingMs + 999L) / 1000L))

    LaunchedEffect(request) {
        val remaining = withContext(Dispatchers.IO) { ParentalPassword.remainingLockMs(context) }
        if (remaining > 0L) error = lockedText(remaining)
    }

    fun submit() {
        if (checking || password.isEmpty()) return
        checking = true
        val attempt = password
        scope.launch {
            val result = withContext(Dispatchers.Default) { ParentalPassword.verify(context, attempt) }
            checking = false
            when (result) {
                ParentalPassword.VerifyResult.Success -> {
                    onDismiss()
                    request.onConfirm()
                }
                ParentalPassword.VerifyResult.Wrong -> {
                    password = ""
                    error = wrongText
                }
                is ParentalPassword.VerifyResult.Locked -> {
                    password = ""
                    error = lockedText(result.remainingMs)
                }
            }
        }
    }

    ClintDialog(
        title = request.title,
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            ClintDialogActionFooter(
                onCancel = onDismiss,
                positiveLabel = request.positiveLabel,
                onPositive = { submit() },
                positiveEnabled = password.isNotEmpty() && !checking
            )
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(request.message, color = colors.onSurface, fontSize = 14.sp)
            Text(
                stringResource(R.string.parental_password_prompt),
                color = colors.secondaryText, fontSize = 13.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
            ClintOutlinedPasswordField(
                value = password,
                onValueChange = { password = it; error = null },
                passwordVisible = visible,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text(stringResource(R.string.parental_password_field_label)) },
                isError = error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = { PasswordVisibilityToggle(visible) { visible = !visible } }
            )
            error?.let { message ->
                Text(message, color = colors.colorError, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun ParentalPasswordSetDialog(
    request: ParentalPasswordSetRequest?,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit
) {
    if (request == null) return
    val colors = LocalClintColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var password by remember(request) { mutableStateOf("") }
    var confirm by remember(request) { mutableStateOf("") }
    var passwordVisible by remember(request) { mutableStateOf(false) }
    var confirmVisible by remember(request) { mutableStateOf(false) }
    var error by remember(request) { mutableStateOf<String?>(null) }
    var saving by remember(request) { mutableStateOf(false) }
    val tooShortText = stringResource(R.string.parental_password_too_short, ParentalPassword.MIN_LENGTH)
    val mismatchText = stringResource(R.string.parental_password_mismatch)

    fun save() {
        if (saving) return
        val problem = when {
            password.length < ParentalPassword.MIN_LENGTH -> tooShortText
            password != confirm -> mismatchText
            else -> null
        }
        error = problem
        if (problem != null) return
        saving = true
        val chosen = password
        scope.launch {
            withContext(Dispatchers.Default) { ParentalPassword.set(context, chosen) }
            onDismiss()
            request.onSaved()
        }
    }

    ClintDialog(
        title = stringResource(if (request.changing) R.string.parental_password_change_title else R.string.parental_password_set_title),
        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
        onDismiss = onDismiss,
        footer = {
            ClintDialogActionFooter(
                onCancel = onDismiss,
                positiveLabel = stringResource(R.string.action_save),
                onPositive = { save() },
                positiveEnabled = password.isNotEmpty() && confirm.isNotEmpty() && !saving
            )
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.parental_password_set_message), color = colors.secondaryText, fontSize = 13.sp)
            ClintOutlinedPasswordField(
                value = password,
                onValueChange = { password = it; error = null },
                passwordVisible = passwordVisible,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                label = { Text(stringResource(R.string.parental_password_new_label)) },
                isError = error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = { PasswordVisibilityToggle(passwordVisible) { passwordVisible = !passwordVisible } }
            )
            ClintOutlinedPasswordField(
                value = confirm,
                onValueChange = { confirm = it; error = null },
                passwordVisible = confirmVisible,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text(stringResource(R.string.parental_password_confirm_label)) },
                isError = error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = { PasswordVisibilityToggle(confirmVisible) { confirmVisible = !confirmVisible } }
            )
            error?.let { message ->
                Text(message, color = colors.colorError, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
