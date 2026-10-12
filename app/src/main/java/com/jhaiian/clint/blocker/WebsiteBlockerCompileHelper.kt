package com.jhaiian.clint.blocker

import com.jhaiian.clint.R
import com.jhaiian.clint.blocker.engine.WebsiteBlockerCompiler
import com.jhaiian.clint.blocker.engine.WebsiteBlockerPaths
import com.jhaiian.clint.blocker.engine.WebsiteBlockerWebIntegration
import com.jhaiian.clint.ui.status.formatElapsedSeconds
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.util.formatFileSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat

internal fun WebsiteBlockerActivity.handleBackNavigation() {
    if (uiState.isCompileRunning) return
    if (!uiState.isConfigurationDirty()) {
        finish()
        return
    }
    uiState.confirmDialog = ConfirmDialogConfig(
        title = getString(R.string.shared_back_dialog_title),
        message = getString(R.string.website_blocker_back_dialog_message),
        neutralLabel = getString(R.string.action_cancel),
        negativeLabel = getString(R.string.shared_back_dialog_discard),
        onNegative = { discardPendingChanges(); finish() },
        positiveLabel = getString(R.string.shared_back_dialog_compile),
        onPositive = { startCompile() }
    )
}

private fun WebsiteBlockerActivity.stageLabelFor(id: String): String =
    if (id == "additional") getString(R.string.additional_websites_title) else getString(categoryTitleRes(id))

internal fun WebsiteBlockerActivity.startCompile() {
    if (uiState.isCompileRunning) return

    val pending = uiState.categories.filter { it.isEnabled && !it.isDownloaded }

    activeJob = activityScope.launch {
        uiState.bannerText = null

        if (pending.isNotEmpty()) {
            uiState.isDownloadRunning = true
            for (category in pending) {
                uiState.downloadProgress = WebsiteBlockerDownloadProgress(category.id, 0, 0)
                val file = WebsiteBlockerPaths.categoryFile(this@startCompile, category.id)
                val result = WebsiteBlockerDownloader.download(
                    url = category.downloadUrl, destination = file,
                    etag = category.etag, lastModified = category.lastModified
                ) { bytesRead, contentLength ->
                    uiState.downloadProgress = WebsiteBlockerDownloadProgress(category.id, bytesRead, contentLength)
                }
                if (result is WebsiteBlockerDownloader.Result.Success) {
                    categoryDb.updateDownloadState(
                        id = category.id, isDownloaded = true, downloadedAt = System.currentTimeMillis(),
                        domainCount = countDomains(file), fileSizeBytes = file.length(),
                        etag = result.etag, lastModified = result.lastModified
                    )
                } else if (result is WebsiteBlockerDownloader.Result.Failure) {
                    uiState.bannerText = result.message
                }
            }
            uiState.downloadProgress = null
            uiState.isDownloadRunning = false
            refreshCategoryDisplay()
        }

        uiState.isCompileRunning = true
        val compileStartMs = System.currentTimeMillis()
        uiState.compileProgress = WebsiteBlockerCompileProgressUi(
            stageText = "", counterText = "",
            elapsedText = getString(R.string.shared_compile_progress_elapsed, "0s")
        )

        var timerJob: Job? = null
        timerJob = activityScope.launch {
            while (true) {
                delay(500L)
                val elapsedSec = (System.currentTimeMillis() - compileStartMs) / 1000L
                uiState.compileProgress = uiState.compileProgress?.copy(
                    elapsedText = getString(R.string.shared_compile_progress_elapsed, formatElapsedSeconds(elapsedSec))
                )
            }
        }

        try {
            val effective = effectiveCategories()
            val additionalHosts = if (com.jhaiian.clint.blocker.additional.AdditionalWebsitesState.isEnabled(this@startCompile))
                additionalDb.getAll().map { it.host } else emptyList()
            val result = WebsiteBlockerCompiler.compile(
                context = this@startCompile,
                categories = effective,
                additionalWebsiteHosts = additionalHosts
            ) { progress ->
                uiState.compileProgress = uiState.compileProgress?.copy(
                    stageText = stageLabelFor(progress.currentLabel),
                    counterText = getString(R.string.shared_compile_progress_list, progress.itemsProcessed, progress.totalItems)
                )
            }
            timerJob.cancel()
            uiState.compileProgress = null

            if (result.success) {
                for ((id, enabled) in uiState.pendingEnabledOverrides) {
                    categoryDb.updateEnabled(id, enabled)
                }
                uiState.pendingEnabledOverrides = emptyMap()
                uiState.bannerText = null

                WebsiteBlockerWebIntegration.onCompileComplete(this@startCompile)
                reload()

                val fmt = NumberFormat.getNumberInstance()
                uiState.compileResult = WebsiteBlockerCompileResultUi(
                    isSuccess = true,
                    title = getString(R.string.shared_compile_success_title),
                    rows = listOf(
                        WebsiteBlockerResultRow(getString(R.string.website_blocker_compile_result_label_domains), fmt.format(result.domainCount)),
                        WebsiteBlockerResultRow(getString(R.string.shared_compile_result_label_size), formatFileSize(result.sizeBytes)),
                        WebsiteBlockerResultRow(getString(R.string.shared_compile_result_label_duration), formatElapsedSeconds((System.currentTimeMillis() - compileStartMs) / 1000L))
                    )
                )
            } else {
                reload()
                val detail = buildString {
                    append(getString(R.string.shared_compile_failure_details, result.error ?: getString(R.string.website_blocker_compile_failed)))
                    append("\n")
                    append(getString(R.string.shared_compile_failure_previous_active))
                }
                uiState.compileResult = WebsiteBlockerCompileResultUi(
                    isSuccess = false,
                    title = getString(R.string.shared_compile_failure_title),
                    failureDetail = detail,
                    onRetry = { startCompile() }
                )
            }
        } catch (e: CancellationException) {
            timerJob.cancel()
            uiState.compileProgress = null
            throw e
        } finally {
            uiState.isCompileRunning = false
        }
    }
}
