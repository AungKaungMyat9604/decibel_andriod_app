package com.decibel.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.min

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

    /**
     * Center-crops [sourcePath] to a square JPEG for ID3 album art.
     * Avoids letterbox borders when players show a 1:1 cover slot.
     */
    fun writeSquareCover(
        sourcePath: String,
        dest: File,
        sizePx: Int = 600,
    ): String? {
        val src = File(sourcePath.removePrefix("file://"))
        if (!src.isFile || src.length() <= 0L) return null
        return runCatching {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(src.absolutePath, opts)
            val maxDim = maxOf(opts.outWidth, opts.outHeight).coerceAtLeast(1)
            val sample = generateSequence(1) { it * 2 }
                .takeWhile { maxDim / it > sizePx * 2 }
                .lastOrNull() ?: 1
            val decoded = BitmapFactory.decodeFile(
                src.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sample },
            ) ?: return@runCatching null

            val side = min(decoded.width, decoded.height)
            if (side <= 0) {
                decoded.recycle()
                return@runCatching null
            }
            val x = (decoded.width - side) / 2
            val y = (decoded.height - side) / 2
            val cropped = Bitmap.createBitmap(decoded, x, y, side, side)
            val square = if (side == sizePx) {
                cropped
            } else {
                Bitmap.createScaledBitmap(cropped, sizePx, sizePx, true).also {
                    if (it !== cropped) cropped.recycle()
                }
            }
            if (square !== decoded) decoded.recycle()

            dest.parentFile?.mkdirs()
            FileOutputStream(dest).use { fos ->
                square.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }
            if (!square.isRecycled) square.recycle()
            dest.takeIf { it.exists() && it.length() > 0 }?.absolutePath
        }.getOrNull()
    }

    fun delete(entryId: String) {
        fileFor(entryId).delete()
    }
}
