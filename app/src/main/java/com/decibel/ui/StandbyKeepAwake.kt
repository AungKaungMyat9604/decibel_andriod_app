package com.decibel.ui

import android.app.Activity
import android.content.Context
import android.os.PowerManager
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * Keeps the display awake while [active].
 *
 * [dimForOled]: for Standby — lower window brightness and skip SCREEN_BRIGHT
 * wake lock so AMOLED stays cooler. Immersive video should pass false.
 *
 * Cross-OEM approach:
 * 1. [WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON]
 * 2. [android.view.View.setKeepScreenOn]
 * 3. Optional screen WakeLock fallback (bright only when not dimming)
 */
@Composable
fun StandbyKeepAwake(
    active: Boolean,
    dimForOled: Boolean = false,
) {
    val view = LocalView.current
    val context = LocalContext.current

    DisposableEffect(active, dimForOled) {
        if (!active) {
            return@DisposableEffect onDispose { }
        }

        val activity = context as? Activity
        val window = activity?.window
        val attrs = window?.attributes
        val previousBrightness = attrs?.screenBrightness
            ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE

        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (dimForOled && attrs != null) {
            // ~20% — enough for waves on black OLED, much less heat/battery.
            window.attributes = attrs.apply {
                screenBrightness = 0.2f
            }
        }
        val previousKeep = view.keepScreenOn
        view.keepScreenOn = true

        // Bright wake lock fights AMOLED dimming; only use for non-dim cases (video).
        val wakeLock = if (dimForOled) {
            null
        } else {
            runCatching {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                @Suppress("DEPRECATION")
                pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                    "decibel:keepAwake",
                ).apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }.getOrNull()
        }

        onDispose {
            runCatching {
                if (wakeLock?.isHeld == true) wakeLock.release()
            }
            if (dimForOled && window != null) {
                window.attributes = window.attributes.apply {
                    screenBrightness = previousBrightness
                }
            }
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            view.keepScreenOn = previousKeep
        }
    }
}
