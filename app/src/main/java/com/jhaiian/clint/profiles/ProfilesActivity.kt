package com.jhaiian.clint.profiles

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.ui.rememberMaxContentWidth
import com.jhaiian.clint.ui.theme.ClintComposeTheme

class ProfilesActivity : ClintActivity() {

    private val profiles = mutableStateListOf<BrowserProfile>()
    private val activeId = mutableStateOf(WebProfiles.DEFAULT_ID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("app_theme", "system") ?: "system"
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)
        val supported = WebProfiles.isSupported()

        activeId.value = ActiveProfile.id(this)
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
                    activeId = activeId.value,
                    onSwitch = { id ->
                        ActiveProfile.set(this@ProfilesActivity, id)
                        activeId.value = ActiveProfile.id(this@ProfilesActivity)
                        val name = profiles.firstOrNull { it.id == id }?.name ?: getString(R.string.profiles_default_name)
                        Toast.makeText(this@ProfilesActivity, getString(R.string.profiles_switched, name), Toast.LENGTH_SHORT).show()
                    },
                    onCreate = { name, color, temporary ->
                        ProfileRepository.create(this@ProfilesActivity, name, color, temporary)
                        reload()
                    },
                    onUpdate = { id, name, color, temporary ->
                        ProfileRepository.update(this@ProfilesActivity, id, name, color, temporary)
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
                            if (activeId.value == profile.id) {
                                ActiveProfile.reset(this@ProfilesActivity)
                                activeId.value = WebProfiles.DEFAULT_ID
                            }
                            ProfileWiper.eraseData(applicationContext, profile.id, keepDownloads = !profile.temporary)
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
