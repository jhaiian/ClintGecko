package com.jhaiian.clint.bookmarks

import com.jhaiian.clint.R

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.listscreen.ConfirmDialogHost
import com.jhaiian.clint.ui.rememberMaxContentWidth
import com.jhaiian.clint.ui.theme.ClintComposeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookmarksActivity : ClintActivity() {

    private lateinit var uiState: BookmarksUiState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        onBackPressedDispatcher.addCallback(this) {
            when {
                uiState.isSearchMode -> { uiState.isSearchMode = false; uiState.searchQuery = "" }
                uiState.isInSelectionMode -> uiState.exitSelectionMode()
                uiState.currentFolderId != null -> uiState.navigateUp()
                else -> { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
            }
        }

        uiState = BookmarksUiState()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("app_theme", "system") ?: "system"
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)

        loadBookmarks()

        setContent {
            ClintComposeTheme(theme = theme) {
                val maxContentWidth = rememberMaxContentWidth(this)

                BookmarksScreen(
                    state = uiState,
                    maxContentWidth = maxContentWidth,
                    hideStatusBar = hideStatusBar,
                    hideSystemNavigation = hideSystemNavigation,
                    onExit = { finish() },
                    onOpenItem = { item -> openBookmark(item) },
                    onDeleteSelectedClick = { showDeleteConfirm() },
                    onCreateFolder = { name -> createFolder(name) },
                    onAddBookmark = { title, url -> addBookmark(title, url) },
                    onRenameFolder = { folder, name -> renameFolder(folder, name) },
                    onMoveConfirm = { targetFolderId -> moveSelected(targetFolderId) },
                    onImportHtml = { uri -> importHtml(uri) },
                    onImportSqlite = { uri -> importSqlite(uri) },
                    onExportHtml = { uri -> exportHtml(uri) },
                    onExportSqlite = { uri -> exportSqlite(uri) }
                )

                ConfirmDialogHost(uiState.deleteConfirm, hideStatusBar, hideSystemNavigation) { uiState.deleteConfirm = null }
            }
        }
    }

    private fun loadBookmarks() {
        lifecycleScope.launch {
            val bookmarks = withContext(Dispatchers.IO) { BookmarkManager.getAll(this@BookmarksActivity) }
            val folders = withContext(Dispatchers.IO) { BookmarkManager.getAllFoldersFlat(this@BookmarksActivity) }
            uiState.allBookmarks = bookmarks
            uiState.allFolders = folders
            uiState.isLoading = false
        }
    }

    private fun openBookmark(item: Bookmark) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
        intent.setPackage(packageName)
        startActivity(intent)
        finish()
    }

    private fun createFolder(name: String) {
        val parentId = uiState.currentFolderId
        lifecycleScope.launch {
            val newId = withContext(Dispatchers.IO) { BookmarkManager.createFolder(applicationContext, name, parentId) }
            uiState.allFolders = uiState.allFolders + BookmarkFolder(
                id = newId, name = name, parentId = parentId, createdAt = System.currentTimeMillis()
            )
        }
    }

    private fun addBookmark(title: String, url: String) {
        val folderId = uiState.currentFolderId
        val normalizedUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        val bookmark = Bookmark(
            url = normalizedUrl,
            title = title.ifBlank { normalizedUrl },
            addedAt = System.currentTimeMillis(),
            folderId = folderId
        )
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { BookmarkManager.add(applicationContext, bookmark) }
            uiState.allBookmarks = uiState.allBookmarks + bookmark
        }
    }

    private fun renameFolder(folder: BookmarkFolder, newName: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { BookmarkManager.renameFolder(applicationContext, folder.id, newName) }
            uiState.allFolders = uiState.allFolders.map { if (it.id == folder.id) it.copy(name = newName) else it }
            uiState.exitSelectionMode()
        }
    }

    private fun showDeleteConfirm() {
        val count = uiState.selectedCount
        if (count == 0) return
        val hasFolders = uiState.selectedFolderIds.isNotEmpty()
        uiState.deleteConfirm = ConfirmDialogConfig(
            title = getString(R.string.bookmarks_delete_confirm_title),
            message = if (hasFolders) {
                getString(R.string.bookmarks_delete_confirm_message_folders, count)
            } else {
                getString(R.string.bookmarks_delete_confirm_message, count)
            },
            negativeLabel = getString(R.string.action_cancel),
            positiveLabel = getString(R.string.bookmarks_delete_selected),
            onPositive = { deleteSelected() }
        )
    }

    private fun deleteSelected() {
        val selectedFolderIds = uiState.selectedFolderIds
        val selectedUrls = uiState.selectedItemUrls
        val descendantIds = collectDescendantIds(uiState.allFolders, selectedFolderIds)
        val removedFolderIds = selectedFolderIds + descendantIds
        val removedUrls = selectedUrls + uiState.allBookmarks
            .filter { it.folderId != null && it.folderId in removedFolderIds }
            .map { it.url }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                selectedFolderIds.forEach { BookmarkManager.deleteFolderRecursive(applicationContext, it) }
                selectedUrls.forEach { BookmarkManager.remove(applicationContext, it) }
            }
            uiState.allFolders = uiState.allFolders.filterNot { it.id in removedFolderIds }
            uiState.allBookmarks = uiState.allBookmarks.filterNot { it.url in removedUrls }
            val openFolderId = uiState.currentFolderId
            if (openFolderId != null && openFolderId in removedFolderIds) uiState.currentFolderId = null
            uiState.exitSelectionMode()
            Toast.makeText(this@BookmarksActivity, getString(R.string.bookmarks_items_deleted), Toast.LENGTH_SHORT).show()
        }
    }

    private fun moveSelected(targetFolderId: Long?) {
        val selectedFolderIds = uiState.selectedFolderIds
        val selectedUrls = uiState.selectedItemUrls
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                selectedFolderIds.forEach { BookmarkManager.moveFolder(applicationContext, it, targetFolderId) }
                selectedUrls.forEach { BookmarkManager.moveBookmark(applicationContext, it, targetFolderId) }
            }
            uiState.allFolders = uiState.allFolders.map {
                if (it.id in selectedFolderIds) it.copy(parentId = targetFolderId) else it
            }
            uiState.allBookmarks = uiState.allBookmarks.map {
                if (it.url in selectedUrls) it.copy(folderId = targetFolderId) else it
            }
            uiState.moveDialogOpen = false
            uiState.exitSelectionMode()
        }
    }

    private fun importHtml(uri: Uri) {
        val rootFolderId = uiState.currentFolderId
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openInputStream(uri)?.use { input ->
                        BookmarksHtmlFormat.import(applicationContext, input.bufferedReader().readText(), rootFolderId)
                    } ?: throw IllegalStateException("Unable to open input stream")
                }
            }
            handleImportResult(result)
        }
    }

    private fun importSqlite(uri: Uri) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openInputStream(uri)?.use { input ->
                        BookmarksSqliteFormat.import(applicationContext, input)
                    } ?: throw IllegalStateException("Unable to open input stream")
                }
            }
            handleImportResult(result)
        }
    }

    private fun handleImportResult(result: Result<Int>) {
        result.onSuccess { count ->
            loadBookmarks()
            Toast.makeText(this, getString(R.string.bookmarks_import_success, count), Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, getString(R.string.bookmarks_import_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportHtml(uri: Uri) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val html = BookmarksHtmlFormat.export(
                        BookmarkManager.getAllFoldersFlat(applicationContext),
                        BookmarkManager.getAll(applicationContext)
                    )
                    contentResolver.openOutputStream(uri)?.use { out -> out.write(html.toByteArray(Charsets.UTF_8)) }
                        ?: throw IllegalStateException("Unable to open output stream")
                }
            }
            handleExportResult(result)
        }
    }

    private fun exportSqlite(uri: Uri) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    contentResolver.openOutputStream(uri)?.use { out -> BookmarksSqliteFormat.export(applicationContext, out) }
                        ?: throw IllegalStateException("Unable to open output stream")
                }
            }
            handleExportResult(result)
        }
    }

    private fun handleExportResult(result: Result<Unit>) {
        result.onSuccess {
            Toast.makeText(this, getString(R.string.bookmarks_export_success), Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, getString(R.string.bookmarks_export_error), Toast.LENGTH_SHORT).show()
        }
    }
}
