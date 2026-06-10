package finder.parsing

import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PropertiesParserTest {

    @Test
    fun javaPropertiesParserTest() {
        val result = JavaPropertiesParser().parse(Path.of("src/test/resources/test.properties").readText())

        println("[DEBUG_LOG] Parsed chunks:")
        result.forEach { chunk ->
            println("[DEBUG_LOG]   ${chunk.type}: '${chunk.content}'")
        }

        val types = result.map { it.type }.toSet()
        println("[DEBUG_LOG] Found types: $types")
        val contents = result.map { it.content }
        println("[DEBUG_LOG] Found contents: $contents")

        // Should find all valid property values
        assertTrue(contents.contains("This is a test value"), "Should parse simple property value")
        assertTrue(contents.contains("Another test value with more content"), "Should parse longer property value")
        assertTrue(contents.contains("Value with = equals sign and # hash symbol"), "Should parse value with special characters")
        assertTrue(contents.contains("Value with spaces"), "Should parse value with trimmed spaces")
        assertTrue(contents.contains("first=second=third"), "Should parse value with multiple equal signs")

        // Should not find invalid or filtered out content
        assertFalse(contents.any { it.startsWith("#") }, "Should not contain comments")
        assertFalse(contents.any { it.startsWith("!") }, "Should not contain comments")

        // Should have the correct type for all elements
        assertEquals(1, types.size, "Should only have one type of elements")
        assertTrue(types.contains("java_property"), "Should find 'property' elements")

        // Verify line numbers are preserved
        val valueWithLineNumber = result.find { it.content == "This is a test value" }
        assertEquals(10, valueWithLineNumber?.lineNumber, "Line numbers should be preserved")
    }
}