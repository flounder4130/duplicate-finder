package finder

import finder.parsing.ParserType
import java.nio.file.Path
import kotlin.io.path.extension

data class DuplicateFinderOptions(
    val root: Path,
    val minSimilarity: Double,
    val minLength: Int,
    val minDuplicates: Int,
    val fileMask: Set<String>,
    val parserType: ParserType,
    val verbose: Boolean,
    val cacheNgrams: Boolean,
    val ngramLength: Int,
    val outputDirectory: Path,
    val keepWhitespace: Boolean,
    val inlineNested: Boolean
) {
    fun fileMaskIncludes(path: Path) = fileMask.isEmpty() || path.extension in fileMask

    fun fileMaskIncludesOnly(extension: String) = fileMask.size == 1 && fileMask.single() == extension

    fun fileMaskIsSubsetOf(extensions: Set<String>) = (fileMask - extensions).isEmpty()

    fun withMinSimilarity(d: Double): DuplicateFinderOptions = copy(minSimilarity = d)
}