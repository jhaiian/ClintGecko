package com.jhaiian.clint.profiles

import android.content.Context
import androidx.preference.PreferenceManager

object ActiveProfile {

    const val PREF_KEY = "active_profile_id"

    @Volatile
    private var cached: String? = null

    fun id(context: Context): String {
        cached?.let { return it }
        val stored = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            .getString(PREF_KEY, WebProfiles.DEFAULT_ID) ?: WebProfiles.DEFAULT_ID
        val resolved = WebProfiles.resolve(context.applicationContext, stored)
        cached = resolved
        return resolved
    }

    fun current(): String = cached ?: WebProfiles.DEFAULT_ID

    fun set(context: Context, profileId: String) {
        val resolved = WebProfiles.resolve(context.applicationContext, profileId)
        cached = resolved
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            .edit()
            .putString(PREF_KEY, resolved)
            .apply()
    }

    fun reset(context: Context) = set(context, WebProfiles.DEFAULT_ID)

    fun invalidate() {
        cached = null
    }
}
