package com.jhaiian.clint.privacy

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager
import androidx.preference.PreferenceManager
import com.jhaiian.clint.browser.webview.ClintGecko
import com.jhaiian.clint.history.SearchHistoryManager
import org.mozilla.geckoview.StorageController
import java.util.concurrent.atomic.AtomicInteger

object PrivacyGuard {

    private val main = Handler(Looper.getMainLooper())
    private var application: Application? = null
    private var startedCount = 0
    private var aliveCount = 0
    private var launches = 0
    private var inBackground = true
    private var backgroundAt = 0L
    private var locked = false
    private var lockVisible = false
    private var clearRunning = false
    private var clipboardTask: Runnable? = null
    private var clipboardStamp = 0L
    private var ignoreClipUntil = 0L

    fun install(app: Application) {
        if (application != null) return
        application = app
        locked = prefs(app).getBoolean(PrivacyPrefs.APP_LOCK, PrivacyPrefs.DEFAULT_APP_LOCK)
        app.registerActivityLifecycleCallbacks(callbacks)
        clipboard(app).addPrimaryClipChangedListener { onClipChanged() }
    }

    fun unlock() {
        locked = false
    }

    fun refreshScreenGuard(activity: Activity) {
        applyScreenGuard(activity)
    }

    fun onRuntimeReady(context: Context) {
        runExitClear(context.applicationContext)
    }

    private fun prefs(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)

    private fun clipboard(context: Context) = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private val callbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            aliveCount++
            if (activity is AppLockActivity) lockVisible = true
            applyScreenGuard(activity)
        }

        override fun onActivityStarted(activity: Activity) {
            startedCount++
            if (startedCount == 1) onForeground(activity)
        }

        override fun onActivityResumed(activity: Activity) {
            applyScreenGuard(activity)
        }

        override fun onActivityPaused(activity: Activity) {}

        override fun onActivityStopped(activity: Activity) {
            startedCount--
            if (startedCount <= 0) {
                startedCount = 0
                if (!activity.isChangingConfigurations) onBackground(activity)
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

        override fun onActivityDestroyed(activity: Activity) {
            aliveCount--
            if (activity is AppLockActivity) lockVisible = false
            if (aliveCount <= 0) {
                aliveCount = 0
                if (activity.isFinishing) onExit(activity)
            }
        }
    }

    private fun onForeground(activity: Activity) {
        if (!inBackground) return
        inBackground = false
        launches++
        val prefs = prefs(activity)
        val lockEnabled = prefs.getBoolean(PrivacyPrefs.APP_LOCK, PrivacyPrefs.DEFAULT_APP_LOCK)
        if (launches == 1) {
            locked = lockEnabled
            runExitClear(activity.applicationContext)
        } else {
            if (!clearRunning) prefs.edit().putBoolean(PrivacyPrefs.EXIT_CLEAR_PENDING, false).apply()
            if (lockEnabled && !locked) {
                val timeout = prefs.getInt(PrivacyPrefs.APP_LOCK_TIMEOUT, PrivacyPrefs.DEFAULT_APP_LOCK_TIMEOUT)
                if (SystemClock.elapsedRealtime() - backgroundAt >= timeout * 1000L) locked = true
            }
        }
        if (!lockEnabled) locked = false
        if (locked) showLock(activity)
    }

    private fun onBackground(activity: Activity) {
        inBackground = true
        backgroundAt = SystemClock.elapsedRealtime()
        val prefs = prefs(activity)
        if (prefs.getBoolean(PrivacyPrefs.CLEAR_ON_EXIT, PrivacyPrefs.DEFAULT_CLEAR_ON_EXIT)) {
            prefs.edit().putBoolean(PrivacyPrefs.EXIT_CLEAR_PENDING, true).apply()
        }
    }

    private fun onExit(activity: Activity) {
        val prefs = prefs(activity)
        if (!prefs.getBoolean(PrivacyPrefs.CLEAR_ON_EXIT, PrivacyPrefs.DEFAULT_CLEAR_ON_EXIT)) return
        prefs.edit().putBoolean(PrivacyPrefs.EXIT_CLEAR_PENDING, true).commit()
        runExitClear(activity.applicationContext)
    }

    private fun showLock(activity: Activity) {
        if (lockVisible || activity is AppLockActivity) return
        lockVisible = true
        activity.startActivity(Intent(activity, AppLockActivity::class.java))
    }

    private fun applyScreenGuard(activity: Activity) {
        val block = prefs(activity).getBoolean(PrivacyPrefs.SCREENSHOT_BLOCKING, PrivacyPrefs.DEFAULT_SCREENSHOT_BLOCKING)
        if (block) activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun runExitClear(context: Context) {
        val prefs = prefs(context)
        if (clearRunning) return
        if (!prefs.getBoolean(PrivacyPrefs.CLEAR_ON_EXIT, PrivacyPrefs.DEFAULT_CLEAR_ON_EXIT)) return
        if (!prefs.getBoolean(PrivacyPrefs.EXIT_CLEAR_PENDING, false)) return
        val history = prefs.getBoolean(PrivacyPrefs.CLEAR_ON_EXIT_HISTORY, PrivacyPrefs.DEFAULT_CLEAR_ON_EXIT_HISTORY)
        val cookies = prefs.getBoolean(PrivacyPrefs.CLEAR_ON_EXIT_COOKIES, PrivacyPrefs.DEFAULT_CLEAR_ON_EXIT_COOKIES)
        val cache = prefs.getBoolean(PrivacyPrefs.CLEAR_ON_EXIT_CACHE, PrivacyPrefs.DEFAULT_CLEAR_ON_EXIT_CACHE)
        if ((cookies || cache) && !ClintGecko.isRuntimeReady()) return
        clearRunning = true
        val remaining = AtomicInteger(1 + (if (cookies) 1 else 0) + (if (cache) 1 else 0))
        val finished: () -> Unit = {
            if (remaining.decrementAndGet() == 0) {
                main.post {
                    prefs.edit().putBoolean(PrivacyPrefs.EXIT_CLEAR_PENDING, false).apply()
                    clearRunning = false
                }
            }
        }
        Thread {
            if (history) runCatching { SearchHistoryManager.clear(context) }
            finished()
        }.start()
        if (cookies) {
            ClintGecko.clearData(StorageController.ClearFlags.COOKIES or StorageController.ClearFlags.DOM_STORAGES, finished)
        }
        if (cache) {
            ClintGecko.clearData(StorageController.ClearFlags.ALL_CACHES, finished)
        }
    }

    private fun onClipChanged() {
        val app = application ?: return
        clipboardTask?.let { main.removeCallbacks(it) }
        clipboardTask = null
        val seconds = prefs(app).getInt(PrivacyPrefs.CLIPBOARD_CLEAR_SECONDS, PrivacyPrefs.DEFAULT_CLIPBOARD_CLEAR_SECONDS)
        if (seconds <= 0 || startedCount <= 0) return
        if (SystemClock.elapsedRealtime() < ignoreClipUntil) return
        val stamp = runCatching { clipboard(app).primaryClipDescription?.timestamp }.getOrNull() ?: return
        clipboardStamp = stamp
        val task = Runnable { clearClipboardIfUnchanged(app) }
        clipboardTask = task
        main.postDelayed(task, seconds * 1000L)
    }

    private fun clearClipboardIfUnchanged(app: Application) {
        clipboardTask = null
        runCatching {
            val manager = clipboard(app)
            if (manager.primaryClipDescription?.timestamp == clipboardStamp) {
                ignoreClipUntil = SystemClock.elapsedRealtime() + 2000L
                if (Build.VERSION.SDK_INT >= 28) manager.clearPrimaryClip()
                else manager.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }
}
