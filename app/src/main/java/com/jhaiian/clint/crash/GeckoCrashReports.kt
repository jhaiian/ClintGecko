package com.jhaiian.clint.crash

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.jhaiian.clint.browser.webview.ClintGecko
import org.json.JSONObject
import org.mozilla.geckoview.GeckoRuntime
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GeckoCrashReports {

    const val TAG = "ClintGeckoCrash"
    private const val LOG_CHUNK = 3500

    private val ALLOWED_KEYS = setOf(
        "ProcessType", "RemoteType", "CrashTime", "StartupTime", "UptimeTS", "SecondsSinceLastCrash",
        "Version", "BuildID", "ApplicationBuildID", "ReleaseChannel", "ProductName",
        "MozCrashReason", "CrashType", "Throttleable", "Notes", "OOMAllocationSize",
        "AvailablePhysicalMemory", "AvailableVirtualMemory", "AvailablePageFile", "TotalPhysicalMemory",
        "SystemMemoryUsePercentage", "IsGarbageCollecting", "GPUProcessStatus", "ShutdownProgress",
        "AsyncShutdownTimeout", "EventLoopNestingLevel", "ContentSandboxCapabilities"
    )

    fun onContentProcessGone(context: Context, killed: Boolean) {
        val kind = if (killed) "killed by the system" else "crashed"
        Log.e(TAG, "GeckoView content process $kind")
        if (!killed) return
        val appContext = context.applicationContext
        ClintGecko.io.execute {
            runCatching {
                val body = buildString {
                    appendReportHeader(appContext, "CONTENT PROCESS KILLED")
                    appendLine("Crash Type    : Content process killed by Android (usually low memory)")
                    appendLine("Minidump      : none (the process was killed, not crashed)")
                    appendLine()
                    appendLine("--- MEMORY STATE ---")
                    appendMemoryState(appContext)
                }
                CrashHandler.saveReport(appContext, body)
                Log.e(TAG, body)
            }
        }
    }

    fun handleNativeCrash(
        context: Context,
        dumpPath: String?,
        extrasPath: String?,
        visibility: String?,
        processType: String?,
        remoteType: String?
    ) {
        val annotations = readAnnotations(extrasPath)
        val dump = dumpPath?.let { File(it) }?.takeIf { it.isFile }
        val fatal = visibility == GeckoRuntime.CRASHED_PROCESS_VISIBILITY_MAIN
        val reason = annotations?.optString("MozCrashReason")?.takeIf { it.isNotEmpty() }
        val javaStack = annotations?.optString("JavaStackTrace")?.takeIf { it.isNotEmpty() }

        val summary = "GeckoView ${if (fatal) "FATAL" else "child"} crash: process=${processType ?: "?"} " +
            "remote=${remoteType ?: "-"} visibility=${visibility ?: "?"} reason=${reason ?: "unknown"}"
        Log.e(TAG, summary)
        javaStack?.let { logLong("JavaStackTrace:\n$it") }

        val body = buildString {
            appendReportHeader(context, "ENGINE CRASH")
            appendLine("Crash Type    : ${if (fatal) "Fatal (main process)" else "Child process"}")
            appendLine("Visibility    : ${visibility ?: "unknown"}")
            appendLine("Process Type  : ${processType ?: "unknown"}")
            appendLine("Remote Type   : ${remoteType?.takeIf { it.isNotEmpty() } ?: "-"}")
            appendLine("Crash Reason  : ${reason ?: "unknown"}")
            appendLine("Minidump      : ${dump?.let { "${it.nameWithoutExtension}.dmp (${it.length() / 1024} KB)" } ?: "none"}")
            appendLine()
            appendLine("--- GECKO ANNOTATIONS ---")
            if (annotations == null) {
                appendLine("unavailable")
            } else {
                annotations.keys().asSequence().filter { it in ALLOWED_KEYS || it.startsWith("Android_") }
                    .sorted()
                    .forEach { key ->
                        val value = annotations.opt(key)?.toString().orEmpty()
                        if (value.isNotEmpty()) appendLine("${key.padEnd(28)}: $value")
                    }
            }
            if (javaStack != null) {
                appendLine()
                appendLine("--- JAVA STACK TRACE ---")
                appendLine(javaStack)
            }
            appendLine("===================")
        }

        CrashHandler.saveReport(context, body, dump)
        runCatching { dump?.delete() }
        runCatching { extrasPath?.let { File(it).delete() } }
    }

    private fun readAnnotations(extrasPath: String?): JSONObject? {
        if (extrasPath.isNullOrEmpty()) return null
        return runCatching { JSONObject(File(extrasPath).readText()) }.getOrNull()
    }

    private fun StringBuilder.appendReportHeader(context: Context, title: String) {
        appendLine("============================")
        appendLine("CLINT GECKO $title REPORT")
        appendLine("============================")
        appendLine("Time          : ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        appendLine()
        CrashHandler.buildDeviceInfo(context).lines().filter { it.isNotBlank() }.forEach { appendLine(it) }
        appendLine()
    }

    private fun StringBuilder.appendMemoryState(context: Context) {
        val info = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.getMemoryInfo(info)
        val runtime = Runtime.getRuntime()
        appendLine("Device RAM    : ${info.availMem / MB} MB free of ${info.totalMem / MB} MB")
        appendLine("Low Threshold : ${info.threshold / MB} MB")
        appendLine("Low Memory    : ${info.lowMemory}")
        appendLine("App Heap      : ${(runtime.totalMemory() - runtime.freeMemory()) / MB} MB used of ${runtime.maxMemory() / MB} MB max")
        appendLine("===================")
    }

    private fun logLong(text: String) {
        var start = 0
        while (start < text.length) {
            val end = minOf(start + LOG_CHUNK, text.length)
            Log.e(TAG, text.substring(start, end))
            start = end
        }
    }

    private const val MB = 1024L * 1024L
}
