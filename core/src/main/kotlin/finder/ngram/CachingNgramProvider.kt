package finder.ngram

import finder.Length
import it.unimi.dsi.fastutil.ints.*
import java.util.concurrent.ConcurrentHashMap

internal class CachingNgramProvider(ngramLength: Length) : NgramProvider {
    private val cache = ConcurrentHashMap<String, IntSet>()
    private val computeProvider = ComputeNgramProvider(ngramLength)

    override fun ngrams(text: String): IntSet = cache.getOrPut(text) { computeProvider.ngrams(text) }
    override fun ngramsOrdered(text: String): IntList = computeProvider.ngramsOrdered(text)
}