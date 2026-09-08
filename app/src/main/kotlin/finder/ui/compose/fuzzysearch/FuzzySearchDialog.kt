@file:Suppress("FunctionName")

package finder.ui.compose.fuzzysearch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import finder.model.FuzzySearchResult
import finder.ui.compose.LocalFinder

@Composable
fun FuzzySearchDialog(onDismiss: () -> Unit) {
    val options = LocalFinder.current.options
    val queryText = remember { mutableStateOf("") }
    val minSimilarity = remember { mutableStateOf(options.analysis.minSimilarity) }
    val results = remember { mutableStateOf<FuzzySearchResult?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.8f)
                .background(Color.White),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                FuzzySearchQuery(queryText, minSimilarity, results)
                results.value?.let { FuzzySearchResults(it.matches) }
            }
        }
    }
}