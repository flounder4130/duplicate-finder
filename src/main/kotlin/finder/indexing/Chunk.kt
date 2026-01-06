package finder.indexing

import finder.parsing.Element
import java.nio.file.Path

class Chunk(
    val content: String,
    val path: String,
    val lineNumber: Int,
    val type: String,
) {
    val preview: String
        get() = if (content.length > 15) "$this – ${content.substring(0, 15)}..." else "$this – $content"

    override fun toString(): String {
        return "$path:$lineNumber"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Chunk

        if (lineNumber != other.lineNumber) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + lineNumber
        return result
    }

    companion object {
        fun of(element: Element, pathFromRoot: Path) = Chunk(element.content, pathFromRoot.toString(), element.lineNumber, element.type)
    }
}