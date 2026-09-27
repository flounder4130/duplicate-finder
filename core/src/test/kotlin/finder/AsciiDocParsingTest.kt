package finder

import finder.parsing.ParserType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals

class AsciiDocParsingTest {
    @TempDir lateinit var root: Path

    private fun chunks(content: String): List<Pair<Int, String>> {
        val finder = DuplicateFinder(DuplicateFinderOptions(
            root = root, fileMask = mapOf("adoc" to ParserType.ASCIIDOC), minLength = 3,
        ))
        val path = Path.of("document.adoc")
        finder.updateContent(path, content)
        return finder.chunksInFile(path).map { it.location.line to it.content }
    }

    @Test fun `listing boundaries isolate code from prose and document markup`() {
        val document = """
            A preceding paragraph.
            ----
            .dotfile
            |===
            [source,kotlin]
            * literal bullet

            = not a title
            ----

            A following paragraph.
        """.trimIndent()

        assertEquals(listOf(
            1 to "A preceding paragraph.",
            3 to ".dotfile |=== [source,kotlin] * literal bullet = not a title",
            11 to "A following paragraph.",
        ), chunks(document))
    }

    @Test fun `unfinished listing keeps its literal text through the end of the document`() {
        assertEquals(listOf(2 to ".dotfile * literal bullet"), chunks("----\n.dotfile\n\n* literal bullet"))
    }

    @Test fun `paragraphs before between and after list items keep their starting lines`() {
        val document = """
            Intro one.
            Intro two.
            * First item
            Text after list.
            More text.
            * Last item
            Trailing text.
            Last line.
        """.trimIndent()

        assertEquals(listOf(
            1 to "Intro one. Intro two.",
            3 to "First item",
            4 to "Text after list. More text.",
            6 to "Last item",
            7 to "Trailing text. Last line.",
        ), chunks(document))
    }
}
