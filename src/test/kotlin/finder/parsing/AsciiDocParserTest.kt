package finder.parsing

import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.assertTrue

class AsciiDocParserTest {
    @Test
    fun `test parsing sample asciidoc file`() {
        val result = AsciiDocParser().parse(Path.of("src/test/resources/test-document.adoc").readText())

        assertTrue(result.isNotEmpty(), "Should have parsed some content from the AsciiDoc file")

        println("[DEBUG_LOG] Parsed chunks:")
        result.forEach { chunk ->
            println("[DEBUG_LOG]   ${chunk.type}: '${chunk.content}'")
        }

        val types = result.map { it.type }.toSet()
        println("[DEBUG_LOG] Found types: $types")
        val contents = result.map { it.content }
        println("[DEBUG_LOG] Found contents: $contents")

        // Verify all expected types are present
        assertTrue(types.contains("adoc_section_0"), "Should have parsed document title (level 0)")
        assertTrue(types.contains("adoc_section_1"), "Should have parsed level 1 sections")
        assertTrue(types.contains("adoc_section_2"), "Should have parsed level 2 sections")
        assertTrue(types.contains("adoc_paragraph"), "Should have parsed paragraphs")
        assertTrue(types.contains("adoc_listing"), "Should have parsed source code blocks")
        assertTrue(types.contains("adoc_list_item"), "Should have parsed list items")
        assertTrue(types.contains("adoc_table_header"), "Should have parsed table headers")
        assertTrue(types.contains("adoc_table_row"), "Should have parsed table rows")

        // Verify specific content at different levels
        // Level 0 (Document Title)
        assertTrue(result.any { it.type == "adoc_section_0" && it.content == "Sample Test Document" }, 
            "Should contain document title at level 0")

        // Level 1 Sections
        assertTrue(result.any { it.type == "adoc_section_1" && it.content == "Introduction" }, 
            "Should contain Introduction section at level 1")
        assertTrue(result.any { it.type == "adoc_section_1" && it.content == "Lists and Tables" }, 
            "Should contain Lists and Tables section at level 1")

        // Level 2 Sections
        assertTrue(result.any { it.type == "adoc_section_2" && it.content == "Section 1.1" }, 
            "Should contain Section 1.1 at level 2")
        assertTrue(result.any { it.type == "adoc_section_2" && it.content == "List Examples" }, 
            "Should contain List Examples section at level 2")

        // Level 3 Sections
        assertTrue(result.any { it.type == "adoc_section_3" && it.content == "Section 1.1.1" }, 
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
        assertTrue(result.any { it.type == "adoc_table_title" && it.content == "Sample Table Title" },
            "Should contain table title")
        assertTrue(result.any { it.type == "adoc_table_header" && it.content == "Header 1 | Header 2" },
            "Should contain table headers")
        assertTrue(result.any { it.type == "adoc_table_row" && it.content == "Cell 1,1 | Cell 1,2" },
            "Should contain first table row")
        assertTrue(result.any { it.type == "adoc_table_row" && it.content == "Cell 2,1 | Cell 2,2" },
            "Should contain second table row")
    }
}
