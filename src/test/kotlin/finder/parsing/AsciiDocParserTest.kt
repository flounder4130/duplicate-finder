package finder.parsing

import finder.indexing.*
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.assertTrue

class AsciiDocParserTest {
    @Test
    fun `test parsing sample asciidoc file`() {
        val result = AsciiDocParser().parse(Path.of("src/test/resources/test-document.adoc").readText(), "test-document.adoc")

        assertTrue(result.isNotEmpty(), "Should have parsed some content from the AsciiDoc file")

        val blockTypes = result.filterIsInstance<AsciiDocChunk>().map { it.blockType }.toSet()
        val contents = result.map { it.content }

        // Verify all expected block types are present
        assertTrue(blockTypes.contains("section_0"), "Should have parsed document title (level 0)")
        assertTrue(blockTypes.contains("section_1"), "Should have parsed level 1 sections")
        assertTrue(blockTypes.contains("section_2"), "Should have parsed level 2 sections")
        assertTrue(blockTypes.contains("paragraph"), "Should have parsed paragraphs")
        assertTrue(blockTypes.contains("listing"), "Should have parsed source code blocks")
        assertTrue(blockTypes.contains("list_item"), "Should have parsed list items")
        assertTrue(blockTypes.contains("table_header"), "Should have parsed table headers")
        assertTrue(blockTypes.contains("table_row"), "Should have parsed table rows")

        // Verify specific content at different levels
        // Level 0 (Document Title)
        assertTrue(result.hasBlock("section_0", "Sample Test Document"),
            "Should contain document title at level 0")

        // Level 1 Sections
        assertTrue(result.hasBlock("section_1", "Introduction"),
            "Should contain Introduction section at level 1")
        assertTrue(result.hasBlock("section_1", "Lists and Tables"),
            "Should contain Lists and Tables section at level 1")

        // Level 2 Sections
        assertTrue(result.hasBlock("section_2", "Section 1.1"),
            "Should contain Section 1.1 at level 2")
        assertTrue(result.hasBlock("section_2", "List Examples"),
            "Should contain List Examples section at level 2")

        // Level 3 Sections
        assertTrue(result.hasBlock("section_3", "Section 1.1.1"),
            "Should contain Section 1.1.1 at level 3")

        // Paragraphs
        assertTrue(contents.any { it.contains("This is a simple test document") },
            "Should contain introduction paragraph")

        // Verify code block content
        assertTrue(contents.any { it.contains("public class Test") }, "Should contain Java code block")
        assertTrue(contents.any { it.contains("System.out.println") }, "Should contain println in code block")

        // Verify list content
        assertTrue(contents.any { it == "Item 1" }, "Should contain list item 1")
        assertTrue(contents.any { it == "Item 2" }, "Should contain list item 2")
        assertTrue(contents.any { it == "Nested item 2.1" }, "Should contain nested list item")
        assertTrue(contents.any { it == "Headers" }, "Should contain 'Headers' list item")
        assertTrue(contents.any { it == "Paragraphs" }, "Should contain 'Paragraphs' list item")

        // Verify formatted text
        assertTrue(contents.any { it.contains("*bold*") }, "Should contain bold text markup")
        assertTrue(contents.any { it.contains("_italic_") }, "Should contain italic text markup")

        // Verify table content
        assertTrue(result.hasBlock("table_title", "Sample Table Title"),
            "Should contain table title")
        assertTrue(result.hasBlock("table_header", "Header 1 | Header 2"),
            "Should contain table headers")
        assertTrue(result.hasBlock("table_row", "Cell 1,1 | Cell 1,2"),
            "Should contain first table row")
        assertTrue(result.hasBlock("table_row", "Cell 2,1 | Cell 2,2"),
            "Should contain second table row")
    }
}

private fun List<Chunk>.hasBlock(blockType: String, content: String) =
    any { it is AsciiDocChunk && it.blockType == blockType && it.content == content }
