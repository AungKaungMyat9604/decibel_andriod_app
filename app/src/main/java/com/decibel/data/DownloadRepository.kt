package com.decibel.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.decibel.media.FfmpegBridge
import com.decibel.youtube.DownloadKind
import com.decibel.youtube.MediaDownloadOption
import com.decibel.youtube.VideoLookup
import com.decibel.youtube.YoutubeExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.SocketException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class DownloadRepository(
    context: Context,
    private val libraryStore: LibraryStore,
) {
    private val appContext = context.applicationContext
    private val storage = libraryStore.storageSettings()

    /** YouTube CDN often resets HTTP/2 mid-stream; prefer HTTP/1.1 + resume. */
    private val client = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.MINUTES)
        .writeTimeout(3, TimeUnit.MINUTES)
        .callTimeout(0, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
        .build()

    companion object {
        private const val TAG = "DownloadRepository"
        private const val DownloadBufferBytes = 256 * 1024
        /** Bounded chunks: open-ended Range gets throttled/416 on googlevideo. */
        private const val ChunkBytes = 8L * 1024 * 1024
        private const val ProgressMinIntervalMs = 250L
        private const val MaxAttempts = 8
        /** Must match ExtractorBootstrap — googlevideo 403s on UA mismatch. */
        private const val UserAgent =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }

    suspend fun fetchInfo(url: String): VideoLookup = withContext(Dispatchers.IO) {
        YoutubeExtractor.lookup(url)
    }

    fun entryIdForVideo(lookup: VideoLookup): String = "${lookup.id}_video"

    fun entryIdForAudio(lookup: VideoLookup): String = "${lookup.id}_audio"

    fun sanitizeTitle(title: String, fallback: String): String =
        title.replace(Regex("""[\\/:*?"<>|]"""), "_")
            .take(80)
            .ifBlank { fallback }

    fun isAlreadyDownloaded(lookup: VideoLookup, option: MediaDownloadOption): Boolean {
        val entryId = entryIdFor(lookup, option)
        if (libraryStore.getById(entryId) != null) return true
        val safeTitle = sanitizeTitle(lookup.title, lookup.id)
        val (ext, type) = when (option.kind) {
            DownloadKind.AUDIO_MP3 -> "mp3" to MediaType.AUDIO
            DownloadKind.VIDEO_HQ, DownloadKind.PROGRESSIVE -> "mp4" to MediaType.VIDEO
        }
        return storage.nameExists("$safeTitle.$ext", type) ||
            libraryStore.findByFileName("$safeTitle.$ext") != null
    }

    fun entryIdFor(lookup: VideoLookup, option: MediaDownloadOption): String =
        when (option.kind) {
            DownloadKind.AUDIO_MP3 -> entryIdForAudio(lookup)
            DownloadKind.VIDEO_HQ, DownloadKind.PROGRESSIVE -> entryIdForVideo(lookup)
        }

    /**
     * Downloads according to [option]: progressive file, DASH merge to MP4, or audio→MP3.
     * Refreshes stream URLs via a fresh lookup when [webpageUrl] is provided.
     */
    suspend fun downloadOption(
        lookupSeed: VideoLookup,
        optionSeed: MediaDownloadOption,
        jobId: String,
        overwrite: Boolean = false,
    ): SavedVideo = withContext(Dispatchers.IO) {
        var lookup = runCatching { YoutubeExtractor.lookup(lookupSeed.webpageUrl) }
            .getOrDefault(lookupSeed)
        var option = YoutubeExtractor.findOption(lookup, optionSeed.id) ?: optionSeed
        var attempt = 0
        while (true) {
            try {
                when (option.kind) {
                    DownloadKind.PROGRESSIVE ->
                        return@withContext downloadProgressive(lookup, option, jobId, overwrite)
                    DownloadKind.VIDEO_HQ ->
                        return@withContext downloadHqVideo(lookup, option, jobId, overwrite)
                    DownloadKind.AUDIO_MP3 ->
                        return@withContext downloadMp3(lookup, option, jobId, overwrite)
                }
            } catch (e: HttpForbiddenException) {
                attempt++
                if (attempt >= 3) throw e
                Log.w(TAG, "HTTP 403 — refreshing stream URLs (attempt $attempt)")
                DownloadProgressHub.update(jobId) {
                    it.copy(message = "Refreshing link…")
                }
                lookup = YoutubeExtractor.lookup(lookupSeed.webpageUrl)
                option = YoutubeExtractor.findOption(lookup, optionSeed.id)
                    ?: YoutubeExtractor.findOption(lookup, option.id)
                    ?: option
            }
        }
        @Suppress("UNREACHABLE_CODE")
        error("unreachable")
    }

    suspend fun convertToMp3(
        source: SavedVideo,
        jobId: String,
        overwrite: Boolean = true,
    ): SavedVideo = withContext(Dispatchers.IO) {
        if (source.format.equals("mp3", ignoreCase = true)) {
            error("Already an MP3 file")
        }
        val entryId = when {
            source.id.endsWith("_audio") -> source.id
            source.id.endsWith("_video") -> source.id.removeSuffix("_video") + "_audio"
            else -> "${source.id}_audio"
        }
        DownloadProgressHub.update(jobId) {
            it.copy(
                entryId = entryId,
                title = source.title,
                state = DownloadJobState.Running,
                percent = 10,
                message = "Encoding MP3…",
            )
        }
        val workDir = File(appContext.cacheDir, "ffmpeg_$jobId").apply { mkdirs() }
        try {
            val inputLocal = materializeToFile(source.filePath, File(workDir, "source.bin"))
            val outMp3 = File(workDir, "out.mp3")
            DownloadProgressHub.update(jobId) {
                it.copy(percent = 40, message = "Encoding MP3 320 kbps…")
            }
            val coverSrc = resolveCoverArt(
                entryId = entryId,
                preferred = source.thumbnailUrl,
                alsoTryEntryId = source.id,
                videoFileRef = source.filePath,
            )
            val coverPath = coverSrc?.let { src ->
                libraryStore.thumbnailStore()
                    .writeSquareCover(src, File(workDir, "cover.jpg"))
                    ?: src
            }
            FfmpegBridge.encodeMp3(
                inputPath = inputLocal.absolutePath,
                outMp3 = outMp3.absolutePath,
                bitrateKbps = 320,
                coverArtPath = coverPath,
                title = source.title,
                artist = source.uploader,
            ).getOrElse { throw it }

            if (overwrite) {
                libraryStore.getById(entryId)?.let { storage.deleteRef(it.filePath) }
            }
            val safeTitle = sanitizeTitle(source.title, source.id)
            val fileName = uniqueFileName(safeTitle, "mp3", entryId, overwrite, MediaType.AUDIO)
            val (fileRef, size) = copyFileToLibrary(outMp3, fileName, MediaType.AUDIO)

            val localThumb = coverSrc
                ?: libraryStore.thumbnailStore().pathIfExists(entryId)
                ?: source.thumbnailUrl

            val saved = SavedVideo(
                id = entryId,
                title = source.title,
                uploader = source.uploader,
                durationSeconds = source.durationSeconds,
                thumbnailUrl = localThumb,
                webpageUrl = source.webpageUrl,
                quality = "MP3 320",
                fileName = fileName,
                filePath = fileRef,
                fileSizeBytes = size,
                downloadedAtEpochMs = System.currentTimeMillis(),
                mediaType = MediaType.AUDIO,
                format = "mp3",
                isFavourite = source.isFavourite,
            )
            libraryStore.upsert(saved)
            DownloadProgressHub.update(jobId) {
                it.copy(
                    state = DownloadJobState.Success,
                    percent = 100,
                    downloadedBytes = size,
                    totalBytes = size,
                    message = null,
                    video = saved,
                )
            }
            saved
        } catch (t: Throwable) {
            DownloadProgressHub.update(jobId) {
                it.copy(
                    state = DownloadJobState.Failed,
                    message = t.message ?: "Convert failed",
                )
            }
            throw t
        } finally {
            workDir.deleteRecursively()
        }
    }

    private suspend fun downloadProgressive(
        lookup: VideoLookup,
        option: MediaDownloadOption,
        jobId: String,
        overwrite: Boolean,
    ): SavedVideo {
        val entryId = entryIdForVideo(lookup)
        val url = option.videoUrl ?: error("Missing progressive URL")
        beginJob(jobId, entryId, lookup.title, option.approxSizeBytes)
        try {
            deleteIfOverwrite(entryId, overwrite)
            val safeTitle = sanitizeTitle(lookup.title, lookup.id)
            val fileName = uniqueFileName(safeTitle, "mp4", entryId, overwrite, MediaType.VIDEO)
            val (fileRef, size) = downloadUrlToStorage(
                url = url,
                fileName = fileName,
                approxSize = option.approxSizeBytes,
                title = lookup.title,
                jobId = jobId,
                percentStart = 0,
                percentEnd = 95,
                mediaType = MediaType.VIDEO,
            )
            return finalizeVideo(lookup, entryId, fileName, fileRef, size, option.label, jobId)
        } catch (t: Throwable) {
            failJob(jobId, t)
            throw t
        }
    }

    private suspend fun downloadHqVideo(
        lookup: VideoLookup,
        option: MediaDownloadOption,
        jobId: String,
        overwrite: Boolean,
    ): SavedVideo {
        val entryId = entryIdForVideo(lookup)
        val videoUrl = option.videoUrl ?: error("Missing video stream")
        val audioUrl = option.audioUrl ?: error("Missing audio stream")
        beginJob(jobId, entryId, lookup.title, option.approxSizeBytes, "Downloading video…")
        val workDir = File(appContext.cacheDir, "ffmpeg_$jobId").apply { mkdirs() }
        try {
            deleteIfOverwrite(entryId, overwrite)
            val videoPart = File(workDir, "video.${option.videoExt}")
            val audioPart = File(workDir, "audio.${option.audioExt}")
            DownloadProgressHub.update(jobId) {
                it.copy(percent = 5, message = "Downloading video + audio…")
            }
            val videoBytes = AtomicLong(0)
            val audioBytes = AtomicLong(0)
            val videoTotalHint = AtomicLong(parseClen(videoUrl) ?: 0L)
            val audioTotalHint = AtomicLong(parseClen(audioUrl) ?: 0L)
            coroutineScope {
                val videoJob = async(Dispatchers.IO) {
                    downloadUrlToFile(
                        url = videoUrl,
                        dest = videoPart,
                        approxSize = parseClen(videoUrl),
                        title = lookup.title,
                        jobId = jobId,
                        percentStart = 5,
                        percentEnd = 55,
                        phase = "Downloading video…",
                        progressWeight = videoBytes,
                        siblingWeight = audioBytes,
                        progressTotalHint = videoTotalHint,
                        siblingTotalHint = audioTotalHint,
                        combinedPhase = true,
                    )
                }
                val audioJob = async(Dispatchers.IO) {
                    downloadUrlToFile(
                        url = audioUrl,
                        dest = audioPart,
                        approxSize = parseClen(audioUrl),
                        title = lookup.title,
                        jobId = jobId,
                        percentStart = 5,
                        percentEnd = 55,
                        phase = "Downloading audio…",
                        progressWeight = audioBytes,
                        siblingWeight = videoBytes,
                        progressTotalHint = audioTotalHint,
                        siblingTotalHint = videoTotalHint,
                        combinedPhase = true,
                    )
                }
                videoJob.await()
                audioJob.await()
            }
            DownloadProgressHub.update(jobId) {
                it.copy(percent = 75, message = "Merging MP4…")
            }
            val merged = File(workDir, "merged.mp4")
            FfmpegBridge.mergeAv(videoPart.absolutePath, audioPart.absolutePath, merged.absolutePath)
                .getOrElse { throw it }

            val safeTitle = sanitizeTitle(lookup.title, lookup.id)
            val fileName = uniqueFileName(safeTitle, "mp4", entryId, overwrite, MediaType.VIDEO)
            val (fileRef, size) = copyFileToLibrary(merged, fileName, MediaType.VIDEO)
            DownloadProgressHub.update(jobId) {
                it.copy(percent = 95, message = "Saving…")
            }
            return finalizeVideo(lookup, entryId, fileName, fileRef, size, option.label, jobId)
        } catch (t: Throwable) {
            failJob(jobId, t)
            throw t
        } finally {
            workDir.deleteRecursively()
        }
    }

    private suspend fun downloadMp3(
        lookup: VideoLookup,
        option: MediaDownloadOption,
        jobId: String,
        overwrite: Boolean,
    ): SavedVideo {
        val entryId = entryIdForAudio(lookup)
        val audioUrl = option.audioUrl ?: error("Missing audio stream")
        beginJob(jobId, entryId, lookup.title, option.approxSizeBytes, "Downloading audio…")
        val workDir = File(appContext.cacheDir, "ffmpeg_$jobId").apply { mkdirs() }
        try {
            deleteIfOverwrite(entryId, overwrite)
            val audioPart = File(workDir, "audio.${option.audioExt}")
            downloadUrlToFile(
                url = audioUrl,
                dest = audioPart,
                approxSize = option.approxSizeBytes,
                title = lookup.title,
                jobId = jobId,
                percentStart = 0,
                percentEnd = 55,
                phase = "Downloading audio…",
            )
            DownloadProgressHub.update(jobId) {
                it.copy(percent = 60, message = "Encoding MP3 320 kbps…")
            }
            val outMp3 = File(workDir, "out.mp3")
            val coverSrc = resolveCoverArt(
                entryId = entryId,
                preferred = lookup.thumbnailUrl,
                alsoTryEntryId = entryIdForVideo(lookup),
                videoFileRef = null,
            )
            val coverPath = coverSrc?.let { src ->
                libraryStore.thumbnailStore()
                    .writeSquareCover(src, File(workDir, "cover.jpg"))
                    ?: src
            }
            FfmpegBridge.encodeMp3(
                inputPath = audioPart.absolutePath,
                outMp3 = outMp3.absolutePath,
                bitrateKbps = 320,
                coverArtPath = coverPath,
                title = lookup.title,
                artist = lookup.uploader,
            ).getOrElse { throw it }

            val safeTitle = sanitizeTitle(lookup.title, lookup.id)
            val fileName = uniqueFileName(safeTitle, "mp3", entryId, overwrite, MediaType.AUDIO)
            val (fileRef, size) = copyFileToLibrary(outMp3, fileName, MediaType.AUDIO)

            val localThumb = coverSrc
                ?: libraryStore.thumbnailStore().pathIfExists(entryId)
                ?: lookup.thumbnailUrl

            val saved = SavedVideo(
                id = entryId,
                title = lookup.title,
                uploader = lookup.uploader,
                durationSeconds = lookup.durationSeconds,
                thumbnailUrl = localThumb,
                webpageUrl = lookup.webpageUrl,
                quality = "MP3 320",
                fileName = fileName,
                filePath = fileRef,
                fileSizeBytes = size,
                downloadedAtEpochMs = System.currentTimeMillis(),
                mediaType = MediaType.AUDIO,
                format = "mp3",
            )
            libraryStore.upsert(saved)
            DownloadProgressHub.update(jobId) {
                it.copy(
                    state = DownloadJobState.Success,
                    percent = 100,
                    downloadedBytes = size,
                    totalBytes = size,
                    message = null,
                    video = saved,
                )
            }
            return saved
        } catch (t: Throwable) {
            failJob(jobId, t)
            throw t
        } finally {
            workDir.deleteRecursively()
        }
    }

    private suspend fun finalizeVideo(
        lookup: VideoLookup,
        entryId: String,
        fileName: String,
        fileRef: String,
        size: Long,
        qualityLabel: String,
        jobId: String,
    ): SavedVideo {
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
            quality = qualityLabel,
            fileName = fileName,
            filePath = fileRef,
            fileSizeBytes = size,
            downloadedAtEpochMs = System.currentTimeMillis(),
            mediaType = MediaType.VIDEO,
            format = "mp4",
        )
        libraryStore.upsert(saved)
        DownloadProgressHub.update(jobId) {
            it.copy(
                state = DownloadJobState.Success,
                percent = 100,
                downloadedBytes = size,
                totalBytes = size,
                message = null,
                video = saved,
            )
        }
        return saved
    }

    private fun beginJob(
        jobId: String,
        entryId: String,
        title: String,
        totalBytes: Long?,
        message: String? = null,
    ) {
        DownloadProgressHub.update(jobId) {
            it.copy(
                entryId = entryId,
                title = title,
                state = DownloadJobState.Running,
                percent = 0,
                downloadedBytes = 0,
                totalBytes = totalBytes,
                message = message,
            )
        }
    }

    private fun failJob(jobId: String, t: Throwable) {
        DownloadProgressHub.update(jobId) {
            it.copy(
                state = DownloadJobState.Failed,
                message = t.message ?: "Download failed",
            )
        }
    }

    private fun deleteIfOverwrite(entryId: String, overwrite: Boolean) {
        if (!overwrite) return
        libraryStore.getById(entryId)?.let { existing ->
            storage.deleteRef(existing.filePath)
        }
    }

    private fun uniqueFileName(
        safeTitle: String,
        ext: String,
        entryId: String,
        overwrite: Boolean,
        mediaType: MediaType,
    ): String {
        val base = "$safeTitle.$ext"
        if (overwrite) return base
        val existingSameId = libraryStore.getById(entryId)
        if (existingSameId != null) return base

        if (!storage.nameExists(base, mediaType) && libraryStore.findByFileName(base) == null) {
            return base
        }
        var n = 2
        while (n < 1000) {
            val candidate = "$safeTitle ($n).$ext"
            if (!storage.nameExists(candidate, mediaType) &&
                libraryStore.findByFileName(candidate) == null
            ) {
                return candidate
            }
            n++
        }
        return "$safeTitle-${System.currentTimeMillis()}.$ext"
    }

    private fun copyFileToLibrary(
        source: File,
        fileName: String,
        mediaType: MediaType,
    ): Pair<String, Long> {
        val (fileRef, out) = storage.openOutput(fileName, mediaType)
        out.use { output ->
            source.inputStream().use { input -> input.copyTo(output) }
        }
        return fileRef to storage.length(fileRef)
    }

    private fun materializeToFile(fileRef: String, dest: File): File {
        dest.parentFile?.mkdirs()
        if (fileRef.startsWith("content:")) {
            appContext.contentResolver.openInputStream(Uri.parse(fileRef))?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: error("Cannot read source file")
        } else {
            File(fileRef).inputStream().use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return dest
    }

    /**
     * Local JPEG path suitable for FFmpeg ID3 cover embedding.
     * Prefers cached thumbs, then remote download, then a frame from the video.
     */
    private fun resolveCoverArt(
        entryId: String,
        preferred: String?,
        alsoTryEntryId: String?,
        videoFileRef: String?,
    ): String? {
        val thumbs = libraryStore.thumbnailStore()
        thumbs.pathIfExists(entryId)?.let { return it }

        alsoTryEntryId?.let { otherId ->
            thumbs.pathIfExists(otherId)?.let { existing ->
                val copied = runCatching {
                    val dest = thumbs.fileFor(entryId)
                    if (dest.absolutePath != existing) {
                        File(existing).inputStream().use { input ->
                            dest.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                    dest.takeIf { it.exists() && it.length() > 0 }?.absolutePath
                }.getOrNull()
                return copied ?: existing
            }
        }

        if (!preferred.isNullOrBlank()) {
            if (thumbs.isLocalPath(preferred)) {
                val local = preferred.removePrefix("file://")
                if (File(local).isFile && File(local).length() > 0L) {
                    runCatching {
                        val dest = thumbs.fileFor(entryId)
                        if (dest.absolutePath != File(local).absolutePath) {
                            File(local).inputStream().use { input ->
                                dest.outputStream().use { output -> input.copyTo(output) }
                            }
                        }
                        dest.takeIf { it.exists() && it.length() > 0 }?.absolutePath
                    }.getOrNull()?.let { return it }
                    return local
                }
            }
            thumbs.cacheFromUrl(entryId, preferred)?.let { return it }
        }

        if (!videoFileRef.isNullOrBlank()) {
            thumbs.cacheFromVideo(entryId, videoFileRef)?.let { return it }
        }
        return null
    }

    private fun downloadUrlToStorage(
        url: String,
        fileName: String,
        approxSize: Long?,
        title: String,
        jobId: String,
        percentStart: Int,
        percentEnd: Int,
        mediaType: MediaType,
    ): Pair<String, Long> {
        val tempFile = File(storage.defaultDir(mediaType), "$fileName.part")
        downloadUrlToFile(
            url = url,
            dest = tempFile,
            approxSize = approxSize,
            title = title,
            jobId = jobId,
            percentStart = percentStart,
            percentEnd = percentEnd,
            phase = null,
        )
        val (fileRef, out) = storage.openOutput(fileName, mediaType)
        out.use { output ->
            tempFile.inputStream().use { input -> input.copyTo(output, DownloadBufferBytes) }
        }
        tempFile.delete()
        val size = storage.length(fileRef)
        return fileRef to size
    }

    /**
     * Resumable CDN download via bounded Range chunks (HTTP/1.1).
     * Avoids open-ended Range / dual range= query which googlevideo answers with 416.
     */
    private fun downloadUrlToFile(
        url: String,
        dest: File,
        approxSize: Long?,
        title: String,
        jobId: String,
        percentStart: Int,
        percentEnd: Int,
        phase: String?,
        progressWeight: AtomicLong? = null,
        siblingWeight: AtomicLong? = null,
        progressTotalHint: AtomicLong? = null,
        siblingTotalHint: AtomicLong? = null,
        combinedPhase: Boolean = false,
    ) {
        dest.parentFile?.mkdirs()
        val cleanUrl = stripRangeParam(url)
        var knownTotal = approxSize ?: parseClen(cleanUrl)
        knownTotal?.let { progressTotalHint?.compareAndSet(0L, it) }

        var attempt = 0
        var lastError: Throwable? = null
        var forceRestart = false

        while (attempt < MaxAttempts) {
            attempt++
            try {
                if (forceRestart) {
                    dest.delete()
                    forceRestart = false
                }
                var already = if (dest.exists()) dest.length() else 0L
                if (knownTotal != null && already >= knownTotal) {
                    progressWeight?.set(already)
                    emitProgress(
                        jobId, title, phase, percentStart, percentEnd,
                        already, knownTotal, progressWeight, siblingWeight,
                        progressTotalHint, siblingTotalHint, combinedPhase, forceEnd = true,
                    )
                    return
                }

                var lastEmitMs = 0L
                progressWeight?.set(already)

                while (true) {
                    already = if (dest.exists()) dest.length() else 0L
                    if (knownTotal != null && already >= knownTotal) break

                    val endExclusive = if (knownTotal != null) {
                        minOf(already + ChunkBytes, knownTotal)
                    } else {
                        already + ChunkBytes
                    }
                    if (endExclusive <= already) break
                    val endInclusive = endExclusive - 1

                    val request = Request.Builder()
                        .url(cleanUrl)
                        .header("User-Agent", UserAgent)
                        .header("Referer", "https://www.youtube.com/")
                        .header("Accept", "*/*")
                        .header("Accept-Encoding", "identity")
                        .header("Range", "bytes=$already-$endInclusive")
                        .build()

                    client.newCall(request).execute().use { response ->
                        val code = response.code
                        if (code == 403 || code == 401) {
                            throw HttpForbiddenException("Download failed (HTTP $code)")
                        }
                        if (code == 416) {
                            val totalFromHeader = parseContentRangeTotal(response.header("Content-Range"))
                            if (totalFromHeader != null) {
                                knownTotal = totalFromHeader
                                progressTotalHint?.set(totalFromHeader)
                                if (already >= totalFromHeader) {
                                    return
                                }
                            }
                            // Offset invalid for this URL — restart once from byte 0.
                            Log.w(TAG, "HTTP 416 at offset $already — restarting file")
                            forceRestart = true
                            throw RangeRestartException("HTTP 416 at $already")
                        }
                        if (code != 206 && code != 200) {
                            error("Download failed (HTTP $code)")
                        }

                        // Server ignored Range and sent full body — rewrite from 0.
                        val append = already > 0L && code == 206
                        if (already > 0L && code == 200) {
                            dest.delete()
                            already = 0L
                        }

                        val body = response.body ?: error("Empty response body")
                        val bodyLen = body.contentLength().takeIf { it > 0 }
                        when {
                            code == 200 && bodyLen != null -> {
                                knownTotal = bodyLen
                                progressTotalHint?.set(bodyLen)
                            }
                            code == 206 -> {
                                parseContentRangeTotal(response.header("Content-Range"))?.let {
                                    knownTotal = it
                                    progressTotalHint?.set(it)
                                }
                            }
                        }

                        var downloaded = if (append) already else 0L
                        progressWeight?.set(downloaded)
                        FileOutputStream(dest, append).use { output ->
                            body.byteStream().use { input ->
                                val buffer = ByteArray(DownloadBufferBytes)
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read <= 0) break
                                    output.write(buffer, 0, read)
                                    downloaded += read
                                    progressWeight?.set(downloaded)
                                    val now = System.currentTimeMillis()
                                    if (now - lastEmitMs >= ProgressMinIntervalMs) {
                                        lastEmitMs = now
                                        emitProgress(
                                            jobId, title, phase, percentStart, percentEnd,
                                            downloaded, knownTotal, progressWeight, siblingWeight,
                                            progressTotalHint, siblingTotalHint, combinedPhase,
                                        )
                                    }
                                }
                            }
                            output.fd.sync()
                        }
                        progressWeight?.set(downloaded)

                        // Full-file 200: done. Short final chunk: done when knownTotal met.
                        if (code == 200) return
                        val totalNow = knownTotal
                        if (totalNow != null && downloaded >= totalNow) return
                        // Unknown length: stop when server returned less than a full chunk.
                        if (totalNow == null && bodyLen != null && bodyLen < ChunkBytes) return
                    }
                }

                val finalLen = if (dest.exists()) dest.length() else 0L
                progressWeight?.set(finalLen)
                emitProgress(
                    jobId, title, phase, percentStart, percentEnd,
                    finalLen, knownTotal ?: finalLen, progressWeight, siblingWeight,
                    progressTotalHint, siblingTotalHint, combinedPhase, forceEnd = true,
                )
                return
            } catch (t: Throwable) {
                lastError = t
                if (t is HttpForbiddenException) throw t
                val retryable = t is RangeRestartException || t.isRetryableNetwork()
                Log.w(TAG, "Download attempt $attempt failed (retryable=$retryable): ${t.message}")
                if (!retryable || attempt >= MaxAttempts) break
                try {
                    Thread.sleep(350L * attempt)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }
        throw lastError ?: IOException("Download failed")
    }

    private fun emitProgress(
        jobId: String,
        title: String,
        phase: String?,
        percentStart: Int,
        percentEnd: Int,
        downloaded: Long,
        total: Long?,
        progressWeight: AtomicLong?,
        siblingWeight: AtomicLong?,
        progressTotalHint: AtomicLong?,
        siblingTotalHint: AtomicLong?,
        combinedPhase: Boolean,
        forceEnd: Boolean = false,
    ) {
        val span = (percentEnd - percentStart).coerceAtLeast(1)
        val percent = if (combinedPhase && progressWeight != null && siblingWeight != null) {
            val a = progressWeight.get().toDouble()
            val b = siblingWeight.get().toDouble()
            val ta = (progressTotalHint?.get() ?: 0L).takeIf { it > 0 }?.toDouble()
                ?: a.coerceAtLeast(1.0)
            val tb = (siblingTotalHint?.get() ?: 0L).takeIf { it > 0 }?.toDouble()
                ?: b.coerceAtLeast(1.0)
            // Average of each stream's completion — stable even when sizes differ a lot.
            val frac = ((a / ta).coerceIn(0.0, 1.0) + (b / tb).coerceIn(0.0, 1.0)) / 2.0
            val local = (frac * span).toInt().coerceIn(0, span)
            (percentStart + local).coerceIn(0, if (forceEnd) percentEnd else 99)
        } else {
            val localPct = if (total != null && total > 0) {
                ((downloaded * span) / total).toInt().coerceIn(0, span)
            } else {
                0
            }
            (percentStart + localPct).coerceIn(0, if (forceEnd) percentEnd else 99)
        }
        val combinedBytes = if (combinedPhase && progressWeight != null && siblingWeight != null) {
            progressWeight.get() + siblingWeight.get()
        } else {
            downloaded
        }
        val combinedTotal = if (combinedPhase && progressTotalHint != null && siblingTotalHint != null) {
            val ta = progressTotalHint.get()
            val tb = siblingTotalHint.get()
            if (ta > 0 && tb > 0) ta + tb else total
        } else {
            total
        }
        DownloadProgressHub.update(jobId) {
            // Never let percent tick backwards during a run (parallel streams used to bounce).
            val nextPercent = if (it.state == DownloadJobState.Running && !forceEnd) {
                maxOf(it.percent, percent)
            } else {
                percent
            }
            it.copy(
                title = title,
                percent = nextPercent,
                downloadedBytes = combinedBytes,
                totalBytes = combinedTotal,
                state = DownloadJobState.Running,
                message = if (combinedPhase) "Downloading video + audio…" else phase,
            )
        }
    }

    /** Remove any extractor-supplied range= so we only range via the HTTP header. */
    private fun stripRangeParam(url: String): String {
        val base = url.substringBefore('#')
        val hash = url.substringAfter('#', missingDelimiterValue = "")
        val stripped = base
            .replace(Regex("""([?&])range=[^&]*"""), "$1")
            .replace(Regex("""\?&"""), "?")
            .replace(Regex("""&&+"""), "&")
            .trimEnd('?', '&')
        return if (hash.isEmpty()) stripped else "$stripped#$hash"
    }

    private fun parseClen(url: String): Long? {
        val m = Regex("""[?&]clen=(\d+)""").find(url) ?: return null
        return m.groupValues[1].toLongOrNull()?.takeIf { it > 0 }
    }

    private fun parseContentRangeTotal(header: String?): Long? {
        if (header.isNullOrBlank()) return null
        // bytes 0-1023/2048  or  bytes */2048
        val m = Regex("""bytes\s+(?:\d+-\d+|\*)/(\d+)""").find(header) ?: return null
        return m.groupValues[1].toLongOrNull()?.takeIf { it > 0 }
    }

    private fun Throwable.isRetryableNetwork(): Boolean {
        var cur: Throwable? = this
        while (cur != null) {
            val msg = cur.message.orEmpty().lowercase()
            if (
                cur is SocketException ||
                cur is IOException ||
                msg.contains("connection reset") ||
                msg.contains("broken pipe") ||
                msg.contains("software caused connection abort") ||
                msg.contains("unexpected end of stream") ||
                msg.contains("timeout") ||
                msg.contains("stream was reset") ||
                msg.contains("http 416")
            ) {
                return true
            }
            cur = cur.cause
        }
        return false
    }
}

/** Signals a 416 resume failure that should wipe the partial and retry from 0. */
private class RangeRestartException(message: String) : IOException(message)

/** Thrown when googlevideo rejects the URL (usually expired or wrong client UA). */
class HttpForbiddenException(message: String) : IOException(message)
