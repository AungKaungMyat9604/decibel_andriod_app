package com.mariesta.menzies.washui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

@Composable
actual fun rememberWashTypography(): WashTypography =
    WashTypography(displayFamily = FontFamily.Serif, bodyFamily = FontFamily.Monospace)

@Composable
actual fun rememberSystemDarkMode(): Boolean = false
