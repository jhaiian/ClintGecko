package com.jhaiian.clint.browser.delegates

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.home.isClintHomeUrl
import com.jhaiian.clint.browser.siteinfo.SiteInfoCertificate
import com.jhaiian.clint.browser.siteinfo.SiteInfoPermission
import com.jhaiian.clint.browser.siteinfo.SiteInfoRequest
import com.jhaiian.clint.browser.siteinfo.SiteInfoTrackerCategory
import com.jhaiian.clint.browser.siteinfo.SiteInfoTrackerGroup
import com.jhaiian.clint.browser.siteinfo.SiteInfoTrackers
import com.jhaiian.clint.browser.webview.ClintGecko
import com.jhaiian.clint.settings.SettingsActivity
import com.jhaiian.clint.settings.sitepermissions.SitePermissionDatabase
import com.jhaiian.clint.settings.sitepermissions.SitePermissionManager
import com.jhaiian.clint.ui.showClintSnackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.ContentBlockingController
import java.security.cert.X509Certificate
import javax.security.auth.x500.X500Principal

private val SiteInfoPermissionTypes = listOf(
    SitePermissionDatabase.TYPE_CAMERA,
    SitePermissionDatabase.TYPE_MIC,
    SitePermissionDatabase.TYPE_LOCATION,
    SitePermissionDatabase.TYPE_NOTIFICATION,
    SitePermissionDatabase.TYPE_CLIPBOARD
)

internal fun MainActivity.openSiteInfo(): Boolean {
    val tab = tabManager.activeTab ?: return false
    val webView = tab.webView
    val url = webView.url?.takeUnless { isClintHomeUrl(it) } ?: return false
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase()
    if (scheme != "http" && scheme != "https") return false
    val host = uri.host ?: return false
    val security = webView.securityInfo?.takeIf { it.host.equals(host, ignoreCase = true) }
    val isSecure = scheme == "https" && (security?.isSecure ?: true)
    val certificate = if (scheme == "https") security?.certificate?.let(::siteInfoCertificate) else null
    val canManageCookies = !tab.isIncognito
    val trackingProtection = if (ClintGecko.isTrackingProtectionEnabled && webView.trackingPermission != null) {
        !webView.hasTrackingProtectionException
    } else {
        null
    }

    lifecycleScope.launch {
        val permissions = withContext(Dispatchers.IO) { loadSiteInfoPermissions(host) }
        uiState.siteInfoRequest = SiteInfoRequest(
            url = url,
            host = host,
            isSecure = isSecure,
            isCertificateException = security?.isException == true,
            certificate = certificate,
            permissions = permissions,
            canManageCookies = canManageCookies,
            trackingProtection = trackingProtection
        )
        if (trackingProtection == true) {
            webView.loadContentBlockingLog { entries ->
                val trackers = siteInfoTrackers(entries)
                uiState.siteInfoRequest?.takeIf { it.url == url }?.let { uiState.siteInfoRequest = it.copy(trackers = trackers) }
            }
        }
    }
    return true
}

internal fun MainActivity.changeSiteInfoPermission(host: String, type: String, state: String?) {
    lifecycleScope.launch {
        val permissions = withContext(Dispatchers.IO) {
            if (state == null) {
                SitePermissionManager.deleteEntry(this@changeSiteInfoPermission, host, type)
            } else {
                SitePermissionManager.setState(this@changeSiteInfoPermission, host, type, state)
            }
            loadSiteInfoPermissions(host)
        }
        uiState.siteInfoRequest?.takeIf { it.host == host }?.let { uiState.siteInfoRequest = it.copy(permissions = permissions) }
    }
}

internal fun MainActivity.resetSiteInfoPermissions(host: String) {
    lifecycleScope.launch {
        withContext(Dispatchers.IO) {
            SiteInfoPermissionTypes.forEach { SitePermissionManager.deleteEntry(this@resetSiteInfoPermissions, host, it) }
        }
        uiState.siteInfoRequest?.takeIf { it.host == host }?.let { uiState.siteInfoRequest = it.copy(permissions = emptyList()) }
        showClintSnackbar(getString(R.string.site_info_permissions_reset, host.removePrefix("www.")))
    }
}

internal fun MainActivity.setSiteInfoTrackingProtection(host: String, enabled: Boolean) {
    val webView = tabManager.activeTab?.webView ?: return
    if (!webView.setTrackingProtectionException(!enabled)) return
    uiState.siteInfoRequest?.takeIf { it.host == host }?.let { uiState.siteInfoRequest = it.copy(trackingProtection = enabled, trackers = null) }
    webView.reload()
}

internal fun MainActivity.openSiteInfoCookies() {
    uiState.siteInfoRequest = null
    openCookiesControl()
}

internal fun MainActivity.openSiteInfoSiteSettings() {
    uiState.siteInfoRequest = null
    startActivity(
        Intent(this, SettingsActivity::class.java)
            .putExtra(SettingsActivity.EXTRA_OPEN_FRAGMENT, "site_settings")
    )
}

private fun MainActivity.loadSiteInfoPermissions(host: String): List<SiteInfoPermission> =
    SiteInfoPermissionTypes.mapNotNull { type ->
        when (val state = SitePermissionManager.getState(this, host, type)) {
            SitePermissionDatabase.STATE_ALLOW, SitePermissionDatabase.STATE_DENY -> SiteInfoPermission(type, state)
            else -> null
        }
    }

private val SiteInfoTrackerEvents = listOf(
    SiteInfoTrackerCategory.SOCIAL to (ContentBlockingController.Event.BLOCKED_SOCIALTRACKING_CONTENT or ContentBlockingController.Event.COOKIES_BLOCKED_SOCIALTRACKER),
    SiteInfoTrackerCategory.COOKIES to (ContentBlockingController.Event.COOKIES_BLOCKED_TRACKER or ContentBlockingController.Event.COOKIES_BLOCKED_FOREIGN),
    SiteInfoTrackerCategory.TRACKING_CONTENT to (ContentBlockingController.Event.BLOCKED_TRACKING_CONTENT or ContentBlockingController.Event.BLOCKED_EMAILTRACKING_CONTENT),
    SiteInfoTrackerCategory.FINGERPRINTERS to ContentBlockingController.Event.BLOCKED_FINGERPRINTING_CONTENT,
    SiteInfoTrackerCategory.CRYPTOMINERS to ContentBlockingController.Event.BLOCKED_CRYPTOMINING_CONTENT
)

private fun siteInfoTrackers(entries: List<ContentBlockingController.LogEntry>): SiteInfoTrackers {
    val groups = SiteInfoTrackerEvents.mapNotNull { (category, mask) ->
        val origins = entries
            .filter { entry -> entry.blockingData.orEmpty().any { it.blocked && (it.category and mask) != 0 } }
            .map { entry -> runCatching { Uri.parse(entry.origin).host }.getOrNull() ?: entry.origin }
            .distinct()
            .sorted()
        origins.takeIf { it.isNotEmpty() }?.let { SiteInfoTrackerGroup(category, it) }
    }
    return SiteInfoTrackers(total = groups.flatMap { it.origins }.distinct().size, groups = groups)
}

private fun siteInfoCertificate(certificate: X509Certificate): SiteInfoCertificate =
    SiteInfoCertificate(
        issuedTo = principalName(certificate.subjectX500Principal),
        issuedBy = principalName(certificate.issuerX500Principal, preferOrganization = true),
        validFrom = runCatching { certificate.notBefore.time }.getOrDefault(0L),
        validUntil = runCatching { certificate.notAfter.time }.getOrDefault(0L)
    )

private fun principalName(principal: X500Principal, preferOrganization: Boolean = false): String? {
    val attributes = runCatching { parseDistinguishedName(principal.getName(X500Principal.RFC2253)) }.getOrDefault(emptyMap())
    val commonName = attributes["CN"]
    val organization = attributes["O"]
    return if (preferOrganization) organization ?: commonName else commonName ?: organization
}

private fun parseDistinguishedName(name: String): Map<String, String> {
    val result = mutableMapOf<String, String>()
    val current = StringBuilder()
    var escaped = false
    val parts = mutableListOf<String>()
    for (c in name) {
        when {
            escaped -> { current.append(c); escaped = false }
            c == '\\' -> escaped = true
            c == ',' || c == '+' -> { parts.add(current.toString()); current.clear() }
            else -> current.append(c)
        }
    }
    parts.add(current.toString())
    for (part in parts) {
        val key = part.substringBefore('=', "").trim().uppercase()
        val value = part.substringAfter('=', "").trim()
        if (key.isNotEmpty() && value.isNotEmpty() && key !in result) result[key] = value
    }
    return result
}
