package finder.parsing

import finder.indexing.XmlChunk
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

const val ELEMENTS_COUNT = 16

class XmlParserTest {

    @Test
    fun xmlParserTest() {
        val result = XmlParser(inlineNested = false).parse(Path.of("src/test/resources/test-document.xml").readText(), "test-document.xml")

        assertEquals(ELEMENTS_COUNT, result.size, "Should have parsed $ELEMENTS_COUNT chunks from the xml file")

        val tags = result.filterIsInstance<XmlChunk>().map { it.tagName }.toSet()
        val contents = result.map { it.content }
        
        
        assertTrue(contents.any { it.contains("Dangling text within a doc") }, "Should parse dangling text within a document")
        assertTrue(contents.any { it.contains("This is paragraph 1") }, "Should parse paragraph 1")
        assertTrue(
            contents.any { it.contains("This is paragraph 2 with") },
            "Should parse paragraph 2"
        )
        assertTrue(contents.any { it.contains("This is paragraph 3") }, "Should parse paragraph 3")
        assertTrue(contents.any { it.contains("This is paragraph 4") }, "Should parse paragraph 4")
        assertTrue(contents.any { it.contains("This is paragraph 5") }, "Should parse paragraph 5")
        assertTrue(
            contents.any { it.contains("Dangling text within a chapter") },
            "Should parse dangling text within a chapter"
        )
        assertTrue(
            contents.any { it.contains("This is a paragraph within a chapter") },
            "Should parse paragraph within a chapter"
        )
        assertTrue(contents.any { it.contains("List item 1") }, "Should parse list item 1")
        assertTrue(contents.any { it.contains("List item 2") }, "Should parse list item 2")
        assertTrue(contents.any { it.contains("List item 3") }, "Should parse list item 3")
        assertTrue(contents.any { it.contains("List item 4") }, "Should parse list item 4")
        assertTrue(contents.any { it.contains("List item 5 with") }, "Should parse list item 5")
        assertTrue(tags.contains("p"), "Should include parsed paragraphs")
        assertTrue(tags.contains("chapter"), "Should include parsed chapter")
        assertTrue(tags.contains("li"), "Should include parsed list items")

        assertFalse(tags.contains("list"), "Should not include list because it doesn't have own text content")

        
    }
}