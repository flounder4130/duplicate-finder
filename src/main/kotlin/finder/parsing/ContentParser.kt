package finder.parsing

import finder.indexing.Chunk

abstract class ContentParser {
    abstract fun parse(content: String, path: String): List<Chunk>
}
