package finder.heatmap

import finder.DuplicateFinderOptions
import finder.indexing.Chunk
import finder.ngram.ngramProvider

internal fun charScores(options: DuplicateFinderOptions, reference: Chunk, duplicates: List<Chunk>): Array<Float> =
    charScores(options, reference.content, duplicates.map { it.content })

internal fun charScores(options: DuplicateFinderOptions, reference: String, duplicates: List<String>): Array<Float> {
    val result = Array(reference.length) { 0f }
    if (duplicates.isEmpty() || reference.length < options.ngramLength) return result
    val provider = ngramProvider(options)
    val occurrences = mutableMapOf<Int, Int>()
    duplicates.forEach { text ->
        provider.ngrams(text).forEach { gram -> occurrences[gram] = (occurrences[gram] ?: 0) + 1 }
    }
    val scores = LongArray(reference.length)
    provider.ngramsOrdered(reference).forEachIndexed { offset, gram ->
        val count = occurrences[gram] ?: return@forEachIndexed
        for (i in 0 until options.ngramLength) scores[offset + i] += count.toLong()
    }
    val gramCount = reference.length - options.ngramLength + 1
    for (i in result.indices) {
        val containing = minOf(i + 1, reference.length - i, gramCount, options.ngramLength)
        result[i] = (scores[i].toDouble() / (duplicates.size.toDouble() * containing)).toFloat().coerceIn(0f, 1f)
    }
    return result
}