package com.jhaiian.clint.profiles

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateListOf
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.shortcuts.ShortcutStore
import com.jhaiian.clint.tabs.TabSessionManager
import com.jhaiian.clint.ui.rememberMaxContentWidth
import com.jhaiian.clint.ui.theme.ClintComposeTheme

class ProfilesActivity : ClintActivity() {

    private val profiles = mutableStateListOf<BrowserProfile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("app_theme", "system") ?: "system"
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)
        val supported = WebProfiles.isSupported()

        reload()

        setContent {
            ClintComposeTheme(theme = theme) {
                val maxContentWidth = rememberMaxContentWidth(this@ProfilesActivity)
                ProfilesScreen(
                    profiles = profiles,
                    supported = supported,
                    maxContentWidth = maxContentWidth,
                    hideStatusBar = hideStatusBar,
                    hideSystemNavigation = hideSystemNavigation,
                    onExit = { finish() },
                    onCreate = { name, color ->
                        ProfileRepository.create(this@ProfilesActivity, name, color)
                        reload()
                    },
                    onUpdate = { id, name, color ->
                        ProfileRepository.update(this@ProfilesActivity, id, name, color)
                        reload()
                    },
                    onClearData = { profile ->
                        WebProfiles.clearData(profile.id)
                        Toast.makeText(this@ProfilesActivity, R.string.profiles_data_cleared, Toast.LENGTH_SHORT).show()
                    },
                    onDelete = { profile ->
                        val result = WebProfiles.deleteProfileData(profile.id)
                        if (result == ProfileDeleteResult.DELETED) {
                            ProfileRepository.remove(this@ProfilesActivity, profile.id)
                            val appContext = applicationContext
                            Thread {
                                ShortcutStore.clearProfile(appContext, profile.id)
                                TabSessionManager.removeProfile(appContext, profile.id)
                            }.start()
                            reload()
                        }
                        result
                    }
                )
            }
        }
    }

    private fun reload() {
        profiles.clear()
        profiles.addAll(ProfileRepository.all(this))
    }
}
