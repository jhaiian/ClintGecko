package com.jhaiian.clint.privacy

import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceManager
import com.jhaiian.clint.browser.webview.ClintGecko
import com.jhaiian.clint.history.SearchHistoryManager
import com.jhaiian.clint.settings.datasaver.DataSaverSiteException
import com.jhaiian.clint.settings.sitepermissions.SitePermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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

data class BrowsingDataSummary(
    val historyCount: Int,
    val historyFirstSite: String?,
    val historyOtherSites: Int,
    val cacheBytes: Long,
    val openTabs: Int,
    val siteSettingsCount: Int
)

object BrowsingDataCleaner {

    const val PREF_RANGE = "delete_data_range"

    @Volatile var openTabCount: Int = 0

    @Volatile private var closeAllTabsPending = false

    private val nonGeckoCacheDirs = setOf(
        "extensions", "favicons", "clint_blobs", "apk_bundle_thumbs", "image_cache",
        "home_icons", "uploads", "updates"
    )

    fun consumeCloseAllTabsRequest(): Boolean {
        val pending = closeAllTabsPending
        closeAllTabsPending = false
        return pending
    }

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
        val history = runCatching { SearchHistoryManager.getSince(app, range.since()) }.getOrDefault(emptyList())
        val sites = history.mapNotNull { item ->
            item.query.takeIf { it.startsWith("http://") || it.startsWith("https://") }
                ?.let { runCatching { Uri.parse(it).host }.getOrNull() }
                ?.removePrefix("www.")
        }.distinct()
        BrowsingDataSummary(
            historyCount = history.size,
            historyFirstSite = sites.firstOrNull(),
            historyOtherSites = (sites.size - 1).coerceAtLeast(0),
            cacheBytes = geckoCacheBytes(app),
            openTabs = openTabCount,
            siteSettingsCount = runCatching { SitePermissionManager.countOrigins(app) }.getOrDefault(0)
        )
    }

    suspend fun delete(context: Context, range: BrowsingDataRange, selection: Set<BrowsingDataType>) {
        val app = context.applicationContext
        if (BrowsingDataType.HISTORY in selection) {
            withContext(Dispatchers.IO) {
                runCatching {
                    if (range == BrowsingDataRange.ALL_TIME) SearchHistoryManager.clear(app)
                    else SearchHistoryManager.deleteSince(app, range.since())
                }
            }
        }
        if (BrowsingDataType.SITE_SETTINGS in selection) {
            withContext(Dispatchers.IO) { runCatching { SitePermissionManager.clearAll(app) } }
            DataSaverSiteException.invalidate()
        }
        var flags = 0L
        if (BrowsingDataType.COOKIES in selection) {
            flags = flags or StorageController.ClearFlags.COOKIES or
                StorageController.ClearFlags.DOM_STORAGES or
                StorageController.ClearFlags.AUTH_SESSIONS
        }
        if (BrowsingDataType.CACHE in selection) flags = flags or StorageController.ClearFlags.ALL_CACHES
        if (BrowsingDataType.SITE_SETTINGS in selection) {
            flags = flags or StorageController.ClearFlags.PERMISSIONS or StorageController.ClearFlags.SITE_SETTINGS
        }
        if (flags != 0L) {
            runCatching { ClintGecko.init(app) }
            if (ClintGecko.isRuntimeReady()) {
                suspendCancellableCoroutine { continuation ->
                    ClintGecko.clearData(flags) { if (continuation.isActive) continuation.resume(Unit) }
                }
            }
        }
        if (BrowsingDataType.TABS in selection) closeAllTabsPending = true
    }

    private fun geckoCacheBytes(context: Context): Long =
        context.cacheDir.listFiles()
            ?.filter { it.name !in nonGeckoCacheDirs }
            ?.sumOf { sizeOf(it) }
            ?: 0L

    private fun sizeOf(file: File): Long =
        if (file.isDirectory) file.listFiles()?.sumOf { sizeOf(it) } ?: 0L else file.length()
}
