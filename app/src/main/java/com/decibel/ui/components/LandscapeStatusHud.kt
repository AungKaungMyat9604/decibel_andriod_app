package com.decibel.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Compact clock + battery (+ optional sleep remaining) for landscape chrome. */
@Composable
fun LandscapeStatusHud(
    modifier: Modifier = Modifier,
    sleepRemainingLabel: String? = null,
    /** Dark pill for overlay on video; plain theme colors when embedded in the app bar. */
    overlayScrim: Boolean = false,
) {
    val context = LocalContext.current
    var timeText by remember { mutableStateOf(currentTimeText()) }
    var batteryPercent by remember { mutableIntStateOf(readBatteryPercent(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            timeText = currentTimeText()
            val now = System.currentTimeMillis()
            val delayMs = 60_000L - (now % 60_000L)
            delay(delayMs.coerceAtLeast(1_000L))
        }
    }

    DisposableEffect(context) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                batteryPercent = percentFromBatteryIntent(intent) ?: batteryPercent
            }
        }
        val sticky = context.registerReceiver(receiver, filter)
        batteryPercent = percentFromBatteryIntent(sticky) ?: batteryPercent
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    val colors = MaterialTheme.colorScheme
    val textColor = if (overlayScrim) Color.White.copy(alpha = 0.92f) else colors.onSurfaceVariant
    val accent = colors.primary

    Row(
        modifier = modifier.then(
            if (overlayScrim) {
                Modifier
                    .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            } else {
                Modifier
            },
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = timeText,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp,
            maxLines = 1,
        )
        Text(
            text = "$batteryPercent%",
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp,
            maxLines = 1,
        )
        if (!sleepRemainingLabel.isNullOrBlank()) {
            Text(
                text = "·",
                color = textColor.copy(alpha = 0.55f),
                fontSize = 11.sp,
                maxLines = 1,
            )
            Text(
                text = "Sleep $sleepRemainingLabel",
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
                maxLines = 1,
            )
        }
    }
}

private fun currentTimeText(): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

private fun readBatteryPercent(context: Context): Int {
    val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    return percentFromBatteryIntent(sticky) ?: 0
}

private fun percentFromBatteryIntent(intent: Intent?): Int? {
    if (intent == null) return null
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return null
    return ((level * 100f) / scale).toInt().coerceIn(0, 100)
}
