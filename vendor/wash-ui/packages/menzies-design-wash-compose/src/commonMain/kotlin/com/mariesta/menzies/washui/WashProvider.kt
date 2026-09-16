package com.mariesta.menzies.washui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.mariesta.menzies.washui.theme.LocalWashColors
import com.mariesta.menzies.washui.theme.LocalWashTheme
import com.mariesta.menzies.washui.theme.WashMode
import com.mariesta.menzies.washui.theme.WashPigment
import com.mariesta.menzies.washui.theme.WashThemeState
import com.mariesta.menzies.washui.theme.WashTypography
import com.mariesta.menzies.washui.theme.rememberSystemDarkMode
import com.mariesta.menzies.washui.theme.rememberWashTypography
import com.mariesta.menzies.washui.theme.resolveWashColorScheme
import kotlinx.coroutines.launch

@Composable
fun WashProvider(
    defaultPigment: WashPigment? = null,
    defaultMode: WashMode? = null,
    followSystemMode: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val systemDark = rememberSystemDarkMode()
    val storage = rememberWashPreferences()

    var pigment by remember { mutableStateOf(defaultPigment ?: WashPigment.mineral) }
    var mode by remember { mutableStateOf(defaultMode ?: if (systemDark) WashMode.Dark else WashMode.Light) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(storage) {
        val storedPigment = storage.readPigment()
        val storedMode = storage.readMode()
        if (defaultPigment == null && storedPigment != null) {
            pigment = storedPigment
        }
        if (defaultMode == null) {
            mode = when {
                // Prefer system appearance when following system so dark mode works out of the box.
                followSystemMode -> if (systemDark) WashMode.Dark else WashMode.Light
                storedMode != null -> storedMode
                else -> WashMode.Light
            }
        }
        loaded = true
    }

    LaunchedEffect(systemDark, followSystemMode, defaultMode) {
        if (followSystemMode && defaultMode == null && loaded) {
            mode = if (systemDark) WashMode.Dark else WashMode.Light
        }
    }

    val colors = remember(pigment, mode) { resolveWashColorScheme(pigment, mode) }
    val typography = rememberWashTypography()

    val state = remember(pigment, mode, colors, typography, loaded) {
        WashThemeState(
            pigment = pigment,
            mode = mode,
            colors = colors,
            typography = typography,
            setPigment = { next ->
                pigment = next
                scope.launch { storage.writePigment(next) }
            },
            setMode = { next ->
                mode = next
                scope.launch { storage.writeMode(next) }
            },
            fontsReady = loaded,
        )
    }

    CompositionLocalProvider(
        LocalWashTheme provides state,
        LocalWashColors provides colors,
        content = content,
    )
}

@Composable
expect fun rememberWashPreferences(): WashPreferences

interface WashPreferences {
    suspend fun readPigment(): WashPigment?
    suspend fun readMode(): WashMode?
    suspend fun writePigment(pigment: WashPigment)
    suspend fun writeMode(mode: WashMode)
}
