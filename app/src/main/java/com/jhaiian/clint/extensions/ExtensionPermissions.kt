package com.jhaiian.clint.extensions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Block
import androidx.compose.ui.graphics.vector.ImageVector
import com.jhaiian.clint.R

class PermissionSpec(val labelRes: Int?, val icon: ImageVector, val risk: ExtensionRisk)

class HostAccess(val allSites: Boolean, val domains: List<String>)

object ExtensionPermissions {

    private val specs: Map<String, PermissionSpec> = mapOf(
        "activeTab" to PermissionSpec(R.string.ext_perm_activeTab, Icons.Filled.Tab, ExtensionRisk.LOW),
        "alarms" to PermissionSpec(R.string.ext_perm_alarms, Icons.Filled.Schedule, ExtensionRisk.LOW),
        "bookmarks" to PermissionSpec(R.string.ext_perm_bookmarks, Icons.Filled.Bookmark, ExtensionRisk.MEDIUM),
        "browserSettings" to PermissionSpec(R.string.ext_perm_browserSettings, Icons.Filled.Tune, ExtensionRisk.MEDIUM),
        "browsingData" to PermissionSpec(R.string.ext_perm_browsingData, Icons.Filled.History, ExtensionRisk.MEDIUM),
        "clipboardRead" to PermissionSpec(R.string.ext_perm_clipboardRead, Icons.Filled.ContentPaste, ExtensionRisk.HIGH),
        "clipboardWrite" to PermissionSpec(R.string.ext_perm_clipboardWrite, Icons.Filled.ContentPaste, ExtensionRisk.LOW),
        "contextMenus" to PermissionSpec(R.string.ext_perm_contextMenus, Icons.Filled.Extension, ExtensionRisk.LOW),
        "menus" to PermissionSpec(R.string.ext_perm_contextMenus, Icons.Filled.Extension, ExtensionRisk.LOW),
        "cookies" to PermissionSpec(R.string.ext_perm_cookies, Icons.Filled.Cookie, ExtensionRisk.HIGH),
        "declarativeNetRequest" to PermissionSpec(R.string.ext_perm_declarativeNetRequest, Icons.Filled.Block, ExtensionRisk.MEDIUM),
        "declarativeNetRequestWithHostAccess" to PermissionSpec(R.string.ext_perm_declarativeNetRequest, Icons.Filled.Block, ExtensionRisk.MEDIUM),
        "declarativeNetRequestFeedback" to PermissionSpec(R.string.ext_perm_webNavigation, Icons.Filled.Visibility, ExtensionRisk.MEDIUM),
        "dns" to PermissionSpec(R.string.ext_perm_dns, Icons.Filled.Dns, ExtensionRisk.LOW),
        "downloads" to PermissionSpec(R.string.ext_perm_downloads, Icons.Filled.Download, ExtensionRisk.MEDIUM),
        "downloads.open" to PermissionSpec(R.string.ext_perm_downloads, Icons.Filled.Download, ExtensionRisk.MEDIUM),
        "find" to PermissionSpec(R.string.ext_perm_find, Icons.Filled.Search, ExtensionRisk.LOW),
        "geolocation" to PermissionSpec(R.string.ext_perm_geolocation, Icons.Filled.LocationOn, ExtensionRisk.HIGH),
        "history" to PermissionSpec(R.string.ext_perm_history, Icons.Filled.History, ExtensionRisk.HIGH),
        "identity" to PermissionSpec(R.string.ext_perm_identity, Icons.Filled.Key, ExtensionRisk.MEDIUM),
        "management" to PermissionSpec(R.string.ext_perm_management, Icons.Filled.Extension, ExtensionRisk.HIGH),
        "nativeMessaging" to PermissionSpec(R.string.ext_perm_nativeMessaging, Icons.Filled.Sync, ExtensionRisk.HIGH),
        "notifications" to PermissionSpec(R.string.ext_perm_notifications, Icons.Filled.Notifications, ExtensionRisk.LOW),
        "privacy" to PermissionSpec(R.string.ext_perm_privacy, Icons.Filled.Security, ExtensionRisk.MEDIUM),
        "proxy" to PermissionSpec(R.string.ext_perm_proxy, Icons.Filled.Language, ExtensionRisk.HIGH),
        "scripting" to PermissionSpec(R.string.ext_perm_scripting, Icons.Filled.Code, ExtensionRisk.MEDIUM),
        "userScripts" to PermissionSpec(R.string.ext_perm_userScripts, Icons.Filled.Code, ExtensionRisk.MEDIUM),
        "search" to PermissionSpec(R.string.ext_perm_search, Icons.Filled.Search, ExtensionRisk.LOW),
        "sessions" to PermissionSpec(R.string.ext_perm_sessions, Icons.Filled.History, ExtensionRisk.MEDIUM),
        "storage" to PermissionSpec(R.string.ext_perm_storage, Icons.Filled.Storage, ExtensionRisk.LOW),
        "unlimitedStorage" to PermissionSpec(R.string.ext_perm_unlimitedStorage, Icons.Filled.Storage, ExtensionRisk.LOW),
        "tabs" to PermissionSpec(R.string.ext_perm_tabs, Icons.Filled.Tab, ExtensionRisk.MEDIUM),
        "theme" to PermissionSpec(R.string.ext_perm_theme, Icons.Filled.Palette, ExtensionRisk.LOW),
        "topSites" to PermissionSpec(R.string.ext_perm_topSites, Icons.Filled.History, ExtensionRisk.MEDIUM),
        "webNavigation" to PermissionSpec(R.string.ext_perm_webNavigation, Icons.Filled.Visibility, ExtensionRisk.MEDIUM),
        "webRequest" to PermissionSpec(R.string.ext_perm_webRequest, Icons.Filled.Visibility, ExtensionRisk.MEDIUM),
        "webRequestBlocking" to PermissionSpec(R.string.ext_perm_webRequestBlocking, Icons.Filled.Lock, ExtensionRisk.HIGH),
        "webRequestAuthProvider" to PermissionSpec(R.string.ext_perm_webRequestBlocking, Icons.Filled.Lock, ExtensionRisk.HIGH),
        "webRequestFilterResponse" to PermissionSpec(R.string.ext_perm_webRequestBlocking, Icons.Filled.Lock, ExtensionRisk.HIGH)
    )

    private val hidden = setOf("geckoViewAddons", "mozillaAddons", "telemetry", "tabHide", "contextualIdentities", "captivePortal", "devtools", "pkcs11", "theme.experiment")

    private val dataLabels: Map<String, Int> = mapOf(
        "authenticationInfo" to R.string.ext_data_authenticationInfo,
        "bookmarksInfo" to R.string.ext_data_bookmarksInfo,
        "browsingActivity" to R.string.ext_data_browsingActivity,
        "financialAndPaymentInfo" to R.string.ext_data_financialAndPaymentInfo,
        "healthInfo" to R.string.ext_data_healthInfo,
        "locationInfo" to R.string.ext_data_locationInfo,
        "personalCommunications" to R.string.ext_data_personalCommunications,
        "personallyIdentifyingInfo" to R.string.ext_data_personallyIdentifyingInfo,
        "searchTerms" to R.string.ext_data_searchTerms,
        "websiteActivity" to R.string.ext_data_websiteActivity,
        "websiteContent" to R.string.ext_data_websiteContent,
        "technicalAndInteraction" to R.string.ext_data_technicalAndInteraction
    )

    fun spec(permission: String): PermissionSpec =
        specs[permission] ?: PermissionSpec(null, Icons.Filled.Extension, ExtensionRisk.LOW)

    fun visible(permissions: List<String>): List<String> =
        permissions.filter { it !in hidden }.distinctBy { specs[it]?.labelRes?.toString() ?: it }

    fun dataLabelRes(key: String): Int? = dataLabels[key]

    fun hostAccess(origins: List<String>): HostAccess {
        var all = false
        val domains = LinkedHashSet<String>()
        for (origin in origins) {
            if (origin == "<all_urls>" || origin == "*://*/*" || origin == "http://*/*" || origin == "https://*/*" ||
                origin == "*://*/" || origin == "https://*/" || origin == "http://*/" || origin == "file:///*"
            ) {
                if (origin != "file:///*") all = true
                continue
            }
            val host = origin.substringAfter("://", origin).substringBefore('/').removePrefix("*.").substringBefore(':')
            if (host.isNotBlank() && host != "*") domains.add(host)
        }
        return HostAccess(all, domains.toList())
    }

    fun risk(permissions: List<String>, origins: List<String>): ExtensionRisk {
        var score = 0
        for (p in visible(permissions)) {
            score += when (spec(p).risk) {
                ExtensionRisk.HIGH -> 3
                ExtensionRisk.MEDIUM -> 1
                ExtensionRisk.LOW -> 0
            }
        }
        val hosts = hostAccess(origins)
        score += if (hosts.allSites) 4 else if (hosts.domains.isNotEmpty()) 1 else 0
        return when {
            score >= 6 -> ExtensionRisk.HIGH
            score >= 2 -> ExtensionRisk.MEDIUM
            else -> ExtensionRisk.LOW
        }
    }
}
