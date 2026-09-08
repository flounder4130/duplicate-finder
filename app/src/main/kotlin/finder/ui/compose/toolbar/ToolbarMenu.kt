@file:Suppress("FunctionName")

package finder.ui.compose.toolbar

import androidx.compose.material.*
import androidx.compose.runtime.*
import finder.ui.MenuItem

@Composable
fun <T: MenuItem>ToolbarMenu(
    title: String,
    options: List<T>,
    state: MutableState<T>
) {
    val expanded = remember { mutableStateOf(false) }

        Button(
            onClick = { expanded.value = true } ,
        ) {
            Text(title)
            DropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(onClick = {
                        state.value = option
                        expanded.value = false
                    }) {
                        Text(option.uiText())
                    }
                }
            }
    }
}