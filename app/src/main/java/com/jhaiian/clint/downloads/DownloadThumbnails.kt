package com.jhaiian.clint.downloads

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import com.caverock.androidsvg.SVG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.Collections
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

enum class DownloadFileKind { VIDEO, AUDIO, IMAGE, DOCUMENT, ARCHIVE, APP, OTHER }

internal fun downloadFileKind(filename: String): DownloadFileKind = when (DownloadCategories.categoryForFilename(filename)) {
    DownloadCategories.CATEGORY_VIDEOS -> DownloadFileKind.VIDEO
    DownloadCategories.CATEGORY_AUDIO -> DownloadFileKind.AUDIO
    DownloadCategories.CATEGORY_IMAGES -> DownloadFileKind.IMAGE
    DownloadCategories.CATEGORY_DOCUMENTS -> DownloadFileKind.DOCUMENT
    DownloadCategories.CATEGORY_ARCHIVES -> DownloadFileKind.ARCHIVE
    DownloadCategories.CATEGORY_APPS -> DownloadFileKind.APP
    else -> DownloadFileKind.OTHER
}

internal class DownloadThumbnail(
    val bitmap: ImageBitmap?,
    val durationMs: Long,
    val width: Int = 0,
    val height: Int = 0,
    val title: String? = null,
    val artist: String? = null,
    val pageCount: Int = 0,
    val appLabel: String? = null,
    val appVersion: String? = null
)

private enum class ThumbnailSource { IMAGE, SVG, VIDEO, AUDIO, APK, APK_BUNDLE, PDF }

private fun thumbnailSourceFor(filename: String): ThumbnailSource? {
    val ext = filename.substringAfterLast('.', "").lowercase(Locale.US)
    return when {
        ext == "svg" -> ThumbnailSource.SVG
        ext == "apk" -> ThumbnailSource.APK
        ext in DownloadThumbnailLoader.APK_BUNDLE_EXTENSIONS -> ThumbnailSource.APK_BUNDLE
        ext == "pdf" -> ThumbnailSource.PDF
        ext in DownloadThumbnailLoader.SUBTITLE_EXTENSIONS -> null
        else -> when (downloadFileKind(filename)) {
            DownloadFileKind.IMAGE -> ThumbnailSource.IMAGE
            DownloadFileKind.VIDEO -> ThumbnailSource.VIDEO
            DownloadFileKind.AUDIO -> ThumbnailSource.AUDIO
            else -> null
        }
    }
}

internal object DownloadThumbnailLoader {

    val SUBTITLE_EXTENSIONS = setOf("srt", "vtt", "ass", "sub", "ssa", "sbv", "smi")
    val APK_BUNDLE_EXTENSIONS = setOf("apks", "apkm", "xapk", "apkz")
    private const val BUNDLE_ICON_PX = 256

    private val cache = object : LruCache<String, DownloadThumbnail>(
        (Runtime.getRuntime().maxMemory() / 1024L / 24L).toInt().coerceIn(4 * 1024, 48 * 1024)
    ) {
        override fun sizeOf(key: String, value: DownloadThumbnail): Int =
            ((value.bitmap?.let { it.width * it.height * 4 } ?: 0) / 1024).coerceAtLeast(1)
    }
    private val bundleCache = LruCache<String, DownloadThumbnail>(12)
    private val misses: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())
    private val gate = Semaphore(3)

    fun keyFor(item: DownloadItem): String? {
        if (item.status != DownloadStatus.COMPLETE) return null
        thumbnailSourceFor(item.filename) ?: return null
        val location = item.file?.absolutePath ?: item.contentUri ?: return null
        return "$location|${item.filename}|${item.bytesDownloaded}|${item.completedAt}"
    }

    fun keyFor(item: DownloadItem, sizePx: Int): String? = keyFor(item)?.let { "$it|$sizePx" }

    fun cached(key: String): DownloadThumbnail? = cache.get(key)

    suspend fun load(context: Context, item: DownloadItem, key: String, sizePx: Int): DownloadThumbnail? {
        cache.get(key)?.let { return it }
        if (key in misses) return null
        val source = thumbnailSourceFor(item.filename) ?: return null
        return withContext(Dispatchers.IO) {
            gate.withPermit {
                cache.get(key) ?: runCatching { decode(context, item, source, sizePx) }.getOrNull().also { result ->
                    if (result == null) misses += key else cache.put(key, result)
                }
            }
        }
    }

    private fun decode(context: Context, item: DownloadItem, source: ThumbnailSource, sizePx: Int): DownloadThumbnail? {
        val file = item.file?.takeIf { it.canRead() }
        val uri = if (file == null) item.contentUri?.let(Uri::parse) ?: return null else null
        return when (source) {
            ThumbnailSource.IMAGE -> {
                val size = imageSize(context, file, uri)
                decodeImage(context, file, uri, sizePx)?.let { DownloadThumbnail(it.asImageBitmap(), 0L, size.first, size.second) }
            }
            ThumbnailSource.SVG -> decodeSvg(context, file, uri, sizePx)?.let { DownloadThumbnail(it.asImageBitmap(), 0L) }
            ThumbnailSource.VIDEO, ThumbnailSource.AUDIO -> decodeMedia(context, file, uri, source == ThumbnailSource.VIDEO, sizePx)
            ThumbnailSource.APK -> withLocalPath(context, file, uri) { decodeApk(context, it, sizePx) }
            ThumbnailSource.APK_BUNDLE -> {
                val bundleKey = keyFor(item) ?: return null
                bundleCache.get(bundleKey) ?: withLocalPath(context, file, uri) { decodeApkBundle(context, it, BUNDLE_ICON_PX) }
                    ?.also { bundleCache.put(bundleKey, it) }
            }
            ThumbnailSource.PDF -> decodePdf(context, file, uri, sizePx)
        }
    }

    private fun open(context: Context, file: File?, uri: Uri?): InputStream? =
        file?.inputStream() ?: uri?.let { context.contentResolver.openInputStream(it) }

    private fun sampleSize(width: Int, height: Int, target: Int): Int {
        var sample = 1
        val shortest = minOf(width, height)
        while (shortest / (sample * 2) >= target) sample *= 2
        return sample
    }

    private fun decodeImage(context: Context, file: File?, uri: Uri?, sizePx: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val decoderSource = file?.let { ImageDecoder.createSource(it) }
                ?: uri?.let { ImageDecoder.createSource(context.contentResolver, it) } ?: return null
            return runCatching {
                ImageDecoder.decodeBitmap(decoderSource) { decoder, info, _ ->
                    decoder.setTargetSampleSize(sampleSize(info.size.width, info.size.height, sizePx))
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            }.getOrNull() ?: decodeImageLegacy(context, file, uri, sizePx)
        }
        return decodeImageLegacy(context, file, uri, sizePx)
    }

    private fun imageSize(context: Context, file: File?, uri: Uri?): Pair<Int, Int> = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(context, file, uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        bounds.outWidth.coerceAtLeast(0) to bounds.outHeight.coerceAtLeast(0)
    }.getOrDefault(0 to 0)

    private fun decodeImageLegacy(context: Context, file: File?, uri: Uri?, sizePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val stream = open(context, file, uri) ?: return null
        stream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, sizePx) }
        return open(context, file, uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun decodeBytes(bytes: ByteArray, sizePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, sizePx) }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun decodeSvg(context: Context, file: File?, uri: Uri?, sizePx: Int): Bitmap? {
        val svg = open(context, file, uri)?.use { SVG.getFromInputStream(it) } ?: return null
        val vb = svg.documentViewBox
        val rawW = vb?.width()?.takeIf { it > 0f } ?: svg.documentWidth.takeIf { it > 0f } ?: sizePx.toFloat()
        val rawH = vb?.height()?.takeIf { it > 0f } ?: svg.documentHeight.takeIf { it > 0f } ?: sizePx.toFloat()
        if (vb == null) svg.setDocumentViewBox(0f, 0f, rawW, rawH)
        val scale = sizePx / maxOf(rawW, rawH)
        val width = (rawW * scale).toInt().coerceAtLeast(1)
        val height = (rawH * scale).toInt().coerceAtLeast(1)
        svg.setDocumentWidth(width.toFloat())
        svg.setDocumentHeight(height.toFloat())
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        svg.renderToCanvas(Canvas(bitmap))
        return bitmap
    }

    private fun decodeMedia(context: Context, file: File?, uri: Uri?, isVideo: Boolean, sizePx: Int): DownloadThumbnail? {
        val retriever = MediaMetadataRetriever()
        try {
            when {
                file != null -> retriever.setDataSource(file.absolutePath)
                uri != null -> retriever.setDataSource(context, uri)
                else -> return null
            }
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val bitmap = if (isVideo) {
                val timeUs = if (durationMs > 0L) (durationMs * 1000L / 5L).coerceAtMost(15_000_000L) else -1L
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, sizePx * 2, sizePx * 2)
                } else {
                    retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { scaleDown(it, sizePx * 2) }
                }
            } else {
                retriever.embeddedPicture?.let { decodeBytes(it, sizePx) }
            }
            if (bitmap == null && durationMs <= 0L) return null
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val rawWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val swap = rotation == 90 || rotation == 270
            return DownloadThumbnail(
                bitmap = bitmap?.asImageBitmap(),
                durationMs = durationMs,
                width = if (swap) rawHeight else rawWidth,
                height = if (swap) rawWidth else rawHeight,
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() },
                artist = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST))?.takeIf { it.isNotBlank() }
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun scaleDown(bitmap: Bitmap, maxPx: Int): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= maxPx) return bitmap
        val ratio = maxPx.toFloat() / largest
        val scaled = bitmap.scale((bitmap.width * ratio).toInt().coerceAtLeast(1), (bitmap.height * ratio).toInt().coerceAtLeast(1))
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }

    private fun <T> withLocalPath(context: Context, file: File?, uri: Uri?, block: (String) -> T?): T? {
        if (file != null) return block(file.absolutePath)
        val descriptor = uri?.let { context.contentResolver.openFileDescriptor(it, "r") } ?: return null
        return descriptor.use { block("/proc/self/fd/${it.fd}") }
    }

    private fun decodeApk(context: Context, path: String, sizePx: Int, splitPaths: List<String> = emptyList()): DownloadThumbnail? {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(path, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(path, 0)
        } ?: return null
        val appInfo = info.applicationInfo ?: return null
        appInfo.sourceDir = path
        appInfo.publicSourceDir = path
        if (splitPaths.isNotEmpty()) {
            appInfo.splitSourceDirs = splitPaths.toTypedArray()
            appInfo.splitPublicSourceDirs = splitPaths.toTypedArray()
        }
        return DownloadThumbnail(
            bitmap = appInfo.loadIcon(pm).toBitmap(sizePx, sizePx).asImageBitmap(),
            durationMs = 0L,
            appLabel = appInfo.loadLabel(pm).toString().takeIf { it.isNotBlank() },
            appVersion = info.versionName?.takeIf { it.isNotBlank() }
        )
    }

    private fun decodeApkBundle(context: Context, path: String, sizePx: Int): DownloadThumbnail? = ZipFile(path).use { zip ->
        val (metaLabel, metaVersion) = bundleMetadata(zip)
        val bundledIcon = zip.getEntry("icon.png")
            ?.let { entry -> zip.getInputStream(entry).use { it.readBytes() } }
            ?.let { decodeBytes(it, sizePx) }
        if (bundledIcon != null && metaLabel != null) {
            return@use DownloadThumbnail(bundledIcon.asImageBitmap(), 0L, appLabel = metaLabel, appVersion = metaVersion)
        }
        val apks = zip.entries().asSequence()
            .filter { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }
            .toList()
        val base = apks.firstOrNull { entryName(it) == "base.apk" }
            ?: apks.filter { !isSplitApk(it) }.maxByOrNull { it.size }
        val fromApk = base?.let { baseEntry ->
            val workDir = File(context.cacheDir, "apk_bundle_thumbs/${UUID.randomUUID()}").apply { mkdirs() }
            try {
                val basePath = extractEntry(zip, baseEntry, File(workDir, "base.apk"))
                val splitPaths = apks
                    .filter { it != baseEntry && isSplitApk(it) && "dpi" in entryName(it) }
                    .mapIndexed { index, entry -> extractEntry(zip, entry, File(workDir, "split_$index.apk")) }
                decodeApk(context, basePath, sizePx, splitPaths)
            } finally {
                workDir.deleteRecursively()
            }
        }
        val bitmap = bundledIcon?.asImageBitmap() ?: fromApk?.bitmap
        val label = fromApk?.appLabel ?: metaLabel
        if (bitmap == null && label == null) return@use null
        DownloadThumbnail(bitmap, 0L, appLabel = label, appVersion = fromApk?.appVersion ?: metaVersion)
    }

    private fun entryName(entry: ZipEntry): String = entry.name.substringAfterLast('/').lowercase(Locale.US)

    private fun isSplitApk(entry: ZipEntry): Boolean = entryName(entry).let { it.startsWith("split_") || it.startsWith("config.") }

    private fun extractEntry(zip: ZipFile, entry: ZipEntry, target: File): String {
        zip.getInputStream(entry).use { input -> target.outputStream().use { input.copyTo(it, 256 * 1024) } }
        return target.absolutePath
    }

    private fun bundleMetadata(zip: ZipFile): Pair<String?, String?> {
        val json = listOf("manifest.json", "info.json").firstNotNullOfOrNull { name ->
            zip.getEntry(name)?.takeIf { it.size in 0..1_048_576 }?.let { entry ->
                runCatching { JSONObject(zip.getInputStream(entry).bufferedReader().use { it.readText() }) }.getOrNull()
            }
        } ?: return null to null
        fun pick(vararg keys: String): String? = keys.firstNotNullOfOrNull { key -> json.optString(key).takeIf { it.isNotBlank() } }
        return pick("name", "app_name", "appname", "label") to pick("version_name", "versionname", "release_version")
    }

    private fun decodePdf(context: Context, file: File?, uri: Uri?, sizePx: Int): DownloadThumbnail? {
        val descriptor = file?.let { ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY) }
            ?: uri?.let { context.contentResolver.openFileDescriptor(it, "r") } ?: return null
        val pdfRenderer = runCatching { PdfRenderer(descriptor) }.getOrElse {
            descriptor.close()
            return null
        }
        return pdfRenderer.use { renderer ->
            if (renderer.pageCount <= 0) return@use null
            renderer.openPage(0).use { page ->
                val scale = (sizePx * 2f) / maxOf(page.width, page.height).coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(
                    (page.width * scale).toInt().coerceAtLeast(1),
                    (page.height * scale).toInt().coerceAtLeast(1),
                    Bitmap.Config.ARGB_8888
                )
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                DownloadThumbnail(bitmap.asImageBitmap(), 0L, pageCount = renderer.pageCount)
            }
        }
    }
}

@Composable
internal fun rememberDownloadThumbnail(item: DownloadItem, sizePx: Int): DownloadThumbnail? {
    val context = LocalContext.current.applicationContext
    val key = remember(item.status, item.file, item.contentUri, item.filename, item.bytesDownloaded, item.completedAt, sizePx) {
        DownloadThumbnailLoader.keyFor(item, sizePx)
    }
    val thumbnail by produceState(initialValue = key?.let(DownloadThumbnailLoader::cached), key) {
        if (key == null) {
            value = null
            return@produceState
        }
        value = DownloadThumbnailLoader.cached(key) ?: DownloadThumbnailLoader.load(context, item, key, sizePx)
    }
    return thumbnail
}
