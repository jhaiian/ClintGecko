package com.jhaiian.clint.backup

import android.content.Context
import com.jhaiian.clint.extensions.ClintExtensions
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

data class ExtensionBackupFile(val relPath: String, val file: File)

object ExtensionBackup {

    const val ZIP_ROOT = "extensions/"
    const val ID_PREFIX = "extensions:"

    private const val PENDING_DIR = "clint_pending_extensions"
    private const val XPI_DIR = "xpi"
    private const val DATA_DIR = "browser-extension-data"
    private const val STORAGE_DIR = "storage"
    private const val EXTENSIONS_JSON = "extensions.json"
    private const val UUIDS_JSON = "uuids.json"
    private const val PREFERENCES_JSON = "extension-preferences.json"
    private const val STARTUP_STATE = "addonStartup.json.lz4"
    private const val UUIDS_PREF = "extensions.webextensions.uuids"
    private const val ORIGIN_PREFIX = "moz-extension+++"
    private val UUIDS_REGEX = Regex("""user_pref\("extensions\.webextensions\.uuids",\s*"((?:\\.|[^"\\])*)"\);""")

    fun profileDir(context: Context): File? {
        val mozilla = File(context.filesDir, "mozilla")
        if (!mozilla.isDirectory) return null
        val ini = File(mozilla, "profiles.ini")
        if (ini.isFile) {
            val profiles = ArrayList<MutableMap<String, String>>()
            var current: MutableMap<String, String>? = null
            for (raw in runCatching { ini.readLines() }.getOrDefault(emptyList())) {
                val line = raw.trim()
                if (line.startsWith("[") && line.endsWith("]")) {
                    current = if (line.startsWith("[Profile", true)) mutableMapOf<String, String>().also { profiles.add(it) } else null
                } else if (current != null && line.contains('=')) {
                    current[line.substringBefore('=').trim()] = line.substringAfter('=').trim()
                }
            }
            val chosen = profiles.firstOrNull { it["Default"] == "1" } ?: profiles.firstOrNull()
            val path = chosen?.get("Path")
            if (!path.isNullOrBlank()) {
                val dir = if (chosen?.get("IsRelative") == "0") File(path) else File(mozilla, path)
                if (dir.isDirectory) return dir
            }
        }
        return mozilla.listFiles()
            ?.filter { it.isDirectory && it.name.endsWith(".default") }
            ?.maxByOrNull { it.lastModified() }
    }

    fun prepare(context: Context, liveUuids: Map<String, String>, workDir: File): List<ExtensionBackupFile> {
        val profile = profileDir(context) ?: return emptyList()
        val dbFile = File(profile, EXTENSIONS_JSON)
        if (!dbFile.isFile) return emptyList()
        val root = runCatching { JSONObject(dbFile.readText()) }.getOrNull() ?: return emptyList()
        val addons = root.optJSONArray("addons") ?: return emptyList()
        val extDir = File(profile, "extensions")

        val kept = JSONArray()
        val ids = ArrayList<String>()
        val files = ArrayList<ExtensionBackupFile>()
        for (i in 0 until addons.length()) {
            val addon = addons.optJSONObject(i) ?: continue
            val id = addon.optString("id")
            if (id.isBlank() || id == ClintExtensions.BUILT_IN_ID) continue
            val xpi = userXpi(extDir, addon) ?: continue
            kept.put(addon)
            ids.add(id)
            files.add(ExtensionBackupFile("$XPI_DIR/${xpi.name}", xpi))
        }
        if (kept.length() == 0) return emptyList()

        workDir.mkdirs()
        root.put("addons", kept)
        val dbCopy = File(workDir, EXTENSIONS_JSON).also { it.writeText(root.toString()) }

        val stored = readUuids(profile)
        val uuids = JSONObject()
        for (id in ids) {
            val uuid = liveUuids[id] ?: stored.optString(id).takeIf { it.isNotBlank() }
            if (uuid != null) uuids.put(id, uuid)
        }
        val uuidsCopy = File(workDir, UUIDS_JSON).also { it.writeText(uuids.toString()) }

        val result = ArrayList<ExtensionBackupFile>()
        result.add(ExtensionBackupFile(EXTENSIONS_JSON, dbCopy))
        result.add(ExtensionBackupFile(UUIDS_JSON, uuidsCopy))

        val prefsFile = File(profile, PREFERENCES_JSON)
        if (prefsFile.isFile) {
            runCatching {
                val all = JSONObject(prefsFile.readText())
                val subset = JSONObject()
                for (id in ids) all.optJSONObject(id)?.let { subset.put(id, it) }
                if (subset.length() > 0) {
                    val copy = File(workDir, PREFERENCES_JSON).also { it.writeText(subset.toString()) }
                    result.add(ExtensionBackupFile(PREFERENCES_JSON, copy))
                }
            }
        }

        result.addAll(files)

        val originsDir = File(profile, "$STORAGE_DIR/default")
        for (id in ids) {
            addTree(File(profile, "$DATA_DIR/$id"), "$DATA_DIR/$id", result)
            val uuid = uuids.optString(id)
            if (uuid.isNotBlank()) {
                originsDir.listFiles()
                    ?.filter { it.isDirectory && it.name.startsWith("$ORIGIN_PREFIX$uuid") }
                    ?.forEach { addTree(it, "$STORAGE_DIR/${it.name}", result) }
            }
        }
        return result
    }

    fun stage(context: Context, zip: ZipFile, entries: List<BackupManifestEntry>, onEach: () -> Unit): Int {
        val root = File(context.filesDir, PENDING_DIR)
        root.deleteRecursively()
        root.mkdirs()
        val rootPath = root.canonicalPath + File.separator
        var count = 0
        for (entry in entries) {
            if (!entry.zipPath.startsWith(ZIP_ROOT)) continue
            val zipEntry = zip.getEntry(entry.zipPath) ?: continue
            val dest = File(root, entry.zipPath.removePrefix(ZIP_ROOT)).canonicalFile
            if (!dest.path.startsWith(rootPath)) continue
            dest.parentFile?.mkdirs()
            zip.getInputStream(zipEntry).use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            count++
            onEach()
        }
        if (!File(root, EXTENSIONS_JSON).isFile) {
            root.deleteRecursively()
            return 0
        }
        return count
    }

    fun applyPending(context: Context) {
        val pending = File(context.filesDir, PENDING_DIR)
        if (!pending.isDirectory) return
        val profile = profileDir(context) ?: return
        runCatching { apply(pending, profile) }
        pending.deleteRecursively()
    }

    private fun apply(pending: File, profile: File) {
        val backupRoot = JSONObject(File(pending, EXTENSIONS_JSON).readText())
        val backupAddons = backupRoot.optJSONArray("addons") ?: JSONArray()
        val backupUuids = File(pending, UUIDS_JSON).takeIf { it.isFile }
            ?.let { runCatching { JSONObject(it.readText()) }.getOrNull() } ?: JSONObject()
        val backupPrefs = File(pending, PREFERENCES_JSON).takeIf { it.isFile }
            ?.let { runCatching { JSONObject(it.readText()) }.getOrNull() }

        val extDir = File(profile, "extensions").apply { mkdirs() }
        val dbFile = File(profile, EXTENSIONS_JSON)
        val currentRoot = if (dbFile.isFile) runCatching { JSONObject(dbFile.readText()) }.getOrNull() else null
        val root = currentRoot ?: JSONObject().also { fresh ->
            for (key in backupRoot.keys()) if (key != "addons") fresh.put(key, backupRoot.get(key))
        }
        val currentAddons = root.optJSONArray("addons") ?: JSONArray()
        val currentUuids = readUuids(profile)

        val removedIds = HashSet<String>()
        val merged = JSONArray()
        for (i in 0 until currentAddons.length()) {
            val addon = currentAddons.optJSONObject(i) ?: continue
            val id = addon.optString("id")
            if (id.isNotBlank() && id != ClintExtensions.BUILT_IN_ID && userXpi(extDir, addon) != null) {
                removedIds.add(id)
            } else {
                merged.put(addon)
            }
        }

        for (id in removedIds) {
            File(profile, "$DATA_DIR/$id").deleteRecursively()
            currentUuids.optString(id).takeIf { it.isNotBlank() }?.let { deleteOrigins(profile, it) }
            currentUuids.remove(id)
        }
        extDir.listFiles()?.filter { it.isFile && it.name.endsWith(".xpi", true) }?.forEach { it.delete() }

        File(pending, XPI_DIR).listFiles()?.filter { it.isFile }?.forEach {
            it.copyTo(File(extDir, it.name), overwrite = true)
        }

        val restoredIds = ArrayList<String>()
        for (i in 0 until backupAddons.length()) {
            val addon = backupAddons.optJSONObject(i) ?: continue
            val id = addon.optString("id")
            if (id.isBlank() || id == ClintExtensions.BUILT_IN_ID) continue
            val name = File(addon.optString("path")).name.takeIf { it.endsWith(".xpi", true) } ?: "$id.xpi"
            val target = File(extDir, name)
            if (!target.isFile) continue
            if (addon.has("path")) addon.put("path", target.absolutePath)
            if (addon.has("rootURI")) addon.put("rootURI", "jar:file://${target.absolutePath}!/")
            merged.put(addon)
            restoredIds.add(id)
            File(profile, "$DATA_DIR/$id").deleteRecursively()
            val uuid = backupUuids.optString(id)
            if (uuid.isNotBlank()) {
                deleteOrigins(profile, uuid)
                currentUuids.put(id, uuid)
            }
        }

        root.put("addons", merged)
        writeAtomic(dbFile, root.toString())
        writeUuids(profile, currentUuids)

        copyTree(File(pending, DATA_DIR), File(profile, DATA_DIR))
        File(pending, STORAGE_DIR).listFiles()?.filter { it.isDirectory }?.forEach {
            copyTree(it, File(profile, "$STORAGE_DIR/default/${it.name}"))
        }

        val prefsFile = File(profile, PREFERENCES_JSON)
        if (backupPrefs != null || (removedIds.isNotEmpty() && prefsFile.isFile)) {
            val current = (if (prefsFile.isFile) runCatching { JSONObject(prefsFile.readText()) }.getOrNull() else null) ?: JSONObject()
            removedIds.forEach { current.remove(it) }
            if (backupPrefs != null) {
                for (key in backupPrefs.keys()) current.put(key, backupPrefs.get(key))
            }
            writeAtomic(prefsFile, current.toString())
        }

        File(profile, STARTUP_STATE).delete()
    }

    private fun userXpi(extDir: File, addon: JSONObject): File? {
        val id = addon.optString("id")
        val name = File(addon.optString("path")).name.takeIf { it.endsWith(".xpi", true) } ?: "$id.xpi"
        return File(extDir, name).takeIf { it.isFile && it.length() > 0 }
    }

    private fun addTree(dir: File, rel: String, out: MutableList<ExtensionBackupFile>) {
        if (!dir.isDirectory) return
        dir.walkTopDown().filter { it.isFile && it.canRead() }.forEach {
            out.add(ExtensionBackupFile("$rel/${it.relativeTo(dir).path.replace(File.separatorChar, '/')}", it))
        }
    }

    private fun copyTree(src: File, dst: File) {
        if (!src.isDirectory) return
        src.walkTopDown().filter { it.isFile }.forEach {
            val out = File(dst, it.relativeTo(src).path)
            out.parentFile?.mkdirs()
            it.copyTo(out, overwrite = true)
        }
    }

    private fun deleteOrigins(profile: File, uuid: String) {
        File(profile, "$STORAGE_DIR/default").listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("$ORIGIN_PREFIX$uuid") }
            ?.forEach { it.deleteRecursively() }
    }

    private fun readUuids(profile: File): JSONObject {
        val prefs = File(profile, "prefs.js")
        if (!prefs.isFile) return JSONObject()
        val match = runCatching { UUIDS_REGEX.find(prefs.readText()) }.getOrNull() ?: return JSONObject()
        return runCatching { JSONObject(unescape(match.groupValues[1])) }.getOrDefault(JSONObject())
    }

    private fun writeUuids(profile: File, uuids: JSONObject) {
        val prefs = File(profile, "prefs.js")
        val line = "user_pref(\"$UUIDS_PREF\", \"${escape(uuids.toString())}\");"
        val text = if (prefs.isFile) prefs.readText() else ""
        val match = UUIDS_REGEX.find(text)
        val updated = if (match != null) {
            text.replaceRange(match.range, line)
        } else {
            text + (if (text.isEmpty() || text.endsWith("\n")) "" else "\n") + line + "\n"
        }
        writeAtomic(prefs, updated)
    }

    private fun unescape(value: String): String {
        val out = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '\\' && i + 1 < value.length) {
                out.append(value[i + 1])
                i += 2
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun writeAtomic(file: File, text: String) {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(text)
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
    }
}
