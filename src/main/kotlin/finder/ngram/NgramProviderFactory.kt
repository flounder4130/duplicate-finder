package finder.ngram

import finder.DuplicateFinderOptions

fun ngramProvider(options: DuplicateFinderOptions): NgramProvider {
    return if (options.cacheNgrams) {
        CachingNgramProvider.getInstance(options.ngramLength)
    } else {
        ComputeNgramProvider.getInstance(options.ngramLength)
    }
}