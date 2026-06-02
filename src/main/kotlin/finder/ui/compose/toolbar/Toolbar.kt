@file:Suppress("FunctionName")

package finder.ui.compose.toolbar

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import finder.ui.sort.SortBy
import finder.ui.compose.*
import finder.ui.compose.fuzzysearch.FuzzySearchDialog

@Composable
fun Toolbar() {
    val options by LocalOptions.current
    var showInClusters by LocalShowInClusters.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.background(colors.primaryVariant)
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        ToolbarMenu("Sort by", SortBy.entries, LocalSorting.current)

        Button(
            onClick = { showInClusters = !(showInClusters) },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (showInClusters) colors.secondary else colors.primary
            )
        ) {
            Text("Show in clusters")
        }

        var showFuzzySearchDialog by remember { mutableStateOf(false) }
        Button(
            onClick = { showFuzzySearchDialog = true },
        ) {
            Text("Fuzzy search")
        }

        if (showFuzzySearchDialog) {
            FuzzySearchDialog { showFuzzySearchDialog = false }
        }

        ToolbarMenu("Font size", FontSize.entries, LocalFontSize.current)
    }
}


