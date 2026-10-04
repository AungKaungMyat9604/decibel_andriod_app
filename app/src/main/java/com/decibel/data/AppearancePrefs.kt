package com.decibel.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    System,
    Light,
    Dark,
    ;

    val label: String
        get() = when (this) {
            System -> "System"
            Light -> "Light"
            Dark -> "Dark"
        }
}

enum class ThemePreset {
    Teal,
    Ocean,
    Ember,
    Slate,
    Forest,
    Violet,
    Rose,
    Amber,
    Indigo,
    Lime,
    Crimson,
    Midnight,
    Sand,
    Mint,
    Graphite,
    Coral,
    Sky,
    Lavender,
    Copper,
    Plum,
    Moss,
    Cherry,
    Arctic,
    Honey,
    Orchid,
    Steel,
    Twilight,
    Sage,
    Magenta,
    Espresso,
    Aqua,
    Peach,
    ;

    val label: String
        get() = name
}

/** AMOLED standby visualizer styles. */
enum class StandbyWaveStyle {
    Bars,
    DenseBars,
    PeakBars,
    FloorBars,
    LedBars,
    SoftWave,
    Rings,
    Orbit,
    Pulse,
    MirroredBars,
    Spectrum,
    SpectrumArea,
    CenterSpectrum,
    SplitSpectrum,
    NeonBars,
    Spiral,
    Lattice,
    Ribbon,
    Starburst,
    ;

    val label: String
        get() = when (this) {
            Bars -> "Equalizer bars"
            DenseBars -> "Dense bars"
            PeakBars -> "Peak bars"
            FloorBars -> "Floor bars"
            LedBars -> "LED bars"
            SoftWave -> "Soft wave"
            Rings -> "Pulse rings"
            Orbit -> "Orbit"
            Pulse -> "Breathing pulse"
            MirroredBars -> "Mirrored bars"
            Spectrum -> "Spectrum fill"
            SpectrumArea -> "Spectrum area"
            CenterSpectrum -> "Center spectrum"
            SplitSpectrum -> "Split spectrum"
            NeonBars -> "Neon bars"
            Spiral -> "Spiral"
            Lattice -> "Lattice"
            Ribbon -> "Ribbon"
            Starburst -> "Starburst"
        }
}

/** Auto-enter standby while music is playing. */
enum class StandbyAutoEnter {
    Off,
    After1Min,
    After5Min,
    After15Min,
    ;

    val label: String
        get() = when (this) {
            Off -> "Off"
            After1Min -> "After 1 minute"
            After5Min -> "After 5 minutes"
            After15Min -> "After 15 minutes"
        }

    val delayMs: Long?
        get() = when (this) {
            Off -> null
            After1Min -> 60_000L
            After5Min -> 5 * 60_000L
            After15Min -> 15 * 60_000L
        }
}

class AppearancePrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _themePreset = MutableStateFlow(loadPreset())
    val themePreset: StateFlow<ThemePreset> = _themePreset.asStateFlow()

    private val _standbyWaveStyle = MutableStateFlow(loadWaveStyle())
    val standbyWaveStyle: StateFlow<StandbyWaveStyle> = _standbyWaveStyle.asStateFlow()

    private val _standbyAutoEnter = MutableStateFlow(loadAutoEnter())
    val standbyAutoEnter: StateFlow<StandbyAutoEnter> = _standbyAutoEnter.asStateFlow()

    private val _standbyCycleWave = MutableStateFlow(prefs.getBoolean(KEY_STANDBY_CYCLE, false))
    val standbyCycleWave: StateFlow<Boolean> = _standbyCycleWave.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setThemePreset(preset: ThemePreset) {
        prefs.edit().putString(KEY_PRESET, preset.name).apply()
        _themePreset.value = preset
    }

    fun setStandbyWaveStyle(style: StandbyWaveStyle) {
        prefs.edit().putString(KEY_STANDBY_WAVE, style.name).apply()
        _standbyWaveStyle.value = style
    }

    fun setStandbyAutoEnter(auto: StandbyAutoEnter) {
        prefs.edit().putString(KEY_STANDBY_AUTO, auto.name).apply()
        _standbyAutoEnter.value = auto
    }

    fun setStandbyCycleWave(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STANDBY_CYCLE, enabled).apply()
        _standbyCycleWave.value = enabled
    }

    private fun loadMode(): ThemeMode =
        ThemeMode.entries.find { it.name == prefs.getString(KEY_MODE, null) }
            ?: ThemeMode.System

    private fun loadPreset(): ThemePreset =
        ThemePreset.entries.find { it.name == prefs.getString(KEY_PRESET, null) }
            ?: ThemePreset.Teal

    private fun loadWaveStyle(): StandbyWaveStyle =
        StandbyWaveStyle.entries.find { it.name == prefs.getString(KEY_STANDBY_WAVE, null) }
            ?: StandbyWaveStyle.SoftWave

    private fun loadAutoEnter(): StandbyAutoEnter =
        StandbyAutoEnter.entries.find { it.name == prefs.getString(KEY_STANDBY_AUTO, null) }
            ?: StandbyAutoEnter.Off

    companion object {
        private const val PREFS = "decibel_appearance"
        private const val KEY_MODE = "theme_mode"
        private const val KEY_PRESET = "theme_preset"
        private const val KEY_STANDBY_WAVE = "standby_wave"
        private const val KEY_STANDBY_AUTO = "standby_auto"
        private const val KEY_STANDBY_CYCLE = "standby_cycle_wave"
    }
}
