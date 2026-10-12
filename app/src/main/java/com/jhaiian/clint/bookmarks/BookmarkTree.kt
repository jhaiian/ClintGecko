package com.jhaiian.clint.bookmarks

data class FolderTreeEntry(val folder: BookmarkFolder, val depth: Int)

fun collectDescendantIds(folders: List<BookmarkFolder>, rootIds: Set<Long>): Set<Long> {
    val byParent = folders.groupBy { it.parentId }
    val result = mutableSetOf<Long>()
    fun collect(id: Long) {
        byParent[id]?.forEach { child ->
            if (result.add(child.id)) collect(child.id)
        }
    }
    rootIds.forEach { collect(it) }
    return result
}

fun buildFolderTree(folders: List<BookmarkFolder>, excludeIds: Set<Long> = emptySet()): List<FolderTreeEntry> {
    val byParent = folders.groupBy { it.parentId }
    val result = mutableListOf<FolderTreeEntry>()
    fun visit(parentId: Long?, depth: Int) {
        val children = byParent[parentId].orEmpty().sortedBy { it.name.lowercase() }
        for (child in children) {
            if (child.id in excludeIds) continue
            result.add(FolderTreeEntry(child, depth))
            visit(child.id, depth + 1)
        }
    }
    visit(null, 0)
    return result
}

fun folderPathFor(folders: List<BookmarkFolder>, folderId: Long?): List<BookmarkFolder> {
    if (folderId == null) return emptyList()
    val byId = folders.associateBy { it.id }
    val path = mutableListOf<BookmarkFolder>()
    var current: Long? = folderId
    val guard = mutableSetOf<Long>()
    while (current != null) {
        val folder = byId[current] ?: break
        if (!guard.add(folder.id)) break
        path.add(0, folder)
        current = folder.parentId
    }
    return path
}
