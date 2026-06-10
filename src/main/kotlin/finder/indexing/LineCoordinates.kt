package finder.indexing

data class LineCoordinates(val lineNumber: Int) : Coordinates {
    override fun toString() = lineNumber.toString()
}