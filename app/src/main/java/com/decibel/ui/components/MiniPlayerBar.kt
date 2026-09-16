package com.decibel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mariesta.menzies.washui.icons.LucideIcons
import com.mariesta.menzies.washui.icons.WashIcon
import com.mariesta.menzies.washui.icons.lucide.Circle
import com.mariesta.menzies.washui.icons.lucide.Music4
import com.mariesta.menzies.washui.icons.lucide.Square
import com.mariesta.menzies.washui.primitives.WashIconButton
import com.mariesta.menzies.washui.primitives.WashText
import com.mariesta.menzies.washui.theme.WashTheme
import com.decibel.player.PlaybackState
import com.decibel.ui.coilImageModel

@Composable
fun MiniPlayerBar(
    playback: PlaybackState,
    onOpen: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = playback.current ?: return
    val colors = WashTheme.colors
    val shape = RoundedCornerShape(topStart = colors.radiusField, topEnd = colors.radiusField)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.base_100.copy(alpha = 0.96f))
            .clickable(onClick = onOpen),
    ) {
        val progress = if (playback.durationMs > 0) {
            (playback.positionMs.toFloat() / playback.durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(colors.base_300),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(2.dp)
                    .background(colors.primary),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.base_300),
                contentAlignment = Alignment.Center,
            ) {
                val thumbModel = remember(current.thumbnailUrl) { coilImageModel(current.thumbnailUrl) }
                if (thumbModel != null) {
                    AsyncImage(
                        model = thumbModel,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                } else {
                    WashIcon(
                        imageVector = LucideIcons.Music4,
                        contentDescription = null,
                        tint = colors.ink_muted,
                        size = 20.dp,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                WashText(
                    text = current.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
                WashText(
                    text = current.uploader + if (current.isLocal) " · Offline" else " · Online",
                    color = colors.ink_muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
            WashIconButton(
                onClick = onTogglePlay,
                imageVector = if (playback.isPlaying) LucideIcons.Square else LucideIcons.Circle,
                contentDescription = if (playback.isPlaying) "Pause" else "Play",
                tint = colors.primary,
            )
        }
    }
}
