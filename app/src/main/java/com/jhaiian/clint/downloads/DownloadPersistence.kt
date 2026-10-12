package com.jhaiian.clint.downloads

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

internal object DownloadPersistence {

    private const val LEGACY_PREFS_NAME = "clint_downloads_prefs"
    private const val LEGACY_KEY_DOWNLOADS = "saved_downloads"

    @Volatile private var db: DownloadDatabase? = null

    private fun db(context: Context): DownloadDatabase {
        return db ?: synchronized(this) {
            db ?: DownloadDatabase(context.applicationContext).also {
                db = it
                migrateLegacyPrefs(context, it.writableDatabase)
            }
        }
    }

    private fun migrateLegacyPrefs(context: Context, writable: SQLiteDatabase) {
        val prefs = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(LEGACY_KEY_DOWNLOADS, null) ?: return
        runCatching {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val status = obj.getString("status")
                if (status == DownloadStatus.DOWNLOADING.name) continue
                val values = ContentValues().apply {
                    put(DownloadDatabase.COL_ID, obj.getInt("id"))
                    put(DownloadDatabase.COL_URL, obj.getString("url"))
                    put(DownloadDatabase.COL_FILENAME, obj.getString("filename"))
                    put(DownloadDatabase.COL_USER_AGENT, obj.optString("userAgent", ""))
                    put(DownloadDatabase.COL_REFERER, obj.optString("referer", ""))
                    put(DownloadDatabase.COL_COOKIES, obj.optString("cookies", ""))
                    put(DownloadDatabase.COL_BYTES_DOWNLOADED, obj.optLong("bytesDownloaded", 0L))
                    put(DownloadDatabase.COL_TOTAL_BYTES, obj.optLong("totalBytes", -1L))
                    put(DownloadDatabase.COL_STATUS, status)
                    val filePath = obj.optString("filePath", "")
                    if (filePath.isNotEmpty()) put(DownloadDatabase.COL_FILE_PATH, filePath)
                    val errorMsg = obj.optString("errorMessage", "")
                    if (errorMsg.isNotEmpty()) put(DownloadDatabase.COL_ERROR_MESSAGE, errorMsg)
                    put(DownloadDatabase.COL_RESUMABLE, 0)
                }
                writable.insertWithOnConflict(
                    DownloadDatabase.TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE
                )
            }
        }
        prefs.edit().remove(LEGACY_KEY_DOWNLOADS).apply()
    }

    suspend fun loadDownloads(context: Context): List<DownloadItem> = withContext(Dispatchers.IO) {
        val cursor = db(context).readableDatabase.query(
            DownloadDatabase.TABLE,
            null,
            null, null, null, null,
            "${DownloadDatabase.COL_ID} DESC"
        )
        val loaded = mutableListOf<DownloadItem>()
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow(DownloadDatabase.COL_ID))
                val statusStr = it.getString(it.getColumnIndexOrThrow(DownloadDatabase.COL_STATUS))
                var status = runCatching { DownloadStatus.valueOf(statusStr) }
                    .getOrDefault(DownloadStatus.FAILED)
                if (status == DownloadStatus.COPYING_TEMP
                    || status == DownloadStatus.DELETING_TEMP
                    || status == DownloadStatus.CONNECTING
                    || status == DownloadStatus.RETRYING
                    || status == DownloadStatus.ALLOCATING
                    || status == DownloadStatus.DOWNLOADING
                ) status = DownloadStatus.FAILED
                val filePath = it.getString(it.getColumnIndexOrThrow(DownloadDatabase.COL_FILE_PATH))
                val resumableIdx = it.getColumnIndex(DownloadDatabase.COL_RESUMABLE)
                val resumable = if (resumableIdx >= 0) it.getInt(resumableIdx) != 0 else false
                val contentUriIdx = it.getColumnIndex(DownloadDatabase.COL_CONTENT_URI)
                val contentUri = if (contentUriIdx >= 0) it.getString(contentUriIdx) else null
                val retryEnabledIdx = it.getColumnIndex(DownloadDatabase.COL_RETRY_ENABLED)
                val retryEnabled = if (retryEnabledIdx >= 0) it.getInt(retryEnabledIdx) != 0 else true
                val unmeteredOnlyIdx = it.getColumnIndex(DownloadDatabase.COL_UNMETERED_ONLY)
                val unmeteredOnly = if (unmeteredOnlyIdx >= 0) it.getInt(unmeteredOnlyIdx) != 0 else false
                val splitPartsIdx = it.getColumnIndex(DownloadDatabase.COL_SPLIT_PARTS)
                val splitParts = if (splitPartsIdx >= 0) it.getInt(splitPartsIdx) else 32
                val multithreadingIdx = it.getColumnIndex(DownloadDatabase.COL_MULTITHREADING)
                val multithreadingParts = if (multithreadingIdx >= 0) it.getInt(multithreadingIdx) else 4
                val speedLimitIdx = it.getColumnIndex(DownloadDatabase.COL_SPEED_LIMIT_BPS)
                val speedLimitBytesPerSec = if (speedLimitIdx >= 0) it.getLong(speedLimitIdx) else 0L
                val locationModeIdx = it.getColumnIndex(DownloadDatabase.COL_LOCATION_MODE)
                val locationMode = if (locationModeIdx >= 0) it.getString(locationModeIdx) ?: "default" else "default"
                val customLocUriIdx = it.getColumnIndex(DownloadDatabase.COL_CUSTOM_LOC_URI)
                val customLocationUri = if (customLocUriIdx >= 0) it.getString(customLocUriIdx) else null
                val categorizeEnabledIdx = it.getColumnIndex(DownloadDatabase.COL_CATEGORIZE_ENABLED)
                val categorizeEnabled = if (categorizeEnabledIdx >= 0) it.getInt(categorizeEnabledIdx) != 0 else false
                val completedMaskIdx = it.getColumnIndex(DownloadDatabase.COL_COMPLETED_PARTS_MASK)
                val completedPartsMask = if (completedMaskIdx >= 0) it.getLong(completedMaskIdx) else 0L
                val partOffsetsIdx = it.getColumnIndex(DownloadDatabase.COL_PART_OFFSETS)
                val partOffsets = if (partOffsetsIdx >= 0) it.getString(partOffsetsIdx) ?: "" else ""
                val scheduledStartAtIdx = it.getColumnIndex(DownloadDatabase.COL_SCHEDULED_START_AT)
                val scheduledStartAtMillis = if (scheduledStartAtIdx >= 0) it.getLong(scheduledStartAtIdx) else 0L
                val waitingCustomScheduleIdx = it.getColumnIndex(DownloadDatabase.COL_WAITING_CUSTOM_SCHEDULE)
                val waitingForCustomSchedule = if (waitingCustomScheduleIdx >= 0) it.getInt(waitingCustomScheduleIdx) != 0 else false
                val item = DownloadItem(
                    id = id,
                    url = it.getString(it.getColumnIndexOrThrow(DownloadDatabase.COL_URL)),
                    filename = it.getString(it.getColumnIndexOrThrow(DownloadDatabase.COL_FILENAME)),
                    userAgent = it.getString(it.getColumnIndexOrThrow(DownloadDatabase.COL_USER_AGENT)),
                    referer = it.getString(it.getColumnIndexOrThrow(DownloadDatabase.COL_REFERER)),
                    cookies = it.getString(it.getColumnIndexOrThrow(DownloadDatabase.COL_COOKIES)),
                    bytesDownloaded = it.getLong(it.getColumnIndexOrThrow(DownloadDatabase.COL_BYTES_DOWNLOADED)),
                    totalBytes = it.getLong(it.getColumnIndexOrThrow(DownloadDatabase.COL_TOTAL_BYTES)),
                    status = status,
                    file = if (filePath != null) File(filePath) else null,
                    errorMessage = it.getString(it.getColumnIndexOrThrow(DownloadDatabase.COL_ERROR_MESSAGE)),
                    startedAt = it.getLong(it.getColumnIndexOrThrow(DownloadDatabase.COL_STARTED_AT)),
                    activeElapsedMs = run {
                        val idx = it.getColumnIndex(DownloadDatabase.COL_ACTIVE_ELAPSED_MS)
                        if (idx >= 0) it.getLong(idx) else 0L
                    },
                    completedAt = run {
                        val idx = it.getColumnIndex(DownloadDatabase.COL_COMPLETED_AT)
                        if (idx >= 0) it.getLong(idx) else 0L
                    },
                    resumable = resumable,
                    contentUri = contentUri,
                    retryEnabled = retryEnabled,
                    unmeteredOnly = unmeteredOnly,
                    splitParts = splitParts,
                    multithreadingParts = multithreadingParts,
                    speedLimitBytesPerSec = speedLimitBytesPerSec,
                    locationMode = locationMode,
                    customLocationUri = customLocationUri,
                    categorizeEnabled = categorizeEnabled,
                    completedPartsMask = completedPartsMask,
                    partOffsets = partOffsets,
                    scheduledStartAtMillis = scheduledStartAtMillis,
                    waitingForCustomSchedule = waitingForCustomSchedule
                )
                val streamJsonIdx = it.getColumnIndex(DownloadDatabase.COL_STREAM_JSON)
                val streamJson = if (streamJsonIdx >= 0) it.getString(streamJsonIdx) else null
                loaded.add(if (streamJson != null) applyStreamState(item, streamJson) else item)
            }
        }
        loaded
    }

    suspend fun persistDownload(context: Context, item: DownloadItem, removedIds: Set<Int>) {
        if (item.id in removedIds) return
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(DownloadDatabase.COL_ID, item.id)
                put(DownloadDatabase.COL_URL, item.url)
                put(DownloadDatabase.COL_FILENAME, item.filename)
                put(DownloadDatabase.COL_USER_AGENT, item.userAgent)
                put(DownloadDatabase.COL_REFERER, item.referer)
                put(DownloadDatabase.COL_COOKIES, item.cookies)
                put(DownloadDatabase.COL_BYTES_DOWNLOADED, item.bytesDownloaded)
                put(DownloadDatabase.COL_TOTAL_BYTES, item.totalBytes)
                put(DownloadDatabase.COL_STATUS, item.status.name)
                put(DownloadDatabase.COL_RESUMABLE, if (item.resumable) 1 else 0)
                if (item.file != null) put(DownloadDatabase.COL_FILE_PATH, item.file.absolutePath)
                else putNull(DownloadDatabase.COL_FILE_PATH)
                if (item.errorMessage != null) put(DownloadDatabase.COL_ERROR_MESSAGE, item.errorMessage)
                else putNull(DownloadDatabase.COL_ERROR_MESSAGE)
                put(DownloadDatabase.COL_STARTED_AT, item.startedAt)
                put(DownloadDatabase.COL_ACTIVE_ELAPSED_MS, item.activeElapsedMs)
                put(DownloadDatabase.COL_COMPLETED_AT, item.completedAt)
                if (item.contentUri != null) put(DownloadDatabase.COL_CONTENT_URI, item.contentUri)
                else putNull(DownloadDatabase.COL_CONTENT_URI)
                put(DownloadDatabase.COL_RETRY_ENABLED, if (item.retryEnabled) 1 else 0)
                put(DownloadDatabase.COL_UNMETERED_ONLY, if (item.unmeteredOnly) 1 else 0)
                put(DownloadDatabase.COL_SPLIT_PARTS, item.splitParts)
                put(DownloadDatabase.COL_MULTITHREADING, item.multithreadingParts)
                put(DownloadDatabase.COL_SPEED_LIMIT_BPS, item.speedLimitBytesPerSec)
                put(DownloadDatabase.COL_LOCATION_MODE, item.locationMode)
                if (item.customLocationUri != null) put(DownloadDatabase.COL_CUSTOM_LOC_URI, item.customLocationUri)
                else putNull(DownloadDatabase.COL_CUSTOM_LOC_URI)
                put(DownloadDatabase.COL_CATEGORIZE_ENABLED, if (item.categorizeEnabled) 1 else 0)
                put(DownloadDatabase.COL_COMPLETED_PARTS_MASK, item.completedPartsMask)
                put(DownloadDatabase.COL_PART_OFFSETS, item.partOffsets)
                put(DownloadDatabase.COL_SCHEDULED_START_AT, item.scheduledStartAtMillis)
                put(DownloadDatabase.COL_WAITING_CUSTOM_SCHEDULE, if (item.waitingForCustomSchedule) 1 else 0)
                if (item.isStream) put(DownloadDatabase.COL_STREAM_JSON, encodeStreamState(item))
                else putNull(DownloadDatabase.COL_STREAM_JSON)
            }
            db(context).writableDatabase.insertWithOnConflict(
                DownloadDatabase.TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE
            )
        }
    }

    private fun encodeStreamState(item: DownloadItem): String = JSONObject().apply {
        put("videoUrl", item.streamVideoUrl)
        putOpt("audioUrl", item.streamAudioUrl)
        putOpt("subtitleUrl", item.streamSubtitleUrl)
        put("format", item.streamFormat)
        put("pageUrl", item.streamPageUrl)
        putOpt("videoWidth", item.streamVideoWidth)
        putOpt("videoHeight", item.streamVideoHeight)
        putOpt("videoBandwidth", item.streamVideoBandwidth)
        putOpt("audioBandwidth", item.streamAudioBandwidth)
        put("primaryIsAudio", item.streamPrimaryIsAudio)
        put("noAudio", item.streamNoAudio)
        put("headers", JSONObject(item.streamHeaders as Map<*, *>))
        put("concurrentSegments", item.streamConcurrentSegments)
        put("isLive", item.streamIsLive)
        put("convertTsToMp4", item.streamConvertTsToMp4)
        putOpt("videoRepresentationId", item.streamVideoRepresentationId)
        putOpt("audioRepresentationId", item.streamAudioRepresentationId)
        put("segmentsCompleted", item.segmentsCompleted)
        put("segmentsTotal", item.segmentsTotal)
    }.toString()

    private fun JSONObject.optNullableString(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null

    private fun JSONObject.optNullableInt(key: String): Int? =
        if (has(key) && !isNull(key)) getInt(key) else null

    private fun JSONObject.optNullableLong(key: String): Long? =
        if (has(key) && !isNull(key)) getLong(key) else null

    private fun applyStreamState(item: DownloadItem, json: String): DownloadItem {
        val o = runCatching { JSONObject(json) }.getOrNull() ?: return item
        val headers = o.optJSONObject("headers")?.let { h ->
            h.keys().asSequence().associateWith { key -> h.optString(key) }
        } ?: emptyMap()
        return item.copy(
            isStream = true,
            streamVideoUrl = o.optString("videoUrl", ""),
            streamAudioUrl = o.optNullableString("audioUrl"),
            streamSubtitleUrl = o.optNullableString("subtitleUrl"),
            streamFormat = o.optString("format", ""),
            streamPageUrl = o.optString("pageUrl", ""),
            streamVideoWidth = o.optNullableInt("videoWidth"),
            streamVideoHeight = o.optNullableInt("videoHeight"),
            streamVideoBandwidth = o.optNullableLong("videoBandwidth"),
            streamAudioBandwidth = o.optNullableLong("audioBandwidth"),
            streamPrimaryIsAudio = o.optBoolean("primaryIsAudio", false),
            streamNoAudio = o.optBoolean("noAudio", false),
            streamHeaders = headers,
            streamConcurrentSegments = o.optInt("concurrentSegments", 6),
            streamIsLive = o.optBoolean("isLive", false),
            streamConvertTsToMp4 = o.optBoolean("convertTsToMp4", false),
            streamVideoRepresentationId = o.optNullableString("videoRepresentationId"),
            streamAudioRepresentationId = o.optNullableString("audioRepresentationId"),
            segmentsCompleted = o.optInt("segmentsCompleted", 0),
            segmentsTotal = o.optInt("segmentsTotal", 0)
        )
    }

    fun checkpointProgress(context: Context, id: Int, bytesDownloaded: Long, completedPartsMask: Long, partOffsets: String) {
        runCatching {
            val values = ContentValues().apply {
                put(DownloadDatabase.COL_BYTES_DOWNLOADED, bytesDownloaded)
                put(DownloadDatabase.COL_COMPLETED_PARTS_MASK, completedPartsMask)
                put(DownloadDatabase.COL_PART_OFFSETS, partOffsets)
            }
            db(context).writableDatabase.update(
                DownloadDatabase.TABLE, values, "${DownloadDatabase.COL_ID} = ?", arrayOf(id.toString())
            )
        }
    }

    suspend fun deletePersistedDownload(context: Context, id: Int) = withContext(Dispatchers.IO) {
        db(context).writableDatabase.delete(
            DownloadDatabase.TABLE,
            "${DownloadDatabase.COL_ID} = ?",
            arrayOf(id.toString())
        )
        Unit
    }
}
