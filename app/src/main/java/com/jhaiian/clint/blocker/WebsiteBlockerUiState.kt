package com.jhaiian.clint.blocker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig
import com.jhaiian.clint.ui.listscreen.ListSortKey
import com.jhaiian.clint.ui.listscreen.ListSortOrder

data class WebsiteBlockerDownloadProgress(
    val categoryId: String,
    val bytesRead: Long,
    val contentLength: Long,
    val processedCount: Int = 0,
    val totalCount: Int = 0
)

data class WebsiteBlockerCompileProgressUi(
    val stageText: String,
    val counterText: String,
    val elapsedText: String
)

data class WebsiteBlockerResultRow(val label: String, val value: String)

data class WebsiteBlockerCompileResultUi(
    val isSuccess: Boolean,
    val title: String,
    val rows: List<WebsiteBlockerResultRow> = emptyList(),
    val failureDetail: String? = null,
    val onRetry: (() -> Unit)? = null
)

class WebsiteBlockerUiState {
    var categories by mutableStateOf(emptyList<WebsiteBlockerCategory>())
    var additionalWebsitesCount by mutableStateOf(0)
    var masterEnabled by mutableStateOf(false)

    var searchQuery by mutableStateOf("")
    var isSearchMode by mutableStateOf(false)
    var sortKey by mutableStateOf(ListSortKey.TITLE)
    var sortOrder by mutableStateOf(ListSortOrder.ASCENDING)
    var sortMenuOpen by mutableStateOf(false)
    var actionsMenuOpen by mutableStateOf(false)
    var selectionOptionsMenuOpen by mutableStateOf(false)

    var isInSelectionMode by mutableStateOf(false)
    var selectedIds by mutableStateOf(emptySet<String>())

    var pendingEnabledOverrides by mutableStateOf<Map<String, Boolean>>(emptyMap())

    var isDownloadRunning by mutableStateOf(false)
    var isCompileRunning by mutableStateOf(false)
    var downloadProgress by mutableStateOf<WebsiteBlockerDownloadProgress?>(null)
    var compileProgress by mutableStateOf<WebsiteBlockerCompileProgressUi?>(null)
    var compileResult by mutableStateOf<WebsiteBlockerCompileResultUi?>(null)
    var bannerText by mutableStateOf<String?>(null)
    var confirmDialog by mutableStateOf<ConfirmDialogConfig?>(null)

    var compiledEnabledIds by mutableStateOf(emptySet<String>())
    var compiledAdditionalCount by mutableStateOf(0)

    fun isConfigurationDirty(): Boolean =
        pendingEnabledOverrides.isNotEmpty() ||
            additionalWebsitesCount != compiledAdditionalCount ||
            compiledEnabledIds.any { id -> categories.none { it.id == id } }

    fun toggleSelection(id: String) {
        selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
    }

    fun enterSelectionWith(id: String) {
        isInSelectionMode = true
        selectedIds = selectedIds + id
    }

    fun selectAll(displayed: List<WebsiteBlockerCategory>) {
        selectedIds = selectedIds + displayed.map { it.id }
    }

    fun invertSelection(displayed: List<WebsiteBlockerCategory>) {
        val displayedIds = displayed.map { it.id }.toSet()
        val keptOutsideView = selectedIds - displayedIds
        val invertedWithinView = displayedIds - selectedIds
        selectedIds = keptOutsideView + invertedWithinView
    }

    fun deselectAll() {
        selectedIds = emptySet()
    }

    fun exitSelectionMode() {
        isInSelectionMode = false
        selectedIds = emptySet()
    }
}

@Composable
fun rememberWebsiteBlockerUiState(): WebsiteBlockerUiState = remember { WebsiteBlockerUiState() }
