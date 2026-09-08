package finder.indexing

internal class LineChunk(
    content: String,
    path: String,
    override val coordinates: Coordinates,
) : Chunk(content, path)