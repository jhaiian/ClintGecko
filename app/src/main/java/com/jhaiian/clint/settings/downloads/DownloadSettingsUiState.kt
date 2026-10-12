package com.jhaiian.clint.settings.downloads

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class DownloadSettingsDialog {
    MEASUREMENT_SYSTEM, RETRY_COUNT, RETRY_INTERVAL, SPEED_LIMIT, DOWNLOAD_MANAGER, DIALOG_UI
}

class DownloadSettingsUiState(
    initialDownloadManagerApp: String,
    initialLocationMode: String,
    initialCustomUri: Uri?,
    initialCategorizeDownloads: Boolean,
    initialDialogUi: String,
    initialMeasurementSystemDecimal: Boolean,
    initialUnmeteredOnly: Boolean,
    initialScheduleEnabled: Boolean,
    initialScheduleStartMinutes: Int,
    initialScheduleEndMinutes: Int,
    initialConcurrentDownloads: Int,
    initialSplitParts: Int,
    initialMultithreadingParts: Int,
    initialConcurrentSegments: Int,
    initialSpeedLimitAmount: Int,
    initialSpeedLimitUnit: String,
    initialRetryEnabled: Boolean,
    initialRetryUnrecoverable: Boolean,
    initialRetryCount: Int,
    initialRetryInterval: Int,
    initialIgnoringBatteryOptimizations: Boolean,
    initialShowGrantAllFilesAccessRow: Boolean,
    initialAllFilesAccessGranted: Boolean,
    initialPushNotifications: Boolean,
    initialKeepScreenOn: Boolean,
    initialQuickDownload: Boolean,
    initialQuickDownloadImages: Boolean,
    initialHideStatusBar: Boolean,
initialHideSystemNavigation: Boolean
) {
    var downloadManagerApp by mutableStateOf(initialDownloadManagerApp)
    var locationMode by mutableStateOf(initialLocationMode)
    var customUri by mutableStateOf(initialCustomUri)
    var categorizeDownloads by mutableStateOf(initialCategorizeDownloads)
    var dialogUi by mutableStateOf(initialDialogUi)
    var measurementSystemDecimal by mutableStateOf(initialMeasurementSystemDecimal)
    var unmeteredOnly by mutableStateOf(initialUnmeteredOnly)

    var scheduleEnabled by mutableStateOf(initialScheduleEnabled)
    var scheduleStartMinutes by mutableStateOf(initialScheduleStartMinutes)
    var scheduleEndMinutes by mutableStateOf(initialScheduleEndMinutes)

    var concurrentDownloads by mutableStateOf(initialConcurrentDownloads)
    var splitParts by mutableStateOf(initialSplitParts)
    var multithreadingParts by mutableStateOf(initialMultithreadingParts)
    var concurrentSegments by mutableStateOf(initialConcurrentSegments)

    var speedLimitAmount by mutableStateOf(initialSpeedLimitAmount)
    var speedLimitUnit by mutableStateOf(initialSpeedLimitUnit)

    var retryEnabled by mutableStateOf(initialRetryEnabled)
    var retryUnrecoverable by mutableStateOf(initialRetryUnrecoverable)
    var retryCount by mutableStateOf(initialRetryCount)
    var retryInterval by mutableStateOf(initialRetryInterval)

    var ignoringBatteryOptimizations by mutableStateOf(initialIgnoringBatteryOptimizations)

    var showGrantAllFilesAccessRow by mutableStateOf(initialShowGrantAllFilesAccessRow)
    var allFilesAccessGranted by mutableStateOf(initialAllFilesAccessGranted)

    var pushNotifications by mutableStateOf(initialPushNotifications)
    var keepScreenOn by mutableStateOf(initialKeepScreenOn)
    var quickDownload by mutableStateOf(initialQuickDownload)
    var quickDownloadImages by mutableStateOf(initialQuickDownloadImages)
    var hideStatusBar by mutableStateOf(initialHideStatusBar)
    var hideSystemNavigation by mutableStateOf(initialHideSystemNavigation)

    var openDialog by mutableStateOf<DownloadSettingsDialog?>(null)
}
