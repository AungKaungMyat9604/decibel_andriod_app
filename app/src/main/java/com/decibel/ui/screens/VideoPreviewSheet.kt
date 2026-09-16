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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mariesta.menzies.washui.primitives.WashButton
import com.mariesta.menzies.washui.primitives.WashButtonVariant
import com.mariesta.menzies.washui.primitives.WashPanel
import com.mariesta.menzies.washui.primitives.WashText
import com.mariesta.menzies.washui.theme.WashTheme
import com.decibel.ui.PreviewUiState
import com.decibel.youtube.VideoFormatOption
import com.decibel.youtube.formatBytes
import com.decibel.youtube.formatDuration

@Composable
fun VideoPreviewSheet(
    state: PreviewUiState,
    activeDownloads: Int,
    onClose: () -> Unit,
    onSelectFormat: (VideoFormatOption) -> Unit,
    onPlayOnline: () -> Unit,
    onDownload: () -> Unit,
) {
    val colors = WashTheme.colors
    val video = state.video ?: return

    WashPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WashText(text = "Preview", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                WashButton(onClick = onClose, text = "Close", variant = WashButtonVariant.Ghost)
            }

            if (!video.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(colors.radiusBox)),
                )
            }

            WashText(text = video.title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            WashText(
                text = buildString {
                    append(video.uploader)
                    if (video.durationSeconds > 0) {
                        append(" · ")
                        append(video.durationSeconds.formatDuration())
                    }
                },
                color = colors.ink_muted,
                fontSize = 13.sp,
            )

            if (state.loading) {
                WashText(text = "Loading streams…", color = colors.ink_muted)
            }

            if (!state.error.isNullOrBlank()) {
                WashText(text = state.error, color = colors.error, fontSize = 13.sp)
            }

            WashButton(
                onClick = onPlayOnline,
                text = "Play online",
                variant = WashButtonVariant.Primary,
                loading = state.playingOnline,
                enabled = !state.loading && state.error.isNullOrBlank(),
                modifier = Modifier.fillMaxWidth(),
            )

            val formats = state.lookup?.videoFormats.orEmpty()
            if (formats.isNotEmpty()) {
                WashText(
                    text = "Quality · ${formats.size} progressive option(s)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                WashText(
                    text = "Pick a resolution before downloading. Highest is selected by default.",
                    color = colors.ink_muted,
                    fontSize = 12.sp,
                )
                formats.forEach { format ->
                    FormatRow(
                        format = format,
                        selected = state.selectedFormat == format,
                        onClick = { onSelectFormat(format) },
                    )
                }
                WashButton(
                    onClick = onDownload,
                    text = if (activeDownloads > 0) {
                        "Download for offline (+$activeDownloads active)"
                    } else {
                        "Download for offline"
                    },
                    variant = WashButtonVariant.Accent,
                    enabled = state.selectedFormat != null && !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (!state.loading && state.lookup != null) {
                WashText(
                    text = "Online play available. No progressive file download for this video.",
                    color = colors.ink_muted,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun FormatRow(
    format: VideoFormatOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = WashTheme.colors
    val shape = RoundedCornerShape(colors.radiusField)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.wash_a.copy(alpha = 0.65f) else colors.base_200)
            .border(2.dp, if (selected) colors.primary else colors.ink_border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            WashText(
                text = format.resolution,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) colors.primary else colors.base_content,
            )
            WashText(
                text = "${format.format.uppercase()} progressive",
                color = colors.ink_muted,
                fontSize = 12.sp,
            )
        }
        WashText(
            text = format.approxSizeBytes.formatBytes(),
            color = colors.ink_muted,
            fontSize = 13.sp,
        )
    }
}
