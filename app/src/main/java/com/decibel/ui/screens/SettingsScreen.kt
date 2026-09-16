package com.decibel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mariesta.menzies.washui.icons.LucideIcons
import com.mariesta.menzies.washui.icons.WashIcon
import com.mariesta.menzies.washui.icons.lucide.FolderOpen
import com.mariesta.menzies.washui.primitives.WashButton
import com.mariesta.menzies.washui.primitives.WashButtonVariant
import com.mariesta.menzies.washui.primitives.WashPanel
import com.mariesta.menzies.washui.primitives.WashText
import com.mariesta.menzies.washui.theme.WashTheme

@Composable
fun SettingsScreen(
    folderLabel: String,
    usingAppFolder: Boolean,
    onChooseFolder: () -> Unit,
    onUseAppFolder: () -> Unit,
    onRefreshFolder: () -> Unit,
) {
    val colors = WashTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WashText(
            text = "Storage",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        WashPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    WashIcon(
                        imageVector = LucideIcons.FolderOpen,
                        contentDescription = null,
                        tint = colors.primary,
                        size = 18.dp,
                    )
                    WashText(
                        text = "Library folder",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
                WashText(
                    text = folderLabel,
                    color = colors.ink_muted,
                    fontSize = 12.sp,
                    maxLines = 3,
                )
                WashText(
                    text = "Downloads are saved here. Scan imports media files already in the folder.",
                    color = colors.ink_muted,
                    fontSize = 12.sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WashButton(
                        onClick = onChooseFolder,
                        text = "Choose folder",
                        variant = WashButtonVariant.Primary,
                        modifier = Modifier.weight(1f),
                    )
                    WashButton(
                        onClick = onRefreshFolder,
                        text = "Scan",
                        variant = WashButtonVariant.Outline,
                    )
                }
                if (!usingAppFolder) {
                    WashButton(
                        onClick = onUseAppFolder,
                        text = "Use app folder",
                        variant = WashButtonVariant.Ghost,
                    )
                }
            }
        }
    }
}
