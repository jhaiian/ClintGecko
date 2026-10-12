package com.jhaiian.clint.crash

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

const val MAX_CRASH_CLIP_CHARS = 450_000

class CrashReportItem(val file: File, val title: String, val content: String)

class CrashUiState(val hideStatusBar: Boolean, val hideSystemNavigation: Boolean) {
    var isLoading by mutableStateOf(true)
    val reports = mutableStateListOf<CrashReportItem>()
    var clearAllConfirmOpen by mutableStateOf(false)
    var detailReport by mutableStateOf<CrashReportItem?>(null)
    var reportTemplate by mutableStateOf("")
}
