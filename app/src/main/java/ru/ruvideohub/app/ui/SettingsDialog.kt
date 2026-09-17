package ru.ruvideohub.app.ui

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ruvideohub.app.model.ALL_SOURCES

@Composable
fun SettingsDialog(prefs: SharedPreferences, onClose: () -> Unit) {
    val fields = remember {
        mutableStateMapOf<String, String>().apply {
            ALL_SOURCES.forEach { spec ->
                this[spec.endpointKey] = prefs.getString(spec.endpointKey, "").orEmpty()
                spec.tokenKey?.let { this[it] = prefs.getString(it, "").orEmpty() }
                spec.mirrorKey?.let { this[it] = prefs.getString(it, "").orEmpty() }
            }
            this["kp_key"] = prefs.getString("kp_key", "").orEmpty()
        }
    }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Источники и API") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 560.dp)) {
                item {
                    Text(
                        "RUTUBE работает без настройки. Остальные источники подключаются через JSON/API-адрес. " +
                            "Все значения на этом экране хранятся в зашифрованном виде на устройстве.",
                        fontSize = 12.sp, color = Color.Gray
                    )
                }
                item { OutlinedTextField(fields["kp_key"].orEmpty(), { fields["kp_key"] = it }, singleLine = true, label = { Text("Kinopoisk API key") }, modifier = Modifier.fillMaxWidth()) }
                item { Text("VK", fontWeight = FontWeight.Bold) }
                item {
                    Text(
                        "VK Video ищется через официальный метод video.search — нужен только ваш access_token " +
                            "(получите его через VK ID / Implicit Flow, право доступа: video).",
                        fontSize = 11.sp, color = Color.Gray
                    )
                }
                item { OutlinedTextField(fields["vk_token"].orEmpty(), { fields["vk_token"] = it }, singleLine = true, label = { Text("VK access_token") }, modifier = Modifier.fillMaxWidth()) }
                ALL_SOURCES.filter { it.key != "rutube" && it.key != "vk" }.forEach { spec ->
                    item { Text(spec.title, fontWeight = FontWeight.Bold) }
                    item { OutlinedTextField(fields[spec.endpointKey].orEmpty(), { fields[spec.endpointKey] = it }, singleLine = true, label = { Text("JSON/API URL с {query}") }, modifier = Modifier.fillMaxWidth()) }
                    spec.tokenKey?.let { key -> item { OutlinedTextField(fields[key].orEmpty(), { fields[key] = it }, singleLine = true, label = { Text("API token (необязательно)") }, modifier = Modifier.fillMaxWidth()) } }
                    spec.mirrorKey?.let { key -> item { OutlinedTextField(fields[key].orEmpty(), { fields[key] = it }, singleLine = true, label = { Text("Зеркало, для {mirror}") }, modifier = Modifier.fillMaxWidth()) } }
                }
                item {
                    Text(
                        "Формат элемента JSON: id, title/name, poster/posterUrl, description, year, rating, playUrl/stream/url. " +
                            "Дополнительно можно передавать quality/audio/subtitles в отдельном варианте воспроизведения.",
                        fontSize = 11.sp, color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val edit = prefs.edit()
                fields.forEach { (k, v) -> edit.putString(k, v.trim()) }
                edit.apply(); onClose()
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Закрыть") } }
    )
}
