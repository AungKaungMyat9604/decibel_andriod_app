package com.decibel.youtube

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.util.concurrent.TimeUnit

object ExtractorBootstrap {
    @Volatile
    private var ready = false

    fun init() {
        if (ready) return
        synchronized(this) {
            if (ready) return
            NewPipe.init(OkHttpDownloader())
            ready = true
        }
    }
}

private class OkHttpDownloader : Downloader() {
    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun execute(request: Request): Response {
        val builder = okhttp3.Request.Builder()
            .url(request.url())
            .method(
                request.httpMethod(),
                request.dataToSend()?.toRequestBody(null),
            )

        request.headers().forEach { (name, values) ->
            values.forEach { value -> builder.addHeader(name, value) }
        }
        if (request.headers()["User-Agent"].isNullOrEmpty()) {
            builder.header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
            )
        }

        val response = client.newCall(builder.build()).execute()
        if (response.code == 429) {
            response.close()
            throw ReCaptchaException("reCaptcha Challenge requested", request.url())
        }

        val body = response.body?.string().orEmpty()
        val headers = linkedMapOf<String, List<String>>()
        response.headers.forEach { (name, value) ->
            headers[name] = listOf(value)
        }
        return Response(
            response.code,
            response.message,
            headers,
            body,
            response.request.url.toString(),
        )
    }
}
