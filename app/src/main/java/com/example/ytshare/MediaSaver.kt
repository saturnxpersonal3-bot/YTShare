package com.example.ytshare

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File

object MediaSaver {
    private val audioExt = setOf("mp3", "m4a", "opus", "aac", "ogg", "wav", "flac")

    /** Copia el archivo a Movies/YTShare o Music/YTShare para que salga en la galería. */
    fun save(ctx: Context, file: File): Uri? {
        val ext = file.extension.lowercase()
        val isAudio = ext in audioExt
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: if (isAudio) "audio/mpeg" else "video/mp4"

        val folder = if (isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES
        val collection = if (isAudio)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "$folder/YTShare")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val resolver = ctx.contentResolver
        val uri = resolver.insert(collection, values) ?: return null
        resolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }
}
