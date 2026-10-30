package com.jhaiian.clint.downloads

import com.jhaiian.clint.R

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import com.jhaiian.clint.ui.ClintSnackbarHost
import com.jhaiian.clint.ui.ConfirmDialogHostActivity
import com.jhaiian.clint.ui.OverlayHostActivity
import com.jhaiian.clint.ui.SnackbarHostActivity
import com.jhaiian.clint.ui.rememberMaxContentWidth
import com.jhaiian.clint.ui.showClintSnackbar
import com.jhaiian.clint.ui.theme.ClintComposeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadsActivity : ClintActivity(), OverlayHostActivity, SnackbarHostActivity, ConfirmDialogHostActivity {

    override var overlayContent by mutableStateOf<(@Composable () -> Unit)?>(null)

    override val snackbarHostState = SnackbarHostState()

    override var confirmDialogConfig: com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig?
        get() = uiState.confirmDialogConfig
        set(value) { uiState.confirmDialogConfig = value }

    companion object {
        const val EXTRA_OPEN_ID = "open_download_id"

        fun open(context: android.content.Context) {
            context.startActivity(
                Intent(context, DownloadsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
        }
    }

    internal lateinit var uiState: DownloadsUiState

    internal var lastRefreshMs = 0L
    internal fun refresh() {}

    internal var manualFolderPickerCallback: ((Uri) -> Unit)? = null
    internal val manualFolderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            manualFolderPickerCallback?.invoke(uri)
        }
        manualFolderPickerCallback = null
    }

    private var pendingApkItem: DownloadItem? = null
    private val installPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val item = pendingApkItem ?: return@registerForActivityResult
        pendingApkItem = null
        if (packageManager.canRequestPackageInstalls()) launchApkInstall(item)
    }

    fun launchManualFolderPicker(onPicked: (Uri) -> Unit) {
        manualFolderPickerCallback = { uri ->
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            onPicked(uri)
        }
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
        manualFolderPickerLauncher.launch(intent)
    }

    private fun submitManualDownload(submission: ManualDownloadSubmission, onDismiss: () -> Unit, onRename: () -> Unit) {
        val ua = com.jhaiian.clint.browser.webview.ClintGecko.defaultUserAgent()
        performManualDownload(
            submission = submission,
            userAgent = ua,
            onDismiss = {
                showClintSnackbar(
                    message = getString(
                        if (submission.startPaused) R.string.download_added_paused else R.string.toast_downloading,
                        submission.filename
                    ),
                    actionLabel = getString(R.string.download_started_view_action),
                    onAction = { DownloadsActivity.open(this) }
                )
                onDismiss()
            },
            onRename = onRename
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        uiState = DownloadsUiState()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        uiState.libraryGrid = prefs.getBoolean(DownloadSettingsKeys.PREF_LIBRARY_GRID, DownloadSettingsKeys.DEFAULT_LIBRARY_GRID)
        handleOpenIntent(intent)
        handleDownloaderIntent(intent)

        val theme = prefs.getString("app_theme", "system") ?: "system"
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)

        setContent {
            ClintComposeTheme(theme = theme) {
                val maxContentWidth = rememberMaxContentWidth(this)
                val allItems by ClintDownloadManager.downloadsFlow.collectAsState()

                LaunchedEffect(allItems) {
                    val keepScreenOnEnabled = prefs.getBoolean(DownloadSettingsKeys.PREF_KEEP_SCREEN_ON, DownloadSettingsKeys.DEFAULT_KEEP_SCREEN_ON)
                    val hasPendingDownloads = allItems.any { it.status in DownloadStatus.NOT_FINISHED }
                    if (keepScreenOnEnabled && hasPendingDownloads) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                var tick by remember { mutableStateOf(0L) }
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(1000)
                        tick++
                    }
                }

                Box {
                    DownloadsScreen(
                        state = uiState,
                        allItems = allItems,
                        tick = tick,
                        maxContentWidth = maxContentWidth,
                        hideStatusBar = hideStatusBar, hideSystemNavigation = hideSystemNavigation,
                        onExit = { finish() },
                        onOpenItem = { item -> handleOpenItem(item) },
                        onDownloadSettingsClick = {
                            startActivity(Intent(this@DownloadsActivity, com.jhaiian.clint.settings.SettingsActivity::class.java)
                                .putExtra(com.jhaiian.clint.settings.SettingsActivity.EXTRA_OPEN_FRAGMENT, "download_settings"))
                        },
                        onPause = { id -> ClintDownloadManager.pause(this@DownloadsActivity, id) },
                        onResume = { id -> ClintDownloadManager.resume(this@DownloadsActivity, id) },
                        onRetry = { id -> ClintDownloadManager.retryFailed(this@DownloadsActivity, id) },
                        itemActions = DownloadItemActions(
                            onOpen = { item -> handleOpenItem(item) },
                            onShare = { item -> shareFile(item) },
                            onOpenFolder = { item -> openFolder(item) },
                            onRename = { item -> uiState.renameItem = item },
                            onRedownload = { item -> redownload(item) },
                            onRedownloadOptions = { item -> showRedownloadDialog(item) },
                            onChangeSettings = { item -> uiState.changeSettingsItem = item },
                            onUpdateLink = { item -> uiState.updateLinkItem = item },
                            onUpdateLinkInBrowser = { item -> openBrowserForRefreshLink(item) },
                            onRemove = { item -> uiState.deleteConfirmItems = listOf(item) },
                            onCopyLink = { item -> copyDownloadLink(item) },
                            onCopyFilename = { item -> copyFileName(item) },
                            onCopyPath = { item -> copyFilePath(item) },
                            onProperties = { item -> uiState.propertiesItem = item }
                        ),
                        onAddClick = { uiState.manualDownloadDialogOpen = true },
                        onDeleteSelectedClick = { items -> uiState.deleteConfirmItems = items },
                        onDeleteConfirmed = { items, deleteFromStorage -> executeDelete(items, deleteFromStorage) },
                        onMultiRedownload = { items -> multiRedownload(items); uiState.exitSelectionMode() },
                        onMultiCopyLink = { items -> multiCopyToClipboard(items.joinToString("\n") { it.url }, getString(R.string.download_menu_link_copied)) },
                        onMultiCopyFilename = { items -> multiCopyToClipboard(items.joinToString("\n") { it.filename }, getString(R.string.download_menu_filename_copied)) },
                        onMultiCopyPath = { items -> multiCopyToClipboard(items.joinToString("\n") { pathFor(it) }, getString(R.string.download_menu_path_copied)) },
                        onSubmitManualDownload = { submission, onDismiss, onRename -> submitManualDownload(submission, onDismiss, onRename) }
                    )
                    com.jhaiian.clint.ui.listscreen.ConfirmDialogHost(uiState.confirmDialogConfig, hideStatusBar, hideSystemNavigation) { uiState.confirmDialogConfig = null }
                    uiState.conflictDialogRequest?.let { req ->
                        DownloadConflictDialog(req, hideStatusBar, hideSystemNavigation) { uiState.conflictDialogRequest = null }
                    }
                    overlayContent?.invoke()
                    ClintSnackbarHost(hostState = snackbarHostState)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenIntent(intent)
        handleDownloaderIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        ClintDownloadManager.resumeTimeoutPaused(this)
    }

    private fun pathFor(item: DownloadItem): String = when {
        item.file != null -> item.file.absolutePath
        item.contentUri != null -> {
            val uri = Uri.parse(item.contentUri)
            val seg = uri.lastPathSegment ?: item.contentUri
            when {
                seg.startsWith("primary:") -> "/storage/emulated/0/${seg.removePrefix("primary:")}"
                seg.contains(":") -> { val p = seg.split(":", limit = 2); "/storage/${p[0]}/${p[1]}" }
                else -> item.contentUri
            }
        }
        else -> item.filename
    }

    private fun executeDelete(toRemove: List<DownloadItem>, deleteFromStorage: Boolean) {
        val count = toRemove.size
        if (count == 0) return
        uiState.deleteProgress = DeleteProgress(0, count)
        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                toRemove.forEachIndexed { index, item ->
                    ClintDownloadManager.remove(this@DownloadsActivity, item.id, deleteFromStorage)
                    val done = index + 1
                    withContext(Dispatchers.Main) { uiState.deleteProgress = DeleteProgress(done, count) }
                }
            }
            uiState.deleteProgress = null
            uiState.exitSelectionMode()
            Toast.makeText(this@DownloadsActivity, getString(R.string.downloads_items_removed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleDownloaderIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val sharedUrl = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim() ?: return
        if (sharedUrl.isEmpty()) return
        uiState.manualDownloadPrefillUrl = sharedUrl
        uiState.manualDownloadDialogOpen = true
    }

    private fun handleOpenIntent(intent: Intent?) {
        val id = intent?.getIntExtra(EXTRA_OPEN_ID, -1) ?: return
        if (id == -1) return
        val item = ClintDownloadManager.findItem(id) ?: return
        handleOpenItem(item)
    }

    internal fun handleOpenItem(item: DownloadItem) {
        if (item.status != DownloadStatus.COMPLETE) return
        if (!fileStillExists(item)) {
            Toast.makeText(this, getString(R.string.download_file_missing), Toast.LENGTH_SHORT).show()
            return
        }
        val ext = when {
            item.file != null -> item.file.extension.lowercase()
            item.contentUri != null -> item.filename.substringAfterLast('.').lowercase()
            else -> return
        }
        if (ext == "apk") handleApkOpen(item) else openFile(item)
    }

    private fun fileStillExists(item: DownloadItem): Boolean = when {
        item.file != null -> item.file.exists()
        item.contentUri != null -> runCatching {
            DocumentFile.fromSingleUri(this, Uri.parse(item.contentUri))?.exists() == true
        }.getOrDefault(false)
        else -> false
    }

    private fun handleApkOpen(item: DownloadItem) {
        uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
            title = getString(R.string.install_apk_dialog_title),
            message = getString(R.string.install_apk_dialog_message, item.filename),
            positiveLabel = getString(R.string.install_apk_dialog_confirm),
            onPositive = {
                if (packageManager.canRequestPackageInstalls()) launchApkInstall(item)
                else showInstallPermissionDialog(item)
            },
            negativeLabel = getString(R.string.action_cancel)
        )
    }

    private fun showInstallPermissionDialog(item: DownloadItem) {
        uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
            title = getString(R.string.install_apk_permission_title),
            message = getString(R.string.install_apk_permission_message),
            positiveLabel = getString(R.string.action_open_settings),
            onPositive = {
                pendingApkItem = item
                installPermissionLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))
                )
            },
            negativeLabel = getString(R.string.action_cancel)
        )
    }

    private fun launchApkInstall(item: DownloadItem) {
        val uri = when {
            item.file != null -> FileProvider.getUriForFile(this, "$packageName.fileprovider", item.file)
            item.contentUri != null -> Uri.parse(item.contentUri)
            else -> return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {}
    }

    private fun multiRedownload(items: List<DownloadItem>) {
        uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
            title = getString(R.string.redownload_multi_confirm_title, items.size),
            message = getString(R.string.redownload_multi_confirm_message),
            positiveLabel = getString(R.string.redownload_confirm_action),
            onPositive = {
                items.forEach { item ->
                    ClintDownloadManager.remove(this, item.id, true)
                    ClintDownloadManager.redownloadItem(this, item)
                }
                lastRefreshMs = 0L
                uiState.exitSelectionMode()
            },
            negativeLabel = getString(R.string.action_cancel)
        )
    }

    private fun multiCopyToClipboard(text: String, toastMessage: String) {
        val clipboard = getSystemService(android.content.ClipboardManager::class.java)
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("", text))
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareFile(item: DownloadItem) {
        val uri = when {
            item.file != null -> FileProvider.getUriForFile(this, "$packageName.fileprovider", item.file)
            item.contentUri != null -> Uri.parse(item.contentUri)
            else -> return
        }
        val ext = item.filename.substringAfterLast('.').lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        try {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, item.filename))
        } catch (_: Exception) {}
    }

    private fun openFolder(item: DownloadItem) {
        when {
            item.file != null -> {
                val parent = item.file.parentFile ?: return
                val standardDownloads = android.os.Environment
                    .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val isStandardDownloads = try {
                    parent.canonicalPath == standardDownloads.canonicalPath
                } catch (_: Exception) { false }

                if (isStandardDownloads) {
                    try {
                        startActivity(Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS))
                        return
                    } catch (_: Exception) {}
                }

                val externalRoot = android.os.Environment.getExternalStorageDirectory().absolutePath
                val relative = parent.absolutePath.removePrefix(externalRoot).trimStart('/')
                val docUri = Uri.parse(
                    "content://com.android.externalstorage.documents/document/primary:" +
                        Uri.encode(relative, "/")
                )
                try {
                    startActivity(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(docUri, "vnd.android.document/directory")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    })
                } catch (_: Exception) {
                    try {
                        startActivity(Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS))
                    } catch (_: Exception) {
                        showOpenFolderError(parent.absolutePath)
                    }
                }
            }
            item.contentUri != null -> {
                val treeUri = Uri.parse(item.contentUri)
                try {
                    val docUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(
                        treeUri,
                        android.provider.DocumentsContract.getTreeDocumentId(treeUri)
                    )
                    startActivity(Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(docUri, "vnd.android.document/directory")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    })
                } catch (_: Exception) {
                    try {
                        startActivity(Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS))
                    } catch (_: Exception) {
                        showOpenFolderError(treeUri.path ?: treeUri.toString())
                    }
                }
            }
            else -> return
        }
    }

    private fun showOpenFolderError(path: String) {
        uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
            title = getString(R.string.open_folder_error_title),
            message = getString(R.string.open_folder_error_message, path),
            positiveLabel = getString(R.string.action_ok)
        )
    }

    private fun redownload(item: DownloadItem) {
        uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
            title = getString(R.string.redownload_confirm_title),
            message = getString(R.string.redownload_confirm_message),
            positiveLabel = getString(R.string.redownload_confirm_action),
            onPositive = {
                ClintDownloadManager.remove(this, item.id, true)
                lastRefreshMs = 0L
                ClintDownloadManager.redownloadItem(this, item)
            },
            negativeLabel = getString(R.string.action_cancel)
        )
    }

    private fun copyDownloadLink(item: DownloadItem) {
        val clipboard = getSystemService(android.content.ClipboardManager::class.java)
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText(getString(R.string.download_dialog_link_clip_label), item.mega?.link?.takeIf { it.isNotBlank() } ?: item.url))
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, getString(R.string.download_menu_link_copied), Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyFileName(item: DownloadItem) {
        val clipboard = getSystemService(android.content.ClipboardManager::class.java)
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText(getString(R.string.download_menu_copy_filename), item.filename))
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, getString(R.string.download_menu_filename_copied), Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyFilePath(item: DownloadItem) {
        val path = when {
            item.file != null -> item.file.absolutePath
            item.contentUri != null -> {
                val uri = Uri.parse(item.contentUri)
                val segment = uri.lastPathSegment ?: item.contentUri
                when {
                    segment.startsWith("primary:") -> "/storage/emulated/0/${segment.removePrefix("primary:")}"
                    segment.contains(":") -> {
                        val parts = segment.split(":", limit = 2)
                        "/storage/${parts[0]}/${parts[1]}"
                    }
                    else -> item.contentUri
                }
            }
            else -> return
        }
        val clipboard = getSystemService(android.content.ClipboardManager::class.java)
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText(getString(R.string.download_menu_copy_path), path))
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, getString(R.string.download_menu_path_copied), Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFile(item: DownloadItem) {
        val ext = item.filename.substringAfterLast('.').lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        val uri = when {
            item.file != null -> FileProvider.getUriForFile(this, "$packageName.fileprovider", item.file)
            item.contentUri != null -> Uri.parse(item.contentUri)
            else -> return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (_: Exception) {}
    }

    private fun openBrowserForRefreshLink(item: DownloadItem) {
        val intent = Intent(this, com.jhaiian.clint.browser.MainActivity::class.java).apply {
            putExtra(com.jhaiian.clint.browser.MainActivity.EXTRA_REFRESH_LINK_MODE, true)
            putExtra(com.jhaiian.clint.browser.MainActivity.EXTRA_REFRESH_LINK_DOWNLOAD_ID, item.id)
            putExtra(com.jhaiian.clint.browser.MainActivity.EXTRA_REFRESH_LINK_FILENAME, item.filename)
            putExtra(com.jhaiian.clint.browser.MainActivity.EXTRA_REFRESH_LINK_ORIGINAL_URL, item.url)
            putExtra(com.jhaiian.clint.browser.MainActivity.EXTRA_REFRESH_LINK_ORIGINAL_REFERER, item.referer)
            putExtra(com.jhaiian.clint.browser.MainActivity.EXTRA_REFRESH_LINK_PAGE_URL, item.pageUrl.ifBlank { item.streamPageUrl })
        }
        startActivity(intent)
    }

}
