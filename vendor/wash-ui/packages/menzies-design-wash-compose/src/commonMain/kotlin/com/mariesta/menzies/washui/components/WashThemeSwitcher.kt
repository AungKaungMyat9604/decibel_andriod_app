package com.mariesta.menzies.washui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mariesta.menzies.washui.primitives.WashChip
import com.mariesta.menzies.washui.primitives.WashText
import com.mariesta.menzies.washui.theme.WashMode
import com.mariesta.menzies.washui.theme.WashTheme
import com.mariesta.menzies.washui.theme.washPigmentCatalog
import com.mariesta.menzies.washui.useWash

@Composable
fun WashThemeSwitcher(modifier: Modifier = Modifier) {
    val wash = useWash()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WashText("Pigment", color = WashTheme.colors.base_content)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            washPigmentCatalog.take(6).forEach { meta ->
                WashChip(
                    selected = wash.pigment == meta.id,
                    onClick = { wash.setPigment(meta.id) },
                    label = meta.label,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WashChip(
                selected = wash.mode == WashMode.Light,
                onClick = { wash.setMode(WashMode.Light) },
                label = "Light",
            )
            WashChip(
                selected = wash.mode == WashMode.Dark,
                onClick = { wash.setMode(WashMode.Dark) },
                label = "Dark",
            )
        }
    }
}
