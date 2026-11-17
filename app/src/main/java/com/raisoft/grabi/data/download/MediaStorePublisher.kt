package com.raisoft.grabi.data.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.raisoft.grabi.domain.model.MediaKind
import java.io.File

object MediaStorePublisher {

    fun publish(context: Context, file: File, kind: MediaKind): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val resolver = context.contentResolver
        val collection = when (kind) {
            MediaKind.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            MediaKind.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            MediaKind.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val directory = when (kind) {
            MediaKind.VIDEO -> Environment.DIRECTORY_MOVIES
            MediaKind.AUDIO -> Environment.DIRECTORY_MUSIC
            MediaKind.IMAGE -> Environment.DIRECTORY_PICTURES
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeFor(file.extension, kind))
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$directory/Grabi")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: return null
        val copied = runCatching {
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                ?: error("no output stream")
        }.isSuccess
        if (!copied) {
            resolver.delete(uri, null, null)
            return null
        }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        file.delete()
        return uri
    }

    fun mimeFor(ext: String, kind: MediaKind): String = when (kind) {
        MediaKind.VIDEO -> "video/mp4"
        MediaKind.IMAGE -> when (ext) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "avif" -> "image/avif"
            else -> "image/jpeg"
        }
        MediaKind.AUDIO -> when (ext) {
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "opus", "ogg" -> "audio/ogg"
            "flac" -> "audio/flac"
            "wav" -> "audio/wav"
            else -> "audio/mpeg"
        }
    }
}