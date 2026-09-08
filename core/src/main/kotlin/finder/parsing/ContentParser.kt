package finder.parsing

import finder.indexing.Chunk

internal abstract class ContentParser {
    abstract fun parse(content: String, path: String): List<Chunk>
}