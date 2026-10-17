package com.jhaiian.clint.app

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.jhaiian.clint.browser.webview.ClintGecko
import com.jhaiian.clint.media.MediaPlaybackController
import com.jhaiian.clint.settings.datasaver.DataSaverMode
import com.jhaiian.clint.util.LocaleHelper
import com.jhaiian.clint.ui.theme.SystemDarkState
import com.jhaiian.clint.util.loadMeasurementSystemPreference
import java.lang.ref.WeakReference

class ClintApplication : Application() {

    private var _currentActivity: WeakReference<Activity>? = null
    private var mainProcess = true
    val currentActivity: Activity? get() = _currentActivity?.get()

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(base))
    }

    override fun onCreate() {
        super.onCreate()
        mainProcess = isMainProcess()
        if (!mainProcess) return
        applyNightMode()
        SystemDarkState.install(this)
        loadMeasurementSystemPreference(this)
        DataSaverMode.register(this)
        com.jhaiian.clint.privacy.PrivacyGuard.install(this)
        MediaPlaybackController.init(this)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                MediaPlaybackController.onActivityStarted()
            }
            override fun onActivityResumed(activity: Activity) {
                _currentActivity = WeakReference(activity)
            }
            override fun onActivityPaused(activity: Activity) {
                if (_currentActivity?.get() === activity) _currentActivity = null
            }
            override fun onActivityStopped(activity: Activity) {
                MediaPlaybackController.onActivityStopped(activity.isChangingConfigurations)
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {
                if (_currentActivity?.get() === activity) _currentActivity = null
            }
        })
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (mainProcess) ClintGecko.trimMemory(level)
    }

    private fun isMainProcess(): Boolean {
        val name = if (Build.VERSION.SDK_INT >= 28) {
            Application.getProcessName()
        } else {
            val pid = android.os.Process.myPid()
            (getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
                ?.runningAppProcesses
                ?.firstOrNull { it.pid == pid }
                ?.processName
        }
        return name == null || name == packageName
    }

    fun applyNightMode() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("app_theme", "system") ?: "system"
        AppCompatDelegate.setDefaultNightMode(
            when (theme) {
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }
}
