package finder.indexing

import finder.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IndexLiveUpdateTest {

    private val options = mockOptionsForNgramLength(3)

    private fun xml(path: String, content: String, start: Int = 0, end: Int = 100) =
        XmlChunk(content, path, OffsetCoordinates(1, start, end, start, end), "p")

    @Test
    fun `reindexFile replaces stale content for an unchanged span`() {
        val index = Index(options)
        index.indexChunk(xml("a.topic", "the quick brown fox jumps over the lazy dog"))
        index.computeDocFrequencies()

        index.reindexFile("a.topic", listOf(xml("a.topic", "completely different replacement sentence here")))

        val chunks = index.chunksFlat()
        assertEquals(1, chunks.size)
        assertEquals("completely different replacement sentence here", chunks.single().content)
    }

    @Test
    fun `reindexFile drops duplicates that no longer match`() {
        val index = Index(options)
        val shared = "the quick brown fox jumps over the lazy dog repeatedly"
        index.indexChunk(xml("a.topic", shared))
        index.indexChunk(xml("b.topic", shared))
        index.computeDocFrequencies()

        assertEquals(1, index.duplicatesOf(xml("b.topic", shared)).size)

        index.reindexFile("a.topic", listOf(xml("a.topic", "an entirely unrelated body of text now")))

        assertTrue(index.duplicatesOf(xml("b.topic", shared)).isEmpty(), "a no longer matches b after reindex")
    }

    @Test
    fun `removeFile removes a file's chunks but keeps others`() {
        val index = Index(options)
        index.indexChunk(xml("a.topic", "the quick brown fox jumps over the lazy dog"))
        index.indexChunk(xml("b.topic", "another wholly separate paragraph of content"))
        index.computeDocFrequencies()

        index.removeFile("a.topic")

        val remaining = index.chunksFlat()
        assertEquals(1, remaining.size)
        assertEquals("b.topic", remaining.single().path)
        assertTrue(index.duplicatesInFile("a.topic").isEmpty())
    }

    @Test
    fun `indexing an already indexed chunk does not count it twice`() {
        val strict = options.copy(analysis = AnalysisOptions(minSimilarity = 0.9))
        val index = Index(strict)
        val body = randomBody(seed = 42L, length = 240)
        val probe = xml("query.topic", nearMissOf(body))

        index.indexChunk(xml("a.topic", body))
        assertTrue(index.duplicatesOf(probe).isEmpty(), "the near-miss probe should not match a single copy")

        index.indexChunk(xml("a.topic", body))
        index.indexChunk(xml("a.topic", body))

        assertEquals(1, index.chunksFlat().size, "the chunk was registered more than once")
        assertTrue(index.duplicatesOf(probe).isEmpty(), "re-indexing inflated the similarity score")
    }

    @Test
    fun `reindexFile rejects chunks filed under a different path`() {
        val index = Index(options)
        assertFailsWith<IllegalArgumentException> {
            index.reindexFile("/abs/a.topic", listOf(xml("a.topic", "some body of text long enough to index")))
        }
    }

    @Test
    fun `trim drops postings emptied by removeFile`() {
        val index = Index(options)
        val body = "the quick brown fox jumps over the lazy dog"
        index.indexChunk(xml("a.topic", body))
        assertTrue(index.getForLength(body.length).isNotEmpty(), "the chunk should have postings to begin with")

        index.removeFile("a.topic")
        assertTrue(index.getForLength(body.length).values.all { it.isEmpty })

        index.trim()

        assertTrue(index.getForLength(body.length).isEmpty(), "trim should have dropped the emptied postings")
    }

    @Test
    fun `clear empties the index`() {
        val index = Index(options)
        index.indexChunk(xml("a.topic", "the quick brown fox jumps over the lazy dog"))
        index.indexChunk(xml("b.topic", "the quick brown fox jumps over the lazy dog"))
        index.computeDocFrequencies()

        index.clear()

        assertTrue(index.chunksFlat().isEmpty())
        assertTrue(index.duplicatesOf(xml("c.topic", "the quick brown fox jumps over the lazy dog")).isEmpty())
        assertTrue(index.allDuplicates().isEmpty())
    }
}