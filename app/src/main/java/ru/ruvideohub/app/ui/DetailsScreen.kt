package ru.ruvideohub.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import ru.ruvideohub.app.model.Movie
import ru.ruvideohub.app.model.PlaybackOption
import ru.ruvideohub.app.ui.components.SelectorBlock

@Composable
fun DetailsScreen(
    movie: Movie,
    tv: Boolean,
    favorite: Boolean,
    loading: Boolean,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    onPlayExternal: (PlaybackOption) -> Unit
) {
    val quality =
        movie.options
            .mapNotNull { it.quality }
            .distinct()

    val audio =
        movie.options
            .mapNotNull { it.audio }
            .distinct()

    val subs =
        movie.options
            .mapNotNull { it.subtitles }
            .distinct()

    var selectedOption by remember(movie.id) {
        mutableStateOf(movie.options.firstOrNull())
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Color(0xFFF7F7F8)
            )
    ) {
        AsyncImage(
            model = movie.backdrop ?: movie.poster,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(470.dp)
        )

        Box(
            Modifier
                .fillMaxWidth()
                .height(520.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xFFF7F7F8),
                            Color(0xFFF7F7F8)
                        )
                    )
                )
        )

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 54.dp),
            contentPadding = PaddingValues(
                top = 24.dp,
                bottom = 55.dp
            ),
            verticalArrangement =
                Arrangement.spacedBy(18.dp)
        ) {
            item {
                IconButton(
                    onClick = onBack
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
            }

            item {
                Row(
                    Modifier.padding(top = 145.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(28.dp),
                    verticalAlignment =
                        Alignment.Bottom
                ) {
                    AsyncImage(
                        model = movie.poster,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(245.dp)
                            .height(355.dp)
                            .clip(
                                RoundedCornerShape(15.dp)
                            )
                    )

                    Column(
                        Modifier.weight(1f)
                    ) {
                        Text(
                            movie.title,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black
                        )

                        if (
                            movie.originalTitle.isNotBlank() &&
                            movie.originalTitle != movie.title
                        ) {
                            Text(
                                movie.originalTitle,
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(
                            Modifier.height(10.dp)
                        )

                        Text(
                            detailsMeta(movie),
                            color = Color.Gray
                        )

                        Spacer(
                            Modifier.height(14.dp)
                        )

                        if (movie.genres.isNotEmpty()) {
                            Text(
                                movie.genres.joinToString(" • "),
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(
                            Modifier.height(15.dp)
                        )

                        Text(
                            movie.description.ifBlank {
                                "Описание отсутствует."
                            },
                            color = Color(0xFF444444),
                            lineHeight = 23.sp,
                            maxLines = 10,
                            overflow =
                                TextOverflow.Ellipsis
                        )

                        Spacer(
                            Modifier.height(20.dp)
                        )

                        Button(
                            enabled =
                                selectedOption != null,
                            onClick = {
                                selectedOption?.let(
                                    onPlayExternal
                                )
                            }
                        ) {
                            Icon(
                                Icons.Default.OpenInNew,
                                contentDescription = null
                            )

                            Spacer(
                                Modifier.width(7.dp)
                            )

                            Text(
                                "Воспроизвести во внешнем плеере"
                            )
                        }

                        Spacer(
                            Modifier.height(10.dp)
                        )

                        androidx.compose.material3.OutlinedButton(
                            onClick = onFavorite
                        ) {
                            Icon(
                                if (favorite)
                                    Icons.Default.Favorite
                                else
                                    Icons.Default.FavoriteBorder,
                                contentDescription = null
                            )

                            Spacer(
                                Modifier.width(7.dp)
                            )

                            Text(
                                if (favorite)
                                    "В избранном"
                                else
                                    "В избранное"
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "Источник: ${
                        movie.source.ifBlank {
                            "RUTUBE"
                        }
                    }",
                    color = Color.Gray
                )
            }

            if (
                movie.views != null ||
                movie.duration != null
            ) {
                item {
                    Text(
                        listOfNotNull(
                            movie.views?.let {
                                "$it просмотров"
                            },
                            movie.duration
                        ).joinToString(" • "),
                        color = Color.Gray
                    )
                }
            }

            item {
                SelectorBlock(
                    "Качество",
                    if (quality.isNotEmpty())
                        quality
                    else
                        listOf("Нет данных"),
                    selectedOption
                ) { chosen ->
                    selectedOption =
                        movie.options.firstOrNull {
                            it.quality == chosen
                        } ?: selectedOption
                }
            }

            item {
                SelectorBlock(
                    "Озвучка",
                    if (audio.isNotEmpty())
                        audio
                    else
                        listOf("Нет данных"),
                    selectedOption
                ) { chosen ->
                    selectedOption =
                        movie.options.firstOrNull {
                            it.audio == chosen
                        } ?: selectedOption
                }
            }

            item {
                SelectorBlock(
                    "Субтитры",
                    if (subs.isNotEmpty())
                        subs
                    else
                        listOf("Нет данных"),
                    selectedOption
                ) { chosen ->
                    selectedOption =
                        movie.options.firstOrNull {
                            it.subtitles == chosen
                        } ?: selectedOption
                }
            }

            if (loading) {
                item {
                    LinearProgressIndicator(
                        Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun detailsMeta(
    movie: Movie
): String {
    return listOfNotNull(
        movie.year?.toString(),
        movie.rating?.let {
            "★ ${"%.1f".format(it)}"
        },
        movie.author.ifBlank { null }
    ).joinToString(" • ")
}
