package com.decibel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.decibel.data.DownloadJob
import com.decibel.data.DownloadJobState
import com.mariesta.menzies.washui.icons.LucideIcons
import com.mariesta.menzies.washui.icons.WashIcon
import com.mariesta.menzies.washui.icons.lucide.Bell
import com.mariesta.menzies.washui.icons.lucide.X
import com.mariesta.menzies.washui.primitives.WashButton
import com.mariesta.menzies.washui.primitives.WashButtonVariant
import com.mariesta.menzies.washui.primitives.WashIconButton
import com.mariesta.menzies.washui.primitives.WashPanel
import com.mariesta.menzies.washui.primitives.WashText
import com.mariesta.menzies.washui.theme.WashTheme

@Composable
fun NotificationsScreen(
    jobs: List<DownloadJob>,
    onMarkSeen: () -> Unit,
    onClearFinished: () -> Unit,
    onDismiss: (String) -> Unit,
) {
    val colors = WashTheme.colors
    LaunchedEffect(jobs.map { it.jobId to it.state }) {
        onMarkSeen()
    }

    val active = jobs.filter { it.isActive }
    val finished = jobs.filter { it.isTerminal }.asReversed()
    val ordered = active + finished

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                WashText(
                    text = "Downloads & alerts",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                WashText(
                    text = when {
                        active.isNotEmpty() -> "${active.size} in progress"
                        finished.isNotEmpty() -> "${finished.size} recent"
                        else -> "Nothing yet"
                    },
                    color = colors.ink_muted,
                    fontSize = 12.sp,
                )
            }
            if (finished.isNotEmpty()) {
                WashButton(
                    onClick = onClearFinished,
                    text = "Clear done",
                    variant = WashButtonVariant.Ghost,
                )
            }
        }

        if (ordered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    WashIcon(
                        imageVector = LucideIcons.Bell,
                        contentDescription = null,
                        tint = colors.ink_muted,
                        size = 36.dp,
                    )
                    WashText(
                        text = "No notifications",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                    )
                    WashText(
                        text = "Download progress and results will show up here.",
                        color = colors.ink_muted,
                        fontSize = 13.sp,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(ordered, key = { it.jobId }) { job ->
                    NotificationJobCard(
                        job = job,
                        onDismiss = if (job.isTerminal) {
                            { onDismiss(job.jobId) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationJobCard(
    job: DownloadJob,
    onDismiss: (() -> Unit)?,
) {
    val colors = WashTheme.colors
    WashPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    WashText(
                        text = statusLabel(job),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = when (job.state) {
                            DownloadJobState.Success -> colors.success
                            DownloadJobState.Failed -> colors.error
                            DownloadJobState.Running -> colors.primary
                            DownloadJobState.Queued -> colors.ink_muted
                        },
                    )
                    WashText(
                        text = job.title,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 2,
                    )
                    if (!job.message.isNullOrBlank() && job.state == DownloadJobState.Failed) {
                        WashText(
                            text = job.message,
                            color = colors.error,
                            fontSize = 12.sp,
                            maxLines = 3,
                        )
                    }
                }
                if (onDismiss != null) {
                    WashIconButton(
                        onClick = onDismiss,
                        imageVector = LucideIcons.X,
                        contentDescription = "Dismiss",
                        tint = colors.ink_muted,
                        iconSize = 16.dp,
                        buttonSize = 32.dp,
                    )
                }
            }

            if (job.state == DownloadJobState.Running || job.state == DownloadJobState.Queued) {
                ProgressTrack(percent = job.percent)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    WashText(
                        text = if (job.state == DownloadJobState.Queued) "Waiting…" else "Downloading",
                        color = colors.ink_muted,
                        fontSize = 11.sp,
                    )
                    WashText(
                        text = "${job.percent.coerceIn(0, 100)}%",
                        color = colors.ink_muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressTrack(percent: Int) {
    val colors = WashTheme.colors
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(shape)
            .background(colors.base_300),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth((percent.coerceIn(0, 100) / 100f))
                .height(8.dp)
                .background(colors.primary),
        )
    }
}

private fun statusLabel(job: DownloadJob): String = when (job.state) {
    DownloadJobState.Queued -> "Queued"
    DownloadJobState.Running -> "Downloading"
    DownloadJobState.Success -> "Download complete"
    DownloadJobState.Failed -> "Download failed"
}
