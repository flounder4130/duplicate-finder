@file:Suppress("FunctionName")

package finder.ui.compose.fuzzysearch

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import finder.model.ChunkSnapshot
import finder.ui.compose.LocalFontSize

@Composable
fun ColumnScope.ResultPreview(chunk: ChunkSnapshot) {
    Text(
        text = chunk.content,
        modifier = Modifier.padding(8.dp).weight(1.0f),
        fontFamily = FontFamily.Monospace,
        fontSize = LocalFontSize.current.value.size.sp,
    )
}