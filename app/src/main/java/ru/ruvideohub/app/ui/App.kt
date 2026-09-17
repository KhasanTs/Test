package ru.ruvideohub.app.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import ru.ruvideohub.app.model.PlaybackOption
import ru.ruvideohub.app.state.MainState

@Composable
fun App(
    prefs: SharedPreferences,
    activity: ComponentActivity
) {
    val state = remember { MainState(prefs) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        activity.requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFFE50914),
            secondary = Color(0xFF5F6368),
            background = Color(0xFFF7F7F8),
            surface = Color.White,
            onBackground = Color(0xFF151515),
            onSurface = Color(0xFF151515)
        )
    ) {
        if (state.selected != null) {
            DetailsScreen(
                movie = state.selected!!,
                tv = true,
                favorite = state.favorites.contains(state.selected!!.id),
                loading = state.loading,
                onBack = {
                    state.selected = null
                },
                onFavorite = {
                    state.toggleFavorite()
                },
                onPlayExternal = { option ->
                    playExternal(activity, option)
                }
            )
        } else {
            HomeScreen(
                tv = true,
                state = state,
                onOpen = { movie ->
                    scope.launch {
                        state.open(movie)
                    }
                },
                onSearch = {
                    scope.launch {
                        state.search()
                    }
                },
                onSource = { source ->
                    state.query = state.query.trim()

                    if (state.query.isNotBlank()) {
                        scope.launch {
                            state.searchSource(source)
                        }
                    }
                },
                onSettings = {
                    state.settingsOpen = true
                }
            )
        }

        if (state.settingsOpen) {
            SettingsDialog(prefs) {
                state.settingsOpen = false
            }
        }
    }
}

/*
 * Внешний плеер.
 *
 * Никакого встроенного Media3-плеера здесь нет.
 * Android показывает список установленных приложений,
 * которые могут открыть данный поток.
 */
private fun playExternal(
    context: Context,
    option: PlaybackOption
) {
    val url =
        if (option.hasSeparateAudio) {
            option.masterUrl ?: option.url
        } else {
            option.url
        }

    if (url.isBlank()) {
        return
    }

    val uri = Uri.parse(url)

    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = uri
        type = option.mimeType ?: "video/*"
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val chooser = Intent.createChooser(
        intent,
        "Выберите внешний плеер"
    )

    context.startActivity(chooser)
}
