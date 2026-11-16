package com.example.ytshare

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) { Home() }
            }
        }
    }
}

@Composable
private fun Home() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var q by remember { mutableStateOf(ctx.lastQuality()) }
    var status by remember { mutableStateOf("") }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("YTShare", style = MaterialTheme.typography.headlineMedium)
        Text("En YouTube o X pulsa Compartir y elige YTShare. También puedes pegar un enlace aquí.")

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text("Enlace") },
            modifier = Modifier.fillMaxWidth()
        )

        QualityPicker(q) { q = it }

        Button(
            enabled = url.trim().startsWith("http"),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                ctx.saveQuality(q)
                DownloadService.start(ctx, url.trim(), q)
                status = "Descarga iniciada. Mira la notificación."
                url = ""
            }
        ) { Text("Descargar") }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                status = "Actualizando yt-dlp…"
                scope.launch {
                    status = withContext(Dispatchers.IO) {
                        runCatching { "yt-dlp: " + Engine.update(ctx.applicationContext) }
                            .getOrElse { "Error: ${it.message}" }
                    }
                }
            }
        ) { Text("Actualizar yt-dlp") }

        if (status.isNotBlank()) Text(status)
        Text("Los archivos se guardan en Movies/YTShare (o Music/YTShare para audio).")
    }
}
