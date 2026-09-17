package ru.ruvideohub.app.player

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Отдельная Activity для встроенного проигрывания видео (RUTUBE HLS и другие источники),
 * вместо передачи URL во внешний плеер через ACTION_VIEW.
 *
 * Запускается через [start]. Если пользователь предпочитает внешний плеер —
 * этот выбор остаётся в DetailsScreen отдельной кнопкой.
 */
class PlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MIME = "extra_mime"

        fun start(context: Context, url: String, title: String, mimeType: String?) {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
                mimeType?.let { putExtra(EXTRA_MIME, it) }
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val url = intent.getStringExtra(EXTRA_URL)
        val mime = intent.getStringExtra(EXTRA_MIME)

        setContent {
            MaterialTheme {
                if (url != null) {
                    VideoPlayerScreen(url = url, mimeType = mime, onFinished = { finish() })
                } else {
                    finish()
                }
            }
        }
    }
}

@Composable
private fun VideoPlayerScreen(url: String, mimeType: String?, onFinished: () -> Unit) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItemBuilder = MediaItem.Builder().setUri(url)
            val resolvedMime = mimeType ?: guessMimeType(url)
            resolvedMime?.let { mediaItemBuilder.setMimeType(it) }
            setMediaItem(mediaItemBuilder.build())
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
                setShowNextButton(false)
                setShowPreviousButton(false)
            }
        }
    )
}

private fun guessMimeType(url: String): String? = when {
    url.contains(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
    url.contains(".mpd", ignoreCase = true) -> MimeTypes.APPLICATION_MPD
    else -> null
}
