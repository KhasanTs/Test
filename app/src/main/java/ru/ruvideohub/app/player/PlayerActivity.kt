package ru.ruvideohub.app.player

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class PlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MIME = "extra_mime"

        fun start(
            context: Context,
            url: String,
            title: String,
            mimeType: String?
        ) {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
                mimeType?.let { putExtra(EXTRA_MIME, it) }
            }
            context.startActivity(intent)
        }
    }

    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val url = intent.getStringExtra(EXTRA_URL)

        if (url.isNullOrBlank()) {
            finish()
            return
        }

        val mimeType = intent.getStringExtra(EXTRA_MIME)
        val title = intent.getStringExtra(EXTRA_TITLE)

        if (!title.isNullOrBlank()) {
            this.title = title
        }

        val playerView = PlayerView(this).apply {
            useController = true
            setShowNextButton(false)
            setShowPreviousButton(false)
        }

        setContentView(playerView)

        val player = ExoPlayer.Builder(this).build()
        exoPlayer = player

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(url)

        val resolvedMime = mimeType ?: guessMimeType(url)

        if (resolvedMime != null) {
            mediaItemBuilder.setMimeType(resolvedMime)
        }

        player.setMediaItem(mediaItemBuilder.build())
        playerView.player = player

        player.playWhenReady = true
        player.prepare()
    }

    override fun onDestroy() {
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }

    private fun guessMimeType(url: String): String? {
        return when {
            url.contains(".m3u8", ignoreCase = true) ->
                MimeTypes.APPLICATION_M3U8

            url.contains(".mpd", ignoreCase = true) ->
                MimeTypes.APPLICATION_MPD

            else ->
                null
        }
    }
}
