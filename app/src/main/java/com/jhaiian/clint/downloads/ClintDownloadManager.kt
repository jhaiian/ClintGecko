package com.jhaiian.clint.downloads

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient

object ClintDownloadManager {

    internal const val CHANNEL_ID = "clint_downloads"
    internal const val EVENT_CHANNEL_ID = "clint_download_events_v2"

    internal val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
        .build()

    internal val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val idCounter = AtomicInteger(1)

    internal val activeJobs: MutableMap<Int, Job> = ConcurrentHashMap()

    internal class CallTag(val id: Int)

    internal val jobRegistry: MutableMap<Int, MutableSet<Job>> = ConcurrentHashMap()

    internal fun registerJob(id: Int, job: Job) {
        jobRegistry.getOrPut(id) { ConcurrentHashMap.newKeySet() }.add(job)
        job.invokeOnCompletion {
            jobRegistry[id]?.let { set ->
                set.remove(job)
                if (set.isEmpty()) jobRegistry.remove(id, set)
            }
        }
    }

    internal fun cancelCalls(id: Int) {
        val dispatcher = httpClient.dispatcher
        (dispatcher.runningCalls() + dispatcher.queuedCalls()).forEach { call ->
            if (call.request().tag(CallTag::class.java)?.id == id) call.cancel()
        }
    }

    internal fun isRemoved(id: Int): Boolean = id in removedIds

    internal val activeSpeedLimiters: MutableMap<Int, SpeedLimiter> = ConcurrentHashMap()

    internal val pauseRequested: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    internal val removedIds: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    internal var appContext: Context? = null
    private var initialized = false

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloadsFlow: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private fun addNew(item: DownloadItem) {
        _downloads.update { listOf(item) + it }
    }

    internal fun publish(item: DownloadItem) {
        _downloads.update { list -> list.map { if (it.id == item.id) item else it } }
    }

    internal fun updateItem(id: Int, transform: (DownloadItem) -> DownloadItem) {
        _downloads.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    internal suspend fun persistDownload(item: DownloadItem) {
        val ctx = appContext ?: return
        DownloadPersistence.persistDownload(ctx, item, removedIds)
    }

    internal fun checkpointProgress(id: Int, bytesDownloaded: Long, completedPartsMask: Long, partOffsets: String) {
        if (id in removedIds) return
        val ctx = appContext ?: return
        DownloadPersistence.checkpointProgress(ctx, id, bytesDownloaded, completedPartsMask, partOffsets)
    }

    private suspend fun deletePersistedDownload(id: Int) {
        val ctx = appContext ?: return
        DownloadPersistence.deletePersistedDownload(ctx, id)
    }

    fun createNotificationChannel(context: Context) {
        DownloadNotificationHelper.createNotificationChannel(context)
    }

    fun init(context: Context): Job {
        appContext = context.applicationContext
        if (initialized) return Job().apply { complete() }
        initialized = true
        val appCtx = context.applicationContext
        return applicationScope.launch {
            loadDownloads(appCtx)
            DownloadNetworkMonitor.register(appCtx)
            DownloadScheduleMonitor.scheduleNextCheck(appCtx)
            DownloadCustomScheduleMonitor.rearmAll(appCtx)
            DownloadStallMonitor.register(appCtx)
            tryDequeueNext(appCtx)
        }
    }

    private suspend fun loadDownloads(context: Context) {
        val loaded = DownloadPersistence.loadDownloads(context).map { item ->
            if (item.url == "blob:" && item.status == DownloadStatus.QUEUED) {
                val expired = item.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = context.getString(R.string.download_error_blob_expired)
                )
                DownloadPersistence.persistDownload(context, expired, removedIds)
                expired
            } else {
                item
            }
        }
        _downloads.update { loaded }
        loaded.maxOfOrNull { it.id }?.let { max ->
            if (max >= idCounter.get()) idCounter.set(max + 1)
        }
        val keepIds = loaded.filter { it.isStream }.map { it.id }.toSet()
        withContext(Dispatchers.IO) { sweepStreamWorkDirs(context, keepIds) }
    }

    private fun sweepStreamWorkDirs(context: Context, keepIds: Set<Int>) {
        File(context.filesDir, "stream_downloads").listFiles()?.forEach { dir ->
            val id = dir.name.toIntOrNull()
            if (id == null || id !in keepIds) runCatching { dir.deleteRecursively() }
        }
    }

    internal fun concurrentLimit(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getInt(
            DownloadSettingsKeys.PREF_CONCURRENT_DOWNLOADS,
            DownloadSettingsKeys.DEFAULT_CONCURRENT_DOWNLOADS
        )
    }

    internal fun activeCount(): Int =
        downloadsFlow.value.count { it.status in DownloadStatus.ACTIVELY_WORKING }

    fun findActiveDownloadForUrl(url: String): DownloadItem? =
        downloadsFlow.value.firstOrNull { it.url == url && it.status in DownloadStatus.NOT_FINISHED }

    internal fun tryDequeueNext(context: Context) {
        val limit = concurrentLimit(context)
        val isMetered = !DownloadNetworkMonitor.isNetworkUnmetered(context)
        val globalWindowOpen = DownloadScheduleMonitor.isWithinWindow(context)
        var dequeued: List<DownloadItem> = emptyList()
        _downloads.update { list ->
            var active = list.count { it.status in DownloadStatus.ACTIVELY_WORKING }
            val result = list.toMutableList()
            val picked = mutableListOf<DownloadItem>()
            while (active < limit) {
                val idx = result.indexOfLast {
                    it.status == DownloadStatus.QUEUED && (!it.unmeteredOnly || !isMetered) &&
                        (globalWindowOpen || it.scheduledStartAtMillis > 0L)
                }
                if (idx == -1) break
                val updated = result[idx].copy(
                    status = DownloadStatus.CONNECTING, speedBytesPerSec = 0L, scheduledStartAtMillis = 0L
                )
                result[idx] = updated
                picked += updated
                active++
            }
            dequeued = picked
            result
        }
        dequeued.forEach { updated ->
            DownloadNotificationHelper.showProgressNotification(context, updated)
            DownloadForegroundService.start(context)
            if (updated.isStream) launchStreamDownload(context, updated) else launchDownload(context, updated)
        }
    }

    private fun launchDownload(context: Context, item: DownloadItem) {
        activeSpeedLimiters[item.id] = SpeedLimiter(item.speedLimitBytesPerSec)
        val appContext = context.applicationContext
        val job = applicationScope.launch {
            persistDownload(item)
            DownloadWorker.run(appContext, item)
        }
        registerJob(item.id, job)
        activeJobs[item.id] = job
        job.invokeOnCompletion {
            activeJobs.remove(item.id, job)
            activeSpeedLimiters.remove(item.id)
        }
    }

    private fun launchStreamDownload(context: Context, item: DownloadItem) {
        activeSpeedLimiters[item.id] = SpeedLimiter(item.speedLimitBytesPerSec)
        val appContext = context.applicationContext
        val job = applicationScope.launch {
            persistDownload(item)
            com.jhaiian.clint.mediacapture.download.StreamDownloadJob.run(appContext, item)
        }
        registerJob(item.id, job)
        activeJobs[item.id] = job
        job.invokeOnCompletion {
            activeJobs.remove(item.id, job)
            activeSpeedLimiters.remove(item.id)
        }
    }

    fun enqueueStream(
        context: Context,
        request: com.jhaiian.clint.mediacapture.download.StreamDownloadRequest,
        videoWidth: Int?,
        videoHeight: Int?,
        videoBandwidth: Long?,
        audioBandwidth: Long?,
        primaryIsAudio: Boolean = false,
        locationMode: String? = null,
        customLocationUri: String? = null,
        categorizeEnabled: Boolean? = null,
        videoRepresentationId: String? = null,
        audioRepresentationId: String? = null
    ) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val effectiveLocationMode = locationMode
            ?: prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_LOCATION_MODE, DownloadSettingsKeys.MODE_DEFAULT)
            ?: DownloadSettingsKeys.MODE_DEFAULT
        val effectiveCustomLocationUri = if (locationMode != null) customLocationUri
            else prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, null)
        val effectiveCategorizeEnabled = categorizeEnabled ?: DownloadCategories.isEnabled(context)
        if (!DownloadFileHelper.isCustomLocationAccessible(context, effectiveLocationMode, effectiveCustomLocationUri)) {
            promptInvalidLocation(context) {
                enqueueStream(context, request, videoWidth, videoHeight, videoBandwidth, audioBandwidth, primaryIsAudio, locationMode, customLocationUri, categorizeEnabled, videoRepresentationId, audioRepresentationId)
            }
            return
        }

        val id = idCounter.getAndIncrement()
        File(context.filesDir, "stream_downloads/$id").deleteRecursively()
        val queued = activeCount() >= concurrentLimit(context)
        val baseItem = DownloadItem(
            id = id,
            url = request.videoUrl ?: request.audioUrl ?: "",
            filename = request.filename,
            userAgent = request.userAgent,
            startedAt = System.currentTimeMillis(),
            status = if (queued) DownloadStatus.QUEUED else DownloadStatus.CONNECTING,
            locationMode = effectiveLocationMode,
            customLocationUri = effectiveCustomLocationUri,
            categorizeEnabled = effectiveCategorizeEnabled,
            resumable = true,
            isStream = true,
            totalBytes = request.estimatedTotalBytes.takeIf { it > 0L } ?: -1L,
            streamVideoUrl = request.videoUrl ?: request.audioUrl ?: "",
            streamAudioUrl = if (request.videoUrl != null) request.audioUrl else null,
            streamSubtitleUrl = request.subtitleUrl,
            streamFormat = when {
                request.subtitleOnly -> com.jhaiian.clint.mediacapture.download.STREAM_FORMAT_HLS_SUBTITLE
                request.format == com.jhaiian.clint.mediacapture.download.StreamContainerFormat.DASH -> "DASH"
                else -> "HLS"
            },
            streamPageUrl = request.pageUrl,
            referer = request.referer,
            streamVideoWidth = videoWidth,
            streamVideoHeight = videoHeight,
            streamVideoBandwidth = videoBandwidth,
            streamAudioBandwidth = audioBandwidth,
            streamPrimaryIsAudio = primaryIsAudio,
            streamNoAudio = request.noAudio,
            streamHeaders = request.headers,
            streamConcurrentSegments = request.concurrentSegments,
            streamIsLive = request.isLive,
            streamConvertTsToMp4 = request.convertTsToMp4,
            streamVideoRepresentationId = videoRepresentationId,
            streamAudioRepresentationId = audioRepresentationId,
            speedLimitBytesPerSec = request.speedLimitBytesPerSec
        )
        val directCustomDir = DownloadFileHelper.resolveDirectCustomDir(context, baseItem)
        val safMode = directCustomDir == null && DownloadFileHelper.isSafCustomMode(context, baseItem)
        val rawDestDir = directCustomDir ?: if (safMode) DownloadFileHelper.tempDownloadDir(context) else DownloadFileHelper.resolveDownloadDir()
        val needsMuxGuess = request.videoUrl != null && request.audioUrl != null
        val convertsTsGuess = request.convertTsToMp4 && !primaryIsAudio
        val userExt = request.filename.substringAfterLast('.', "").trim().takeIf { it.isNotBlank() }
        val guessedExt = userExt ?: when {
            request.subtitleOnly -> "vtt"
            needsMuxGuess || convertsTsGuess -> "mp4"
            primaryIsAudio && request.convertTsToMp4 -> "aac"
            else -> "ts"
        }
        val dot = request.filename.lastIndexOf('.')
        val guessedBase = if (dot > 0) request.filename.substring(0, dot) else request.filename
        val guessedFilename = "$guessedBase.$guessedExt"
        val destDir = if (safMode) rawDestDir else DownloadCategories.resolveDir(effectiveCategorizeEnabled, rawDestDir, guessedFilename)
        destDir.mkdirs()
        val previewName = DownloadFileHelper.previewUniqueName(destDir, guessedFilename)
        val item = baseItem.copy(filename = previewName)
        addNew(item)

        if (queued) {
            DownloadNotificationHelper.showQueuedNotification(context, item)
            return
        }

        DownloadNotificationHelper.showProgressNotification(context, item)
        DownloadForegroundService.start(context)
        launchStreamDownload(context, item)
    }

    internal fun withLiveSettings(item: DownloadItem): DownloadItem {
        val current = downloadsFlow.value.find { it.id == item.id } ?: return item
        return item.copy(
            retryEnabled = current.retryEnabled,
            unmeteredOnly = current.unmeteredOnly,
            speedLimitBytesPerSec = current.speedLimitBytesPerSec
        )
    }

    fun updateDownloadSettings(
        context: Context,
        id: Int,
        retryEnabled: Boolean,
        unmeteredOnly: Boolean,
        speedLimitBytesPerSec: Long
    ) {
        val item = downloadsFlow.value.find { it.id == id } ?: return
        val updated = item.copy(
            retryEnabled = retryEnabled,
            unmeteredOnly = unmeteredOnly,
            speedLimitBytesPerSec = speedLimitBytesPerSec
        )
        publish(updated)
        applicationScope.launch { persistDownload(updated) }
        activeSpeedLimiters[id]?.updateLimit(speedLimitBytesPerSec)
    }

    private fun promptInvalidLocation(context: Context, onUseDefault: () -> Unit) {
        val config = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
            title = context.getString(R.string.download_location_invalid_title),
            message = context.getString(R.string.download_location_invalid_message),
            positiveLabel = context.getString(R.string.action_open_settings),
            onPositive = {
                context.startActivity(
                    android.content.Intent(context, com.jhaiian.clint.settings.SettingsActivity::class.java)
                        .putExtra(com.jhaiian.clint.settings.SettingsActivity.EXTRA_OPEN_FRAGMENT, "download_settings")
                )
            },
            negativeLabel = context.getString(R.string.download_location_use_default_action),
            onNegative = {
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putString(DownloadSettingsKeys.PREF_DOWNLOAD_LOCATION_MODE, DownloadSettingsKeys.MODE_DEFAULT)
                    .apply()
                onUseDefault()
            },
            neutralLabel = context.getString(R.string.action_cancel)
        )
        if (context is com.jhaiian.clint.ui.ConfirmDialogHostActivity) context.confirmDialogConfig = config
    }

    fun enqueueBlob(context: Context, base64: String, filename: String, mimeType: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val locationMode = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_LOCATION_MODE, DownloadSettingsKeys.MODE_DEFAULT)
            ?: DownloadSettingsKeys.MODE_DEFAULT
        val customLocationUri = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, null)
        if (!DownloadFileHelper.isCustomLocationAccessible(context, locationMode, customLocationUri)) {
            promptInvalidLocation(context) { enqueueBlob(context, base64, filename, mimeType) }
            return
        }
        val id = idCounter.getAndIncrement()
        val item = DownloadItem(
            id = id, url = "blob:", filename = filename, userAgent = "",
            startedAt = System.currentTimeMillis()
        )
        addNew(item)
        DownloadNotificationHelper.showProgressNotification(context, item)
        DownloadForegroundService.start(context)
        val directCustomDir = DownloadFileHelper.resolveDirectCustomDir(context)
        val safMode = DownloadFileHelper.isSafCustomMode(context) && directCustomDir == null
        val job = applicationScope.launch {
            var current = item
            try {
                val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                var finalFilename = current.filename
                if (finalFilename.endsWith(".bin") || !finalFilename.contains(".")) {
                    val ext = DownloadFileHelper.detectExtFromMagicBytes(bytes.copyOf(minOf(bytes.size, 512)))
                        ?: android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                    if (ext != null) finalFilename = "${finalFilename.removeSuffix(".bin")}.$ext"
                }
                val rawDestDir = directCustomDir
                    ?: if (safMode) DownloadFileHelper.tempDownloadDir(context) else DownloadFileHelper.resolveDownloadDir()
                val destDir = if (safMode) rawDestDir else DownloadCategories.resolveDir(DownloadCategories.isEnabled(context), rawDestDir, finalFilename)
                destDir.mkdirs()
                val destFile = DownloadFileHelper.uniqueFile(destDir, finalFilename)
                java.io.FileOutputStream(destFile).use { it.write(bytes) }
                current = current.copy(
                    filename = destFile.name,
                    file = destFile,
                    totalBytes = bytes.size.toLong(),
                    bytesDownloaded = bytes.size.toLong()
                )
                if (safMode) {
                    DownloadWorker.moveTempToSaf(context, current)
                } else {
                    current = current.copy(status = DownloadStatus.COMPLETE)
                    persistDownload(current)
                    publish(current)
                    DownloadNotificationHelper.showCompleteNotification(context, current)
                    tryDequeueNext(context)
                }
            } catch (e: Throwable) {
                DownloadWorker.fail(context, current, e.message ?: context.getString(R.string.download_error_unknown))
            }
        }
        registerJob(id, job)
        activeJobs[id] = job
        job.invokeOnCompletion { activeJobs.remove(id, job) }
    }

    private fun dispatchOrQueue(context: Context, source: DownloadItem, record: (DownloadItem) -> Unit) {
        var current = source

        if (current.scheduledStartAtMillis > System.currentTimeMillis()) {
            val updated = current.copy(
                status = DownloadStatus.PAUSED, waitingForCustomSchedule = true,
                waitingForUnmetered = false, waitingForNetwork = false, waitingForSchedule = false
            )
            record(updated)
            applicationScope.launch { persistDownload(updated) }
            DownloadCustomScheduleMonitor.schedule(context, updated.id, updated.scheduledStartAtMillis)
            DownloadNotificationHelper.showWaitingCustomScheduleNotification(context, updated)
            DownloadForegroundService.start(context)
            return
        }

        if (current.scheduledStartAtMillis == 0L && !DownloadScheduleMonitor.isWithinWindow(context)) {
            DownloadScheduleMonitor.scheduleWaitingIds.add(current.id)
            val updated = current.copy(
                status = DownloadStatus.PAUSED, waitingForSchedule = true,
                waitingForUnmetered = false, waitingForNetwork = false, waitingForCustomSchedule = false
            )
            record(updated)
            applicationScope.launch { persistDownload(updated) }
            DownloadNotificationHelper.showWaitingScheduleNotification(context, updated)
            DownloadForegroundService.start(context)
            return
        }

        if (current.scheduledStartAtMillis != 0L) {
            current = current.copy(scheduledStartAtMillis = 0L)
        }

        if (current.unmeteredOnly && !DownloadNetworkMonitor.isNetworkUnmetered(context)) {
            DownloadNetworkMonitor.unmeteredPausedIds.add(current.id)
            val updated = current.copy(status = DownloadStatus.PAUSED, waitingForUnmetered = true, waitingForCustomSchedule = false)
            record(updated)
            applicationScope.launch { persistDownload(updated) }
            DownloadNotificationHelper.showWaitingUnmeteredNotification(context, updated)
            DownloadForegroundService.start(context)
            return
        }

        if (activeCount() >= concurrentLimit(context)) {
            val updated = current.copy(
                status = DownloadStatus.QUEUED, waitingForUnmetered = false, waitingForNetwork = false,
                waitingForCustomSchedule = false
            )
            record(updated)
            applicationScope.launch { persistDownload(updated) }
            DownloadNotificationHelper.showQueuedNotification(context, updated)
            DownloadForegroundService.start(context)
            return
        }

        val updated = current.copy(
            waitingForUnmetered = false, waitingForNetwork = false, waitingForCustomSchedule = false,
            status = DownloadStatus.CONNECTING, speedBytesPerSec = 0L
        )
        record(updated)
        DownloadNotificationHelper.showProgressNotification(context, updated)
        DownloadForegroundService.start(context)
        if (updated.isStream) launchStreamDownload(context, updated) else launchDownload(context, updated)
    }

    fun enqueue(
        context: Context,
        url: String,
        filename: String,
        userAgent: String,
        referer: String = "",
        cookies: String = "",
        retryEnabled: Boolean = true,
        unmeteredOnly: Boolean = false,
        splitParts: Int = 32,
        multithreadingParts: Int = 4,
        speedLimitBytesPerSec: Long = 0L,
        locationMode: String = "default",
        customLocationUri: String? = null,
        scheduledStartAtMillis: Long = 0L,
        categorizeEnabled: Boolean = DownloadSettingsKeys.DEFAULT_CATEGORIZE_DOWNLOADS
    ) {
        if (!DownloadFileHelper.isCustomLocationAccessible(context, locationMode, customLocationUri)) {
            promptInvalidLocation(context) {
                enqueue(
                    context, url, filename, userAgent, referer, cookies, retryEnabled, unmeteredOnly,
                    splitParts, multithreadingParts, speedLimitBytesPerSec,
                    DownloadSettingsKeys.MODE_DEFAULT, null, scheduledStartAtMillis, categorizeEnabled
                )
            }
            return
        }
        val id = idCounter.getAndIncrement()
        val baseItem = DownloadItem(
            id = id, url = url, filename = filename, userAgent = userAgent, referer = referer,
            cookies = cookies, retryEnabled = retryEnabled, unmeteredOnly = unmeteredOnly,
            splitParts = splitParts, multithreadingParts = multithreadingParts,
            speedLimitBytesPerSec = speedLimitBytesPerSec,
            locationMode = locationMode, customLocationUri = customLocationUri, categorizeEnabled = categorizeEnabled,
            startedAt = System.currentTimeMillis(), scheduledStartAtMillis = scheduledStartAtMillis
        )
        val directCustomDir = DownloadFileHelper.resolveDirectCustomDir(context, baseItem)
        val safMode = directCustomDir == null && DownloadFileHelper.isSafCustomMode(context, baseItem)
        val rawDestDir = directCustomDir ?: if (safMode) DownloadFileHelper.tempDownloadDir(context) else DownloadFileHelper.resolveDownloadDir()
        val destDir = if (safMode) rawDestDir else DownloadCategories.resolveDir(categorizeEnabled, rawDestDir, filename)
        destDir.mkdirs()
        val previewName = DownloadFileHelper.previewUniqueName(destDir, filename)
        dispatchOrQueue(context, baseItem.copy(filename = previewName), ::addNew)
    }

    fun cancel(context: Context, id: Int) {
        remove(context, id)
    }

    fun pause(context: Context, id: Int) {
        val item = downloadsFlow.value.find { it.id == id } ?: return

        if (item.status == DownloadStatus.QUEUED) {
            val updated = item.copy(status = DownloadStatus.PAUSED, waitingForUnmetered = false)
            publish(updated)
            applicationScope.launch { persistDownload(updated) }
            return
        }

        pauseRequested.add(id)

        if (item.status == DownloadStatus.RETRYING || item.status == DownloadStatus.CONNECTING) {
            val updated = item.copy(
                status = DownloadStatus.PAUSED, retryDelaySec = 0, retryAttempt = 0, waitingForUnmetered = false
            )
            DownloadNetworkMonitor.unmeteredPausedIds.remove(id)
            publish(updated)
            applicationScope.launch { persistDownload(updated) }
            tryDequeueNext(context)
        } else if (item.waitingForUnmetered) {
            DownloadNetworkMonitor.unmeteredPausedIds.remove(id)
            val updated = item.copy(waitingForUnmetered = false, status = DownloadStatus.PAUSED)
            context.getSystemService(NotificationManager::class.java).cancel(id)
            publish(updated)
            applicationScope.launch { persistDownload(updated) }
        } else if (item.waitingForNetwork) {
            DownloadNetworkMonitor.networkWaitingIds.remove(id)
            val updated = item.copy(waitingForNetwork = false, status = DownloadStatus.PAUSED)
            context.getSystemService(NotificationManager::class.java).cancel(id)
            publish(updated)
            applicationScope.launch { persistDownload(updated) }
        } else if (item.waitingForSchedule) {
            DownloadScheduleMonitor.scheduleWaitingIds.remove(id)
            val updated = item.copy(waitingForSchedule = false, status = DownloadStatus.PAUSED)
            context.getSystemService(NotificationManager::class.java).cancel(id)
            publish(updated)
            applicationScope.launch { persistDownload(updated) }
        } else if (item.waitingForCustomSchedule) {
            DownloadCustomScheduleMonitor.cancel(context, id)
            val updated = item.copy(waitingForCustomSchedule = false, status = DownloadStatus.PAUSED)
            context.getSystemService(NotificationManager::class.java).cancel(id)
            publish(updated)
            applicationScope.launch { persistDownload(updated) }
        }
    }

    internal fun pauseForUnmeteredWait(context: Context, id: Int) {
        val item = downloadsFlow.value.find { it.id == id } ?: return
        if (item.status !in DownloadStatus.ACTIVELY_WORKING) return
        pauseRequested.add(id)
        if (item.status == DownloadStatus.RETRYING || item.status == DownloadStatus.CONNECTING) {
            val updated = item.copy(
                status = DownloadStatus.PAUSED, waitingForUnmetered = true,
                retryDelaySec = 0, retryAttempt = 0, speedBytesPerSec = 0L
            )
            publish(updated)
            applicationScope.launch { persistDownload(updated) }
            DownloadNotificationHelper.showWaitingUnmeteredNotification(context, updated)
            tryDequeueNext(context)
        }
    }

    fun resume(context: Context, id: Int) {
        val item = downloadsFlow.value.find { it.id == id } ?: return
        if (item.status != DownloadStatus.PAUSED) return
        pauseRequested.remove(id)
        DownloadNetworkMonitor.unmeteredPausedIds.remove(id)
        DownloadNetworkMonitor.networkWaitingIds.remove(id)
        DownloadScheduleMonitor.scheduleWaitingIds.remove(id)
        DownloadCustomScheduleMonitor.cancel(context, id)
        dispatchOrQueue(context, item, ::publish)
    }

    fun remove(context: Context, id: Int, deleteFile: Boolean = false) {
        removedIds.add(id)
        pauseRequested.remove(id)
        DownloadNetworkMonitor.unmeteredPausedIds.remove(id)
        DownloadNetworkMonitor.networkWaitingIds.remove(id)
        DownloadCustomScheduleMonitor.cancel(context, id)
        val item = downloadsFlow.value.find { it.id == id }
        val jobs = (jobRegistry[id]?.toList() ?: emptyList()) + listOfNotNull(activeJobs[id])
        jobs.forEach { it.cancel() }
        cancelCalls(id)
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.cancel(id)
        DownloadNotificationHelper.cancelCompleteNotification(context, id)
        _downloads.update { list -> list.filterNot { it.id == id } }
        val appCtx = appContext
        applicationScope.launch {
            jobs.forEach { j ->
                var attempts = 0
                while (!j.isCompleted && attempts < 50) {
                    cancelCalls(id)
                    kotlinx.coroutines.withTimeoutOrNull(200L) { j.join() }
                    attempts++
                }
            }
            nm.cancel(id)
            DownloadNotificationHelper.cancelCompleteNotification(context, id)
            if (deleteFile) {
                item?.file?.delete()
                item?.contentUri?.let { uriStr ->
                    runCatching {
                        val ctx = appCtx ?: return@runCatching
                        val docFile = DocumentFile.fromSingleUri(ctx, Uri.parse(uriStr))
                        docFile?.delete()
                    }
                }
            }
            val tempDir = DownloadFileHelper.tempDownloadDir(appCtx)
            item?.filename?.let { name ->
                File(tempDir, name).takeIf { it.exists() }?.delete()
            }
            appCtx?.let { File(it.filesDir, "stream_downloads/$id").deleteRecursively() }
            deletePersistedDownload(id)
            removedIds.remove(id)
        }
    }

    fun clearCompleted() {
        var toDelete: List<Int> = emptyList()
        _downloads.update { list ->
            val (keep, remove) = list.partition { it.status in DownloadStatus.NOT_FINISHED }
            toDelete = remove.map { it.id }
            keep
        }
        applicationScope.launch {
            toDelete.forEach { id ->
                deletePersistedDownload(id)
                appContext?.let { File(it.filesDir, "stream_downloads/$id").deleteRecursively() }
            }
        }
    }

    fun retryFailed(context: Context, id: Int) {
        val item = downloadsFlow.value.find { it.id == id } ?: return
        if (item.status != DownloadStatus.FAILED) return
        val reset = item.copy(retryAttempt = 0, retryDelaySec = 0, errorMessage = null, speedBytesPerSec = 0L)
        dispatchOrQueue(context, reset, ::publish)
    }

    enum class RenameResult { SUCCESS, EXISTS, MISSING, FAILED }

    suspend fun renameFile(context: Context, id: Int, newName: String): RenameResult = kotlinx.coroutines.withContext(Dispatchers.IO) {
        val item = downloadsFlow.value.find { it.id == id }
        if (item == null || item.status != DownloadStatus.COMPLETE) return@withContext RenameResult.FAILED
        val sourceExists = when {
            item.file != null -> item.file.exists()
            item.contentUri != null -> runCatching { DocumentFile.fromSingleUri(context, Uri.parse(item.contentUri))?.exists() == true }.getOrDefault(false)
            else -> false
        }
        if (!sourceExists) return@withContext RenameResult.MISSING
        val name = DownloadFileHelper.sanitizeFileName(newName)
        val renamed: DownloadItem? = try {
            when {
                item.file != null -> {
                    val target = File(item.file.parentFile, name)
                    if (target.exists() && target.absolutePath != item.file.absolutePath) return@withContext RenameResult.EXISTS
                    if (item.file.renameTo(target)) item.copy(filename = name, file = target) else null
                }
                item.contentUri != null -> {
                    val newUri = android.provider.DocumentsContract.renameDocument(context.contentResolver, Uri.parse(item.contentUri), name)
                    if (newUri != null) {
                        val finalName = DocumentFile.fromSingleUri(context, newUri)?.name ?: name
                        item.copy(filename = finalName, contentUri = newUri.toString())
                    } else null
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
        if (renamed == null) return@withContext RenameResult.FAILED
        updateItem(id) { renamed }
        persistDownload(renamed)
        RenameResult.SUCCESS
    }

    fun updateDownloadUrl(id: Int, newUrl: String) {
        updateItem(id) { current ->
            if (current.isStream) {
                if (current.streamPrimaryIsAudio) current.copy(url = newUrl, streamAudioUrl = newUrl)
                else current.copy(url = newUrl, streamVideoUrl = newUrl)
            } else {
                current.copy(url = newUrl)
            }
        }
    }

    fun onUnmeteredOnlyChanged(context: Context, enabled: Boolean) {
        DownloadNetworkMonitor.onUnmeteredOnlyChanged(context, enabled)
    }
}
