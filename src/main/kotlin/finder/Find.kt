package finder

import finder.indexing.*
import finder.similarity.similarityRatio
import it.unimi.dsi.fastutil.ints.*
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import java.util.stream.Collectors
import kotlin.collections.*
import kotlin.math.max

fun findAll(index: Index): Map<Chunk, List<Chunk>> {
    val options = index.options
    val chunksFlat = index.chunksFlat()
    val processedChunksCount = AtomicInteger(0)
    return chunksFlat.parallelStream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                {
                    if (options.verbose && processedChunksCount.incrementAndGet() % 100 == 0) {
                        println("Searching duplicates for chunk ${processedChunksCount.get()}/${chunksFlat.size}")
                    }
                    findForChunk(it, index)
                },
                { _, _ -> throw RuntimeException("Chunk already analyzed") },
            )
        )
        .filter { it.value.size >= options.minDuplicates.coerceAtLeast(1) }
}

fun findForChunk(
    referenceChunk: Chunk,
    index: Index,
    options: DuplicateFinderOptions = index.options,
): List<Chunk> {
    val length = referenceChunk.content.length
    val margin = (length - (length * options.minSimilarity)).toInt()
    val minLength = length - margin
    val maxLength = length + margin
    val thisNgramsOrdered = index.orderByFrequency(index.ngramProvider.ngrams(referenceChunk.content))
    return buildList {
        (minLength..maxLength).forEach { length ->
            val indexForLength = index.getForLength(length)
            val resultsForLength = findForChunk(referenceChunk, thisNgramsOrdered, indexForLength, index, options)
            addAll(resultsForLength)
        }
    }
}

private fun findForChunk(
    referenceChunk: Chunk,
    thisNgrams: IntList,
    ngramBucket: Int2ObjectOpenHashMap<MutableList<Chunk>>,
    index: Index,
    options: DuplicateFinderOptions
): List<Chunk> {
    val ngramProvider = index.ngramProvider
    val scores = Object2IntOpenHashMap<Chunk>()
    val minScoreFilter = (thisNgrams.size * options.minSimilarity).toInt()
    var currentMaxScore = 0

    for (evaluatedNgrams in thisNgrams.indices) {
        val ngram = thisNgrams.getInt(evaluatedNgrams)
        val remainingNgrams = thisNgrams.size - evaluatedNgrams
        val chunksWithNgram = ngramBucket.get(ngram) ?: emptyList()
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
            val maxNgrams = max(ngramProvider.ngrams(candidate.content).size, thisNgrams.size)
            if (similarityRatio(score, maxNgrams) >= options.minSimilarity) {
                add(candidate)
            }
        }
    }

    return duplicates
}