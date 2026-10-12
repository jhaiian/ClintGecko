package com.jhaiian.clint.settings.site

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SiteSettingsUiState(
    initialCameraBehavior: String,
    initialMicBehavior: String,
    initialLocationBehavior: String,
    initialNotificationsBehavior: String,
    initialClipboardBehavior: String,
    initialDesktopModeSaveState: String,
    initialOpenInAppBehavior: String
) {
    var cameraBehavior by mutableStateOf(initialCameraBehavior)
    var micBehavior by mutableStateOf(initialMicBehavior)
    var locationBehavior by mutableStateOf(initialLocationBehavior)
    var notificationsBehavior by mutableStateOf(initialNotificationsBehavior)
    var clipboardBehavior by mutableStateOf(initialClipboardBehavior)
    var desktopModeSaveState by mutableStateOf(initialDesktopModeSaveState)
    var openInAppBehavior by mutableStateOf(initialOpenInAppBehavior)
}
