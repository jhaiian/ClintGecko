package com.jhaiian.clint.browser.webview

import android.net.Uri

class ClintRequest(
    val url: Uri,
    val method: String,
    val isForMainFrame: Boolean,
    val requestHeaders: Map<String, String>,
    val resourceType: String = "other",
    val hasGesture: Boolean = false
) {
    companion object {
        fun fromExtension(url: String, method: String, type: String, headers: Map<String, String>? = null): ClintRequest {
            val merged = LinkedHashMap<String, String>()
            when (type) {
                "document", "sub_frame" -> {
                    merged["Accept"] = "text/html,application/xhtml+xml"
                    merged["Upgrade-Insecure-Requests"] = "1"
                    merged["Sec-Fetch-Dest"] = if (type == "document") "document" else "iframe"
                }
                "script" -> merged["Sec-Fetch-Dest"] = "script"
                "stylesheet" -> merged["Sec-Fetch-Dest"] = "style"
                "image" -> {
                    merged["Accept"] = "image/*"
                    merged["Sec-Fetch-Dest"] = "image"
                }
                "font" -> merged["Sec-Fetch-Dest"] = "font"
                "media" -> merged["Sec-Fetch-Dest"] = "video"
                "xhr" -> merged["Sec-Fetch-Dest"] = "empty"
            }
            if (headers != null) merged.putAll(headers)
            return ClintRequest(Uri.parse(url), method, type == "document", merged, type)
        }
    }
}
