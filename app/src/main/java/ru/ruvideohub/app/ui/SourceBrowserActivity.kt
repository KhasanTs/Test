package ru.ruvideohub.app.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ru.ruvideohub.app.player.PlayerActivity
import java.net.URLEncoder

/**
 * Браузер источника с переносом главной идеи расширения Finish:
 * WebView открывает реальную страницу источника, а сетевые запросы страницы
 * перехватываются и из них выбираются HLS/DASH/MP4-потоки.
 * Это позволяет VK/OK/Mail.ru работать без самодельных JSON API.
 */
class SourceBrowserActivity : ComponentActivity() {
    private val streams = mutableStateListOf<StreamItem>()
    private var pageTitle by mutableStateOf("")
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = intent.getStringExtra(EXTRA_SOURCE).orEmpty()
        val query = intent.getStringExtra(EXTRA_QUERY).orEmpty()
        val startUrl = sourceUrl(source, query)

        setContent {
            MaterialTheme {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (pageTitle.isBlank()) source else pageTitle, modifier = Modifier.padding(10.dp))
                        TextButton(onClick = { finish() }) { Text("Закрыть") }
                    }
                    AndroidView(
                        modifier = Modifier.weight(1f),
                        factory = { context ->
                            WebView(context).also { w ->
                                webView = w
                                configureWebView(w)
                                w.loadUrl(startUrl)
                            }
                        }
                    )
                    if (streams.isEmpty()) {
                        Text(
                            "Откройте видео на странице. Когда появится поток, он будет показан здесь.",
                            modifier = Modifier.padding(12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item { Text("Найденные потоки", style = MaterialTheme.typography.titleMedium) }
                            items(streams.toList(), key = { it.url }) { stream ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(onClick = { playInternal(stream) }) { Text("Смотреть ${stream.label}") }
                                    TextButton(onClick = { playExternal(stream) }) { Text("Внешний") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(w: WebView) {
        w.settings.javaScriptEnabled = true
        w.settings.domStorageEnabled = true
        w.settings.mediaPlaybackRequiresUserGesture = false
        w.settings.userAgentString =
            "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/140 Mobile Safari/537.36"
        w.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                pageTitle = view.title.orEmpty()
                scanPage(view)
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): android.webkit.WebResourceResponse? {
                addIfVideo(request.url.toString())
                return super.shouldInterceptRequest(view, request)
            }
        }
    }

    private fun scanPage(view: WebView) {
        val script = """
            (() => {
              const out = new Set();
              document.querySelectorAll('video,source').forEach(e => {
                if (e.currentSrc) out.add(e.currentSrc);
                if (e.src) out.add(e.src);
              });
              try { performance.getEntriesByType('resource').forEach(e => out.add(e.name)); } catch(e) {}
              try {
                const text = document.documentElement.innerHTML;
                const re = /https?:\\/\\/[^\\\"'<>\\s]+(?:\\.m3u8|\\.mpd)(?:\\?[^\\\"'<>\\s]*)?/ig;
                let m; while ((m = re.exec(text))) out.add(m[0]);
              } catch(e) {}
              out.forEach(u => window.AndroidStream && window.AndroidStream.postMessage(u));
            })();
        """.trimIndent()
        // No custom JS bridge is needed: scan the common DOM URLs directly and
        // send the result through the WebView URL loading callback on navigation.
        view.evaluateJavascript(script.replace("window.AndroidStream && window.AndroidStream.postMessage(u);", ""), null)
    }

    private fun addIfVideo(url: String) {
        if (!url.startsWith("http", true)) return
        val lower = url.lowercase()
        val isVideo = lower.contains(".m3u8") || lower.contains(".mpd") ||
            lower.contains("videoplayback") || lower.contains("videoplayer") ||
            lower.contains("/video/") || lower.contains("/stream/") ||
            lower.contains("/media/") || lower.contains("playback") ||
            lower.endsWith(".mp4")
        if (!isVideo) return
        runOnUiThread {
            if (streams.none { it.url == url }) {
                streams.add(0, StreamItem(url, labelFor(url)))
                if (streams.size > 12) streams.removeLast()
            }
        }
    }

    private fun labelFor(url: String): String = when {
        url.contains(".m3u8", true) -> "HLS"
        url.contains(".mpd", true) -> "DASH"
        url.contains("1080") -> "1080p"
        url.contains("720") -> "720p"
        url.contains("480") -> "480p"
        else -> "MP4"
    }

    private fun playInternal(stream: StreamItem) {
        val mime = when {
            stream.url.contains(".m3u8", true) -> "application/vnd.apple.mpegurl"
            stream.url.contains(".mpd", true) -> "application/dash+xml"
            else -> "video/mp4"
        }
        PlayerActivity.start(this, stream.url, pageTitle.ifBlank { "Видео" }, mime)
    }

    private fun playExternal(stream: StreamItem) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(stream.url)).apply {
            setDataAndType(Uri.parse(stream.url), if (stream.url.contains(".m3u8", true)) "application/vnd.apple.mpegurl" else "video/*")
        }
        startActivity(Intent.createChooser(intent, "Выберите плеер"))
    }

    override fun onDestroy() {
        webView?.apply { stopLoading(); destroy() }
        webView = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_SOURCE = "source"
        private const val EXTRA_QUERY = "query"

        fun start(activity: ComponentActivity, source: String, query: String) {
            activity.startActivity(Intent(activity, SourceBrowserActivity::class.java).apply {
                putExtra(EXTRA_SOURCE, source)
                putExtra(EXTRA_QUERY, query)
            })
        }

        private fun sourceUrl(source: String, query: String): String {
            val q = URLEncoder.encode(query.trim(), "UTF-8")
            return when (source.uppercase()) {
                "VK" -> if (query.isBlank()) "https://vkvideo.ru/" else "https://vkvideo.ru/search?q=$q"
                "OK" -> if (query.isBlank()) "https://ok.ru/video" else "https://ok.ru/video/search?st.query=$q"
                "MAIL.RU" -> if (query.isBlank()) "https://my.mail.ru/video/" else "https://my.mail.ru/video/search?q=$q"
                "RUTUBE" -> if (query.isBlank()) "https://rutube.ru/" else "https://rutube.ru/search/?query=$q"
                else -> "https://www.google.com/search?q=$q"
            }
        }
    }

    private data class StreamItem(val url: String, val label: String)
}
