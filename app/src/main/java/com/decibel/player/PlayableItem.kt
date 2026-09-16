package com.decibel.player

import com.decibel.data.SavedVideo
import com.decibel.youtube.CatalogVideo

/**
 * Unified queue item for online streams and offline files.
 * Progressive video+audio when available; audio-only streams fall back to artwork.
 */
data class PlayableItem(
    val id: String,
    val title: String,
    val uploader: String,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val webpageUrl: String,
    /** Local absolute path when offline; null when online-only. */
    val localPath: String? = null,
) {
    val isLocal: Boolean get() = !localPath.isNullOrBlank()

    companion object {
        fun fromLocal(video: SavedVideo) = PlayableItem(
            id = video.id,
            title = video.title,
            uploader = video.uploader,
            durationSeconds = video.durationSeconds,
            thumbnailUrl = video.thumbnailUrl,
            webpageUrl = video.webpageUrl,
            localPath = video.filePath,
        )

        fun fromCatalog(video: CatalogVideo) = PlayableItem(
            id = video.id,
            title = video.title,
            uploader = video.uploader,
            durationSeconds = video.durationSeconds,
            thumbnailUrl = video.thumbnailUrl,
            webpageUrl = video.webpageUrl,
            localPath = null,
        )
    }
}
