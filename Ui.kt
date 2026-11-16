package com.example.ytshare

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

enum class Quality(val label: String, val height: Int?, val audioOnly: Boolean = false) {
    BEST("Máxima calidad disponible", null),
    P1080("1080p (Full HD)", 1080),
    P720("720p (HD)", 720),
    P480("480p", 480),
    AUDIO("Solo audio (MP3)", null, true)
}

fun Context.lastQuality(): Quality =
    runCatching {
        Quality.valueOf(getSharedPreferences("prefs", 0).getString("q", null) ?: "")
    }.getOrDefault(Quality.P1080)

fun Context.saveQuality(q: Quality) {
    getSharedPreferences("prefs", 0).edit().putString("q", q.name).apply()
}

@Composable
fun QualityPicker(selected: Quality, onSelect: (Quality) -> Unit) {
    Column {
        Quality.entries.forEach { q ->
            Row(
                Modifier.fillMaxWidth().clickable { onSelect(q) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = q == selected, onClick = { onSelect(q) })
                Text(q.label)
            }
        }
    }
}
