package com.decibel.data

import android.content.Context
import com.decibel.youtube.VideoFormatOption
import com.decibel.youtube.VideoLookup
import com.decibel.youtube.YoutubeExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class DownloadRepository(
    context: Context,
    private val libraryStore: LibraryStore,
) {
    private val storage = libraryStore.storageSettings()
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .readTimeout(5, TimeUnit.MINUTES)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun fetchInfo(url: String): VideoLookup = withContext(Dispatchers.IO) {
        YoutubeExtractor.lookup(url)
    }

    fun entryIdFor(lookup: VideoLookup): String = "${lookup.id}_video"

    fun sanitizeTitle(title: String, fallback: String): String =
        title.replace(Regex("""[\\/:*?"<>|]"""), "_")
            .take(80)
            .ifBlank { fallback }

    /**
     * True when this YouTube id is already in the library, or a same-title file
     * already exists in the active folder.
     */
    fun isAlreadyDownloaded(lookup: VideoLookup): Boolean {
        val entryId = entryIdFor(lookup)
        if (libraryStore.getById(entryId) != null) return true
        val safeTitle = sanitizeTitle(lookup.title, lookup.id)
        return libraryStore.hasFileWithBaseName(safeTitle)
    }

    /**
     * Downloads a progressive video file. Progress is reported per [jobId]
     * via [DownloadProgressHub].
     */
    suspend fun downloadVideo(
        lookup: VideoLookup,
        format: VideoFormatOption,
        jobId: String,
        overwrite: Boolean = false,
    ): SavedVideo = withContext(Dispatchers.IO) {
        val entryId = entryIdFor(lookup)
        DownloadProgressHub.update(jobId) {
            it.copy(
                entryId = entryId,
                title = lookup.title,
                state = DownloadJobState.Running,
                percent = 0,
                downloadedBytes = 0,
                totalBytes = format.approxSizeBytes,
            )
        }
        try {
            if (overwrite) {
                libraryStore.getById(entryId)?.let { existing ->
                    storage.deleteRef(existing.filePath)
                }
            }
            val safeTitle = sanitizeTitle(lookup.title, lookup.id)
            val ext = format.format.ifBlank { "mp4" }
            val fileName = uniqueFileName(safeTitle, ext, entryId, overwrite)
            val (fileRef, size) = downloadToStorage(
                url = format.url,
                fileName = fileName,
                approxSize = format.approxSizeBytes,
                title = lookup.title,
                jobId = jobId,
            )

            val localThumb = libraryStore.thumbnailStore()
                .cacheFromUrl(entryId, lookup.thumbnailUrl)
                ?: libraryStore.thumbnailStore().cacheFromVideo(entryId, fileRef)

            val saved = SavedVideo(
                id = entryId,
                title = lookup.title,
                uploader = lookup.uploader,
                durationSeconds = lookup.durationSeconds,
                thumbnailUrl = localThumb ?: lookup.thumbnailUrl,
                webpageUrl = lookup.webpageUrl,
                quality = format.resolution,
                fileName = fileName,
                filePath = fileRef,
                fileSizeBytes = size,
                downloadedAtEpochMs = System.currentTimeMillis(),
                mediaType = MediaType.VIDEO,
                format = ext,
            )
            libraryStore.upsert(saved)
            DownloadProgressHub.update(jobId) {
                it.copy(
                    state = DownloadJobState.Success,
                    percent = 100,
                    downloadedBytes = size,
                    totalBytes = size,
                    video = saved,
                )
            }
            saved
        } catch (t: Throwable) {
            DownloadProgressHub.update(jobId) {
                it.copy(
                    state = DownloadJobState.Failed,
                    message = t.message ?: "Download failed",
                )
            }
            throw t
        }
    }

    private fun uniqueFileName(
        safeTitle: String,
        ext: String,
        entryId: String,
        overwrite: Boolean,
    ): String {
        val base = "$safeTitle.$ext"
        if (overwrite) return base
        val existingSameId = libraryStore.getById(entryId)
        if (existingSameId != null) return base

        if (!storage.nameExists(base) && libraryStore.findByFileName(base) == null) {
            return base
        }
        // Collision with a different video — append (2), (3), …
        var n = 2
        while (n < 1000) {
            val candidate = "$safeTitle ($n).$ext"
            if (!storage.nameExists(candidate) && libraryStore.findByFileName(candidate) == null) {
                return candidate
            }
            n++
        }
        return "$safeTitle-${System.currentTimeMillis()}.$ext"
    }

    private fun downloadToStorage(
        url: String,
        fileName: String,
        approxSize: Long?,
        title: String,
        jobId: String,
    ): Pair<String, Long> {
        val tempFile = File(storage.defaultDir(), "$fileName.part")
        tempFile.parentFile?.mkdirs()

        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
            )
            .header("Referer", "https://www.youtube.com/")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Download failed (${response.code})")
            val body = response.body ?: error("Empty response body")
            val total = body.contentLength().takeIf { it > 0 } ?: approxSize
            var downloaded = 0L
            tempFile.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val percent = if (total != null && total > 0) {
                            ((downloaded * 100) / total).toInt().coerceIn(0, 99)
                        } else {
                            0
                        }
                        DownloadProgressHub.update(jobId) {
                            it.copy(
                                title = title,
                                percent = percent,
                                downloadedBytes = downloaded,
                                totalBytes = total,
                                state = DownloadJobState.Running,
                            )
                        }
                    }
                }
            }
        }

        val (fileRef, out) = storage.openOutput(fileName)
        out.use { output ->
            tempFile.inputStream().use { input -> input.copyTo(output) }
        }
        tempFile.delete()
        val size = storage.length(fileRef)
        DownloadProgressHub.update(jobId) {
            it.copy(
                title = title,
                percent = 100,
                downloadedBytes = size,
                totalBytes = size,
                state = DownloadJobState.Running,
            )
        }
        return fileRef to size
    }
}
