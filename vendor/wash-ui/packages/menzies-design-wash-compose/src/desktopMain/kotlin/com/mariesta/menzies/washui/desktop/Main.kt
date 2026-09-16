package com.mariesta.menzies.washui.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.mariesta.menzies.washui.WashProvider
import com.mariesta.menzies.washui.components.WashThemeSwitcher
import com.mariesta.menzies.washui.primitives.WashPanel
import com.mariesta.menzies.washui.primitives.WashText
import com.mariesta.menzies.washui.theme.WashTheme

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Wash UI Compose") {
        WashProvider {
            Column(Modifier.padding(16.dp)) {
                WashText("Wash UI Desktop", color = WashTheme.colors.base_content)
                WashPanel {
                    WashThemeSwitcher()
                }
            }
        }
    }
}
