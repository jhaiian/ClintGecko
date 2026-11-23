package com.jhaiian.clint.blocker

import com.jhaiian.clint.R
import com.jhaiian.clint.parental.ParentalGateRequest

internal fun WebsiteBlockerActivity.confirmMasterDisable() {
    if (!uiState.masterEnabled) return
    uiState.guardRequest = ParentalGateRequest(
        title = getString(R.string.website_blocker_guard_disable_title),
        message = getString(R.string.website_blocker_guard_disable_message),
        positiveLabel = getString(R.string.website_blocker_guard_turn_off_action),
        onConfirm = { setMasterEnabled(false) }
    )
}

internal fun WebsiteBlockerActivity.confirmCategoryDisable(category: WebsiteBlockerCategory) {
    uiState.guardRequest = ParentalGateRequest(
        title = getString(R.string.website_blocker_guard_category_title, categoryTitle(category)),
        message = getString(R.string.website_blocker_guard_category_message),
        positiveLabel = getString(R.string.website_blocker_guard_turn_off_action),
        onConfirm = { applyToggle(category, false) }
    )
}
