package com.jhaiian.clint.downloads.mega

sealed class MegaLink {
    abstract val url: String
    abstract val handle: String
    abstract val key: String

    val identity: String get() = "$handle#$key"

    data class File(override val url: String, override val handle: String, override val key: String) : MegaLink()

    data class Folder(
        override val url: String,
        override val handle: String,
        override val key: String,
        val focusHandle: String?
    ) : MegaLink()

    companion object {
        private val HOSTS = setOf("mega.nz", "www.mega.nz", "mega.co.nz", "www.mega.co.nz", "mega.app", "www.mega.app")
        private val TOKEN = Regex("^[A-Za-z0-9_-]+$")

        fun parse(raw: String?): MegaLink? {
            val url = raw?.trim().orEmpty()
            val host = hostOf(url) ?: return null
            if (host !in HOSTS) return null
            val afterScheme = url.substringAfter("://")
            val tail = afterScheme.substring(afterScheme.indexOfFirst { it == '/' || it == '#' || it == '?' }.let { if (it < 0) afterScheme.length else it })
            val fragment = tail.substringAfter('#', "")
            val path = tail.substringBefore('#').substringBefore('?').trimEnd('/')
            return when {
                path.startsWith("/file/") || path.startsWith("/embed/") -> {
                    val segment = path.substringAfter('/', "").substringAfter('/')
                    val handle = segment.substringBefore('!')
                    val key = fragment.ifEmpty { segment.substringAfter('!', "") }.trimStart('!').substringBefore('/')
                    file(url, handle, key)
                }
                path.startsWith("/folder/") -> {
                    val handle = path.removePrefix("/folder/").substringBefore('/')
                    val key = fragment.substringBefore('/')
                    folder(url, handle, key, focusOf(fragment.substringAfter('/', "")))
                }
                (path.isEmpty() || path == "/embed") && fragment.startsWith("!") -> {
                    val parts = fragment.removePrefix("!").split('!')
                    file(url, parts.getOrNull(0).orEmpty(), parts.getOrNull(1).orEmpty())
                }
                path.isEmpty() && fragment.startsWith("F!") -> {
                    val parts = fragment.removePrefix("F!").split('!', '?', limit = 3)
                    folder(url, parts.getOrNull(0).orEmpty(), parts.getOrNull(1).orEmpty(), parts.getOrNull(2)?.takeIf { TOKEN.matches(it) })
                }
                else -> null
            }
        }

        private fun focusOf(rest: String): String? {
            val node = when {
                rest.startsWith("file/") -> rest.removePrefix("file/")
                rest.startsWith("folder/") -> rest.removePrefix("folder/")
                else -> return null
            }.substringBefore('/')
            return node.takeIf { TOKEN.matches(it) }
        }

        private fun file(url: String, handle: String, key: String): File? =
            if (TOKEN.matches(handle) && keyLength(key) == 32) File(url, handle, key) else null

        private fun folder(url: String, handle: String, key: String, focus: String?): Folder? =
            if (TOKEN.matches(handle) && keyLength(key) == 16) Folder(url, handle, key, focus) else null

        private fun keyLength(key: String): Int =
            if (!TOKEN.matches(key)) -1 else runCatching { MegaCrypto.decodeBase64(key).size }.getOrDefault(-1)

        private fun hostOf(url: String): String? {
            val schemeEnd = url.indexOf("://")
            if (schemeEnd <= 0) return null
            val scheme = url.substring(0, schemeEnd).lowercase()
            if (scheme != "https" && scheme != "http") return null
            val authority = url.substring(schemeEnd + 3).takeWhile { it != '/' && it != '#' && it != '?' }
            return authority.substringAfterLast('@').substringBefore(':').lowercase().ifEmpty { null }
        }
    }
}
