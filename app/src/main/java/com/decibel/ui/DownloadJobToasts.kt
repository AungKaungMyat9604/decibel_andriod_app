package com.decibel.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.decibel.data.DownloadJob
import com.decibel.data.DownloadJobState
import com.mariesta.menzies.washui.primitives.WashToastState
import com.mariesta.menzies.washui.primitives.WashToastTone

/**
 * Pushes a Wash toast for each meaningful download notification transition
 * (queued, started, saved, failed). Skips percent-only progress updates.
 */
@Composable
fun DownloadJobToasts(
    jobs: List<DownloadJob>,
    toast: WashToastState,
) {
    var previous by remember { mutableStateOf<Map<String, DownloadJobState>>(emptyMap()) }

    LaunchedEffect(jobs) {
        val next = jobs.associate { it.jobId to it.state }
        for (job in jobs) {
            val old = previous[job.jobId]
            val title = job.title.toastTitle()
            when {
                old == null && job.state == DownloadJobState.Queued -> {
                    toast.push("Queued · $title", WashToastTone.Info)
                }
                old == null && job.state == DownloadJobState.Running -> {
                    toast.push("Downloading · $title", WashToastTone.Info)
                }
                old != null &&
                    old != DownloadJobState.Running &&
                    job.state == DownloadJobState.Running -> {
                    toast.push("Downloading · $title", WashToastTone.Info)
                }
                old != DownloadJobState.Success && job.state == DownloadJobState.Success -> {
                    toast.push("Saved · $title", WashToastTone.Success)
                }
                old != DownloadJobState.Failed && job.state == DownloadJobState.Failed -> {
                    val detail = job.message?.takeIf { it.isNotBlank() }?.toastTitle()
                    toast.push(
                        detail ?: "Failed · $title",
                        WashToastTone.Error,
                    )
                }
            }
        }
        previous = next
    }
}

private fun String.toastTitle(max: Int = 42): String {
    val trimmed = trim()
    if (trimmed.length <= max) return trimmed
    return trimmed.take(max - 1).trimEnd() + "…"
}
