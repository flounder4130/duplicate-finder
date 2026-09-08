package finder.ui.sort

import finder.model.ChunkSnapshot
import finder.model.DuplicateMatch
import finder.ui.sort.SortBy.*

fun chunkComparator(sortBy: SortBy): Comparator<Pair<ChunkSnapshot, List<DuplicateMatch>>> = when (sortBy) {
    MAX_DUPLICATES -> compareByDescending { it.second.size }
    MAX_LENGTH -> compareByDescending { it.first.content.length }
    MAX_AVG_SIMILARITY -> compareByDescending<Pair<ChunkSnapshot, List<DuplicateMatch>>> {
        it.second.map { match -> match.similarity }.average()
    }.thenByDescending { it.second.size }
}