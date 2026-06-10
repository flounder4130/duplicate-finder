package finder.ui.heatmap

import finder.mockChunkOf
import finder.mockOptionsForNgramLength
import finder.indexing.*
import finder.ui.utils.charScores
import org.junit.jupiter.api.Test

private val options = mockOptionsForNgramLength(3)

private val oddLength = mockChunkOf("cat")
private val anotherOddLength = mockChunkOf("dog")
private val evenLength = mockChunkOf("duck")
private val long = mockChunkOf("am I working correctly?")

class CharScoresTests {

    @Test
    fun oneOddIdentical() = assert(
        charScores(
            reference = oddLength,
            duplicates = listOf(oddLength),
            options = options
        ).all { it == 1.0f }
    )

    @Test
    fun twoOddIdentical() = assert(
        charScores(
            reference = oddLength,
            duplicates = listOf(oddLength, oddLength),
            options = options
        ).all { it == 1.0f }
    )

    @Test
    fun evenLengthIdentical() = assert(
        charScores(
            reference = evenLength,
            duplicates = listOf(evenLength),
            options = options
        ).all { it == 1.0f }
    )

    @Test
    fun evenOdd() {
        val odd = LineChunk("123", "", LineCoordinates(0))
        val even = LineChunk("1234", "", LineCoordinates(0))
        assert(
            charScores(
                reference = odd,
                duplicates = listOf(even),
                options = options
            ).contentEquals(arrayOf(1.0f, 1.0f, 1.0f))
        )

        assert(
            charScores(
                reference = even,
                duplicates = listOf(odd),
                options = options
            ).contentEquals(arrayOf(1.0f, 0.5f, 0.5f, 0.0f))
        )
    }

    @Test
    fun oneLongIdentical() = assert(
        charScores(
            reference = long,
            duplicates = listOf(long),
            options = options
        ).all { it == 1.0f }
    )

    @Test
    fun twoLongIdentical() = assert(
        charScores(
            reference = long,
            duplicates = listOf(long, long),
            options = options
        ).all { it == 1.0f }
    )

    @Test
    fun twoDifferent() {
        val scores = charScores(
            reference = oddLength,
            duplicates = listOf(anotherOddLength),
            options = options
        )
        assert(scores.all { it == 0.0f })
    }

    @Test
    fun halfMatches() {
        val match = LineChunk("123", "", LineCoordinates(0))
        val different = LineChunk("456", "", LineCoordinates(0))
        assert(
            charScores(
                reference = match,
                duplicates = listOf(match, different),
                options = options
            ).contentEquals(arrayOf(0.5f, 0.5f, 0.5f))
        )
    }
}
