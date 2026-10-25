package com.jhaiian.clint.browser.delegates
import com.jhaiian.clint.browser.sheets.*
import com.jhaiian.clint.browser.MainActivity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.webkit.MimeTypeMap
import com.jhaiian.clint.browser.webview.ClintWebView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.jhaiian.clint.R
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

private fun dataUriBytes(dataUri: String): ByteArray? {
    val commaIdx = dataUri.indexOf(",")
    if (commaIdx < 0) return null
    val header = dataUri.substring(0, commaIdx)
    val content = dataUri.substring(commaIdx + 1)
    return if (header.contains(";base64")) {
        try { android.util.Base64.decode(content, android.util.Base64.DEFAULT) } catch (_: Exception) { null }
    } else {
        try {
            java.net.URLDecoder.decode(content, "UTF-8").toByteArray(Charsets.UTF_8)
        } catch (_: Exception) { content.toByteArray(Charsets.UTF_8) }
    }
}

private fun dataUriMimeType(dataUri: String): String {
    val after = dataUri.removePrefix("data:")
    val end = after.indexOfFirst { it == ';' || it == ',' }
    return if (end > 0) after.substring(0, end).trim().lowercase() else "image/jpeg"
}

internal fun MainActivity.setupImageLongPress(webView: ClintWebView) {
    setupLinkLongPress(webView)
}

internal fun isStandaloneImagePage(url: String): Boolean {
    val lower = url.lowercase().substringBefore("?").substringBefore("#")
    return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
            lower.endsWith(".png") || lower.endsWith(".gif") ||
            lower.endsWith(".webp") || lower.endsWith(".bmp") ||
            lower.endsWith(".svg") || lower.endsWith(".avif")
}

internal fun MainActivity.showImageLongPressSheet(imageUrl: String, pageTitle: String, isStandalone: Boolean, referer: String = "") {
    if (uiState.imageLongPressRequest != null) return
    uiState.imageLongPressRequest = ImageLongPressRequest(imageUrl, pageTitle, isStandalone, referer)
}

internal fun MainActivity.handleImageOpenInNewTab(imageUrl: String) {
    openNewTab(isIncognito = false, url = imageUrl, profileId = activeProfileId())
}

internal fun MainActivity.handleImagePreview(imageUrl: String) {
    if (uiState.contentPreviewRequest != null) return
    uiState.contentPreviewRequest = ContentPreviewRequest.forImage(imageUrl)
}

internal fun MainActivity.handleImageDownload(imageUrl: String, altText: String) {
    val quickImages = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        .getBoolean(com.jhaiian.clint.settings.downloads.DownloadSettingsKeys.PREF_QUICK_DOWNLOAD_IMAGES, com.jhaiian.clint.settings.downloads.DownloadSettingsKeys.DEFAULT_QUICK_DOWNLOAD_IMAGES)
    if (imageUrl.startsWith("data:")) {
        val commaIdx = imageUrl.indexOf(",")
        if (commaIdx < 0) return
        val header = imageUrl.substring(0, commaIdx)
        val content = imageUrl.substring(commaIdx + 1)
        val mimeType = dataUriMimeType(imageUrl)
        val filename = resolveImageFilename(imageUrl, altText)
        val appContext = applicationContext
        com.jhaiian.clint.browser.webview.ClintGecko.io.execute {
            val blob = if (header.contains(";base64")) {
                com.jhaiian.clint.downloads.DownloadBlobStore.fromBase64(appContext, content)
            } else {
                runCatching { java.net.URLDecoder.decode(content, "UTF-8").toByteArray(Charsets.UTF_8) }.getOrNull()
                    ?.let { com.jhaiian.clint.downloads.DownloadBlobStore.fromBytes(appContext, it) }
            }
            runOnUiThread {
                if (blob == null) {
                    Toast.makeText(appContext, R.string.download_response_failed, Toast.LENGTH_SHORT).show()
                } else if (isFinishing || isDestroyed) {
                    com.jhaiian.clint.downloads.ClintDownloadManager.enqueueBlob(appContext, blob, filename, mimeType)
                } else {
                    showDownloadDialogForBlob(blob, filename, mimeType, quickImages)
                }
            }
        }
        return
    }
    val userAgent = tabManager.activeTab?.webView?.settings?.userAgentString ?: buildUserAgent()
    val referer = tabManager.activeTab?.webView?.url ?: ""
    val filename = resolveImageFilename(imageUrl, altText)
    val activeView = tabManager.activeTab?.webView
    if (activeView == null) {
        showDownloadDialog(imageUrl, filename, userAgent, referer, "", quickImages)
    } else {
        com.jhaiian.clint.profiles.WebProfiles.cookieForAsync(activeView, imageUrl) { cookies ->
            showDownloadDialog(imageUrl, filename, userAgent, referer, cookies, quickImages)
        }
    }
}

private fun resolveImageFilename(imageUrl: String, altText: String = ""): String {
    val knownExts = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "avif")

    fun extFromUrl(url: String): String {
        val seg = url.substringBefore("?").substringAfterLast(".")
            .lowercase().substringBefore("/")
        return if (seg in knownExts) seg else "jpg"
    }

    fun filenameFromUrl(url: String): String? {
        val path = url.substringBefore("?").substringAfterLast("/")
        val ext = path.substringAfterLast(".").lowercase()
        return if (ext in knownExts) path else null
    }

    fun sanitize(name: String) = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(100)

    if (imageUrl.startsWith("data:")) {
        val mimeType = dataUriMimeType(imageUrl)
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
        return if (altText.isNotEmpty()) "${sanitize(altText)}.$ext" else "image_${System.currentTimeMillis()}.$ext"
    }

    if (altText.isNotEmpty()) {
        val ext = filenameFromUrl(imageUrl)?.substringAfterLast(".")
            ?: run {
                try {
                    val uri = android.net.Uri.parse(imageUrl)
                    listOf("u", "url", "src", "img", "imgurl").firstNotNullOfOrNull { key ->
                        uri.getQueryParameter(key)?.let { param ->
                            java.net.URLDecoder.decode(param, "UTF-8").let { filenameFromUrl(it)?.substringAfterLast(".") }
                        }
                    }
                } catch (_: Exception) { null }
            }
            ?: extFromUrl(imageUrl)
        return "${sanitize(altText)}.$ext"
    }

    filenameFromUrl(imageUrl)?.let { return it }

    try {
        val uri = android.net.Uri.parse(imageUrl)
        for (key in listOf("u", "url", "src", "img", "imgurl")) {
            val param = uri.getQueryParameter(key) ?: continue
            val decoded = java.net.URLDecoder.decode(param, "UTF-8")
            filenameFromUrl(decoded)?.let { return it }
        }
    } catch (_: Exception) {}

    return "image_${System.currentTimeMillis()}.${extFromUrl(imageUrl)}"
}

internal fun MainActivity.handleImageCopy(imageUrl: String) {
    if (imageUrl.startsWith("data:")) {
        Thread {
            val bytes = dataUriBytes(imageUrl) ?: run {
                runOnUiThread { Toast.makeText(this, getString(R.string.image_copy_failed), Toast.LENGTH_SHORT).show() }
                return@Thread
            }
            val mimeType = dataUriMimeType(imageUrl)
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
            try {
                val cacheDir = File(cacheDir, "image_cache").also { it.mkdirs() }
                val file = File(cacheDir, "copied_image.$ext")
                file.writeBytes(bytes)
                val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                runOnUiThread {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newUri(contentResolver, getString(R.string.image_copy), uri)
                    clipboard.setPrimaryClip(clip)
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                        Toast.makeText(this, getString(R.string.image_copied), Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                runOnUiThread { Toast.makeText(this, getString(R.string.image_copy_failed), Toast.LENGTH_SHORT).show() }
            }
        }.start()
        return
    }
    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    Thread {
        try {
            val request = Request.Builder().url(imageUrl).build()
            val response = client.newCall(request).execute()
            val bytes = response.body.bytes()
            val ext = imageUrl.substringAfterLast(".").substringBefore("?").lowercase().let { e ->
                if (e in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")) e else "jpg"
            }
            val mimeType = when (ext) {
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "bmp" -> "image/bmp"
                else -> "image/jpeg"
            }
            val cacheDir = File(cacheDir, "image_cache").also { it.mkdirs() }
            val file = File(cacheDir, "copied_image.$ext")
            file.writeBytes(bytes)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            runOnUiThread {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newUri(contentResolver, getString(R.string.image_copy), uri)
                clipboard.setPrimaryClip(clip)
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                    Toast.makeText(this, getString(R.string.image_copied), Toast.LENGTH_SHORT).show()
                }
            }
        } catch (_: Exception) {
            runOnUiThread { Toast.makeText(this, getString(R.string.image_copy_failed), Toast.LENGTH_SHORT).show() }
        }
    }.start()
}

internal fun MainActivity.handleImageShare(imageUrl: String) {
    if (imageUrl.startsWith("data:")) {
        Thread {
            val bytes = dataUriBytes(imageUrl)
            if (bytes != null) {
                val mimeType = dataUriMimeType(imageUrl)
                val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
                try {
                    val cacheDir = File(cacheDir, "image_cache").also { it.mkdirs() }
                    val file = File(cacheDir, "shared_image.$ext")
                    file.writeBytes(bytes)
                    val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
                    runOnUiThread {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = mimeType
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(intent, getString(R.string.image_share)))
                    }
                } catch (_: Exception) {
                    runOnUiThread { shareImageUrl(imageUrl) }
                }
            } else {
                runOnUiThread { shareImageUrl(imageUrl) }
            }
        }.start()
        return
    }
    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    Thread {
        try {
            val request = Request.Builder().url(imageUrl).build()
            val response = client.newCall(request).execute()
            val ext = imageUrl.substringAfterLast(".").substringBefore("?").lowercase().let { e ->
                if (e in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")) e else "jpg"
            }
            val mimeType = when (ext) {
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "bmp" -> "image/bmp"
                else -> "image/jpeg"
            }
            val bytes = response.body.bytes()
            val cacheDir = File(cacheDir, "image_cache").also { it.mkdirs() }
            val file = File(cacheDir, "shared_image.$ext")
            file.writeBytes(bytes)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            runOnUiThread {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.image_share)))
            }
        } catch (_: Exception) {
            runOnUiThread { shareImageUrl(imageUrl) }
        }
    }.start()
}

private fun MainActivity.shareImageUrl(imageUrl: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, imageUrl)
    }
    startActivity(Intent.createChooser(intent, getString(R.string.image_share)))
}
