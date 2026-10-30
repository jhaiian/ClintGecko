package com.jhaiian.clint.browser.webview

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.dialogs.HttpAuthRequest
import com.jhaiian.clint.browser.dialogs.HttpsOnlyRequest
import com.jhaiian.clint.browser.dialogs.SslWarningRequest
import com.jhaiian.clint.blocker.engine.WebsiteBlockerEngine
import com.jhaiian.clint.blocker.engine.WebsiteBlockerWebIntegration
import com.jhaiian.clint.mediacapture.MediaCaptureDetector
import com.jhaiian.clint.mediacapture.MediaCaptureStore
import com.jhaiian.clint.settings.sitepermissions.SitePermissionActivity
import com.jhaiian.clint.settings.sitepermissions.SitePermissionDatabase
import com.jhaiian.clint.settings.sitepermissions.SitePermissionManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class ClintWebViewClient(
    private val prefs: SharedPreferences,
    private val isActive: () -> Boolean = { true },
    private val onPageStartedCallback: (String) -> Unit = {},
    private val onPageFinishedCallback: (String) -> Unit = {},
    private val onTabUrlUpdatedCallback: (ClintWebView, String) -> Unit = { _, _ -> },
    private val onWebsiteBlockedCallback: (String) -> Unit = {},
    private val getDesktopHeaders: () -> Map<String, String>? = { null },
    private val getTabId: () -> String = { "" },
    private val onHttpAuthRequest: (HttpAuthRequest) -> Unit = {},
    private val onSslWarning: (SslWarningRequest) -> Unit = {},
    private val isIncognito: () -> Boolean = { false },
    private val onHttpsOnlyBlocked: (HttpsOnlyRequest) -> Unit = {}
) {

    @Volatile private var cachedPageUrl: String? = null

    private val cooldownDomains = mutableMapOf<String, Long>()
    private val allowedHttpHosts: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
    private val upgradedHostOrigins = java.util.concurrent.ConcurrentHashMap<String, String>()
    private val ipv4Regex = Regex("""^(\d{1,3}\.){3}\d{1,3}$""")

    @Volatile private var exceptionCacheHost: String? = null
    @Volatile private var exceptionCacheValid: Boolean = false
    @Volatile private var exceptionCacheState: Boolean = false
    private val exceptionCacheLock = Any()

    companion object {
        private const val COOLDOWN_MS = 4000L
        private const val BROWSER_PACKAGES_TTL_MS = 60_000L
        private val internalSchemes = setOf("about", "data", "blob", "javascript", "file", "view-source", "moz-extension", "resource")
        @Volatile private var browserPackagesCache: Set<String>? = null
        @Volatile private var browserPackagesCachedAt = 0L
    }

    fun upgradeForHttpsOnly(url: String): String {
        val uri = Uri.parse(url)
        if (!uri.scheme.equals("http", ignoreCase = true)) return url
        if (!prefs.getBoolean("https_only", true)) return url
        val hostKey = uri.host?.lowercase() ?: return url
        if (hostKey in allowedHttpHosts || hostKey == "localhost" || hostKey.contains(':') || ipv4Regex.matches(hostKey)) return url
        val builder = uri.buildUpon().scheme("https")
        if (uri.port == 80) builder.encodedAuthority(uri.encodedAuthority?.removeSuffix(":80"))
        upgradedHostOrigins[hostKey] = url
        return builder.build().toString()
    }

    private fun registeredDomain(host: String): String =
        "https://$host".toHttpUrlOrNull()?.topPrivateDomain() ?: host

    private fun isInCooldown(host: String): Boolean {
        val domain = registeredDomain(host)
        val timestamp = cooldownDomains[domain] ?: return false
        if (System.currentTimeMillis() - timestamp >= COOLDOWN_MS) {
            cooldownDomains.remove(domain)
            return false
        }
        return true
    }

    private fun startCooldown(host: String) {
        cooldownDomains[registeredDomain(host)] = System.currentTimeMillis()
    }

    fun onPageStarted(view: ClintWebView, url: String) {
        cachedPageUrl = url
        MediaCaptureStore.updatePageUrl(getTabId(), url)
        exceptionCacheValid = false
        MediaCaptureStore.clearForTab(getTabId())
        if (isActive()) onPageStartedCallback(url)
    }

    fun onPageFinished(view: ClintWebView, url: String) {
        if (url.startsWith("https://", ignoreCase = true)) Uri.parse(url).host?.lowercase()?.let { upgradedHostOrigins.remove(it) }
        cachedPageUrl = url
        MediaCaptureStore.updatePageUrl(getTabId(), url)
        if (url.isNotEmpty()) onTabUrlUpdatedCallback(view, url)
        if (isActive()) onPageFinishedCallback(url) else if (!view.isMediaPlaying) view.evaluateJavascript(TabMediaControl.PAUSE_SCRIPT, null)
    }

    fun doUpdateVisitedHistory(view: ClintWebView, url: String) {
        val changed = cachedPageUrl?.substringBefore('#') != url.substringBefore('#')
        cachedPageUrl = url
        MediaCaptureStore.updatePageUrl(getTabId(), url)
        if (changed) MediaCaptureStore.clearForTab(getTabId())
        onTabUrlUpdatedCallback(view, url)
    }

    fun shouldOverrideUrlLoading(view: ClintWebView, uri: Uri, isRedirect: Boolean): Boolean {
        val scheme = uri.scheme?.lowercase() ?: return true

        if (scheme in internalSchemes) return false

        if (scheme == "intent") {
            return handleIntentScheme(view, uri.toString())
        }

        if (scheme != "http" && scheme != "https") {
            return handleCustomScheme(view, uri)
        }

        if (scheme == "http") {
            val original = uri.toString()
            val upgraded = upgradeForHttpsOnly(original)
            if (upgraded != original) {
                view.loadUrlDeferred(upgraded)
                return true
            }
        }

        if (prefs.getBoolean(com.jhaiian.clint.privacy.PrivacyPrefs.STRIP_TRACKING_PARAMS, com.jhaiian.clint.privacy.PrivacyPrefs.DEFAULT_STRIP_TRACKING_PARAMS)) {
            val original = uri.toString()
            val cleaned = com.jhaiian.clint.privacy.TrackingParams.strip(original)
            if (cleaned != original) {
                view.loadUrlDeferred(cleaned)
                return true
            }
        }

        if (tryOpenInApp(view, uri)) return true

        val websiteBlockerEnabled = prefs.getBoolean("website_blocker_enabled", false)
        if (websiteBlockerEnabled && WebsiteBlockerEngine.isActive && WebsiteBlockerWebIntegration.isHostBlocked(uri.host)) {
            onWebsiteBlockedCallback(uri.toString())
            return true
        }

        return false
    }

    private fun resolveOpenInAppMode(context: android.content.Context, host: String?): String {
        if (isIncognito()) return SitePermissionActivity.PREF_VALUE_ASK
        if (!host.isNullOrEmpty()) {
            val stored = SitePermissionManager.getState(context, host, SitePermissionDatabase.TYPE_OPEN_IN_APP)
            if (stored == SitePermissionDatabase.STATE_STAY || stored == SitePermissionDatabase.STATE_OPEN || stored == SitePermissionDatabase.STATE_ASK) return stored
        }
        return prefs.getString("site_perm_default_${SitePermissionDatabase.TYPE_OPEN_IN_APP}", SitePermissionActivity.PREF_VALUE_ASK)
            ?: SitePermissionActivity.PREF_VALUE_ASK
    }

    private fun rememberOpenInApp(context: android.content.Context, host: String?, state: String) {
        if (isIncognito() || host.isNullOrEmpty()) return
        SitePermissionManager.setState(context, host, SitePermissionDatabase.TYPE_OPEN_IN_APP, state)
    }

    private fun launchOrPromptExternalApp(
        view: ClintWebView,
        activity: android.app.Activity,
        intent: Intent,
        resolveInfo: ResolveInfo,
        sourceHost: String,
        pageHost: String?
    ) {
        val open: () -> Unit = { try { activity.startActivity(intent) } catch (_: ActivityNotFoundException) {} }
        when (resolveOpenInAppMode(activity, pageHost)) {
            SitePermissionDatabase.STATE_OPEN -> activity.runOnUiThread { open() }
            SitePermissionDatabase.STATE_STAY -> Unit
            else -> {
                val pm = activity.packageManager
                val appName = resolveInfo.loadLabel(pm).toString()
                val appIcon = runCatching { resolveInfo.loadIcon(pm) }.getOrNull()
                activity.runOnUiThread {
                    view.pauseTimers()
                    val mainActivity = activity as? com.jhaiian.clint.browser.MainActivity
                    if (mainActivity == null) {
                        open()
                        view.resumeTimers()
                    } else {
                        mainActivity.uiState.openInAppRequest = com.jhaiian.clint.browser.webview.OpenInAppRequest(
                            host = sourceHost,
                            matches = listOf(com.jhaiian.clint.browser.webview.OpenInAppMatch(appName, appIcon, resolveInfo.activityInfo.packageName)),
                            showRemember = !isIncognito() && !pageHost.isNullOrEmpty(),
                            onStayHere = { remember ->
                                view.resumeTimers()
                                if (remember) rememberOpenInApp(activity, pageHost, SitePermissionDatabase.STATE_STAY)
                            },
                            onOpenApp = { _, remember ->
                                view.resumeTimers()
                                if (remember) rememberOpenInApp(activity, pageHost, SitePermissionDatabase.STATE_OPEN)
                                open()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun handleIntentScheme(view: ClintWebView, uriString: String): Boolean {
        return try {
            val intent = Intent.parseUri(uriString, Intent.URI_INTENT_SCHEME).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pm = view.context.packageManager
            val resolveInfo = resolveActivityCompat(pm, intent)
            val activity = view.context as? android.app.Activity

            if (resolveInfo != null && activity != null) {
                val pageHost = view.url?.let { runCatching { Uri.parse(it).host }.getOrNull() }
                val sourceHost = pageHost ?: activity.getString(R.string.open_in_app_dialog_source_fallback)
                launchOrPromptExternalApp(view, activity, intent, resolveInfo, sourceHost, pageHost)
            } else {
                val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                if (!fallbackUrl.isNullOrEmpty()) view.loadUrlDeferred(fallbackUrl)
            }
            true
        } catch (_: Exception) {
            true
        }
    }

    private fun handleCustomScheme(view: ClintWebView, uri: Uri): Boolean {
        val context = view.context
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val resolveInfo = resolveActivityCompat(pm, intent)
        val activity = context as? android.app.Activity

        if (resolveInfo != null && activity != null) {
            val pageHost = view.url?.let { runCatching { Uri.parse(it).host }.getOrNull() }
            val sourceHost = pageHost
                ?: uri.scheme
                ?: activity.getString(R.string.open_in_app_dialog_source_fallback)
            launchOrPromptExternalApp(view, activity, intent, resolveInfo, sourceHost, pageHost)
            return true
        }

        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            true
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveActivityCompat(pm: PackageManager, intent: Intent): ResolveInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            pm.resolveActivity(intent, 0)
        }
    }

    @Suppress("DEPRECATION")
    private fun queryActivities(pm: PackageManager, intent: Intent): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            pm.queryIntentActivities(intent, 0)
        }
    }

    private fun cachedBrowserPackages(pm: PackageManager): Set<String> {
        val now = android.os.SystemClock.elapsedRealtime()
        val cached = browserPackagesCache
        if (cached != null && now - browserPackagesCachedAt < BROWSER_PACKAGES_TTL_MS) return cached
        val packages = (
            queryActivities(pm, Intent(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                data = Uri.parse("http://example.com/")
            }) +
            queryActivities(pm, Intent(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                data = Uri.parse("https://example.com/")
            })
        ).map { it.activityInfo.packageName }.toSet()
        browserPackagesCache = packages
        browserPackagesCachedAt = now
        return packages
    }

    fun resolveAppMatches(uri: Uri, context: android.content.Context): List<ResolveInfo> {
        val pm = context.packageManager
        val browserPackages = cachedBrowserPackages(pm)

        return queryActivities(pm, Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addCategory(Intent.CATEGORY_DEFAULT)
        }).filter { ri ->
            val pkg = ri.activityInfo.packageName
            pkg != context.packageName && pkg !in browserPackages
        }
    }

    fun tryOpenInApp(view: ClintWebView, uri: Uri, force: Boolean = false): Boolean {
        val uriStr = uri.toString()
        val host = uri.host ?: uriStr

        if (!force && isInCooldown(host)) return false

        val context = view.context
        val mode = if (force) SitePermissionActivity.PREF_VALUE_ASK else resolveOpenInAppMode(context, uri.host)
        if (mode == SitePermissionDatabase.STATE_STAY) return false

        val pm = context.packageManager

        val browserPackages = cachedBrowserPackages(pm)

        val appMatches = queryActivities(pm, Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addCategory(Intent.CATEGORY_DEFAULT)
        }).filter { ri ->
            val pkg = ri.activityInfo.packageName
            pkg != context.packageName && pkg !in browserPackages
        }

        if (appMatches.isEmpty()) return false

        val activity = context as? com.jhaiian.clint.browser.MainActivity ?: return false

        if (mode == SitePermissionDatabase.STATE_OPEN && appMatches.size == 1) {
            val directIntent = Intent(Intent.ACTION_VIEW, uri)
                .setPackage(appMatches[0].activityInfo.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.runOnUiThread {
                try { context.startActivity(directIntent) } catch (_: ActivityNotFoundException) {}
            }
            return true
        }

        val canRemember = !force && !isIncognito() && mode == SitePermissionDatabase.STATE_ASK && !uri.host.isNullOrEmpty()

        activity.runOnUiThread {
            val matches = appMatches.map { ri ->
                com.jhaiian.clint.browser.webview.OpenInAppMatch(
                    label = ri.loadLabel(pm).toString(),
                    icon = runCatching { ri.loadIcon(pm) }.getOrNull(),
                    packageName = ri.activityInfo.packageName
                )
            }
            activity.uiState.openInAppRequest = com.jhaiian.clint.browser.webview.OpenInAppRequest(
                host = host,
                matches = matches,
                showRemember = canRemember,
                onStayHere = { remember ->
                    if (remember) rememberOpenInApp(context, uri.host, SitePermissionDatabase.STATE_STAY)
                    startCooldown(host)
                    view.loadUrlDeferred(uriStr)
                },
                onOpenApp = { packageName, remember ->
                    if (remember) rememberOpenInApp(context, uri.host, SitePermissionDatabase.STATE_OPEN)
                    val specificIntent = Intent(Intent.ACTION_VIEW, uri)
                        .setPackage(packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try { context.startActivity(specificIntent) } catch (_: ActivityNotFoundException) {}
                }
            )
        }
        return true
    }

    fun isMediaCaptureEnabled(): Boolean =
        prefs.getBoolean(com.jhaiian.clint.mediacapture.MEDIA_CAPTURE_ENABLED_PREF, true)

    fun onMediaObserved(request: ClintRequest, pageUrl: String?) {
        if (isMediaCaptureEnabled()) {
            MediaCaptureDetector.onRequestObserved(getTabId(), pageUrl ?: cachedPageUrl, request)
        }
    }

    fun onPageMediaFound(message: org.json.JSONObject) {
        if (!isMediaCaptureEnabled()) return
        val items = message.optJSONArray("items") ?: return
        val candidates = (0 until minOf(items.length(), 200)).mapNotNull { index ->
            val item = items.optJSONObject(index) ?: return@mapNotNull null
            val url = item.optString("u").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            com.jhaiian.clint.mediacapture.PageMediaCandidate(
                url = url,
                label = item.optString("l").takeIf { it.isNotBlank() },
                language = item.optString("g").takeIf { it.isNotBlank() }
            )
        }
        if (candidates.isEmpty()) return
        MediaCaptureDetector.onPageMediaFound(
            getTabId(),
            MediaCaptureStore.pageUrlFor(getTabId()) ?: cachedPageUrl,
            message.optString("f").takeIf { it.isNotBlank() },
            message.optString("ua").takeIf { it.isNotBlank() },
            candidates
        )
    }

    fun onAuthRequest(
        view: ClintWebView,
        host: String,
        realm: String?,
        onSignIn: (String, String) -> Unit,
        onCancel: () -> Unit
    ) {
        if (!isActive()) {
            onCancel()
            return
        }
        val isSecure = view.url?.startsWith("http://", ignoreCase = true) != true
        onHttpAuthRequest(
            HttpAuthRequest(
                host = host,
                realm = realm.orEmpty(),
                isSecure = isSecure,
                onSignIn = onSignIn,
                onCancel = onCancel
            )
        )
    }

    fun onReceivedError(view: ClintWebView, uri: Uri, isHostLookup: Boolean): Boolean {
        val hostKey = uri.host?.lowercase() ?: return false
        val origin = upgradedHostOrigins.remove(hostKey) ?: return false
        if (isHostLookup) return false
        onHttpsOnlyBlocked(
            HttpsOnlyRequest(
                host = hostKey,
                onProceed = {
                    allowedHttpHosts.add(hostKey)
                    view.loadUrlDeferred(origin)
                },
                onCancel = {}
            )
        )
        return true
    }
}
