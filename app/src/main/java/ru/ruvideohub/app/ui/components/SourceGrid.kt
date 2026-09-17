package ru.ruvideohub.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ruvideohub.app.model.ALL_SOURCES

@Composable
fun SourceGrid(
    tv: Boolean,
    onSource: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        items(
            ALL_SOURCES,
            key = { it.key }
        ) { spec ->

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.clickable {
                    onSource(spec.key)
                }
            ) {
                Column(
                    Modifier.padding(
                        horizontal = 17.dp,
                        vertical = 13.dp
                    ),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {
                    Text(
                        spec.title,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Искать видео",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
