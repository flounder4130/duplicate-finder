@file:Suppress("FunctionName")

package finder.ui.compose

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import finder.model.HeatMap

@Composable
fun HeatMapReferencePreview(heatMap: HeatMap) {
    SelectionContainer {
        Text(
            text = heatMapText(heatMap),
            fontSize = LocalFontSize.current.value.size.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

internal fun heatMapText(heatMap: HeatMap): AnnotatedString = buildAnnotatedString {
    heatMap.content.forEachIndexed { index, char ->
        withStyle(SpanStyle(color = Color.hsv(120f * heatMap.scores[index], 1f, 0.6f))) { append(char) }
    }
}