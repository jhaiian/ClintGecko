package com.jhaiian.clint.privacy

import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager
import com.jhaiian.clint.browser.webview.ClintGecko
import com.jhaiian.clint.history.SearchHistoryManager
import com.jhaiian.clint.settings.datasaver.DataSaverSiteException
import com.jhaiian.clint.settings.sitepermissions.SitePermissionManager
import com.jhaiian.clint.util.registeredDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.mozilla.geckoview.StorageController
import java.io.File
import kotlin.coroutines.resume

enum class BrowsingDataRange(val key: String, val millis: Long?) {
    LAST_HOUR("last_hour", 60L * 60L * 1000L),
    LAST_DAY("last_day", 24L * 60L * 60L * 1000L),
    LAST_WEEK("last_week", 7L * 24L * 60L * 60L * 1000L),
    LAST_FOUR_WEEKS("last_four_weeks", 28L * 24L * 60L * 60L * 1000L),
    ALL_TIME("all_time", null);

    fun since(now: Long = System.currentTimeMillis()): Long = millis?.let { now - it } ?: 0L

    companion object {
        fun fromKey(key: String?): BrowsingDataRange = entries.firstOrNull { it.key == key } ?: LAST_HOUR
    }
}

enum class BrowsingDataType(val prefKey: String, val defaultChecked: Boolean) {
    HISTORY("delete_data_history", true),
    COOKIES("delete_data_cookies", true),
    CACHE("delete_data_cache", true),
    TABS("delete_data_tabs", false),
    SITE_SETTINGS("delete_data_site_settings", false)
}

data class OpenTabSnapshot(
    val url: String,
    val lastActiveAt: Long,
    val isActive: Boolean
)

data class BrowsingDataSummary(
    val allTime: Boolean,
    val historyCount: Int,
    val historyFirstSite: String?,
    val historyOtherSites: Int,
    val cacheBytes: Long,
    val openTabs: Int,
    val siteSettingsCount: Int
)

object BrowsingDataCleaner {

    const val PREF_RANGE = "delete_data_range"

    private val SITE_DATA_FLAGS = StorageController.ClearFlags.COOKIES or
        StorageController.ClearFlags.DOM_STORAGES or
        StorageController.ClearFlags.AUTH_SESSIONS

    private val SITE_SETTINGS_FLAGS = StorageController.ClearFlags.PERMISSIONS or
        StorageController.ClearFlags.SITE_SETTINGS

    @Volatile var openTabs: List<OpenTabSnapshot> = emptyList()

    @Volatile private var closeTabsSince: Long? = null

    private val nonGeckoCacheDirs = setOf(
        "extensions", "favicons", "clint_blobs", "apk_bundle_thumbs", "image_cache",
        "home_icons", "uploads", "updates"
    )

    fun consumeCloseTabsRequest(): Long? {
        val pending = closeTabsSince
        closeTabsSince = null
        return pending
    }

    fun isTabInRange(lastActiveAt: Long, isActive: Boolean, since: Long): Boolean =
        isActive || lastActiveAt >= since

    fun savedRange(context: Context): BrowsingDataRange =
        BrowsingDataRange.fromKey(PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_RANGE, null))

    fun saveRange(context: Context, range: BrowsingDataRange) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PREF_RANGE, range.key).apply()
    }

    fun savedSelection(context: Context): Set<BrowsingDataType> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return BrowsingDataType.entries.filter { prefs.getBoolean(it.prefKey, it.defaultChecked) }.toSet()
    }

    fun saveSelection(context: Context, selection: Set<BrowsingDataType>) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        BrowsingDataType.entries.forEach { editor.putBoolean(it.prefKey, it in selection) }
        editor.apply()
    }

    suspend fun summary(context: Context, range: BrowsingDataRange): BrowsingDataSummary = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val since = range.since()
        val allTime = range == BrowsingDataRange.ALL_TIME
        val history = runCatching { SearchHistoryManager.getSince(app, since) }.getOrDefault(emptyList())
        val sites = history.mapNotNull { item ->
            item.query.takeIf { it.startsWith("http://") || it.startsWith("https://") }
                ?.let { runCatching { Uri.parse(it).host }.getOrNull() }
                ?.removePrefix("www.")
        }.distinct()
        BrowsingDataSummary(
            allTime = allTime,
            historyCount = history.size,
            historyFirstSite = sites.firstOrNull(),
            historyOtherSites = (sites.size - 1).coerceAtLeast(0),
            cacheBytes = geckoCacheBytes(app),
            openTabs = openTabs.count { isTabInRange(it.lastActiveAt, it.isActive, since) },
            siteSettingsCount = runCatching {
                if (allTime) SitePermissionManager.countOrigins(app) else SitePermissionManager.countOriginsSince(app, since)
            }.getOrDefault(0)
        )
    }

    suspend fun delete(context: Context, range: BrowsingDataRange, selection: Set<BrowsingDataType>) {
        val app = context.applicationContext
        val since = range.since()
        val allTime = range == BrowsingDataRange.ALL_TIME
        val visitedSites = if (allTime) emptySet() else withContext(Dispatchers.IO) { visitedSitesSince(app, since) }
        if (BrowsingDataType.HISTORY in selection) {
            withContext(Dispatchers.IO) {
                runCatching {
                    if (allTime) SearchHistoryManager.clear(app)
                    else SearchHistoryManager.deleteSince(app, since)
                }
            }
        }
        var changedSiteSettings = emptySet<String>()
        if (BrowsingDataType.SITE_SETTINGS in selection) {
            withContext(Dispatchers.IO) {
                runCatching {
                    if (allTime) SitePermissionManager.clearAll(app)
                    else changedSiteSettings = SitePermissionManager.deleteSince(app, since)
                }
            }
            DataSaverSiteException.invalidate()
        }
        if (allTime) clearAllTime(app, selection) else clearInRange(app, since, selection, visitedSites, changedSiteSettings)
        if (BrowsingDataType.TABS in selection) closeTabsSince = since
    }

    private suspend fun clearAllTime(app: Context, selection: Set<BrowsingDataType>) {
        var flags = 0L
        if (BrowsingDataType.COOKIES in selection) flags = flags or SITE_DATA_FLAGS
        if (BrowsingDataType.CACHE in selection) flags = flags or StorageController.ClearFlags.ALL_CACHES
        if (BrowsingDataType.SITE_SETTINGS in selection) flags = flags or SITE_SETTINGS_FLAGS
        if (flags == 0L || !ensureRuntime(app)) return
        suspendCancellableCoroutine { continuation ->
            ClintGecko.clearData(flags) { if (continuation.isActive) continuation.resume(Unit) }
        }
    }

    private suspend fun clearInRange(
        app: Context,
        since: Long,
        selection: Set<BrowsingDataType>,
        visitedSites: Set<String>,
        changedSiteSettings: Set<String>
    ) {
        val wantsCookies = BrowsingDataType.COOKIES in selection
        val wantsCache = BrowsingDataType.CACHE in selection
        if (!wantsCookies && !wantsCache && changedSiteSettings.isEmpty()) return
        if (!ensureRuntime(app)) return
        val cookiesRemovedByTime = wantsCookies && removeCookiesSince(since)
        var visitedFlags = 0L
        if (wantsCookies) {
            visitedFlags = visitedFlags or StorageController.ClearFlags.DOM_STORAGES or StorageController.ClearFlags.AUTH_SESSIONS
            if (!cookiesRemovedByTime) visitedFlags = visitedFlags or StorageController.ClearFlags.COOKIES
        }
        if (wantsCache) visitedFlags = visitedFlags or StorageController.ClearFlags.ALL_CACHES
        val targets = LinkedHashMap<String, Long>()
        if (visitedFlags != 0L) visitedSites.forEach { targets[it] = visitedFlags }
        changedSiteSettings.forEach { targets[it] = (targets[it] ?: 0L) or SITE_SETTINGS_FLAGS }
        targets.forEach { (domain, flags) ->
            suspendCancellableCoroutine { continuation ->
                ClintGecko.clearDataFromBaseDomain(domain, flags) { if (continuation.isActive) continuation.resume(Unit) }
            }
        }
    }

    private fun ensureRuntime(app: Context): Boolean {
        runCatching { ClintGecko.init(app) }
        return ClintGecko.isRuntimeReady()
    }

    private suspend fun removeCookiesSince(since: Long): Boolean = suspendCancellableCoroutine { continuation ->
        ClintGecko.cookieOpAsync("since", "", null, JSONObject().put("since", since)) { result ->
            if (continuation.isActive) continuation.resume(result == true)
        }
    }

    private fun visitedSitesSince(context: Context, since: Long): Set<String> {
        val historyUrls = runCatching { SearchHistoryManager.getSince(context, since) }
            .getOrDefault(emptyList())
            .map { it.query }
        val tabUrls = openTabs.filter { isTabInRange(it.lastActiveAt, it.isActive, since) }.map { it.url }
        return (historyUrls + tabUrls)
            .filter { it.startsWith("http://") || it.startsWith("https://") }
            .mapNotNull { url -> runCatching { Uri.parse(url).host }.getOrNull()?.takeIf { it.isNotBlank() } }
            .mapNotNull { host -> runCatching { registeredDomain(host) }.getOrNull()?.takeIf { it.isNotBlank() } }
            .toSet()
    }

    private fun geckoCacheBytes(context: Context): Long =
        context.cacheDir.listFiles()
            ?.filter { it.name !in nonGeckoCacheDirs }
            ?.sumOf { sizeOf(it) }
            ?: 0L

    private fun sizeOf(file: File): Long =
        if (file.isDirectory) file.listFiles()?.sumOf { sizeOf(it) } ?: 0L else file.length()
}
