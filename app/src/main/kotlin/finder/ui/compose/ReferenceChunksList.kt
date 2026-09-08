@file:Suppress("FunctionName")

package finder.ui.compose

import finder.ui.preview

import androidx.compose.foundation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import finder.model.ChunkSnapshot
import finder.model.DuplicateMatch

@Composable
fun RowScope.ReferenceChunksList(
    entries: List<Pair<ChunkSnapshot, List<DuplicateMatch>>>,
    selectedReference: MutableState<ChunkSnapshot>,
    selectedDuplicate: MutableState<ChunkSnapshot?>,
) = LazyColumn(
    Modifier.border(1.0.dp, Color.Gray)
        .fillMaxHeight()
        .weight(0.3f)
        .padding(4.dp)
) {
    items(entries) { entry ->
        Text(
            text = "(${entry.numDuplicates}) ${entry.reference.preview}",
            fontSize = LocalFontSize.current.value.size.sp,
            modifier = Modifier.fillMaxWidth()
                .background(if (entry.reference == selectedReference.value) { colors.secondary } else { Color.Transparent })
                .padding(vertical = 4.dp)
                .clickable {
                    selectedReference.value = entry.reference
                    selectedDuplicate.value = null
                }
        )
    }
}

private val Pair<ChunkSnapshot, List<DuplicateMatch>>.reference
    get() = first

private val Pair<ChunkSnapshot, List<DuplicateMatch>>.numDuplicates
    get() = second.size