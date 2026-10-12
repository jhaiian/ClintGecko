package com.jhaiian.clint.bookmarks

data class Bookmark(
    val id: Long = 0L,
    val url: String,
    val title: String,
    val faviconUrl: String = "",
    val lastVisit: Long = 0L,
    val addedAt: Long = 0L,
    val folderId: Long? = null
)
