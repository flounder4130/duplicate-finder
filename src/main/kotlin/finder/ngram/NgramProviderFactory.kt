package finder.ngram

import finder.DuplicateFinderOptions

fun ngramProvider(options: DuplicateFinderOptions): NgramProvider {
    return if (options.cacheNgrams) {
        CachingNgramProvider(options.ngramLength)
    } else {
        ComputeNgramProvider(options.ngramLength)
    }
}