package ru.ruvideohub.app.network

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import ru.ruvideohub.app.model.Movie
import ru.ruvideohub.app.model.PlaybackOption
import ru.ruvideohub.app.model.SourceSpec
import ru.ruvideohub.app.model.VideoSearchResult
import ru.ruvideohub.app.util.HlsMasterParser
import java.net.URLEncoder

class GenericJsonSourceClient(private val prefs: SharedPreferences) {
    private val http = OkHttpClient()

    suspend fun search(spec: SourceSpec, query: String): VideoSearchResult = withContext(Dispatchers.IO) {
        var endpoint = prefs.getString(spec.endpointKey, "").orEmpty().trim()
        if (endpoint.isBlank()) return@withContext VideoSearchResult(emptyList(), "${spec.title}: API/JSON не настроен")
        endpoint = endpoint
            .replace("{query}", URLEncoder.encode(query, "UTF-8"))
            .replace("{rawQuery}", query)
        spec.mirrorKey?.let { key ->
            val mirror = prefs.getString(key, "").orEmpty().trim()
            if (mirror.isNotBlank()) endpoint = endpoint.replace("{mirror}", mirror.trimEnd('/'))
        }
        val builder = Request.Builder().url(endpoint).header("User-Agent", "RuVideoHub/13 Android").header("Accept", "application/json")
        spec.tokenKey?.let { k -> prefs.getString(k, "").orEmpty().trim().takeIf { it.isNotBlank() }?.let { token -> builder.header("Authorization", "Bearer $token") } }
        val response = runCatching { http.newCall(builder.build()).execute() }.getOrNull()
            ?: return@withContext VideoSearchResult(emptyList(), "${spec.title}: нет соединения")
        val parsed = response.use {
            if (!it.isSuccessful) {
                return@withContext VideoSearchResult(emptyList(), "${spec.title}: сервер ответил ${it.code}")
            }
            val raw = it.body?.string() ?: return@withContext VideoSearchResult(emptyList(), "${spec.title}: пустой ответ")
            runCatching { parse(raw, spec.title) }.getOrElse { VideoSearchResult(emptyList(), "${spec.title}: неверный формат JSON") }
        }
        // Если источник отдал единственную ссылку на HLS-мастер (".m3u8"), разворачиваем
        // её в реальный список качеств вместо одной кнопки "Авто" — так же, как для RUTUBE.
        val expanded = parsed.movies.map { movie -> expandHlsOptions(movie, spec.title) }
        VideoSearchResult(expanded, parsed.error)
    }

    private fun expandHlsOptions(movie: Movie, source: String): Movie {
        val single = movie.options.singleOrNull() ?: return movie
        if (!single.url.contains(".m3u8", ignoreCase = true)) return movie
        val playlist = runCatching {
            val req = Request.Builder().url(single.url).header("User-Agent", "RuVideoHub/13 Android").build()
            http.newCall(req).execute().use { r -> if (r.isSuccessful) r.body?.string() else null }
        }.getOrNull() ?: return movie
        val variants = runCatching { HlsMasterParser.parse(playlist, single.url) }.getOrElse { emptyList() }
        if (variants.isEmpty()) return movie
        return movie.copy(options = HlsMasterParser.toPlaybackOptions(variants, source))
    }

    private fun parse(raw: String, sourceName: String): VideoSearchResult {
        val root = JSONObject(raw)
        val arr = sequenceOf(
            root.optJSONArray("results"), root.optJSONArray("items"), root.optJSONArray("data"), root.optJSONArray("videos"), root.optJSONArray("films"),
            root.optJSONObject("data")?.optJSONArray("results"), root.optJSONObject("data")?.optJSONArray("items"), root.optJSONObject("data")?.optJSONArray("videos")
        ).firstOrNull { it != null } ?: JSONArray()
        val list = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = first(o, "id", "video_id", "videoId", "kinopoiskId", "filmId", "url") ?: continue
                val title = first(o, "title", "name", "nameRu", "nameEn") ?: continue
                val play = first(o, "playUrl", "stream", "url", "videoUrl", "m3u8")
                val mime = play?.let { if (it.contains(".m3u8", ignoreCase = true)) "application/vnd.apple.mpegurl" else null }
                add(
                    Movie(
                        id = "$sourceName:$id",
                        title = title,
                        originalTitle = first(o, "originalTitle", "nameOriginal", "nameEn").orEmpty(),
                        year = first(o, "year", "releaseYear")?.toIntOrNull(),
                        poster = first(o, "poster", "posterUrl", "poster_url", "thumbnail", "thumbnailUrl", "picture"),
                        backdrop = first(o, "backdrop", "backdropUrl", "coverUrl", "cover"),
                        description = first(o, "description", "desc", "overview").orEmpty(),
                        rating = first(o, "rating", "ratingKinopoisk", "imdbRating")?.replace(',', '.')?.toDoubleOrNull(),
                        genres = o.optJSONArray("genres")?.let { a -> buildList { for (j in 0 until a.length()) add(a.optJSONObject(j)?.optString("name").orEmpty().ifBlank { a.optString(j) }) }.filter { it.isNotBlank() } } ?: emptyList(),
                        source = sourceName,
                        author = first(o, "author", "owner_name").orEmpty(),
                        views = first(o, "views", "viewCount")?.toLongOrNull(),
                        duration = first(o, "duration", "durationText"),
                        options = play?.let { listOf(PlaybackOption(sourceName, "Авто", it, mimeType = mime)) } ?: emptyList()
                    )
                )
            }
        }
        return VideoSearchResult(list)
    }

    private fun first(o: JSONObject, vararg keys: String): String? = keys.firstNotNullOfOrNull { k -> o.opt(k)?.toString()?.takeIf { it.isNotBlank() && it != "null" } }
}
