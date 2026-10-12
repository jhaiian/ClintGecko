package com.jhaiian.clint.blocker

internal fun WebsiteBlockerActivity.effectiveCategories(): List<WebsiteBlockerCategory> {
    val all = categoryDb.getAll()
    return all.map { row -> uiState.pendingEnabledOverrides[row.id]?.let { row.copy(isEnabled = it) } ?: row }
}

internal fun WebsiteBlockerActivity.refreshCategoryDisplay() {
    uiState.categories = effectiveCategories()
}

internal fun WebsiteBlockerActivity.setPendingEnabled(id: String, enabled: Boolean) {
    val baseline = id in uiState.compiledEnabledIds
    uiState.pendingEnabledOverrides = if (enabled == baseline) {
        uiState.pendingEnabledOverrides - id
    } else {
        uiState.pendingEnabledOverrides + (id to enabled)
    }
    refreshCategoryDisplay()
}

internal fun WebsiteBlockerActivity.discardPendingChanges() {
    uiState.pendingEnabledOverrides = emptyMap()
    refreshCategoryDisplay()
}
