package finder.ngram

import it.unimi.dsi.fastutil.ints.*

interface NgramProvider {
    fun ngrams(text: String): IntSet
    fun ngramsOrdered(text: String): IntList
}