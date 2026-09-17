package ru.ruvideohub.app.model

data class Movie(
    val id: String,
    val title: String,
    val originalTitle: String = "",
    val year: Int? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val description: String = "",
    val rating: Double? = null,
    val genres: List<String> = emptyList(),
    val kpId: Int? = null,
    val source: String = "",
    val author: String = "",
    val views: Long? = null,
    val duration: String? = null,
    val options: List<PlaybackOption> = emptyList()
)

data class PlaybackOption(
    val source: String,
    val label: String,
    val url: String,
    val quality: String? = null,
    val audio: String? = null,
    val subtitles: String? = null,
    /** MIME-тип потока, нужен плееру (например HLS). Null = плеер определит сам. */
    val mimeType: String? = null,
    /**
     * Для HLS-мастеров, у которых видео и звук лежат в РАЗНЫХ плейлистах
     * (так отдают RUTUBE/OK/Mail.ru): если true — нужно проигрывать не отдельный
     * вариант качества, а именно master-плейлист [masterUrl], иначе будет видео без звука.
     */
    val hasSeparateAudio: Boolean = false,
    val masterUrl: String? = null
)

data class VideoSearchResult(
    val movies: List<Movie>,
    val error: String? = null
)

data class SourceSpec(
    val key: String,
    val title: String,
    val endpointKey: String,
    val tokenKey: String? = null,
    val mirrorKey: String? = null
)

val ALL_SOURCES = listOf(
    SourceSpec("rutube", "RUTUBE", "rutube_endpoint"),
    SourceSpec("vk", "VK", "vk_endpoint", "vk_token"),
    SourceSpec("ok", "OK", "ok_endpoint", "ok_token"),
    SourceSpec("mail", "MAIL", "mail_endpoint", "mail_token"),
    SourceSpec("seena", "SEENA", "seena_endpoint", mirrorKey = "seena_mirror"),
    SourceSpec("zona", "ZONA", "zona_endpoint", mirrorKey = "zona_mirror"),
    SourceSpec("hdrezka", "HDREZKA", "hdrezka_endpoint", mirrorKey = "hdrezka_mirror"),
    SourceSpec("filmix", "FILMIX", "filmix_endpoint", mirrorKey = "filmix_mirror")
)
