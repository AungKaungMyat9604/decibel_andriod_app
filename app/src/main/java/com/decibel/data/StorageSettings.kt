package com.decibel.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.OutputStream

/**
 * Remembers user-chosen Video and Music library folders (SAF tree URIs),
 * or falls back to app-private `media/Video` and `media/Music`.
 */
class StorageSettings(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val legacyDir = File(appContext.filesDir, "media")
    private val defaultVideoDir = File(legacyDir, "Video").apply { mkdirs() }
    private val defaultMusicDir = File(legacyDir, "Music").apply { mkdirs() }

    init {
        migrateLegacyPrefs()
        migrateLegacyFlatFiles()
    }

    fun treeUri(type: MediaType): Uri? {
        val key = keyFor(type)
        return prefs.getString(key, null)?.let(Uri::parse)
    }

    fun setTreeUri(type: MediaType, uri: Uri?) {
        prefs.edit().apply {
            val key = keyFor(type)
            if (uri == null) remove(key) else putString(key, uri.toString())
        }.apply()
    }

    fun takePersistablePermission(type: MediaType, uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            appContext.contentResolver.takePersistableUriPermission(uri, flags)
        }
        setTreeUri(type, uri)
    }

    fun clearCustomFolder(type: MediaType) {
        treeUri(type)?.let { uri ->
            // Only release if the other media type is not using the same URI.
            val other = if (type == MediaType.VIDEO) MediaType.AUDIO else MediaType.VIDEO
            if (treeUri(other) != uri) {
                runCatching {
                    appContext.contentResolver.releasePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                }
            }
        }
        setTreeUri(type, null)
    }

    fun clearAllCustomFolders() {
        clearCustomFolder(MediaType.VIDEO)
        clearCustomFolder(MediaType.AUDIO)
    }

    fun isUsingAppFolder(type: MediaType): Boolean = treeUri(type) == null

    fun isUsingAppFolder(): Boolean =
        isUsingAppFolder(MediaType.VIDEO) && isUsingAppFolder(MediaType.AUDIO)

    fun displayPath(type: MediaType): String {
        val uri = treeUri(type) ?: return defaultDir(type).absolutePath
        val doc = DocumentFile.fromTreeUri(appContext, uri)
        val label = if (type == MediaType.VIDEO) "Video" else "Music"
        return doc?.name?.let { "$label · $it" } ?: "$label · $uri"
    }

    /** Combined label for settings summary. */
    fun displayPath(): String =
        "Video: ${shortLabel(MediaType.VIDEO)}\nMusic: ${shortLabel(MediaType.AUDIO)}"

    private fun shortLabel(type: MediaType): String {
        val uri = treeUri(type) ?: return defaultDir(type).absolutePath
        val doc = DocumentFile.fromTreeUri(appContext, uri)
        return doc?.name?.let { "Folder · $it" } ?: uri.toString()
    }

    fun defaultDir(type: MediaType): File =
        if (type == MediaType.AUDIO) defaultMusicDir else defaultVideoDir

    /** @deprecated Use [defaultDir] with [MediaType]. */
    fun defaultDir(): File = defaultVideoDir

    fun openOutput(fileName: String, type: MediaType): Pair<String, OutputStream> {
        val uri = treeUri(type)
        if (uri != null) {
            val tree = DocumentFile.fromTreeUri(appContext, uri)
                ?: error("Cannot open selected ${folderNoun(type)} folder")
            tree.findFile(fileName)?.delete()
            val created = tree.createFile(mimeFor(fileName), fileName)
                ?: error("Cannot create file in selected ${folderNoun(type)} folder")
            val out = appContext.contentResolver.openOutputStream(created.uri, "w")
                ?: error("Cannot write to selected ${folderNoun(type)} folder")
            return created.uri.toString() to out
        }
        val dir = defaultDir(type)
        dir.mkdirs()
        val file = File(dir, fileName)
        if (file.exists()) file.delete()
        return file.absolutePath to file.outputStream()
    }

    fun deleteRef(fileRef: String) {
        if (fileRef.startsWith("content:")) {
            runCatching {
                DocumentFile.fromSingleUri(appContext, Uri.parse(fileRef))?.delete()
            }
        } else {
            File(fileRef).delete()
        }
    }

    fun exists(fileRef: String): Boolean =
        if (fileRef.startsWith("content:")) {
            DocumentFile.fromSingleUri(appContext, Uri.parse(fileRef))?.exists() == true
        } else {
            File(fileRef).exists()
        }

    fun nameExists(fileName: String, type: MediaType): Boolean {
        val uri = treeUri(type)
        if (uri != null) {
            val tree = DocumentFile.fromTreeUri(appContext, uri) ?: return false
            return tree.findFile(fileName)?.exists() == true
        }
        return File(defaultDir(type), fileName).exists()
    }

    fun length(fileRef: String): Long =
        if (fileRef.startsWith("content:")) {
            DocumentFile.fromSingleUri(appContext, Uri.parse(fileRef))?.length() ?: 0L
        } else {
            File(fileRef).length()
        }

    /** Lists media files in both Video and Music folders. */
    fun listMediaFiles(): List<DocumentOrFile> =
        listMediaFiles(MediaType.VIDEO) + listMediaFiles(MediaType.AUDIO)

    fun listMediaFiles(type: MediaType): List<DocumentOrFile> {
        val uri = treeUri(type)
        if (uri != null) {
            val tree = DocumentFile.fromTreeUri(appContext, uri) ?: return emptyList()
            return tree.listFiles()
                .filter { it.isFile && isMediaName(it.name.orEmpty(), type) }
                .map {
                    DocumentOrFile(
                        name = it.name.orEmpty(),
                        ref = it.uri.toString(),
                        sizeBytes = it.length(),
                        lastModified = it.lastModified(),
                        mediaType = type,
                    )
                }
        }
        return defaultDir(type).listFiles()
            ?.filter { it.isFile && isMediaName(it.name, type) }
            ?.map {
                DocumentOrFile(
                    name = it.name,
                    ref = it.absolutePath,
                    sizeBytes = it.length(),
                    lastModified = it.lastModified(),
                    mediaType = type,
                )
            }
            .orEmpty()
    }

    private fun mimeFor(fileName: String): String =
        when (fileName.substringAfterLast('.', "").lowercase()) {
            "mp4", "m4v" -> "video/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "opus" -> "audio/opus"
            else -> "application/octet-stream"
        }

    private fun isMediaName(name: String, type: MediaType): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (type) {
            MediaType.VIDEO -> ext in VIDEO_EXTS
            MediaType.AUDIO -> ext in AUDIO_EXTS
        }
    }

    private fun folderNoun(type: MediaType): String =
        if (type == MediaType.AUDIO) "music" else "video"

    private fun keyFor(type: MediaType): String =
        if (type == MediaType.AUDIO) KEY_TREE_URI_MUSIC else KEY_TREE_URI_VIDEO

    /** Old single `tree_uri` becomes the Video folder. */
    private fun migrateLegacyPrefs() {
        if (prefs.contains(KEY_TREE_URI_VIDEO)) return
        val legacy = prefs.getString(KEY_TREE_URI_LEGACY, null) ?: return
        prefs.edit()
            .putString(KEY_TREE_URI_VIDEO, legacy)
            .remove(KEY_TREE_URI_LEGACY)
            .apply()
    }

    /**
     * Move files that lived in flat `filesDir/media/` into Video/ or Music/
     * (only when not using a custom SAF folder for that type).
     */
    private fun migrateLegacyFlatFiles() {
        if (!legacyDir.isDirectory) return
        val files = legacyDir.listFiles()?.filter { it.isFile } ?: return
        for (file in files) {
            val ext = file.extension.lowercase()
            val type = when {
                ext in AUDIO_EXTS -> MediaType.AUDIO
                ext in VIDEO_EXTS -> MediaType.VIDEO
                else -> continue
            }
            if (!isUsingAppFolder(type)) continue
            val destDir = defaultDir(type)
            val dest = File(destDir, file.name)
            if (dest.exists()) {
                file.delete()
                continue
            }
            if (!file.renameTo(dest)) {
                runCatching {
                    file.inputStream().use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    }
                    file.delete()
                }
            }
        }
    }

    data class DocumentOrFile(
        val name: String,
        val ref: String,
        val sizeBytes: Long,
        val lastModified: Long,
        val mediaType: MediaType = MediaType.VIDEO,
    )

    companion object {
        private const val PREFS = "decibel_storage"
        private const val KEY_TREE_URI_LEGACY = "tree_uri"
        private const val KEY_TREE_URI_VIDEO = "tree_uri_video"
        private const val KEY_TREE_URI_MUSIC = "tree_uri_music"
        private val VIDEO_EXTS = setOf("mp4", "m4v", "webm", "mkv")
        private val AUDIO_EXTS = setOf("mp3", "m4a", "opus")
    }
}
