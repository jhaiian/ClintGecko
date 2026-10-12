package com.jhaiian.clint.browser.webview

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import com.jhaiian.clint.settings.sitepermissions.SitePermissionDatabase
import com.jhaiian.clint.settings.sitepermissions.SitePermissionManager
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

    private val views = Collections.synchronizedMap(WeakHashMap<GeckoSession, ClintWebView>())
    private val pendingNonces = ConcurrentHashMap<String, WeakReference<ClintWebView>>()
    private val pendingTabs = ConcurrentHashMap<String, Pair<Int, String>>()
    private val tabByExt = ConcurrentHashMap<Int, WeakReference<ClintWebView>>()
    private val storeByProfile = ConcurrentHashMap<String, String>()
    private val cookieWaiters = ConcurrentHashMap<Int, (String) -> Unit>()
    private val cookieSeq = AtomicInteger()

    @Volatile var foregroundView: WeakReference<ClintWebView>? = null

    @Volatile var configProvider: (() -> JSONObject)? = null

    private class ScriptEntry(val code: String, val topOnly: Boolean)

    private val scripts = LinkedHashMap<String, ScriptEntry>()
    private val specs = ConcurrentHashMap<String, List<Pair<String, String>>>()
    private val specRefs = ConcurrentHashMap<String, WeakReference<Any>>()
    private var bundleSeq = 0
    private var bundleScheduled = false
    private var runtimeJs: String? = null
    @Volatile private var bundleInFlight = false
    private val syncWaiters = ArrayList<() -> Unit>()

    private val kinds = mapOf(
        "ClintNotificationBridge.getPermissionState" to "p",
        "ClintClipboardBridge.getPermissionState" to "p",
        "ClintClipboardBridge.readClipboardText" to "a",
        "ClintDataSaverBridge.isExcepted" to "x"
    )

    val geckoVersion: String by lazy {
        runCatching {
            Class.forName("org.mozilla.geckoview.BuildConfig").getField("MOZ_APP_VERSION").get(null) as String
        }.getOrDefault("159.0")
    }

    fun defaultUserAgent(): String {
        val major = geckoVersion.substringBefore('.')
        return "Mozilla/5.0 (Android ${Build.VERSION.RELEASE}; Mobile; rv:$major.0) Gecko/$major.0 Firefox/$major.0"
    }

    fun init(context: Context) {
        if (runtimeRef != null) return
        appContext = context.applicationContext
        val settings = GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(true)
            .aboutConfigEnabled(false)
            .remoteDebuggingEnabled(false)
            .extensionsWebAPIEnabled(true)
            .consoleOutput((appContext.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0)
            .build()
        runCatching { com.jhaiian.clint.backup.ExtensionBackup.applyPending(appContext) }
        runCatching { com.jhaiian.clint.backup.CookieBackup.applyPending(appContext) }
        runtimeRef = GeckoRuntime.create(appContext, settings)
        disableTelemetry()
        com.jhaiian.clint.extensions.ClintExtensions.init(appContext, runtime)
        installExtension()
        main.postDelayed({ markReady() }, READY_TIMEOUT_MS)
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
                    synchronized(views) { views.values.toList() }.forEach { it.attachExtension(ext) }
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
            }
            "bundleOk" -> {
                if (msg.optInt("seq") >= synchronized(this) { bundleSeq }) {
                    bundleInFlight = false
                    flushSyncWaiters()
                }
                markReady()
            }
            "cookiesRes" -> cookieWaiters.remove(msg.optInt("id"))?.invoke(msg.optString("cookie"))
        }
    }

    private fun postToBackground(msg: JSONObject) {
        main.post { bgPort?.postMessage(msg) }
    }

    fun registerView(view: ClintWebView) {
        views[view.session] = view
        com.jhaiian.clint.extensions.ClintExtensions.onSessionCreated(view.session)
        extension?.let { view.attachExtension(it) }
    }

    fun unregisterView(view: ClintWebView) {
        views.remove(view.session)
        tabByExt.entries.removeAll { it.value.get() === view || it.value.get() == null }
        pendingNonces.entries.removeAll { it.value.get() === view || it.value.get() == null }
    }

    fun viewFor(session: GeckoSession?): ClintWebView? = session?.let { views[it] }

    fun noteNonce(nonce: String, view: ClintWebView) {
        val tab = pendingTabs.remove(nonce)
        if (tab != null) {
            bindTab(tab.first, tab.second, view)
        } else {
            pendingNonces[nonce] = WeakReference(view)
        }
    }

    private fun onTabInfo(nonce: String, tabId: Int, storeId: String) {
        val view = pendingNonces.remove(nonce)?.get()
        if (view != null) bindTab(tabId, storeId, view) else pendingTabs[nonce] = tabId to storeId
    }

    private fun bindTab(tabId: Int, storeId: String, view: ClintWebView) {
        if (tabId >= 0) tabByExt[tabId] = WeakReference(view)
        if (storeId.isNotEmpty()) {
            view.cookieStoreId = storeId
            storeByProfile[view.profileId ?: ""] = storeId
        }
    }

    private fun resolveView(tabId: Int, pageUrl: String): ClintWebView? {
        tabByExt[tabId]?.get()?.let { return it }
        if (pageUrl.isNotEmpty()) {
            val match = synchronized(views) { views.values.firstOrNull { it.url == pageUrl || it.rawUrl == pageUrl } }
            if (match != null) return match
        }
        return foregroundView?.get()
    }

    private fun handleObservation(msg: JSONObject) {
        io.execute {
            runCatching {
                val page = msg.optString("page")
                val view = resolveView(msg.optInt("tab", -1), page)
                val client = view?.webViewClient ?: return@execute
                val headers = LinkedHashMap<String, String>()
                msg.optJSONObject("h")?.let { h -> h.keys().forEach { k -> headers[k] = h.optString(k) } }
                val request = ClintRequest.fromExtension(msg.optString("u"), msg.optString("mt", "GET"), msg.optString("t", "other"), headers)
                client.onMediaObserved(request, page.ifEmpty { null })
            }
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
        val methods = obj.javaClass.methods.filter { it.isAnnotationPresent(JavascriptInterface::class.java) }
        val list = methods.map { it.name to (kinds["$name.${it.name}"] ?: "v") }.distinctBy { it.first }
        val changed = specs.put(name, list) != list
        specRefs[name] = WeakReference(obj)
        if (changed) scheduleBundle()
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
                appContext.assets.open("JavaScript/readability.js").bufferedReader().use { it.readText() }
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

    fun clearCache() {
        runtime.storageController.clearData(StorageController.ClearFlags.ALL_CACHES)
    }

    fun clearContextData(contextId: String) {
        runtime.storageController.clearDataForSessionContext(contextId)
    }

    fun setBlockThirdPartyCookies(block: Boolean) {
        runCatching {
            runtime.settings.contentBlocking.setCookieBehavior(
                if (block) ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY else ContentBlocking.CookieBehavior.ACCEPT_ALL
            )
        }
    }

    fun setDarkMode(mode: Int) {
        runtime.settings.preferredColorScheme = when (mode) {
            1 -> GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
            2 -> GeckoRuntimeSettings.COLOR_SCHEME_DARK
            else -> GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM
        }
    }
}
