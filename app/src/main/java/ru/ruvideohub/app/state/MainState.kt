package ru.ruvideohub.app.state

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import ru.ruvideohub.app.model.ALL_SOURCES
import ru.ruvideohub.app.model.Movie
import ru.ruvideohub.app.model.PlaybackOption
import ru.ruvideohub.app.network.GenericJsonSourceClient
import ru.ruvideohub.app.network.KinopoiskClient
import ru.ruvideohub.app.network.RutubeClient
import ru.ruvideohub.app.network.VkClient
import ru.ruvideohub.app.util.normalize
import ru.ruvideohub.app.util.relevance

class MainState(private val prefs: SharedPreferences) {
    private val rutube = RutubeClient()
    private val kp = KinopoiskClient(prefs)
    private val vk = VkClient(prefs)
    private val generic = GenericJsonSourceClient(prefs)

    var query by mutableStateOf("")
    var results by mutableStateOf<List<Movie>>(emptyList())
    var selected by mutableStateOf<Movie?>(null)
    var options by mutableStateOf<List<PlaybackOption>>(emptyList())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    /** Ошибки по отдельным источникам за последний поиск (источник -> текст ошибки). */
    var sourceErrors by mutableStateOf<Map<String, String>>(emptyMap())

    var favorites by mutableStateOf(loadIds("favorites"))
    var history by mutableStateOf(loadIds("history"))

    private fun loadIds(key: String): Set<String> = prefs.getStringSet(key, emptySet()).orEmpty()
    private fun saveIds(key: String, value: Set<String>) = prefs.edit().putStringSet(key, value).apply()

    suspend fun search() {
        val q = query.trim()
        if (q.isBlank()) return
        loading = true
        error = null
        sourceErrors = emptyMap()
        try {
            val errors = mutableMapOf<String, String>()
            val all = coroutineScope {
                val rDeferred = async { rutube.search(q) }
                val kpDeferred = async { kp.search(q).take(12) }
                val vkDeferred = async { vk.search(q) }
                // VK обслуживается отдельным VkClient (официальный video.search + access_token),
                // поэтому из общего JSON-адаптера его исключаем, чтобы не дублировать источник.
                val sourceDeferreds = ALL_SOURCES.filter { it.key != "rutube" && it.key != "vk" }.associateWith { spec ->
                    async { generic.search(spec, q) }
                }
                val lists = mutableListOf<Movie>()

                val rResult = rDeferred.await()
                rResult.error?.let { errors["RUTUBE"] = it }
                lists += rResult.movies

                lists += kpDeferred.await()

                val vkResult = vkDeferred.await()
                vkResult.error?.let { errors["VK"] = it }
                lists += vkResult.movies

                sourceDeferreds.forEach { (spec, deferred) ->
                    val r = deferred.await()
                    r.error?.let { errors[spec.title] = it }
                    lists += r.movies.take(20)
                }
                lists
            }
            sourceErrors = errors
            val combined = all
                .groupBy { normalize(it.title) }
                .map { (_, same) ->
                    val first = same.maxByOrNull { relevance(q, it.title) }!!
                    val kpMovie = same.firstOrNull { it.kpId != null }
                    first.copy(kpId = first.kpId ?: kpMovie?.kpId)
                }
                .sortedByDescending { relevance(q, it.title) }
            results = combined
            if (combined.isEmpty()) {
                error = errors.values.firstOrNull() ?: "Ничего не найдено"
            }
        } finally {
            loading = false
        }
    }

    suspend fun open(movie: Movie) {
        loading = true
        error = null
        val metadata = movie.kpId?.let { kp.details(it) }
        val playback = when {
            movie.id.startsWith("rutube:") -> rutube.playback(movie)
            else -> movie.options
        }
        selected = (metadata ?: movie).copy(
            options = playback,
            source = movie.source.ifBlank { metadata?.source.orEmpty() }.ifBlank { "RUTUBE" }
        )
        options = playback
        history = history.toMutableSet().apply { add(movie.id) }
        saveIds("history", history)
        loading = false
    }

    fun toggleFavorite() {
        val m = selected ?: return
        favorites = favorites.toMutableSet().apply { if (!add(m.id)) remove(m.id) }
        saveIds("favorites", favorites)
    }
}
