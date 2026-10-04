package com.decibel.ui

import android.app.Activity
import android.graphics.Color as AndroidColor
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.decibel.ui.theme.LocalDecibelDarkTheme

/**
 * Status-bar icon contrast + hide system navigation (and status bar in landscape).
 * Re-applies on resume — Samsung One UI often restores bars otherwise.
 */
@Composable
fun DecibelSystemBars(hideStatusBar: Boolean = false) {
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val darkTheme = LocalDecibelDarkTheme.current

    SideEffect {
        applyImmersiveNavigation(
            view = view,
            lightStatusBars = !darkTheme,
            hideStatusBar = hideStatusBar,
        )
    }

    DisposableEffect(lifecycleOwner, view, darkTheme, hideStatusBar) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                applyImmersiveNavigation(
                    view = view,
                    lightStatusBars = !darkTheme,
                    hideStatusBar = hideStatusBar,
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

fun applyImmersiveNavigation(
    view: View,
    lightStatusBars: Boolean? = null,
    hideStatusBar: Boolean = false,
) {
    val window = (view.context as? Activity)?.window ?: return

    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = AndroidColor.TRANSPARENT
    window.navigationBarColor = AndroidColor.TRANSPARENT
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isNavigationBarContrastEnforced = false
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    val hideTypes = if (hideStatusBar) {
        WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.statusBars()
    } else {
        WindowInsetsCompat.Type.navigationBars()
    }

    WindowCompat.getInsetsController(window, view).apply {
        if (lightStatusBars != null) {
            isAppearanceLightStatusBars = lightStatusBars
            isAppearanceLightNavigationBars = lightStatusBars
        }
        systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(hideTypes)
        if (!hideStatusBar) {
            show(WindowInsetsCompat.Type.statusBars())
        }
    }

    // Sticky immersive fallback — still required on some Samsung One UI versions.
    @Suppress("DEPRECATION")
    var flags = (
        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    if (hideStatusBar) {
        flags = flags or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_FULLSCREEN
    }
    @Suppress("DEPRECATION")
    view.systemUiVisibility = flags
}
