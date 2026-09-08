package finder.ngram

import finder.DuplicateFinderOptions

internal fun ngramProvider(options: DuplicateFinderOptions): NgramProvider {
    return if (options.cacheNgrams) {
        CachingNgramProvider(options.ngramLength)
    } else {
        ComputeNgramProvider(options.ngramLength)
    }
}