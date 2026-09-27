package finder

import finder.model.ChunkSnapshot
import finder.parsing.ParserType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.*

class MarkdownParsingTest {
    @TempDir lateinit var root: Path

    private val body = "When creating documentation for new features, consider other writers and try to avoid product-specific details if this does not negatively affect content quality."
    private val path = Path.of("document.md")

    private fun finder(minLength: Int = 150) = DuplicateFinder(DuplicateFinderOptions(
        root = root, fileMask = mapOf("md" to ParserType.MARKDOWN), minLength = minLength,
        analysis = AnalysisOptions(minSimilarity = 0.90),
    ))

    @Test fun `list items and quoted paragraphs are independently searchable`() {
        val documents = mapOf(
            "bullet list" to "* $body\n* $body",
            "ordered list" to "1. $body\n2. $body",
            "nested list" to "* Parent\n  * $body\n  * $body",
            "block quote" to "> $body\n>\n> $body",
            "list in a quote" to "> * $body\n> * $body",
        )
        for ((label, markdown) in documents) {
            val finder = finder()
            finder.updateContent(path, markdown)
            val chunks = finder.chunksInFile(path)
            assertEquals(2, chunks.size, label)
            assertEquals(listOf(body, body), chunks.map { it.content }, label)
            for (chunk in chunks) {
                val match = finder.findFuzzy(FuzzyQuery.Reference(chunk)).matches.single()
                assertNotEquals(chunk.id, match.chunk.id, label)
                assertEquals(1.0, match.similarity, label)
                assertEquals(body, originalText(markdown, chunk), label)
            }
        }
    }

    @Test fun `containers do not introduce duplicate copies of their descendants`() {
        val markdown = "> 1. Parent\n>    * $body"
        val finder = finder()
        finder.updateContent(path, markdown)
        val chunk = finder.chunksInFile(path).single()
        assertEquals(body, chunk.content)
        assertEquals(2, chunk.location.line)
        assertTrue(finder.findFuzzy(FuzzyQuery.Reference(chunk)).matches.isEmpty())
    }

    @Test fun `multiple paragraphs in one list item retain their own complete ranges`() {
        val markdown = """
            # Heading

            1. **First paragraph** starts here
               and continues on a second line.

               A second paragraph with `inline code`.

               > A quoted child
               > on a second line.
        """.trimIndent()
        val finder = finder(minLength = 3)
        finder.updateContent(path, markdown)
        val chunks = finder.chunksInFile(path)
        assertEquals(listOf(1, 3, 6, 8), chunks.map { it.location.line })
        assertEquals(listOf(
            "# Heading",
            "**First paragraph** starts here\n   and continues on a second line.",
            "A second paragraph with `inline code`.",
            "A quoted child\n   > on a second line.",
        ), chunks.map { originalText(markdown, it) })
        assertEquals("First paragraph starts here and continues on a second line.", chunks[1].content)
    }

    @Test fun `ranges use original UTF16 offsets for LF CRLF and CR documents`() {
        for (newline in listOf("\n", "\r\n", "\r")) {
            val markdown = listOf(
                "# Context 😀", "", "* **First 😀 paragraph**", "  continuation αβ", "* Last item",
            ).joinToString(newline)
            val finder = finder(minLength = 3)
            finder.updateContent(path, markdown)
            val chunk = finder.chunksInFile(path).first { it.location.line == 3 }
            val range = assertNotNull(chunk.location.sourceRange)
            assertEquals(markdown.indexOf("**First"), range.start)
            assertEquals(markdown.indexOf("continuation αβ") + "continuation αβ".length, range.endExclusive)
            assertEquals("**First 😀 paragraph**${newline}  continuation αβ", originalText(markdown, chunk))
        }
    }

    @Test fun `code blocks inside quotes retain fences and full source ranges`() {
        val markdown = "> ```text\n> first line\n> second line\n> ```"
        val finder = finder(minLength = 3)
        finder.updateContent(path, markdown)
        val chunk = finder.chunksInFile(path).single()
        assertEquals("first line second line", chunk.content)
        assertEquals(1, chunk.location.line)
        assertEquals("```text\n> first line\n> second line\n> ```", originalText(markdown, chunk))
    }

    @Test fun `indented code and HTML keep their source elements`() {
        val markdown = "    first line\n    second line\n\n<div>\nHTML body\n</div>"
        val finder = finder(minLength = 3)
        finder.updateContent(path, markdown)
        val chunks = finder.chunksInFile(path)
        assertEquals(2, chunks.size)
        assertTrue(originalText(markdown, chunks[0]).contains("first line\n    second line"))
        assertEquals("<div>\nHTML body\n</div>", originalText(markdown, chunks[1]))
    }

    @Test fun `empty containers and separators do not become passages`() {
        val finder = finder(minLength = 3)
        finder.updateContent(path, "---\n\n```\n```\n\n>\n\n* ")
        assertTrue(finder.chunksInFile(path).isEmpty())
    }

    @Test fun `updates replace Markdown ranges and invalidate old references`() {
        val finder = finder()
        val original = "* $body\n* $body"
        finder.updateContent(path, original)
        val old = finder.chunksInFile(path).first()
        val updated = "# Intro\n\n$original"
        finder.updateContent(path, updated)
        assertFailsWith<IllegalArgumentException> { finder.findFuzzy(FuzzyQuery.Reference(old)) }
        val chunks = finder.chunksInFile(path)
        assertEquals(listOf(3, 4), chunks.map { it.location.line })
        assertTrue(chunks.all { originalText(updated, it) == body })
    }

    private fun originalText(markdown: String, chunk: ChunkSnapshot): String {
        val range = assertNotNull(chunk.location.sourceRange)
        return markdown.substring(range.start, range.endExclusive)
    }
}
