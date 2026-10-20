package com.jhaiian.clint.downloads

import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.InputStream
import java.util.Locale

internal object DownloadFilenameResolver {

    private const val FALLBACK_NAME = "download"
    const val SNIFF_BYTES = 512

    private val scriptExtensions = setOf(
        "php", "php3", "php4", "php5", "php7", "phtml", "asp", "aspx", "ashx", "asmx", "axd",
        "jsp", "jspx", "jsf", "do", "action", "cgi", "fcgi", "cfm", "cfml", "nsf", "srf",
        "wss", "yaws", "lasso", "shtml"
    )

    private val placeholderExtensions = setOf("bin", "tmp", "download", "crdownload", "part")

    private val markupExtensions = setOf("html", "htm", "xhtml")

    private val textExtensions = setOf("html", "htm", "xhtml", "xml", "txt", "json", "js", "css", "csv", "svg", "php")

    private val genericMimeTypes = setOf(
        "application/octet-stream", "binary/octet-stream", "application/binary",
        "application/force-download", "application/download", "application/x-download",
        "application/unknown", "application/x-unknown", "application/save", "application/x-octet-stream"
    )

    private val queryNameKeys = listOf(
        "filename", "file_name", "fname", "fn", "file", "name", "title", "download", "dl", "attachment"
    )

    private val mimeExtensions = mapOf(
        "video/mp4" to "mp4", "video/x-m4v" to "m4v", "video/webm" to "webm",
        "video/x-matroska" to "mkv", "video/matroska" to "mkv", "video/quicktime" to "mov",
        "video/x-msvideo" to "avi", "video/avi" to "avi", "video/x-flv" to "flv",
        "video/mp2t" to "ts", "video/3gpp" to "3gp", "video/3gpp2" to "3g2",
        "video/mpeg" to "mpg", "video/ogg" to "ogv", "video/x-ms-wmv" to "wmv",
        "audio/mpeg" to "mp3", "audio/mp3" to "mp3", "audio/mpeg3" to "mp3",
        "audio/mp4" to "m4a", "audio/x-m4a" to "m4a", "audio/m4a" to "m4a",
        "audio/aac" to "aac", "audio/x-aac" to "aac", "audio/aacp" to "aac",
        "audio/ogg" to "ogg", "audio/opus" to "opus", "audio/flac" to "flac", "audio/x-flac" to "flac",
        "audio/wav" to "wav", "audio/x-wav" to "wav", "audio/wave" to "wav", "audio/webm" to "weba",
        "audio/x-ms-wma" to "wma", "audio/amr" to "amr",
        "application/vnd.apple.mpegurl" to "m3u8", "application/x-mpegurl" to "m3u8",
        "audio/mpegurl" to "m3u8", "audio/x-mpegurl" to "m3u8", "application/dash+xml" to "mpd",
        "image/jpeg" to "jpg", "image/jpg" to "jpg", "image/pjpeg" to "jpg", "image/png" to "png",
        "image/gif" to "gif", "image/webp" to "webp", "image/avif" to "avif", "image/heic" to "heic",
        "image/heif" to "heif", "image/bmp" to "bmp", "image/svg+xml" to "svg", "image/x-icon" to "ico",
        "image/vnd.microsoft.icon" to "ico", "image/tiff" to "tiff",
        "application/pdf" to "pdf", "application/zip" to "zip", "application/x-zip-compressed" to "zip",
        "application/x-7z-compressed" to "7z", "application/x-rar-compressed" to "rar",
        "application/vnd.rar" to "rar", "application/x-rar" to "rar", "application/gzip" to "gz",
        "application/x-gzip" to "gz", "application/x-tar" to "tar", "application/x-bzip2" to "bz2",
        "application/x-xz" to "xz", "application/zstd" to "zst",
        "application/vnd.android.package-archive" to "apk", "application/x-apks" to "apks",
        "application/java-archive" to "jar", "application/x-msdownload" to "exe",
        "application/x-msi" to "msi", "application/x-iso9660-image" to "iso",
        "application/x-apple-diskimage" to "dmg", "application/vnd.debian.binary-package" to "deb",
        "application/x-bittorrent" to "torrent", "application/epub+zip" to "epub",
        "application/msword" to "doc", "application/vnd.ms-excel" to "xls",
        "application/vnd.ms-powerpoint" to "ppt",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to "docx",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" to "xlsx",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation" to "pptx",
        "application/vnd.oasis.opendocument.text" to "odt", "application/rtf" to "rtf",
        "application/json" to "json", "application/xml" to "xml", "text/xml" to "xml",
        "application/javascript" to "js", "text/javascript" to "js", "text/css" to "css",
        "text/plain" to "txt", "text/html" to "html", "application/xhtml+xml" to "xhtml",
        "text/csv" to "csv", "text/markdown" to "md", "text/vtt" to "vtt",
        "application/x-subrip" to "srt", "text/srt" to "srt", "font/ttf" to "ttf",
        "font/otf" to "otf", "font/woff" to "woff", "font/woff2" to "woff2"
    )

    private val knownExtensions: Set<String> by lazy { mimeExtensions.values.toSet() }

    fun resolve(url: String, contentDisposition: String?, mimeType: String?): String {
        val mime = normalizeMime(mimeType)
        val fromHeader = contentDisposition?.let { DownloadFileHelper.extractFilenameFromContentDisposition(it) }?.let(::clean)
        if (fromHeader != null) return withMimeExtension(fromHeader, mime)
        val fromPath = runCatching { Uri.parse(url).lastPathSegment }.getOrNull()?.let(::clean)
        if (fromPath != null && hasTrustedExtension(fromPath)) return withMimeExtension(fromPath, mime)
        val fromQuery = nameFromQuery(url)
        if (fromQuery != null) return fromQuery
        return withMimeExtension(fromPath ?: FALLBACK_NAME, mime)
    }

    fun applyContentDisposition(filename: String, contentDisposition: String?): String =
        contentDisposition?.let { DownloadFileHelper.extractFilenameFromContentDisposition(it) }?.let(::clean) ?: filename

    fun refine(filename: String, mimeType: String?, head: ByteArray?): String {
        val (base, ext) = split(filename)
        val sniffed = head?.takeIf { it.isNotEmpty() }?.let { DownloadFileHelper.detectExtFromMagicBytes(it) }
            ?.lowercase(Locale.ROOT)?.takeIf(::isValidExtension)
        val safeBase = base.ifBlank { FALLBACK_NAME }
        return when {
            ext.isEmpty() || isUntrusted(ext) -> {
                val replacement = sniffed ?: normalizeMime(mimeType)?.let(::extensionForMime)
                if (replacement != null) "$safeBase.$replacement" else filename
            }
            ext in markupExtensions && sniffed != null && sniffed !in textExtensions -> "$safeBase.$sniffed"
            else -> filename
        }
    }

    fun needsSniffing(filename: String): Boolean {
        val ext = split(filename).second
        return ext.isEmpty() || isUntrusted(ext) || ext in markupExtensions
    }

    fun readHead(stream: InputStream, max: Int = SNIFF_BYTES): ByteArray {
        val buffer = ByteArray(max)
        var total = 0
        while (total < max) {
            val read = stream.read(buffer, total, max - total)
            if (read <= 0) break
            total += read
        }
        return buffer.copyOf(total)
    }

    fun extensionForMime(mimeType: String): String? {
        val mime = normalizeMime(mimeType) ?: return null
        return mimeExtensions[mime]
            ?: MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)?.lowercase(Locale.ROOT)?.takeIf(::isValidExtension)
    }

    private fun withMimeExtension(name: String, mime: String?): String {
        val (base, ext) = split(name)
        val mimeExt = mime?.let(::extensionForMime)
        return when {
            ext.isEmpty() -> if (mimeExt != null) "$name.$mimeExt" else name
            isUntrusted(ext) -> if (mimeExt != null) "${base.ifBlank { FALLBACK_NAME }}.$mimeExt" else name
            mimeExt != null && !isKnownExtension(ext) -> "$name.$mimeExt"
            else -> name
        }
    }

    private fun nameFromQuery(url: String): String? {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        if (uri.isOpaque) return null
        val names = runCatching { uri.queryParameterNames }.getOrNull() ?: return null
        val lookup = names.associateBy { it.lowercase(Locale.ROOT) }
        for (key in queryNameKeys) {
            val actual = lookup[key] ?: continue
            val value = runCatching { uri.getQueryParameter(actual) }.getOrNull() ?: continue
            val candidate = clean(value) ?: continue
            if (hasTrustedExtension(candidate)) return candidate
        }
        return null
    }

    private fun hasTrustedExtension(name: String): Boolean {
        val ext = split(name).second
        return ext.isNotEmpty() && !isUntrusted(ext) && isKnownExtension(ext)
    }

    private fun isUntrusted(ext: String): Boolean = ext in scriptExtensions || ext in placeholderExtensions

    private fun isKnownExtension(ext: String): Boolean =
        ext in knownExtensions || MimeTypeMap.getSingleton().hasExtension(ext)

    private fun isValidExtension(ext: String): Boolean =
        ext.length in 1..10 && ext.all { it in 'a'..'z' || it in '0'..'9' }

    private fun split(name: String): Pair<String, String> {
        val dot = name.lastIndexOf('.')
        if (dot <= 0 || dot == name.length - 1) return name to ""
        val ext = name.substring(dot + 1).lowercase(Locale.ROOT)
        if (!isValidExtension(ext)) return name to ""
        return name.substring(0, dot) to ext
    }

    private fun clean(raw: String): String? {
        val trimmed = raw.substringAfterLast('/').substringAfterLast('\\').trim().trimEnd('.')
        if (trimmed.isBlank() || trimmed.all { it == '.' }) return null
        return DownloadFileHelper.sanitizeFileName(trimmed)
    }

    private fun normalizeMime(mimeType: String?): String? {
        val mime = mimeType?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
        if (mime.isNullOrEmpty() || !mime.contains('/') || mime in genericMimeTypes) return null
        return mime
    }
}
