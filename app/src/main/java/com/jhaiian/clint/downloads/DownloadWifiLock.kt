package com.jhaiian.clint.downloads

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal object DownloadWifiLock {

    private val TRANSFERRING = setOf(DownloadStatus.CONNECTING, DownloadStatus.DOWNLOADING)

    @Volatile
    private var started = false

    fun register(context: Context) {
        if (started || Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        started = true
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java) ?: return
        val lock = runCatching {
            @Suppress("DEPRECATION")
            wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Clint:downloads").apply { setReferenceCounted(false) }
        }.getOrNull() ?: return
        ClintDownloadManager.applicationScope.launch {
            ClintDownloadManager.downloadsFlow
                .map { list -> list.any { it.status in TRANSFERRING } }
                .distinctUntilChanged()
                .collect { transferring ->
                    runCatching {
                        if (transferring && !lock.isHeld) lock.acquire()
                        if (!transferring && lock.isHeld) lock.release()
                    }
                }
        }
    }
}
