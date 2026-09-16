package com.decibel.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

enum class MediaType {
    VIDEO,
    AUDIO,
}

data class SavedVideo(
    val id: String,
    val title: String,
    val uploader: String,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val webpageUrl: String,
    val quality: String,
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val downloadedAtEpochMs: Long,
    val mediaType: MediaType = MediaType.VIDEO,
    val format: String = "mp4",
    val isFavourite: Boolean = false,
)

enum class DownloadJobState {
    Queued,
    Running,
    Success,
    Failed,
}

data class DownloadJob(
    val jobId: String,
    val entryId: String,
    val title: String,
    val percent: Int = 0,
    val downloadedBytes: Long = 0,
    val totalBytes: Long? = null,
    val state: DownloadJobState = DownloadJobState.Queued,
    val message: String? = null,
    val video: SavedVideo? = null,
) {
    val isActive: Boolean
        get() = state == DownloadJobState.Queued || state == DownloadJobState.Running
    val isTerminal: Boolean
        get() = state == DownloadJobState.Success || state == DownloadJobState.Failed
}

/** Shared hub so download workers can push multi-job progress to the UI. */
object DownloadProgressHub {
    private val _jobs = MutableStateFlow<List<DownloadJob>>(emptyList())
    val jobs: StateFlow<List<DownloadJob>> = _jobs.asStateFlow()

    /** Terminal jobs the user has already opened in Notifications. */
    private val _seenIds = MutableStateFlow<Set<String>>(emptySet())
    val seenIds: StateFlow<Set<String>> = _seenIds.asStateFlow()

    val hasActive: Boolean
        get() = _jobs.value.any { it.isActive }

    val activeCount: Int
        get() = _jobs.value.count { it.isActive }

    /** Active downloads + unread completed/failed jobs. */
    fun badgeCount(jobs: List<DownloadJob> = _jobs.value, seen: Set<String> = _seenIds.value): Int {
        val active = jobs.count { it.isActive }
        val unreadTerminal = jobs.count { it.isTerminal && it.jobId !in seen }
        return active + unreadTerminal
    }

    fun newJobId(): String = UUID.randomUUID().toString()

    fun enqueue(job: DownloadJob) {
        _jobs.update { list -> list + job }
    }

    fun update(jobId: String, transform: (DownloadJob) -> DownloadJob) {
        _jobs.update { list ->
            list.map { if (it.jobId == jobId) transform(it) else it }
        }
    }

    fun markAllSeen() {
        _seenIds.value = _jobs.value.map { it.jobId }.toSet()
    }

    fun dismissTerminal() {
        val terminalIds = _jobs.value.filter { it.isTerminal }.map { it.jobId }.toSet()
        _jobs.update { list -> list.filterNot { it.isTerminal } }
        _seenIds.update { it - terminalIds }
    }

    fun dismiss(jobId: String) {
        _jobs.update { list -> list.filterNot { it.jobId == jobId } }
        _seenIds.update { it - jobId }
    }
}
