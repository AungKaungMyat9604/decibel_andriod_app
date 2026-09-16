package com.decibel.ui

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.mariesta.menzies.washui.theme.WashMode
import com.mariesta.menzies.washui.theme.WashTheme

/** Keeps status / navigation bar contrast in sync with Wash light/dark mode. */
@Composable
fun DecibelSystemBars() {
    val view = LocalView.current
    val colors = WashTheme.colors
    val dark = WashTheme.mode == WashMode.Dark
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = colors.base_100.copy(alpha = 0.96f).toArgb()
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}
