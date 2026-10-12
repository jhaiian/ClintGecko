package com.jhaiian.clint.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
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
import com.jhaiian.clint.browser.webview.ClintGecko
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.Image
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtension.InstallException.ErrorCodes
import org.mozilla.geckoview.WebExtensionController
import java.util.WeakHashMap

object ClintExtensions {

    const val BUILT_IN_ID = "clint@jhaiian.com"
    const val STORE_URL = "https://addons.mozilla.org/android/"

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
    private var actionVersion by mutableIntStateOf(0)

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
        c.install(uri, method).accept(
            { ext ->
                if (ext != null) {
                    register(ext)
                    toastOnce("installed:${ext.id}", R.string.ext_toast_installed, ext.metaData.name ?: ext.id)
                }
                refresh()
            },
            { error ->
                refresh()
                reportFailure(error, null)
            }
        )
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
        }
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
        main.postDelayed({ runCatching { current.session.close() } }, 400)
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
