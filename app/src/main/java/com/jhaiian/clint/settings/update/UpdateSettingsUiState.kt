package com.jhaiian.clint.settings.update

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class UpdateSettingsUiState(
    initialCheckOnLaunch: Boolean,
    initialSkipOnMetered: Boolean,
    initialBetaChannel: Boolean,
    val hideStatusBar: Boolean,
val hideSystemNavigation: Boolean
) {
    var checkOnLaunch by mutableStateOf(initialCheckOnLaunch)
    var skipOnMetered by mutableStateOf(initialSkipOnMetered)
    var betaChannel by mutableStateOf(initialBetaChannel)
    var betaConfirmDialogOpen by mutableStateOf(false)
}
