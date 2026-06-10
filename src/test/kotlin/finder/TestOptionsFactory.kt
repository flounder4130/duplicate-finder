package finder

import java.nio.file.Path

fun mockOptionsForNgramLength(length: Int) = DuplicateFinderOptions(
    root = Path.of("./"),
    minSimilarity = 0.8,
    minLength = 5,
    minDuplicates = 1,
    fileMask = emptyMap(),
    verbose = false,
    cacheNgrams = false,
    ngramLength = length,
    outputDirectory = Path.of("./"),
    keepWhitespace = true,
    inlineNested = false,
)

fun mockOptions() = DuplicateFinderOptions(
    root = Path.of("./"),
    minSimilarity = 0.8,
    minLength = 5,
    minDuplicates = 1,
    fileMask = emptyMap(),
    verbose = false,
    cacheNgrams = false,
    ngramLength = 3,
    outputDirectory = Path.of("./"),
    keepWhitespace = true,
    inlineNested = false
)