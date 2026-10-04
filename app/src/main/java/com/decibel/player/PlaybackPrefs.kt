package com.decibel.player

import android.content.Context

enum class RepeatMode {
    Off,
    All,
    One,
}

data class LastPlaybackSession(
    val itemId: String,
    val queueIds: List<String>,
    val positionMs: Long,
)

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

    fun saveLastSession(itemId: String, queueIds: List<String>, positionMs: Long) {
        prefs.edit()
            .putString(KEY_LAST_ID, itemId)
            .putString(KEY_LAST_QUEUE, queueIds.joinToString("\u001f"))
            .putLong(KEY_LAST_POS, positionMs.coerceAtLeast(0L))
            .apply()
    }

    fun loadLastSession(): LastPlaybackSession? {
        val id = prefs.getString(KEY_LAST_ID, null)?.takeIf { it.isNotBlank() } ?: return null
        val queueRaw = prefs.getString(KEY_LAST_QUEUE, null).orEmpty()
        val queue = queueRaw.split('\u001f').map { it.trim() }.filter { it.isNotEmpty() }
            .ifEmpty { listOf(id) }
        val pos = prefs.getLong(KEY_LAST_POS, 0L).coerceAtLeast(0L)
        return LastPlaybackSession(itemId = id, queueIds = queue, positionMs = pos)
    }

    fun clearLastSession() {
        prefs.edit()
            .remove(KEY_LAST_ID)
            .remove(KEY_LAST_QUEUE)
            .remove(KEY_LAST_POS)
            .apply()
    }

    companion object {
        private const val PREFS = "decibel_playback"
        private const val KEY_SHUFFLE = "shuffle"
        private const val KEY_REPEAT = "repeat"
        private const val KEY_LAST_ID = "last_item_id"
        private const val KEY_LAST_QUEUE = "last_queue_ids"
        private const val KEY_LAST_POS = "last_position_ms"
    }
}
