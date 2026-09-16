package com.decibel.ui

import android.net.Uri
import java.io.File

/** Coil / Media3 model for remote URLs or absolute local thumb paths. */
fun coilImageModel(pathOrUrl: String?): Any? = when {
    pathOrUrl.isNullOrBlank() -> null
    pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://") -> pathOrUrl
    pathOrUrl.startsWith("file:") || pathOrUrl.startsWith("content:") -> pathOrUrl
    else -> File(pathOrUrl).takeIf { it.exists() }
}

fun artworkUri(pathOrUrl: String?): Uri? = when {
    pathOrUrl.isNullOrBlank() -> null
    pathOrUrl.startsWith("http://") ||
        pathOrUrl.startsWith("https://") ||
        pathOrUrl.startsWith("file:") ||
        pathOrUrl.startsWith("content:") -> Uri.parse(pathOrUrl)
    else -> File(pathOrUrl).takeIf { it.exists() }?.let { Uri.fromFile(it) }
}
