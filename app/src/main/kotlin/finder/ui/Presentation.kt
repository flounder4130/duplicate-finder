package finder.ui

import finder.model.ChunkSnapshot
import finder.model.DuplicateMatch

val ChunkSnapshot.preview: String
    get() = "$this - ${content.take(15)}${if (content.length > 15) "..." else ""}"

val DuplicateMatch.similarityPercent: Int get() = (similarity * 100).toInt()