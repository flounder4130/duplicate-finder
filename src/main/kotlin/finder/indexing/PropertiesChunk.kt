package finder.indexing

class PropertiesChunk(
    content: String,
    path: String,
    override val coordinates: Coordinates,
) : Chunk(content, path)