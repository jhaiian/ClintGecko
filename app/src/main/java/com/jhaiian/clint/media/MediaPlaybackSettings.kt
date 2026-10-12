package com.jhaiian.clint.media

import android.content.Context
import androidx.preference.PreferenceManager

object MediaPlaybackSettings {
    const val PREF_BACKGROUND_PLAYBACK = "background_playback_enabled"

    fun isBackgroundPlaybackEnabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_BACKGROUND_PLAYBACK, true)
}
