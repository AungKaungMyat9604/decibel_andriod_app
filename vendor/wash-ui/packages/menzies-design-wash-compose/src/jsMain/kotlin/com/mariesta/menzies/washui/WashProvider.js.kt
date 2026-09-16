package com.mariesta.menzies.washui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mariesta.menzies.washui.theme.WashMode
import com.mariesta.menzies.washui.theme.WashPigment

private object JsWashPreferences : WashPreferences {
    private var pigment: WashPigment? = null
    private var mode: WashMode? = null
    override suspend fun readPigment(): WashPigment? = pigment
    override suspend fun readMode(): WashMode? = mode
    override suspend fun writePigment(pigment: WashPigment) { this.pigment = pigment }
    override suspend fun writeMode(mode: WashMode) { this.mode = mode }
}

@Composable
actual fun rememberWashPreferences(): WashPreferences = remember { JsWashPreferences }
