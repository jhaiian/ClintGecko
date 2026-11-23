package com.jhaiian.clint.parental

import androidx.compose.runtime.Composable

data class ParentalGateRequest(
    val title: String,
    val message: String,
    val positiveLabel: String,
    val onConfirm: () -> Unit
)

@Composable
fun ParentalGateHost(
    request: ParentalGateRequest?,
    hideStatusBar: Boolean,
    hideSystemNavigation: Boolean,
    onDismiss: () -> Unit
) {
    if (request == null) return
    ParentalPasswordVerifyDialog(request, hideStatusBar, hideSystemNavigation, onDismiss)
}
