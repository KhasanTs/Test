package ru.ruvideohub.app.ui

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import ru.ruvideohub.app.model.PlaybackOption
import ru.ruvideohub.app.player.PlayerActivity
import ru.ruvideohub.app.state.MainState

@Composable
fun App(prefs: SharedPreferences, activity: ComponentActivity) {
    var mode by remember { mutableStateOf(prefs.getString("mode", null)) }
    var settingsOpen by remember { mutableStateOf(false) }
    val state = remember { MainState(prefs) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(mode) {
        when (mode) {
            "tv" -> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            "phone" -> activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFFD00000),
            secondary = Color(0xFF555B66),
            background = Color(0xFFF7F7F8),
            surface = Color.White,
            surfaceVariant = Color(0xFFEDEEF1)
        )
    ) {
        when {
            mode == null -> ModePicker { mode = it; prefs.edit().putString("mode", it).apply() }
            state.selected != null -> DetailsScreen(
                movie = state.selected!!,
                tv = mode == "tv",
                favorite = state.favorites.contains(state.selected!!.id),
                loading = state.loading,
                onBack = { state.selected = null },
                onFavorite = state::toggleFavorite,
                onPlayInternal = { option -> playInternal(context, state.selected!!.title, option) },
                onPlayExternal = { option -> playExternal(context, option) }
            )
            else -> HomeScreen(
                tv = mode == "tv", state = state,
                onOpen = { scope.launch { state.open(it) } },
                onSearch = { scope.launch { state.search() } },
                onSettings = { settingsOpen = true },
                onSource = { source -> SourceBrowserActivity.start(activity, source, state.query) }
            )
        }
        if (settingsOpen) SettingsDialog(prefs) { settingsOpen = false }
    }
}

/**
 * Если у выбранного качества видео и звук лежат в разных HLS-плейлистах
 * (RUTUBE/OK/Mail.ru), проигрывать нужно не конкретный вариант, а master-плейлист —
 * иначе будет картинка без звука. ExoPlayer сам выберет из него подходящую дорожку.
 */
private fun effectiveUrl(option: PlaybackOption): String =
    if (option.hasSeparateAudio) option.masterUrl ?: option.url else option.url

private fun playInternal(context: android.content.Context, title: String, option: PlaybackOption) {
    PlayerActivity.start(context, effectiveUrl(option), title, option.mimeType)
}

private fun playExternal(context: android.content.Context, option: PlaybackOption) {
    val url = effectiveUrl(option)
    val view = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        setDataAndType(Uri.parse(url), option.mimeType ?: "video/*")
    }
    context.startActivity(Intent.createChooser(view, "Выберите плеер"))
}
