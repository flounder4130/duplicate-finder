package finder.indexing

class MdChunk(
    content: String,
    path: String,
    override val coordinates: Coordinates,
    val blockType: String,
) : Chunk(content, path)