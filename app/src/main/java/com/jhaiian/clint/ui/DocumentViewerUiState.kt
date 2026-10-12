package com.jhaiian.clint.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class DocumentViewerUiState {
    var isLoading by mutableStateOf(true)
    var markdown by mutableStateOf<String?>(null)
    var isError by mutableStateOf(false)
}
