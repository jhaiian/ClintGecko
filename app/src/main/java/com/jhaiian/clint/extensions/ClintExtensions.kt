package com.jhaiian.clint.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.jhaiian.clint.R
import com.jhaiian.clint.app.ClintApplication
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.dialogs.JsDialogRequest
import com.jhaiian.clint.browser.dialogs.JsDialogType
import com.jhaiian.clint.browser.webview.ClintGecko
import com.jhaiian.clint.browser.webview.UploadCache
import com.jhaiian.clint.downloads.ResponseDownloads
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.Image
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtension.InstallException.ErrorCodes
import org.mozilla.geckoview.WebExtensionController
import org.mozilla.geckoview.WebResponse
import java.util.WeakHashMap

object ClintExtensions {

    const val BUILT_IN_ID = "clint@jhaiian.com"
    const val STORE_URL = "https://addons.mozilla.org/android/"
    private const val UNSIGNED_PREFS = "clint_unsigned_extensions"
    private const val MAX_UNSIGNED_BYTES = 256L * 1024L * 1024L

    private lateinit var appContext: Context
    private var controller: WebExtensionController? = null
    private val main = Handler(Looper.getMainLooper())

    val extensions = mutableStateListOf<ExtensionInfo>()
    val icons = mutableStateMapOf<String, ImageBitmap>()
    val actionIcons = mutableStateMapOf<String, ImageBitmap>()

    var loaded by mutableStateOf(false)
        private set
    var checkingAll by mutableStateOf(false)
        private set
    var busyIds by mutableStateOf<Set<String>>(emptySet())
        private set
    var prompt by mutableStateOf<ExtensionPrompt?>(null)
        private set
    var popup by mutableStateOf<ExtensionPopup?>(null)
        private set
    var popupFileRequest by mutableStateOf<ExtensionFileRequest?>(null)
        private set
    var popupDialog by mutableStateOf<JsDialogRequest?>(null)
        private set
    var unsignedRequest by mutableStateOf<UnsignedInstallRequest?>(null)
        private set
    private var actionVersion by mutableIntStateOf(0)
    private var installsInFlight = 0
    private var unsignedPromptAt = 0L
    private var unsignedRestored = false

    @Volatile var tabOpener: ((String, Boolean) -> GeckoSession?)? = null
    @Volatile var tabOpenerOwner: Any? = null

    private val promptQueue = ArrayDeque<ExtensionPrompt>()
    private val hosts = ArrayList<Any>()
    var activeHost by mutableStateOf<Any?>(null)
        private set

    private val defaultActions = HashMap<String, WebExtension.Action>()
    private val sessionActions = WeakHashMap<GeckoSession, HashMap<String, WebExtension.Action>>()
    private val sessions = WeakHashMap<GeckoSession, Boolean>()
    private var activeSession: GeckoSession? = null
    private var refreshPosted = false
    private val recentToasts = HashMap<String, Long>()

    fun init(context: Context, runtime: GeckoRuntime) {
        if (controller != null) return
        appContext = context.applicationContext
        val c = runtime.webExtensionController
        controller = c
        c.setPromptDelegate(promptDelegate)
        c.setAddonManagerDelegate(addonDelegate)
        refresh()
    }

    fun liveUuids(): Map<String, String> {
        val out = HashMap<String, String>()
        for (info in extensions.toList()) {
            val meta = runCatching { info.raw.metaData }.getOrNull() ?: continue
            val base = runCatching { meta.javaClass.getField("baseUrl").get(meta) as? String }.getOrNull() ?: continue
            val uuid = base.removePrefix("moz-extension://").substringBefore('/')
            if (uuid.isNotBlank() && !uuid.contains(':')) out[info.id] = uuid
        }
        return out
    }

    fun registerHost(token: Any) {
        hosts.remove(token)
        hosts.add(token)
        activeHost = token
    }

    fun unregisterHost(token: Any) {
        hosts.remove(token)
        activeHost = hosts.lastOrNull()
    }

    private fun isBuiltIn(ext: WebExtension) = ext.isBuiltIn || ext.id == BUILT_IN_ID

    fun isXpiUrl(url: String?): Boolean {
        if (url == null) return false
        if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) return false
        return url.substringBefore('#').substringBefore('?').endsWith(".xpi", true)
    }

    fun isXpiMime(mime: String?, disposition: String?, url: String?): Boolean {
        if (mime != null && mime.equals("application/x-xpinstall", true)) return true
        if (disposition != null && Regex("filename\\*?=[^;]*\\.xpi", RegexOption.IGNORE_CASE).containsMatchIn(disposition.replace("\"", ""))) return true
        return isXpiUrl(url)
    }

    fun installFromWeb(url: String): Boolean {
        if (controller == null) return false
        install(url, WebExtensionController.INSTALLATION_METHOD_MANAGER)
        return true
    }

    fun refresh() {
        if (controller == null || refreshPosted) return
        refreshPosted = true
        main.postDelayed({
            refreshPosted = false
            val c = controller ?: return@postDelayed
            c.list().accept(
                { list -> applyList(list.orEmpty()) },
                { loaded = true }
            )
        }, 60)
    }

    private fun applyList(list: List<WebExtension>) {
        val visible = list.filter { !isBuiltIn(it) }
        visible.forEach { register(it) }
        val infos = visible.mapNotNull { runCatching { ExtensionInfo.from(it) }.getOrNull() }
            .sortedBy { it.name.lowercase() }
        extensions.clear()
        extensions.addAll(infos)
        val ids = infos.map { it.id }.toSet()
        icons.keys.retainAll(ids)
        actionIcons.keys.retainAll(ids)
        for (info in infos) {
            if (icons[info.id] == null) {
                loadIcon(runCatching { info.raw.metaData.icon }.getOrNull(), 144) { icons[info.id] = it }
            }
        }
        loaded = true
    }

    private fun loadIcon(image: Image?, size: Int, onDone: (ImageBitmap) -> Unit) {
        if (image == null) return
        runCatching {
            image.getBitmap(size).accept(
                { bitmap -> if (bitmap != null) onDone(bitmap.asImageBitmap()) },
                { }
            )
        }
    }

    private fun register(ext: WebExtension) {
        if (isBuiltIn(ext)) return
        ext.setActionDelegate(actionDelegate)
        ext.setTabDelegate(tabDelegate)
        val known = synchronized(sessions) { sessions.keys.toList() }
        known.forEach { runCatching { it.webExtensionController.setActionDelegate(ext, sessionActionDelegate) } }
    }

    fun onSessionCreated(session: GeckoSession) {
        synchronized(sessions) { sessions[session] = true }
        if (controller == null) return
        extensions.forEach { info ->
            runCatching { session.webExtensionController.setActionDelegate(info.raw, sessionActionDelegate) }
        }
    }

    fun onSessionReopened(session: GeckoSession) {
        sessionActions.remove(session)
        onSessionCreated(session)
        if (activeSession === session) {
            val c = controller
            if (c != null) {
                runCatching { c.setTabActive(session, false) }
                runCatching { c.setTabActive(session, true) }
            }
        }
        actionVersion++
    }

    fun reassertActiveSession() {
        val c = controller ?: return
        val session = activeSession ?: return
        if (!session.isOpen) return
        runCatching { c.setTabActive(session, true) }
        actionVersion++
    }

    fun setActiveSession(session: GeckoSession?) {
        val c = controller ?: return
        val previous = activeSession
        if (previous === session) return
        activeSession = session
        previous?.let { runCatching { c.setTabActive(it, false) } }
        session?.let { runCatching { c.setTabActive(it, true) } }
        actionVersion++
    }

    fun actionFor(id: String): WebExtension.Action? {
        val version = actionVersion
        if (version < 0) return null
        val default = defaultActions[id]
        val perSession = activeSession?.let { sessionActions[it]?.get(id) }
        return when {
            default != null && perSession != null -> runCatching { perSession.withDefault(default) }.getOrDefault(perSession)
            else -> perSession ?: default
        }
    }

    fun hasAction(id: String): Boolean = actionFor(id) != null

    fun clickAction(id: String) {
        val action = actionFor(id) ?: return
        if (action.enabled == false) return
        runCatching { action.click() }
    }

    private fun toast(res: Int, vararg args: Any) {
        Toast.makeText(appContext, appContext.getString(res, *args), Toast.LENGTH_SHORT).show()
    }

    private fun toastOnce(key: String, res: Int, vararg args: Any) {
        val now = System.currentTimeMillis()
        val last = recentToasts[key]
        if (last != null && now - last < 2500) return
        recentToasts[key] = now
        toast(res, *args)
    }

    private fun setBusy(id: String, busy: Boolean) {
        busyIds = if (busy) busyIds + id else busyIds - id
    }

    fun install(uri: String, method: String? = WebExtensionController.INSTALLATION_METHOD_MANAGER) {
        val c = controller ?: return
        toastOnce("installing", R.string.ext_toast_installing)
        installsInFlight++
        c.install(uri, method).accept(
            { ext ->
                installsInFlight = (installsInFlight - 1).coerceAtLeast(0)
                if (ext != null) {
                    register(ext)
                    toastOnce("installed:${ext.id}", R.string.ext_toast_installed, ext.metaData.name ?: ext.id)
                }
                refresh()
            },
            { error ->
                installsInFlight = (installsInFlight - 1).coerceAtLeast(0)
                refresh()
                if ((error as? WebExtension.InstallException)?.code == ErrorCodes.ERROR_SIGNEDSTATE_REQUIRED) {
                    offerUnsigned(uri)
                } else {
                    reportFailure(error, null)
                }
            }
        )
    }

    private fun offerUnsigned(uri: String) {
        unsignedPromptAt = System.currentTimeMillis()
        if (unsignedRequest == null) unsignedRequest = UnsignedInstallRequest(uri)
    }

    fun answerUnsigned(request: UnsignedInstallRequest, allow: Boolean) {
        if (unsignedRequest === request) unsignedRequest = null
        if (!allow) return
        toastOnce("installing", R.string.ext_toast_installing)
        val appCtx = appContext
        Thread {
            val staged = runCatching { stageUnsigned(appCtx, request.uri) }.getOrNull()
            main.post {
                if (staged == null) {
                    toast(R.string.ext_unsigned_failed)
                } else {
                    ClintGecko.installUnsignedAsync(staged.absolutePath) { res -> onUnsignedInstalled(staged, res) }
                }
            }
        }.start()
    }

    private fun unsignedDir(context: Context): java.io.File =
        java.io.File(context.filesDir, "unsigned-extensions").apply { mkdirs() }

    private fun unsignedPrefs(context: Context) =
        context.getSharedPreferences(UNSIGNED_PREFS, Context.MODE_PRIVATE)

    private fun stageUnsigned(context: Context, uri: String): java.io.File? {
        val target = java.io.File(unsignedDir(context), "${java.util.UUID.randomUUID()}.xpi")
        val parsed = Uri.parse(uri)
        val copied = runCatching {
            when (parsed.scheme?.lowercase()) {
                "file" -> java.io.File(parsed.path ?: return null).inputStream().use { input ->
                    target.outputStream().use { out -> input.copyTo(out) }
                }
                "content" -> context.contentResolver.openInputStream(parsed)!!.use { input ->
                    target.outputStream().use { out -> input.copyTo(out) }
                }
                "http", "https" -> {
                    val request = okhttp3.Request.Builder().url(uri).build()
                    com.jhaiian.clint.downloads.ClintDownloadManager.httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@runCatching false
                        val body = response.body
                        val length = body.contentLength()
                        if (length > MAX_UNSIGNED_BYTES) return@runCatching false
                        body.byteStream().use { input ->
                            target.outputStream().use { out ->
                                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                var total = 0L
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    total += read
                                    if (total > MAX_UNSIGNED_BYTES) return@runCatching false
                                    out.write(buffer, 0, read)
                                }
                            }
                        }
                    }
                }
                else -> return@runCatching false
            }
            true
        }.getOrDefault(false)
        val valid = copied && runCatching {
            java.util.zip.ZipFile(target).use { it.getEntry("manifest.json") != null }
        }.getOrDefault(false)
        if (!valid) {
            target.delete()
            return null
        }
        return target
    }

    private fun installedId(res: org.json.JSONObject?): String? =
        res?.takeIf { !it.has("error") }?.optString("id")?.takeIf { it.isNotBlank() }

    private fun rememberUnsigned(id: String, file: java.io.File) {
        val prefs = unsignedPrefs(appContext)
        val editor = prefs.edit()
        prefs.all.forEach { (key, value) ->
            if (key == id || value == file.name) {
                if (value != file.name) java.io.File(unsignedDir(appContext), value.toString()).delete()
                editor.remove(key)
            }
        }
        editor.putString(id, file.name).apply()
    }

    private fun onUnsignedInstalled(staged: java.io.File, res: org.json.JSONObject?) {
        val id = installedId(res)
        if (id == null) {
            staged.delete()
            toast(R.string.ext_unsigned_failed)
            refresh()
            return
        }
        rememberUnsigned(id, staged)
        toastOnce("installed:$id", R.string.ext_toast_installed, res?.optString("name")?.ifBlank { id } ?: id)
        refresh()
    }

    fun onBridgeReady() {
        if (unsignedRestored || !::appContext.isInitialized) return
        unsignedRestored = true
        val dir = unsignedDir(appContext)
        val entries = unsignedPrefs(appContext).all.mapNotNull { (id, value) -> (value as? String)?.let { id to it } }
        val known = entries.map { it.second }.toSet()
        dir.listFiles().orEmpty().filter { it.isFile && it.name !in known }.forEach { it.delete() }
        entries.forEach { (id, name) ->
            val file = java.io.File(dir, name)
            if (!file.isFile) {
                unsignedPrefs(appContext).edit().remove(id).apply()
                return@forEach
            }
            ClintGecko.installUnsignedAsync(file.absolutePath) { res ->
                val newId = installedId(res)
                if (newId != null && newId != id) rememberUnsigned(newId, file)
                refresh()
            }
        }
    }

    private fun forgetUnsigned(id: String) {
        if (!::appContext.isInitialized) return
        val prefs = unsignedPrefs(appContext)
        val name = prefs.getString(id, null) ?: return
        runCatching { java.io.File(unsignedDir(appContext), name).delete() }
        prefs.edit().remove(id).apply()
    }

    fun installFromFile(context: Context, source: Uri) {
        val appCtx = context.applicationContext
        Thread {
            val target = runCatching {
                val dir = java.io.File(appCtx.cacheDir, "extensions").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() }
                val file = java.io.File(dir, "install-${System.currentTimeMillis()}.xpi")
                appCtx.contentResolver.openInputStream(source)!!.use { input ->
                    file.outputStream().use { out -> input.copyTo(out) }
                }
                file
            }.getOrNull()
            main.post {
                if (target == null) {
                    toast(R.string.ext_toast_file_failed)
                } else {
                    install(Uri.fromFile(target).toString(), WebExtensionController.INSTALLATION_METHOD_FROM_FILE)
                }
            }
        }.start()
    }

    fun uninstall(info: ExtensionInfo) {
        val c = controller ?: return
        setBusy(info.id, true)
        c.uninstall(info.raw).accept(
            {
                setBusy(info.id, false)
                defaultActions.remove(info.id)
                forgetUnsigned(info.id)
                toastOnce("removed:${info.id}", R.string.ext_toast_removed, info.name)
                refresh()
            },
            {
                setBusy(info.id, false)
                refresh()
            }
        )
    }

    fun setEnabled(info: ExtensionInfo, enabled: Boolean) {
        val c = controller ?: return
        setBusy(info.id, true)
        val result = if (enabled) {
            c.enable(info.raw, WebExtensionController.EnableSource.USER)
        } else {
            c.disable(info.raw, WebExtensionController.EnableSource.USER)
        }
        result.accept(
            { ext ->
                ext?.let { register(it) }
                setBusy(info.id, false)
                refresh()
            },
            {
                setBusy(info.id, false)
                refresh()
            }
        )
    }

    fun setAllowedInPrivate(info: ExtensionInfo, allowed: Boolean) {
        val c = controller ?: return
        setBusy(info.id, true)
        c.setAllowedInPrivateBrowsing(info.raw, allowed).accept(
            { ext ->
                ext?.let { register(it) }
                setBusy(info.id, false)
                refresh()
            },
            {
                setBusy(info.id, false)
                refresh()
            }
        )
    }

    fun setOptionalAccess(info: ExtensionInfo, permission: String?, origin: String?, granted: Boolean) {
        val c = controller ?: return
        val perms = if (permission != null) arrayOf(permission) else emptyArray<String>()
        val origins = if (origin != null) arrayOf(origin) else emptyArray<String>()
        setBusy(info.id, true)
        val result = if (granted) {
            c.addOptionalPermissions(info.id, perms, origins, emptyArray<String>())
        } else {
            c.removeOptionalPermissions(info.id, perms, origins, emptyArray<String>())
        }
        result.accept(
            { ext ->
                ext?.let { register(it) }
                setBusy(info.id, false)
                refresh()
            },
            {
                setBusy(info.id, false)
                refresh()
            }
        )
    }

    fun checkUpdate(info: ExtensionInfo, onDone: (() -> Unit)? = null, silent: Boolean = false) {
        val c = controller ?: return
        setBusy(info.id, true)
        c.update(info.raw).accept(
            { ext ->
                setBusy(info.id, false)
                if (ext != null) register(ext)
                val newVersion = ext?.metaData?.version
                if (!silent) {
                    if (newVersion != null && newVersion != info.version) {
                        toast(R.string.ext_toast_updated, info.name, newVersion)
                    } else {
                        toast(R.string.ext_toast_up_to_date, info.name)
                    }
                }
                refresh()
                onDone?.invoke()
            },
            { error ->
                setBusy(info.id, false)
                if (!silent) {
                    val code = (error as? WebExtension.InstallException)?.code
                    if (code == ErrorCodes.ERROR_USER_CANCELED ||
                        code == ErrorCodes.ERROR_POSTPONED
                    ) {
                        toast(R.string.ext_toast_up_to_date, info.name)
                    } else {
                        toast(R.string.ext_toast_update_failed)
                    }
                }
                refresh()
                onDone?.invoke()
            }
        )
    }

    fun checkAllUpdates() {
        if (checkingAll) return
        val targets = extensions.filter { !it.temporary }
        if (targets.isEmpty()) {
            toast(R.string.ext_toast_no_extensions)
            return
        }
        checkingAll = true
        var updated = 0
        fun next(index: Int) {
            if (index >= targets.size) {
                checkingAll = false
                toast(R.string.ext_toast_all_checked, targets.size, updated)
                return
            }
            val info = targets[index]
            val c = controller
            if (c == null) {
                checkingAll = false
                return
            }
            setBusy(info.id, true)
            c.update(info.raw).accept(
                { ext ->
                    setBusy(info.id, false)
                    if (ext != null) {
                        register(ext)
                        if (ext.metaData.version != info.version) updated++
                    }
                    next(index + 1)
                },
                {
                    setBusy(info.id, false)
                    next(index + 1)
                }
            )
        }
        next(0)
        refresh()
    }

    private fun reportFailure(error: Throwable?, name: String?) {
        val ex = error as? WebExtension.InstallException
        val code = ex?.code
        if (code == ErrorCodes.ERROR_USER_CANCELED || code == ErrorCodes.ERROR_POSTPONED) return
        if (code == ErrorCodes.ERROR_SIGNEDSTATE_REQUIRED &&
            (installsInFlight > 0 || unsignedRequest != null || System.currentTimeMillis() - unsignedPromptAt < 5000)
        ) return
        val res = when (code) {
            ErrorCodes.ERROR_NETWORK_FAILURE -> R.string.ext_err_network
            ErrorCodes.ERROR_CORRUPT_FILE, ErrorCodes.ERROR_INCORRECT_HASH -> R.string.ext_err_corrupt
            ErrorCodes.ERROR_FILE_ACCESS -> R.string.ext_err_file
            ErrorCodes.ERROR_SIGNEDSTATE_REQUIRED -> R.string.ext_err_unsigned
            ErrorCodes.ERROR_UNEXPECTED_ADDON_TYPE, ErrorCodes.ERROR_UNSUPPORTED_ADDON_TYPE -> R.string.ext_err_unsupported
            ErrorCodes.ERROR_BLOCKLISTED, ErrorCodes.ERROR_SOFT_BLOCKED -> R.string.ext_err_blocked
            ErrorCodes.ERROR_INCOMPATIBLE, ErrorCodes.ERROR_UNEXPECTED_ADDON_VERSION -> R.string.ext_err_incompatible
            ErrorCodes.ERROR_INVALID_DOMAIN -> R.string.ext_err_insecure
            else -> R.string.ext_err_generic
        }
        toastOnce("fail:$res", res)
    }

    private fun enqueue(p: ExtensionPrompt) {
        if (prompt == null) prompt = p else promptQueue.addLast(p)
    }

    private fun advancePrompt() {
        prompt = promptQueue.removeFirstOrNull()
    }

    fun answerInstall(p: ExtensionPrompt.Install, allow: Boolean, allowPrivate: Boolean) {
        p.result.complete(WebExtension.PermissionPromptResponse(allow, allow && allowPrivate, allow))
        advancePrompt()
    }

    fun answer(p: ExtensionPrompt, allow: Boolean) {
        val decision = if (allow) AllowOrDeny.ALLOW else AllowOrDeny.DENY
        when (p) {
            is ExtensionPrompt.Update -> p.result.complete(decision)
            is ExtensionPrompt.Optional -> p.result.complete(decision)
            is ExtensionPrompt.Install -> p.result.complete(WebExtension.PermissionPromptResponse(allow, false, allow))
        }
        advancePrompt()
    }

    private val promptDelegate = object : WebExtensionController.PromptDelegate {
        override fun onInstallPromptRequest(
            extension: WebExtension,
            permissions: Array<String>,
            origins: Array<String>,
            dataCollectionPermissions: Array<String>
        ): GeckoResult<WebExtension.PermissionPromptResponse>? {
            val result = GeckoResult<WebExtension.PermissionPromptResponse>()
            enqueue(ExtensionPrompt.Install(extension, permissions.toList(), origins.toList(), dataCollectionPermissions.toList(), result))
            return result
        }

        override fun onUpdatePrompt(
            extension: WebExtension,
            newPermissions: Array<String>,
            newOrigins: Array<String>,
            newDataCollectionPermissions: Array<String>
        ): GeckoResult<AllowOrDeny>? {
            val result = GeckoResult<AllowOrDeny>()
            enqueue(ExtensionPrompt.Update(extension, newPermissions.toList(), newOrigins.toList(), newDataCollectionPermissions.toList(), result))
            return result
        }

        override fun onOptionalPrompt(
            extension: WebExtension,
            permissions: Array<String>,
            origins: Array<String>,
            dataCollectionPermissions: Array<String>
        ): GeckoResult<AllowOrDeny>? {
            val result = GeckoResult<AllowOrDeny>()
            enqueue(ExtensionPrompt.Optional(extension, permissions.toList(), origins.toList(), dataCollectionPermissions.toList(), result))
            return result
        }
    }

    private val addonDelegate = object : WebExtensionController.AddonManagerDelegate {
        override fun onInstalled(extension: WebExtension) {
            if (isBuiltIn(extension)) return
            register(extension)
            toastOnce("installed:${extension.id}", R.string.ext_toast_installed, extension.metaData.name ?: extension.id)
            refresh()
        }

        override fun onUninstalled(extension: WebExtension) {
            if (isBuiltIn(extension)) return
            defaultActions.remove(extension.id)
            refresh()
        }

        override fun onEnabled(extension: WebExtension) {
            if (!isBuiltIn(extension)) refresh()
        }

        override fun onDisabled(extension: WebExtension) {
            if (!isBuiltIn(extension)) refresh()
        }

        override fun onReady(extension: WebExtension) {
            if (!isBuiltIn(extension)) refresh()
        }

        override fun onOptionalPermissionsChanged(extension: WebExtension) {
            if (!isBuiltIn(extension)) refresh()
        }

        override fun onInstallationFailed(extension: WebExtension?, installException: WebExtension.InstallException) {
            reportFailure(installException, extension?.metaData?.name)
        }
    }

    private fun storeAction(extension: WebExtension, session: GeckoSession?, action: WebExtension.Action) {
        if (session == null) {
            defaultActions[extension.id] = action
            loadIcon(action.icon, 96) { actionIcons[extension.id] = it }
        } else {
            sessionActions.getOrPut(session) { HashMap() }[extension.id] = action
        }
        actionVersion++
    }

    private val actionDelegate = object : WebExtension.ActionDelegate {
        override fun onBrowserAction(extension: WebExtension, session: GeckoSession?, action: WebExtension.Action) {
            storeAction(extension, session, action)
        }

        override fun onTogglePopup(extension: WebExtension, action: WebExtension.Action): GeckoResult<GeckoSession>? =
            openSession(extension, false)

        override fun onOpenPopup(extension: WebExtension, action: WebExtension.Action): GeckoResult<GeckoSession>? =
            openSession(extension, false)
    }

    private val sessionActionDelegate = object : WebExtension.ActionDelegate {
        override fun onBrowserAction(extension: WebExtension, session: GeckoSession?, action: WebExtension.Action) {
            storeAction(extension, session, action)
        }
    }

    private val tabDelegate = object : WebExtension.TabDelegate {
        override fun onNewTab(source: WebExtension, createDetails: WebExtension.CreateTabDetails): GeckoResult<GeckoSession>? {
            val url = createDetails.url ?: return null
            val opener = tabOpener ?: return null
            val session = opener(url, createDetails.active != false)
            return GeckoResult.fromValue(session)
        }

        override fun onOpenOptionsPage(source: WebExtension) {
            showOptions(source)
        }
    }

    fun showOptions(extension: WebExtension) {
        val url = extension.metaData.optionsPageUrl?.takeIf { it.isNotBlank() } ?: return
        val result = openSession(extension, true)
        result.accept({ session -> session?.loadUri(url) }, { })
    }

    fun showOptions(info: ExtensionInfo) = showOptions(info.raw)

    private fun openSession(extension: WebExtension, fullScreen: Boolean): GeckoResult<GeckoSession> {
        closePopup()
        val session = GeckoSession()
        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onCloseRequest(session: GeckoSession) {
                closePopup()
            }

            override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                saveDownload(response)
            }
        }
        session.promptDelegate = popupPromptDelegate
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                openInTab(uri)
                closePopup()
                return null
            }

            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny>? {
                val target = request.uri
                if (target.startsWith("http://") || target.startsWith("https://")) {
                    openInTab(target)
                    closePopup()
                    return GeckoResult.fromValue(AllowOrDeny.DENY)
                }
                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }
        }
        session.open(ClintGecko.runtime)
        popup = ExtensionPopup(extension.id, extension.metaData.name ?: extension.id, session, fullScreen)
        return GeckoResult.fromValue(session)
    }

    fun closePopup() {
        val current = popup ?: return
        popup = null
        finishFilePick(emptyList())
        popupDialog?.cancel()
        popupDialog = null
        main.postDelayed({ runCatching { current.session.close() } }, 400)
    }

    fun finishFilePick(uris: List<Uri>) {
        val request = popupFileRequest ?: return
        popupFileRequest = null
        request.onResult(uris)
    }

    fun dismissPopupDialog(request: JsDialogRequest) {
        request.cancel()
        if (popupDialog === request) popupDialog = null
    }

    private fun pickerMimeTypes(raw: Array<String>?): Array<String> {
        val types = raw.orEmpty().mapNotNull { token ->
            val value = token.trim().lowercase()
            when {
                value.isEmpty() -> null
                value.startsWith(".") -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(value.removePrefix(".")) ?: "*/*"
                value.contains('/') -> value
                else -> "*/*"
            }
        }.distinct()
        return if (types.isEmpty() || "*/*" in types) arrayOf("*/*") else types.toTypedArray()
    }

    private fun saveDownload(response: WebResponse) {
        val uri = response.uri
        if (!ResponseDownloads.isInline(uri) && (uri.startsWith("http://", true) || uri.startsWith("https://", true))) {
            runCatching { response.body?.close() }
            openInTab(uri)
            return
        }
        if (!ResponseDownloads.save(currentActivityContext(), response)) toast(R.string.download_response_failed)
    }

    private val popupPromptDelegate = object : GeckoSession.PromptDelegate {
        private fun dialog(
            session: GeckoSession,
            type: JsDialogType,
            message: String?,
            default: String,
            onConfirm: (String) -> Unit,
            onCancel: () -> Unit
        ) {
            val current = popup
            if (current == null || current.session !== session) {
                onCancel()
                return
            }
            popupDialog?.cancel()
            popupDialog = JsDialogRequest(type, current.title, message.orEmpty(), default, onConfirm, onCancel)
        }

        override fun onAlertPrompt(session: GeckoSession, prompt: GeckoSession.PromptDelegate.AlertPrompt): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
            val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
            dialog(session, JsDialogType.Alert, prompt.message, "", { result.complete(prompt.dismiss()) }, { result.complete(prompt.dismiss()) })
            return result
        }

        override fun onButtonPrompt(session: GeckoSession, prompt: GeckoSession.PromptDelegate.ButtonPrompt): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
            val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
            dialog(
                session, JsDialogType.Confirm, prompt.message, "",
                { result.complete(prompt.confirm(GeckoSession.PromptDelegate.ButtonPrompt.Type.POSITIVE)) },
                { result.complete(prompt.confirm(GeckoSession.PromptDelegate.ButtonPrompt.Type.NEGATIVE)) }
            )
            return result
        }

        override fun onTextPrompt(session: GeckoSession, prompt: GeckoSession.PromptDelegate.TextPrompt): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
            val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
            dialog(
                session, JsDialogType.Prompt, prompt.message, prompt.defaultValue.orEmpty(),
                { value -> result.complete(prompt.confirm(value)) },
                { result.complete(prompt.dismiss()) }
            )
            return result
        }

        override fun onFilePrompt(session: GeckoSession, prompt: GeckoSession.PromptDelegate.FilePrompt): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
            val current = popup
            if (current == null || current.session !== session) return GeckoResult.fromValue(prompt.dismiss())
            val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
            val multiple = prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE
            finishFilePick(emptyList())
            popupFileRequest = ExtensionFileRequest(pickerMimeTypes(prompt.mimeTypes), multiple) { uris ->
                if (uris.isEmpty()) {
                    result.complete(prompt.dismiss())
                } else {
                    UploadCache.copyAsync(appContext, uris.toTypedArray()) { files ->
                        when {
                            files.isEmpty() -> {
                                toast(R.string.ext_toast_file_failed)
                                result.complete(prompt.dismiss())
                            }
                            multiple -> result.complete(prompt.confirm(appContext, files))
                            else -> result.complete(prompt.confirm(appContext, files[0]))
                        }
                    }
                }
            }
            return result
        }
    }

    fun openInTab(url: String) {
        val opener = tabOpener
        if (opener != null) {
            opener(url, true)
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .setClass(appContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { appContext.startActivity(intent) }
    }

    fun currentActivityContext(): Context =
        (appContext as? ClintApplication)?.currentActivity ?: appContext
}
