package com.mariesta.menzies.washui.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import com.mariesta.menzies.washui.FontDownloadManager

@Composable
actual fun rememberWashTypography(): WashTypography {
    val context = LocalContext.current
    val display = remember { FontFamily.Serif }
    val body = remember(context) {
        FontDownloadManager.loadBodyFamily(context) ?: FontFamily.Monospace
    }
    return WashTypography(displayFamily = display, bodyFamily = body)
}

@Composable
actual fun rememberSystemDarkMode(): Boolean {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}
