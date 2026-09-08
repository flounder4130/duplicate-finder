package finder.ui.swing

import finder.model.*
import finder.ui.sort.SortBy
import finder.ui.sort.chunkComparator
import finder.ui.utils.filterClustered
import javax.swing.DefaultListModel

class ListsData(val report: Map<ChunkSnapshot, List<DuplicateMatch>>) {
    val referenceChunksListModel = DefaultListModel<ChunkSnapshot>()
    val duplicateChunksListModel = DefaultListModel<DuplicateMatch>()
    var sorting = SortBy.MAX_DUPLICATES
    var showInClusters = true

    init { update() }

    fun showDuplicatesFor(chunk: ChunkSnapshot) {
        duplicateChunksListModel.removeAllElements()
        duplicateChunksListModel.addAll(report[chunk].orEmpty())
    }

    fun sort(sortBy: SortBy) {
        sorting = sortBy
        update()
    }

    fun showInClusters(value: Boolean) {
        showInClusters = value
        update()
    }

    private fun addReferenceChunks() = report
        .filterClustered(filter = showInClusters)
        .toList()
        .sortedWith(chunkComparator(sorting))
        .forEach { referenceChunksListModel.addElement(it.first) }

    private fun clear() {
        referenceChunksListModel.removeAllElements()
        duplicateChunksListModel.removeAllElements()
    }

    private fun update() {
        clear()
        addReferenceChunks()
    }
}