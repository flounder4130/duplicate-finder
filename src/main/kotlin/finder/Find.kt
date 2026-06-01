package finder

import finder.ngram.ngramProvider
import finder.indexing.*
import finder.similarity.similarityRatio
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import java.util.stream.Collectors
import kotlin.collections.*
import kotlin.math.max

fun findAll(
    options: DuplicateFinderOptions
): Map<Chunk, List<Chunk>> {
    val (_, _, _, minDuplicates, _, _, verbose) = options
    val index = Index.getInstance(options)
    val chunksFlat = index.chunksFlat()
    val processedChunksCount = AtomicInteger(0)
    return chunksFlat.parallelStream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                {
                    if (verbose && processedChunksCount.incrementAndGet() % 100 == 0) {
                        println("Searching duplicates for chunk ${processedChunksCount.get()}/${chunksFlat.size}")
                    }
                    findForChunk(it, options)
                },
                { _, _ -> throw RuntimeException("Chunk already analyzed") },
            )
        )
        .filter { it.value.size >= minDuplicates.coerceAtLeast(1) }
}

fun findForChunk(
    referenceChunk: Chunk,
    options: DuplicateFinderOptions,
): List<Chunk> {
    val index = Index.getInstance(options)
    val length = referenceChunk.content.length
    val margin = (length - (length * options.minSimilarity)).toInt()
    val minLength = length - margin
    val maxLength = length + margin
    return buildList {
        (minLength..maxLength).forEach { length ->
            val indexForLength = index.getForLength(length)
            val resultsForLength = findForChunk(referenceChunk, indexForLength, options)
            addAll(resultsForLength)
        }
    }
}

private fun findForChunk(
    referenceChunk: Chunk,
    index: Map<Ngram, List<Chunk>>,
    options: DuplicateFinderOptions
): List<Chunk> {
    val ngramProvider = ngramProvider(options)
    val thisNgrams = ngramProvider.ngrams(referenceChunk.content)
    val thisNgramsOrdered = Index.getInstance(options).orderByFrequency(thisNgrams)
    val scores = Object2IntOpenHashMap<Chunk>()
    val minScoreFilter = (thisNgrams.size * options.minSimilarity).toInt()
    var currentMaxScore = 0

    for ((evaluatedNgrams, ngram) in thisNgramsOrdered.withIndex()) {
        val remainingNgrams = thisNgramsOrdered.size - evaluatedNgrams
        val chunksWithNgram = (index[ngram] ?: emptyList())
        for (other in chunksWithNgram) {
            if (other === referenceChunk) continue
            val score = scores.getInt(other) + 1
            scores.put(other, score)
            currentMaxScore = max(score, currentMaxScore)
        }
        if (currentMaxScore + remainingNgrams < minScoreFilter) return emptyList()
    }

    val duplicates = buildList {
        scores.object2IntEntrySet().fastForEach { (candidate, score) ->
            if (score < minScoreFilter) return@fastForEach
            val maxNgrams = max(ngramProvider.ngrams(candidate.content).size, thisNgramsOrdered.size)
            if (similarityRatio(score, maxNgrams) >= options.minSimilarity) {
                add(candidate)
            }
        }
    }

    return duplicates
}