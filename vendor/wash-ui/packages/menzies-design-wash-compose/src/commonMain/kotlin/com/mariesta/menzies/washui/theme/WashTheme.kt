package com.mariesta.menzies.washui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode

object WashTheme {
    val colors: WashColorScheme
        @Composable get() = LocalWashColors.current

    val pigment: WashPigment
        @Composable get() = LocalWashTheme.current.pigment

    val mode: WashMode
        @Composable get() = LocalWashTheme.current.mode

    val typography: WashTypography
        @Composable get() = LocalWashTheme.current.typography
}

@Composable
fun WashTheme(
    pigment: WashPigment = WashPigment.mineral,
    mode: WashMode = WashMode.Light,
    typography: WashTypography = rememberWashTypography(),
    content: @Composable () -> Unit,
) {
    val colors = remember(pigment, mode) { resolveWashColorScheme(pigment, mode) }
    val state = remember(pigment, mode, colors, typography) {
        WashThemeState(
            pigment = pigment,
            mode = mode,
            colors = colors,
            typography = typography,
            setPigment = {},
            setMode = {},
            fontsReady = true,
        )
    }
    CompositionLocalProvider(
        LocalWashTheme provides state,
        LocalWashColors provides colors,
        content = content,
    )
}

@Composable
expect fun rememberWashTypography(): WashTypography

@Composable
expect fun rememberSystemDarkMode(): Boolean
