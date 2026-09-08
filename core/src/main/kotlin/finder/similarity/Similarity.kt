package finder.similarity

import finder.*
import finder.indexing.Chunk
import finder.ngram.ngramProvider
import kotlin.math.max

internal fun similarityRatio(ngramsLeft: Set<Int>, ngramsRight: Set<Int>): Double {
    val intersection = ngramsLeft.intersect(ngramsRight)
    val max = max(ngramsLeft.size, ngramsRight.size)
    return similarityRatio(intersection.size, max)
}

internal fun similarityRatio(intersection: Int, max: Int): Double = if (max == 0) 0.0 else intersection.toDouble() / max

internal fun Chunk.similarity(other: Chunk, options: DuplicateFinderOptions): Int {
    val ngramProvider = ngramProvider(options)
    val thisNgrams = ngramProvider.ngrams(this.content)
    val otherNgrams = ngramProvider.ngrams(other.content)
    return (similarityRatio(thisNgrams, otherNgrams) * 100).toInt()
}