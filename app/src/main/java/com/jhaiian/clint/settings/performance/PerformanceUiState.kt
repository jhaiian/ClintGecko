package com.jhaiian.clint.settings.performance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class PerformanceUiState(
    initialReduceBlur: Boolean,
    initialDisableWebGl: Boolean,
    initialLimitFrameRate: Boolean,
    initialReduceMotion: Boolean,
    initialStopAnimatedImages: Boolean
) {
    var reduceBlur by mutableStateOf(initialReduceBlur)
    var disableWebGl by mutableStateOf(initialDisableWebGl)
    var limitFrameRate by mutableStateOf(initialLimitFrameRate)
    var reduceMotion by mutableStateOf(initialReduceMotion)
    var stopAnimatedImages by mutableStateOf(initialStopAnimatedImages)
}
