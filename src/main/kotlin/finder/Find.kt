package finder

import finder.indexing.*
import finder.similarity.similarityRatio
import it.unimi.dsi.fastutil.ints.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import java.util.stream.Collectors
import kotlin.collections.*
import kotlin.math.max

internal fun findAll(index: Index): Map<Chunk, List<Chunk>> {
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

internal fun findForChunk(
    referenceChunk: Chunk,
    index: Index,
    options: DuplicateFinderOptions = index.options,
): List<Chunk> {
    val length = referenceChunk.content.length
    val margin = (length - (length * options.minSimilarity)).toInt()
    val minLength = length - margin
    val maxLength = length + margin
    val thisNgramsOrdered = index.orderByFrequency(index.ngramProvider.ngrams(referenceChunk.content))
    val scores = Int2IntOpenHashMap()
    return buildList {
        (minLength..maxLength).forEach { length ->
            val indexForLength = index.bucketForLength(length) ?: return@forEach
            val resultsForLength = findForChunk(referenceChunk, thisNgramsOrdered, indexForLength, index, options, scores)
            addAll(resultsForLength)
        }
    }
}

private fun findForChunk(
    referenceChunk: Chunk,
    thisNgrams: IntList,
    ngramBucket: Int2ObjectOpenHashMap<IntArrayList>,
    index: Index,
    options: DuplicateFinderOptions,
    scores: Int2IntOpenHashMap
): List<Chunk> {
    val ngramProvider = index.ngramProvider
    scores.clear()
    val minScoreFilter = (thisNgrams.size * options.minSimilarity).toInt()
    var currentMaxScore = 0
    val referenceId = index.chunkId(referenceChunk)

    for (evaluatedNgrams in thisNgrams.indices) {
        val ngram = thisNgrams.getInt(evaluatedNgrams)
        val remainingNgrams = thisNgrams.size - evaluatedNgrams
        val idsWithNgram = ngramBucket.get(ngram)
        if (idsWithNgram != null) {
            val ids = idsWithNgram.elements()
            val count = idsWithNgram.size
            for (i in 0 until count) {
                val id = ids[i]
                if (id == referenceId) continue
                val score = scores.addTo(id, 1) + 1
                currentMaxScore = max(score, currentMaxScore)
            }
        }
        if (currentMaxScore + remainingNgrams < minScoreFilter) return emptyList()
    }

    val duplicates = buildList {
        scores.int2IntEntrySet().fastForEach { entry ->
            val score = entry.intValue
            if (score < minScoreFilter) return@fastForEach
            val candidate = index.chunkForId(entry.intKey) ?: return@fastForEach
            if (referenceChunk.overlaps(candidate)) return@fastForEach
            val maxNgrams = max(ngramProvider.ngrams(candidate.content).size, thisNgrams.size)
            if (similarityRatio(score, maxNgrams) >= options.minSimilarity) {
                add(candidate)
            }
        }
    }

    return duplicates
}