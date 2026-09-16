package com.decibel.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Caches library preview JPEGs under filesDir/thumbs so Coil works offline.
 */
class ThumbnailStore(context: Context) {
    private val appContext = context.applicationContext
    private val thumbsDir = File(appContext.filesDir, "thumbs").apply { mkdirs() }
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun fileFor(entryId: String): File = File(thumbsDir, "$entryId.jpg")

    fun pathIfExists(entryId: String): String? {
        val f = fileFor(entryId)
        return f.takeIf { it.exists() && it.length() > 0 }?.absolutePath
    }

    fun isLocalPath(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return path.startsWith("/") ||
            path.startsWith("file:") ||
            (!path.startsWith("http://") && !path.startsWith("https://") && File(path).exists())
    }

    /** Download remote image URL into thumbs/{entryId}.jpg. Returns absolute path or null. */
    fun cacheFromUrl(entryId: String, remoteUrl: String?): String? {
        if (remoteUrl.isNullOrBlank()) return pathIfExists(entryId)
        val out = fileFor(entryId)
        return runCatching {
            val request = Request.Builder()
                .url(remoteUrl)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
                )
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body ?: return@use null
                out.outputStream().use { output -> body.byteStream().copyTo(output) }
            }
            out.takeIf { it.exists() && it.length() > 0 }?.absolutePath
        }.getOrNull() ?: pathIfExists(entryId)
    }

    /** Extract a frame from a local video file/content URI. */
    fun cacheFromVideo(entryId: String, fileRef: String): String? {
        pathIfExists(entryId)?.let { return it }
        val out = fileFor(entryId)
        return runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                if (fileRef.startsWith("content:")) {
                    retriever.setDataSource(appContext, Uri.parse(fileRef))
                } else {
                    retriever.setDataSource(fileRef)
                }
                val bitmap = retriever.getFrameAtTime(
                    1_000_000,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                ) ?: retriever.frameAtTime ?: return null
                FileOutputStream(out).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 82, fos)
                }
                if (!bitmap.isRecycled) bitmap.recycle()
                out.takeIf { it.exists() && it.length() > 0 }?.absolutePath
            } finally {
                runCatching { retriever.release() }
            }
        }.getOrNull()
    }

    fun delete(entryId: String) {
        fileFor(entryId).delete()
    }
}
