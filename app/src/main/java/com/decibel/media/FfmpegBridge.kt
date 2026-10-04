package com.decibel.media

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File

/**
 * Thin wrapper around FFmpeg Kit for DASH merge and MP3 encode.
 * Prefer stream-copy; fall back to AAC audio re-encode when needed for MP4.
 */
object FfmpegBridge {
    fun mergeAv(videoPath: String, audioPath: String, outMp4: String): Result<Unit> {
        File(outMp4).parentFile?.mkdirs()
        val copy = run(
            "-y -i ${q(videoPath)} -i ${q(audioPath)} " +
                "-map 0:v:0 -map 1:a:0 -c:v copy -c:a copy -shortest ${q(outMp4)}",
        )
        if (copy.isSuccess) return copy
        File(outMp4).delete()
        return run(
            "-y -i ${q(videoPath)} -i ${q(audioPath)} " +
                "-map 0:v:0 -map 1:a:0 -c:v copy -c:a aac -b:a 256k -shortest ${q(outMp4)}",
        )
    }

    /**
     * Encode audio to MP3. When [coverArtPath] points to a local JPEG/PNG,
     * embeds it as ID3 attached picture so file managers / other apps show art.
     */
    fun encodeMp3(
        inputPath: String,
        outMp3: String,
        bitrateKbps: Int = 320,
        coverArtPath: String? = null,
        title: String? = null,
        artist: String? = null,
    ): Result<Unit> {
        File(outMp3).parentFile?.mkdirs()
        val meta = buildString {
            if (!title.isNullOrBlank()) append(" -metadata title=${q(title)}")
            if (!artist.isNullOrBlank()) append(" -metadata artist=${q(artist)}")
        }
        val cover = coverArtPath
            ?.takeIf { path -> File(path).let { it.isFile && it.length() > 0L } }

        if (cover != null) {
            val withArt = run(
                "-y -i ${q(inputPath)} -i ${q(cover)} " +
                    "-map 0:a:0 -map 1:0 " +
                    "-c:a libmp3lame -b:a ${bitrateKbps}k " +
                    "-c:v copy -disposition:v:0 attached_pic " +
                    "-id3v2_version 3 " +
                    "-metadata:s:v title=${q("Album cover")} " +
                    "-metadata:s:v comment=${q("Cover (front)")}" +
                    "$meta ${q(outMp3)}",
            )
            if (withArt.isSuccess) return withArt
            File(outMp3).delete()
            val reencoded = run(
                "-y -i ${q(inputPath)} -i ${q(cover)} " +
                    "-map 0:a:0 -map 1:0 " +
                    "-c:a libmp3lame -b:a ${bitrateKbps}k " +
                    "-c:v mjpeg -disposition:v:0 attached_pic " +
                    "-id3v2_version 3 " +
                    "-metadata:s:v title=${q("Album cover")} " +
                    "-metadata:s:v comment=${q("Cover (front)")}" +
                    "$meta ${q(outMp3)}",
            )
            if (reencoded.isSuccess) return reencoded
            File(outMp3).delete()
        }

        return run(
            "-y -i ${q(inputPath)} -vn -acodec libmp3lame -b:a ${bitrateKbps}k" +
                "$meta ${q(outMp3)}",
        )
    }

    private fun run(command: String): Result<Unit> {
        val session = FFmpegKit.execute(command)
        return if (ReturnCode.isSuccess(session.returnCode)) {
            Result.success(Unit)
        } else {
            val detail = session.failStackTrace
                ?: session.allLogsAsString?.takeLast(800)
                ?: "FFmpeg failed (${session.returnCode})"
            Result.failure(IllegalStateException(detail.trim().ifBlank { "FFmpeg failed" }))
        }
    }

    private fun q(path: String): String = "'${path.replace("'", "'\\''")}'"
}
