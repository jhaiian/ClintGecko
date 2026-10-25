package com.jhaiian.clint.browser.delegates
import com.jhaiian.clint.browser.MainActivity

import android.Manifest
import com.jhaiian.clint.browser.webview.GeolocationCallback
import com.jhaiian.clint.browser.webview.ClintPermissionRequest as PermissionRequest
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.sitepermissions.SitePermissionDatabase
import com.jhaiian.clint.settings.sitepermissions.SitePermissionManager
import com.jhaiian.clint.settings.sitepermissions.SitePermissionActivity

private fun originFromRequest(request: PermissionRequest): String =
    request.origin.host?.takeIf { it.isNotEmpty() }
        ?: request.origin.toString()

private fun originFromString(origin: String): String =
    android.net.Uri.parse(origin).host?.takeIf { it.isNotEmpty() } ?: origin

internal fun MainActivity.isSystemPermissionGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

private fun MainActivity.showWebPermissionDialog(
    title: String,
    message: String,
    isIncognito: Boolean,
    onAllow: (remember: Boolean) -> Unit,
    onDeny: (remember: Boolean) -> Unit
) {
    uiState.webPermissionDialogRequest = com.jhaiian.clint.ui.WebPermissionDialogRequest(
        title = title,
        message = message,
        isIncognito = isIncognito,
        onAllow = onAllow,
        onDeny = onDeny
    )
}

internal fun MainActivity.onWebPermissionRequest(request: PermissionRequest) {
    val resources = request.resources
    val wantsCamera = PermissionRequest.RESOURCE_VIDEO_CAPTURE in resources
    val wantsMic = PermissionRequest.RESOURCE_AUDIO_CAPTURE in resources

    if (!wantsCamera && !wantsMic) {
        request.deny()
        return
    }

    val isIncognito = tabManager.activeTab?.isIncognito == true
    val origin = originFromRequest(request)

    if (wantsCamera && !wantsMic) {
        if (!isIncognito) {
            val stored = SitePermissionManager.getState(this, origin, SitePermissionDatabase.TYPE_CAMERA)
            if (stored == SitePermissionDatabase.STATE_ALLOW) {
                if (isSystemPermissionGranted(Manifest.permission.CAMERA)) {
                    request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                } else {
                    pendingWebPermissionRequest = request
                    webCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
                return
            }
            if (stored == SitePermissionDatabase.STATE_DENY) {
                request.deny()
                return
            }
        }
        if (isSystemPermissionGranted(Manifest.permission.CAMERA)) {
            if (!isIncognito) {
                val globalDefault = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString("site_perm_default_${SitePermissionDatabase.TYPE_CAMERA}", SitePermissionActivity.PREF_VALUE_ASK) ?: SitePermissionActivity.PREF_VALUE_ASK
                when (globalDefault) {
                    SitePermissionActivity.PREF_VALUE_ALLOW -> { request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)); return }
                    SitePermissionActivity.PREF_VALUE_DENY -> { request.deny(); return }
                    else -> {}
                }
            }
            showWebCameraDialog(request)
        } else {
            pendingWebPermissionRequest = request
            val needsRationale = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
            if (needsRationale) {
                uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
                    title = getString(R.string.camera_permission_title),
                    message = getString(R.string.camera_permission_message),
                    positiveLabel = getString(R.string.action_allow),
                    onPositive = { webCameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    negativeLabel = getString(R.string.action_deny),
                    onNegative = {
                        pendingWebPermissionRequest?.deny()
                        pendingWebPermissionRequest = null
                    }
                )
            } else {
                webCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        return
    }

    if (wantsMic && !wantsCamera) {
        if (!isIncognito) {
            val stored = SitePermissionManager.getState(this, origin, SitePermissionDatabase.TYPE_MIC)
            if (stored == SitePermissionDatabase.STATE_ALLOW) {
                if (isSystemPermissionGranted(Manifest.permission.RECORD_AUDIO)) {
                    request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                } else {
                    pendingWebMicPermissionRequest = request
                    webMicrophonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
                return
            }
            if (stored == SitePermissionDatabase.STATE_DENY) {
                request.deny()
                return
            }
        }
        if (isSystemPermissionGranted(Manifest.permission.RECORD_AUDIO)) {
            if (!isIncognito) {
                val globalDefault = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString("site_perm_default_${SitePermissionDatabase.TYPE_MIC}", SitePermissionActivity.PREF_VALUE_ASK) ?: SitePermissionActivity.PREF_VALUE_ASK
                when (globalDefault) {
                    SitePermissionActivity.PREF_VALUE_ALLOW -> { request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)); return }
                    SitePermissionActivity.PREF_VALUE_DENY -> { request.deny(); return }
                    else -> {}
                }
            }
            showWebMicDialog(request)
        } else {
            pendingWebMicPermissionRequest = request
            val needsRationale = shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)
            if (needsRationale) {
                uiState.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
                    title = getString(R.string.voice_search_permission_title),
                    message = getString(R.string.voice_search_permission_message),
                    positiveLabel = getString(R.string.action_allow),
                    onPositive = { webMicrophonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    negativeLabel = getString(R.string.action_deny),
                    onNegative = {
                        pendingWebMicPermissionRequest?.deny()
                        pendingWebMicPermissionRequest = null
                    }
                )
            } else {
                webMicrophonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        return
    }

    if (!isIncognito) {
        val cameraStored = SitePermissionManager.getState(this, origin, SitePermissionDatabase.TYPE_CAMERA)
        val micStored = SitePermissionManager.getState(this, origin, SitePermissionDatabase.TYPE_MIC)
        if (cameraStored != null && micStored != null) {
            val toGrant = mutableListOf<String>()
            if (cameraStored == SitePermissionDatabase.STATE_ALLOW && isSystemPermissionGranted(Manifest.permission.CAMERA))
                toGrant.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
            if (micStored == SitePermissionDatabase.STATE_ALLOW && isSystemPermissionGranted(Manifest.permission.RECORD_AUDIO))
                toGrant.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
            if (toGrant.isEmpty()) request.deny() else request.grant(toGrant.toTypedArray())
            return
        }
    }

    val cameraOk = isSystemPermissionGranted(Manifest.permission.CAMERA)
    val micOk = isSystemPermissionGranted(Manifest.permission.RECORD_AUDIO)
    if (cameraOk && micOk) {
        showWebCameraAndMicDialog(request, origin)
    } else {
        request.deny()
    }
}

internal fun MainActivity.showWebCameraDialog(request: PermissionRequest) {
    val isIncognito = tabManager.activeTab?.isIncognito == true
    val rawOrigin = originFromRequest(request)
    val origin = rawOrigin.ifEmpty { getString(R.string.camera_web_request_origin_unknown) }
    showWebPermissionDialog(
        title = getString(R.string.camera_web_request_title),
        message = getString(R.string.camera_web_request_message, origin),
        isIncognito = isIncognito,
        onAllow = { remember ->
            if (remember && !isIncognito) SitePermissionManager.setState(this, rawOrigin, SitePermissionDatabase.TYPE_CAMERA, SitePermissionDatabase.STATE_ALLOW)
            request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
        },
        onDeny = { remember ->
            if (remember && !isIncognito) SitePermissionManager.setState(this, rawOrigin, SitePermissionDatabase.TYPE_CAMERA, SitePermissionDatabase.STATE_DENY)
            request.deny()
        }
    )
}

internal fun MainActivity.showWebMicDialog(request: PermissionRequest) {
    val isIncognito = tabManager.activeTab?.isIncognito == true
    val rawOrigin = originFromRequest(request)
    val origin = rawOrigin.ifEmpty { getString(R.string.mic_web_request_origin_unknown) }
    showWebPermissionDialog(
        title = getString(R.string.mic_web_request_title),
        message = getString(R.string.mic_web_request_message, origin),
        isIncognito = isIncognito,
        onAllow = { remember ->
            if (remember && !isIncognito) SitePermissionManager.setState(this, rawOrigin, SitePermissionDatabase.TYPE_MIC, SitePermissionDatabase.STATE_ALLOW)
            request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
        },
        onDeny = { remember ->
            if (remember && !isIncognito) SitePermissionManager.setState(this, rawOrigin, SitePermissionDatabase.TYPE_MIC, SitePermissionDatabase.STATE_DENY)
            request.deny()
        }
    )
}

private fun MainActivity.showWebCameraAndMicDialog(request: PermissionRequest, rawOrigin: String) {
    val isIncognito = tabManager.activeTab?.isIncognito == true
    val origin = rawOrigin.ifEmpty { getString(R.string.camera_web_request_origin_unknown) }
    showWebPermissionDialog(
        title = getString(R.string.camera_web_request_title),
        message = getString(R.string.camera_web_request_message, origin),
        isIncognito = isIncognito,
        onAllow = { remember ->
            if (remember && !isIncognito) {
                SitePermissionManager.setState(this, rawOrigin, SitePermissionDatabase.TYPE_CAMERA, SitePermissionDatabase.STATE_ALLOW)
                SitePermissionManager.setState(this, rawOrigin, SitePermissionDatabase.TYPE_MIC, SitePermissionDatabase.STATE_ALLOW)
            }
            request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE, PermissionRequest.RESOURCE_AUDIO_CAPTURE))
        },
        onDeny = { remember ->
            if (remember && !isIncognito) {
                SitePermissionManager.setState(this, rawOrigin, SitePermissionDatabase.TYPE_CAMERA, SitePermissionDatabase.STATE_DENY)
                SitePermissionManager.setState(this, rawOrigin, SitePermissionDatabase.TYPE_MIC, SitePermissionDatabase.STATE_DENY)
            }
            request.deny()
        }
    )
}

private fun MainActivity.isSystemLocationGranted(): Boolean =
    isSystemPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
        isSystemPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)

private fun MainActivity.grantWebLocation(origin: String, callback: GeolocationCallback) {
    if (isSystemLocationGranted()) {
        callback.invoke(origin, true, false)
    } else {
        pendingWebGeoOrigin = origin
        pendingWebGeoCallback = callback
        webLocationPermissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }
}

internal fun MainActivity.onWebGeolocationRequest(origin: String, callback: GeolocationCallback) {
    val isIncognito = tabManager.activeTab?.isIncognito == true
    val rawOrigin = originFromString(origin)

    if (!isIncognito) {
        when (SitePermissionManager.getState(this, rawOrigin, SitePermissionDatabase.TYPE_LOCATION)) {
            SitePermissionDatabase.STATE_ALLOW -> { grantWebLocation(origin, callback); return }
            SitePermissionDatabase.STATE_DENY -> { callback.invoke(origin, false, false); return }
            else -> {}
        }

        val globalDefault = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("site_perm_default_${SitePermissionDatabase.TYPE_LOCATION}", SitePermissionActivity.PREF_VALUE_ASK) ?: SitePermissionActivity.PREF_VALUE_ASK
        when (globalDefault) {
            SitePermissionActivity.PREF_VALUE_ALLOW -> { grantWebLocation(origin, callback); return }
            SitePermissionActivity.PREF_VALUE_DENY -> { callback.invoke(origin, false, false); return }
            else -> {}
        }
    }

    showWebLocationDialog(origin, callback)
}

internal fun MainActivity.showWebLocationDialog(origin: String, callback: GeolocationCallback) {
    val isIncognito = tabManager.activeTab?.isIncognito == true
    val rawOrigin = originFromString(origin)
    val displayOrigin = rawOrigin.ifEmpty { getString(R.string.location_web_request_origin_unknown) }
    showWebPermissionDialog(
        title = getString(R.string.location_web_request_title),
        message = getString(R.string.location_web_request_message, displayOrigin),
        isIncognito = isIncognito,
        onAllow = { remember ->
            if (remember && !isIncognito) SitePermissionManager.setState(this, rawOrigin, SitePermissionDatabase.TYPE_LOCATION, SitePermissionDatabase.STATE_ALLOW)
            grantWebLocation(origin, callback)
        },
        onDeny = { remember ->
            if (remember && !isIncognito) SitePermissionManager.setState(this, rawOrigin, SitePermissionDatabase.TYPE_LOCATION, SitePermissionDatabase.STATE_DENY)
            callback.invoke(origin, false, false)
        }
    )
}
