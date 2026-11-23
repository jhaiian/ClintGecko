package com.jhaiian.clint.downloads

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.preference.PreferenceManager
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys

object DownloadManagerAppIds {
    const val CLINT = "clint"
    const val ONEDM = "1dm"
    const val ONEDM_PLUS = "1dm_plus"
    const val ONEDM_LITE = "1dm_lite"
    const val ADM = "adm"
    const val CUSTOM = "custom"
}

data class ExternalDownloadManagerApp(val packageName: String, val label: String, val icon: Drawable?)

private const val CUSTOM_PROBE_URL = "https://example.com/file.bin"
private val CUSTOM_PROBE_MIME_TYPES = listOf("application/octet-stream", "application/zip")

private const val PKG_1DM = "idm.internet.download.manager"
private const val PKG_1DM_PLUS = "idm.internet.download.manager.plus"
private const val PKG_1DM_LITE = "idm.internet.download.manager.adm.lite"
private const val PKG_ADM = "com.dv.adm"

private const val ONEDM_COMPONENT_CLASS = "idm.internet.download.manager.Downloader"
private const val ADM_COMPONENT_CLASS = "com.dv.adm.AEditor"

private fun packageNameFor(context: Context, appId: String): String? = when (appId) {
    DownloadManagerAppIds.ONEDM -> PKG_1DM
    DownloadManagerAppIds.ONEDM_PLUS -> PKG_1DM_PLUS
    DownloadManagerAppIds.ONEDM_LITE -> PKG_1DM_LITE
    DownloadManagerAppIds.ADM -> PKG_ADM
    DownloadManagerAppIds.CUSTOM -> customDownloadManagerPackage(context) ?: ""
    else -> null
}

fun customDownloadManagerPackage(context: Context): String? =
    PreferenceManager.getDefaultSharedPreferences(context)
        .getString(DownloadSettingsKeys.PREF_CUSTOM_DOWNLOAD_MANAGER_PACKAGE, null)
        ?.takeIf { it.isNotBlank() }

fun isDownloadManagerAppInstalled(context: Context, appId: String): Boolean {
    val packageName = packageNameFor(context, appId) ?: return true
    if (packageName.isEmpty()) return appId == DownloadManagerAppIds.CUSTOM
    return isPackageInstalled(context, packageName)
}

private fun isPackageInstalled(context: Context, packageName: String): Boolean = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(packageName, 0)
    }
    true
} catch (e: PackageManager.NameNotFoundException) {
    false
}

fun customDownloadManagerLabel(context: Context, packageName: String): String? = try {
    val pm = context.packageManager
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.getApplicationInfo(packageName, 0)
    }
    pm.getApplicationLabel(info).toString()
} catch (e: PackageManager.NameNotFoundException) {
    null
}

fun queryExternalDownloadManagerApps(context: Context): List<ExternalDownloadManagerApp> {
    val pm = context.packageManager
    val seen = LinkedHashMap<String, ExternalDownloadManagerApp>()
    CUSTOM_PROBE_MIME_TYPES.forEach { mimeType ->
        val probe = Intent(Intent.ACTION_VIEW).apply { setDataAndType(Uri.parse(CUSTOM_PROBE_URL), mimeType) }
        val matches = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(probe, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(probe, PackageManager.MATCH_ALL)
        }
        matches.forEach { info ->
            val packageName = info.activityInfo?.packageName ?: return@forEach
            if (packageName == context.packageName || seen.containsKey(packageName)) return@forEach
            seen[packageName] = ExternalDownloadManagerApp(
                packageName = packageName,
                label = info.loadLabel(pm).toString(),
                icon = runCatching { info.loadIcon(pm) }.getOrNull()
            )
        }
    }
    return seen.values.sortedBy { it.label.lowercase() }
}

fun sendToExternalDownloadManager(
    context: Context,
    appId: String,
    url: String,
    filename: String,
    userAgent: String,
    referer: String,
    cookies: String
): Boolean {
    val packageName = packageNameFor(context, appId)?.takeIf { it.isNotEmpty() } ?: return false
    if (!isPackageInstalled(context, packageName)) return false
    if (appId == DownloadManagerAppIds.CUSTOM) {
        return sendToCustomDownloadManager(context, packageName, url, filename, userAgent, referer, cookies)
    }

    val intent = when (appId) {
        DownloadManagerAppIds.ONEDM, DownloadManagerAppIds.ONEDM_PLUS, DownloadManagerAppIds.ONEDM_LITE ->
            Intent(Intent.ACTION_VIEW).apply {
                setClassName(packageName, ONEDM_COMPONENT_CLASS)
                data = Uri.parse(url)
                putExtra("extra_filename", filename)
                putOneDmSessionExtras(userAgent, referer, cookies)
            }
        DownloadManagerAppIds.ADM ->
            Intent(Intent.ACTION_MAIN).apply {
                setClassName(packageName, ADM_COMPONENT_CLASS)
                putExtra("android.intent.extra.TEXT", url)
                putExtra("com.android.extra.filename", filename)
                putAdmSessionExtras(userAgent, referer, cookies)
            }
        else -> return false
    }

    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }
}

private fun Intent.putOneDmSessionExtras(userAgent: String, referer: String, cookies: String) {
    if (cookies.isNotEmpty()) putExtra("extra_cookies", cookies)
    if (referer.isNotEmpty()) putExtra("extra_referer", referer)
    if (userAgent.isNotEmpty()) putExtra("extra_useragent", userAgent)
}

private fun Intent.putAdmSessionExtras(userAgent: String, referer: String, cookies: String) {
    if (cookies.isNotEmpty()) putExtra("Cookie", cookies)
    if (userAgent.isNotEmpty()) putExtra("User-Agent", userAgent)
    if (referer.isNotEmpty()) putExtra("Referer", referer)
}

private fun Intent.putHeaderBundleExtra(userAgent: String, referer: String, cookies: String) {
    val headers = android.os.Bundle().apply {
        if (cookies.isNotEmpty()) putString("Cookie", cookies)
        if (userAgent.isNotEmpty()) putString("User-Agent", userAgent)
        if (referer.isNotEmpty()) putString("Referer", referer)
    }
    if (!headers.isEmpty) putExtra("headers", headers)
}

private fun sendToCustomDownloadManager(
    context: Context,
    packageName: String,
    url: String,
    filename: String,
    userAgent: String,
    referer: String,
    cookies: String
): Boolean {
    val uri = Uri.parse(url)
    fun Intent.withSession(): Intent = apply {
        if (filename.isNotEmpty()) {
            putExtra(Intent.EXTRA_TITLE, filename)
            putExtra("extra_filename", filename)
            putExtra("com.android.extra.filename", filename)
        }
        putOneDmSessionExtras(userAgent, referer, cookies)
        putAdmSessionExtras(userAgent, referer, cookies)
        putHeaderBundleExtra(userAgent, referer, cookies)
    }
    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setPackage(packageName)
        setDataAndType(uri, CUSTOM_PROBE_MIME_TYPES.first())
    }.withSession()
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        setPackage(packageName)
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }.withSession()
    return listOf(viewIntent, sendIntent).any { intent -> runCatching { context.startActivity(intent) }.isSuccess }
}
