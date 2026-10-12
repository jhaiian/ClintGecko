package com.jhaiian.clint.profiles

import android.content.Context
import com.jhaiian.clint.browser.webview.ClintGecko
import com.jhaiian.clint.browser.webview.ClintWebView
import java.util.concurrent.ConcurrentHashMap

enum class ProfileDeleteResult { DELETED, IN_USE }

object WebProfiles {

    const val DEFAULT_ID = "default"
    const val PROFILE_HEADER = "X-Clint-Profile"

    private val tabProfiles = ConcurrentHashMap<String, String>()

    fun isSupported(): Boolean = true

    fun resolve(context: Context, requestedId: String?): String {
        if (requestedId.isNullOrEmpty() || requestedId == DEFAULT_ID) return DEFAULT_ID
        return if (ProfileRepository.get(context, requestedId) != null) requestedId else DEFAULT_ID
    }

    fun bind(webView: ClintWebView, profileId: String) {}

    fun profileOf(webView: ClintWebView): String = webView.profileId ?: DEFAULT_ID

    fun registerTab(tabId: String, profileId: String) {
        if (profileId == DEFAULT_ID) tabProfiles.remove(tabId) else tabProfiles[tabId] = profileId
    }

    fun unregisterTab(tabId: String) {
        tabProfiles.remove(tabId)
    }

    fun profileOfTab(tabId: String): String = tabProfiles[tabId] ?: DEFAULT_ID

    private fun storeProfile(profileId: String?): String? =
        if (profileId.isNullOrEmpty() || profileId == DEFAULT_ID) null else profileId

    fun cookieFor(profileId: String?, url: String): String =
        runCatching { ClintGecko.cookiesBlocking(url, storeProfile(profileId)) }.getOrDefault("")

    fun cookieForAsync(webView: ClintWebView, url: String, callback: (String) -> Unit) {
        ClintGecko.cookiesAsync(url, webView.cookieStoreId, callback)
    }

    fun cookieForHeaders(headers: Map<String, String>?, url: String): String =
        cookieFor(headers?.get(PROFILE_HEADER), url)

    fun withMarker(headers: Map<String, String>?, profileId: String): Map<String, String> {
        val base = headers.orEmpty()
        return if (profileId == DEFAULT_ID) base else base + (PROFILE_HEADER to profileId)
    }

    fun flushAll() {}

    fun clearData(profileId: String) {
        if (profileId == DEFAULT_ID) return
        runCatching { ClintGecko.clearContextData(profileId) }
    }

    fun deleteProfileData(profileId: String): ProfileDeleteResult {
        if (tabProfiles.containsValue(profileId)) return ProfileDeleteResult.IN_USE
        runCatching { ClintGecko.clearContextData(profileId) }
        return ProfileDeleteResult.DELETED
    }
}
