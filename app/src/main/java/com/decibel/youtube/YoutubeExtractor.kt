package com.decibel.youtube

import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import java.util.Locale

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
    /** Best stream URL for audio-only / music playback (online). */
    val playbackUrl: String,
)

object YoutubeExtractor {
    fun lookup(rawUrl: String): VideoLookup {
        ExtractorBootstrap.init()
        val url = normalizeUrl(rawUrl)
        val info = StreamInfo.getInfo(ServiceList.YouTube, url)

        val videoFormats = info.videoStreams
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

        val playbackUrl = pickPlaybackUrl(info)
            ?: videoFormats.firstOrNull()?.url
            ?: error("No playable stream found for this video.")

        return VideoLookup(
            id = info.id,
            title = info.name.orEmpty().ifBlank { "Untitled" },
            uploader = info.uploaderName.orEmpty().ifBlank { "Unknown" },
            durationSeconds = info.duration,
            thumbnailUrl = info.thumbnails.maxByOrNull { it.height }?.url,
            webpageUrl = info.url ?: url,
            videoFormats = videoFormats,
            playbackUrl = playbackUrl,
        )
    }

    fun resolvePlaybackUrl(webpageUrl: String): String {
        return lookup(webpageUrl).playbackUrl
    }

    private fun pickPlaybackUrl(info: StreamInfo): String? {
        // Prefer progressive video+audio so the in-app player can show video.
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

        return info.audioStreams
            .asSequence()
            .filter { it.isUrl && it.content.isNotBlank() }
            .sortedWith(
                compareByDescending<AudioStream> { it.averageBitrate }
                    .thenByDescending { it.bitrate },
            )
            .firstOrNull()
            ?.content
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
