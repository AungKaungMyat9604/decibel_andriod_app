package com.mariesta.menzies.washui.primitives

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mariesta.menzies.washui.theme.WashTheme
import kotlinx.coroutines.delay

enum class WashToastTone {
    Success,
    Error,
    Warning,
    Info,
}

data class WashToastItem(
    val id: String,
    val message: String,
    val tone: WashToastTone = WashToastTone.Success,
)

class WashToastState internal constructor(
    private val addItem: (WashToastItem) -> Unit,
) {
    fun push(message: String, tone: WashToastTone = WashToastTone.Success) {
        val id = "toast-${kotlin.random.Random.nextLong()}"
        addItem(WashToastItem(id = id, message = message, tone = tone))
    }
}

val LocalWashToast = staticCompositionLocalOf<WashToastState> {
    error("WashToastProvider not found. Wrap content in WashToastProvider.")
}

@Composable
fun rememberWashToastState(): WashToastState {
    return LocalWashToast.current
}

@Composable
fun WashToastProvider(
    durationMs: Long = 3200,
    alignment: Alignment = Alignment.BottomEnd,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val items = remember { mutableStateListOf<WashToastItem>() }
    val state = remember {
        WashToastState { item ->
            items.add(item)
        }
    }

    CompositionLocalProvider(LocalWashToast provides state) {
        Box(modifier = modifier.fillMaxSize()) {
            content()
            WashToastHost(
                items = items,
                durationMs = durationMs,
                alignment = alignment,
                onDismiss = { id -> items.removeAll { it.id == id } },
            )
        }
    }
}

@Composable
fun WashToastHost(
    items: List<WashToastItem>,
    durationMs: Long = 3200,
    alignment: Alignment = Alignment.BottomEnd,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WashTheme.colors
    val fromTop = alignment == Alignment.TopStart ||
        alignment == Alignment.TopCenter ||
        alignment == Alignment.TopEnd

    items.forEach { item ->
        LaunchedEffect(item.id) {
            delay(durationMs)
            onDismiss(item.id)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (fromTop) Modifier.statusBarsPadding() else Modifier)
            .padding(16.dp)
            .zIndex(100f),
        contentAlignment = alignment,
    ) {
        Column {
            items.forEach { item ->
                val (bg, content) = toastColors(item.tone, colors)
                Box(
                    modifier = Modifier
                        .padding(
                            bottom = if (fromTop) 8.dp else 0.dp,
                            top = if (fromTop) 0.dp else 8.dp,
                        )
                        .background(bg, RoundedCornerShape(colors.radiusField))
                        .border(1.dp, colors.ink_border, RoundedCornerShape(colors.radiusField))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    WashText(text = item.message, color = content)
                }
            }
        }
    }
}

private fun toastColors(
    tone: WashToastTone,
    colors: com.mariesta.menzies.washui.theme.WashColorScheme,
): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> =
    when (tone) {
        WashToastTone.Success -> colors.success to colors.success_content
        WashToastTone.Error -> colors.error to colors.error_content
        WashToastTone.Warning -> colors.warning to colors.warning_content
        WashToastTone.Info -> colors.info to colors.info_content
    }
