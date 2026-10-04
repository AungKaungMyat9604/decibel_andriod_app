package com.decibel.player

import android.content.Context

class EqPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var presetId: String
        get() = prefs.getString(KEY_PRESET, PRESET_FLAT) ?: PRESET_FLAT
        set(value) = prefs.edit().putString(KEY_PRESET, value).apply()

    var bandLevelsMb: IntArray
        get() {
            val raw = prefs.getString(KEY_BANDS, null) ?: return intArrayOf()
            if (raw.isBlank()) return intArrayOf()
            return raw.split(',').mapNotNull { it.trim().toIntOrNull() }.toIntArray()
        }
        set(value) = prefs.edit().putString(KEY_BANDS, value.joinToString(",")).apply()

    companion object {
        const val PRESET_FLAT = "flat"
        const val PRESET_BASS = "bass"
        const val PRESET_TREBLE = "treble"
        const val PRESET_VOCAL = "vocal"
        const val PRESET_ROCK = "rock"
        const val PRESET_POP = "pop"
        const val PRESET_ELECTRONIC = "electronic"
        const val PRESET_HIP_HOP = "hip_hop"
        const val PRESET_JAZZ = "jazz"
        const val PRESET_CLASSICAL = "classical"
        const val PRESET_ACOUSTIC = "acoustic"
        const val PRESET_LOUDNESS = "loudness"
        const val PRESET_CUSTOM = "custom"

        private const val PREFS = "decibel_eq"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_PRESET = "preset"
        private const val KEY_BANDS = "bands_mb"
    }
}
