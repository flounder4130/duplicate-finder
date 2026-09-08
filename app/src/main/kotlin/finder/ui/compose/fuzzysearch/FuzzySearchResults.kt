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
import finder.model.*
import finder.ui.preview
import finder.ui.similarityPercent
import finder.ui.compose.*

@Composable
fun ColumnScope.FuzzySearchResults(
    results: List<DuplicateMatch>,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
    ) {
        val selectedResult = remember(results) { mutableStateOf<ChunkSnapshot?>(null) }
        val fontSize = LocalFontSize.current.value.size.sp

        LazyColumn(
            modifier = Modifier.border(1.dp, Color.Gray)
                .padding(8.dp)
        ) {
            items(results) { match ->
                val chunk = match.chunk

                Text(
                    text = "${match.similarityPercent}% ${chunk.preview}",
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