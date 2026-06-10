@file:Suppress("FunctionName")

package finder.ui.compose.fuzzysearch

import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.dp
import finder.DuplicateFinderOptions
import finder.indexing.*
import finder.similarity.similarity
import finder.ui.compose.*

@Composable
fun ColumnScope.FuzzySearchResults(
    queryText: String,
    results: List<Chunk>,
    options: DuplicateFinderOptions,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
    ) {
        val selectedResult = remember { mutableStateOf<Chunk?>(null) }
        val fontSize = LocalFontSize.current.value.size.sp

        LazyColumn(
            modifier = Modifier.border(1.dp, Color.Gray)
                .padding(8.dp)
        ) {
            val queryChunk = LineChunk(queryText, "", LineCoordinates(0))
            items(results) { chunk ->
                val similarity = chunk.similarity(queryChunk, options)

                Text(
                    text = "${(similarity).toInt()}% ${chunk.preview}",
                    fontSize = fontSize,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth()
                        .clickable { selectedResult.value = chunk }
                        .background(
                            if (chunk == selectedResult.value)
                                colors.secondary
                            else
                                Color.Transparent
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        selectedResult.value?.let { ResultPreview(it) }
    }
}