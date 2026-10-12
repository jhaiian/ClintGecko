package com.jhaiian.clint.blocker

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.jhaiian.clint.base.ClintActivity
import com.jhaiian.clint.blocker.additional.AdditionalWebsitesActivity
import com.jhaiian.clint.blocker.additional.AdditionalWebsitesDatabase
import com.jhaiian.clint.blocker.engine.CompiledWebsiteBlockerManifest
import com.jhaiian.clint.blocker.engine.WebsiteBlockerNative
import com.jhaiian.clint.blocker.engine.WebsiteBlockerPaths
import com.jhaiian.clint.ui.listscreen.ConfirmDialogHost
import com.jhaiian.clint.ui.rememberMaxContentWidth
import com.jhaiian.clint.ui.theme.ClintComposeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class WebsiteBlockerActivity : ClintActivity() {

    companion object {
        const val PREF_ENABLED = "website_blocker_enabled"
    }

    internal lateinit var categoryDb: WebsiteBlockerCategoryDatabase
    internal lateinit var additionalDb: AdditionalWebsitesDatabase
    internal lateinit var uiState: WebsiteBlockerUiState
    internal val activityScope: CoroutineScope get() = lifecycleScope
    internal var activeJob: Job? = null

    internal fun reload() {
        WebsiteBlockerPaths.categoryFile(this, "ads").delete()
        refreshCategoryDisplay()
        uiState.additionalWebsitesCount = additionalDb.getAll().size
        val manifest = CompiledWebsiteBlockerManifest.read(WebsiteBlockerPaths.manifestFile(this))
        uiState.compiledEnabledIds = manifest?.enabledCategoryIds ?: emptySet()
        uiState.compiledAdditionalCount = manifest?.additionalWebsitesCount ?: 0
    }

    internal fun categoryTitle(category: WebsiteBlockerCategory): String = getString(categoryTitleRes(category.id))

    internal fun selectedCategories(): List<WebsiteBlockerCategory> = uiState.categories.filter { it.id in uiState.selectedIds }

    internal suspend fun countDomains(file: File): Long = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext 0L
        val builderPtr = WebsiteBlockerNative.nativeCreateBuilder()
        if (builderPtr == 0L) return@withContext 0L
        val stats = JSONObject(WebsiteBlockerNative.nativeAddText(builderPtr, file.readText()))
        WebsiteBlockerNative.nativeDestroyBuilder(builderPtr)
        stats.optLong("added", 0L)
    }

    internal fun downloadCategories(categories: List<WebsiteBlockerCategory>, force: Boolean, onComplete: () -> Unit = {}) {
        if (categories.isEmpty()) { onComplete(); return }
        activeJob = activityScope.launch {
            uiState.isDownloadRunning = true
            uiState.bannerText = null
            try {
                val total = categories.size
                categories.forEachIndexed { index, category ->
                    uiState.downloadProgress = WebsiteBlockerDownloadProgress(category.id, 0, 0, index, total)
                    val file = WebsiteBlockerPaths.categoryFile(this@WebsiteBlockerActivity, category.id)
                    val result = WebsiteBlockerDownloader.download(
                        url = category.downloadUrl,
                        destination = file,
                        etag = if (force) null else category.etag,
                        lastModified = if (force) null else category.lastModified
                    ) { bytesRead, contentLength ->
                        uiState.downloadProgress = WebsiteBlockerDownloadProgress(category.id, bytesRead, contentLength, index, total)
                    }
                    uiState.downloadProgress = uiState.downloadProgress?.copy(processedCount = index + 1)
                    when (result) {
                        is WebsiteBlockerDownloader.Result.Success -> {
                            categoryDb.updateDownloadState(
                                id = category.id,
                                isDownloaded = true,
                                downloadedAt = System.currentTimeMillis(),
                                domainCount = countDomains(file),
                                fileSizeBytes = file.length(),
                                etag = result.etag,
                                lastModified = result.lastModified
                            )
                        }
                        is WebsiteBlockerDownloader.Result.Failure -> uiState.bannerText = result.message
                        WebsiteBlockerDownloader.Result.NotModified -> {}
                    }
                }
            } finally {
                uiState.downloadProgress = null
                uiState.isDownloadRunning = false
                reload()
                onComplete()
            }
        }
    }

    internal fun handleToggle(category: WebsiteBlockerCategory, enabled: Boolean) {
        setPendingEnabled(category.id, enabled)
        if (enabled && !category.isDownloaded) {
            downloadCategories(listOf(category), force = false)
        }
    }

    internal fun removeCategories(ids: Collection<String>) {
        for (id in ids) {
            categoryDb.clearDownloadState(id)
            WebsiteBlockerPaths.categoryFile(this, id).delete()
        }
        reload()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        onBackPressedDispatcher.addCallback(this) {
            when {
                uiState.isSearchMode -> { uiState.isSearchMode = false; uiState.searchQuery = "" }
                uiState.isInSelectionMode -> uiState.exitSelectionMode()
                else -> handleBackNavigation()
            }
        }

        categoryDb = WebsiteBlockerCategoryDatabase(this)
        additionalDb = AdditionalWebsitesDatabase(this)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val theme = prefs.getString("app_theme", "system") ?: "system"
        val hideStatusBar = prefs.getBoolean("hide_status_bar", false)
        val hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false)

        uiState = WebsiteBlockerUiState()
        uiState.masterEnabled = prefs.getBoolean(PREF_ENABLED, false)
        reload()

        setContent {
            ClintComposeTheme(theme = theme) {
                val maxContentWidth = rememberMaxContentWidth(this)
                Box {
                    WebsiteBlockerScreen(
                        state = uiState,
                        maxContentWidth = maxContentWidth,
                        onExit = { handleBackNavigation() },
                        onMasterToggle = { enabled ->
                            uiState.masterEnabled = enabled
                            prefs.edit().putBoolean(PREF_ENABLED, enabled).apply()
                        },
                        onAdditionalWebsitesClick = {
                            startActivity(android.content.Intent(this@WebsiteBlockerActivity, AdditionalWebsitesActivity::class.java))
                        },
                        onItemToggle = { category, enabled -> handleToggle(category, enabled) },
                        onDeleteSelected = { confirmRemoveForSelection() },
                        onFabPrimaryClick = { startCompile() },
                        onRefreshClick = { confirmCheckUpdateAllCategories() },
                        onCheckUpdateActive = { confirmCheckUpdateActive() },
                        onCheckUpdateAll = { confirmCheckUpdateAllCategories() },
                        onForceUpdateActive = { confirmForceUpdateActive() },
                        onForceUpdateAll = { confirmForceUpdateAllCategories() },
                        onRecompile = { confirmRecompile() },
                        onItemCheckUpdate = { category -> confirmCheckUpdateForItem(category) },
                        onItemForceUpdate = { category -> confirmForceUpdateForItem(category) },
                        onItemRemove = { category -> confirmRemoveForItem(category) },
                        onItemCopyName = { category -> copyCategoryName(category) },
                        onItemCopyLink = { category -> copyCategoryLink(category) },
                        onItemShareLink = { category -> shareCategoryLink(category) },
                        onSelectionCheckUpdate = { confirmCheckUpdateForSelection() },
                        onSelectionForceUpdate = { confirmForceUpdateForSelection() },
                        onSelectionRemove = { confirmRemoveForSelection() },
                        onSelectionCopyName = { copySelectedCategoryNames() },
                        onSelectionCopyLink = { copySelectedCategoryLinks() },
                        onSelectionShareLink = { shareSelectedCategoryLinks() }
                    )

                    ConfirmDialogHost(uiState.confirmDialog, hideStatusBar, hideSystemNavigation) { uiState.confirmDialog = null }
                    val downloadingCategoryName = uiState.downloadProgress?.let { stringResourceCategoryTitle(it.categoryId) }
                    WebsiteBlockerDownloadProgressDialog(uiState.downloadProgress, downloadingCategoryName, hideStatusBar, hideSystemNavigation) { activeJob?.cancel() }
                    WebsiteBlockerCompileProgressDialog(uiState.compileProgress, hideStatusBar, hideSystemNavigation)
                    WebsiteBlockerCompileResultDialog(uiState.compileResult, hideStatusBar, hideSystemNavigation) { uiState.compileResult = null }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun stringResourceCategoryTitle(id: String): String =
    if (id == "additional") androidx.compose.ui.res.stringResource(com.jhaiian.clint.R.string.additional_websites_title)
    else androidx.compose.ui.res.stringResource(categoryTitleRes(id))
