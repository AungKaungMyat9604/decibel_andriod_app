package com.decibel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mariesta.menzies.washui.primitives.WashButton
import com.mariesta.menzies.washui.primitives.WashButtonVariant
import com.mariesta.menzies.washui.primitives.WashText
import com.mariesta.menzies.washui.theme.WashTheme
import com.decibel.data.DownloadJob
import com.decibel.data.DownloadJobState

@Composable
fun DownloadStatusBar(
    jobs: List<DownloadJob>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (jobs.isEmpty()) return
    val colors = WashTheme.colors
    val shape = RoundedCornerShape(topStart = colors.radiusField, topEnd = colors.radiusField)
    val active = jobs.filter { it.isActive }
    val terminal = jobs.filter { it.isTerminal }
    val shown = (active + terminal.takeLast(2)).distinctBy { it.jobId }.take(4)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.base_100.copy(alpha = 0.97f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WashText(
                text = when {
                    active.isNotEmpty() -> "Downloads · ${active.size} active"
                    else -> "Downloads"
                },
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            if (terminal.isNotEmpty() && active.isEmpty()) {
                WashButton(onClick = onDismiss, text = "Clear", variant = WashButtonVariant.Ghost)
            }
        }
        shown.forEach { job ->
            JobRow(job = job)
        }
        val hidden = jobs.size - shown.size
        if (hidden > 0) {
            WashText(text = "+$hidden more", color = colors.ink_muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun JobRow(job: DownloadJob) {
    val colors = WashTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        WashText(
            text = when (job.state) {
                DownloadJobState.Queued -> "Queued · ${job.title}"
                DownloadJobState.Running -> "Downloading ${job.title}"
                DownloadJobState.Success -> "Saved · ${job.title}"
                DownloadJobState.Failed -> job.message ?: "Failed · ${job.title}"
            },
            fontSize = 12.sp,
            maxLines = 1,
            color = when (job.state) {
                DownloadJobState.Success -> colors.success
                DownloadJobState.Failed -> colors.error
                else -> colors.base_content
            },
        )
        if (job.state == DownloadJobState.Running || job.state == DownloadJobState.Queued) {
            ProgressTrack(percent = job.percent)
            if (job.state == DownloadJobState.Running) {
                WashText(text = "${job.percent}%", color = colors.ink_muted, fontSize = 11.sp)
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
