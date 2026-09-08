@file:Suppress("FunctionName")

package finder.ui.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.*
import finder.model.ChunkSnapshot
import finder.model.DuplicateMatch
import finder.ui.sort.chunkComparator
import finder.ui.utils.filterClustered

@Composable
fun Main(report: Map<ChunkSnapshot, List<DuplicateMatch>>) {
    val sorting by LocalSorting.current
    val showInClusters by LocalShowInClusters.current
    val entries = remember(report, showInClusters, sorting) {
        report.filterClustered(showInClusters).toList().sortedWith(chunkComparator(sorting))
    }
    if (entries.isEmpty()) return
    val selectedReference = remember(entries) { mutableStateOf(entries.first().first) }
    val duplicates = report.getValue(selectedReference.value)
    val selectedDuplicate = remember(selectedReference.value) {
        mutableStateOf<ChunkSnapshot?>(duplicates.firstOrNull()?.chunk)
    }

    Row {
        ReferenceChunksList(entries, selectedReference, selectedDuplicate)
        DuplicateChunksList(duplicates, selectedDuplicate)
        ChunksPreview(selectedReference, selectedDuplicate, duplicates)
    }
}