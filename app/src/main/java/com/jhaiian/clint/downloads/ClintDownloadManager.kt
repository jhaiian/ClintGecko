package com.jhaiian.clint.downloads

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.jhaiian.clint.R
import com.jhaiian.clint.downloads.mega.MegaPaths
import com.jhaiian.clint.downloads.mega.MegaSource
import com.jhaiian.clint.settings.downloads.DownloadSettingsKeys
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
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
        .connectionPool(ConnectionPool(32, 5, TimeUnit.MINUTES))
        .build()

    internal val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    internal val rangedHttpClient: OkHttpClient by lazy {
        httpClient.newBuilder().protocols(listOf(okhttp3.Protocol.HTTP_1_1)).build()
    }

    internal val globalSpeedLimiter = SpeedLimiter(0L)
    private var globalLimitListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null

    private val publishLock = Any()
    private val pendingProgress = HashMap<Int, DownloadItem>()
    private val progressFlushScheduled = java.util.concurrent.atomic.AtomicBoolean(false)
    private const val PROGRESS_FLUSH_MS = 250L
    private const val SUPERSEDE_WAIT_ATTEMPTS = 50

    private val launchGenerations: MutableMap<Int, Long> = ConcurrentHashMap()
    private val generationCounter = AtomicLong(0L)
    private val pendingAttempts: MutableMap<Int, DownloadItem> = ConcurrentHashMap()

    internal val lastActivityAt: MutableMap<Int, Long> = ConcurrentHashMap()

    internal fun noteActivity(id: Int) {
        val now = System.currentTimeMillis()
        val last = lastActivityAt[id]
        if (last == null || now - last >= 1000L) lastActivityAt[id] = now
    }

    internal fun requestNextAttempt(item: DownloadItem) {
        if (item.generation == 0L || isStale(item)) return
        pendingAttempts[item.id] = item
    }

    internal fun isStale(item: DownloadItem): Boolean = isStaleGeneration(item.id, item.generation)

    private fun isStaleGeneration(id: Int, generation: Long): Boolean =
        generation != 0L && launchGenerations[id] != generation

    private fun mergeOwned(current: DownloadItem, incoming: DownloadItem): DownloadItem =
        if (incoming.generation == 0L) incoming
        else incoming.copy(
            url = current.url,
            referer = current.referer,
            pageUrl = current.pageUrl,
            cookies = current.cookies,
            streamVideoUrl = current.streamVideoUrl,
            streamAudioUrl = current.streamAudioUrl,
            retryEnabled = current.retryEnabled,
            unmeteredOnly = current.unmeteredOnly,
            speedLimitBytesPerSec = current.speedLimitBytesPerSec,
            queueOrder = current.queueOrder,
            generation = 0L
        )

    private fun currentItemLocked(id: Int): DownloadItem? =
        pendingProgress[id] ?: _downloads.value.firstOrNull { it.id == id }

    private fun isProgressOnly(before: DownloadItem, after: DownloadItem): Boolean =
        before.status == after.status && before.copy(
            bytesDownloaded = after.bytesDownloaded,
            totalBytes = after.totalBytes,
            speedBytesPerSec = after.speedBytesPerSec,
            partProgress = after.partProgress,
            segmentsCompleted = after.segmentsCompleted,
            segmentsTotal = after.segmentsTotal,
            streamSegDone = after.streamSegDone,
            streamSegTotal = after.streamSegTotal,
            streamSegBytes = after.streamSegBytes,
            muxProgress = after.muxProgress,
            copyProgress = after.copyProgress,
            allocationProgress = after.allocationProgress,
            activeElapsedMs = after.activeElapsedMs,
            activeStartedAt = after.activeStartedAt,
            activeConnections = after.activeConnections
        ) == after

    private fun flushProgress() {
        synchronized(publishLock) {
            progressFlushScheduled.set(false)
            if (pendingProgress.isEmpty()) return
            val batch = HashMap(pendingProgress)
            pendingProgress.clear()
            _downloads.update { list -> list.map { batch[it.id] ?: it } }
        }
    }

    private fun refreshGlobalSpeedLimit(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val amount = prefs.getInt(DownloadSettingsKeys.PREF_TOTAL_SPEED_LIMIT_AMOUNT, DownloadSettingsKeys.DEFAULT_TOTAL_SPEED_LIMIT_AMOUNT)
        val unit = prefs.getString(DownloadSettingsKeys.PREF_TOTAL_SPEED_LIMIT_UNIT, DEFAULT_SPEED_LIMIT_UNIT) ?: DEFAULT_SPEED_LIMIT_UNIT
        globalSpeedLimiter.updateLimit(resolveSpeedLimitBytesPerSec(context, amount, unit))
    }

    private val idCounter = AtomicInteger(1)
    @Volatile private var idSeeded = false

    private fun raiseIdFloor(maxId: Int) {
        idCounter.updateAndGet { maxOf(it, maxId + 1) }
    }

    private fun nextId(context: Context): Int {
        if (!idSeeded) {
            synchronized(idCounter) {
                if (!idSeeded) {
                    raiseIdFloor(runCatching { DownloadPersistence.maxId(context) }.getOrDefault(0))
                    idSeeded = true
                }
            }
        }
        return idCounter.getAndIncrement()
    }

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
    internal val timeoutPausedIds: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    fun resumeTimeoutPaused(context: Context) {
        val appCtx = context.applicationContext
        applicationScope.launch { resumeSystemPaused(appCtx) }
    }

    internal suspend fun resumeSystemPaused(context: Context) {
        init(context).join()
        if (timeoutPausedIds.isEmpty()) return
        val ids = timeoutPausedIds.toList()
        timeoutPausedIds.clear()
        ids.forEach { resume(context, it) }
    }

    internal val removedIds: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    @Volatile internal var appContext: Context? = null
    private val initLock = Any()
    @Volatile private var initJob: Job? = null
    @Volatile private var initStartedAt = 0L

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloadsFlow: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    @Volatile var incognitoSource: (() -> Boolean)? = null
    private val unrecordedItems: MutableMap<Int, DownloadItem> = ConcurrentHashMap()

    private fun startedInIncognito(): Boolean = incognitoSource?.invoke() == true

    fun recordsHistory(context: Context, isIncognito: Boolean): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return if (isIncognito) {
            prefs.getBoolean(DownloadSettingsKeys.PREF_HISTORY_INCOGNITO, DownloadSettingsKeys.DEFAULT_HISTORY_INCOGNITO)
        } else {
            prefs.getBoolean(DownloadSettingsKeys.PREF_HISTORY_NORMAL, DownloadSettingsKeys.DEFAULT_HISTORY_NORMAL)
        }
    }

    fun findItem(id: Int): DownloadItem? = downloadsFlow.value.find { it.id == id } ?: unrecordedItems[id]

    internal fun onDownloadCompleted(context: Context, item: DownloadItem) {
        DownloadNotificationHelper.showCompleteNotification(context, item)
        if (recordsHistory(context, item.isIncognito)) return
        DownloadLog.i(item.id, "HISTORY") { "Finished download not kept in the list" }
        unrecordedItems[item.id] = item
        removedIds.add(item.id)
        _downloads.update { list -> list.filterNot { it.id == item.id } }
        DownloadLog.deleteItemLog(item.id)
        applicationScope.launch { deletePersistedDownload(item.id) }
    }

    private fun addNew(item: DownloadItem) {
        DownloadLog.section(item.id, "Download added") { DownloadLog.describe(item) }
        _downloads.update { listOf(item) + it }
    }

    internal fun publish(item: DownloadItem) {
        synchronized(publishLock) {
            if (isStale(item)) return
            val current = currentItemLocked(item.id)
            val next = if (current != null) mergeOwned(current, item) else item.copy(generation = 0L)
            if (DownloadLog.enabled) DownloadLog.observe(current, next)
            if (current != null && isProgressOnly(current, next)) {
                pendingProgress[item.id] = next
                if (progressFlushScheduled.compareAndSet(false, true)) {
                    applicationScope.launch {
                        kotlinx.coroutines.delay(PROGRESS_FLUSH_MS)
                        flushProgress()
                    }
                }
                return
            }
            pendingProgress.remove(item.id)
            _downloads.update { list -> list.map { if (it.id == item.id) next else it } }
        }
    }

    internal fun updateItem(id: Int, transform: (DownloadItem) -> DownloadItem) {
        synchronized(publishLock) {
            val pending = pendingProgress.remove(id)
            val before = pending ?: _downloads.value.firstOrNull { it.id == id }
            _downloads.update { list -> list.map { if (it.id == id) transform(pending ?: it) else it } }
            if (DownloadLog.enabled && before != null) _downloads.value.firstOrNull { it.id == id }?.let { DownloadLog.observe(before, it) }
        }
    }

    private fun mergedForPersist(item: DownloadItem): Pair<DownloadItem, Long> = synchronized(publishLock) {
        (currentItemLocked(item.id)?.let { mergeOwned(it, item) } ?: item) to DownloadPersistence.nextSequence()
    }

    internal suspend fun persistDownload(item: DownloadItem) {
        if (isStale(item)) return
        val ctx = appContext ?: return
        val (merged, sequence) = mergedForPersist(item)
        DownloadPersistence.persistDownload(ctx, merged, removedIds, sequence)
    }

    internal fun persistLater(item: DownloadItem) {
        if (isStale(item)) return
        val ctx = appContext ?: return
        val (merged, sequence) = mergedForPersist(item)
        applicationScope.launch { DownloadPersistence.persistDownload(ctx, merged, removedIds, sequence) }
    }

    internal fun checkpointProgress(
        id: Int,
        bytesDownloaded: Long,
        completedPartsMask: Long,
        partOffsets: String,
        partMap: String = "",
        generation: Long = 0L
    ) {
        if (id in removedIds || isStaleGeneration(id, generation)) return
        val ctx = appContext ?: return
        DownloadPersistence.checkpointProgress(ctx, id, bytesDownloaded, completedPartsMask, partOffsets, partMap)
    }

    private fun persistCurrent(id: Int) {
        val item = downloadsFlow.value.find { it.id == id } ?: return
        persistLater(item)
    }

    private suspend fun deletePersistedDownload(id: Int) {
        val ctx = appContext ?: return
        DownloadPersistence.deletePersistedDownload(ctx, id)
    }

    fun createNotificationChannel(context: Context) {
        DownloadNotificationHelper.createNotificationChannel(context)
    }

    fun init(context: Context): Job {
        initJob?.let { return it }
        synchronized(initLock) {
            initJob?.let { return it }
            initStartedAt = System.currentTimeMillis()
            val appCtx = context.applicationContext
            appContext = appCtx
            DownloadLog.init(appCtx)
            DownloadNotificationHelper.createNotificationChannel(appCtx)
            refreshGlobalSpeedLimit(appCtx)
            val limitListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == DownloadSettingsKeys.PREF_TOTAL_SPEED_LIMIT_AMOUNT || key == DownloadSettingsKeys.PREF_TOTAL_SPEED_LIMIT_UNIT ||
                    key == com.jhaiian.clint.util.PREF_MEASUREMENT_SYSTEM
                ) refreshGlobalSpeedLimit(appCtx)
            }
            globalLimitListener = limitListener
            PreferenceManager.getDefaultSharedPreferences(appCtx).registerOnSharedPreferenceChangeListener(limitListener)
            val job = applicationScope.launch {
                loadDownloads(appCtx)
                DownloadNetworkMonitor.register(appCtx)
                DownloadScheduleMonitor.reconcile(appCtx)
                DownloadCustomScheduleMonitor.rearmAll(appCtx)
                DownloadStallMonitor.register(appCtx)
                DownloadSpeedHistory.start(applicationScope, downloadsFlow)
                tryDequeueNext(appCtx)
            }
            initJob = job
            return job
        }
    }

    private suspend fun loadDownloads(context: Context) {
        val (unrecorded, kept) = DownloadPersistence.loadDownloads(context)
            .partition { it.status == DownloadStatus.COMPLETE && !recordsHistory(context, it.isIncognito) }
        unrecorded.forEach {
            unrecordedItems[it.id] = it
            deletePersistedDownload(it.id)
        }
        raiseIdFloor(unrecorded.maxOfOrNull { it.id } ?: 0)
        val loaded = kept.map { item ->
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
        _downloads.update { current ->
            val currentIds = current.mapTo(HashSet()) { it.id }
            current + loaded.filterNot { it.id in currentIds }
        }
        val maxLoadedId = loaded.maxOfOrNull { it.id } ?: 0
        raiseIdFloor(maxLoadedId)
        idSeeded = true
        loaded.filter { it.status == DownloadStatus.PAUSED }.forEach { item ->
            if (item.waitingForNetwork) DownloadNetworkMonitor.networkWaitingIds.add(item.id)
            if (item.waitingForUnmetered) DownloadNetworkMonitor.unmeteredPausedIds.add(item.id)
            if (item.waitingForSchedule) DownloadScheduleMonitor.scheduleWaitingIds.add(item.id)
        }
        val keepIds = downloadsFlow.value.filter { it.isStream }.mapTo(HashSet()) { it.id }
        withContext(Dispatchers.IO) {
            sweepStreamWorkDirs(context, keepIds, maxLoadedId)
            sweepTempFiles(context)
        }
    }

    private fun sweepTempFiles(context: Context) {
        val referenced = downloadsFlow.value.mapNotNullTo(HashSet()) { it.file?.absolutePath }
        DownloadFileHelper.tempDownloadDir(context).listFiles()?.forEach { file ->
            if (file.isFile && file.absolutePath !in referenced && file.lastModified() < initStartedAt) {
                runCatching { file.delete() }
            }
        }
        DownloadBlobStore.sweep(context, initStartedAt)
    }

    private fun sweepStreamWorkDirs(context: Context, keepIds: Set<Int>, maxLoadedId: Int) {
        File(context.filesDir, "stream_downloads").listFiles()?.forEach { dir ->
            val id = dir.name.toIntOrNull()
            if (id == null || (id !in keepIds && id <= maxLoadedId)) runCatching { dir.deleteRecursively() }
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
                val idx = result.withIndex()
                    .filter { (_, candidate) ->
                        candidate.status == DownloadStatus.QUEUED && (!candidate.unmeteredOnly || !isMetered) &&
                            (globalWindowOpen || candidate.scheduledStartAtMillis > 0L)
                    }
                    .minWithOrNull(compareBy<IndexedValue<DownloadItem>>({ it.value.queueOrder }, { it.value.id }))
                    ?.index ?: -1
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
            DownloadLog.i(updated.id, "QUEUE") { "Starting from queue \u00B7 limit $limit concurrent download(s)" }
            DownloadNotificationHelper.showProgressNotification(context, updated)
            DownloadForegroundService.start(context)
            if (updated.isStream) launchStreamDownload(context, updated) else launchDownload(context, updated)
        }
    }

    private fun launchDownload(context: Context, item: DownloadItem) {
        launchWorker(context, item) { ctx, owned -> DownloadWorker.run(ctx, owned) }
    }

    private fun launchStreamDownload(context: Context, item: DownloadItem) {
        launchWorker(context, item) { ctx, owned ->
            com.jhaiian.clint.mediacapture.download.StreamDownloadJob.run(ctx, owned)
        }
    }

    private fun launchWorker(context: Context, item: DownloadItem, body: suspend (Context, DownloadItem) -> Unit) {
        val appContext = context.applicationContext
        val generation = generationCounter.incrementAndGet()
        launchGenerations[item.id] = generation
        val owned = item.copy(generation = generation)
        val limiter = SpeedLimiter(item.speedLimitBytesPerSec)
        activeSpeedLimiters[item.id] = limiter
        val previous = activeJobs[item.id]?.takeIf { !it.isCompleted }
        val job = applicationScope.launch {
            if (previous != null) awaitSuperseded(item.id, previous)
            pendingAttempts.remove(item.id)
            persistDownload(owned)
            var attempt: DownloadItem? = owned
            while (attempt != null) {
                body(appContext, attempt)
                attempt = pendingAttempts.remove(item.id)?.takeIf { it.generation == generation && !isStale(it) }
            }
        }
        registerJob(item.id, job)
        activeJobs[item.id] = job
        job.invokeOnCompletion {
            activeJobs.remove(item.id, job)
            activeSpeedLimiters.remove(item.id, limiter)
            launchGenerations.remove(item.id, generation)
        }
    }

    private suspend fun awaitSuperseded(id: Int, previous: Job) {
        DownloadLog.i(id, "QUEUE") { "Stopping the previous attempt before starting again" }
        previous.cancel()
        var attempts = 0
        while (!previous.isCompleted && attempts < SUPERSEDE_WAIT_ATTEMPTS) {
            cancelCalls(id)
            kotlinx.coroutines.withTimeoutOrNull(200L) { previous.join() }
            attempts++
        }
    }

    private fun stopWorker(id: Int) {
        launchGenerations.remove(id)
        pendingAttempts.remove(id)
        pauseRequested.remove(id)
        activeJobs[id]?.cancel()
        cancelCalls(id)
    }

    private fun stopClock(item: DownloadItem): DownloadItem =
        if (item.activeStartedAt > 0L) {
            item.copy(
                activeElapsedMs = item.activeElapsedMs + (System.currentTimeMillis() - item.activeStartedAt),
                activeStartedAt = 0L
            )
        } else item

    fun enqueueStreamFromItem(
        context: Context,
        item: DownloadItem,
        filename: String = item.filename,
        speedLimitBytesPerSec: Long = item.speedLimitBytesPerSec,
        locationMode: String = item.locationMode,
        customLocationUri: String? = item.customLocationUri,
        categorizeEnabled: Boolean = item.categorizeEnabled,
        startPaused: Boolean = false
    ) {
        val primaryUrl = item.streamVideoUrl.ifBlank { item.url }
        val request = com.jhaiian.clint.mediacapture.download.StreamDownloadRequest(
            format = if (item.streamFormat == "DASH") com.jhaiian.clint.mediacapture.download.StreamContainerFormat.DASH
            else com.jhaiian.clint.mediacapture.download.StreamContainerFormat.HLS,
            videoUrl = if (item.streamPrimaryIsAudio) null else primaryUrl,
            audioUrl = if (item.streamPrimaryIsAudio) primaryUrl else item.streamAudioUrl,
            subtitleUrl = item.streamSubtitleUrl,
            pageUrl = item.streamPageUrl,
            referer = item.referer,
            cookies = item.cookies,
            userAgent = item.userAgent,
            filename = filename,
            headers = item.streamHeaders,
            estimatedTotalBytes = item.totalBytes.coerceAtLeast(0L),
            speedLimitBytesPerSec = speedLimitBytesPerSec,
            concurrentSegments = item.streamConcurrentSegments,
            noAudio = item.streamNoAudio,
            isLive = item.streamIsLive,
            convertTsToMp4 = item.streamConvertTsToMp4,
            subtitleOnly = item.streamFormat == com.jhaiian.clint.mediacapture.download.STREAM_FORMAT_HLS_SUBTITLE,
            audioLanguage = item.streamAudioLanguage,
            audioLabel = item.streamAudioLabel
        )
        enqueueStream(
            context = context,
            request = request,
            videoWidth = item.streamVideoWidth,
            videoHeight = item.streamVideoHeight,
            videoBandwidth = item.streamVideoBandwidth,
            audioBandwidth = item.streamAudioBandwidth,
            primaryIsAudio = item.streamPrimaryIsAudio,
            locationMode = locationMode.ifBlank { null },
            customLocationUri = customLocationUri,
            categorizeEnabled = categorizeEnabled,
            videoRepresentationId = item.streamVideoRepresentationId,
            audioRepresentationId = item.streamAudioRepresentationId,
            audioLanguage = item.streamAudioLanguage,
            audioLabel = item.streamAudioLabel,
            startPaused = startPaused
        )
    }

    fun redownloadItem(context: Context, item: DownloadItem) {
        DownloadLog.i(item.id, "USER") { "Redownload requested for ${item.filename}" }
        if (item.isStream) {
            enqueueStreamFromItem(context, item)
            return
        }
        val mega = item.mega
        if (mega != null) {
            enqueueMega(
                context, listOf(MegaDownloadEntry(mega, item.filename, item.totalBytes, item.url)), item.userAgent,
                retryEnabled = item.retryEnabled,
                unmeteredOnly = item.unmeteredOnly,
                splitParts = item.splitParts,
                multithreadingParts = item.multithreadingParts,
                speedLimitBytesPerSec = item.speedLimitBytesPerSec,
                locationMode = item.locationMode,
                customLocationUri = item.customLocationUri,
                categorizeEnabled = item.categorizeEnabled
            )
            return
        }
        enqueue(
            context, item.url, item.filename, item.userAgent,
            item.referer, item.cookies,
            retryEnabled = item.retryEnabled,
            unmeteredOnly = item.unmeteredOnly,
            splitParts = item.splitParts,
            multithreadingParts = item.multithreadingParts,
            speedLimitBytesPerSec = item.speedLimitBytesPerSec,
            locationMode = item.locationMode,
            customLocationUri = item.customLocationUri,
            pageUrl = item.pageUrl
        )
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
        audioRepresentationId: String? = null,
        audioLanguage: String? = null,
        audioLabel: String? = null,
        startPaused: Boolean = false
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
                enqueueStream(context, request, videoWidth, videoHeight, videoBandwidth, audioBandwidth, primaryIsAudio, locationMode, customLocationUri, categorizeEnabled, videoRepresentationId, audioRepresentationId, audioLanguage, audioLabel, startPaused)
            }
            return
        }
        if (requestStoragePermissionIfNeeded(context, effectiveLocationMode) {
                enqueueStream(context, request, videoWidth, videoHeight, videoBandwidth, audioBandwidth, primaryIsAudio, locationMode, customLocationUri, categorizeEnabled, videoRepresentationId, audioRepresentationId, audioLanguage, audioLabel, startPaused)
            }
        ) return

        val id = nextId(context)
        File(context.filesDir, "stream_downloads/$id").deleteRecursively()
        val queued = activeCount() >= concurrentLimit(context)
        val baseItem = DownloadItem(
            id = id,
            isIncognito = startedInIncognito(),
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
            pageUrl = request.pageUrl,
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
            streamAudioLanguage = audioLanguage,
            streamAudioLabel = audioLabel,
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
        if (startPaused) {
            val paused = item.copy(status = DownloadStatus.PAUSED)
            DownloadLog.i(id, "USER") { "Added to the list paused" }
            addNew(paused)
            persistLater(paused)
            return
        }
        addNew(item)

        if (queued) {
            DownloadNotificationHelper.showQueuedNotification(context, item)
            DownloadForegroundService.start(context)
            return
        }

        DownloadNotificationHelper.showProgressNotification(context, item)
        DownloadForegroundService.start(context)
        launchStreamDownload(context, item)
    }

    internal fun withLiveSettings(item: DownloadItem): DownloadItem {
        val current = downloadsFlow.value.find { it.id == item.id } ?: return item
        return item.copy(
            url = current.url,
            referer = current.referer,
            pageUrl = current.pageUrl,
            cookies = current.cookies,
            streamVideoUrl = current.streamVideoUrl,
            streamAudioUrl = current.streamAudioUrl,
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
        if (downloadsFlow.value.none { it.id == id }) return
        updateItem(id) {
            it.copy(
                retryEnabled = retryEnabled,
                unmeteredOnly = unmeteredOnly,
                speedLimitBytesPerSec = speedLimitBytesPerSec
            )
        }
        persistCurrent(id)
        activeSpeedLimiters[id]?.updateLimit(speedLimitBytesPerSec)
    }

    fun requestStoragePermissionIfNeeded(context: Context, locationMode: String, onGranted: () -> Unit): Boolean {
        if (locationMode == DownloadSettingsKeys.MODE_CUSTOM) return false
        if (!DownloadFileHelper.needsLegacyStoragePermission(context)) return false
        val activity = context as? com.jhaiian.clint.base.ClintActivity ?: return false
        val dialogHost = context as? com.jhaiian.clint.ui.ConfirmDialogHostActivity ?: return false
        dialogHost.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
            title = context.getString(R.string.download_storage_permission_title),
            message = context.getString(R.string.download_storage_permission_message),
            positiveLabel = context.getString(R.string.action_allow),
            onPositive = {
                activity.requestLegacyStoragePermission { granted ->
                    if (granted) onGranted() else showStoragePermissionDenied(activity, dialogHost)
                }
            },
            negativeLabel = context.getString(R.string.action_cancel)
        )
        return true
    }

    private fun showStoragePermissionDenied(context: Context, dialogHost: com.jhaiian.clint.ui.ConfirmDialogHostActivity) {
        dialogHost.confirmDialogConfig = com.jhaiian.clint.ui.listscreen.ConfirmDialogConfig(
            title = context.getString(R.string.download_storage_permission_title),
            message = context.getString(R.string.download_storage_permission_denied_message),
            positiveLabel = context.getString(R.string.action_open_settings),
            onPositive = {
                context.startActivity(
                    android.content.Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            },
            negativeLabel = context.getString(R.string.action_cancel)
        )
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

    fun enqueueBlob(context: Context, blob: File, filename: String, mimeType: String, startPaused: Boolean = false) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val locationMode = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_LOCATION_MODE, DownloadSettingsKeys.MODE_DEFAULT)
            ?: DownloadSettingsKeys.MODE_DEFAULT
        val customLocationUri = prefs.getString(DownloadSettingsKeys.PREF_DOWNLOAD_CUSTOM_URI, null)
        if (!DownloadFileHelper.isCustomLocationAccessible(context, locationMode, customLocationUri)) {
            promptInvalidLocation(context) { enqueueBlob(context, blob, filename, mimeType, startPaused) }
            return
        }
        if (requestStoragePermissionIfNeeded(context, locationMode) { enqueueBlob(context, blob, filename, mimeType, startPaused) }) return
        val id = nextId(context)
        val item = DownloadItem(
            id = id, url = "blob:", filename = filename, userAgent = "",
            startedAt = System.currentTimeMillis(), isIncognito = startedInIncognito()
        )
        if (startPaused) {
            addPausedBlob(context, item.copy(status = DownloadStatus.PAUSED, totalBytes = blob.length()), blob, mimeType)
            return
        }
        addNew(item)
        startBlobMove(context, item, blob, mimeType)
    }

    private fun addPausedBlob(context: Context, item: DownloadItem, blob: File, mimeType: String) {
        DownloadLog.i(item.id, "USER") { "Added to the list paused" }
        addNew(item)
        applicationScope.launch {
            val kept = withContext(Dispatchers.IO) { DownloadBlobStore.keepPaused(context, blob, item.id) }
            if (kept == null) {
                DownloadWorker.fail(context, item, context.getString(R.string.download_error_unknown), scheduleRetry = false)
                return@launch
            }
            if (isRemoved(item.id)) {
                kept.delete()
                return@launch
            }
            val head = withContext(Dispatchers.IO) { runCatching { kept.inputStream().use { DownloadFilenameResolver.readHead(it) } }.getOrNull() }
            val refined = DownloadFilenameResolver.refine(item.filename, mimeType, head)
            updateItem(item.id) { it.copy(file = kept, filename = refined) }
            persistCurrent(item.id)
        }
    }

    private fun resumePausedBlob(context: Context, item: DownloadItem) {
        val blob = item.file?.takeIf { DownloadBlobStore.isPausedBlob(context, it) && it.exists() }
        if (blob == null) {
            applicationScope.launch {
                DownloadWorker.fail(context, item, context.getString(R.string.download_error_blob_expired), scheduleRetry = false)
            }
            return
        }
        val started = item.copy(status = DownloadStatus.DOWNLOADING, file = null)
        publish(started)
        startBlobMove(context, started, blob, "")
    }

    private fun startBlobMove(context: Context, item: DownloadItem, blob: File, mimeType: String) {
        val id = item.id
        DownloadNotificationHelper.showProgressNotification(context, item)
        DownloadForegroundService.start(context)
        val directCustomDir = DownloadFileHelper.resolveDirectCustomDir(context)
        val safMode = DownloadFileHelper.isSafCustomMode(context) && directCustomDir == null
        val job = applicationScope.launch {
            var current = item
            var destination: File? = null
            try {
                val head = runCatching { blob.inputStream().use { DownloadFilenameResolver.readHead(it) } }.getOrNull()
                val finalFilename = DownloadFilenameResolver.refine(current.filename, mimeType, head)
                val size = blob.length()
                val rawDestDir = directCustomDir
                    ?: if (safMode) DownloadFileHelper.tempDownloadDir(context) else DownloadFileHelper.resolveDownloadDir()
                val destDir = if (safMode) rawDestDir else DownloadCategories.resolveDir(DownloadCategories.isEnabled(context), rawDestDir, finalFilename)
                destDir.mkdirs()
                val destFile = DownloadFileHelper.uniqueFile(destDir, finalFilename)
                destination = destFile
                DownloadBlobStore.moveTo(blob, destFile)
                current = current.copy(
                    filename = destFile.name,
                    file = destFile,
                    totalBytes = size,
                    bytesDownloaded = size
                )
                if (safMode) {
                    DownloadWorker.moveTempToSaf(context, current)
                } else {
                    current = current.copy(status = DownloadStatus.COMPLETE, completedAt = System.currentTimeMillis())
                    persistDownload(current)
                    publish(current)
                    onDownloadCompleted(context, current)
                    tryDequeueNext(context)
                }
            } catch (e: Throwable) {
                runCatching { blob.delete() }
                if (current.file == null) destination?.let { runCatching { it.delete() } }
                DownloadWorker.fail(context, current, e.message ?: context.getString(R.string.download_error_unknown), scheduleRetry = false)
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
            DownloadLog.i(updated.id, "SCHEDULE") { "Waiting for scheduled start at ${DownloadLog.formatTime(updated.scheduledStartAtMillis)}" }
            record(updated)
            persistLater(updated)
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
            DownloadLog.i(updated.id, "SCHEDULE") { "Outside the download schedule window; waiting" }
            record(updated)
            persistLater(updated)
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
            DownloadLog.i(updated.id, "NETWORK") { "Wi-Fi only download on a metered network; waiting for Wi-Fi" }
            record(updated)
            persistLater(updated)
            DownloadNotificationHelper.showWaitingUnmeteredNotification(context, updated)
            DownloadForegroundService.start(context)
            return
        }

        if (activeCount() >= concurrentLimit(context)) {
            val updated = current.copy(
                status = DownloadStatus.QUEUED, waitingForUnmetered = false, waitingForNetwork = false,
                waitingForCustomSchedule = false
            )
            DownloadLog.i(updated.id, "QUEUE") { "Queued \u00B7 ${activeCount()} of ${concurrentLimit(context)} download slots busy" }
            record(updated)
            persistLater(updated)
            DownloadNotificationHelper.showQueuedNotification(context, updated)
            DownloadForegroundService.start(context)
            return
        }

        val updated = current.copy(
            waitingForUnmetered = false, waitingForNetwork = false, waitingForCustomSchedule = false,
            status = DownloadStatus.CONNECTING, speedBytesPerSec = 0L
        )
        DownloadLog.i(updated.id, "START") { "Starting immediately (${if (updated.isStream) "stream" else "direct"} download)" }
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
        splitParts: Int = DownloadAutoTuner.AUTO,
        multithreadingParts: Int = DownloadAutoTuner.AUTO,
        speedLimitBytesPerSec: Long = 0L,
        locationMode: String = "default",
        customLocationUri: String? = null,
        scheduledStartAtMillis: Long = 0L,
        categorizeEnabled: Boolean = DownloadSettingsKeys.DEFAULT_CATEGORIZE_DOWNLOADS,
        pageUrl: String = "",
        startPaused: Boolean = false
    ) {
        if (!DownloadFileHelper.isCustomLocationAccessible(context, locationMode, customLocationUri)) {
            promptInvalidLocation(context) {
                enqueue(
                    context, url, filename, userAgent, referer, cookies, retryEnabled, unmeteredOnly,
                    splitParts, multithreadingParts, speedLimitBytesPerSec,
                    DownloadSettingsKeys.MODE_DEFAULT, null, scheduledStartAtMillis, categorizeEnabled, pageUrl, startPaused
                )
            }
            return
        }
        if (requestStoragePermissionIfNeeded(context, locationMode) {
                enqueue(
                    context, url, filename, userAgent, referer, cookies, retryEnabled, unmeteredOnly,
                    splitParts, multithreadingParts, speedLimitBytesPerSec,
                    locationMode, customLocationUri, scheduledStartAtMillis, categorizeEnabled, pageUrl, startPaused
                )
            }
        ) return
        val id = nextId(context)
        val baseItem = DownloadItem(
            id = id, isIncognito = startedInIncognito(), url = url, filename = filename, userAgent = userAgent, referer = referer, pageUrl = pageUrl,
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
        if (startPaused) {
            val paused = baseItem.copy(filename = previewName, status = DownloadStatus.PAUSED, scheduledStartAtMillis = 0L)
            DownloadLog.i(id, "USER") { "Added to the list paused" }
            addNew(paused)
            persistLater(paused)
            return
        }
        dispatchOrQueue(context, baseItem.copy(filename = previewName), ::addNew)
    }

    fun enqueueMega(
        context: Context,
        entries: List<MegaDownloadEntry>,
        userAgent: String,
        retryEnabled: Boolean = true,
        unmeteredOnly: Boolean = false,
        splitParts: Int = DownloadAutoTuner.AUTO,
        multithreadingParts: Int = DownloadAutoTuner.AUTO,
        speedLimitBytesPerSec: Long = 0L,
        locationMode: String = "default",
        customLocationUri: String? = null,
        scheduledStartAtMillis: Long = 0L,
        categorizeEnabled: Boolean = DownloadSettingsKeys.DEFAULT_CATEGORIZE_DOWNLOADS,
        startPaused: Boolean = false
    ) {
        if (entries.isEmpty()) return
        fun enqueueAt(mode: String, uri: String?) = enqueueMega(
            context, entries, userAgent, retryEnabled, unmeteredOnly, splitParts, multithreadingParts,
            speedLimitBytesPerSec, mode, uri, scheduledStartAtMillis, categorizeEnabled, startPaused
        )
        if (!DownloadFileHelper.isCustomLocationAccessible(context, locationMode, customLocationUri)) {
            promptInvalidLocation(context) { enqueueAt(DownloadSettingsKeys.MODE_DEFAULT, null) }
            return
        }
        if (requestStoragePermissionIfNeeded(context, locationMode) { enqueueAt(locationMode, customLocationUri) }) return
        for (entry in entries) {
            val source = entry.source.copy(path = MegaPaths.clean(entry.source.path))
            val filename = DownloadFileHelper.sanitizeFileName(entry.filename)
            val baseItem = DownloadItem(
                id = nextId(context), isIncognito = startedInIncognito(), url = entry.url.ifBlank { source.link }, filename = filename, userAgent = userAgent,
                totalBytes = entry.size, resumable = entry.size > 0L, retryEnabled = retryEnabled, unmeteredOnly = unmeteredOnly,
                splitParts = splitParts, multithreadingParts = multithreadingParts,
                speedLimitBytesPerSec = speedLimitBytesPerSec,
                locationMode = locationMode, customLocationUri = customLocationUri,
                categorizeEnabled = categorizeEnabled && source.path.isEmpty(),
                startedAt = System.currentTimeMillis(), scheduledStartAtMillis = scheduledStartAtMillis,
                mega = source
            )
            val directCustomDir = DownloadFileHelper.resolveDirectCustomDir(context, baseItem)
            val safMode = directCustomDir == null && DownloadFileHelper.isSafCustomMode(context, baseItem)
            val rawDestDir = directCustomDir ?: if (safMode) DownloadFileHelper.tempDownloadDir(context) else DownloadFileHelper.resolveDownloadDir()
            val destDir = if (safMode) rawDestDir else MegaPaths.localDir(DownloadCategories.resolveDir(baseItem.categorizeEnabled, rawDestDir, filename), baseItem)
            destDir.mkdirs()
            val previewName = DownloadFileHelper.previewUniqueName(destDir, filename)
            if (startPaused) {
                val paused = baseItem.copy(filename = previewName, status = DownloadStatus.PAUSED, scheduledStartAtMillis = 0L)
                DownloadLog.i(paused.id, "USER") { "Added to the list paused" }
                addNew(paused)
                persistLater(paused)
            } else {
                dispatchOrQueue(context, baseItem.copy(filename = previewName), ::addNew)
            }
        }
    }

    internal fun updateResolvedUrl(id: Int, url: String) {
        updateItem(id) { it.copy(url = url) }
        persistCurrent(id)
    }

    fun queuedInOrder(items: List<DownloadItem> = downloadsFlow.value): List<DownloadItem> =
        items.filter { it.status == DownloadStatus.QUEUED }.sortedWith(compareBy({ it.queueOrder }, { it.id }))

    fun moveInQueue(id: Int, offset: Int) {
        val queue = queuedInOrder().toMutableList()
        val from = queue.indexOfFirst { it.id == id }
        if (from < 0 || queue.size < 2) return
        val to = (from.toLong() + offset).coerceIn(0L, queue.lastIndex.toLong()).toInt()
        if (to == from) return
        queue.add(to, queue.removeAt(from))
        val slots = queue.map { it.queueOrder }.sorted().toMutableList()
        for (i in 1 until slots.size) {
            if (slots[i] <= slots[i - 1]) slots[i] = slots[i - 1] + 1
        }
        DownloadLog.i(id, "QUEUE") { "Moved in queue from position ${from + 1} to ${to + 1} of ${queue.size}" }
        val newOrders = queue.mapIndexed { index, item -> item.id to slots[index] }.toMap()
        _downloads.update { list -> list.map { item -> newOrders[item.id]?.let { item.copy(queueOrder = it) } ?: item } }
        val changed = downloadsFlow.value.filter { it.id in newOrders }
        applicationScope.launch { changed.forEach { persistDownload(it) } }
    }

    fun moveToFrontOfQueue(id: Int) = moveInQueue(id, Int.MIN_VALUE)

    fun cancel(context: Context, id: Int) {
        remove(context, id)
    }

    fun pause(context: Context, id: Int) {
        val item = downloadsFlow.value.find { it.id == id } ?: return
        DownloadLog.i(id, "USER") { "Pause requested while ${item.status.name.lowercase()}" }

        if (item.status == DownloadStatus.QUEUED) {
            val updated = item.copy(status = DownloadStatus.PAUSED, waitingForUnmetered = false)
            publish(updated)
            persistLater(updated)
            return
        }

        pauseRequested.add(id)

        if (item.status == DownloadStatus.RETRYING || item.status == DownloadStatus.CONNECTING) {
            stopWorker(id)
            val waitingForSchedule = id in DownloadScheduleMonitor.scheduleWaitingIds
            val updated = stopClock(item).copy(
                status = DownloadStatus.PAUSED, retryDelaySec = 0, retryAttempt = 0, waitingForUnmetered = false,
                waitingForSchedule = waitingForSchedule, speedBytesPerSec = 0L
            )
            DownloadNetworkMonitor.unmeteredPausedIds.remove(id)
            publish(updated)
            persistLater(updated)
            context.getSystemService(NotificationManager::class.java).cancel(id)
            if (waitingForSchedule) {
                DownloadNotificationHelper.showWaitingScheduleNotification(context, updated)
            } else {
                DownloadNotificationHelper.showPausedNotification(context, updated)
            }
            tryDequeueNext(context)
        } else if (item.waitingForUnmetered) {
            DownloadNetworkMonitor.unmeteredPausedIds.remove(id)
            val updated = item.copy(waitingForUnmetered = false, status = DownloadStatus.PAUSED)
            context.getSystemService(NotificationManager::class.java).cancel(id)
            publish(updated)
            persistLater(updated)
        } else if (item.waitingForNetwork) {
            DownloadNetworkMonitor.networkWaitingIds.remove(id)
            val updated = item.copy(waitingForNetwork = false, status = DownloadStatus.PAUSED)
            context.getSystemService(NotificationManager::class.java).cancel(id)
            publish(updated)
            persistLater(updated)
        } else if (item.waitingForSchedule) {
            DownloadScheduleMonitor.scheduleWaitingIds.remove(id)
            val updated = item.copy(waitingForSchedule = false, status = DownloadStatus.PAUSED)
            context.getSystemService(NotificationManager::class.java).cancel(id)
            publish(updated)
            persistLater(updated)
        } else if (item.waitingForCustomSchedule) {
            DownloadCustomScheduleMonitor.cancel(context, id)
            val updated = item.copy(waitingForCustomSchedule = false, status = DownloadStatus.PAUSED)
            context.getSystemService(NotificationManager::class.java).cancel(id)
            publish(updated)
            persistLater(updated)
        }
    }

    internal fun pauseForUnmeteredWait(context: Context, id: Int) {
        val item = downloadsFlow.value.find { it.id == id } ?: return
        if (item.status !in DownloadStatus.ACTIVELY_WORKING) return
        pauseRequested.add(id)
        if (item.status == DownloadStatus.RETRYING || item.status == DownloadStatus.CONNECTING) {
            stopWorker(id)
            val updated = stopClock(item).copy(
                status = DownloadStatus.PAUSED, waitingForUnmetered = true,
                retryDelaySec = 0, retryAttempt = 0, speedBytesPerSec = 0L
            )
            context.getSystemService(NotificationManager::class.java).cancel(id)
            publish(updated)
            persistLater(updated)
            DownloadNotificationHelper.showWaitingUnmeteredNotification(context, updated)
            tryDequeueNext(context)
        }
    }

    fun resume(context: Context, id: Int) {
        val item = downloadsFlow.value.find { it.id == id } ?: return
        if (item.status != DownloadStatus.PAUSED) return
        DownloadLog.i(id, "USER") { "Resume requested" }
        pauseRequested.remove(id)
        timeoutPausedIds.remove(id)
        DownloadNetworkMonitor.unmeteredPausedIds.remove(id)
        DownloadNetworkMonitor.networkWaitingIds.remove(id)
        DownloadScheduleMonitor.scheduleWaitingIds.remove(id)
        DownloadCustomScheduleMonitor.cancel(context, id)
        if (item.url == "blob:") {
            resumePausedBlob(context, item)
            return
        }
        dispatchOrQueue(context, item, ::publish)
    }

    fun remove(context: Context, id: Int, deleteFile: Boolean = false) {
        DownloadLog.i(id, "USER") { "Removed from list" + if (deleteFile) " and file deleted" else "" }
        DownloadLog.deleteItemLog(id)
        removedIds.add(id)
        launchGenerations.remove(id)
        pendingAttempts.remove(id)
        lastActivityAt.remove(id)
        pauseRequested.remove(id)
        timeoutPausedIds.remove(id)
        DownloadNetworkMonitor.unmeteredPausedIds.remove(id)
        DownloadNetworkMonitor.networkWaitingIds.remove(id)
        DownloadScheduleMonitor.scheduleWaitingIds.remove(id)
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
            val leftover = item?.file
            if (leftover != null && appCtx != null &&
                (DownloadFileHelper.isInTempDir(appCtx, leftover) || DownloadFileHelper.isPartFile(leftover) || DownloadBlobStore.isPausedBlob(appCtx, leftover))
            ) {
                runCatching { leftover.delete() }
            }
            val partialDocument = item?.takeIf { it.file == null && it.status != DownloadStatus.COMPLETE }?.contentUri
            if (partialDocument != null && appCtx != null) SafDirectWriter.deleteIfPart(appCtx, Uri.parse(partialDocument))
            appCtx?.let { File(it.filesDir, "stream_downloads/$id").deleteRecursively() }
            deletePersistedDownload(id)
            removedIds.remove(id)
        }
    }

    fun clearCompleted() {
        var cleared: List<DownloadItem> = emptyList()
        _downloads.update { list ->
            val (keep, remove) = list.partition { it.status in DownloadStatus.NOT_FINISHED }
            cleared = remove
            keep
        }
        applicationScope.launch {
            val ctx = appContext
            cleared.forEach { item ->
                deletePersistedDownload(item.id)
                ctx?.let { File(it.filesDir, "stream_downloads/${item.id}").deleteRecursively() }
                val leftover = item.file
                if (item.status == DownloadStatus.FAILED && leftover != null && ctx != null &&
                    (DownloadFileHelper.isInTempDir(ctx, leftover) || DownloadFileHelper.isPartFile(leftover))
                ) {
                    runCatching { leftover.delete() }
                }
                val partialDocument = item.takeIf { it.status == DownloadStatus.FAILED && it.file == null }?.contentUri
                if (partialDocument != null && ctx != null) SafDirectWriter.deleteIfPart(ctx, Uri.parse(partialDocument))
            }
        }
    }

    fun retryFailed(context: Context, id: Int) {
        DownloadLog.i(id, "USER") { "Manual retry requested" }
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

    fun updateDownloadUrl(id: Int, newUrl: String, referer: String? = null, cookies: String? = null, pageUrl: String? = null) {
        updateItem(id) { current ->
            val withUrl = if (current.isStream) current.copy(url = newUrl, streamVideoUrl = newUrl) else current.copy(url = newUrl)
            withUrl.copy(
                referer = referer?.takeIf { it.isNotBlank() } ?: withUrl.referer,
                cookies = cookies?.takeIf { it.isNotBlank() } ?: withUrl.cookies,
                pageUrl = pageUrl?.takeIf { it.isNotBlank() } ?: withUrl.pageUrl
            )
        }
        DownloadLog.i(id, "USER") { "Download link updated" }
        persistCurrent(id)
    }

    fun updateStreamUrls(id: Int, primaryUrl: String?, audioUrl: String?) {
        updateItem(id) { current ->
            if (!current.isStream) return@updateItem current
            var next = current
            if (primaryUrl != null) next = next.copy(url = primaryUrl, streamVideoUrl = primaryUrl)
            if (audioUrl != null && next.streamAudioUrl != null && !next.streamPrimaryIsAudio) next = next.copy(streamAudioUrl = audioUrl)
            next
        }
        DownloadLog.i(id, "USER") { "Stream links updated" }
        persistCurrent(id)
    }

    fun onUnmeteredOnlyChanged(context: Context, enabled: Boolean) {
        DownloadNetworkMonitor.onUnmeteredOnlyChanged(context, enabled)
    }
}

class MegaDownloadEntry(val source: MegaSource, val filename: String, val size: Long, val url: String = "")
