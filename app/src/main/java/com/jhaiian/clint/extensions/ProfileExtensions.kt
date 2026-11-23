package com.jhaiian.clint.extensions

import android.content.Context
import androidx.preference.PreferenceManager
import com.jhaiian.clint.profiles.WebProfiles

object ProfileExtensions {

    private const val KEY_PREFIX = "profile_ext_"

    private fun key(profileId: String) = KEY_PREFIX + profileId

    private fun prefs(context: Context) = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    fun read(context: Context, profileId: String): Set<String>? =
        prefs(context).getStringSet(key(profileId), null)?.toSet()

    private fun write(context: Context, profileId: String, ids: Set<String>) {
        prefs(context).edit().putStringSet(key(profileId), ids).apply()
    }

    fun snapshotDefault(context: Context, liveEnabled: Set<String>) {
        if (read(context, WebProfiles.DEFAULT_ID) == null) write(context, WebProfiles.DEFAULT_ID, liveEnabled)
    }

    fun set(context: Context, profileId: String, extensionId: String, enabled: Boolean, liveEnabled: Set<String>) {
        val current = read(context, profileId) ?: if (profileId == WebProfiles.DEFAULT_ID) liveEnabled else emptySet()
        write(context, profileId, if (enabled) current + extensionId else current - extensionId)
    }

    fun forget(context: Context, profileId: String) {
        prefs(context).edit().remove(key(profileId)).apply()
    }

    fun forgetExtension(context: Context, extensionId: String) {
        val preferences = prefs(context)
        val edit = preferences.edit()
        preferences.all.forEach { (name, value) ->
            if (name.startsWith(KEY_PREFIX) && value is Set<*> && extensionId in value) {
                edit.putStringSet(name, value.filterIsInstance<String>().toSet() - extensionId)
            }
        }
        edit.apply()
    }
}
