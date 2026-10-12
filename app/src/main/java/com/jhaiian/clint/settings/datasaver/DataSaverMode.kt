package com.jhaiian.clint.settings.datasaver

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager

object DataSaverMode {

    const val PREF_ENABLED = "data_saver_enabled"
    const val PREF_METERED_ONLY = "data_saver_metered_only"

    @Volatile
    private var metered = true
    private var registered = false
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    var onActiveChanged: (() -> Unit)? = null

    fun isActive(prefs: SharedPreferences): Boolean {
        if (!prefs.getBoolean(PREF_ENABLED, false)) return false
        if (!prefs.getBoolean(PREF_METERED_ONLY, true)) return true
        return metered
    }

    fun register(context: Context) {
        if (registered) return
        val appContext = context.applicationContext
        val manager = appContext.getSystemService(ConnectivityManager::class.java) ?: return
        val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
        metered = manager.isActiveNetworkMetered
        registered = true
        runCatching {
            manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    update(prefs, !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED))
                }

                override fun onLost(network: Network) {
                    update(prefs, true)
                }
            })
        }
    }

    private fun update(prefs: SharedPreferences, newMetered: Boolean) {
        val before = isActive(prefs)
        metered = newMetered
        if (before != isActive(prefs)) {
            mainHandler.post { onActiveChanged?.invoke() }
        }
    }
}
