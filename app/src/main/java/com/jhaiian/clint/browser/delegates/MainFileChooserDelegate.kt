package com.jhaiian.clint.browser.delegates
import com.jhaiian.clint.browser.MainActivity

internal fun MainActivity.onShowFileChooser(
    callback: android.webkit.ValueCallback<Array<android.net.Uri>>,
    params: com.jhaiian.clint.browser.webview.FileChooserParams
): Boolean {
    filePathCallback?.onReceiveValue(null)
    filePathCallback = null

    val hasCameraPermission = androidx.core.content.ContextCompat.checkSelfPermission(
        this, android.Manifest.permission.CAMERA
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    if (!hasCameraPermission) {
        pendingFileChooserCallback = callback
        pendingFileChooserParams = params
        if (shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)) {
            uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
                title = getString(com.jhaiian.clint.R.string.camera_permission_title),
                message = getString(com.jhaiian.clint.R.string.camera_permission_message),
                cancelable = false,
                positiveLabel = getString(com.jhaiian.clint.R.string.action_allow),
                onPositive = { cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) },
                negativeLabel = getString(com.jhaiian.clint.R.string.action_not_now),
                onNegative = {
                    val cb = pendingFileChooserCallback
                    val pending = pendingFileChooserParams
                    pendingFileChooserCallback = null
                    pendingFileChooserParams = null
                    if (cb != null && pending != null && !launchFileChooser(cb, pending)) cb.onReceiveValue(null)
                }
            )
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
        return true
    }

    return launchFileChooser(callback, params)
}

internal fun MainActivity.launchFileChooser(
    callback: android.webkit.ValueCallback<Array<android.net.Uri>>,
    params: com.jhaiian.clint.browser.webview.FileChooserParams
): Boolean {
    filePathCallback = callback

    val accept = params.acceptTypes?.joinToString(",") ?: "*/*"
    val isImageOnly = accept.contains("image") && !accept.contains("video") && !accept.contains("audio") && !accept.contains("*/*")
    val isVideoOnly = accept.contains("video") && !accept.contains("image") && !accept.contains("*/*")
    val allowMultiple = params.mode == com.jhaiian.clint.browser.webview.FileChooserParams.MODE_OPEN_MULTIPLE

    val extraIntents = mutableListOf<android.content.Intent>()

    val canUseCamera = androidx.core.content.ContextCompat.checkSelfPermission(
        this, android.Manifest.permission.CAMERA
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    if (!isVideoOnly && canUseCamera) {
        try {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.TITLE, "clint_capture_${System.currentTimeMillis()}")
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
            val uri = contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                cameraImageUri = uri
                extraIntents.add(android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri)
                })
            }
        } catch (_: Exception) {}
    }

    if (!isImageOnly) {
        try { extraIntents.add(android.content.Intent(android.provider.MediaStore.Audio.Media.RECORD_SOUND_ACTION)) } catch (_: Exception) {}
        if (canUseCamera) extraIntents.add(android.content.Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE))
    }

    val mimeType = when {
        isImageOnly -> "image/*"
        isVideoOnly -> "video/*"
        accept.contains("audio") && !accept.contains("*/*") -> "audio/*"
        else -> "*/*"
    }

    val contentIntent = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
        type = mimeType
        addCategory(android.content.Intent.CATEGORY_OPENABLE)
        if (allowMultiple) putExtra(android.content.Intent.EXTRA_ALLOW_MULTIPLE, true)
    }

    val chooser = android.content.Intent.createChooser(contentIntent, null).apply {
        if (extraIntents.isNotEmpty()) putExtra(android.content.Intent.EXTRA_INITIAL_INTENTS, extraIntents.toTypedArray())
    }

    try {
        fileChooserLauncher.launch(chooser)
    } catch (_: android.content.ActivityNotFoundException) {
        filePathCallback = null
        cameraImageUri = null
        cameraVideoUri = null
        return false
    }
    return true
}
