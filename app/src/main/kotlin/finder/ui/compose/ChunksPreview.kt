@file:Suppress("FunctionName")

package finder.ui.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import finder.model.ChunkSnapshot
import finder.model.DuplicateMatch

@Composable
fun RowScope.ChunksPreview(
    selectedReference: MutableState<ChunkSnapshot>,
    selectedDuplicate: MutableState<ChunkSnapshot?>,
    duplicates: List<DuplicateMatch>,
) {
    Column(Modifier.border(1.0.dp, Color.Gray).fillMaxHeight().weight(0.3f)) {
        Row(
            Modifier.weight(0.5f)
                .border(1.0.dp, Color.Gray)
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            HeatMapReferencePreview(LocalFinder.current.heatMap(selectedReference.value, duplicates))
        }
        Row(
            Modifier.weight(0.5f)
                .border(1.0.dp, Color.Gray)
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            SelectionContainer {
                Text(
                    text = selectedDuplicate.value?.content ?: "",
                    fontFamily = FontFamily.Monospace,
                    fontSize = LocalFontSize.current.value.size.sp
                )
            }
        }
    }
}