package finder.indexing

internal class FileChunk(
    content: String,
    path: String,
    override val coordinates: Coordinates,
) : Chunk(content, path)