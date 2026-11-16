package com.example.ytshare

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val active = AtomicInteger(0)
    private lateinit var nm: NotificationManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Descargas", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this, 1, progressNotification("YTShare", -1),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )

        val url = intent?.getStringExtra(EXTRA_URL)
        if (url == null) {
            if (active.get() == 0) stopSelf(startId)
            return START_NOT_STICKY
        }
        val quality = runCatching { Quality.valueOf(intent.getStringExtra(EXTRA_Q) ?: "") }
            .getOrDefault(Quality.P1080)

        val jobId = counter.incrementAndGet()
        active.incrementAndGet()
        scope.launch {
            try {
                download(jobId, url, quality)
            } finally {
                if (active.decrementAndGet() == 0) stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private fun download(id: Int, url: String, q: Quality) {
        val nid = 100 + id
        val tmp = File(getExternalFilesDir(null) ?: filesDir, "tmp/$id").apply { mkdirs() }
        try {
            notifyProgress(nid, "Preparando…", -1)
            Engine.ensureInit(applicationContext)
            Engine.autoUpdateOnce(applicationContext)

            val req = YoutubeDLRequest(url).apply {
                if (q.audioOnly) {
                    addOption("-f", "ba/b")
                    addOption("-x")
                    addOption("--audio-format", "mp3")
                    addOption("--audio-quality", "0")
                } else {
                    addOption("-f", "bv*+ba/b")
                    // Prefiere la resolución pedida y, a igualdad, H.264 + AAC (compatible con la galería)
                    val res = q.height?.let { "res:$it" } ?: "res"
                    addOption("-S", "$res,vcodec:h264,acodec:m4a")
                    addOption("--merge-output-format", "mp4")
                }
                addOption("--no-playlist")
                addOption("--no-mtime")
                addOption("-o", "${tmp.absolutePath}/%(title).80s [%(id)s].%(ext)s")
            }

            var lastPct = -2
            YoutubeDL.getInstance().execute(req, "dl$id") { progress, _, _ ->
                val pct = progress.toInt()
                if (pct != lastPct) {
                    lastPct = pct
                    notifyProgress(nid, "Descargando…", pct)
                }
            }

            notifyProgress(nid, "Guardando en la galería…", -1)
            val ok = setOf("mp4", "mkv", "webm", "mp3", "m4a", "opus")
            val partial = Regex("\\.f\\d+\\.")
            val files = tmp.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in ok && !partial.containsMatchIn(it.name) }
                .toList()
            if (files.isEmpty()) error("No se generó ningún archivo")

            var saved: Uri? = null
            files.forEach { saved = MediaSaver.save(this, it) }
            notifyDone(nid, files.first().nameWithoutExtension, saved, error = false)
        } catch (e: Exception) {
            notifyDone(nid, (e.message ?: "Error desconocido").take(400), null, error = true)
        } finally {
            tmp.deleteRecursively()
        }
    }

    private fun progressNotification(title: String, pct: Int): Notification =
        NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, pct.coerceIn(0, 100), pct < 0)
            .build()

    private fun notifyProgress(nid: Int, title: String, pct: Int) {
        nm.notify(nid, progressNotification(title, pct))
    }

    private fun notifyDone(nid: Int, text: String, uri: Uri?, error: Boolean) {
        val b = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(
                if (error) android.R.drawable.stat_notify_error
                else android.R.drawable.stat_sys_download_done
            )
            .setContentTitle(if (error) "Falló la descarga" else "Descarga completa")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
        if (uri != null) {
            val view = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, contentResolver.getType(uri))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            b.setContentIntent(
                PendingIntent.getActivity(
                    this, nid, view,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        }
        nm.notify(nid, b.build())
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "downloads"
        private const val EXTRA_URL = "url"
        private const val EXTRA_Q = "quality"
        private val counter = AtomicInteger(0)

        fun start(ctx: Context, url: String, q: Quality) {
            val i = Intent(ctx, DownloadService::class.java)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_Q, q.name)
            ContextCompat.startForegroundService(ctx, i)
        }
    }
}
