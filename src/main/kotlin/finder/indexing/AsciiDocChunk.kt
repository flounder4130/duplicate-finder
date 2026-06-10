package finder.indexing

class AsciiDocChunk(
    content: String,
    path: String,
    override val coordinates: Coordinates,
    val blockType: String,
) : Chunk(content, path)