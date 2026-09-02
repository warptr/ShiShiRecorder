package com.warptr.CaptureCapMP3

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecordingEntry(
    val uri: String,
    val name: String,
    val createdAt: Long,
    val durationMs: Long,
    val customFolder: Boolean,
)

data class RecordingTarget(val uri: Uri, val customFolder: Boolean, val name: String)

class RecordingCatalog(private val context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val resolver: ContentResolver = context.contentResolver

    fun getBitRate(): Int = preferences.getInt(KEY_BIT_RATE, 128).takeIf { it in BIT_RATES } ?: 128

    fun setBitRate(value: Int) {
        require(value in BIT_RATES)
        preferences.edit().putInt(KEY_BIT_RATE, value).apply()
    }

    fun getCustomFolder(): Uri? = preferences.getString(KEY_CUSTOM_FOLDER, null)?.let(Uri::parse)

    fun setCustomFolder(uri: Uri?) {
        preferences.edit().apply {
            if (uri == null) remove(KEY_CUSTOM_FOLDER) else putString(KEY_CUSTOM_FOLDER, uri.toString())
        }.apply()
    }

    fun createTarget(): RecordingTarget {
        val name = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.CHINA).format(Date()) + ".mp3"
        val treeUri = getCustomFolder()
        if (treeUri != null) {
            val documentUri = DocumentsContract.createDocument(resolver, treeUri, "audio/mpeg", name)
                ?: error("无法在所选目录创建录音文件")
            return RecordingTarget(documentUri, true, name)
        }
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, name)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/MP3/")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values) ?: error("无法在音乐目录创建录音文件")
        return RecordingTarget(uri, false, name)
    }

    fun publish(target: RecordingTarget, durationMs: Long) {
        if (!target.customFolder) {
            resolver.update(target.uri, ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }, null, null)
        }
        val items = loadMutable().apply {
            removeAll { it.uri == target.uri.toString() }
            add(RecordingEntry(target.uri.toString(), target.name, System.currentTimeMillis(), durationMs, target.customFolder))
        }
        save(items)
    }

    fun discard(target: RecordingTarget?) {
        if (target != null) runCatching { resolver.delete(target.uri, null, null) }
    }

    fun list(): List<RecordingEntry> = loadMutable()
        .filter { it.uri.isNotBlank() }
        .sortedByDescending { it.createdAt }

    fun delete(entry: RecordingEntry) {
        resolver.delete(Uri.parse(entry.uri), null, null)
        val items = loadMutable().apply { removeAll { it.uri == entry.uri } }
        save(items)
    }

    fun rename(entry: RecordingEntry, requestedName: String): RecordingEntry {
        val normalized = requestedName.trim().removeSuffix(".mp3").trim() + ".mp3"
        require(normalized != ".mp3" && normalized.none { it in FORBIDDEN_FILENAME_CHARS }) { "文件名无效" }
        val oldUri = Uri.parse(entry.uri)
        val newUri = if (entry.customFolder) {
            DocumentsContract.renameDocument(resolver, oldUri, normalized) ?: error("重命名失败")
        } else {
            val changed = resolver.update(oldUri, ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, normalized)
            }, null, null)
            if (changed <= 0) error("重命名失败")
            oldUri
        }
        val updated = entry.copy(uri = newUri.toString(), name = normalized)
        val items = loadMutable().apply {
            val index = indexOfFirst { it.uri == entry.uri }
            if (index >= 0) set(index, updated)
        }
        save(items)
        return updated
    }

    fun setOverlayEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_OVERLAY_ENABLED, enabled).apply()
    }

    fun isOverlayEnabled(): Boolean = preferences.getBoolean(KEY_OVERLAY_ENABLED, false)

    private fun loadMutable(): MutableList<RecordingEntry> {
        val raw = preferences.getString(KEY_RECORDINGS, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            MutableList(array.length()) { index ->
                val item = array.getJSONObject(index)
                RecordingEntry(
                    uri = item.getString("uri"),
                    name = item.getString("name"),
                    createdAt = item.getLong("createdAt"),
                    durationMs = item.optLong("durationMs", 0),
                    customFolder = item.optBoolean("customFolder", false),
                )
            }
        }.getOrElse { mutableListOf() }
    }

    private fun save(items: List<RecordingEntry>) {
        val array = JSONArray()
        items.forEach { entry ->
            array.put(JSONObject().apply {
                put("uri", entry.uri)
                put("name", entry.name)
                put("createdAt", entry.createdAt)
                put("durationMs", entry.durationMs)
                put("customFolder", entry.customFolder)
            })
        }
        preferences.edit().putString(KEY_RECORDINGS, array.toString()).apply()
    }

    companion object {
        const val PREFERENCES = "shishi_recorder"
        const val KEY_ACTIVE = "recording_active"
        const val KEY_PREPARED = "recording_prepared"
        const val KEY_STARTED_AT = "recording_started_at"
        const val KEY_BIT_RATE = "bit_rate"
        const val KEY_CUSTOM_FOLDER = "custom_folder"
        private const val KEY_RECORDINGS = "recordings"
        private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        val BIT_RATES = listOf(128, 192, 256, 320)
        private val FORBIDDEN_FILENAME_CHARS = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
    }
}
