package finder.indexing

abstract class Chunk(
    var content: String,
    val path: String,
) {
    abstract val coordinates: Coordinates

    val preview: String
        get() = if (content.length > 15) "$this – ${content.substring(0, 15)}..." else "$this – $content"

    override fun toString(): String {
        return "$path:$coordinates"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Chunk

        if (coordinates != other.coordinates) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + coordinates.hashCode()
        return result
    }
}