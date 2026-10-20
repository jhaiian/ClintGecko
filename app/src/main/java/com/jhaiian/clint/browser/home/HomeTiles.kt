package com.jhaiian.clint.browser.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceManager
import com.caverock.androidsvg.SVG
import com.jhaiian.clint.bookmarks.BookmarkManager
import com.jhaiian.clint.settings.datasaver.DataSaverMode
import com.jhaiian.clint.ui.rememberClintFavicon
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Collections
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal sealed class HomeTile {
    abstract val id: String
}

internal data class HomeSiteTile(
    override val id: String,
    val url: String,
    val title: String,
    val customIcon: Boolean = false
) : HomeTile()

internal data class HomeFolderTile(
    override val id: String,
    val name: String,
    val sites: List<HomeSiteTile>
) : HomeTile()

internal fun newHomeTileId(): String = UUID.randomUUID().toString().replace("-", "")

internal fun homeTileHost(url: String): String =
    runCatching { Uri.parse(url).host.orEmpty().removePrefix("www.") }.getOrDefault("")

internal fun HomeSiteTile.label(): String = title.ifBlank { homeTileHost(url) }.ifBlank { url }

internal fun List<HomeTile>.allSites(): List<HomeSiteTile> = flatMap {
    when (it) {
        is HomeSiteTile -> listOf(it)
        is HomeFolderTile -> it.sites
    }
}

internal fun List<HomeTile>.findSite(id: String): HomeSiteTile? = allSites().firstOrNull { it.id == id }

internal fun List<HomeTile>.folderOf(siteId: String): HomeFolderTile? =
    filterIsInstance<HomeFolderTile>().firstOrNull { folder -> folder.sites.any { it.id == siteId } }

private fun List<HomeTile>.dropEmptyFolders(): List<HomeTile> = filter { it !is HomeFolderTile || it.sites.isNotEmpty() }

internal fun List<HomeTile>.removeSite(id: String): List<HomeTile> = mapNotNull { tile ->
    when (tile) {
        is HomeSiteTile -> tile.takeIf { it.id != id }
        is HomeFolderTile -> tile.copy(sites = tile.sites.filter { it.id != id })
    }
}.dropEmptyFolders()

internal fun List<HomeTile>.replaceSite(site: HomeSiteTile): List<HomeTile> = map { tile ->
    when (tile) {
        is HomeSiteTile -> if (tile.id == site.id) site else tile
        is HomeFolderTile -> tile.copy(sites = tile.sites.map { if (it.id == site.id) site else it })
    }
}

internal fun List<HomeTile>.replaceFolder(folder: HomeFolderTile): List<HomeTile> =
    map { if (it.id == folder.id) folder else it }.dropEmptyFolders()

internal fun List<HomeTile>.ungroup(folderId: String): List<HomeTile> = flatMap { tile ->
    if (tile is HomeFolderTile && tile.id == folderId) tile.sites else listOf(tile)
}

internal fun List<HomeTile>.moveSiteToFolder(siteId: String, folderId: String, newFolderName: String): List<HomeTile> {
    val site = findSite(siteId) ?: return this
    val anchor = indexOfFirst { it.id == siteId || (it is HomeFolderTile && it.sites.any { s -> s.id == siteId }) }
    val without = removeSite(siteId)
    if (without.none { it.id == folderId }) {
        val folder = HomeFolderTile(folderId, newFolderName, listOf(site))
        return without.toMutableList().apply { add(anchor.coerceIn(0, size), folder) }
    }
    return without.map { if (it is HomeFolderTile && it.id == folderId) it.copy(sites = it.sites + site) else it }
}

internal fun List<HomeTile>.moveSiteOutOfFolder(siteId: String): List<HomeTile> {
    val folder = folderOf(siteId) ?: return this
    val site = folder.sites.first { it.id == siteId }
    val updated = removeSite(siteId).toMutableList()
    val folderIndex = updated.indexOfFirst { it.id == folder.id }
    updated.add(if (folderIndex >= 0) folderIndex + 1 else updated.size, site)
    return updated
}

internal fun List<HomeTile>.mergeTiles(draggedId: String, targetId: String, newFolderName: String): List<HomeTile> {
    val dragged = firstOrNull { it.id == draggedId } as? HomeSiteTile ?: return this
    val target = firstOrNull { it.id == targetId } ?: return this
    val merged: HomeTile = when (target) {
        is HomeSiteTile -> HomeFolderTile(newHomeTileId(), newFolderName, listOf(target, dragged))
        is HomeFolderTile -> target.copy(sites = target.sites + dragged)
    }
    return mapNotNull {
        when (it.id) {
            draggedId -> null
            targetId -> merged
            else -> it
        }
    }
}

internal object HomeTileStore {
    private const val FILE_NAME = "home_tiles.json"
    private const val ICON_PREFIX = "tile_icon_"
    private const val ICON_MAX_SIDE = 256
    private const val SEED_LIMIT = 8

    private val writer = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var cache: List<HomeTile>? = null

    private fun dataFile(context: Context): File = File(homepageImageDir(context), FILE_NAME)

    private fun iconFile(context: Context, id: String): File = File(homepageImageDir(context), "$ICON_PREFIX$id.png")

    @Synchronized
    fun load(context: Context): List<HomeTile> {
        cache?.let { return it }
        val file = dataFile(context)
        val loaded = if (!file.exists()) {
            seed(context).also { save(context, it) }
        } else {
            runCatching { parse(JSONArray(file.readText())) }.getOrElse { emptyList() }
        }
        cache = loaded
        return loaded
    }

    fun saveAsync(context: Context, tiles: List<HomeTile>, icon: Pair<String, Bitmap>? = null, onDone: (() -> Unit)? = null) {
        val appContext = context.applicationContext
        cache = tiles
        writer.execute {
            icon?.let { saveIcon(appContext, it.first, it.second) }
            save(appContext, tiles)
            onDone?.let { mainHandler.post(it) }
        }
    }

    @Synchronized
    private fun save(context: Context, tiles: List<HomeTile>) {
        runCatching {
            val dir = homepageImageDir(context)
            dir.mkdirs()
            val temp = File(dir, "$FILE_NAME.tmp")
            temp.writeText(serialize(tiles).toString())
            if (!temp.renameTo(dataFile(context))) {
                temp.copyTo(dataFile(context), overwrite = true)
                temp.delete()
            }
            val keep = tiles.allSites().filter { it.customIcon }.map { "$ICON_PREFIX${it.id}.png" }.toSet()
            dir.listFiles()?.forEach { if (it.name.startsWith(ICON_PREFIX) && it.name !in keep) it.delete() }
        }
    }

    private fun saveIcon(context: Context, id: String, bitmap: Bitmap): Boolean = runCatching {
        val dir = homepageImageDir(context)
        dir.mkdirs()
        val scale = ICON_MAX_SIDE.toFloat() / maxOf(bitmap.width, bitmap.height)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
        } else bitmap
        iconFile(context, id).outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
        true
    }.getOrDefault(false)

    fun loadIcon(context: Context, id: String): Bitmap? {
        val file = iconFile(context, id)
        if (!file.exists()) return null
        return runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
    }

    private fun seed(context: Context): List<HomeTile> {
        val marks = runCatching { BookmarkManager.getAll(context) }.getOrElse { emptyList() }
        return marks
            .filter { it.url.startsWith("http") }
            .sortedByDescending { maxOf(it.lastVisit, it.addedAt) }
            .distinctBy { it.url }
            .take(SEED_LIMIT)
            .map { HomeSiteTile(newHomeTileId(), it.url, it.title) }
    }

    private fun siteJson(site: HomeSiteTile): JSONObject = JSONObject()
        .put("type", "site")
        .put("id", site.id)
        .put("url", site.url)
        .put("title", site.title)
        .put("icon", site.customIcon)

    private fun serialize(tiles: List<HomeTile>): JSONArray {
        val array = JSONArray()
        tiles.forEach { tile ->
            when (tile) {
                is HomeSiteTile -> array.put(siteJson(tile))
                is HomeFolderTile -> {
                    val sites = JSONArray()
                    tile.sites.forEach { sites.put(siteJson(it)) }
                    array.put(JSONObject().put("type", "folder").put("id", tile.id).put("name", tile.name).put("sites", sites))
                }
            }
        }
        return array
    }

    private fun parseSite(obj: JSONObject): HomeSiteTile? {
        val url = obj.optString("url")
        if (url.isBlank()) return null
        return HomeSiteTile(
            id = obj.optString("id").ifBlank { newHomeTileId() },
            url = url,
            title = obj.optString("title"),
            customIcon = obj.optBoolean("icon", false)
        )
    }

    private fun parse(array: JSONArray): List<HomeTile> {
        val result = ArrayList<HomeTile>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            if (obj.optString("type") == "folder") {
                val sitesArray = obj.optJSONArray("sites") ?: JSONArray()
                val sites = (0 until sitesArray.length()).mapNotNull { j -> sitesArray.optJSONObject(j)?.let(::parseSite) }
                if (sites.isNotEmpty()) {
                    result.add(HomeFolderTile(obj.optString("id").ifBlank { newHomeTileId() }, obj.optString("name"), sites))
                }
            } else {
                parseSite(obj)?.let(result::add)
            }
        }
        return result
    }
}

internal object HomeTileIconFetcher {
    private const val MIN_SIDE = 96
    private const val MAX_SIDE = 256
    private const val PAGE_LIMIT = 512 * 1024
    private const val MANIFEST_LIMIT = 128 * 1024
    private const val IMAGE_LIMIT = 2 * 1024 * 1024
    private const val FALLBACK_MIN_SIDE = 48
    private const val MAX_ATTEMPTS = 6
    private const val MISS_RETRY_MS = 10 * 60 * 1000L
    private const val USER_AGENT = "Mozilla/5.0 (Android 14; Mobile; rv:140.0) Gecko/140.0 Firefox/140.0"

    private val memory = LruCache<String, Bitmap>(64)
    private val misses = Collections.synchronizedMap(HashMap<String, Long>())
    private val linkTag = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val whitespace = Regex("\\s+")

    private class Candidate(val url: String, val size: Int, val vector: Boolean = false)

    private fun originOf(pageUrl: String): String? = runCatching {
        val url = URL(pageUrl)
        if (url.protocol != "http" && url.protocol != "https") return@runCatching null
        if (url.port == -1) "${url.protocol}://${url.host}" else "${url.protocol}://${url.host}:${url.port}"
    }.getOrNull()

    private fun keyFor(value: String): String =
        MessageDigest.getInstance("MD5").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun cacheFile(context: Context, key: String): File {
        val dir = File(context.cacheDir, "home_icons")
        dir.mkdirs()
        return File(dir, "$key.png")
    }

    private fun isNetworkBlocked(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return DataSaverMode.isActive(prefs) && prefs.getBoolean("data_saver_disable_favicons", true)
    }

    fun cached(pageUrl: String): Bitmap? = originOf(pageUrl)?.let { memory.get(keyFor(it)) }

    suspend fun load(context: Context, pageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        val origin = originOf(pageUrl) ?: return@withContext null
        val key = keyFor(origin)
        memory.get(key)?.let { return@withContext it }
        val appContext = context.applicationContext
        val file = cacheFile(appContext, key)
        if (file.exists()) {
            val stored = runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
            if (stored != null) {
                memory.put(key, stored)
                return@withContext stored
            }
        }
        val missedAt = misses[key]
        if (missedAt != null && SystemClock.elapsedRealtime() - missedAt < MISS_RETRY_MS) return@withContext null
        if (isNetworkBlocked(appContext)) return@withContext null
        val bitmap = runCatching { fetchBest(pageUrl, origin) }.getOrNull()
        if (bitmap != null) {
            memory.put(key, bitmap)
            if (minOf(bitmap.width, bitmap.height) >= MIN_SIDE) {
                misses.remove(key)
                runCatching { file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            } else {
                misses[key] = SystemClock.elapsedRealtime()
            }
        } else {
            misses[key] = SystemClock.elapsedRealtime()
        }
        bitmap
    }

    private fun open(url: String): HttpURLConnection? = runCatching {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 6000
        conn.readTimeout = 6000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/manifest+json,image/avif,image/webp,image/png,image/svg+xml,image/*;q=0.8,*/*;q=0.5")
        conn.setRequestProperty("Accept-Language", java.util.Locale.getDefault().toLanguageTag())
        if (conn.responseCode !in 200..299) {
            conn.disconnect()
            null
        } else conn
    }.getOrNull()

    private fun readLimited(stream: InputStream, limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (total < limit) {
            val read = stream.read(buffer, 0, minOf(buffer.size, limit - total))
            if (read <= 0) break
            out.write(buffer, 0, read)
            total += read
        }
        return out.toByteArray()
    }

    private fun readText(url: String, limit: Int): Pair<String, String>? {
        val conn = open(url) ?: return null
        val text = runCatching { conn.inputStream.use { String(readLimited(it, limit), Charsets.UTF_8) } }.getOrNull()
        val finalUrl = conn.url.toString()
        conn.disconnect()
        return text?.let { it to finalUrl }
    }

    private fun attr(tag: String, name: String): String? {
        val match = Regex("\\b$name\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s>]+))", RegexOption.IGNORE_CASE).find(tag) ?: return null
        return match.groupValues[2].ifEmpty { match.groupValues[3] }.ifEmpty { match.groupValues[4] }
            .replace("&amp;", "&")
            .trim()
    }

    private fun resolve(base: String, href: String): String? = runCatching {
        if (href.startsWith("data:", ignoreCase = true)) return@runCatching null
        val resolved = URL(URL(base), href)
        if (resolved.protocol != "http" && resolved.protocol != "https") null else resolved.toString()
    }.getOrNull()

    private fun largestSize(sizes: String?): Int? {
        if (sizes.isNullOrBlank()) return null
        if (sizes.contains("any", ignoreCase = true)) return 512
        return sizes.split(whitespace).mapNotNull { token ->
            token.lowercase().split('x').firstOrNull()?.toIntOrNull()
        }.maxOrNull()
    }

    private fun isVector(url: String, type: String?): Boolean =
        type?.contains("svg", ignoreCase = true) == true || url.substringBefore('?').endsWith(".svg", ignoreCase = true)

    private fun fetchBest(pageUrl: String, origin: String): Bitmap? {
        val candidates = ArrayList<Candidate>()
        val page = readText(pageUrl, PAGE_LIMIT)
        if (page != null) {
            val (html, base) = page
            val headEnd = html.indexOf("</head>", ignoreCase = true)
            val head = if (headEnd > 0) html.substring(0, headEnd) else html
            var manifestUrl: String? = null
            linkTag.findAll(head).forEach { match ->
                val tag = match.value
                val rels = attr(tag, "rel")?.lowercase()?.split(whitespace) ?: return@forEach
                val href = attr(tag, "href")?.takeIf { it.isNotBlank() } ?: return@forEach
                val url = resolve(base, href) ?: return@forEach
                val vector = isVector(url, attr(tag, "type"))
                val declared = largestSize(attr(tag, "sizes"))
                when {
                    "manifest" in rels -> manifestUrl = url
                    "apple-touch-icon" in rels || "apple-touch-icon-precomposed" in rels ->
                        candidates.add(Candidate(url, declared ?: 180, vector))
                    "icon" in rels || "fluid-icon" in rels ->
                        candidates.add(Candidate(url, if (vector) 512 else declared ?: 32, vector))
                }
            }
            Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE).findAll(head).forEach { match ->
                val tag = match.value
                val name = (attr(tag, "property") ?: attr(tag, "name"))?.lowercase() ?: return@forEach
                if (name == "msapplication-tileimage") {
                    attr(tag, "content")?.let { resolve(base, it) }?.let { candidates.add(Candidate(it, 144)) }
                }
            }
            manifestUrl?.let { candidates.addAll(manifestCandidates(it)) }
        }
        candidates.add(Candidate("$origin/apple-touch-icon.png", 180))
        candidates.add(Candidate("$origin/apple-touch-icon-precomposed.png", 179))
        val ordered = candidates
            .filter { it.size >= MIN_SIDE }
            .distinctBy { it.url }
            .sortedWith(
                compareBy<Candidate> { if (it.size in MIN_SIDE..512 || it.vector) 0 else 1 }
                    .thenBy { if (it.vector) 1 else 0 }
                    .thenByDescending { it.size }
            )
            .take(MAX_ATTEMPTS)
        var best: Bitmap? = null
        fun consider(bitmap: Bitmap?): Boolean {
            if (bitmap == null) return false
            if (minOf(bitmap.width, bitmap.height) >= MIN_SIDE) {
                best = bitmap
                return true
            }
            if (minOf(bitmap.width, bitmap.height) > (best?.let { minOf(it.width, it.height) } ?: (FALLBACK_MIN_SIDE - 1))) best = bitmap
            return false
        }
        for (candidate in ordered) {
            if (consider(fetchImage(candidate.url))) return best
        }
        val host = runCatching { URL(origin).host }.getOrNull()
        if (host != null) {
            val encoded = java.net.URLEncoder.encode(origin, "UTF-8")
            val services = listOf(
                "https://t3.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=$encoded&size=256",
                "https://icons.duckduckgo.com/ip3/$host.ico"
            )
            for (service in services) {
                if (consider(fetchImage(service))) return best
            }
        }
        return best
    }

    private fun manifestCandidates(manifestUrl: String): List<Candidate> {
        val (text, base) = readText(manifestUrl, MANIFEST_LIMIT) ?: return emptyList()
        val icons = runCatching { JSONObject(text).optJSONArray("icons") }.getOrNull() ?: return emptyList()
        val result = ArrayList<Candidate>()
        for (i in 0 until icons.length()) {
            val icon = icons.optJSONObject(i) ?: continue
            val purpose = icon.optString("purpose").lowercase()
            if (purpose.isNotBlank() && !purpose.split(whitespace).contains("any")) continue
            val src = icon.optString("src").takeIf { it.isNotBlank() } ?: continue
            val url = resolve(base, src) ?: continue
            val vector = isVector(url, icon.optString("type"))
            result.add(Candidate(url, if (vector) 512 else largestSize(icon.optString("sizes")) ?: 0, vector))
        }
        return result
    }

    private fun fetchImage(url: String): Bitmap? {
        val conn = open(url) ?: return null
        val bytes = runCatching { conn.inputStream.use { readLimited(it, IMAGE_LIMIT) } }.getOrNull()
        conn.disconnect()
        if (bytes == null || bytes.isEmpty()) return null
        return decodeRaster(bytes) ?: decodeVector(bytes)
    }

    private fun decodeRaster(bytes: ByteArray): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return@runCatching null
        val side = minOf(decoded.width, decoded.height)
        if (side > MAX_SIDE) {
            val scale = MAX_SIDE.toFloat() / side
            Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
        } else decoded
    }.getOrNull()

    private fun decodeVector(bytes: ByteArray): Bitmap? = runCatching {
        val text = String(bytes, Charsets.UTF_8)
        if (!text.contains("<svg", ignoreCase = true)) return@runCatching null
        val svg = SVG.getFromString(text)
        val width = svg.documentWidth
        val height = svg.documentHeight
        if (svg.documentViewBox == null) {
            if (width <= 0f || height <= 0f) return@runCatching null
            svg.setDocumentViewBox(0f, 0f, width, height)
        }
        svg.setDocumentWidth(MAX_SIDE.toFloat())
        svg.setDocumentHeight(MAX_SIDE.toFloat())
        val bitmap = Bitmap.createBitmap(MAX_SIDE, MAX_SIDE, Bitmap.Config.ARGB_8888)
        svg.renderToCanvas(Canvas(bitmap))
        bitmap
    }.getOrNull()
}

internal class HomeTileIconImage(val bitmap: ImageBitmap, val fill: Boolean)

@Composable
internal fun rememberHomeTileIcon(site: HomeSiteTile, iconVersion: Int): HomeTileIconImage? {
    val context = LocalContext.current
    val hd by produceState(
        if (site.customIcon) null else HomeTileIconFetcher.cached(site.url),
        site.url, site.id, site.customIcon, iconVersion
    ) {
        value = if (site.customIcon) {
            withContext(Dispatchers.IO) { HomeTileStore.loadIcon(context, site.id) }
        } else {
            HomeTileIconFetcher.load(context, site.url)
        }
    }
    val favicon = rememberClintFavicon(site.url)
    return remember(hd, favicon) {
        hd?.let { HomeTileIconImage(it.asImageBitmap(), site.customIcon || minOf(it.width, it.height) >= 96) }
            ?: favicon?.takeIf { hd == null }?.let { HomeTileIconImage(it.asImageBitmap(), minOf(it.width, it.height) >= 96) }
    }
}
