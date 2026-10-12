package com.jhaiian.clint.history

data class HistoryItem(
    val query: String,
    val title: String,
    val timestamp: Long = 0L
)
