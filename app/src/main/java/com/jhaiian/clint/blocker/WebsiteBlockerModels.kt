package com.jhaiian.clint.blocker

import com.jhaiian.clint.ui.listscreen.ListSortKey
import com.jhaiian.clint.ui.listscreen.ListSortOrder

fun filterAndSortCategories(
    items: List<WebsiteBlockerCategory>,
    query: String,
    sortKey: ListSortKey,
    sortOrder: ListSortOrder,
    titleOf: (WebsiteBlockerCategory) -> String
): List<WebsiteBlockerCategory> {
    val filtered = if (query.isBlank()) {
        items
    } else {
        val lower = query.trim().lowercase()
        items.filter { titleOf(it).lowercase().contains(lower) }
    }
    return when (sortKey) {
        ListSortKey.TITLE -> if (sortOrder == ListSortOrder.ASCENDING)
            filtered.sortedBy { titleOf(it).lowercase() }
        else
            filtered.sortedByDescending { titleOf(it).lowercase() }

        ListSortKey.DATE_ADDED -> if (sortOrder == ListSortOrder.ASCENDING)
            filtered.sortedBy { it.downloadedAt }
        else
            filtered.sortedByDescending { it.downloadedAt }
    }
}

fun sectionLetterForCategory(entry: WebsiteBlockerCategory, sortKey: ListSortKey, titleOf: (WebsiteBlockerCategory) -> String): String =
    when (sortKey) {
        ListSortKey.TITLE -> titleOf(entry).firstOrNull()?.uppercaseChar()?.toString() ?: "#"
        ListSortKey.DATE_ADDED -> "#"
    }
