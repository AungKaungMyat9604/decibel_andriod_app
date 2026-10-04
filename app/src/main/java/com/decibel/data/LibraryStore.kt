package com.decibel.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class LibraryStore(
    context: Context,
    private val storage: StorageSettings,
    private val thumbs: ThumbnailStore,
) {
    private val gson = Gson()
    private val metaFile = File(context.filesDir, "library.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()
    private val _videos = MutableStateFlow(emptyList<SavedVideo>())
    val videos: StateFlow<List<SavedVideo>> = _videos.asStateFlow()

    init {
        val loaded = load().map { video ->
            thumbs.pathIfExists(video.id)?.let { video.copy(thumbnailUrl = it) } ?: video
        }
        _videos.value = loaded
        scope.launch {
            writeMutex.withLock {
                val current = _videos.value
                val withThumbs = current.map { ensureLocalThumb(it) }
                if (withThumbs != current) {
                    persist(withThumbs)
                }
            }
        }
    }

    fun storageSettings(): StorageSettings = storage

    fun thumbnailStore(): ThumbnailStore = thumbs

    fun displayFolder(): String = storage.displayPath()

    fun displayFolder(type: MediaType): String = storage.displayPath(type)

    /**
     * If a library entry still points at legacy flat `media/file` after the
     * Video/Music split, retarget to the new location when the file exists there.
     */
    private fun relocateIfMoved(video: SavedVideo): SavedVideo {
        if (storage.exists(video.filePath)) return video
        if (video.filePath.startsWith("content:")) return video
        val name = video.fileName.ifBlank { File(video.filePath).name }
        val candidate = File(storage.defaultDir(video.mediaType), name)
        return if (candidate.exists()) {
            video.copy(filePath = candidate.absolutePath, fileName = name)
        } else {
            video
        }
    }

    fun getById(id: String): SavedVideo? = _videos.value.firstOrNull { it.id == id }

    fun findByFileName(fileName: String): SavedVideo? =
        _videos.value.firstOrNull { it.fileName.equals(fileName, ignoreCase = true) }

    fun hasFileWithBaseName(baseName: String): Boolean {
        val lower = baseName.lowercase()
        if (_videos.value.any {
                it.fileName.substringBeforeLast('.').equals(baseName, ignoreCase = true)
            }
        ) {
            return true
        }
        return storage.listMediaFiles().any {
            it.name.substringBeforeLast('.').equals(baseName, ignoreCase = true) ||
                it.name.lowercase().startsWith("$lower.")
        }
    }

    suspend fun upsert(video: SavedVideo) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val existing = _videos.value.firstOrNull { it.id == video.id }
            val merged = video.copy(isFavourite = video.isFavourite || (existing?.isFavourite == true))
            val withThumb = ensureLocalThumb(merged)
            val next = listOf(withThumb) + _videos.value.filterNot { it.id == withThumb.id }
            persist(next)
        }
    }

    suspend fun setFavourite(id: String, favourite: Boolean) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val next = _videos.value.map {
                if (it.id == id) it.copy(isFavourite = favourite) else it
            }
            persist(next)
        }
    }

    suspend fun toggleFavourite(id: String) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val next = _videos.value.map {
                if (it.id == id) it.copy(isFavourite = !it.isFavourite) else it
            }
            persist(next)
        }
    }

    suspend fun delete(id: String) = deleteMany(listOf(id))

    /**
     * Atomically remove many entries. Concurrent single deletes used to race on
     * [persist] and resurrect metadata for files that were already deleted.
     */
    suspend fun deleteMany(ids: Collection<String>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val idSet = ids.toSet()
        writeMutex.withLock {
            val current = _videos.value
            val removing = current.filter { it.id in idSet }
            removing.forEach { existing ->
                storage.deleteRef(existing.filePath)
                thumbs.delete(existing.id)
            }
            persist(current.filterNot { it.id in idSet })
        }
    }

    /**
     * Reorder a visible subset (All or Favourites list) and splice that relative
     * order back into the persisted full library list.
     */
    suspend fun setOrderForSubset(orderedVisibleIds: List<String>) = withContext(Dispatchers.IO) {
        if (orderedVisibleIds.isEmpty()) return@withContext
        writeMutex.withLock {
            val full = _videos.value.toMutableList()
            val idSet = orderedVisibleIds.toSet()
            if (idSet.size != orderedVisibleIds.size) return@withLock
            val slots = full.withIndex()
                .filter { it.value.id in idSet }
                .map { it.index }
            if (slots.size != orderedVisibleIds.size) return@withLock
            val byId = full.associateBy { it.id }
            if (!orderedVisibleIds.all { it in byId }) return@withLock
            slots.zip(orderedVisibleIds).forEach { (slot, id) ->
                full[slot] = byId.getValue(id)
            }
            persist(full)
        }
    }

    suspend fun refreshFromFolder() = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            // Preserve the user's custom list order; only append newly discovered files.
            val current = _videos.value.ifEmpty { load() }
            val knownPaths = current.map { it.filePath }.toHashSet()
            val discovered = mutableListOf<SavedVideo>()
            for (file in storage.listMediaFiles()) {
                if (file.ref in knownPaths) continue
                val id = "file_${file.name.hashCode()}_${file.sizeBytes}"
                if (current.any { it.id == id } || discovered.any { it.id == id }) continue
                val ext = file.name.substringAfterLast('.', "mp4").lowercase()
                discovered += SavedVideo(
                    id = id,
                    title = file.name.substringBeforeLast('.').ifBlank { file.name },
                    uploader = "Local file",
                    durationSeconds = 0L,
                    thumbnailUrl = null,
                    webpageUrl = "",
                    quality = "local",
                    fileName = file.name,
                    filePath = file.ref,
                    fileSizeBytes = file.sizeBytes,
                    downloadedAtEpochMs = file.lastModified,
                    mediaType = file.mediaType,
                    format = ext,
                )
            }
            val keptExisting = current
                .filter { storage.exists(it.filePath) }
                .map { ensureLocalThumb(it) }
            val appended = discovered.map { ensureLocalThumb(it) }
            persist(keptExisting + appended)
        }
    }

    /**
     * Prefer cached local thumb; otherwise extract a frame from the video file.
     * Remote https URLs are replaced with a local path when possible.
     */
    fun ensureLocalThumb(video: SavedVideo): SavedVideo {
        thumbs.pathIfExists(video.id)?.let { local ->
            if (video.thumbnailUrl != local) {
                return video.copy(thumbnailUrl = local)
            }
            return video
        }
        if (thumbs.isLocalPath(video.thumbnailUrl) && File(video.thumbnailUrl!!).exists()) {
            return video
        }
        // Try frame grab from local media (works offline).
        val fromVideo = thumbs.cacheFromVideo(video.id, video.filePath)
        if (fromVideo != null) {
            return video.copy(thumbnailUrl = fromVideo)
        }
        // Last resort: if we still have a remote URL and network works, cache it.
        val remote = video.thumbnailUrl
        if (!remote.isNullOrBlank() && (remote.startsWith("http://") || remote.startsWith("https://"))) {
            thumbs.cacheFromUrl(video.id, remote)?.let {
                return video.copy(thumbnailUrl = it)
            }
        }
        return video
    }

    private fun load(): List<SavedVideo> {
        if (!metaFile.exists()) return emptyList()
        return runCatching {
            val root = JsonParser.parseString(metaFile.readText()).asJsonArray
            val migrated = root.mapNotNull { migrateItem(it.asJsonObject) }
            val relocated = migrated.map { relocateIfMoved(it) }
            val kept = relocated.filter { storage.exists(it.filePath) }
            val pathsChanged = migrated.zip(relocated).any { (before, after) ->
                before.filePath != after.filePath
            }
            if (pathsChanged) {
                metaFile.writeText(gson.toJson(kept))
            }
            kept
        }.getOrDefault(emptyList())
    }

    private fun migrateItem(obj: JsonObject): SavedVideo? {
        val filePath = obj.get("filePath")?.asString ?: return null
        val fileName = obj.get("fileName")?.asString ?: File(filePath).name
        val ext = fileName.substringAfterLast('.', "mp4").lowercase()
        val mediaType = when {
            obj.has("mediaType") -> runCatching {
                MediaType.valueOf(obj.get("mediaType").asString)
            }.getOrElse {
                if (ext == "mp3" || ext == "m4a" || ext == "opus" || ext == "webm") {
                    MediaType.AUDIO
                } else {
                    MediaType.VIDEO
                }
            }
            ext == "mp3" || ext == "m4a" || ext == "opus" -> MediaType.AUDIO
            else -> MediaType.VIDEO
        }
        val format = obj.get("format")?.asString?.ifBlank { null } ?: ext
        return SavedVideo(
            id = obj.get("id")?.asString ?: return null,
            title = obj.get("title")?.asString.orEmpty().ifBlank { "Untitled" },
            uploader = obj.get("uploader")?.asString.orEmpty().ifBlank { "Unknown" },
            durationSeconds = obj.get("durationSeconds")?.asLong ?: 0L,
            thumbnailUrl = obj.get("thumbnailUrl")?.takeIf { !it.isJsonNull }?.asString,
            webpageUrl = obj.get("webpageUrl")?.asString.orEmpty(),
            quality = obj.get("quality")?.asString.orEmpty(),
            fileName = fileName,
            filePath = filePath,
            fileSizeBytes = obj.get("fileSizeBytes")?.asLong ?: storage.length(filePath),
            downloadedAtEpochMs = obj.get("downloadedAtEpochMs")?.asLong ?: 0L,
            mediaType = mediaType,
            format = format,
            isFavourite = obj.get("isFavourite")?.takeIf { !it.isJsonNull }?.asBoolean == true,
        )
    }

    private fun persist(items: List<SavedVideo>) {
        metaFile.writeText(gson.toJson(items))
        _videos.value = items
    }
}
