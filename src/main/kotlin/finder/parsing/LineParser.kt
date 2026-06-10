package finder.parsing

import finder.indexing.*

class LineParser : ContentParser() {

    override fun parse(content: String, path: String) = content.lines()
        .withIndex()
        .map { (number, line) -> LineChunk(line, path, LineCoordinates(number)) }
        .toList()
}