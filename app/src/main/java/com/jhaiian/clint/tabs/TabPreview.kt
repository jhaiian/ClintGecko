package com.jhaiian.clint.tabs

import com.jhaiian.clint.media.TabAudioState

data class TabPreview(
    val id: String,
    val title: String,
    val url: String,
    val isIncognito: Boolean,
    val profileId: String = "default",
    val audio: TabAudioState? = null
)
