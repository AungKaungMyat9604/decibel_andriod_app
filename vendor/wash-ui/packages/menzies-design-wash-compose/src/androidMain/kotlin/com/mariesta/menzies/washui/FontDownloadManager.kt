package com.mariesta.menzies.washui

import android.content.Context
import androidx.compose.ui.text.font.FontFamily
import java.io.File
import java.net.URL

object FontDownloadManager {
    private const val MAPLE_URL =
        "https://github.com/subframe7536/maple-font/raw/refs/heads/master/fonts/MapleMono-NF-Regular.ttf"
    private const val FRAUNCES_URL =
        "https://github.com/google/fonts/raw/main/ofl/fraunces/Fraunces%5Bopsz,wght%5D.ttf"

    /**
     * Load a cached body font if present. Never blocks on network; use [ensureFonts] to download.
     */
    fun loadBodyFamily(context: Context): FontFamily? {
        val file = fontFile(context, "MapleMono-NF-Regular.ttf")
        if (!file.exists()) return null
        return runCatching {
            FontFamily(androidx.compose.ui.text.font.Font(file))
        }.getOrNull()
    }

    suspend fun ensureFonts(context: Context, onProgress: (Float) -> Unit = {}) {
        val targets = listOf(
            "MapleMono-NF-Regular.ttf" to MAPLE_URL,
            "Fraunces.ttf" to FRAUNCES_URL,
        )
        targets.forEachIndexed { index, (name, url) ->
            val file = fontFile(context, name)
            if (!file.exists()) download(context, url, file)
            onProgress((index + 1f) / targets.size)
        }
    }

    private fun fontFile(context: Context, name: String): File {
        val dir = File(context.filesDir, "wash-fonts")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, name)
    }

    private fun download(context: Context, url: String, dest: File) {
        context.assets // touch context classloader
        URL(url).openStream().use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
    }
}
