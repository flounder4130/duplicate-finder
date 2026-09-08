@file:Suppress("FunctionName")

package finder.ui.compose.fuzzysearch

import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import finder.model.*
import finder.FuzzyQuery
import finder.ui.compose.*

@Composable
fun ColumnScope.FuzzySearchQuery(
    queryText: MutableState<String>,
    minSimilarity: MutableState<Double>,
    results: MutableState<FuzzySearchResult?>,
) {
    val fontSize = LocalFontSize.current.value.size.sp
    val finder = LocalFinder.current

    Text(
        "Fuzzy Search",
        style = MaterialTheme.typography.h6,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    OutlinedTextField(
        value = queryText.value,
        onValueChange = { queryText.value = it },
        label = { Text("Search Query") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = fontSize)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Min. Similarity: ${(minSimilarity.value * 100).toInt()}%",
            modifier = Modifier.width(150.dp)
        )
        Slider(
            value = minSimilarity.value.toFloat(),
            onValueChange = { minSimilarity.value = it.toDouble() },
            valueRange = 0.5f..1f,
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = {
                if (queryText.value.isNotEmpty()) {
                    results.value = finder.findFuzzy(FuzzyQuery.Text(queryText.value), minSimilarity.value)
                }
            },
            modifier = Modifier.padding(start = 16.dp)
        ) {
            Text("Find")
        }
    }
}