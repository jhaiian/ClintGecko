package com.jhaiian.clint.downloads

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object DownloadManagerAppIds {
    const val CLINT = "clint"
    const val ONEDM = "1dm"
    const val ONEDM_PLUS = "1dm_plus"
    const val ONEDM_LITE = "1dm_lite"
    const val ADM = "adm"
}

private const val PKG_1DM = "idm.internet.download.manager"
private const val PKG_1DM_PLUS = "idm.internet.download.manager.plus"
private const val PKG_1DM_LITE = "idm.internet.download.manager.adm.lite"
private const val PKG_ADM = "com.dv.adm"

private const val ONEDM_COMPONENT_CLASS = "idm.internet.download.manager.Downloader"
private const val ADM_COMPONENT_CLASS = "com.dv.adm.AEditor"

private fun packageNameFor(appId: String): String? = when (appId) {
    DownloadManagerAppIds.ONEDM -> PKG_1DM
    DownloadManagerAppIds.ONEDM_PLUS -> PKG_1DM_PLUS
    DownloadManagerAppIds.ONEDM_LITE -> PKG_1DM_LITE
    DownloadManagerAppIds.ADM -> PKG_ADM
    else -> null
}

fun isDownloadManagerAppInstalled(context: Context, appId: String): Boolean {
    val packageName = packageNameFor(appId) ?: return true
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
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
    val packageName = packageNameFor(appId) ?: return false
    if (!isDownloadManagerAppInstalled(context, appId)) return false

    val intent = when (appId) {
        DownloadManagerAppIds.ONEDM, DownloadManagerAppIds.ONEDM_PLUS, DownloadManagerAppIds.ONEDM_LITE ->
            Intent(Intent.ACTION_VIEW).apply {
                setClassName(packageName, ONEDM_COMPONENT_CLASS)
                data = Uri.parse(url)
                putExtra("extra_filename", filename)
                if (cookies.isNotEmpty()) putExtra("extra_cookies", cookies)
                if (referer.isNotEmpty()) putExtra("extra_referer", referer)
                if (userAgent.isNotEmpty()) putExtra("extra_useragent", userAgent)
            }
        DownloadManagerAppIds.ADM ->
            Intent(Intent.ACTION_MAIN).apply {
                setClassName(packageName, ADM_COMPONENT_CLASS)
                putExtra("android.intent.extra.TEXT", url)
                putExtra("com.android.extra.filename", filename)
                if (cookies.isNotEmpty()) putExtra("Cookie", cookies)
                if (userAgent.isNotEmpty()) putExtra("User-Agent", userAgent)
                if (referer.isNotEmpty()) putExtra("Referer", referer)
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
