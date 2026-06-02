package finder.ngram

import finder.Length
import it.unimi.dsi.fastutil.ints.*
import java.util.concurrent.ConcurrentHashMap

class CachingNgramProvider private constructor(ngramLength: Length) : NgramProvider {
    companion object {
        @Volatile
        private var instance: CachingNgramProvider? = null

        fun getInstance(ngramLength: Length) = instance ?: synchronized(this) {
            instance ?: CachingNgramProvider(ngramLength).also { instance = it }
        }
    }

    private val cache = ConcurrentHashMap<String, IntSet>()
    private val computeProvider = ComputeNgramProvider.getInstance(ngramLength)

    override fun ngrams(text: String): IntSet = cache.getOrPut(text) { computeProvider.ngrams(text) }
    override fun ngramsOrdered(text: String): IntList = computeProvider.ngramsOrdered(text)
}
