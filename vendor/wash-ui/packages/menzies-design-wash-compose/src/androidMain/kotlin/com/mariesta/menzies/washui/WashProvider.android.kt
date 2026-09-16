package com.mariesta.menzies.washui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mariesta.menzies.washui.theme.MODE_STORAGE_KEY
import com.mariesta.menzies.washui.theme.THEME_STORAGE_KEY
import com.mariesta.menzies.washui.theme.WashMode
import com.mariesta.menzies.washui.theme.WashPigment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.washDataStore by preferencesDataStore(name = "wash_ui_prefs")

private val pigmentKey = stringPreferencesKey(THEME_STORAGE_KEY)
private val modeKey = stringPreferencesKey(MODE_STORAGE_KEY)

class AndroidWashPreferences(
    private val context: Context,
) : WashPreferences {
    override suspend fun readPigment(): WashPigment? {
        val id = context.washDataStore.data.map { it[pigmentKey] }.first() ?: return null
        return runCatching { WashPigment.fromId(id) }.getOrNull()
    }

    override suspend fun readMode(): WashMode? {
        val raw = context.washDataStore.data.map { it[modeKey] }.first() ?: return null
        return when (raw) {
            "dark" -> WashMode.Dark
            "light" -> WashMode.Light
            else -> null
        }
    }

    override suspend fun writePigment(pigment: WashPigment) {
        context.washDataStore.edit { it[pigmentKey] = pigment.id }
    }

    override suspend fun writeMode(mode: WashMode) {
        context.washDataStore.edit {
            it[modeKey] = when (mode) {
                WashMode.Light -> "light"
                WashMode.Dark -> "dark"
            }
        }
    }
}

@Composable
actual fun rememberWashPreferences(): WashPreferences {
    val context = LocalContext.current
    return remember(context) { AndroidWashPreferences(context.applicationContext) }
}
