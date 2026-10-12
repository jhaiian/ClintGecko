package com.jhaiian.clint.settings.misc

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MiscUiState(
    initialDefaultBrowserSummary: String,
    val hideStatusBar: Boolean,
val hideSystemNavigation: Boolean
) {
    var defaultBrowserSummary by mutableStateOf(initialDefaultBrowserSummary)
    var rerunSetupConfirmDialogOpen by mutableStateOf(false)
}
