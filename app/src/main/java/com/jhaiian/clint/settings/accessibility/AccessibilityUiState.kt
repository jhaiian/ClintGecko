package com.jhaiian.clint.settings.accessibility

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AccessibilityUiState(
    initialAutoFontSize: Boolean,
    initialFontSizePercent: Int,
    initialForceZoom: Boolean
) {
    var autoFontSize by mutableStateOf(initialAutoFontSize)
    var fontSizePercent by mutableIntStateOf(initialFontSizePercent)
    var forceZoom by mutableStateOf(initialForceZoom)
}
