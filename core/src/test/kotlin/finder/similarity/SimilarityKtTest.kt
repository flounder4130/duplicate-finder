package finder.similarity

import finder.mockChunkOf
import finder.mockOptionsForNgramLength
import org.junit.jupiter.api.Test

private const val blah = "1234567890"

private val options = mockOptionsForNgramLength(3)

private val one = mockChunkOf("one")
private val two = mockChunkOf("two")
private val oneLong = mockChunkOf(blah.repeat(2))
private val twoLong = mockChunkOf(blah.reversed().repeat(2))

class SimilarityKtTest {

    @Test
    fun fullSimilarityTest() {
        assert(one.similarity(one, options) == 100)
        assert(oneLong.similarity(oneLong, options) == 100)
    }

    @Test
    fun zeroSimilarityTest() {
        assert(one.similarity(two, options) == 0)
        assert(oneLong.similarity(twoLong, options) == 0)
    }
}