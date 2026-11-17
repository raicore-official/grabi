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
        val isVideo = kind == MediaKind.VIDEO
        val collection = if (isVideo) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeFor(file.extension, isVideo))
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                (if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC) + "/Grabi",
            )
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

    private fun mimeFor(ext: String, isVideo: Boolean): String = when {
        isVideo -> "video/mp4"
        ext == "mp3" -> "audio/mpeg"
        ext == "m4a" -> "audio/mp4"
        ext == "opus" || ext == "ogg" -> "audio/ogg"
        ext == "flac" -> "audio/flac"
        ext == "wav" -> "audio/wav"
        else -> "audio/mpeg"
    }
}