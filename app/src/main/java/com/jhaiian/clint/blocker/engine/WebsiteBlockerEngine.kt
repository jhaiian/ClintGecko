package com.jhaiian.clint.blocker.engine

import android.content.Context

object WebsiteBlockerEngine {
    @Volatile
    private var handle: Long = 0L

    @Volatile
    var isActive: Boolean = false
        private set

    @Volatile
    var domainCount: Long = 0L
        private set

    @Synchronized
    fun activate(context: Context): Boolean {
        val file = WebsiteBlockerPaths.engineFile(context)
        if (!file.exists()) {
            deactivateLocked()
            return false
        }
        val newHandle = WebsiteBlockerNative.nativeLoadDatabase(file.absolutePath)
        if (newHandle == 0L) {
            deactivateLocked()
            return false
        }
        val oldHandle = handle
        handle = newHandle
        domainCount = WebsiteBlockerNative.nativeDomainCount(newHandle)
        isActive = true
        if (oldHandle != 0L) {
            WebsiteBlockerNative.nativeUnloadDatabase(oldHandle)
        }
        return true
    }

    @Synchronized
    fun deactivate() {
        deactivateLocked()
    }

    private fun deactivateLocked() {
        if (handle != 0L) {
            WebsiteBlockerNative.nativeUnloadDatabase(handle)
        }
        handle = 0L
        isActive = false
        domainCount = 0L
    }

    fun isBlocked(host: String): Boolean {
        val activeHandle = handle
        if (activeHandle == 0L || host.isBlank()) return false
        return WebsiteBlockerNative.nativeIsBlocked(activeHandle, host)
    }
}
