package com.jhaiian.clint.downloads

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.preference.PreferenceManager
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

internal object DownloadNetworkMonitor {

    internal val unmeteredPausedIds: MutableSet<Int> = ConcurrentHashMap.newKeySet()
    internal val networkWaitingIds: MutableSet<Int> = ConcurrentHashMap.newKeySet()
    private var monitoringStarted = false

    private sealed class NetworkEvent {
        object Available : NetworkEvent()
        data class CapabilitiesChanged(val unmetered: Boolean) : NetworkEvent()
        object Lost : NetworkEvent()
    }

    private fun networkEvents(context: Context): Flow<NetworkEvent> = callbackFlow {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkEvent.Available)
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(NetworkEvent.CapabilitiesChanged(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)))
            }

            override fun onLost(network: Network) {
                trySend(NetworkEvent.Lost)
            }
        }
        cm.registerDefaultNetworkCallback(callback)
        awaitClose { cm.unregisterNetworkCallback(callback) }
    }

    fun register(context: Context) {
        if (monitoringStarted) return
        monitoringStarted = true
        val appContext = context.applicationContext
        ClintDownloadManager.applicationScope.launch {
            networkEvents(appContext).collect { event ->
                when (event) {
                    is NetworkEvent.Available -> handleAvailable(appContext)
                    is NetworkEvent.CapabilitiesChanged -> handleCapabilitiesChanged(appContext, event.unmetered)
                    is NetworkEvent.Lost -> pauseActiveForMetered(appContext)
                }
            }
        }
    }

    private fun handleAvailable(context: Context) {
        val toResume = networkWaitingIds.toList()
        networkWaitingIds.clear()
        val current = ClintDownloadManager.downloadsFlow.value
        toResume.forEach { id ->
            val item = current.find { it.id == id } ?: return@forEach
            if (item.status == DownloadStatus.PAUSED && item.waitingForNetwork) {
                ClintDownloadManager.resume(context, id)
            }
        }
        ClintDownloadManager.tryDequeueNext(context)
    }

    private fun handleCapabilitiesChanged(context: Context, unmetered: Boolean) {
        if (unmetered) {
            val toResume = unmeteredPausedIds.toList()
            unmeteredPausedIds.clear()
            val current = ClintDownloadManager.downloadsFlow.value
            toResume.forEach { id ->
                val item = current.find { it.id == id } ?: return@forEach
                if (item.status == DownloadStatus.PAUSED) {
                    ClintDownloadManager.resume(context, id)
                }
            }
            ClintDownloadManager.tryDequeueNext(context)
        } else {
            pauseActiveForMetered(context)
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun isUnmeteredOnlyEnabled(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(
            DownloadSettingsKeys.PREF_UNMETERED_ONLY,
            DownloadSettingsKeys.DEFAULT_UNMETERED_ONLY
        )
    }

    fun isNetworkUnmetered(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    fun pauseActiveForMetered(context: Context) {
        val active = ClintDownloadManager.downloadsFlow.value.filter {
            it.unmeteredOnly && (
                it.status == DownloadStatus.DOWNLOADING ||
                it.status == DownloadStatus.CONNECTING ||
                it.status == DownloadStatus.ALLOCATING ||
                it.status == DownloadStatus.RETRYING
            )
        }
        active.forEach { item ->
            unmeteredPausedIds.add(item.id)
            ClintDownloadManager.pauseForUnmeteredWait(context, item.id)
        }
    }

    fun onUnmeteredOnlyChanged(context: Context, enabled: Boolean) {
        val ctx = context.applicationContext
        if (!enabled) {
            val toResume = unmeteredPausedIds.toList()
            unmeteredPausedIds.clear()
            val current = ClintDownloadManager.downloadsFlow.value
            toResume.forEach { id ->
                val item = current.find { it.id == id } ?: return@forEach
                if (item.status == DownloadStatus.PAUSED) {
                    ClintDownloadManager.resume(ctx, id)
                }
            }
            ClintDownloadManager.tryDequeueNext(ctx)
        } else {
            if (!isNetworkUnmetered(ctx)) {
                pauseActiveForMetered(ctx)
            }
        }
    }
}
