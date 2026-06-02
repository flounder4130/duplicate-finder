package finder.similarity

import finder.*
import finder.indexing.Chunk
import finder.ngram.ngramProvider
import kotlin.math.max

fun similarityRatio(ngramsLeft: Set<Int>, ngramsRight: Set<Int>): Double {
    val intersection = ngramsLeft.intersect(ngramsRight)
    val max = max(ngramsLeft.size, ngramsRight.size)
    return similarityRatio(intersection.size, max)
}

fun similarityRatio(intersection: Int, max: Int): Double = intersection.toDouble() / max

fun Chunk.similarity(other: Chunk, options: DuplicateFinderOptions): Int {
    val ngramProvider = ngramProvider(options)
    val thisNgrams = ngramProvider.ngrams(this.content)
    val otherNgrams = ngramProvider.ngrams(other.content)
    return (similarityRatio(thisNgrams, otherNgrams) * 100).toInt()
}
