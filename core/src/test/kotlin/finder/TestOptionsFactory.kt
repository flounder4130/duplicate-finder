package finder

import java.nio.file.Path

internal fun mockOptionsForNgramLength(length: Int) = DuplicateFinderOptions(
    root = Path.of("./"),
    analysis = AnalysisOptions(minSimilarity = 0.8),
    minLength = 5,
    fileMask = emptyMap(),
    cacheNgrams = false,
    ngramLength = length,
    keepWhitespace = true,
    inlineNested = false,
)

internal fun mockOptions() = DuplicateFinderOptions(
    root = Path.of("./"),
    analysis = AnalysisOptions(minSimilarity = 0.8),
    minLength = 5,
    fileMask = emptyMap(),
    cacheNgrams = false,
    ngramLength = 3,
    keepWhitespace = true,
    inlineNested = false
)