package com.decibel.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.decibel.data.DownloadJob
import com.decibel.data.DownloadJobState

/**
 * Snackbars for download state transitions only (queued / started / saved / failed).
 * Percent-only progress updates must not re-trigger alerts.
 */
@Composable
fun DownloadJobToasts(
    jobs: List<DownloadJob>,
    snackbarHostState: SnackbarHostState,
) {
    var previous by remember { mutableStateOf<Map<String, DownloadJobState>>(emptyMap()) }

    // Key on jobId→state only so byte/percent churn does not restart this effect.
    val stateSignature = remember(jobs) {
        jobs.joinToString(separator = "|") { "${it.jobId}:${it.state.name}" }
    }

    LaunchedEffect(stateSignature) {
        val next = jobs.associate { it.jobId to it.state }
        val messages = mutableListOf<Pair<String, SnackbarDuration>>()

        for (job in jobs) {
            val old = previous[job.jobId]
            if (old == job.state) continue

            val title = job.title.toastTitle()
            val message = when {
                old == null && job.state == DownloadJobState.Queued ->
                    "Queued · $title"
                old == null && job.state == DownloadJobState.Running ->
                    "Downloading · $title"
                old != null &&
                    old != DownloadJobState.Running &&
                    job.state == DownloadJobState.Running ->
                    "Downloading · $title"
                job.state == DownloadJobState.Success ->
                    "Saved · $title"
                job.state == DownloadJobState.Failed ->
                    job.message?.takeIf { it.isNotBlank() }?.toastTitle()
                        ?: "Failed · $title"
                else -> null
            }
            if (message != null) {
                val duration = if (job.state == DownloadJobState.Failed) {
                    SnackbarDuration.Long
                } else {
                    SnackbarDuration.Short
                }
                messages += message to duration
            }
        }

        // Commit before suspending showSnackbar so a cancel/restart cannot re-fire.
        previous = next

        for ((message, duration) in messages) {
            snackbarHostState.showSnackbar(message = message, duration = duration)
        }
    }
}

private fun String.toastTitle(max: Int = 42): String {
    val trimmed = trim()
    if (trimmed.length <= max) return trimmed
    return trimmed.take(max - 1).trimEnd() + "…"
}
