package finder.indexing

data class OffsetCoordinates(
    val line: Int,
    val start: Int,
    val end: Int,
    val contentStart: Int,
    val contentEnd: Int,
) : Coordinates {
    override fun toString() = line.toString()

    override fun intersects(other: Coordinates): Boolean =
        other is OffsetCoordinates && start < other.end && other.start < end
}