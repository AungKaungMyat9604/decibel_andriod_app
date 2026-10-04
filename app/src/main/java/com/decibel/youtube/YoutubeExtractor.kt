package com.decibel.youtube

import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import java.util.Locale

enum class DownloadKind {
    VIDEO_HQ,
    AUDIO_MP3,
    PROGRESSIVE,
}

data class MediaDownloadOption(
    val id: String,
    val kind: DownloadKind,
    val label: String,
    val videoUrl: String? = null,
    val audioUrl: String? = null,
    val videoExt: String = "mp4",
    val audioExt: String = "m4a",
    val height: Int = 0,
    val approxSizeBytes: Long? = null,
)

/** @deprecated Prefer [MediaDownloadOption]; kept for progressive mapping helpers. */
data class VideoFormatOption(
    val quality: String,
    val resolution: String,
    val format: String,
    val url: String,
    val approxSizeBytes: Long?,
    val bitrate: Int = 0,
)

data class VideoLookup(
    val id: String,
    val title: String,
    val uploader: String,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val webpageUrl: String,
    val videoFormats: List<VideoFormatOption>,
    val downloadOptions: List<MediaDownloadOption>,
    /** Best stream URL for audio-only / music playback (online). */
    val playbackUrl: String,
)

object YoutubeExtractor {
    private val PreferredHeights = listOf(2160, 1440, 1080, 720, 480)

    fun lookup(rawUrl: String): VideoLookup {
        ExtractorBootstrap.init()
        val url = normalizeUrl(rawUrl)
        val info = StreamInfo.getInfo(ServiceList.YouTube, url)

        val progressive = info.videoStreams
            .asSequence()
            .filter { stream ->
                !stream.isVideoOnly() &&
                    stream.isUrl &&
                    stream.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP &&
                    stream.content.isNotBlank()
            }
            .distinctBy { "${it.getResolution()}|${it.format?.suffix}|${it.bitrate}" }
            .sortedWith(
                compareByDescending<VideoStream> { resolutionRank(it.getResolution()) }
                    .thenByDescending { it.bitrate },
            )
            .map { stream ->
                val resolution = stream.getResolution().ifBlank { "unknown" }
                VideoFormatOption(
                    quality = resolution,
                    resolution = resolution,
                    format = stream.format?.suffix ?: "mp4",
                    url = stream.content,
                    approxSizeBytes = stream.itagItem?.contentLength,
                    bitrate = stream.bitrate,
                )
            }
            .toList()

        val bestAudio = pickBestAudio(info.audioStreams)
        val hqOptions = buildHqVideoOptions(info.videoOnlyStreams, bestAudio)
        val mp3Option = bestAudio?.let { audio ->
            MediaDownloadOption(
                id = "mp3_320",
                kind = DownloadKind.AUDIO_MP3,
                label = "MP3 320 kbps",
                audioUrl = audio.content,
                audioExt = audio.format?.suffix ?: "m4a",
                approxSizeBytes = audio.itagItem?.contentLength,
            )
        }
        val progressiveOptions = progressive.map { fmt ->
            MediaDownloadOption(
                id = "prog_${fmt.resolution}_${fmt.format}",
                kind = DownloadKind.PROGRESSIVE,
                label = "${fmt.resolution} (quick)",
                videoUrl = fmt.url,
                videoExt = fmt.format.ifBlank { "mp4" },
                height = resolutionRank(fmt.resolution),
                approxSizeBytes = fmt.approxSizeBytes,
            )
        }

        val downloadOptions = buildList {
            addAll(hqOptions)
            mp3Option?.let { add(it) }
            addAll(progressiveOptions)
        }

        val playbackUrl = pickPlaybackUrl(info)
            ?: progressive.firstOrNull()?.url
            ?: bestAudio?.content
            ?: error("No playable stream found for this video.")

        return VideoLookup(
            id = info.id,
            title = info.name.orEmpty().ifBlank { "Untitled" },
            uploader = info.uploaderName.orEmpty().ifBlank { "Unknown" },
            durationSeconds = info.duration,
            thumbnailUrl = info.thumbnails.maxByOrNull { it.height }?.url,
            webpageUrl = info.url ?: url,
            videoFormats = progressive,
            downloadOptions = downloadOptions,
            playbackUrl = playbackUrl,
        )
    }

    fun resolvePlaybackUrl(webpageUrl: String): String {
        return lookup(webpageUrl).playbackUrl
    }

    fun findOption(lookup: VideoLookup, optionId: String): MediaDownloadOption? {
        lookup.downloadOptions.find { it.id == optionId }?.let { return it }
        // Re-match by kind+height if id drifted after refresh
        val parts = optionId.split('_')
        if (parts.firstOrNull() == "hq" && parts.size >= 2) {
            val height = parts[1].toIntOrNull() ?: return null
            return lookup.downloadOptions
                .filter { it.kind == DownloadKind.VIDEO_HQ }
                .minByOrNull { kotlin.math.abs(it.height - height) }
        }
        if (optionId.startsWith("mp3")) {
            return lookup.downloadOptions.find { it.kind == DownloadKind.AUDIO_MP3 }
        }
        return null
    }

    private fun buildHqVideoOptions(
        videoOnlyStreams: List<VideoStream>,
        bestAudio: AudioStream?,
    ): List<MediaDownloadOption> {
        if (bestAudio == null) return emptyList()
        val audioUrl = bestAudio.content.takeIf { it.isNotBlank() } ?: return emptyList()
        val audioExt = bestAudio.format?.suffix ?: "m4a"
        val audioLen = bestAudio.itagItem?.contentLength ?: 0L

        // NewPipe keeps DASH video-only on StreamInfo.videoOnlyStreams (not videoStreams).
        val onlyVideo = videoOnlyStreams
            .asSequence()
            .filter {
                it.content.isNotBlank() &&
                    it.deliveryMethod != DeliveryMethod.TORRENT
            }
            .toList()
        if (onlyVideo.isEmpty()) return emptyList()

        val byHeight = onlyVideo
            .groupBy { resolutionRank(it.getResolution()) }
            .filterKeys { it >= 360 }
            .toSortedMap(compareByDescending { it })

        val pickedHeights = LinkedHashSet<Int>()
        for (preferred in PreferredHeights) {
            if (byHeight.containsKey(preferred)) pickedHeights.add(preferred)
        }
        for (h in byHeight.keys) {
            if (pickedHeights.size >= 5) break
            pickedHeights.add(h)
        }

        return pickedHeights.mapNotNull { height ->
            val candidates = byHeight[height] ?: return@mapNotNull null
            val chosen = candidates.minWithOrNull(
                compareBy<VideoStream> { videoCodecRank(it) }
                    .thenByDescending { it.bitrate },
            ) ?: return@mapNotNull null
            val vExt = chosen.format?.suffix ?: "mp4"
            val vLen = chosen.itagItem?.contentLength ?: 0L
            val approx = (vLen + audioLen).takeIf { it > 0 }
            MediaDownloadOption(
                id = "hq_${height}_$vExt",
                kind = DownloadKind.VIDEO_HQ,
                label = "${height}p MP4",
                videoUrl = chosen.content,
                audioUrl = audioUrl,
                videoExt = vExt,
                audioExt = audioExt,
                height = height,
                approxSizeBytes = approx,
            )
        }
    }

    /** Lower is better: prefer AVC/mp4 for MP4 remux. */
    private fun videoCodecRank(stream: VideoStream): Int {
        val suffix = stream.format?.suffix?.lowercase().orEmpty()
        val name = stream.format?.name?.lowercase().orEmpty()
        return when {
            suffix == "mp4" || name.contains("avc") || name.contains("h264") -> 0
            suffix == "webm" || name.contains("vp9") || name.contains("vp09") -> 1
            name.contains("av01") || name.contains("av1") -> 2
            else -> 3
        }
    }

    private fun pickBestAudio(streams: List<AudioStream>): AudioStream? {
        return streams
            .asSequence()
            .filter { it.content.isNotBlank() }
            .sortedWith(
                compareBy<AudioStream> { audioCodecRank(it) }
                    .thenByDescending { it.averageBitrate.takeIf { b -> b > 0 } ?: it.bitrate },
            )
            .firstOrNull()
    }

    private fun audioCodecRank(stream: AudioStream): Int {
        val suffix = stream.format?.suffix?.lowercase().orEmpty()
        val name = stream.format?.name?.lowercase().orEmpty()
        return when {
            suffix == "m4a" || name.contains("mp4a") || name.contains("aac") -> 0
            suffix == "webm" || name.contains("opus") -> 1
            else -> 2
        }
    }

    private fun pickPlaybackUrl(info: StreamInfo): String? {
        val progressiveVideo = info.videoStreams
            .asSequence()
            .filter {
                !it.isVideoOnly() &&
                    it.isUrl &&
                    it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP &&
                    it.content.isNotBlank()
            }
            .sortedWith(
                compareByDescending<VideoStream> { resolutionRank(it.getResolution()) }
                    .thenByDescending { it.bitrate },
            )
            .firstOrNull()
            ?.content
        if (!progressiveVideo.isNullOrBlank()) return progressiveVideo

        return pickBestAudio(info.audioStreams)?.content
    }

    fun normalizeUrl(input: String): String {
        val trimmed = input.trim()
        val match = Regex(
            """(?:https?://)?(?:www\.)?(?:youtube\.com/watch\?v=|youtu\.be/|youtube\.com/shorts/)([A-Za-z0-9_-]{6,})""",
        ).find(trimmed)
        return if (match != null) {
            "https://www.youtube.com/watch?v=${match.groupValues[1]}"
        } else {
            trimmed
        }
    }

    private fun resolutionRank(resolution: String?): Int {
        if (resolution.isNullOrBlank()) return 0
        // "1080p", "720p60"
        Regex("""(\d{3,4})p""", RegexOption.IGNORE_CASE).find(resolution)
            ?.groupValues?.get(1)?.toIntOrNull()
            ?.let { return it }
        // "1920x1080" → height
        Regex("""(\d+)\s*[x×]\s*(\d+)""", RegexOption.IGNORE_CASE).find(resolution)
            ?.groupValues?.get(2)?.toIntOrNull()
            ?.let { return it }
        val digits = Regex("""(\d+)""").find(resolution)?.groupValues?.get(1) ?: return 0
        return digits.toIntOrNull() ?: 0
    }
}

fun Long.formatDuration(): String {
    val total = coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) {
        String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%d:%02d", m, s)
    }
}

fun Long?.formatBytes(): String {
    if (this == null || this <= 0) return "—"
    val kb = this / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1 -> String.format(Locale.US, "%.1f MB", mb)
        else -> String.format(Locale.US, "%.0f KB", kb)
    }
}
