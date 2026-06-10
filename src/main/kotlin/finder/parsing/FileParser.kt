package finder.parsing

import finder.indexing.*

class FileParser : ContentParser() {

    override fun parse(content: String, path: String) =
        listOf(FileChunk(content, path, LineCoordinates(0)))
}