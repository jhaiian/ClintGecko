package com.jhaiian.clint.downloads

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import java.io.File
import java.util.Locale

internal object DownloadCategories {

    const val CATEGORY_VIDEOS = "Videos"
    const val CATEGORY_IMAGES = "Images"
    const val CATEGORY_AUDIO = "Audio"
    const val CATEGORY_DOCUMENTS = "Documents"
    const val CATEGORY_ARCHIVES = "Archives"
    const val CATEGORY_APPS = "Apps"
    const val CATEGORY_OTHERS = "Others"

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "wmv", "flv", "m4v", "3gp", "3g2",
        "mpg", "mpeg", "ts", "f4v", "vob", "ogv", "m2ts", "rmvb", "mts",
        "divx", "xvid", "asf", "m2v", "mxf", "ogm",
        "srt", "vtt", "ass", "sub", "ssa", "sbv", "smi"
    )
    private val IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif", "svg",
        "tiff", "tif", "ico", "avif", "raw", "cr2", "nef", "orf", "arw",
        "dng", "jfif", "jp2", "tga"
    )
    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "m4a", "aac", "wav", "flac", "ogg", "wma", "opus", "mid", "midi", "amr",
        "aiff", "aif", "alac", "ape", "mka", "caf", "dsd", "dsf", "dff", "ra", "rm",
        "spx", "voc"
    )
    private val DOCUMENT_EXTENSIONS = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf",
        "odt", "ods", "odp", "csv", "epub", "mobi", "md", "json", "xml",
        "xlsm", "tsv", "numbers", "pptm", "key", "docm", "pages", "markdown",
        "log", "wpd", "odf", "azw", "azw3", "azw4", "fb2", "djvu", "cbz", "cbr"
    )
    private val ARCHIVE_EXTENSIONS = setOf(
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso", "lz4", "zst",
        "br", "cab", "tgz", "tbz2", "txz", "z", "lzma", "lzh", "arj", "ace",
        "sit", "cpio", "xar"
    )
    private val APP_EXTENSIONS = setOf(
        "apk", "apks", "xapk", "aab", "apkm", "apkz", "exe", "msi", "msix",
        "dmg", "pkg", "deb", "rpm", "appimage", "run", "bat", "com", "jar"
    )

    fun isEnabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
            DownloadSettingsKeys.PREF_CATEGORIZE_DOWNLOADS,
            DownloadSettingsKeys.DEFAULT_CATEGORIZE_DOWNLOADS
        )

    fun categoryForFilename(filename: String): String {
        val ext = filename.substringAfterLast('.', "").lowercase(Locale.US)
        return when (ext) {
            in VIDEO_EXTENSIONS -> CATEGORY_VIDEOS
            in IMAGE_EXTENSIONS -> CATEGORY_IMAGES
            in AUDIO_EXTENSIONS -> CATEGORY_AUDIO
            in DOCUMENT_EXTENSIONS -> CATEGORY_DOCUMENTS
            in ARCHIVE_EXTENSIONS -> CATEGORY_ARCHIVES
            in APP_EXTENSIONS -> CATEGORY_APPS
            else -> CATEGORY_OTHERS
        }
    }

    fun categorySubpathDisplay(categorize: Boolean, filename: String): String =
        if (categorize) "${categoryForFilename(filename)}/" else ""

    fun resolveDir(categorize: Boolean, baseDir: File, filename: String): File {
        if (!categorize) return baseDir
        return File(baseDir, categoryForFilename(filename))
    }

    fun resolveSafDir(categorize: Boolean, docDir: DocumentFile, filename: String): DocumentFile {
        if (!categorize) return docDir
        val name = categoryForFilename(filename)
        val existing = docDir.findFile(name)
        if (existing != null && existing.isDirectory) return existing
        return docDir.createDirectory(name) ?: docDir
    }

    fun findExistingSafDir(categorize: Boolean, docDir: DocumentFile, filename: String): DocumentFile {
        if (!categorize) return docDir
        val name = categoryForFilename(filename)
        return docDir.findFile(name)?.takeIf { it.isDirectory } ?: docDir
    }
}
