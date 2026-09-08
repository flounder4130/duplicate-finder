package finder.ui.utils

import finder.model.ChunkSnapshot
import finder.model.DuplicateMatch

fun Map<ChunkSnapshot, List<DuplicateMatch>>.filterClustered(filter: Boolean): Map<ChunkSnapshot, List<DuplicateMatch>> {
    if (!filter) return this
    val seen = mutableSetOf<ChunkSnapshot>()
    return filter { (reference, duplicates) ->
        seen.addAll(duplicates.map { it.chunk })
        reference !in seen
    }
}