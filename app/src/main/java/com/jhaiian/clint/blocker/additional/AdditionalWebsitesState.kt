package com.jhaiian.clint.blocker.additional

import android.content.Context
import androidx.preference.PreferenceManager

object AdditionalWebsitesState {

    private const val PREF_ENABLED = "additional_websites_enabled"

    fun isEnabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(PREF_ENABLED, enabled)
            .apply()
    }
}
