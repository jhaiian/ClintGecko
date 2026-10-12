package com.jhaiian.clint.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
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
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null
) {
    val colors = LocalClintColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        enabled = enabled,
        readOnly = readOnly,
        isError = isError,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        colors = OutlinedTextFieldDefaults.colors(
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
    )
}
