package finder.parsing

import finder.indexing.*
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PropertiesParserTest {

    @Test
    fun javaPropertiesParserTest() {
        val result = JavaPropertiesParser().parse(Path.of("src/test/resources/test.properties").readText(), "test.properties")

        val contents = result.map { it.content }

        // Should find all valid property values
        assertTrue(contents.contains("This is a test value"), "Should parse simple property value")
        assertTrue(contents.contains("Another test value with more content"), "Should parse longer property value")
        assertTrue(contents.contains("Value with = equals sign and # hash symbol"), "Should parse value with special characters")
        assertTrue(contents.contains("Value with spaces"), "Should parse value with trimmed spaces")
        assertTrue(contents.contains("first=second=third"), "Should parse value with multiple equal signs")

        // Should not find invalid or filtered out content
        assertFalse(contents.any { it.startsWith("#") }, "Should not contain comments")
        assertFalse(contents.any { it.startsWith("!") }, "Should not contain comments")

        // Should produce only java property chunks
        assertTrue(result.all { it is PropertiesChunk }, "Should only produce java property chunks")

        // Verify line numbers are preserved
        val valueWithLineNumber = result.find { it.content == "This is a test value" }
        assertEquals(LineCoordinates(10), valueWithLineNumber?.coordinates, "Line numbers should be preserved")
    }
}