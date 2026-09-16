package com.decibel.player

import android.content.Context

enum class RepeatMode {
    Off,
    All,
    One,
}

class PlaybackPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var shuffleEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHUFFLE, false)
        set(value) = prefs.edit().putBoolean(KEY_SHUFFLE, value).apply()

    var repeatMode: RepeatMode
        get() = when (prefs.getString(KEY_REPEAT, RepeatMode.Off.name)) {
            RepeatMode.All.name -> RepeatMode.All
            RepeatMode.One.name -> RepeatMode.One
            else -> RepeatMode.Off
        }
        set(value) = prefs.edit().putString(KEY_REPEAT, value.name).apply()

    companion object {
        private const val PREFS = "decibel_playback"
        private const val KEY_SHUFFLE = "shuffle"
        private const val KEY_REPEAT = "repeat"
    }
}
