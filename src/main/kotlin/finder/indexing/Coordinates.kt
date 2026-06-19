package finder.indexing

sealed interface Coordinates {
    fun intersects(other: Coordinates): Boolean = false
}