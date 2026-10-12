package com.jhaiian.clint.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class TabAudioState {
    var audible by mutableStateOf(false)
    var muted by mutableStateOf(false)
}
