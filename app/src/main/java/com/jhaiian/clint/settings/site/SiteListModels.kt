package com.jhaiian.clint.settings.site

import com.jhaiian.clint.ui.listscreen.ListSortKey
import com.jhaiian.clint.ui.listscreen.ListSortOrder

data class SiteEntry(val origin: String, val state: String, val addedAt: Long)

fun filterAndSortSites(
    items: List<SiteEntry>,
    query: String,
    sortKey: ListSortKey,
    sortOrder: ListSortOrder
): List<SiteEntry> {
    val filtered = if (query.isBlank()) {
        items
    } else {
        val lower = query.trim().lowercase()
        items.filter { it.origin.lowercase().contains(lower) }
    }
    return when (sortKey) {
        ListSortKey.TITLE -> if (sortOrder == ListSortOrder.ASCENDING)
            filtered.sortedBy { it.origin.lowercase() }
        else
            filtered.sortedByDescending { it.origin.lowercase() }
        ListSortKey.DATE_ADDED -> if (sortOrder == ListSortOrder.ASCENDING)
            filtered.sortedBy { it.addedAt }
        else
            filtered.sortedByDescending { it.addedAt }
    }
}

fun sectionLetterFor(entry: SiteEntry, sortKey: ListSortKey): String = when (sortKey) {
    ListSortKey.TITLE -> entry.origin.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
    ListSortKey.DATE_ADDED -> "#"
}
