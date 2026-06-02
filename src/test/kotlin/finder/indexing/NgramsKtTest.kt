package finder.indexing

import finder.ngram.ComputeNgramProvider
import finder.similarity.similarityRatio
import org.junit.jupiter.api.Test

val ngramProvider = ComputeNgramProvider.getInstance(3)
val oneNgram = ngramProvider.ngrams("123")
val twoNgram = ngramProvider.ngrams("1234")
val threeNgram = ngramProvider.ngrams("12345")

class NgramsKtTest {
    private fun trigram(s: String) = ngramProvider.ngrams(s)

    @Test
    fun ngramsTest() {
        assert(oneNgram.size == 1)
        assert(oneNgram.containsAll(trigram("123")))

        assert(twoNgram.size == 2)
        assert(twoNgram.containsAll(trigram("123")))
        assert(twoNgram.containsAll(trigram("234")))

        assert(threeNgram.size == 3)
        assert(threeNgram.containsAll(trigram("123")))
        assert(threeNgram.containsAll(trigram("234")))
        assert(threeNgram.containsAll(trigram("345")))
    }

    @Test
    fun identityTest() {
        assert(similarityRatio(oneNgram, oneNgram) == 1.0)
    }

    @Test
    fun differenceTest1() {
        val one = ngramProvider.ngrams("one")
        val two = ngramProvider.ngrams("two")
        assert(similarityRatio(one, two) == 0.0)
    }

    @Test
    fun differenceTest2() {
        val left = ngramProvider.ngrams("left")
        val right = ngramProvider.ngrams("right")
        assert(similarityRatio(left, right) == 0.0)
    }

    @Test
    fun partialSimilarityTest() {
        val card = ngramProvider.ngrams("card")
        val scar = ngramProvider.ngrams("scar")
        assert(similarityRatio(scar, card) == 0.5)
    }

}