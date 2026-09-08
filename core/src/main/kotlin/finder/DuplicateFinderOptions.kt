package finder

import finder.parsing.*
import java.nio.file.Path
import kotlin.io.path.extension

data class DuplicateFinderOptions(
    val root: Path,
    val fileMask: Map<String, ParserType>,
    val minLength: Int = 100,
    val ngramLength: Int = 3,
    val keepWhitespace: Boolean = false,
    val inlineNested: Boolean = false,
    val cacheNgrams: Boolean = false,
    val analysis: AnalysisOptions = AnalysisOptions(),
) {
    init {
        require(ngramLength > 0) { "ngramLength must be positive" }
        require(minLength >= ngramLength) { "minLength must be at least ngramLength" }
        require(fileMask.keys.all { it.isNotBlank() }) { "fileMask extensions must not be blank" }
    }

    internal fun fileMaskIncludes(path: Path) = path.extension in fileMask || "*" in fileMask

    internal fun parserFor(path: Path): ParserType = fileMask[path.extension] ?: fileMask.getValue("*")
}

data class AnalysisOptions(
    val minSimilarity: Double = 0.90,
    val minDuplicates: Int = 1,
) {
    init {
        validateSimilarity(minSimilarity)
        require(minDuplicates >= 1) { "minDuplicates must be at least one" }
    }
}

internal fun validateSimilarity(value: Double) {
    require(value.isFinite() && value > 0.0 && value <= 1.0) {
        "minSimilarity must be finite and in (0, 1]"
    }
}