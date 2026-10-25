package com.jhaiian.clint.downloads

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import com.jhaiian.clint.util.formatFileSize
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

object DownloadLog {

    enum class Level(val tag: String) { DEBUG("DEBUG"), INFO("INFO "), WARN("WARN "), ERROR("ERROR") }

    const val GLOBAL_ID = -1
    private const val DIR = "download_logs"
    private const val GLOBAL_FILE = "global.log"
    private const val MAX_GLOBAL_BYTES = 4L * 1024L * 1024L
    private const val MAX_ITEM_BYTES = 2L * 1024L * 1024L
    private const val VIEW_LINE_LIMIT = 5000
    private const val BATCH_LIMIT = 400
    private const val CONTINUATION = "                                        "

    private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @Volatile
    var enabled: Boolean = false
        private set

    @Volatile
    private var appContext: Context? = null
    private var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private sealed interface Message
    private class Line(val id: Int, val text: String, val toGlobal: Boolean) : Message
    private class Flush(val done: CompletableDeferred<Unit>) : Message
    private class Clear(val id: Int, val done: CompletableDeferred<Unit>?) : Message

    private val channel = Channel<Message>(Channel.UNLIMITED)

    fun init(context: Context) {
        if (appContext != null) return
        val ctx = context.applicationContext
        appContext = ctx
        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        enabled = prefs.getBoolean(DownloadSettingsKeys.PREF_DOWNLOAD_LOG, DownloadSettingsKeys.DEFAULT_DOWNLOAD_LOG)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == DownloadSettingsKeys.PREF_DOWNLOAD_LOG) {
                setEnabled(p.getBoolean(DownloadSettingsKeys.PREF_DOWNLOAD_LOG, DownloadSettingsKeys.DEFAULT_DOWNLOAD_LOG))
            }
        }
        prefListener = listener
        prefs.registerOnSharedPreferenceChangeListener(listener)
        ClintDownloadManager.applicationScope.launch { consume(ctx) }
    }

    private fun setEnabled(value: Boolean) {
        if (value == enabled) return
        if (value) {
            enabled = true
            write(GLOBAL_ID, Level.INFO, "LOGGING", true, "Download logging turned on")
        } else {
            write(GLOBAL_ID, Level.INFO, "LOGGING", true, "Download logging turned off")
            enabled = false
        }
    }

    inline fun d(id: Int, category: String, message: () -> String) {
        if (enabled) write(id, Level.DEBUG, category, false, message())
    }

    inline fun i(id: Int, category: String, message: () -> String) {
        if (enabled) write(id, Level.INFO, category, true, message())
    }

    inline fun w(id: Int, category: String, message: () -> String) {
        if (enabled) write(id, Level.WARN, category, true, message())
    }

    inline fun e(id: Int, category: String, message: () -> String) {
        if (enabled) write(id, Level.ERROR, category, true, message())
    }

    inline fun section(id: Int, title: String, details: () -> List<Pair<String, Any?>>) {
        if (!enabled) return
        val rows = details().filter { it.second != null && it.second.toString().isNotBlank() }
        val width = rows.maxOfOrNull { it.first.length } ?: 0
        val body = buildString {
            append("──── ").append(title).append(" ────")
            rows.forEach { (key, value) -> append('\n').append(key.padEnd(width)).append(" : ").append(value) }
        }
        write(id, Level.INFO, "SUMMARY", true, body)
    }

    fun error(id: Int, category: String, message: String, throwable: Throwable) {
        if (!enabled) return
        val trace = throwable.stackTraceToString().lineSequence().take(14).joinToString("\n")
        write(id, Level.ERROR, category, true, "$message\n$trace")
    }

    @PublishedApi
    internal fun write(id: Int, level: Level, category: String, toGlobal: Boolean, message: String) {
        if (appContext == null) return
        val stamp = LocalDateTime.now().format(timeFormat)
        val owner = if (id == GLOBAL_ID) "#--" else "#$id"
        val text = buildString {
            append(stamp).append("  ").append(level.tag).append("  ").append(owner.padEnd(5)).append("  ")
                .append(category.padEnd(9)).append("  ")
            message.lineSequence().forEachIndexed { index, line ->
                if (index > 0) append('\n').append(CONTINUATION)
                append(line)
            }
        }
        channel.trySend(Line(id, text, toGlobal || id == GLOBAL_ID))
    }

    fun deleteItemLog(id: Int) {
        if (appContext == null) return
        channel.trySend(Clear(id, null))
    }

    suspend fun clear(id: Int) {
        if (appContext == null) return
        val done = CompletableDeferred<Unit>()
        channel.send(Clear(id, done))
        done.await()
    }

    suspend fun flush() {
        if (appContext == null) return
        val done = CompletableDeferred<Unit>()
        channel.send(Flush(done))
        done.await()
    }

    fun hasLog(context: Context, id: Int): Boolean =
        files(context, id).any { it.exists() && it.length() > 0L }

    suspend fun readLines(context: Context, id: Int): List<String> {
        flush()
        return withContext(Dispatchers.IO) {
            val lines = ArrayDeque<String>()
            files(context, id).filter { it.exists() }.forEach { file ->
                runCatching {
                    file.forEachLine { line ->
                        lines.addLast(line)
                        if (lines.size > VIEW_LINE_LIMIT) lines.removeFirst()
                    }
                }
            }
            lines.toList()
        }
    }

    suspend fun export(context: Context, id: Int): File? {
        flush()
        return withContext(Dispatchers.IO) {
            val sources = files(context, id).filter { it.exists() && it.length() > 0L }
            if (sources.isEmpty()) return@withContext null
            val dir = File(context.filesDir, DIR).apply { mkdirs() }
            val target = File(dir, if (id == GLOBAL_ID) "clint-download-log.txt" else "clint-download-$id-log.txt")
            runCatching {
                target.outputStream().use { out ->
                    sources.forEach { source -> source.inputStream().use { it.copyTo(out) } }
                }
                target
            }.getOrNull()
        }
    }

    fun formatTime(millis: Long): String? =
        if (millis <= 0L) null
        else Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime().format(dateFormat)

    fun formatBytes(bytes: Long): String = if (bytes < 0L) "unknown" else "${formatFileSize(bytes)} ($bytes B)"

    private fun autoOrValue(value: Int): Any = if (DownloadAutoTuner.isAuto(value)) "auto" else value

    fun describe(item: DownloadItem): List<Pair<String, Any?>> = listOf(
        "File" to item.filename,
        "Download ID" to item.id,
        "URL" to item.url.takeIf { !item.isStream },
        "Page" to item.streamPageUrl.ifBlank { null },
        "Referer" to item.referer.ifBlank { null },
        "Cookies" to if (item.cookies.isNotBlank()) "present (${item.cookies.length} chars, not logged)" else null,
        "Size" to if (item.totalBytes > 0L) formatBytes(item.totalBytes) else null,
        "Downloaded" to if (item.bytesDownloaded > 0L) formatBytes(item.bytesDownloaded) else null,
        "Resumable" to item.resumable,
        "Split parts" to autoOrValue(item.splitParts).takeIf { !item.isStream },
        "Connections" to autoOrValue(item.multithreadingParts).takeIf { !item.isStream },
        "Speed limit" to if (item.speedLimitBytesPerSec > 0L) "${formatFileSize(item.speedLimitBytesPerSec)}/s" else "none",
        "Wi-Fi only" to item.unmeteredOnly,
        "Retry" to item.retryEnabled,
        "Location" to (item.customLocationUri?.let { "${item.locationMode} → $it" } ?: item.locationMode),
        "Categorize" to item.categorizeEnabled,
        "Saved to" to (item.file?.absolutePath ?: item.contentUri),
        "Scheduled" to formatTime(item.scheduledStartAtMillis),
        "Stream format" to item.streamFormat.ifBlank { null },
        "Video URL" to item.streamVideoUrl.ifBlank { null },
        "Audio URL" to item.streamAudioUrl,
        "Subtitle URL" to item.streamSubtitleUrl,
        "Resolution" to if (item.streamVideoWidth != null && item.streamVideoHeight != null) "${item.streamVideoWidth}×${item.streamVideoHeight}" else null,
        "Video bitrate" to item.streamVideoBandwidth?.let { "${it / 1000} kbps" },
        "Audio bitrate" to item.streamAudioBandwidth?.let { "${it / 1000} kbps" },
        "Audio track" to listOfNotNull(item.streamAudioLanguage, item.streamAudioLabel).joinToString(" ").ifBlank { null },
        "Live" to item.streamIsLive.takeIf { item.isStream },
        "Segment workers" to autoOrValue(item.streamConcurrentSegments).takeIf { item.isStream },
        "Convert TS" to item.streamConvertTsToMp4.takeIf { item.isStream }
    )

    private val progressMarks = ConcurrentHashMap<Int, Int>()

    private fun statusLabel(status: DownloadStatus): String = status.name.lowercase().replace('_', ' ')

    private fun progressText(item: DownloadItem): String = buildString {
        if (item.totalBytes > 0L) append("${formatFileSize(item.bytesDownloaded)} of ${formatFileSize(item.totalBytes)}")
        else append(formatFileSize(item.bytesDownloaded))
        if (item.segmentsTotal > 0) append(" \u00B7 segments ${item.segmentsCompleted}/${item.segmentsTotal}")
        if (item.partProgress.size > 1) append(" \u00B7 parts ${item.partProgress.count { it >= 1f }}/${item.partProgress.size} done")
    }

    fun observe(before: DownloadItem?, after: DownloadItem) {
        if (!enabled || before == null) return
        val id = after.id
        if (before.status != after.status) {
            val detail = when (after.status) {
                DownloadStatus.RETRYING -> "attempt ${after.retryAttempt}, next try in ${after.retryDelaySec}s \u00B7 ${after.errorMessage ?: "no error message"}"
                DownloadStatus.FAILED -> after.errorMessage ?: "unknown error"
                DownloadStatus.PAUSED -> listOfNotNull(
                    "waiting for network".takeIf { after.waitingForNetwork },
                    "waiting for Wi-Fi".takeIf { after.waitingForUnmetered },
                    "waiting for schedule window".takeIf { after.waitingForSchedule },
                    formatTime(after.scheduledStartAtMillis)?.let { "scheduled for $it" }.takeIf { after.waitingForCustomSchedule }
                ).joinToString(", ").ifBlank { "paused" } + " \u00B7 " + progressText(after)
                else -> progressText(after)
            }
            val level = when (after.status) {
                DownloadStatus.FAILED -> Level.ERROR
                DownloadStatus.RETRYING -> Level.WARN
                else -> Level.INFO
            }
            write(id, level, "STATUS", true, "${statusLabel(before.status)} \u2192 ${statusLabel(after.status)} \u00B7 $detail")
            if (after.status == DownloadStatus.COMPLETE) {
                progressMarks.remove(id)
                val activeMs = after.activeElapsedMs
                section(id, "Download finished") {
                    listOf(
                        "File" to after.filename,
                        "Size" to formatBytes(after.bytesDownloaded),
                        "Active time" to if (activeMs > 0L) formatElapsed(activeMs / 1000L) else null,
                        "Average speed" to if (activeMs > 0L) "${formatFileSize(after.bytesDownloaded * 1000L / activeMs)}/s" else null,
                        "Started" to formatTime(after.startedAt),
                        "Finished" to formatTime(after.completedAt),
                        "Segments" to if (after.segmentsTotal > 0) "${after.segmentsCompleted}/${after.segmentsTotal}" else null,
                        "Saved to" to (after.file?.absolutePath ?: after.contentUri)
                    )
                }
            }
        }
        if (before.streamStage != after.streamStage && after.streamStage != StreamStage.NONE) {
            write(id, Level.INFO, "STAGE", true, "Stream stage: ${after.streamStage.name.lowercase()}")
        }
        if (before.filename != after.filename) {
            write(id, Level.INFO, "FILE", true, "File name: ${before.filename} \u2192 ${after.filename}")
        }
        if (before.url != after.url) {
            write(id, Level.INFO, "LINK", true, "Download link updated: ${after.url}")
        }
        if (before.speedLimitBytesPerSec != after.speedLimitBytesPerSec || before.unmeteredOnly != after.unmeteredOnly || before.retryEnabled != after.retryEnabled) {
            write(
                id, Level.INFO, "SETTINGS", true,
                "Settings changed \u00B7 speed limit " + (if (after.speedLimitBytesPerSec > 0L) "${formatFileSize(after.speedLimitBytesPerSec)}/s" else "none") +
                    " \u00B7 Wi-Fi only ${after.unmeteredOnly} \u00B7 retry ${after.retryEnabled}"
            )
        }
        if (after.status == DownloadStatus.DOWNLOADING) {
            val pct = when {
                after.segmentsTotal > 0 -> after.segmentsCompleted * 100 / after.segmentsTotal
                else -> after.progressPercent
            }
            if (pct >= 0) {
                val mark = pct / 10
                val last = progressMarks[id]
                if (last == null || mark > last) {
                    progressMarks[id] = mark
                    if (last != null || mark > 0) {
                        val activeMs = after.activeElapsedMs + if (after.activeStartedAt > 0L) System.currentTimeMillis() - after.activeStartedAt else 0L
                        val average = after.averageSpeedBytesPerSec()
                        val remaining = after.totalBytes - after.bytesDownloaded
                        write(
                            id, Level.INFO, "PROGRESS", true,
                            "$pct% \u00B7 ${progressText(after)} \u00B7 now ${formatFileSize(after.speedBytesPerSec)}/s \u00B7 avg ${formatFileSize(average)}/s" +
                                (if (activeMs >= 1000L) " \u00B7 active ${formatElapsed(activeMs / 1000L)}" else "") +
                                (if (average > 0L && after.totalBytes > 0L && remaining > 0L) " \u00B7 ETA ${formatElapsed(remaining / average)}" else "")
                        )
                    }
                }
            }
        }
    }

    private fun files(context: Context, id: Int): List<File> {
        val dir = File(context.filesDir, DIR)
        val name = if (id == GLOBAL_ID) GLOBAL_FILE else "download_$id.log"
        return listOf(File(dir, "$name.1"), File(dir, name))
    }

    private suspend fun consume(context: Context) {
        val dir = File(context.filesDir, DIR)
        val globalFile = File(dir, GLOBAL_FILE)
        for (first in channel) {
            val pending = LinkedHashMap<File, StringBuilder>()
            var message: Message? = first
            var count = 0
            while (message != null) {
                when (val current = message) {
                    is Line -> {
                        if (current.toGlobal) pending.getOrPut(globalFile) { StringBuilder() }.append(current.text).append('\n')
                        if (current.id != GLOBAL_ID) {
                            pending.getOrPut(File(dir, "download_${current.id}.log")) { StringBuilder() }.append(current.text).append('\n')
                        }
                    }
                    is Flush -> {
                        writePending(dir, globalFile, pending)
                        current.done.complete(Unit)
                    }
                    is Clear -> {
                        writePending(dir, globalFile, pending)
                        val clearId = current.id
                        runCatching { files(context, clearId).forEach { it.delete() } }
                        current.done?.complete(Unit)
                    }
                }
                count++
                message = if (count < BATCH_LIMIT) channel.tryReceive().getOrNull() else null
            }
            writePending(dir, globalFile, pending)
        }
    }

    private fun writePending(dir: File, globalFile: File, pending: MutableMap<File, StringBuilder>) {
        if (pending.isEmpty()) return
        runCatching {
            dir.mkdirs()
            pending.forEach { (file, text) ->
                val limit = if (file == globalFile) MAX_GLOBAL_BYTES else MAX_ITEM_BYTES
                if (file.length() > limit) {
                    val rotated = File(file.parentFile, file.name + ".1")
                    rotated.delete()
                    file.renameTo(rotated)
                }
                file.appendText(text.toString())
            }
        }
        pending.clear()
    }
}
