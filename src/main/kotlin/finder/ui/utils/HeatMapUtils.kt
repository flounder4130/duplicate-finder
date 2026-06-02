package finder.ui.utils

import finder.*
import finder.indexing.Chunk
import finder.ngram.ngramProvider
import kotlin.collections.forEach

fun charScores(
    options: DuplicateFinderOptions,
    reference: Chunk,
    duplicates: List<Chunk>
): Array<Float> {
    val ngramProvider = ngramProvider(options)
    val ngramsOccurrences = mutableMapOf<Int, Int>()
    duplicates.forEach { duplicate ->
        ngramProvider.ngrams(duplicate.content).forEach { ngram ->
            ngramsOccurrences[ngram] = (ngramsOccurrences[ngram] ?: 0) + 1
        }
    }

    val charScores = Array(reference.content.length) { 0 }
    val referenceNgrams = ngramProvider.ngramsOrdered(reference.content)
    referenceNgrams.forEachIndexed { offset, ngram ->
        val ngramScore = ngramsOccurrences[ngram] ?: return@forEachIndexed
        val left = 0
        val right = options.ngramLength - 1
        for (i in left..right) charScores[offset + i] += ngramScore
    }

    return charScoresFloat(charScores, duplicates.size, options.ngramLength)
}

fun charScoresFloat(
    scores: Array<Int>,
    numDuplicates: Int,
    ngramLength: Int
): Array<Float> {
    val floats = Array(scores.size) { 0f }

    var left = 0
    var right = scores.size - 1

    while (left <= right) {
        val numNgrams = scores.size - (ngramLength - 1)
        val maxContainingNgrams = (left + 1).coerceAtMost(numNgrams).coerceAtMost(ngramLength)
        val maxScoreForIndex = numDuplicates * maxContainingNgrams

        val leftProportion = scores[left].toFloat() / maxScoreForIndex
        floats[left] = leftProportion

        val rightProportion = scores[right].toFloat() / maxScoreForIndex
        floats[right] = rightProportion

        left++
        right--
    }

    return floats
}

fun charsToScores(
    options: DuplicateFinderOptions,
    baseChunk: Chunk,
    duplicates: List<Chunk>
): List<Pair<Char, Float>> {
    val charScores = charScores(options, baseChunk, duplicates)
    val chars = baseChunk.content.toCharArray()
    val charsToScores = chars.zip(charScores)
    return charsToScores
}
