package com.jhaiian.clint.blocker.additional

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class AdditionalWebsitesUiState {
    var rules by mutableStateOf(emptyList<AdditionalWebsiteRule>())
    var isEnabled by mutableStateOf(true)
    var isSearchMode by mutableStateOf(false)
    var searchQuery by mutableStateOf("")
    var isInSelectionMode by mutableStateOf(false)
    var selectedIds by mutableStateOf(emptySet<Long>())
    var isAddDialogOpen by mutableStateOf(false)
    var addDialogText by mutableStateOf("")
    var addDialogError by mutableStateOf<String?>(null)

    fun toggleSelection(id: Long) {
        selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
    }

    fun exitSelectionMode() {
        isInSelectionMode = false
        selectedIds = emptySet()
    }
}

@Composable
fun rememberAdditionalWebsitesUiState(): AdditionalWebsitesUiState = remember { AdditionalWebsitesUiState() }

fun filterRules(rules: List<AdditionalWebsiteRule>, query: String): List<AdditionalWebsiteRule> {
    if (query.isBlank()) return rules
    return rules.filter { it.host.contains(query, ignoreCase = true) }
}
