package finder

import finder.parsing.*
import java.nio.file.Path
import kotlin.io.path.extension

data class DuplicateFinderOptions(
    val root: Path,
    val minSimilarity: Double,
    val minLength: Int,
    val minDuplicates: Int,
    val fileMask: Map<String, ParserType>,
    val verbose: Boolean,
    val cacheNgrams: Boolean,
    val ngramLength: Int,
    val outputDirectory: Path,
    val keepWhitespace: Boolean,
    val inlineNested: Boolean
) {
    fun fileMaskIncludes(path: Path) = path.extension in fileMask || FileMask.WILDCARD in fileMask

    fun parserFor(path: Path): ParserType = fileMask[path.extension] ?: fileMask.getValue(FileMask.WILDCARD)

    fun withMinSimilarity(d: Double): DuplicateFinderOptions = copy(minSimilarity = d)
}