package com.jhaiian.clint.browser.webview

import android.net.Uri

fun interface CustomViewCallback {
    fun onCustomViewHidden()
}

fun interface GeolocationCallback {
    fun invoke(origin: String, allow: Boolean, retain: Boolean)
}

class FileChooserParams(
    val acceptTypes: Array<String>?,
    val mode: Int,
    val isCaptureEnabled: Boolean
) {
    companion object {
        const val MODE_OPEN = 0
        const val MODE_OPEN_MULTIPLE = 1

        fun parseResult(resultCode: Int, data: android.content.Intent?): Array<Uri>? {
            if (resultCode != android.app.Activity.RESULT_OK || data == null) return null
            val clip = data.clipData
            if (clip != null && clip.itemCount > 0) {
                return Array(clip.itemCount) { clip.getItemAt(it).uri }
            }
            val single = data.data ?: return null
            return arrayOf(single)
        }
    }
}

class ClintPermissionRequest(
    val origin: Uri,
    val resources: Array<String>,
    private val onGrant: (Array<String>) -> Unit,
    private val onDeny: () -> Unit
) {
    private var done = false

    fun grant(granted: Array<String>) {
        if (done) return
        done = true
        onGrant(granted)
    }

    fun deny() {
        if (done) return
        done = true
        onDeny()
    }

    companion object {
        const val RESOURCE_VIDEO_CAPTURE = "android.webkit.resource.VIDEO_CAPTURE"
        const val RESOURCE_AUDIO_CAPTURE = "android.webkit.resource.AUDIO_CAPTURE"
    }
}

class ContextMenuInfo(
    val type: Int,
    val linkUrl: String?,
    val srcUrl: String?,
    val title: String?,
    val altText: String?
) {
    companion object {
        const val TYPE_NONE = 0
        const val TYPE_IMAGE = 1
        const val TYPE_VIDEO = 2
        const val TYPE_AUDIO = 3
    }
}
