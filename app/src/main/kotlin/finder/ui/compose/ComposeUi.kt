@file:Suppress("FunctionName")

package finder.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import finder.DuplicateFinder
import finder.ui.compose.toolbar.Toolbar

fun composeUi(finder: DuplicateFinder) {
    val report = finder.report
    application {
        val windowState = rememberWindowState(
            width = 1200.dp,
            height = 800.dp,
            placement = WindowPlacement.Maximized,
        )
        Window(onCloseRequest = ::exitApplication, title = "Duplicate Finder", state = windowState) {
            CompositionLocalProvider(LocalFinder provides finder) {
                MaterialTheme {
                    Column {
                        if (report.duplicates.isEmpty()) {
                            AlertDialog(
                                onDismissRequest = ::exitApplication,
                                title = { Text("Duplicate Finder") },
                                text = { Text("No duplicates found") },
                                confirmButton = { Button(onClick = ::exitApplication) { Text("Close") } },
                            )
                        } else {
                            Toolbar()
                            Main(report.duplicates)
                        }
                    }
                }
            }
        }
    }
}