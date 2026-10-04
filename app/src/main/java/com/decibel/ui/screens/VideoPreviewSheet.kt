package com.decibel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.decibel.ui.PreviewUiState
import com.decibel.youtube.DownloadKind
import com.decibel.youtube.MediaDownloadOption
import com.decibel.youtube.formatBytes
import com.decibel.youtube.formatDuration

@Composable
fun VideoPreviewSheet(
    state: PreviewUiState,
    activeDownloads: Int,
    onClose: () -> Unit,
    onSelectOption: (MediaDownloadOption) -> Unit,
    onPlayOnline: () -> Unit,
    onDownload: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val video = state.video ?: return
    val options = state.lookup?.downloadOptions.orEmpty()
    val hq = options.filter { it.kind == DownloadKind.VIDEO_HQ }
    val mp3 = options.filter { it.kind == DownloadKind.AUDIO_MP3 }
    val quick = options.filter { it.kind == DownloadKind.PROGRESSIVE }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Preview",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = colors.onSurface,
                )
                TextButton(onClick = onClose) { Text("Close") }
            }

            if (!video.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }

            Text(
                text = video.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = colors.onSurface,
            )
            Text(
                text = buildString {
                    append(video.uploader)
                    if (video.durationSeconds > 0) {
                        append(" · ")
                        append(video.durationSeconds.formatDuration())
                    }
                },
                color = colors.onSurfaceVariant,
                fontSize = 13.sp,
            )

            if (state.loading) {
                Text(text = "Loading streams…", color = colors.onSurfaceVariant)
            }

            if (!state.error.isNullOrBlank()) {
                Text(text = state.error, color = colors.error, fontSize = 13.sp)
            }

            Button(
                onClick = onPlayOnline,
                enabled = !state.loading && state.error.isNullOrBlank() && !state.playingOnline,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.playingOnline) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = colors.onPrimary,
                    )
                }
                Text("Play online")
            }

            if (hq.isNotEmpty()) {
                SectionTitle("High quality video")
                Text(
                    text = "Best DASH streams merged to MP4.",
                    color = colors.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                hq.forEach { option ->
                    OptionRow(
                        option = option,
                        selected = state.selectedOption == option,
                        onClick = { onSelectOption(option) },
                    )
                }
            }

            if (mp3.isNotEmpty()) {
                SectionTitle("Music")
                mp3.forEach { option ->
                    OptionRow(
                        option = option,
                        selected = state.selectedOption == option,
                        onClick = { onSelectOption(option) },
                    )
                }
            }

            if (quick.isNotEmpty()) {
                SectionTitle("Quick video")
                Text(
                    text = "Progressive single-file download (often 360p).",
                    color = colors.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                quick.forEach { option ->
                    OptionRow(
                        option = option,
                        selected = state.selectedOption == option,
                        onClick = { onSelectOption(option) },
                    )
                }
            }

            if (options.isNotEmpty()) {
                FilledTonalButton(
                    onClick = onDownload,
                    enabled = state.selectedOption != null && !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val label = state.selectedOption?.label ?: "Download"
                    Text(
                        if (activeDownloads > 0) {
                            "Download $label (+$activeDownloads active)"
                        } else {
                            "Download $label"
                        },
                    )
                }
            } else if (!state.loading && state.lookup != null) {
                Text(
                    text = "Online play available. No download options for this video.",
                    color = colors.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun OptionRow(
    option: MediaDownloadOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    val subtitle = when (option.kind) {
        DownloadKind.VIDEO_HQ -> "Merged MP4 · excellent quality"
        DownloadKind.AUDIO_MP3 -> "Encoded from best audio"
        DownloadKind.PROGRESSIVE -> "Progressive file"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.primaryContainer else colors.surfaceVariant)
            .border(2.dp, if (selected) colors.primary else colors.outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.label,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) colors.primary else colors.onSurface,
            )
            Text(
                text = subtitle,
                color = colors.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
        Text(
            text = option.approxSizeBytes.formatBytes(),
            color = colors.onSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}
