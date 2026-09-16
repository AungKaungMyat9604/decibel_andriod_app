package com.mariesta.menzies.washui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class WashColorScheme(
    val pigment: WashPigment,
    val mode: WashMode,
    val base_100: Color,
    val base_200: Color,
    val base_300: Color,
    val base_content: Color,
    val primary: Color,
    val primary_content: Color,
    val secondary: Color,
    val secondary_content: Color,
    val accent: Color,
    val accent_content: Color,
    val neutral: Color,
    val neutral_content: Color,
    val info: Color,
    val info_content: Color,
    val success: Color,
    val success_content: Color,
    val warning: Color,
    val warning_content: Color,
    val error: Color,
    val error_content: Color,
    val radiusBox: Dp = 16.dp,
    val radiusField: Dp = 16.dp,
    val wash_a: Color = Color(0xFFD9EEF5),
    val wash_b: Color = Color(0xFFF2E1C6),
    val wash_c: Color = Color(0xFFE8C9C3),
    val paper_fiber: Color = Color(0xFFE8E1D4),
    val pigment_grain: Color = Color(0xFF5A6D73),
    val ink_muted: Color = Color(0xFF4A4640),
    val ink_border: Color = Color(0xFFC9C1B2),
    val wash_panel_bg: Color = Color(0xFFFFFFFF),
) {
    val base100 get() = base_100
    val base200 get() = base_200
    val base300 get() = base_300
    val baseContent get() = base_content
    val primaryContent get() = primary_content
}

data class WashTypography(
    val displayFamily: androidx.compose.ui.text.font.FontFamily,
    val bodyFamily: androidx.compose.ui.text.font.FontFamily,
)

data class WashThemeState(
    val pigment: WashPigment,
    val mode: WashMode,
    val colors: WashColorScheme,
    val typography: WashTypography,
    val setPigment: (WashPigment) -> Unit,
    val setMode: (WashMode) -> Unit,
    val fontsReady: Boolean,
)

val LocalWashTheme = staticCompositionLocalOf<WashThemeState> {
    error("WashTheme not provided")
}

val LocalWashColors = staticCompositionLocalOf<WashColorScheme> {
    error("WashTheme not provided")
}
