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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.decibel.data.DownloadJob
import com.decibel.data.DownloadJobState

@Composable
fun NotificationsScreen(
    jobs: List<DownloadJob>,
    onMarkSeen: () -> Unit,
    onDismiss: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    LaunchedEffect(jobs.map { it.jobId to it.state }) {
        onMarkSeen()
    }

    val active = jobs.filter { it.isActive }
    val finished = jobs.filter { it.isTerminal }.asReversed()
    val ordered = active + finished

    if (ordered.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.padding(4.dp),
                )
                Text(
                    text = "No notifications",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = colors.onSurface,
                )
                Text(
                    text = "Download progress and results will show up here.",
                    color = colors.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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

@Composable
private fun NotificationJobCard(
    job: DownloadJob,
    onDismiss: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = statusLabel(job),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = when (job.state) {
                            DownloadJobState.Success -> colors.primary
                            DownloadJobState.Failed -> colors.error
                            DownloadJobState.Running -> colors.primary
                            DownloadJobState.Queued -> colors.onSurfaceVariant
                        },
                    )
                    Text(
                        text = job.title,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onSurface,
                    )
                    if (!job.message.isNullOrBlank() && job.state == DownloadJobState.Failed) {
                        Text(
                            text = job.message,
                            color = colors.error,
                            fontSize = 12.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (onDismiss != null) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = colors.onSurfaceVariant,
                        )
                    }
                }
            }

            if (job.state == DownloadJobState.Running || job.state == DownloadJobState.Queued) {
                ProgressTrack(percent = job.percent)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (job.state == DownloadJobState.Queued) "Waiting…" else "Downloading",
                        color = colors.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                    Text(
                        text = "${job.percent.coerceIn(0, 100)}%",
                        color = colors.onSurfaceVariant,
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
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(shape)
            .background(colors.surfaceVariant),
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
