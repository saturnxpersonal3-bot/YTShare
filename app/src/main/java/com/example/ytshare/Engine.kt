package com.example.ytshare

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

object Engine {
    private var ready = false
    private var autoUpdated = false

    @Synchronized
    fun ensureInit(ctx: Context) {
        if (ready) return
        YoutubeDL.getInstance().init(ctx)
        FFmpeg.getInstance().init(ctx)
        ready = true
    }

    /** Actualiza yt-dlp una vez por ejecución del proceso (si falla, se ignora). */
    @Synchronized
    fun autoUpdateOnce(ctx: Context) {
        if (autoUpdated) return
        autoUpdated = true
        runCatching { YoutubeDL.getInstance().updateYoutubeDL(ctx) }
    }

    /** Actualización manual (botón de la app). */
    fun update(ctx: Context): String {
        ensureInit(ctx)
        return YoutubeDL.getInstance().updateYoutubeDL(ctx)?.toString() ?: "Sin cambios"
    }
}
