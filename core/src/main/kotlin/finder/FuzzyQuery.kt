package finder

import finder.model.ChunkSnapshot

sealed interface FuzzyQuery {
    data class Text(val content: String) : FuzzyQuery
    data class Reference(val chunk: ChunkSnapshot) : FuzzyQuery
}