package finder.ngram

import finder.Length
import it.unimi.dsi.fastutil.ints.*

class ComputeNgramProvider private constructor(private val ngramLength: Int) : NgramProvider {

    companion object {
        @Volatile
        private var instance: ComputeNgramProvider? = null

        fun getInstance(ngramLength: Length) = instance ?: synchronized(this) {
            instance ?: ComputeNgramProvider(ngramLength).also { instance = it }
        }
    }

    override fun ngrams(text: String): IntSet {
        val set = IntOpenHashSet()
        text.forEachNgram(ngramLength) { set.add(it) }
        return set
    }

    override fun ngramsOrdered(text: String): IntList {
        val list = IntArrayList()
        text.forEachNgram(ngramLength) { list.add(it) }
        return list
    }
}

private inline fun String.forEachNgram(ngramLength: Int, consume: (Int) -> Unit) {
    if (this.length >= ngramLength) {
        for (i in 0..this.length - ngramLength) {
            var h = 0
            for (j in 0 until ngramLength) h = h * 31 + this[i + j].code
            consume(h)
        }
    }
}


