package ru.ruvideohub.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ruvideohub.app.model.Movie
import ru.ruvideohub.app.state.MainState
import ru.ruvideohub.app.ui.components.Catalogs
import ru.ruvideohub.app.ui.components.Hero
import ru.ruvideohub.app.ui.components.MovieCard
import ru.ruvideohub.app.ui.components.SearchBox
import ru.ruvideohub.app.ui.components.SourceGrid

@Composable
fun HomeScreen(tv: Boolean, state: MainState, onOpen: (Movie) -> Unit, onSearch: () -> Unit, onSettings: () -> Unit, onSource: (String) -> Unit) {
    val pad = if (tv) 44.dp else 16.dp
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = pad, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("RU VIDEO HUB", fontSize = if (tv) 27.sp else 22.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(22.dp))
            SearchBox(state.query, { state.query = it }, onSearch, tv)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Настройки") }
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = pad), contentPadding = PaddingValues(bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
            item { Hero(tv) }
            item { Catalogs(tv) }
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { msg -> item { Text(msg, color = MaterialTheme.colorScheme.error) } }
            if (state.sourceErrors.isNotEmpty()) item {
                Text(
                    "Не ответили: " + state.sourceErrors.entries.joinToString(", ") { "${it.key} (${it.value})" },
                    color = Color.Gray, fontSize = 12.sp
                )
            }
            if (state.results.isNotEmpty()) item {
                Text("Результаты", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(state.results, key = { it.id }) { MovieCard(it, tv, onOpen) }
                }
            }
            item { Text("Источники", fontSize = 23.sp, fontWeight = FontWeight.Bold) }
            item { SourceGrid(tv, onSource) }
        }
    }
}
