package finder.indexing

class XmlChunk(
    content: String,
    path: String,
    override val coordinates: Coordinates,
    val tagName: String,
) : Chunk(content, path)