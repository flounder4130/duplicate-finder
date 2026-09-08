package finder.indexing

internal sealed interface Coordinates {
    fun intersects(other: Coordinates): Boolean = false
}