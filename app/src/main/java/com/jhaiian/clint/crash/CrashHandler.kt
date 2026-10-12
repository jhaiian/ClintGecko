package com.jhaiian.clint.crash

import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    companion object {
        const val CRASH_DIR = "crash_reports"
        const val MAX_REPORTS = 20
        val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val FILE_DATE_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

        fun install(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context.applicationContext))
        }

        fun getCrashDir(context: Context): File {
            return File(context.filesDir, CRASH_DIR).also { it.mkdirs() }
        }

        fun getCrashFiles(context: Context): List<File> {
            return getCrashDir(context)
                .listFiles { f -> f.name.endsWith(".txt") }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        }

        fun deleteOldReports(context: Context) {
            val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
            val cutoff = System.currentTimeMillis() - sevenDaysMs
            getCrashFiles(context).forEach { file ->
                if (file.lastModified() < cutoff) file.delete()
            }
        }

        fun clearAllReports(context: Context) {
            getCrashFiles(context).forEach { it.delete() }
        }

        fun buildDeviceInfo(context: Context): String {
            return runCatching {
                val pInfo = runCatching {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }.getOrNull()
                val version = pInfo?.versionName ?: "unknown"
                val build = pInfo?.let { PackageInfoCompat.getLongVersionCode(it) } ?: 0L
                val arch = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
                val webViewInfo = "GeckoView v${com.jhaiian.clint.browser.webview.ClintGecko.geckoVersion}"
                buildString {
                    appendLine("App Version   : $version (build $build)")
                    appendLine("Architecture  : $arch")
                    appendLine("Device        : ${Build.MANUFACTURER} ${Build.MODEL}")
                    appendLine("Android       : ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    appendLine("Brand         : ${Build.BRAND}")
                    appendLine("Product       : ${Build.PRODUCT}")
                    appendLine("Engine        : $webViewInfo")
                }
            }.getOrElse { "Device info unavailable" }
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()

            val timestamp = DATE_FORMAT.format(Date())
            val fileTimestamp = FILE_DATE_FORMAT.format(Date())

            val report = buildString {
                appendLine("============================")
                appendLine("CLINT GECKO CRASH REPORT")
                appendLine("============================")
                appendLine("Time          : $timestamp")
                appendLine()
                buildDeviceInfo(context).lines().forEach { appendLine(it) }
                appendLine()
                appendLine("Thread        : ${thread.name}")
                appendLine()
                appendLine("--- STACK TRACE ---")
                appendLine(stackTrace)
                appendLine("===================")
            }

            val dir = getCrashDir(context)
            val file = File(dir, "crash_$fileTimestamp.txt")
            file.writeText(report)

            val files = getCrashFiles(context)
            if (files.size > MAX_REPORTS) {
                files.drop(MAX_REPORTS).forEach { it.delete() }
            }
        } catch (e: Exception) {
        }

        defaultHandler?.uncaughtException(thread, throwable)
    }
}
