package com.jhaiian.clint.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jhaiian.clint.ui.theme.LocalClintColors

@Composable
fun ClintOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null
) {
    val state = rememberSyncedTextFieldState(value, onValueChange)
    OutlinedTextField(
        state = state,
        modifier = modifier,
        label = label?.let { content -> { content() } },
        enabled = enabled,
        readOnly = readOnly,
        isError = isError,
        lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(minLines, maxLines),
        keyboardOptions = keyboardOptions,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        colors = clintOutlinedTextFieldColors()
    )
}

@Composable
fun ClintOutlinedPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    passwordVisible: Boolean,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val state = rememberSyncedTextFieldState(value, onValueChange)
    OutlinedSecureTextField(
        state = state,
        modifier = modifier,
        label = label?.let { content -> { content() } },
        isError = isError,
        textObfuscationMode = if (passwordVisible) TextObfuscationMode.Visible else TextObfuscationMode.Hidden,
        keyboardOptions = keyboardOptions,
        trailingIcon = trailingIcon,
        colors = clintOutlinedTextFieldColors()
    )
}

@Composable
private fun clintOutlinedTextFieldColors(): TextFieldColors {
    val colors = LocalClintColors.current
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.onSurface,
        unfocusedTextColor = colors.onSurface,
        disabledTextColor = colors.onSurface,
        errorTextColor = colors.onSurface,
        focusedBorderColor = colors.primary,
        unfocusedBorderColor = colors.divider,
        disabledBorderColor = colors.divider,
        errorBorderColor = colors.colorError,
        cursorColor = colors.primary,
        errorCursorColor = colors.colorError,
        focusedLabelColor = colors.primary,
        unfocusedLabelColor = colors.secondaryText,
        disabledLabelColor = colors.secondaryText,
        errorLabelColor = colors.colorError,
        focusedTrailingIconColor = colors.iconTint,
        unfocusedTrailingIconColor = colors.iconTint,
        disabledTrailingIconColor = colors.iconTint,
        errorTrailingIconColor = colors.colorError
    )
}
