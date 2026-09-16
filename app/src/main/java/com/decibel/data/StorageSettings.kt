package com.decibel.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.OutputStream

/**
 * Remembers the user-chosen download / library folder (SAF tree URI),
 * or falls back to app-private storage.
 */
class StorageSettings(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val defaultDir = File(appContext.filesDir, "media").apply { mkdirs() }

    fun treeUri(): Uri? =
        prefs.getString(KEY_TREE_URI, null)?.let(Uri::parse)

    fun setTreeUri(uri: Uri?) {
        prefs.edit().apply {
            if (uri == null) remove(KEY_TREE_URI) else putString(KEY_TREE_URI, uri.toString())
        }.apply()
    }

    fun takePersistablePermission(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            appContext.contentResolver.takePersistableUriPermission(uri, flags)
        }
        setTreeUri(uri)
    }

    fun clearCustomFolder() {
        treeUri()?.let { uri ->
            runCatching {
                appContext.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
        }
        setTreeUri(null)
    }

    fun isUsingAppFolder(): Boolean = treeUri() == null

    fun displayPath(): String {
        val uri = treeUri() ?: return defaultDir.absolutePath
        val doc = DocumentFile.fromTreeUri(appContext, uri)
        return doc?.name?.let { "Folder · $it" } ?: uri.toString()
    }

    fun defaultDir(): File = defaultDir

    /**
     * Creates a new file in the active library folder and returns a writable stream
     * plus a stable reference string to store in [SavedVideo.filePath].
     */
    fun openOutput(fileName: String): Pair<String, OutputStream> {
        val uri = treeUri()
        if (uri != null) {
            val tree = DocumentFile.fromTreeUri(appContext, uri)
                ?: error("Cannot open selected folder")
            tree.findFile(fileName)?.delete()
            val created = tree.createFile(mimeFor(fileName), fileName)
                ?: error("Cannot create file in selected folder")
            val out = appContext.contentResolver.openOutputStream(created.uri, "w")
                ?: error("Cannot write to selected folder")
            return created.uri.toString() to out
        }
        val file = File(defaultDir, fileName)
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

    fun nameExists(fileName: String): Boolean {
        val uri = treeUri()
        if (uri != null) {
            val tree = DocumentFile.fromTreeUri(appContext, uri) ?: return false
            return tree.findFile(fileName)?.exists() == true
        }
        return File(defaultDir, fileName).exists()
    }

    fun length(fileRef: String): Long =
        if (fileRef.startsWith("content:")) {
            DocumentFile.fromSingleUri(appContext, Uri.parse(fileRef))?.length() ?: 0L
        } else {
            File(fileRef).length()
        }

    /** Lists media files in the active folder (for refresh / import). */
    fun listMediaFiles(): List<DocumentOrFile> {
        val uri = treeUri()
        if (uri != null) {
            val tree = DocumentFile.fromTreeUri(appContext, uri) ?: return emptyList()
            return tree.listFiles()
                .filter { it.isFile && isMediaName(it.name.orEmpty()) }
                .map {
                    DocumentOrFile(
                        name = it.name.orEmpty(),
                        ref = it.uri.toString(),
                        sizeBytes = it.length(),
                        lastModified = it.lastModified(),
                    )
                }
        }
        return defaultDir.listFiles()
            ?.filter { it.isFile && isMediaName(it.name) }
            ?.map {
                DocumentOrFile(
                    name = it.name,
                    ref = it.absolutePath,
                    sizeBytes = it.length(),
                    lastModified = it.lastModified(),
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
            else -> "application/octet-stream"
        }

    private fun isMediaName(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in setOf("mp4", "m4v", "webm", "mkv", "mp3", "m4a", "opus")
    }

    data class DocumentOrFile(
        val name: String,
        val ref: String,
        val sizeBytes: Long,
        val lastModified: Long,
    )

    companion object {
        private const val PREFS = "decibel_storage"
        private const val KEY_TREE_URI = "tree_uri"
    }
}
