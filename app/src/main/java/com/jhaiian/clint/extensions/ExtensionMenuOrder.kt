package com.jhaiian.clint.extensions

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.preference.PreferenceManager

object ExtensionMenuOrder {
    const val SORT_ENABLED = "enabled"
    const val SORT_NAME = "name"
    const val SORT_RECENT = "recent"
    const val SORT_CUSTOM = "custom"
    val SORT_MODES = listOf(SORT_ENABLED, SORT_NAME, SORT_RECENT, SORT_CUSTOM)

    private const val PREF_SORT = "extensions_menu_sort"
    private const val PREF_ORDER = "extensions_menu_order"
    private const val PREF_RECENT = "extensions_menu_recent"

    var sortMode by mutableStateOf(SORT_ENABLED)
        private set
    var customOrder by mutableStateOf<List<String>>(emptyList())
        private set
    var lastUsed by mutableStateOf<Map<String, Long>>(emptyMap())
        private set
    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        sortMode = prefs.getString(PREF_SORT, null)?.takeIf { it in SORT_MODES } ?: SORT_ENABLED
        customOrder = prefs.getString(PREF_ORDER, null)?.split('\n')?.filter { it.isNotBlank() }.orEmpty()
        lastUsed = prefs.getString(PREF_RECENT, null)?.split('\n').orEmpty().mapNotNull { line ->
            val id = line.substringBeforeLast('\t', "")
            val time = line.substringAfterLast('\t').toLongOrNull()
            if (id.isBlank() || time == null) null else id to time
        }.toMap()
    }

    fun setSort(context: Context, mode: String) {
        ensureLoaded(context)
        if (mode !in SORT_MODES || mode == sortMode) return
        sortMode = mode
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PREF_SORT, mode).apply()
    }

    fun setOrder(context: Context, ids: List<String>) {
        ensureLoaded(context)
        customOrder = ids
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PREF_ORDER, ids.joinToString("\n")).apply()
    }

    fun markUsed(context: Context, id: String) {
        ensureLoaded(context)
        val next = lastUsed + (id to System.currentTimeMillis())
        lastUsed = next
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(PREF_RECENT, next.entries.joinToString("\n") { "${it.key}\t${it.value}" })
            .apply()
    }

    fun customSorted(list: List<ExtensionInfo>): List<ExtensionInfo> {
        val index = customOrder.withIndex().associate { it.value to it.index }
        return list.sortedWith(compareBy<ExtensionInfo> { index[it.id] ?: Int.MAX_VALUE }.thenBy { it.name.lowercase() })
    }

    fun apply(list: List<ExtensionInfo>, topIds: Set<String>): List<ExtensionInfo> {
        val ordered = when (sortMode) {
            SORT_NAME -> list.sortedBy { it.name.lowercase() }
            SORT_RECENT -> list.sortedWith(compareByDescending<ExtensionInfo> { lastUsed[it.id] ?: 0L }.thenBy { it.name.lowercase() })
            SORT_CUSTOM -> customSorted(list)
            else -> list.sortedWith(compareByDescending<ExtensionInfo> { it.status == ExtensionStatus.ON }.thenBy { it.name.lowercase() })
        }
        if (topIds.isEmpty()) return ordered
        return ordered.filter { it.id in topIds } + ordered.filter { it.id !in topIds }
    }
}
