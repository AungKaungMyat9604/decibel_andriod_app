package com.mariesta.menzies.washui.theme

import com.mariesta.menzies.washui.theme.WashPigment.mineral
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class WashThemeTest {
    @Test
    fun mineralLightSchemeExists() {
        val scheme = resolveWashColorScheme(mineral, WashMode.Light)
        assertEquals(mineral, scheme.pigment)
        assertEquals(WashMode.Light, scheme.mode)
        assertEquals(0.15294118f, scheme.primary.red, 0.001f) // #276C8E
    }

    @Test
    fun mineralDarkSchemeExists() {
        val scheme = resolveWashColorScheme(mineral, WashMode.Dark)
        assertNotNull(scheme)
        assertEquals(WashMode.Dark, scheme.mode)
    }

    @Test
    fun allPigmentsHaveLightAndDark() {
        WashPigment.entries.forEach { pigment ->
            assertNotNull(resolveWashColorScheme(pigment, WashMode.Light))
            assertNotNull(resolveWashColorScheme(pigment, WashMode.Dark))
        }
    }
}
