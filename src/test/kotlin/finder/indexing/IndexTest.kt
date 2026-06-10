package finder.indexing

import finder.*
import finder.parsing.ParserType
import org.junit.jupiter.api.*
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@Suppress("SameParameterValue")
class IndexTest {

    private fun chunkOf(content: String, path: String = "", lineNumber: Int = 0) =
        Chunk(content, path, lineNumber, "")

    @BeforeEach
    @AfterEach
    fun resetSingleton() {
        Index.resetInstance()
    }

    @Test
    fun `getInstance returns same instance for same options`() {
        val options = mockOptions()
        val index1 = Index.getInstance(options)
        val index2 = Index.getInstance(options)
        assertSame(index1, index2)
    }

    @Test
    fun `getForLength returns empty map for new length`() {
        val index = Index.getInstance(mockOptions())
        val forLength = index.getForLength(10)
        assertTrue(forLength.isEmpty())
    }

    @Test
    fun `getForLength returns same map for same length`() {
        val index = Index.getInstance(mockOptions())
        val forLength1 = index.getForLength(10)
        val forLength2 = index.getForLength(10)
        assertSame(forLength1, forLength2)
    }

    @Test
    fun `chunksFlat returns empty list for empty index`() {
        val index = Index.getInstance(mockOptions())
        assertTrue(index.chunksFlat().isEmpty())
    }

    @Test
    fun `chunksFlat returns distinct chunks after indexing`() {
        val options = mockOptionsForNgramLength(3)
        val index = Index.getInstance(options)

        val chunk1 = chunkOf("hello world", "file1.txt", 1)
        val chunk2 = chunkOf("another text", "file2.txt", 1)

        index.indexChunk(chunk1)
        index.indexChunk(chunk2)

        val chunks = index.chunksFlat()
        assertEquals(2, chunks.size)
        assertTrue(chunks.contains(chunk1))
        assertTrue(chunks.contains(chunk2))
    }

    @Test
    fun `indexing chunks with same length groups them correctly`() {
        val options = mockOptionsForNgramLength(3)
        val index = Index.getInstance(options)

        val chunk1 = chunkOf("12345", "file1.txt", 1)
        val chunk2 = chunkOf("abcde", "file2.txt", 1)

        index.indexChunk(chunk1)
        index.indexChunk(chunk2)

        val forLength5 = index.getForLength(5)
        assertTrue(forLength5.isNotEmpty())
    }

    @Test
    fun `indexing chunks with different lengths creates separate buckets`() {
        val options = mockOptionsForNgramLength(3)
        val index = Index.getInstance(options)

        val chunk1 = chunkOf("12345", "file1.txt", 1)
        val chunk2 = chunkOf("1234567", "file2.txt", 1)

        index.indexChunk(chunk1)
        index.indexChunk(chunk2)

        val forLength5 = index.getForLength(5)
        val forLength7 = index.getForLength(7)

        assertTrue(forLength5.isNotEmpty())
        assertTrue(forLength7.isNotEmpty())
    }

    @Test
    fun `chunks with common ngrams are grouped together`() {
        val options = mockOptionsForNgramLength(3)
        val index = Index.getInstance(options)

        val chunk1 = chunkOf("abcde", "file1.txt", 1)
        val chunk2 = chunkOf("abcxy", "file2.txt", 1)

        index.indexChunk(chunk1)
        index.indexChunk(chunk2)

        val forLength5 = index.getForLength(5)
        assertTrue(forLength5.values.any { it.containsAll(listOf(chunk1, chunk2)) })
    }

    @Test
    fun `indexDirectory indexes files matching file mask`() {
        val tempDir = Files.createTempDirectory("index-test")
        try {
            val file1 = tempDir.resolve("test1.txt")
            val file2 = tempDir.resolve("test2.txt")
            Files.writeString(file1, "hello world content")
            Files.writeString(file2, "another file content")

            resetSingleton()
            val options = mockOptions().copy(root = tempDir, fileMask = mapOf("txt" to ParserType.FILE))
            val index = Index.getInstance(options)
            index.indexDirectory()

            val chunks = index.chunksFlat()
            assertTrue(chunks.isNotEmpty())
        } finally {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        }
    }

    @Test
    fun `indexDirectory skips files not matching file mask`() {
        val tempDir = Files.createTempDirectory("index-test")
        try {
            val file1 = tempDir.resolve("test1.txt")
            val file2 = tempDir.resolve("test2.md")
            Files.writeString(file1, "hello world content")
            Files.writeString(file2, "markdown content")

            resetSingleton()
            val options = mockOptions().copy(root = tempDir, fileMask = mapOf("txt" to ParserType.FILE))
            val index = Index.getInstance(options)
            index.indexDirectory()

            val chunks = index.chunksFlat()
            assertTrue(chunks.all { it.path.endsWith(".txt") })
        } finally {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        }
    }

    @Test
    fun `removeChunksForPath removes all chunks with specified path`() {
        val options = mockOptionsForNgramLength(3)
        val index = Index.getInstance(options)

        val chunk1 = chunkOf("hello world", "file1.txt", 1)
        val chunk2 = chunkOf("hello there", "file1.txt", 2)
        val chunk3 = chunkOf("another text", "file2.txt", 1)

        index.indexChunk(chunk1)
        index.indexChunk(chunk2)
        index.indexChunk(chunk3)

        assertEquals(3, index.chunksFlat().size)

        index.removeChunksForPath("file1.txt")

        val remainingChunks = index.chunksFlat()
        assertEquals(1, remainingChunks.size)
        assertTrue(remainingChunks.contains(chunk3))
    }

    @Test
    fun `removeChunksForPath does nothing when path not found`() {
        val options = mockOptionsForNgramLength(3)
        val index = Index.getInstance(options)

        val chunk1 = chunkOf("hello world", "file1.txt", 1)
        index.indexChunk(chunk1)

        index.removeChunksForPath("nonexistent.txt")

        assertEquals(1, index.chunksFlat().size)
    }

    @Test
    fun `removeChunksForPath removes chunks from multiple length buckets`() {
        val options = mockOptionsForNgramLength(3)
        val index = Index.getInstance(options)

        val chunk1 = chunkOf("short", "file1.txt", 1)
        val chunk2 = chunkOf("much longer text", "file1.txt", 2)
        val chunk3 = chunkOf("other", "file2.txt", 1)

        index.indexChunk(chunk1)
        index.indexChunk(chunk2)
        index.indexChunk(chunk3)

        index.removeChunksForPath("file1.txt")

        val remainingChunks = index.chunksFlat()
        assertEquals(1, remainingChunks.size)
        assertTrue(remainingChunks.all { it.path == "file2.txt" })
    }
}
