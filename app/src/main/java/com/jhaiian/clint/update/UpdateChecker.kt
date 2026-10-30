package com.jhaiian.clint.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.ui.OverlayHostActivity
import com.jhaiian.clint.ui.theme.ClintComposeTheme
import com.jhaiian.clint.util.formatFileSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile

object UpdateChecker {

    private const val STABLE_URL =
        "https://raw.githubusercontent.com/jhaiian/ClintGecko/main/Update/Stable.json"
    private const val BETA_URL =
        "https://raw.githubusercontent.com/jhaiian/ClintGecko/main/Update/Beta.json"

    private const val PREFS_NAME = "update_prefs"
    private const val KEY_SKIPPED_VERSION_CODE = "skipped_version_code"
    private const val KEY_CACHED_APK_VERSION_CODE = "cached_apk_version_code"
    private const val ARCH_UNIVERSAL = "universal"

    private const val PROGRESS_UI_THROTTLE_BYTES = 65536L
    private const val SPEED_SAMPLE_INTERVAL_MS = 400L

    private val client = OkHttpClient()

    private fun scopeFor(activity: Activity): CoroutineScope =
        (activity as? LifecycleOwner)?.lifecycleScope ?: CoroutineScope(Dispatchers.Main.immediate)

    private fun mountFlow(activity: Activity): UpdateFlowState {
        val host = activity as? OverlayHostActivity
            ?: return UpdateFlowState(hideStatusBar = false, hideSystemNavigation = false)
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val theme = prefs.getString("app_theme", "system") ?: "system"
        val state = UpdateFlowState(hideStatusBar = prefs.getBoolean("hide_status_bar", false), hideSystemNavigation = prefs.getBoolean("hide_system_navigation", false))

        val dismiss: () -> Unit = {
            state.step = UpdateFlowStep.None
            host.overlayContent = null
        }

        host.overlayContent = {
            ClintComposeTheme(theme = theme) {
                UpdateFlowHost(
                    state = state,
                    onDismiss = dismiss,
                    onSkip = { versionCode ->
                        activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit().putLong(KEY_SKIPPED_VERSION_CODE, versionCode).apply()
                        dismiss()
                    },
                    onDownload = { url, versionCode -> startDownload(activity, url, versionCode, state, dismiss) },
                    onViewGithub = {
                        activity.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/jhaiian/ClintGecko/releases"))
                        )
                        dismiss()
                    },
                    onCancelDownload = { state.downloadJob?.cancel() }
                )
            }
        }
        return state
    }

    fun check(activity: Activity, isBeta: Boolean, silent: Boolean) {
        scopeFor(activity).launch {
            try {
                val (json, isSelectedBeta) = withContext(Dispatchers.IO) {
                    val stableJson = fetchJson(STABLE_URL)
                    val betaJson = if (isBeta) fetchJson(BETA_URL) else null
                    when {
                        betaJson == null -> Pair(stableJson, false)
                        betaJson.getLong("versionCode") > stableJson.getLong("versionCode") ->
                            Pair(betaJson, true)
                        else -> Pair(stableJson, false)
                    }
                }

                val remoteVersion = json.getString("version")
                val remoteVersionCode = json.getLong("versionCode")
                val changelog = json.getString("changelog")
                val downloads = json.getJSONObject("downloads")

                val (installedArch, isUniversalInstall) = withContext(Dispatchers.IO) {
                    getInstalledAppArch(activity)
                }
                val downloadUrl = resolveDownloadUrl(downloads, installedArch, isUniversalInstall)

                val currentVersionCode = PackageInfoCompat.getLongVersionCode(
                    activity.packageManager.getPackageInfo(activity.packageName, 0)
                )

                val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val skippedVersionCode = prefs.getLong(KEY_SKIPPED_VERSION_CODE, -1L)

                withContext(Dispatchers.IO) { cleanStaleApk(activity, currentVersionCode) }

                val hasUpdate = remoteVersionCode > currentVersionCode
                val isSkipped = silent && remoteVersionCode == skippedVersionCode

                if (hasUpdate && !isSkipped) {
                    val state = mountFlow(activity)
                    state.step = UpdateFlowStep.Available(
                        version = remoteVersion,
                        versionCode = remoteVersionCode,
                        changelog = changelog.trim(),
                        downloadUrl = downloadUrl,
                        isBeta = isSelectedBeta
                    )
                } else if (!silent) {
                    mountFlow(activity).step = UpdateFlowStep.NoUpdate
                }
            } catch (_: Throwable) {
                if (!silent) mountFlow(activity).step = UpdateFlowStep.CheckFailed
            }
        }
    }

    private fun fetchJson(url: String): JSONObject {
        val request = Request.Builder().url(url).build()
        val body = client.newCall(request).execute().body.string()
        return JSONObject(body)
    }

    private fun resolveDownloadUrl(downloads: JSONObject, arch: String, isUniversal: Boolean): String? {
        return if (isUniversal) {
            downloads.optString(ARCH_UNIVERSAL).takeIf { it.isNotEmpty() }
                ?: downloads.optString(arch).takeIf { it.isNotEmpty() }
        } else {
            downloads.optString(arch).takeIf { it.isNotEmpty() }
                ?: downloads.optString(ARCH_UNIVERSAL).takeIf { it.isNotEmpty() }
        }
    }

    private fun getInstalledAppArch(context: Context): Pair<String, Boolean> {
        val appInfo = context.applicationInfo
        val apkPaths = mutableListOf(appInfo.sourceDir)
        appInfo.splitSourceDirs?.let { apkPaths.addAll(it) }

        val foundAbis = mutableSetOf<String>()
        for (path in apkPaths) {
            if (path.isNullOrEmpty()) continue
            try {
                ZipFile(path).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val name = entries.nextElement().name
                        if (name.startsWith("lib/") && name.endsWith(".so")) {
                            val abi = name.removePrefix("lib/").substringBefore('/')
                            if (abi.isNotEmpty()) foundAbis.add(abi)
                        }
                    }
                }
            } catch (_: Exception) {

            }
        }

        val isUniversal = foundAbis.size > 1
        val arch = when {
            isUniversal || foundAbis.isEmpty() -> ARCH_UNIVERSAL
            foundAbis.contains("arm64-v8a") -> "arm64-v8a"
            foundAbis.contains("armeabi-v7a") -> "armeabi-v7a"
            foundAbis.contains("x86_64") -> "x86_64"
            else -> ARCH_UNIVERSAL
        }
        return Pair(arch, isUniversal)
    }

    private fun cleanStaleApk(activity: Activity, currentVersionCode: Long) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedVersionCode = prefs.getLong(KEY_CACHED_APK_VERSION_CODE, -1L)
        if (cachedVersionCode <= currentVersionCode) {
            val apkFile = File(activity.cacheDir, "updates/update.apk")
            if (apkFile.exists()) apkFile.delete()
            prefs.edit().remove(KEY_CACHED_APK_VERSION_CODE).apply()
        }
    }

    private fun startDownload(
        activity: Activity,
        downloadUrl: String,
        remoteVersionCode: Long,
        state: UpdateFlowState,
        dismiss: () -> Unit
    ) {
        val apkFile = File(activity.cacheDir, "updates/update.apk").also {
            it.parentFile?.mkdirs()
        }

        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedVersionCode = prefs.getLong(KEY_CACHED_APK_VERSION_CODE, -1L)

        if (apkFile.exists() && apkFile.length() > 0 && cachedVersionCode == remoteVersionCode) {
            dismiss()
            installApk(activity, apkFile)
            return
        }

        apkFile.delete()

        state.download.statusText = activity.getString(R.string.update_download_preparing)
        state.download.isIndeterminate = true
        state.download.progressFraction = 0f
        state.download.sizeText = ""
        state.download.speedText = ""
        state.step = UpdateFlowStep.Downloading

        state.downloadJob = scopeFor(activity).launch {
            try {
                withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(downloadUrl).build()
                    val call = client.newCall(request)
                    currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }

                    call.execute().use { response ->
                        val body = response.body
                        val contentLength = body.contentLength()

                        withContext(Dispatchers.Main) {
                            state.download.statusText = activity.getString(R.string.update_download_in_progress)
                            state.download.isIndeterminate = contentLength <= 0L
                        }

                        var downloaded = 0L
                        var lastUiBytes = 0L
                        var lastSpeedBytes = 0L
                        var lastSpeedTime = System.currentTimeMillis()
                        var speedBps = 0L

                        body.byteStream().use { input ->
                            apkFile.outputStream().use { output ->
                                val buffer = ByteArray(65536)
                                var bytes: Int
                                while (input.read(buffer).also { bytes = it } != -1) {
                                    output.write(buffer, 0, bytes)
                                    downloaded += bytes
                                    val isLastChunk = contentLength in 1..downloaded
                                    if (downloaded - lastUiBytes >= PROGRESS_UI_THROTTLE_BYTES || isLastChunk) {
                                        lastUiBytes = downloaded
                                        val now = System.currentTimeMillis()
                                        val elapsed = now - lastSpeedTime
                                        if (elapsed >= SPEED_SAMPLE_INTERVAL_MS || isLastChunk) {
                                            val delta = downloaded - lastSpeedBytes
                                            speedBps = if (elapsed > 0) delta * 1000L / elapsed else speedBps
                                            lastSpeedBytes = downloaded
                                            lastSpeedTime = now
                                        }
                                        updateDownloadProgress(activity, state.download, downloaded, contentLength, speedBps)
                                    }
                                }
                            }
                        }
                    }
                }

                prefs.edit().putLong(KEY_CACHED_APK_VERSION_CODE, remoteVersionCode).apply()
                dismiss()
                installApk(activity, apkFile)
            } catch (_: Exception) {
                apkFile.delete()
                dismiss()
            }
        }
    }

    private suspend fun updateDownloadProgress(
        activity: Activity,
        progress: DownloadProgressState,
        downloaded: Long,
        contentLength: Long,
        speedBps: Long
    ) = withContext(Dispatchers.Main) {
        if (contentLength > 0) {
            val pct = (downloaded * 100 / contentLength).toInt()
            progress.progressFraction = pct / 100f
            progress.sizeText = activity.getString(
                R.string.update_download_progress_size_known,
                formatFileSize(downloaded), formatFileSize(contentLength), pct
            )
        } else {
            progress.sizeText = activity.getString(
                R.string.update_download_progress_size_unknown, formatFileSize(downloaded)
            )
        }
        progress.speedText = if (speedBps > 0) activity.getString(R.string.download_speed_only, formatFileSize(speedBps)) else ""
    }

    private fun installApk(activity: Activity, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            activity, "${activity.packageName}.fileprovider", apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        activity.startActivity(intent)
    }

    private fun extractLatestChangelog(raw: String): String {
        val lines = raw.split("\n")
        val result = mutableListOf<String>()
        var sectionPrefix: String? = null
        for (line in lines) {
            val trimmed = line.trimStart()
            if (sectionPrefix == null) {
                sectionPrefix = when {
                    trimmed.startsWith("## ") -> "## "
                    trimmed.startsWith("# ")  -> "# "
                    else -> null
                }
                if (sectionPrefix != null) result.add(line)
            } else {
                if (trimmed.startsWith(sectionPrefix)) break
                result.add(line)
            }
        }
        return result.joinToString("\n").trim()
    }
}
