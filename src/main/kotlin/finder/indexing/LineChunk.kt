package finder.indexing

class LineChunk(
    content: String,
    path: String,
    override val coordinates: Coordinates,
) : Chunk(content, path)