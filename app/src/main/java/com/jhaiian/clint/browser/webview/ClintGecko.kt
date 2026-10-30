package com.jhaiian.clint.browser.webview

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import com.jhaiian.clint.settings.sitepermissions.SitePermissionDatabase
import com.jhaiian.clint.settings.sitepermissions.SitePermissionManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoPreferenceController
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.WebExtension
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object ClintGecko {

    private const val EXT_ID = "clint@jhaiian.com"
    private const val EXT_URI = "resource://android/assets/clint-ext/"
    private const val BG_APP = "clintBg"
    const val CONTENT_APP = "clint"
    private const val READY_TIMEOUT_MS = 8000L
    private const val MAX_PENDING_BINDINGS = 64
    private const val RUNNING_CRITICAL = 15
    private const val TRIM_MEMORY_BACKGROUND_LEVEL = 40
    private const val TRIM_MEMORY_MODERATE_LEVEL = 60
    private const val TRIM_DEBOUNCE_MS = 5000L
    private const val COOKIE_OP_TIMEOUT_MS = 8000L
    private const val UNSIGNED_INSTALL_TIMEOUT_MS = 30000L
    private const val MAX_QUEUED_OBSERVATION_TABS = 16
    private const val MAX_QUEUED_OBSERVATIONS = 48
    private const val OBSERVATION_BIND_GRACE_MS = 4000L
    const val PREF_WEBSITE_ISOLATION = "website_isolation_enabled"
    const val DEFAULT_WEBSITE_ISOLATION = false

    private lateinit var appContext: Context
    private var runtimeRef: GeckoRuntime? = null
    val runtime: GeckoRuntime get() = runtimeRef!!

    val main = Handler(Looper.getMainLooper())
    val io: ExecutorService = idlePool(3)
    val bridgeExecutor: ExecutorService = idlePool(1)

    private fun idlePool(size: Int): ExecutorService =
        ThreadPoolExecutor(size, size, 30L, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>()).apply {
            allowCoreThreadTimeOut(true)
        }

    @Volatile var isReady = false
        private set
    private val waiters = ArrayList<() -> Unit>()
    @Volatile var extension: WebExtension? = null
        private set
    private var bgPort: WebExtension.Port? = null

    private val views = Collections.synchronizedMap(WeakHashMap<GeckoSession, WeakReference<ClintWebView>>())
    private val pendingNonces = ConcurrentHashMap<String, WeakReference<ClintWebView>>()
    private val pendingTabs = ConcurrentHashMap<String, Pair<Int, String>>()
    private val tabByExt = ConcurrentHashMap<Int, WeakReference<ClintWebView>>()
    private val queuedObservations = LinkedHashMap<Int, MutableList<JSONObject>>()
    private val storeByProfile = ConcurrentHashMap<String, String>()
    private val cookieWaiters = ConcurrentHashMap<Int, (String) -> Unit>()
    private val cookieSeq = AtomicInteger()
    private val cookieOpWaiters = ConcurrentHashMap<Int, (Any?) -> Unit>()
    private val unsignedWaiters = ConcurrentHashMap<Int, (JSONObject?) -> Unit>()
    private val unsignedQueue = ArrayList<JSONObject>()

    @Volatile var foregroundView: WeakReference<ClintWebView>? = null

    @Volatile var configProvider: (() -> JSONObject)? = null

    private class ScriptEntry(val code: String, val topOnly: Boolean)

    private data class BridgeMethodKey(val type: Class<*>, val name: String, val arity: Int)

    private val scripts = LinkedHashMap<String, ScriptEntry>()
    private val specs = ConcurrentHashMap<String, List<Pair<String, String>>>()
    private val specRefs = ConcurrentHashMap<String, WeakReference<Any>>()
    private val interfaceMethodNames = ConcurrentHashMap<Class<*>, List<String>>()
    private val bridgeMethods = ConcurrentHashMap<BridgeMethodKey, Method>()
    private var bundleSeq = 0
    private var bundleScheduled = false
    @Volatile private var runtimeJs: String? = null
    @Volatile private var lastTrimAt = 0L
    @Volatile private var bundleInFlight = false
    private val syncWaiters = ArrayList<() -> Unit>()

    private val kinds = mapOf(
        "ClintNotificationBridge.getPermissionState" to "p",
        "ClintDataSaverBridge.isExcepted" to "x"
    )

    val geckoVersion: String by lazy {
        runCatching {
            Class.forName("org.mozilla.geckoview.BuildConfig").getField("MOZ_APP_VERSION").get(null) as String
        }.getOrDefault("159.0")
    }

    fun nativeUserAgent(): String {
        val major = geckoVersion.substringBefore('.')
        return "Mozilla/5.0 (Android ${Build.VERSION.RELEASE}; Mobile; rv:$major.0) Gecko/$major.0 Firefox/$major.0"
    }

    fun genericUserAgent(): String {
        val major = geckoVersion.substringBefore('.')
        return "Mozilla/5.0 (Android 10; Mobile; rv:$major.0) Gecko/$major.0 Firefox/$major.0"
    }

    fun isGenericUserAgentEnabled(): Boolean {
        if (!::appContext.isInitialized) return true
        val context = appContext
        return runCatching {
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("custom_user_agent", true)
        }.getOrDefault(true)
    }

    fun defaultUserAgent(): String =
        if (isGenericUserAgentEnabled()) genericUserAgent() else nativeUserAgent()

    fun init(context: Context) {
        if (runtimeRef != null) return
        appContext = context.applicationContext
        val startupPrefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(appContext)
        val aboutConfigEnabled = startupPrefs.getBoolean("about_config_enabled", false)
        val websiteIsolationEnabled = startupPrefs.getBoolean(PREF_WEBSITE_ISOLATION, DEFAULT_WEBSITE_ISOLATION)
        val settings = GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(true)
            .aboutConfigEnabled(aboutConfigEnabled)
            .fissionEnabled(websiteIsolationEnabled)
            .remoteDebuggingEnabled(false)
            .extensionsWebAPIEnabled(true)
            .consoleOutput((appContext.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0)
            .crashHandler(com.jhaiian.clint.crash.GeckoCrashService::class.java)
            .build()
        runCatching { com.jhaiian.clint.backup.ExtensionBackup.applyPending(appContext) }
        runCatching { com.jhaiian.clint.backup.CookieBackup.applyPending(appContext) }
        runtimeRef = GeckoRuntime.create(appContext, settings)
        disableTelemetry()
        applyFontSize(appContext)
        applyForceZoom(appContext)
        com.jhaiian.clint.translation.ClintTranslations.applyPrefs(appContext)
        SecureDns.apply(appContext)
        com.jhaiian.clint.privacy.PrivacyPrefs.applyGeckoPrefs(appContext)
        applyTrackingProtection(appContext)
        com.jhaiian.clint.privacy.PrivacyGuard.onRuntimeReady(appContext)
        com.jhaiian.clint.extensions.ClintExtensions.init(appContext, runtime)
        installExtension()
        main.postDelayed({ markReady() }, READY_TIMEOUT_MS)
        io.execute {
            runCatching { loadRuntime() }
            runCatching { "https://www.example.com/".toHttpUrlOrNull()?.topPrivateDomain() }
        }
    }

    private val telemetryOffBooleans = listOf(
        "datareporting.healthreport.uploadEnabled",
        "datareporting.policy.dataSubmissionEnabled",
        "datareporting.usage.uploadEnabled",
        "toolkit.telemetry.enabled",
        "toolkit.telemetry.unified",
        "toolkit.telemetry.archive.enabled",
        "toolkit.telemetry.newProfilePing.enabled",
        "toolkit.telemetry.shutdownPingSender.enabled",
        "toolkit.telemetry.updatePing.enabled",
        "toolkit.telemetry.bhrPing.enabled",
        "toolkit.telemetry.firstShutdownPing.enabled",
        "toolkit.telemetry.coverage.opt-out",
        "toolkit.coverage.opt-out",
        "browser.ping-centre.telemetry",
        "browser.discovery.enabled",
        "browser.crashReports.unsubmittedCheck.enabled",
        "browser.crashReports.unsubmittedCheck.autoSubmit2",
        "app.shield.optoutstudies.enabled",
        "app.normandy.enabled",
        "messaging-system.rsexperimentloader.enabled",
        "network.captive-portal-service.enabled",
        "network.connectivity-service.enabled"
    )

    private val telemetryOffStrings = mapOf(
        "toolkit.telemetry.server" to "data:,",
        "toolkit.telemetry.server_owner" to "",
        "toolkit.coverage.endpoint.base" to "",
        "datareporting.healthreport.about.reportUrl" to ""
    )

    private fun disableTelemetry() {
        telemetryOffBooleans.forEach { pref ->
            runCatching { GeckoPreferenceController.setGeckoPref(pref, false, GeckoPreferenceController.PREF_BRANCH_USER) }
        }
        telemetryOffStrings.forEach { (pref, value) ->
            runCatching { GeckoPreferenceController.setGeckoPref(pref, value, GeckoPreferenceController.PREF_BRANCH_USER) }
        }
    }

    fun whenReady(action: () -> Unit) {
        if (isReady) {
            action()
            return
        }
        synchronized(waiters) { waiters.add(action) }
    }

    private fun markReady() {
        if (isReady) return
        isReady = true
        val list = synchronized(waiters) { ArrayList(waiters).also { waiters.clear() } }
        list.forEach { it() }
    }

    private fun installExtension() {
        runtime.webExtensionController.ensureBuiltIn(EXT_URI, EXT_ID).accept(
            { ext ->
                if (ext != null) {
                    extension = ext
                    ext.setMessageDelegate(backgroundDelegate, BG_APP)
                    synchronized(views) { views.values.toList() }.forEach { it.get()?.attachExtension(ext) }
                } else {
                    markReady()
                }
            },
            { markReady() }
        )
    }

    private val backgroundDelegate = object : WebExtension.MessageDelegate {
        override fun onConnect(port: WebExtension.Port) {
            bgPort = port
            port.setDelegate(object : WebExtension.PortDelegate {
                override fun onPortMessage(message: Any, port: WebExtension.Port) {
                    (message as? JSONObject)?.let { onBackgroundPortMessage(it) }
                }

                override fun onDisconnect(port: WebExtension.Port) {
                    if (bgPort === port) bgPort = null
                }
            })
        }

        override fun onMessage(nativeApp: String, message: Any, sender: WebExtension.MessageSender): GeckoResult<Any>? {
            val msg = message as? JSONObject ?: return null
            return when (msg.optString("k")) {
                "obs" -> {
                    handleObservation(msg)
                    null
                }
                "tab" -> {
                    onTabInfo(msg.optString("n"), msg.optInt("tabId", -1), msg.optString("storeId"))
                    null
                }
                else -> null
            }
        }
    }

    private fun onBackgroundPortMessage(msg: JSONObject) {
        when (msg.optString("k")) {
            "ready" -> {
                pushConfig()
                pushBundle()
                flushUnsignedQueue()
                com.jhaiian.clint.extensions.ClintExtensions.onBridgeReady()
            }
            "unsignedRes" -> unsignedWaiters.remove(msg.optInt("id"))?.invoke(msg.optJSONObject("res"))
            "bundleOk" -> {
                if (msg.optInt("seq") >= synchronized(this) { bundleSeq }) {
                    bundleInFlight = false
                    flushSyncWaiters()
                }
                markReady()
            }
            "cookiesRes" -> cookieWaiters.remove(msg.optInt("id"))?.invoke(msg.optString("cookie"))
            "ckRes" -> cookieOpWaiters.remove(msg.optInt("id"))?.invoke(msg.opt("res")?.takeIf { it !== JSONObject.NULL })
        }
    }

    private fun postToBackground(msg: JSONObject) {
        main.post { bgPort?.postMessage(msg) }
    }

    fun registerView(view: ClintWebView) {
        views[view.session] = WeakReference(view)
        com.jhaiian.clint.extensions.ClintExtensions.onSessionCreated(view.session)
        extension?.let { view.attachExtension(it) }
    }

    fun unregisterView(view: ClintWebView) {
        views.remove(view.session)
        tabByExt.entries.removeAll { it.value.get() === view || it.value.get() == null }
        pendingNonces.entries.removeAll { it.value.get() === view || it.value.get() == null }
    }

    fun setForeground(view: ClintWebView) {
        if (foregroundView?.get() !== view) foregroundView = WeakReference(view)
    }

    fun clearForeground(view: ClintWebView) {
        if (foregroundView?.get() === view) foregroundView = null
    }

    private fun <V : Any> ConcurrentHashMap<String, V>.capAt(limit: Int) {
        var excess = size - limit
        if (excess <= 0) return
        val iter = keys.iterator()
        while (excess > 0 && iter.hasNext()) {
            remove(iter.next())
            excess--
        }
    }

    fun trimMemory(level: Int) {
        val rt = runtimeRef ?: return
        val severe = level >= TRIM_MEMORY_MODERATE_LEVEL || level == RUNNING_CRITICAL ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && level >= TRIM_MEMORY_BACKGROUND_LEVEL)
        if (level >= TRIM_MEMORY_BACKGROUND_LEVEL) runtimeJs = null
        if (!severe) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastTrimAt < TRIM_DEBOUNCE_MS) return
        lastTrimAt = now
        runCatching { rt.storageController.clearData(StorageController.ClearFlags.IMAGE_CACHE) }
    }

    fun viewFor(session: GeckoSession?): ClintWebView? = session?.let { views[it]?.get() }

    fun noteNonce(nonce: String, view: ClintWebView) {
        val tab = pendingTabs.remove(nonce)
        if (tab != null) {
            bindTab(tab.first, tab.second, view)
        } else {
            pendingNonces[nonce] = WeakReference(view)
            pendingNonces.capAt(MAX_PENDING_BINDINGS)
        }
    }

    private fun onTabInfo(nonce: String, tabId: Int, storeId: String) {
        val view = pendingNonces.remove(nonce)?.get()
        if (view != null) {
            bindTab(tabId, storeId, view)
        } else {
            pendingTabs[nonce] = tabId to storeId
            pendingTabs.capAt(MAX_PENDING_BINDINGS)
        }
    }

    private fun bindTab(tabId: Int, storeId: String, view: ClintWebView) {
        if (tabId >= 0) {
            val queued = synchronized(queuedObservations) {
                tabByExt[tabId] = WeakReference(view)
                queuedObservations.remove(tabId)
            }
            queued?.forEach { handleObservation(it) }
        }
        if (storeId.isNotEmpty()) {
            view.cookieStoreId = storeId
            storeByProfile[view.profileId ?: ""] = storeId
        }
    }

    private fun viewForPage(pageUrl: String): ClintWebView? {
        if (pageUrl.isEmpty()) return null
        val matches = synchronized(views) {
            views.values.mapNotNull { ref -> ref.get()?.takeIf { it.url == pageUrl || it.rawUrl == pageUrl } }
        }
        val foreground = foregroundView?.get()
        return matches.firstOrNull { it === foreground } ?: matches.singleOrNull()
    }

    private fun siteOf(url: String?): String? =
        url?.toHttpUrlOrNull()?.let { it.topPrivateDomain() ?: it.host }

    private fun viewForSite(pageUrl: String): ClintWebView? {
        viewForPage(pageUrl)?.let { return it }
        val site = siteOf(pageUrl) ?: return null
        return foregroundView?.get()?.takeIf { siteOf(it.url) == site }
    }

    private fun resolveObservation(msg: JSONObject): ClintWebView? {
        val tabId = msg.optInt("tab", -1)
        if (tabId < 0) return viewForSite(msg.optString("page"))
        synchronized(queuedObservations) {
            tabByExt[tabId]?.get()?.let { return it }
            val queue = queuedObservations[tabId]
            if (queue != null) {
                if (queue.size < MAX_QUEUED_OBSERVATIONS) queue.add(msg)
                return null
            }
            if (queuedObservations.size >= MAX_QUEUED_OBSERVATION_TABS) {
                queuedObservations.remove(queuedObservations.keys.first())
            }
            queuedObservations[tabId] = mutableListOf(msg)
        }
        main.postDelayed({ releaseQueuedObservations(tabId) }, OBSERVATION_BIND_GRACE_MS)
        return null
    }

    private fun releaseQueuedObservations(tabId: Int) {
        val queued = synchronized(queuedObservations) { queuedObservations.remove(tabId) } ?: return
        io.execute {
            queued.forEach { msg ->
                runCatching {
                    val view = tabByExt[tabId]?.get() ?: viewForPage(msg.optString("page"))
                    if (view != null) deliverObservation(view, msg)
                }
            }
        }
    }

    private fun deliverObservation(view: ClintWebView, msg: JSONObject) {
        val client = view.webViewClient ?: return
        val headers = LinkedHashMap<String, String>()
        msg.optJSONObject("h")?.let { h -> h.keys().forEach { k -> headers[k] = h.optString(k) } }
        val request = ClintRequest.fromExtension(
            msg.optString("u"),
            msg.optString("mt", "GET"),
            msg.optString("t", "other"),
            headers,
            msg.optString("ct").takeIf { it.isNotBlank() }
        )
        val page = msg.optString("page").takeIf { it.isNotEmpty() && msg.optInt("tab", -1) >= 0 }
        client.onMediaObserved(request, page)
    }

    private fun handleObservation(msg: JSONObject) {
        io.execute {
            runCatching { resolveObservation(msg)?.let { deliverObservation(it, msg) } }
        }
    }

    fun pushConfig() {
        val cfg = runCatching { configProvider?.invoke() }.getOrNull() ?: return
        postToBackground(JSONObject().put("k", "cfg").put("cfg", cfg))
    }

    fun setScript(id: String, code: String?, topOnly: Boolean = false) {
        val changed = synchronized(scripts) {
            if (code == null) scripts.remove(id) != null
            else {
                val prev = scripts[id]
                scripts[id] = ScriptEntry(code, topOnly)
                prev == null || prev.code != code || prev.topOnly != topOnly
            }
        }
        if (changed) scheduleBundle()
    }

    fun registerInterface(name: String, obj: Any) {
        val names = interfaceMethodNames.getOrPut(obj.javaClass) {
            obj.javaClass.methods.filter { it.isAnnotationPresent(JavascriptInterface::class.java) }.map { it.name }.distinct()
        }
        val list = names.map { it to (kinds["$name.$it"] ?: "v") }
        val changed = specs.put(name, list) != list
        specRefs[name] = WeakReference(obj)
        if (changed) scheduleBundle()
    }

    fun bridgeMethod(type: Class<*>, name: String, arity: Int): Method? {
        val key = BridgeMethodKey(type, name, arity)
        bridgeMethods[key]?.let { return it }
        val method = type.methods.firstOrNull {
            it.name == name && it.parameterTypes.size == arity && it.isAnnotationPresent(JavascriptInterface::class.java)
        } ?: return null
        method.isAccessible = true
        bridgeMethods[key] = method
        return method
    }

    fun syncThen(action: () -> Unit) {
        val pending = synchronized(this) { bundleScheduled } || bundleInFlight
        if (!pending) {
            main.postDelayed({ action() }, 120)
            return
        }
        val fired = java.util.concurrent.atomic.AtomicBoolean(false)
        val run: () -> Unit = { if (fired.compareAndSet(false, true)) action() }
        synchronized(syncWaiters) { syncWaiters.add(run) }
        main.postDelayed({ run() }, 1500)
    }

    private fun flushSyncWaiters() {
        val list = synchronized(syncWaiters) { ArrayList(syncWaiters).also { syncWaiters.clear() } }
        list.forEach { task -> main.post { task() } }
    }

    fun scheduleBundle() {
        synchronized(this) {
            if (bundleScheduled) return
            bundleScheduled = true
        }
        main.postDelayed({
            synchronized(this) { bundleScheduled = false }
            pushBundle()
        }, 150)
    }

    private fun pushBundle() {
        if (bgPort == null) return
        io.execute {
            val code = runCatching { buildBundle() }.getOrNull() ?: return@execute
            val seq = synchronized(this) { ++bundleSeq }
            bundleInFlight = true
            postToBackground(JSONObject().put("k", "bundle").put("seq", seq).put("code", code))
        }
    }

    private fun loadRuntime(): String =
        runtimeJs ?: (
            appContext.assets.open("JavaScript/clint_runtime.js").bufferedReader().use { it.readText() } + "\n" +
                appContext.assets.open("JavaScript/readability.js").bufferedReader().use { it.readText() } + "\n" +
                appContext.assets.open("JavaScript/media_scanner.js").bufferedReader().use { it.readText() }
            ).also { runtimeJs = it }

    private fun buildBundle(): String {
        val cfg = JSONObject()
        val ifaces = JSONArray()
        val snap = JSONObject()
        for ((name, methods) in specs) {
            val spec = JSONObject().put("n", name)
            val arr = JSONArray()
            for ((m, kind) in methods) {
                arr.put(JSONObject().put("n", m).put("k", kind))
                if (kind == "c") {
                    val instance = specRefs[name]?.get()
                    val value = runCatching {
                        instance?.javaClass?.getMethod(m)?.also { it.isAccessible = true }?.invoke(instance)
                    }.getOrNull()
                    snap.put("$name.$m", value ?: true)
                }
            }
            ifaces.put(spec.put("m", arr))
        }
        cfg.put("ifaces", ifaces)
        cfg.put("snap", snap)

        cfg.put("perm", JSONObject())

        val dsx = JSONArray()
        runCatching {
            SitePermissionManager.getAllByType(appContext, SitePermissionDatabase.TYPE_DATA_SAVER_EXCEPTION).forEach { dsx.put(it.first) }
        }
        cfg.put("dsx", dsx)

        val list = JSONArray()
        synchronized(scripts) {
            scripts.forEach { (_, e) -> list.put(JSONObject().put("c", e.code).put("top", e.topOnly)) }
        }
        cfg.put("scripts", list)

        val json = cfg.toString().replace("\u2028", "\\u2028").replace("\u2029", "\\u2029")
        return "(function(){var CFG=$json;\n${loadRuntime()}\n})();"
    }

    fun cookiesAsync(url: String, storeId: String?, callback: (String) -> Unit) {
        val id = cookieSeq.incrementAndGet()
        cookieWaiters[id] = callback
        val msg = JSONObject().put("k", "cookies").put("id", id).put("url", url)
        if (!storeId.isNullOrEmpty()) msg.put("storeId", storeId)
        postToBackground(msg)
        main.postDelayed({ cookieWaiters.remove(id)?.invoke("") }, 3000)
    }

    fun cookieOpAsync(op: String, url: String, storeId: String?, payload: JSONObject?, callback: (Any?) -> Unit) {
        val id = cookieSeq.incrementAndGet()
        cookieOpWaiters[id] = callback
        val msg = JSONObject().put("k", "ck").put("id", id).put("op", op).put("url", url)
        if (!storeId.isNullOrEmpty()) msg.put("storeId", storeId)
        payload?.keys()?.forEach { key -> msg.put(key, payload.get(key)) }
        postToBackground(msg)
        main.postDelayed({ cookieOpWaiters.remove(id)?.invoke(null) }, COOKIE_OP_TIMEOUT_MS)
    }

    fun installUnsignedAsync(path: String, callback: (JSONObject?) -> Unit) {
        main.post {
            val id = cookieSeq.incrementAndGet()
            unsignedWaiters[id] = callback
            val msg = JSONObject().put("k", "unsigned").put("id", id).put("path", path)
            val port = bgPort
            if (port != null) port.postMessage(msg) else unsignedQueue.add(msg)
            main.postDelayed({
                unsignedQueue.remove(msg)
                unsignedWaiters.remove(id)?.invoke(null)
            }, UNSIGNED_INSTALL_TIMEOUT_MS)
        }
    }

    private fun flushUnsignedQueue() {
        val port = bgPort ?: return
        val pending = ArrayList(unsignedQueue)
        unsignedQueue.clear()
        pending.forEach { port.postMessage(it) }
    }

    fun cookieStoreFor(profileId: String?): String? = storeByProfile[profileId ?: ""]

    fun cookiesBlocking(url: String, profileId: String? = null): String {
        if (Looper.myLooper() == Looper.getMainLooper()) return ""
        val latch = CountDownLatch(1)
        var out = ""
        cookiesAsync(url, cookieStoreFor(profileId)) {
            out = it
            latch.countDown()
        }
        latch.await(3500, TimeUnit.MILLISECONDS)
        return out
    }

    fun clearAllData() {
        runtime.storageController.clearData(StorageController.ClearFlags.ALL)
    }

    fun isRuntimeReady(): Boolean = runtimeRef != null

    fun clearData(flags: Long, onDone: () -> Unit) {
        val started = runCatching {
            runtime.storageController.clearData(flags).accept({ onDone() }, { onDone() })
        }
        if (started.isFailure) onDone()
    }

    fun clearDataFromBaseDomain(baseDomain: String, flags: Long, onDone: () -> Unit) {
        val started = runCatching {
            runtime.storageController.clearDataFromBaseDomain(baseDomain, flags).accept({ onDone() }, { onDone() })
        }
        if (started.isFailure) onDone()
    }

    fun clearCache() {
        runtime.storageController.clearData(StorageController.ClearFlags.ALL_CACHES)
    }

    fun clearContextData(contextId: String) {
        runtime.storageController.clearDataForSessionContext(contextId)
    }

    fun setCookiePolicy(policy: String) {
        val behavior = when (policy) {
            com.jhaiian.clint.privacy.PrivacyPrefs.COOKIE_POLICY_ALLOW_ALL -> ContentBlocking.CookieBehavior.ACCEPT_ALL
            com.jhaiian.clint.privacy.PrivacyPrefs.COOKIE_POLICY_BLOCK_THIRD_PARTY -> ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY
            else -> ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS
        }
        val privateBehavior = if (behavior == ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY) {
            ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY
        } else {
            ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS
        }
        runCatching {
            runtime.settings.contentBlocking
                .setCookieBehavior(behavior)
                .setCookieBehaviorPrivateMode(privateBehavior)
        }
    }

    @Volatile
    var trackingProtectionLevel: String = com.jhaiian.clint.privacy.PrivacyPrefs.DEFAULT_ETP_LEVEL
        private set

    val isTrackingProtectionEnabled: Boolean
        get() = trackingProtectionLevel != com.jhaiian.clint.privacy.PrivacyPrefs.ETP_LEVEL_OFF

    fun blocksTrackingContent(isPrivate: Boolean): Boolean = when (trackingProtectionLevel) {
        com.jhaiian.clint.privacy.PrivacyPrefs.ETP_LEVEL_STRICT -> true
        com.jhaiian.clint.privacy.PrivacyPrefs.ETP_LEVEL_STANDARD -> isPrivate
        else -> false
    }

    fun applyTrackingProtection(context: Context) {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val level = com.jhaiian.clint.privacy.PrivacyPrefs.etpLevel(prefs)
        val fingerprinting = prefs.getBoolean(
            com.jhaiian.clint.privacy.PrivacyPrefs.FINGERPRINTING_PROTECTION,
            com.jhaiian.clint.privacy.PrivacyPrefs.DEFAULT_FINGERPRINTING_PROTECTION
        )
        trackingProtectionLevel = level
        val categories = when (level) {
            com.jhaiian.clint.privacy.PrivacyPrefs.ETP_LEVEL_STRICT -> ContentBlocking.AntiTracking.STRICT
            com.jhaiian.clint.privacy.PrivacyPrefs.ETP_LEVEL_STANDARD -> ContentBlocking.AntiTracking.DEFAULT or ContentBlocking.AntiTracking.CRYPTOMINING
            else -> ContentBlocking.AntiTracking.NONE
        }
        val antiTracking = if (fingerprinting) {
            categories or ContentBlocking.AntiTracking.FINGERPRINTING
        } else {
            categories and ContentBlocking.AntiTracking.FINGERPRINTING.inv()
        }
        val etpLevel = when (level) {
            com.jhaiian.clint.privacy.PrivacyPrefs.ETP_LEVEL_STRICT -> ContentBlocking.EtpLevel.STRICT
            com.jhaiian.clint.privacy.PrivacyPrefs.ETP_LEVEL_STANDARD -> ContentBlocking.EtpLevel.DEFAULT
            else -> ContentBlocking.EtpLevel.NONE
        }
        val category = when (level) {
            com.jhaiian.clint.privacy.PrivacyPrefs.ETP_LEVEL_STRICT -> ContentBlocking.EtpCategory.STRICT
            com.jhaiian.clint.privacy.PrivacyPrefs.ETP_LEVEL_STANDARD -> ContentBlocking.EtpCategory.STANDARD
            else -> ContentBlocking.EtpCategory.CUSTOM
        }
        runCatching {
            runtime.settings.contentBlocking
                .setEnhancedTrackingProtectionLevel(etpLevel)
                .setEnhancedTrackingProtectionCategory(category)
                .setAntiTracking(antiTracking)
                .setStrictSocialTrackingProtection(level == com.jhaiian.clint.privacy.PrivacyPrefs.ETP_LEVEL_STRICT)
                .setEmailTrackerBlockingPrivateBrowsing(level != com.jhaiian.clint.privacy.PrivacyPrefs.ETP_LEVEL_OFF)
        }
    }

    fun applyFontSize(context: Context) {
        val rt = runtimeRef ?: return
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val auto = com.jhaiian.clint.settings.accessibility.AccessibilityPrefs.autoFontSize(prefs)
        val percent = com.jhaiian.clint.settings.accessibility.AccessibilityPrefs.fontSizePercent(prefs)
        runCatching {
            rt.settings.setAutomaticFontSizeAdjustment(auto)
            if (!auto) rt.settings.setFontSizeFactor(percent / 100f)
        }
    }

    fun applyForceZoom(context: Context) {
        val rt = runtimeRef ?: return
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val enabled = com.jhaiian.clint.settings.accessibility.AccessibilityPrefs.forceZoom(prefs)
        runCatching { rt.settings.setForceUserScalableEnabled(enabled) }
    }

    fun setDarkMode(mode: Int) {
        runtime.settings.preferredColorScheme = when (mode) {
            1 -> GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
            2 -> GeckoRuntimeSettings.COLOR_SCHEME_DARK
            else -> GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM
        }
    }
}
