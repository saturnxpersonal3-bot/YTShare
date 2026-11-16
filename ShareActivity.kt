package com.example.ytshare

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val url = Regex("https?://\\S+").find(text)?.value
        if (url == null) {
            Toast.makeText(this, "No se encontró ningún enlace", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            MaterialTheme {
                var q by remember { mutableStateOf(lastQuality()) }
                AlertDialog(
                    onDismissRequest = { finish() },
                    title = { Text("Descargar video") },
                    text = {
                        Column {
                            Text(url, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(12.dp))
                            QualityPicker(q) { q = it }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            saveQuality(q)
                            DownloadService.start(this@ShareActivity, url, q)
                            Toast.makeText(this@ShareActivity, "Descargando…", Toast.LENGTH_SHORT).show()
                            finish()
                        }) { Text("Descargar") }
                    },
                    dismissButton = { TextButton(onClick = { finish() }) { Text("Cancelar") } }
                )
            }
        }
    }
}
