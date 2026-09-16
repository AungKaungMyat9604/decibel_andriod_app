package com.mariesta.menzies.washui

import androidx.compose.runtime.Composable
import com.mariesta.menzies.washui.theme.LocalWashTheme
import com.mariesta.menzies.washui.theme.WashMode
import com.mariesta.menzies.washui.theme.WashPigment

@Composable
fun useWash(): WashContext {
    val state = LocalWashTheme.current
    return WashContext(
        pigment = state.pigment,
        mode = state.mode,
        setPigment = state.setPigment,
        setMode = state.setMode,
        fontsReady = state.fontsReady,
    )
}

data class WashContext(
    val pigment: WashPigment,
    val mode: WashMode,
    val setPigment: (WashPigment) -> Unit,
    val setMode: (WashMode) -> Unit,
    val fontsReady: Boolean,
)
