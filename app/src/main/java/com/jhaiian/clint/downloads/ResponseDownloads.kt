package com.jhaiian.clint.downloads

import android.content.Context
import android.util.Base64
import android.webkit.URLUtil
import android.widget.Toast
import com.jhaiian.clint.R
import com.jhaiian.clint.browser.MainActivity
import com.jhaiian.clint.browser.delegates.showDownloadDialogForBlob
import com.jhaiian.clint.browser.webview.ClintGecko
import org.mozilla.geckoview.WebResponse

object ResponseDownloads {

    fun isInline(uri: String): Boolean =
        uri.startsWith("data:", true) || uri.startsWith("blob:moz-extension:", true)

    fun save(context: Context, response: WebResponse): Boolean {
        val body = response.body ?: return false
        fun header(name: String): String? = response.headers.entries.firstOrNull { it.key.equals(name, true) }?.value
        val mime = header("Content-Type")?.substringBefore(';')?.trim()?.takeIf { it.isNotEmpty() }
            ?: response.uri.takeIf { it.startsWith("data:", true) }
                ?.substringAfter(':')?.substringBefore(',')?.substringBefore(';')?.trim()?.takeIf { it.contains('/') }
        val source = if (isInline(response.uri)) "" else response.uri
        val filename = URLUtil.guessFileName(source, header("Content-Disposition"), mime)
        val appContext = context.applicationContext
        ClintGecko.io.execute {
            val encoded = runCatching {
                Base64.encodeToString(body.use { it.readBytes() }, Base64.NO_WRAP)
            }.getOrNull()
            ClintGecko.main.post {
                if (encoded == null) {
                    Toast.makeText(appContext, R.string.download_response_failed, Toast.LENGTH_SHORT).show()
                } else if (context is MainActivity && !context.isFinishing && !context.isDestroyed) {
                    context.showDownloadDialogForBlob(encoded, filename, mime ?: "application/octet-stream")
                } else {
                    ClintDownloadManager.enqueueBlob(context, encoded, filename, mime ?: "application/octet-stream")
                }
            }
        }
        return true
    }
}
