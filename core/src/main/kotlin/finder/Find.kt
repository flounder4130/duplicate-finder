package finder

import finder.indexing.*
import finder.similarity.similarityRatio
import it.unimi.dsi.fastutil.ints.*
import java.util.function.Function
import java.util.stream.Collectors
import kotlin.math.max

internal data class ScoredChunk(val chunk: Chunk, val similarity: Double)

internal fun findAll(index: Index, options: AnalysisOptions = index.options.analysis): Map<Chunk, List<ScoredChunk>> =
    index.chunksFlat().parallelStream()
        .collect(Collectors.toMap(
            Function.identity(),
            { findMatches(it, index, options.minSimilarity) },
            { _, _ -> error("Chunk already analyzed") },
        ))
        .filterValues { it.size >= options.minDuplicates }

internal fun findForChunk(
    referenceChunk: Chunk,
    index: Index,
    options: DuplicateFinderOptions = index.options,
): List<Chunk> = findMatches(referenceChunk, index, options.analysis.minSimilarity).map { it.chunk }

internal fun findMatches(reference: Chunk, index: Index, minSimilarity: Double): List<ScoredChunk> {
    val length = reference.content.length
    if (length < index.options.ngramLength) return emptyList()
    val margin = (length - length * minSimilarity).toInt()
    val ngrams = index.orderByFrequency(index.ngramProvider.ngrams(reference.content))
    val scores = Int2IntOpenHashMap()
    return buildList {
        for (candidateLength in length - margin..length + margin) {
            val bucket = index.bucketForLength(candidateLength) ?: continue
            addAll(findMatches(reference, ngrams, bucket, index, minSimilarity, scores))
        }
    }
}

private fun findMatches(
    reference: Chunk,
    ngrams: IntList,
    bucket: Int2ObjectOpenHashMap<IntArrayList>,
    index: Index,
    minSimilarity: Double,
    scores: Int2IntOpenHashMap,
): List<ScoredChunk> {
    scores.clear()
    val minScore = (ngrams.size * minSimilarity).toInt()
    var maxScore = 0
    val referenceId = index.chunkId(reference)

    for (evaluated in ngrams.indices) {
        val posting = bucket.get(ngrams.getInt(evaluated))
        if (posting != null) {
            val ids = posting.elements()
            for (i in 0 until posting.size) {
                val id = ids[i]
                if (id == referenceId) continue
                maxScore = max(maxScore, scores.addTo(id, 1) + 1)
            }
        }
        if (maxScore + ngrams.size - evaluated < minScore) return emptyList()
    }

    return buildList {
        scores.int2IntEntrySet().fastForEach { entry ->
            if (entry.intValue < minScore) return@fastForEach
            val candidate = index.chunkForId(entry.intKey) ?: return@fastForEach
            if (reference.overlaps(candidate)) return@fastForEach
            val maxNgrams = max(index.ngramProvider.ngrams(candidate.content).size, ngrams.size)
            val similarity = similarityRatio(entry.intValue, maxNgrams)
            if (similarity >= minSimilarity) add(ScoredChunk(candidate, similarity))
        }
    }
}