package com.decibel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.decibel.data.ThemeMode
import com.decibel.data.ThemePreset

val LocalDecibelDarkTheme = staticCompositionLocalOf { false }

/** Compact text style for all Decibel input fields. */
val DecibelFieldTextStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.1.sp,
)

private data class PresetPalette(
    val light: ColorScheme,
    val dark: ColorScheme,
)

private fun palette(
    lightPrimary: Long,
    lightPrimaryContainer: Long,
    lightOnPrimaryContainer: Long,
    lightSecondary: Long,
    lightSecondaryContainer: Long,
    lightOnSecondaryContainer: Long,
    lightBg: Long,
    lightOnBg: Long,
    lightSurfaceVariant: Long,
    lightOnSurfaceVariant: Long,
    lightOutline: Long,
    darkPrimary: Long,
    darkOnPrimary: Long,
    darkPrimaryContainer: Long,
    darkOnPrimaryContainer: Long,
    darkSecondary: Long,
    darkOnSecondary: Long,
    darkSecondaryContainer: Long,
    darkOnSecondaryContainer: Long,
    darkBg: Long,
    darkOnBg: Long,
    darkSurfaceVariant: Long,
    darkOnSurfaceVariant: Long,
    darkOutline: Long,
): PresetPalette = PresetPalette(
    light = lightColorScheme(
        primary = Color(lightPrimary),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(lightPrimaryContainer),
        onPrimaryContainer = Color(lightOnPrimaryContainer),
        secondary = Color(lightSecondary),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(lightSecondaryContainer),
        onSecondaryContainer = Color(lightOnSecondaryContainer),
        background = Color(lightBg),
        onBackground = Color(lightOnBg),
        surface = Color(lightBg),
        onSurface = Color(lightOnBg),
        surfaceVariant = Color(lightSurfaceVariant),
        onSurfaceVariant = Color(lightOnSurfaceVariant),
        outline = Color(lightOutline),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
    ),
    dark = darkColorScheme(
        primary = Color(darkPrimary),
        onPrimary = Color(darkOnPrimary),
        primaryContainer = Color(darkPrimaryContainer),
        onPrimaryContainer = Color(darkOnPrimaryContainer),
        secondary = Color(darkSecondary),
        onSecondary = Color(darkOnSecondary),
        secondaryContainer = Color(darkSecondaryContainer),
        onSecondaryContainer = Color(darkOnSecondaryContainer),
        background = Color(darkBg),
        onBackground = Color(darkOnBg),
        surface = Color(darkBg),
        onSurface = Color(darkOnBg),
        surfaceVariant = Color(darkSurfaceVariant),
        onSurfaceVariant = Color(darkOnSurfaceVariant),
        outline = Color(darkOutline),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
    ),
)

private fun paletteFor(preset: ThemePreset): PresetPalette = when (preset) {
    ThemePreset.Teal -> palette(
        lightPrimary = 0xFF1B6B5A, lightPrimaryContainer = 0xFFD4EDE6, lightOnPrimaryContainer = 0xFF002119,
        lightSecondary = 0xFF4A635C, lightSecondaryContainer = 0xFFCCE8E0, lightOnSecondaryContainer = 0xFF05201A,
        lightBg = 0xFFF7F9F8, lightOnBg = 0xFF191C1B, lightSurfaceVariant = 0xFFDAE5E0,
        lightOnSurfaceVariant = 0xFF3F4945, lightOutline = 0xFF6F7975,
        darkPrimary = 0xFF88D4C0, darkOnPrimary = 0xFF00382C, darkPrimaryContainer = 0xFF005140,
        darkOnPrimaryContainer = 0xFFA4F1DC, darkSecondary = 0xFFB1CCC4, darkOnSecondary = 0xFF1C3530,
        darkSecondaryContainer = 0xFF334B46, darkOnSecondaryContainer = 0xFFCCE8E0,
        darkBg = 0xFF191C1B, darkOnBg = 0xFFE1E3E1, darkSurfaceVariant = 0xFF3F4945,
        darkOnSurfaceVariant = 0xFFBFC9C4, darkOutline = 0xFF89938E,
    )
    ThemePreset.Ocean -> palette(
        lightPrimary = 0xFF006493, lightPrimaryContainer = 0xFFCAE6FF, lightOnPrimaryContainer = 0xFF001E30,
        lightSecondary = 0xFF50606E, lightSecondaryContainer = 0xFFD3E5F5, lightOnSecondaryContainer = 0xFF0C1D29,
        lightBg = 0xFFF7F9FC, lightOnBg = 0xFF181C20, lightSurfaceVariant = 0xFFDDE3EA,
        lightOnSurfaceVariant = 0xFF41484D, lightOutline = 0xFF71787E,
        darkPrimary = 0xFF8CCDFF, darkOnPrimary = 0xFF00344F, darkPrimaryContainer = 0xFF004B70,
        darkOnPrimaryContainer = 0xFFCAE6FF, darkSecondary = 0xFFB7C9D8, darkOnSecondary = 0xFF22323F,
        darkSecondaryContainer = 0xFF384956, darkOnSecondaryContainer = 0xFFD3E5F5,
        darkBg = 0xFF101417, darkOnBg = 0xFFE0E3E8, darkSurfaceVariant = 0xFF41484D,
        darkOnSurfaceVariant = 0xFFC1C7CE, darkOutline = 0xFF8B9298,
    )
    ThemePreset.Ember -> palette(
        lightPrimary = 0xFF9C4328, lightPrimaryContainer = 0xFFFFDBD0, lightOnPrimaryContainer = 0xFF3A0B00,
        lightSecondary = 0xFF77574C, lightSecondaryContainer = 0xFFFFDBD0, lightOnSecondaryContainer = 0xFF2C160D,
        lightBg = 0xFFFFF8F6, lightOnBg = 0xFF221A17, lightSurfaceVariant = 0xFFF5DED6,
        lightOnSurfaceVariant = 0xFF53433E, lightOutline = 0xFF85736D,
        darkPrimary = 0xFFFFB59D, darkOnPrimary = 0xFF5E1700, darkPrimaryContainer = 0xFF7D2C13,
        darkOnPrimaryContainer = 0xFFFFDBD0, darkSecondary = 0xFFE7BDB0, darkOnSecondary = 0xFF442A21,
        darkSecondaryContainer = 0xFF5D4035, darkOnSecondaryContainer = 0xFFFFDBD0,
        darkBg = 0xFF1A110F, darkOnBg = 0xFFF1DFD9, darkSurfaceVariant = 0xFF53433E,
        darkOnSurfaceVariant = 0xFFD8C2BB, darkOutline = 0xFFA08D86,
    )
    ThemePreset.Slate -> palette(
        lightPrimary = 0xFF4A5D73, lightPrimaryContainer = 0xFFD2E4FF, lightOnPrimaryContainer = 0xFF031C31,
        lightSecondary = 0xFF545F70, lightSecondaryContainer = 0xFFD8E3F8, lightOnSecondaryContainer = 0xFF111C2B,
        lightBg = 0xFFF8F9FC, lightOnBg = 0xFF191C20, lightSurfaceVariant = 0xFFDFE2EB,
        lightOnSurfaceVariant = 0xFF43474E, lightOutline = 0xFF73777F,
        darkPrimary = 0xFFB2C8E8, darkOnPrimary = 0xFF1A3248, darkPrimaryContainer = 0xFF324A60,
        darkOnPrimaryContainer = 0xFFD2E4FF, darkSecondary = 0xFFBCC7DB, darkOnSecondary = 0xFF263141,
        darkSecondaryContainer = 0xFF3C4758, darkOnSecondaryContainer = 0xFFD8E3F8,
        darkBg = 0xFF111418, darkOnBg = 0xFFE1E2E6, darkSurfaceVariant = 0xFF43474E,
        darkOnSurfaceVariant = 0xFFC3C6CF, darkOutline = 0xFF8D9199,
    )
    ThemePreset.Forest -> palette(
        lightPrimary = 0xFF3E6A00, lightPrimaryContainer = 0xFFB9F474, lightOnPrimaryContainer = 0xFF0F2000,
        lightSecondary = 0xFF57624A, lightSecondaryContainer = 0xFFDBE7C8, lightOnSecondaryContainer = 0xFF151E0B,
        lightBg = 0xFFF8FBF1, lightOnBg = 0xFF191D13, lightSurfaceVariant = 0xFFE0E4D6,
        lightOnSurfaceVariant = 0xFF44483E, lightOutline = 0xFF74796D,
        darkPrimary = 0xFF9ED75B, darkOnPrimary = 0xFF1E3700, darkPrimaryContainer = 0xFF2E5000,
        darkOnPrimaryContainer = 0xFFB9F474, darkSecondary = 0xFFBFCBAD, darkOnSecondary = 0xFF2A331F,
        darkSecondaryContainer = 0xFF404A34, darkOnSecondaryContainer = 0xFFDBE7C8,
        darkBg = 0xFF11150D, darkOnBg = 0xFFE2E3D8, darkSurfaceVariant = 0xFF44483E,
        darkOnSurfaceVariant = 0xFFC4C8BB, darkOutline = 0xFF8E9285,
    )
    ThemePreset.Violet -> palette(
        lightPrimary = 0xFF6B4EA2, lightPrimaryContainer = 0xFFEBDDFF, lightOnPrimaryContainer = 0xFF23005C,
        lightSecondary = 0xFF645A70, lightSecondaryContainer = 0xFFEBDDF7, lightOnSecondaryContainer = 0xFF1F182A,
        lightBg = 0xFFFEF7FF, lightOnBg = 0xFF1D1A22, lightSurfaceVariant = 0xFFE8E0EB,
        lightOnSurfaceVariant = 0xFF49454E, lightOutline = 0xFF7A757F,
        darkPrimary = 0xFFD3BBFF, darkOnPrimary = 0xFF3B1D70, darkPrimaryContainer = 0xFF533688,
        darkOnPrimaryContainer = 0xFFEBDDFF, darkSecondary = 0xFFCEC2DB, darkOnSecondary = 0xFF352D40,
        darkSecondaryContainer = 0xFF4C4358, darkOnSecondaryContainer = 0xFFEBDDF7,
        darkBg = 0xFF141218, darkOnBg = 0xFFE7E0E8, darkSurfaceVariant = 0xFF49454E,
        darkOnSurfaceVariant = 0xFFCBC4CF, darkOutline = 0xFF948F99,
    )
    ThemePreset.Rose -> palette(
        lightPrimary = 0xFF984061, lightPrimaryContainer = 0xFFFFD9E2, lightOnPrimaryContainer = 0xFF3E001D,
        lightSecondary = 0xFF74565F, lightSecondaryContainer = 0xFFFFD9E2, lightOnSecondaryContainer = 0xFF2B151C,
        lightBg = 0xFFFFF8F8, lightOnBg = 0xFF201A1B, lightSurfaceVariant = 0xFFF2DDE2,
        lightOnSurfaceVariant = 0xFF514347, lightOutline = 0xFF837377,
        darkPrimary = 0xFFFFB1C8, darkOnPrimary = 0xFF5E1133, darkPrimaryContainer = 0xFF7B2949,
        darkOnPrimaryContainer = 0xFFFFD9E2, darkSecondary = 0xFFE2BDC6, darkOnSecondary = 0xFF422931,
        darkSecondaryContainer = 0xFF5B3F47, darkOnSecondaryContainer = 0xFFFFD9E2,
        darkBg = 0xFF181113, darkOnBg = 0xFFEDDFE1, darkSurfaceVariant = 0xFF514347,
        darkOnSurfaceVariant = 0xFFD5C2C6, darkOutline = 0xFF9E8C90,
    )
    ThemePreset.Amber -> palette(
        lightPrimary = 0xFF7C5800, lightPrimaryContainer = 0xFFFFDEA7, lightOnPrimaryContainer = 0xFF271900,
        lightSecondary = 0xFF6D5C3F, lightSecondaryContainer = 0xFFF7DFBB, lightOnSecondaryContainer = 0xFF251A04,
        lightBg = 0xFFFFF8F2, lightOnBg = 0xFF1F1B13, lightSurfaceVariant = 0xFFEEE1CF,
        lightOnSurfaceVariant = 0xFF4E4639, lightOutline = 0xFF807667,
        darkPrimary = 0xFFF7BD48, darkOnPrimary = 0xFF422C00, darkPrimaryContainer = 0xFF5E4100,
        darkOnPrimaryContainer = 0xFFFFDEA7, darkSecondary = 0xFFDAC3A0, darkOnSecondary = 0xFF3C2E15,
        darkSecondaryContainer = 0xFF54452A, darkOnSecondaryContainer = 0xFFF7DFBB,
        darkBg = 0xFF17130B, darkOnBg = 0xFFEAE1D4, darkSurfaceVariant = 0xFF4E4639,
        darkOnSurfaceVariant = 0xFFD1C5B4, darkOutline = 0xFF9A9080,
    )
    ThemePreset.Indigo -> palette(
        lightPrimary = 0xFF4758A9, lightPrimaryContainer = 0xFFDDE1FF, lightOnPrimaryContainer = 0xFF001257,
        lightSecondary = 0xFF5A5D72, lightSecondaryContainer = 0xFFDFE1F9, lightOnSecondaryContainer = 0xFF171B2C,
        lightBg = 0xFFFEFBFF, lightOnBg = 0xFF1B1B1F, lightSurfaceVariant = 0xFFE2E1EC,
        lightOnSurfaceVariant = 0xFF45464F, lightOutline = 0xFF767680,
        darkPrimary = 0xFFB8C4FF, darkOnPrimary = 0xFF142778, darkPrimaryContainer = 0xFF2E3F90,
        darkOnPrimaryContainer = 0xFFDDE1FF, darkSecondary = 0xFFC3C5DD, darkOnSecondary = 0xFF2C2F42,
        darkSecondaryContainer = 0xFF434659, darkOnSecondaryContainer = 0xFFDFE1F9,
        darkBg = 0xFF131316, darkOnBg = 0xFFE4E1E6, darkSurfaceVariant = 0xFF45464F,
        darkOnSurfaceVariant = 0xFFC6C5D0, darkOutline = 0xFF90909A,
    )
    ThemePreset.Lime -> palette(
        lightPrimary = 0xFF4C6700, lightPrimaryContainer = 0xFFCDF067, lightOnPrimaryContainer = 0xFF151F00,
        lightSecondary = 0xFF5B6146, lightSecondaryContainer = 0xFFE0E6C4, lightOnSecondaryContainer = 0xFF191E08,
        lightBg = 0xFFFBFFE7, lightOnBg = 0xFF1A1D12, lightSurfaceVariant = 0xFFE2E4D4,
        lightOnSurfaceVariant = 0xFF45483C, lightOutline = 0xFF76786B,
        darkPrimary = 0xFFB2D34E, darkOnPrimary = 0xFF263500, darkPrimaryContainer = 0xFF384E00,
        darkOnPrimaryContainer = 0xFFCDF067, darkSecondary = 0xFFC4CAAA, darkOnSecondary = 0xFF2E331C,
        darkSecondaryContainer = 0xFF444A31, darkOnSecondaryContainer = 0xFFE0E6C4,
        darkBg = 0xFF12150A, darkOnBg = 0xFFE3E3D7, darkSurfaceVariant = 0xFF45483C,
        darkOnSurfaceVariant = 0xFFC6C8B8, darkOutline = 0xFF909487,
    )
    ThemePreset.Crimson -> palette(
        lightPrimary = 0xFFB3261E, lightPrimaryContainer = 0xFFFFDAD5, lightOnPrimaryContainer = 0xFF410001,
        lightSecondary = 0xFF775651, lightSecondaryContainer = 0xFFFFDAD5, lightOnSecondaryContainer = 0xFF2C1512,
        lightBg = 0xFFFFF8F7, lightOnBg = 0xFF231917, lightSurfaceVariant = 0xFFF5DDDA,
        lightOnSurfaceVariant = 0xFF534341, lightOutline = 0xFF857370,
        darkPrimary = 0xFFFFB4AB, darkOnPrimary = 0xFF690005, darkPrimaryContainer = 0xFF93000A,
        darkOnPrimaryContainer = 0xFFFFDAD5, darkSecondary = 0xFFE7BDB7, darkOnSecondary = 0xFF442925,
        darkSecondaryContainer = 0xFF5D3F3B, darkOnSecondaryContainer = 0xFFFFDAD5,
        darkBg = 0xFF1A1110, darkOnBg = 0xFFF1DFDC, darkSurfaceVariant = 0xFF534341,
        darkOnSurfaceVariant = 0xFFD8C2BE, darkOutline = 0xFFA08C89,
    )
    ThemePreset.Midnight -> palette(
        lightPrimary = 0xFF2F4B75, lightPrimaryContainer = 0xFFD6E3FF, lightOnPrimaryContainer = 0xFF001B3D,
        lightSecondary = 0xFF555F71, lightSecondaryContainer = 0xFFD9E3F8, lightOnSecondaryContainer = 0xFF121C2B,
        lightBg = 0xFFF9F9FF, lightOnBg = 0xFF1A1C20, lightSurfaceVariant = 0xFFE0E2EC,
        lightOnSurfaceVariant = 0xFF43474E, lightOutline = 0xFF74777F,
        darkPrimary = 0xFFA9C7FF, darkOnPrimary = 0xFF002F65, darkPrimaryContainer = 0xFF123F5C,
        darkOnPrimaryContainer = 0xFFD6E3FF, darkSecondary = 0xFFBDC7DC, darkOnSecondary = 0xFF273141,
        darkSecondaryContainer = 0xFF3D4758, darkOnSecondaryContainer = 0xFFD9E3F8,
        darkBg = 0xFF0B0E14, darkOnBg = 0xFFE2E2E9, darkSurfaceVariant = 0xFF43474E,
        darkOnSurfaceVariant = 0xFFC3C6CF, darkOutline = 0xFF8D9199,
    )
    ThemePreset.Sand -> palette(
        lightPrimary = 0xFF7A5900, lightPrimaryContainer = 0xFFFFDEA3, lightOnPrimaryContainer = 0xFF261900,
        lightSecondary = 0xFF6C5C3F, lightSecondaryContainer = 0xFFF5E0BB, lightOnSecondaryContainer = 0xFF241A04,
        lightBg = 0xFFFFF8F0, lightOnBg = 0xFF1F1B13, lightSurfaceVariant = 0xFFEDE1CF,
        lightOnSurfaceVariant = 0xFF4D4639, lightOutline = 0xFF7F7667,
        darkPrimary = 0xFFF5BE48, darkOnPrimary = 0xFF412D00, darkPrimaryContainer = 0xFF5D4200,
        darkOnPrimaryContainer = 0xFFFFDEA3, darkSecondary = 0xFFD8C4A0, darkOnSecondary = 0xFF3B2F15,
        darkSecondaryContainer = 0xFF53452A, darkOnSecondaryContainer = 0xFFF5E0BB,
        darkBg = 0xFF16130B, darkOnBg = 0xFFEAE1D4, darkSurfaceVariant = 0xFF4D4639,
        darkOnSurfaceVariant = 0xFFD0C5B4, darkOutline = 0xFF998F80,
    )
    ThemePreset.Mint -> palette(
        lightPrimary = 0xFF006B5A, lightPrimaryContainer = 0xFF7AF8D9, lightOnPrimaryContainer = 0xFF00201A,
        lightSecondary = 0xFF4A635C, lightSecondaryContainer = 0xFFCCE8E0, lightOnSecondaryContainer = 0xFF05201A,
        lightBg = 0xFFF4FBF7, lightOnBg = 0xFF161D1A, lightSurfaceVariant = 0xFFDAE5E0,
        lightOnSurfaceVariant = 0xFF3F4945, lightOutline = 0xFF6F7975,
        darkPrimary = 0xFF5BDBC0, darkOnPrimary = 0xFF00382D, darkPrimaryContainer = 0xFF005143,
        darkOnPrimaryContainer = 0xFF7AF8D9, darkSecondary = 0xFFB1CCC4, darkOnSecondary = 0xFF1C3530,
        darkSecondaryContainer = 0xFF334B46, darkOnSecondaryContainer = 0xFFCCE8E0,
        darkBg = 0xFF0E1512, darkOnBg = 0xFFDCE5E0, darkSurfaceVariant = 0xFF3F4945,
        darkOnSurfaceVariant = 0xFFBFC9C4, darkOutline = 0xFF89938E,
    )
    ThemePreset.Graphite -> palette(
        lightPrimary = 0xFF5C5C5C, lightPrimaryContainer = 0xFFE3E3E3, lightOnPrimaryContainer = 0xFF1B1B1B,
        lightSecondary = 0xFF5F5E5E, lightSecondaryContainer = 0xFFE4E2E2, lightOnSecondaryContainer = 0xFF1C1B1B,
        lightBg = 0xFFFCFCFC, lightOnBg = 0xFF1C1B1B, lightSurfaceVariant = 0xFFE2E2E2,
        lightOnSurfaceVariant = 0xFF474747, lightOutline = 0xFF777777,
        darkPrimary = 0xFFC7C6C6, darkOnPrimary = 0xFF303030, darkPrimaryContainer = 0xFF474747,
        darkOnPrimaryContainer = 0xFFE3E3E3, darkSecondary = 0xFFC8C6C6, darkOnSecondary = 0xFF313030,
        darkSecondaryContainer = 0xFF484747, darkOnSecondaryContainer = 0xFFE4E2E2,
        darkBg = 0xFF131313, darkOnBg = 0xFFE5E2E1, darkSurfaceVariant = 0xFF474747,
        darkOnSurfaceVariant = 0xFFC7C6C6, darkOutline = 0xFF919090,
    )
    ThemePreset.Coral -> palette(
        lightPrimary = 0xFFA13D2D, lightPrimaryContainer = 0xFFFFDAD3, lightOnPrimaryContainer = 0xFF3E0400,
        lightSecondary = 0xFF775751, lightSecondaryContainer = 0xFFFFDAD3, lightOnSecondaryContainer = 0xFF2C1511,
        lightBg = 0xFFFFF8F6, lightOnBg = 0xFF221917, lightSurfaceVariant = 0xFFF5DDD9,
        lightOnSurfaceVariant = 0xFF534341, lightOutline = 0xFF857370,
        darkPrimary = 0xFFFFB4A5, darkOnPrimary = 0xFF640E05, darkPrimaryContainer = 0xFF812618,
        darkOnPrimaryContainer = 0xFFFFDAD3, darkSecondary = 0xFFE7BDB6, darkOnSecondary = 0xFF442925,
        darkSecondaryContainer = 0xFF5D3F3A, darkOnSecondaryContainer = 0xFFFFDAD3,
        darkBg = 0xFF1A1110, darkOnBg = 0xFFF1DFDC, darkSurfaceVariant = 0xFF534341,
        darkOnSurfaceVariant = 0xFFD8C2BE, darkOutline = 0xFFA08C89,
    )
    ThemePreset.Sky -> palette(
        lightPrimary = 0xFF006A6A, lightPrimaryContainer = 0xFF6FF7F6, lightOnPrimaryContainer = 0xFF002020,
        lightSecondary = 0xFF4A6363, lightSecondaryContainer = 0xFFCCE8E7, lightOnSecondaryContainer = 0xFF051F1F,
        lightBg = 0xFFF4FBFA, lightOnBg = 0xFF161D1D, lightSurfaceVariant = 0xFFDAE5E4,
        lightOnSurfaceVariant = 0xFF3F4948, lightOutline = 0xFF6F7978,
        darkPrimary = 0xFF4CDADB, darkOnPrimary = 0xFF003737, darkPrimaryContainer = 0xFF004F50,
        darkOnPrimaryContainer = 0xFF6FF7F6, darkSecondary = 0xFFB0CCCB, darkOnSecondary = 0xFF1C3534,
        darkSecondaryContainer = 0xFF324B4B, darkOnSecondaryContainer = 0xFFCCE8E7,
        darkBg = 0xFF0E1515, darkOnBg = 0xFFDCE4E3, darkSurfaceVariant = 0xFF3F4948,
        darkOnSurfaceVariant = 0xFFBEC9C8, darkOutline = 0xFF889392,
    )
    ThemePreset.Lavender -> palette(
        lightPrimary = 0xFF6750A4, lightPrimaryContainer = 0xFFE9DDFF, lightOnPrimaryContainer = 0xFF22005D,
        lightSecondary = 0xFF625B71, lightSecondaryContainer = 0xFFE8DEF8, lightOnSecondaryContainer = 0xFF1E192B,
        lightBg = 0xFFFEF7FF, lightOnBg = 0xFF1D1B20, lightSurfaceVariant = 0xFFE7E0EB,
        lightOnSurfaceVariant = 0xFF49454E, lightOutline = 0xFF7A757F,
        darkPrimary = 0xFFCFBCFF, darkOnPrimary = 0xFF381E72, darkPrimaryContainer = 0xFF4F378A,
        darkOnPrimaryContainer = 0xFFE9DDFF, darkSecondary = 0xFFCBC2DB, darkOnSecondary = 0xFF332D41,
        darkSecondaryContainer = 0xFF4A4458, darkOnSecondaryContainer = 0xFFE8DEF8,
        darkBg = 0xFF141218, darkOnBg = 0xFFE6E1E6, darkSurfaceVariant = 0xFF49454E,
        darkOnSurfaceVariant = 0xFFCAC4CF, darkOutline = 0xFF948F99,
    )
    ThemePreset.Copper -> palette(
        lightPrimary = 0xFF8F4E00, lightPrimaryContainer = 0xFFFFDCC2, lightOnPrimaryContainer = 0xFF2E1500,
        lightSecondary = 0xFF745943, lightSecondaryContainer = 0xFFFFDCC2, lightOnSecondaryContainer = 0xFF2A1706,
        lightBg = 0xFFFFF8F5, lightOnBg = 0xFF221A14, lightSurfaceVariant = 0xFFF3DFD1,
        lightOnSurfaceVariant = 0xFF52443A, lightOutline = 0xFF847468,
        darkPrimary = 0xFFFFB77A, darkOnPrimary = 0xFF4C2700, darkPrimaryContainer = 0xFF6C3A00,
        darkOnPrimaryContainer = 0xFFFFDCC2, darkSecondary = 0xFFE4BFA3, darkOnSecondary = 0xFF422B1A,
        darkSecondaryContainer = 0xFF5B412E, darkOnSecondaryContainer = 0xFFFFDCC2,
        darkBg = 0xFF19120D, darkOnBg = 0xFFF0DFD5, darkSurfaceVariant = 0xFF52443A,
        darkOnSurfaceVariant = 0xFFD6C3B6, darkOutline = 0xFF9F8D81,
    )
    ThemePreset.Plum -> palette(
        lightPrimary = 0xFF7D5260, lightPrimaryContainer = 0xFFFFD9E3, lightOnPrimaryContainer = 0xFF31101D,
        lightSecondary = 0xFF6F5860, lightSecondaryContainer = 0xFFF9DAE5, lightOnSecondaryContainer = 0xFF271620,
        lightBg = 0xFFFFF8F8, lightOnBg = 0xFF201A1C, lightSurfaceVariant = 0xFFF2DDE2,
        lightOnSurfaceVariant = 0xFF514347, lightOutline = 0xFF837377,
        darkPrimary = 0xFFEFB8C8, darkOnPrimary = 0xFF492532, darkPrimaryContainer = 0xFF633B48,
        darkOnPrimaryContainer = 0xFFFFD9E3, darkSecondary = 0xFFDCBFC9, darkOnSecondary = 0xFF3E2A32,
        darkSecondaryContainer = 0xFF564049, darkOnSecondaryContainer = 0xFFF9DAE5,
        darkBg = 0xFF171214, darkOnBg = 0xFFECE0E2, darkSurfaceVariant = 0xFF514347,
        darkOnSurfaceVariant = 0xFFD5C2C6, darkOutline = 0xFF9E8C90,
    )
    ThemePreset.Moss -> palette(
        lightPrimary = 0xFF556500, lightPrimaryContainer = 0xFFD8EC7A, lightOnPrimaryContainer = 0xFF181E00,
        lightSecondary = 0xFF5C6146, lightSecondaryContainer = 0xFFE1E6C3, lightOnSecondaryContainer = 0xFF191E08,
        lightBg = 0xFFF9FBEC, lightOnBg = 0xFF1A1D12, lightSurfaceVariant = 0xFFE2E4D4,
        lightOnSurfaceVariant = 0xFF45483C, lightOutline = 0xFF76786B,
        darkPrimary = 0xFFBCD061, darkOnPrimary = 0xFF2B3400, darkPrimaryContainer = 0xFF404C00,
        darkOnPrimaryContainer = 0xFFD8EC7A, darkSecondary = 0xFFC5CAA8, darkOnSecondary = 0xFF2E331C,
        darkSecondaryContainer = 0xFF444A31, darkOnSecondaryContainer = 0xFFE1E6C3,
        darkBg = 0xFF12150A, darkOnBg = 0xFFE3E3D7, darkSurfaceVariant = 0xFF45483C,
        darkOnSurfaceVariant = 0xFFC6C8B8, darkOutline = 0xFF909487,
    )
    ThemePreset.Cherry -> palette(
        lightPrimary = 0xFFB0123A, lightPrimaryContainer = 0xFFFFD9DE, lightOnPrimaryContainer = 0xFF400011,
        lightSecondary = 0xFF76565A, lightSecondaryContainer = 0xFFFFD9DE, lightOnSecondaryContainer = 0xFF2C1518,
        lightBg = 0xFFFFF8F7, lightOnBg = 0xFF22191A, lightSurfaceVariant = 0xFFF4DDDF,
        lightOnSurfaceVariant = 0xFF524345, lightOutline = 0xFF857374,
        darkPrimary = 0xFFFFB2BE, darkOnPrimary = 0xFF670020, darkPrimaryContainer = 0xFF8E002C,
        darkOnPrimaryContainer = 0xFFFFD9DE, darkSecondary = 0xFFE5BDC1, darkOnSecondary = 0xFF43292D,
        darkSecondaryContainer = 0xFF5C3F43, darkOnSecondaryContainer = 0xFFFFD9DE,
        darkBg = 0xFF1A1112, darkOnBg = 0xFFF0DFE0, darkSurfaceVariant = 0xFF524345,
        darkOnSurfaceVariant = 0xFFD7C1C3, darkOutline = 0xFF9F8C8E,
    )
    ThemePreset.Arctic -> palette(
        lightPrimary = 0xFF006877, lightPrimaryContainer = 0xFFA1EFFF, lightOnPrimaryContainer = 0xFF001F25,
        lightSecondary = 0xFF4A6268, lightSecondaryContainer = 0xFFCDE7ED, lightOnSecondaryContainer = 0xFF051F23,
        lightBg = 0xFFF5FAFB, lightOnBg = 0xFF171D1E, lightSurfaceVariant = 0xFFDBE4E6,
        lightOnSurfaceVariant = 0xFF3F484A, lightOutline = 0xFF6F797B,
        darkPrimary = 0xFF54D7F0, darkOnPrimary = 0xFF00363E, darkPrimaryContainer = 0xFF004E59,
        darkOnPrimaryContainer = 0xFFA1EFFF, darkSecondary = 0xFFB1CBD1, darkOnSecondary = 0xFF1C3439,
        darkSecondaryContainer = 0xFF334A50, darkOnSecondaryContainer = 0xFFCDE7ED,
        darkBg = 0xFF0E1416, darkOnBg = 0xFFDEE3E5, darkSurfaceVariant = 0xFF3F484A,
        darkOnSurfaceVariant = 0xFFBFC8CA, darkOutline = 0xFF899294,
    )
    ThemePreset.Honey -> palette(
        lightPrimary = 0xFF825500, lightPrimaryContainer = 0xFFFFDDB3, lightOnPrimaryContainer = 0xFF291800,
        lightSecondary = 0xFF6F5B40, lightSecondaryContainer = 0xFFFADFBB, lightOnSecondaryContainer = 0xFF261904,
        lightBg = 0xFFFFF8F3, lightOnBg = 0xFF1F1B16, lightSurfaceVariant = 0xFFF0E0CF,
        lightOnSurfaceVariant = 0xFF4F4539, lightOutline = 0xFF817567,
        darkPrimary = 0xFFFFB951, darkOnPrimary = 0xFF442B00, darkPrimaryContainer = 0xFF624000,
        darkOnPrimaryContainer = 0xFFFFDDB3, darkSecondary = 0xFFDDC3A1, darkOnSecondary = 0xFF3E2D16,
        darkSecondaryContainer = 0xFF56432B, darkOnSecondaryContainer = 0xFFFADFBB,
        darkBg = 0xFF17130D, darkOnBg = 0xFFEAE1D9, darkSurfaceVariant = 0xFF4F4539,
        darkOnSurfaceVariant = 0xFFD3C4B4, darkOutline = 0xFF9C8F80,
    )
    ThemePreset.Orchid -> palette(
        lightPrimary = 0xFF9A25AE, lightPrimaryContainer = 0xFFFFD6FF, lightOnPrimaryContainer = 0xFF350040,
        lightSecondary = 0xFF6B586B, lightSecondaryContainer = 0xFFF4DBF1, lightOnSecondaryContainer = 0xFF251626,
        lightBg = 0xFFFFF7FB, lightOnBg = 0xFF1F1A1F, lightSurfaceVariant = 0xFFEDDFE8,
        lightOnSurfaceVariant = 0xFF4E444B, lightOutline = 0xFF7F747C,
        darkPrimary = 0xFFF9ABFF, darkOnPrimary = 0xFF57006B, darkPrimaryContainer = 0xFF7B008F,
        darkOnPrimaryContainer = 0xFFFFD6FF, darkSecondary = 0xFFD7BFD5, darkOnSecondary = 0xFF3B2B3C,
        darkSecondaryContainer = 0xFF534153, darkOnSecondaryContainer = 0xFFF4DBF1,
        darkBg = 0xFF171217, darkOnBg = 0xFFEAE0E7, darkSurfaceVariant = 0xFF4E444B,
        darkOnSurfaceVariant = 0xFFD0C3CC, darkOutline = 0xFF998D96,
    )
    ThemePreset.Steel -> palette(
        lightPrimary = 0xFF515F7A, lightPrimaryContainer = 0xFFD8E3FF, lightOnPrimaryContainer = 0xFF0D1B33,
        lightSecondary = 0xFF575E71, lightSecondaryContainer = 0xFFDBE2F9, lightOnSecondaryContainer = 0xFF141B2C,
        lightBg = 0xFFF9F9FC, lightOnBg = 0xFF1A1B1F, lightSurfaceVariant = 0xFFE1E2EC,
        lightOnSurfaceVariant = 0xFF44474F, lightOutline = 0xFF757780,
        darkPrimary = 0xFFB9C7E5, darkOnPrimary = 0xFF233148, darkPrimaryContainer = 0xFF394861,
        darkOnPrimaryContainer = 0xFFD8E3FF, darkSecondary = 0xFFBFC6DC, darkOnSecondary = 0xFF293041,
        darkSecondaryContainer = 0xFF3F4759, darkOnSecondaryContainer = 0xFFDBE2F9,
        darkBg = 0xFF121317, darkOnBg = 0xFFE2E2E6, darkSurfaceVariant = 0xFF44474F,
        darkOnSurfaceVariant = 0xFFC4C6D0, darkOutline = 0xFF8E9099,
    )
    ThemePreset.Twilight -> palette(
        lightPrimary = 0xFF5355A9, lightPrimaryContainer = 0xFFE1E0FF, lightOnPrimaryContainer = 0xFF0C0664,
        lightSecondary = 0xFF5D5C72, lightSecondaryContainer = 0xFFE2E0F9, lightOnSecondaryContainer = 0xFF1A1A2C,
        lightBg = 0xFFFFF8FF, lightOnBg = 0xFF1B1B23, lightSurfaceVariant = 0xFFE3E1EC,
        lightOnSurfaceVariant = 0xFF46464F, lightOutline = 0xFF777680,
        darkPrimary = 0xFFC0C1FF, darkOnPrimary = 0xFF242478, darkPrimaryContainer = 0xFF3B3D8F,
        darkOnPrimaryContainer = 0xFFE1E0FF, darkSecondary = 0xFFC6C4DC, darkOnSecondary = 0xFF2F2F42,
        darkSecondaryContainer = 0xFF454559, darkOnSecondaryContainer = 0xFFE2E0F9,
        darkBg = 0xFF13131A, darkOnBg = 0xFFE4E1E9, darkSurfaceVariant = 0xFF46464F,
        darkOnSurfaceVariant = 0xFFC7C5D0, darkOutline = 0xFF91909A,
    )
    ThemePreset.Sage -> palette(
        lightPrimary = 0xFF4A6700, lightPrimaryContainer = 0xFFC9F174, lightOnPrimaryContainer = 0xFF141F00,
        lightSecondary = 0xFF5A6147, lightSecondaryContainer = 0xFFDEE6C4, lightOnSecondaryContainer = 0xFF171E09,
        lightBg = 0xFFF8FAED, lightOnBg = 0xFF1A1D12, lightSurfaceVariant = 0xFFE1E4D4,
        lightOnSurfaceVariant = 0xFF44483D, lightOutline = 0xFF75786B,
        darkPrimary = 0xFFAED55B, darkOnPrimary = 0xFF253600, darkPrimaryContainer = 0xFF374E00,
        darkOnPrimaryContainer = 0xFFC9F174, darkSecondary = 0xFFC2CAA9, darkOnSecondary = 0xFF2C331C,
        darkSecondaryContainer = 0xFF424A31, darkOnSecondaryContainer = 0xFFDEE6C4,
        darkBg = 0xFF12150A, darkOnBg = 0xFFE2E3D7, darkSurfaceVariant = 0xFF44483D,
        darkOnSurfaceVariant = 0xFFC5C8B8, darkOutline = 0xFF8F9285,
    )
    ThemePreset.Magenta -> palette(
        lightPrimary = 0xFFA90079, lightPrimaryContainer = 0xFFFFD8EA, lightOnPrimaryContainer = 0xFF3B0028,
        lightSecondary = 0xFF725764, lightSecondaryContainer = 0xFFFDD9E9, lightOnSecondaryContainer = 0xFF2A151F,
        lightBg = 0xFFFFF8F9, lightOnBg = 0xFF21191D, lightSurfaceVariant = 0xFFF4DCE5,
        lightOnSurfaceVariant = 0xFF524248, lightOutline = 0xFF857378,
        darkPrimary = 0xFFFFAEDC, darkOnPrimary = 0xFF600043, darkPrimaryContainer = 0xFF87005E,
        darkOnPrimaryContainer = 0xFFFFD8EA, darkSecondary = 0xFFE0BBCD, darkOnSecondary = 0xFF412A34,
        darkSecondaryContainer = 0xFF59404B, darkOnSecondaryContainer = 0xFFFDD9E9,
        darkBg = 0xFF181115, darkOnBg = 0xFFEDDFE4, darkSurfaceVariant = 0xFF524248,
        darkOnSurfaceVariant = 0xFFD7C1C9, darkOutline = 0xFF9F8C92,
    )
    ThemePreset.Espresso -> palette(
        lightPrimary = 0xFF6F5734, lightPrimaryContainer = 0xFFFADEB4, lightOnPrimaryContainer = 0xFF261900,
        lightSecondary = 0xFF6B5D48, lightSecondaryContainer = 0xFFF4E0C3, lightOnSecondaryContainer = 0xFF241A09,
        lightBg = 0xFFFFF8F3, lightOnBg = 0xFF1F1B16, lightSurfaceVariant = 0xFFEFE1D0,
        lightOnSurfaceVariant = 0xFF4F4539, lightOutline = 0xFF817567,
        darkPrimary = 0xFFDDC29A, darkOnPrimary = 0xFF3D2E0A, darkPrimaryContainer = 0xFF55431E,
        darkOnPrimaryContainer = 0xFFFADEB4, darkSecondary = 0xFFD7C4A8, darkOnSecondary = 0xFF3A2F1C,
        darkSecondaryContainer = 0xFF524531, darkOnSecondaryContainer = 0xFFF4E0C3,
        darkBg = 0xFF15110C, darkOnBg = 0xFFEAE1D9, darkSurfaceVariant = 0xFF4F4539,
        darkOnSurfaceVariant = 0xFFD2C5B4, darkOutline = 0xFF9B8F80,
    )
    ThemePreset.Aqua -> palette(
        lightPrimary = 0xFF006972, lightPrimaryContainer = 0xFF8DF2FF, lightOnPrimaryContainer = 0xFF001F23,
        lightSecondary = 0xFF4A6366, lightSecondaryContainer = 0xFFCDE7EB, lightOnSecondaryContainer = 0xFF051F22,
        lightBg = 0xFFF4FAFB, lightOnBg = 0xFF161D1E, lightSurfaceVariant = 0xFFDAE4E6,
        lightOnSurfaceVariant = 0xFF3F484A, lightOutline = 0xFF6F797B,
        darkPrimary = 0xFF4ED8E7, darkOnPrimary = 0xFF00363C, darkPrimaryContainer = 0xFF004F56,
        darkOnPrimaryContainer = 0xFF8DF2FF, darkSecondary = 0xFFB1CBCE, darkOnSecondary = 0xFF1C3437,
        darkSecondaryContainer = 0xFF334B4E, darkOnSecondaryContainer = 0xFFCDE7EB,
        darkBg = 0xFF0E1516, darkOnBg = 0xFFDEE3E5, darkSurfaceVariant = 0xFF3F484A,
        darkOnSurfaceVariant = 0xFFBEC8CA, darkOutline = 0xFF899295,
    )
    ThemePreset.Peach -> palette(
        lightPrimary = 0xFF9C4325, lightPrimaryContainer = 0xFFFFDBCF, lightOnPrimaryContainer = 0xFF380D00,
        lightSecondary = 0xFF77574B, lightSecondaryContainer = 0xFFFFDBCF, lightOnSecondaryContainer = 0xFF2C160D,
        lightBg = 0xFFFFF8F6, lightOnBg = 0xFF221A17, lightSurfaceVariant = 0xFFF5DED7,
        lightOnSurfaceVariant = 0xFF53433E, lightOutline = 0xFF85736D,
        darkPrimary = 0xFFFFB59A, darkOnPrimary = 0xFF5B1A00, darkPrimaryContainer = 0xFF7C2E10,
        darkOnPrimaryContainer = 0xFFFFDBCF, darkSecondary = 0xFFE7BDB0, darkOnSecondary = 0xFF442A20,
        darkSecondaryContainer = 0xFF5D4035, darkOnSecondaryContainer = 0xFFFFDBCF,
        darkBg = 0xFF1A110F, darkOnBg = 0xFFF1DFD9, darkSurfaceVariant = 0xFF53433E,
        darkOnSurfaceVariant = 0xFFD8C2BA, darkOutline = 0xFFA08D86,
    )
}

private val DecibelTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)

private val DecibelShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun DecibelTheme(
    themeMode: ThemeMode = ThemeMode.System,
    preset: ThemePreset = ThemePreset.Teal,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val colors = paletteFor(preset)
    CompositionLocalProvider(LocalDecibelDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = if (dark) colors.dark else colors.light,
            typography = DecibelTypography,
            shapes = DecibelShapes,
            content = content,
        )
    }
}
