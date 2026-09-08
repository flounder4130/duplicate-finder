package finder

import finder.indexing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FindOverlapTest {

    private val text = "the quick brown fox jumps over the lazy dog repeatedly and again"

    private fun xml(path: String, line: Int, start: Int, end: Int) =
        XmlChunk(text, path, OffsetCoordinates(line, start, end, start, end), "p")

    private fun indexOf(vararg chunks: Chunk): Index {
        val index = Index(mockOptionsForNgramLength(3))
        chunks.forEach { index.indexChunk(it) }
        index.computeDocFrequencies()
        return index
    }

    @Test
    fun `nested elements in the same file are not reported as duplicates`() {
        // child [10,40) is nested inside parent [0,100): same file, intersecting spans.
        val parent = xml("a.topic", line = 1, start = 0, end = 100)
        val child = xml("a.topic", line = 5, start = 10, end = 40)
        val index = indexOf(parent, child)

        assertTrue(findForChunk(parent, index).isEmpty(), "parent must not report its descendant")
        assertTrue(findForChunk(child, index).isEmpty(), "child must not report its ancestor")
    }

    @Test
    fun `identical elements in different files are still reported`() {
        val a = xml("a.topic", line = 1, start = 0, end = 100)
        val b = xml("b.topic", line = 1, start = 0, end = 100)
        val index = indexOf(a, b)

        assertEquals(listOf(b), findForChunk(a, index))
        assertEquals(listOf(a), findForChunk(b, index))
    }

    @Test
    fun `disjoint siblings in the same file are still reported`() {
        // touching but disjoint: [0,50) and [50,100).
        val first = xml("a.topic", line = 1, start = 0, end = 50)
        val second = xml("a.topic", line = 9, start = 50, end = 100)
        val index = indexOf(first, second)

        assertEquals(listOf(second), findForChunk(first, index))
        assertEquals(listOf(first), findForChunk(second, index))
    }
}