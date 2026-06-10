package finder.parsing

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
        val result = XmlParser(inlineNested = false).parse(Path.of("src/test/resources/test-document.xml").readText())

        assertEquals(ELEMENTS_COUNT, result.size, "Should have parsed $ELEMENTS_COUNT chunks from the xml file")

        println("[DEBUG_LOG] Parsed chunks:")
        result.forEach { chunk ->
            println("[DEBUG_LOG]   ${chunk.type}: '${chunk.content}'")
        }

        val types = result.map { it.type }.toSet()
        println("[DEBUG_LOG] Found types: $types")
        val contents = result.map { it.content }
        println("[DEBUG_LOG] Found contents: $contents")
        
        
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
        assertTrue(types.contains("xml_p"), "Should include parsed paragraphs")
        assertTrue(types.contains("xml_chapter"), "Should include parsed chapter")
        assertTrue(types.contains("xml_li"), "Should include parsed list items")

        assertFalse(types.contains("xml_list"), "Should not include list because it doesn't have own text content")

        
    }
}